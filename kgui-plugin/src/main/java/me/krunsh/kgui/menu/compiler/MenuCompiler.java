package me.krunsh.kgui.menu.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import me.krunsh.kgui.menu.MenuType;

/**
 * Side-effect-free Kgui YAML V2 compiler.
 *
 * It parses every source as UTF-8, rejects structural errors, resolves template
 * inheritance and returns immutable models. It never mutates the live registry.
 */
public final class MenuCompiler {
    private static final Pattern ACTION_SYNTAX = Pattern.compile("^\\[([a-zA-Z0-9_.:/-]+)](?:\\s+.*)?$");
    public static final int SCHEMA_VERSION = 2;
    private static final long MAX_SOURCE_BYTES = 4L * 1024L * 1024L;

    private static final Set<String> ROOT_KEYS = setOf(
        "schema_version", "id", "title", "size", "type", "template", "extends", "inherit_from",
        "open_command", "open_commands", "open_actions", "close_actions", "open_requirements",
        "permission", "block_in_combat", "allowed_worlds", "blocked_worlds", "open_on_region_enter",
        "allowed_regions", "blocked_regions", "cooldown", "update_interval", "pagination",
        "content_slots", "prev_button_slot", "next_button_slot", "max_pages", "provider",
        "provider_args", "empty_message", "empty_item", "items", "animations"
    );
    private static final Set<String> PAGINATION_KEYS = setOf(
        "enabled", "content_slots", "prev_button_slot", "next_button_slot", "max_pages",
        "provider", "provider_args", "empty_message", "empty_item"
    );
    private static final Set<String> ITEM_KEYS = setOf(
        "slot", "slots", "item_id", "item", "material", "data", "display_name", "name", "lore",
        "glow", "skull", "skull_owner", "head_database", "hdb", "cit", "cit_key", "amount",
        "priority", "view_requirements", "view_requirement", "click_requirements", "click_requirement",
        "left_click_actions", "right_click_actions", "shift_click_actions", "middle_click_actions",
        "click_actions", "deny_actions", "cooldown", "update", "animation", "hide_attributes",
        "kgui_protected"
    );
    private static final Set<String> ACTION_KEYS = setOf(
        "open_actions", "close_actions", "left_click_actions", "right_click_actions",
        "shift_click_actions", "middle_click_actions", "click_actions", "deny_actions"
    );
    private static final Set<String> MENU_TYPES = setOf(
        "normal", "pagination", "paginated", "paged", "scroll", "scrolling", "horizontal",
        "dynamic", "animated"
    );

    private final File templatesFolder;
    private final File menusFolder;

    public MenuCompiler(File templatesFolder, File menusFolder) {
        this.templatesFolder = templatesFolder;
        this.menusFolder = menusFolder;
    }

    public MenuCompilationResult compileAll() {
        List<MenuDiagnostic> diagnostics = new ArrayList<>();
        Map<String, SourceDocument> templates = loadFolder(templatesFolder, true, diagnostics);
        Map<String, SourceDocument> menus = loadFolder(menusFolder, false, diagnostics);

        for (SourceDocument document : templates.values()) validateStructure(document, diagnostics);
        for (SourceDocument document : menus.values()) validateStructure(document, diagnostics);

        Map<String, Map<String, Object>> resolvedTemplates = new LinkedHashMap<>();
        Map<String, Integer> states = new HashMap<>();
        List<String> chain = new ArrayList<>();
        for (String id : templates.keySet()) {
            resolveTemplate(id, templates, resolvedTemplates, states, chain, diagnostics);
        }

        Map<String, CompiledMenu> compiledTemplates = new LinkedHashMap<>();
        for (Map.Entry<String, SourceDocument> entry : templates.entrySet()) {
            Map<String, Object> resolved = resolvedTemplates.get(entry.getKey());
            if (resolved != null) {
                validateResolved(entry.getValue(), resolved, diagnostics);
                compiledTemplates.put(entry.getKey(), compile(entry.getValue(), resolved, diagnostics));
            }
        }

        Map<String, CompiledMenu> compiledMenus = new LinkedHashMap<>();
        for (Map.Entry<String, SourceDocument> entry : menus.entrySet()) {
            SourceDocument document = entry.getValue();
            String parent = parentOf(document, diagnostics);
            Map<String, Object> resolved = withoutInheritance(document.root);
            if (parent != null) {
                Map<String, Object> template = resolvedTemplates.get(parent.toLowerCase(Locale.ROOT));
                if (template == null) {
                    error(diagnostics, document, "UNKNOWN_TEMPLATE", parentPath(document),
                        "Template inconnu: '" + parent + "'.");
                } else {
                    resolved = deepMerge(template, resolved);
                }
            }
            validateResolved(document, resolved, diagnostics);
            compiledMenus.put(entry.getKey(), compile(document, resolved, diagnostics));
        }

        return new MenuCompilationResult(compiledMenus, compiledTemplates, diagnostics);
    }

    private Map<String, SourceDocument> loadFolder(File folder, boolean template,
                                                    List<MenuDiagnostic> diagnostics) {
        Map<String, SourceDocument> documents = new LinkedHashMap<>();
        File[] sources = folder == null ? null : folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (sources == null) return documents;
        Arrays.sort(sources, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File source : sources) {
            String filename = source.getName();
            String id = filename.substring(0, filename.length() - 4).toLowerCase(Locale.ROOT);
            if (documents.containsKey(id)) {
                diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, "DUPLICATE_ID", filename,
                    "id", 0, "Un autre fichier déclare déjà l'identifiant '" + id + "'."));
                continue;
            }
            SourceDocument document = load(source, id, template, diagnostics);
            if (document != null) documents.put(id, document);
        }
        return documents;
    }

    private SourceDocument load(File source, String id, boolean template,
                                List<MenuDiagnostic> diagnostics) {
        if (source.length() > MAX_SOURCE_BYTES) {
            diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, "SOURCE_TOO_LARGE", source.getName(),
                "", 0, "Le fichier dépasse la limite de 4 Mio."));
            return null;
        }
        String content;
        try {
            content = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
            if (!content.isEmpty() && content.charAt(0) == '\uFEFF') content = content.substring(1);
        } catch (IOException exception) {
            diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, "READ_ERROR", source.getName(),
                "", 0, "Lecture impossible: " + exception.getMessage()));
            return null;
        }

        YamlSourceIndex index = new YamlSourceIndex(source.getName(), content);
        diagnostics.addAll(index.getDiagnostics());
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(content);
        } catch (InvalidConfigurationException exception) {
            diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, "INVALID_YAML", source.getName(),
                "", extractLine(exception.getMessage()), compact(exception.getMessage())));
            return null;
        }
        return new SourceDocument(id, source, template, toMap(yaml), index);
    }

    private void validateStructure(SourceDocument document, List<MenuDiagnostic> diagnostics) {
        for (String key : document.root.keySet()) {
            if (!ROOT_KEYS.contains(key)) {
                error(diagnostics, document, "UNKNOWN_KEY", key, "Clé inconnue dans un menu Kgui V2.");
            }
        }

        Object schema = document.root.get("schema_version");
        if (schema == null) {
            warning(diagnostics, document, "LEGACY_SCHEMA", "schema_version",
                "Ajoutez 'schema_version: 2'. L'absence reste tolérée uniquement pendant la migration.");
        } else if (!(schema instanceof Number) || ((Number) schema).intValue() != SCHEMA_VERSION) {
            error(diagnostics, document, "UNSUPPORTED_SCHEMA", "schema_version",
                "La seule version prise en charge est schema_version: 2.");
        }

        string(document, "id", diagnostics, false);
        string(document, "title", diagnostics, false);
        integer(document, "size", diagnostics, false);
        string(document, "type", diagnostics, false);
        string(document, "open_command", diagnostics, false);
        string(document, "permission", diagnostics, false);
        bool(document, "block_in_combat", diagnostics);
        integer(document, "cooldown", diagnostics, false);
        integer(document, "update_interval", diagnostics, false);
        integer(document, "prev_button_slot", diagnostics, false);
        integer(document, "next_button_slot", diagnostics, false);
        integer(document, "max_pages", diagnostics, false);
        string(document, "provider", diagnostics, false);
        string(document, "empty_message", diagnostics, false);
        if (document.root.containsKey("content_slots")) {
            parseSlots(document, document.root.get("content_slots"), "content_slots", diagnostics);
        }
        stringList(document, "open_commands", diagnostics);
        stringList(document, "allowed_worlds", diagnostics);
        stringList(document, "blocked_worlds", diagnostics);
        stringList(document, "open_on_region_enter", diagnostics);
        stringList(document, "allowed_regions", diagnostics);
        stringList(document, "blocked_regions", diagnostics);
        actionList(document, document.root, "open_actions", "open_actions", diagnostics);
        actionList(document, document.root, "close_actions", "close_actions", diagnostics);
        validateRequirements(document, document.root.get("open_requirements"), "open_requirements", true, diagnostics);

        Object pagination = document.root.get("pagination");
        if (pagination != null) {
            if (!(pagination instanceof Map)) {
                typeError(diagnostics, document, "pagination", "une section YAML");
            } else {
                validatePagination(document, map(pagination), "pagination", diagnostics);
            }
        }
        validateProviderArgs(document, document.root.get("provider_args"), "provider_args", diagnostics);
        if (document.root.containsKey("empty_item")) {
            validateItem(document, document.root.get("empty_item"), "empty_item", false, diagnostics);
        }

        Object items = document.root.get("items");
        if (items != null) {
            if (!(items instanceof Map)) {
                typeError(diagnostics, document, "items", "une section YAML");
            } else {
                for (Map.Entry<String, Object> entry : map(items).entrySet()) {
                    validateItem(document, entry.getValue(), "items." + entry.getKey(), true, diagnostics);
                }
            }
        }
        if (document.root.containsKey("animations") && !(document.root.get("animations") instanceof Map)) {
            typeError(diagnostics, document, "animations", "une section YAML");
        }
    }

    private void validatePagination(SourceDocument document, Map<String, Object> pagination, String path,
                                    List<MenuDiagnostic> diagnostics) {
        for (String key : pagination.keySet()) {
            if (!PAGINATION_KEYS.contains(key)) {
                error(diagnostics, document, "UNKNOWN_KEY", path + "." + key,
                    "Clé inconnue dans la section pagination.");
            }
        }
        if (pagination.containsKey("enabled") && !(pagination.get("enabled") instanceof Boolean)) {
            typeError(diagnostics, document, path + ".enabled", "un booléen");
        }
        for (String key : Arrays.asList("prev_button_slot", "next_button_slot", "max_pages")) {
            if (pagination.containsKey(key) && !(pagination.get(key) instanceof Number)) {
                typeError(diagnostics, document, path + "." + key, "un entier");
            }
        }
        if (pagination.containsKey("provider") && !(pagination.get("provider") instanceof String)) {
            typeError(diagnostics, document, path + ".provider", "une chaîne");
        }
        if (pagination.containsKey("empty_message") && !(pagination.get("empty_message") instanceof String)) {
            typeError(diagnostics, document, path + ".empty_message", "une chaîne");
        }
        validateProviderArgs(document, pagination.get("provider_args"), path + ".provider_args", diagnostics);
        if (pagination.containsKey("empty_item")) {
            validateItem(document, pagination.get("empty_item"), path + ".empty_item", false, diagnostics);
        }
        if (pagination.containsKey("content_slots")) {
            parseSlots(document, pagination.get("content_slots"), path + ".content_slots", diagnostics);
        }
    }

    private void validateItem(SourceDocument document, Object value, String path, boolean requireSlot,
                              List<MenuDiagnostic> diagnostics) {
        if (!(value instanceof Map)) {
            typeError(diagnostics, document, path, "une section d'item");
            return;
        }
        Map<String, Object> item = map(value);
        for (String key : item.keySet()) {
            if (!ITEM_KEYS.contains(key)) {
                error(diagnostics, document, "UNKNOWN_KEY", path + "." + key,
                    "Clé inconnue dans une définition d'item.");
            }
        }
        if (item.containsKey("slot")) parseSlots(document, item.get("slot"), path + ".slot", diagnostics);
        if (item.containsKey("slots")) parseSlots(document, item.get("slots"), path + ".slots", diagnostics);
        for (String key : ITEM_KEYS) {
            if (ACTION_KEYS.contains(key)) actionList(document, item, key, path + "." + key, diagnostics);
        }
        validateRequirements(document, item.get("view_requirements"), path + ".view_requirements", true, diagnostics);
        validateRequirements(document, item.get("view_requirement"), path + ".view_requirement", false, diagnostics);
        validateRequirements(document, item.get("click_requirements"), path + ".click_requirements", true, diagnostics);
        validateRequirements(document, item.get("click_requirement"), path + ".click_requirement", false, diagnostics);
        for (String key : Arrays.asList("data", "priority", "cooldown")) {
            if (item.containsKey(key) && !(item.get(key) instanceof Number)) {
                typeError(diagnostics, document, path + "." + key, "un entier");
            }
        }
        for (String key : Arrays.asList("glow", "update", "hide_attributes", "kgui_protected")) {
            if (item.containsKey(key) && !(item.get(key) instanceof Boolean)) {
                typeError(diagnostics, document, path + "." + key, "un booléen");
            }
        }
        for (String key : Arrays.asList("item_id", "item", "material", "display_name", "name", "skull",
                                        "skull_owner", "head_database", "hdb", "cit", "cit_key", "animation")) {
            if (item.containsKey(key) && !(item.get(key) instanceof String)) {
                typeError(diagnostics, document, path + "." + key, "une chaîne");
            }
        }
        if (item.containsKey("amount") && !(item.get("amount") instanceof String)
            && !(item.get("amount") instanceof Number)) {
            typeError(diagnostics, document, path + ".amount", "une chaîne ou un nombre");
        }
        if (item.containsKey("lore")) validateStringList(document, item.get("lore"), path + ".lore", diagnostics);
    }

    private void validateRequirements(SourceDocument document, Object value, String path, boolean collection,
                                      List<MenuDiagnostic> diagnostics) {
        if (value == null) return;
        if (value instanceof List) {
            validateStringList(document, value, path, diagnostics);
            return;
        }
        if (!(value instanceof Map)) {
            typeError(diagnostics, document, path, "une section ou une liste de chaînes");
            return;
        }
        Map<String, Object> requirements = map(value);
        if (!collection || requirements.containsKey("type")) {
            validateRequirement(document, requirements, path, diagnostics);
            return;
        }
        for (Map.Entry<String, Object> entry : requirements.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                typeError(diagnostics, document, path + "." + entry.getKey(), "une section de condition");
            } else {
                validateRequirement(document, map(entry.getValue()), path + "." + entry.getKey(), diagnostics);
            }
        }
    }

    private void validateRequirement(SourceDocument document, Map<String, Object> requirement, String path,
                                     List<MenuDiagnostic> diagnostics) {
        Object type = requirement.get("type");
        if (!(type instanceof String) || ((String) type).trim().isEmpty()) {
            error(diagnostics, document, "MISSING_REQUIREMENT_TYPE", path + ".type",
                "Chaque condition doit définir un type non vide.");
        }
    }

    private void validateResolved(SourceDocument document, Map<String, Object> root,
                                  List<MenuDiagnostic> diagnostics) {
        int size = number(root.get("size"), 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            error(diagnostics, document, "INVALID_SIZE", "size", "La taille doit être un multiple de 9 entre 9 et 54.");
        }
        String type = text(root.get("type"), "normal").toLowerCase(Locale.ROOT);
        if (!MENU_TYPES.contains(type)) {
            error(diagnostics, document, "INVALID_MENU_TYPE", "type", "Type inconnu: '" + type + "'.");
        }
        for (String key : Arrays.asList("cooldown", "update_interval", "max_pages")) {
            if (root.containsKey(key) && number(root.get(key), 0) < 0) {
                error(diagnostics, document, "NEGATIVE_VALUE", key, "La valeur ne peut pas être négative.");
            }
        }
        validateSlotBounds(document, root.get("content_slots"), "content_slots", size, diagnostics);
        validateButtonSlot(document, root.get("prev_button_slot"), "prev_button_slot", size, diagnostics);
        validateButtonSlot(document, root.get("next_button_slot"), "next_button_slot", size, diagnostics);

        Object paginationValue = root.get("pagination");
        if (paginationValue instanceof Map) {
            Map<String, Object> pagination = map(paginationValue);
            validateSlotBounds(document, pagination.get("content_slots"), "pagination.content_slots", size, diagnostics);
            validateButtonSlot(document, pagination.get("prev_button_slot"), "pagination.prev_button_slot", size, diagnostics);
            validateButtonSlot(document, pagination.get("next_button_slot"), "pagination.next_button_slot", size, diagnostics);
            if (pagination.containsKey("max_pages") && number(pagination.get("max_pages"), 0) < 0) {
                error(diagnostics, document, "NEGATIVE_VALUE", "pagination.max_pages", "La valeur ne peut pas être négative.");
            }
        }

        Object itemsValue = root.get("items");
        if (itemsValue instanceof Map) {
            for (Map.Entry<String, Object> entry : map(itemsValue).entrySet()) {
                if (!(entry.getValue() instanceof Map)) continue;
                Map<String, Object> item = map(entry.getValue());
                String path = "items." + entry.getKey();
                if (!item.containsKey("slot") && !item.containsKey("slots")) {
                    error(diagnostics, document, "MISSING_SLOT", path, "L'item doit définir 'slot' ou 'slots'.");
                }
                validateSlotBounds(document, item.get("slot"), path + ".slot", size, diagnostics);
                validateSlotBounds(document, item.get("slots"), path + ".slots", size, diagnostics);
                if (!hasItemSource(item)) {
                    error(diagnostics, document, "MISSING_ITEM_SOURCE", path,
                        "L'item doit définir item/item_id ou une source inline (material, skull, hdb ou cit)." );
                }
                if (item.containsKey("cooldown") && number(item.get("cooldown"), 0) < 0) {
                    error(diagnostics, document, "NEGATIVE_VALUE", path + ".cooldown", "La valeur ne peut pas être négative.");
                }
            }
        }
    }

    private CompiledMenu compile(SourceDocument document, Map<String, Object> resolved,
                                 List<MenuDiagnostic> diagnostics) {
        Map<String, CompiledItem> items = new LinkedHashMap<>();
        Object itemValue = resolved.get("items");
        if (itemValue instanceof Map) {
            for (Map.Entry<String, Object> entry : map(itemValue).entrySet()) {
                if (!(entry.getValue() instanceof Map)) continue;
                Map<String, Object> item = map(entry.getValue());
                List<Integer> slots = new ArrayList<>();
                slots.addAll(parseSlots(document, item.get("slot"), "items." + entry.getKey() + ".slot", diagnostics));
                slots.addAll(parseSlots(document, item.get("slots"), "items." + entry.getKey() + ".slots", diagnostics));
                slots = new ArrayList<>(new LinkedHashSet<>(slots));
                items.put(entry.getKey(), new CompiledItem(entry.getKey(), slots, item));
            }
        }
        int schema = resolved.get("schema_version") instanceof Number
            ? ((Number) resolved.get("schema_version")).intValue() : 1;
        return new CompiledMenu(document.id, document.file, document.template, schema,
            rawParent(document), text(resolved.get("title"), "&8Menu"), number(resolved.get("size"), 54),
            MenuType.fromString(text(resolved.get("type"), "normal")), items, resolved);
    }

    private Map<String, Object> resolveTemplate(String id, Map<String, SourceDocument> templates,
                                                Map<String, Map<String, Object>> resolved,
                                                Map<String, Integer> states, List<String> chain,
                                                List<MenuDiagnostic> diagnostics) {
        Integer state = states.get(id);
        if (state != null && state == 2) return resolved.get(id);
        SourceDocument document = templates.get(id);
        if (state != null && state == 1) {
            List<String> cycle = new ArrayList<>(chain);
            cycle.add(id);
            error(diagnostics, document, "TEMPLATE_CYCLE", parentPath(document),
                "Cycle d'héritage détecté: " + String.join(" -> ", cycle));
            return withoutInheritance(document.root);
        }

        states.put(id, 1);
        chain.add(id);
        String parent = parentOf(document, diagnostics);
        Map<String, Object> value = withoutInheritance(document.root);
        if (parent != null) {
            String parentId = parent.toLowerCase(Locale.ROOT);
            SourceDocument parentDocument = templates.get(parentId);
            if (parentDocument == null) {
                error(diagnostics, document, "UNKNOWN_TEMPLATE", parentPath(document),
                    "Template parent inconnu: '" + parent + "'.");
            } else {
                Map<String, Object> parentValue = resolveTemplate(parentId, templates, resolved, states, chain, diagnostics);
                value = deepMerge(parentValue, value);
            }
        }
        chain.remove(chain.size() - 1);
        states.put(id, 2);
        resolved.put(id, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepMerge(Map<String, Object> parent, Map<String, Object> child) {
        Map<String, Object> merged = mutableMap(parent);
        for (Map.Entry<String, Object> entry : child.entrySet()) {
            Object inherited = merged.get(entry.getKey());
            Object override = entry.getValue();
            if (inherited instanceof Map && override instanceof Map) {
                merged.put(entry.getKey(), deepMerge((Map<String, Object>) inherited, (Map<String, Object>) override));
            } else if (("open_actions".equals(entry.getKey()) || "close_actions".equals(entry.getKey()))
                       && inherited instanceof List && override instanceof List) {
                List<Object> combined = new ArrayList<>((List<Object>) inherited);
                combined.addAll((List<Object>) override);
                merged.put(entry.getKey(), combined);
            } else {
                merged.put(entry.getKey(), mutable(entry.getValue()));
            }
        }
        return merged;
    }

    private String parentOf(SourceDocument document, List<MenuDiagnostic> diagnostics) {
        List<String> present = new ArrayList<>();
        for (String alias : Arrays.asList("template", "extends", "inherit_from")) {
            if (document.root.containsKey(alias)) present.add(alias);
        }
        if (present.size() > 1) {
            error(diagnostics, document, "AMBIGUOUS_TEMPLATE", present.get(1),
                "Utilisez une seule clé d'héritage: 'template' (recommandée), 'extends' ou 'inherit_from'.");
        }
        if (present.isEmpty()) return null;
        Object value = document.root.get(present.get(0));
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            typeError(diagnostics, document, present.get(0), "un identifiant de template non vide");
            return null;
        }
        return ((String) value).trim();
    }

    private static String rawParent(SourceDocument document) {
        for (String alias : Arrays.asList("template", "extends", "inherit_from")) {
            Object value = document.root.get(alias);
            if (value instanceof String && !((String) value).trim().isEmpty()) return ((String) value).trim();
        }
        return null;
    }

    private static String parentPath(SourceDocument document) {
        for (String alias : Arrays.asList("template", "extends", "inherit_from")) {
            if (document.root.containsKey(alias)) return alias;
        }
        return "template";
    }

    private void validateSlotBounds(SourceDocument document, Object value, String path, int size,
                                    List<MenuDiagnostic> diagnostics) {
        if (value == null) return;
        for (Integer slot : parseSlots(document, value, path, diagnostics)) {
            if (slot < 0 || slot >= size) {
                error(diagnostics, document, "SLOT_OUT_OF_BOUNDS", path,
                    "Le slot " + slot + " est hors de l'inventaire (0-" + (size - 1) + ").");
            }
        }
    }

    private void validateButtonSlot(SourceDocument document, Object value, String path, int size,
                                    List<MenuDiagnostic> diagnostics) {
        if (value == null) return;
        if (!(value instanceof Number)) return;
        int slot = ((Number) value).intValue();
        if (slot < -1 || slot >= size) {
            error(diagnostics, document, "SLOT_OUT_OF_BOUNDS", path,
                "Le slot doit valoir -1 ou être compris entre 0 et " + (size - 1) + ".");
        }
    }

    private List<Integer> parseSlots(SourceDocument document, Object value, String path,
                                     List<MenuDiagnostic> diagnostics) {
        if (value == null) return Collections.emptyList();
        List<Integer> slots = new ArrayList<>();
        if (value instanceof Number) {
            slots.add(((Number) value).intValue());
            return slots;
        }
        if (value instanceof List) {
            for (Object entry : (List<?>) value) {
                if (!(entry instanceof Number)) {
                    typeError(diagnostics, document, path, "une liste d'entiers");
                    return slots;
                }
                slots.add(((Number) entry).intValue());
            }
            return slots;
        }
        if (!(value instanceof String)) {
            typeError(diagnostics, document, path, "un entier, une liste ou une plage de slots");
            return slots;
        }
        String expression = ((String) value).trim();
        if (expression.isEmpty()) return slots;
        for (String part : expression.split(",")) {
            String token = part.trim();
            try {
                int dash = token.indexOf('-');
                if (dash > 0) {
                    int start = Integer.parseInt(token.substring(0, dash).trim());
                    int end = Integer.parseInt(token.substring(dash + 1).trim());
                    if (start > end) throw new NumberFormatException("descending range");
                    for (int slot = start; slot <= end; slot++) slots.add(slot);
                } else {
                    slots.add(Integer.parseInt(token));
                }
            } catch (NumberFormatException exception) {
                error(diagnostics, document, "INVALID_SLOT_EXPRESSION", path,
                    "Expression de slots invalide: '" + token + "'.");
            }
        }
        return slots;
    }

    private static boolean hasItemSource(Map<String, Object> item) {
        for (String key : Arrays.asList("item", "item_id", "material", "skull", "skull_owner",
                                       "head_database", "hdb", "cit", "cit_key")) {
            Object value = item.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) return true;
        }
        return false;
    }

    private void validateProviderArgs(SourceDocument document, Object value, String path,
                                      List<MenuDiagnostic> diagnostics) {
        if (value == null) return;
        if (!(value instanceof Map)) {
            typeError(diagnostics, document, path, "une section clé/valeur");
            return;
        }
        for (Map.Entry<String, Object> entry : map(value).entrySet()) {
            if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
                typeError(diagnostics, document, path + "." + entry.getKey(), "une valeur scalaire");
            }
        }
    }

    private void actionList(SourceDocument document, Map<String, Object> values, String key, String path,
                            List<MenuDiagnostic> diagnostics) {
        if (!values.containsKey(key)) return;
        Object configured = values.get(key);
        validateStringList(document, configured, path, diagnostics);
        if (!(configured instanceof List)) return;
        int index = 0;
        for (Object entry : (List<?>) configured) {
            if (entry instanceof String) {
                Matcher matcher = ACTION_SYNTAX.matcher((String) entry);
                if (!matcher.matches()) {
                    error(diagnostics, document, "INVALID_ACTION", path + "[" + index + "]",
                        "Action invalide; format attendu: [namespace:type] arguments.");
                } else if ("op".equalsIgnoreCase(matcher.group(1)) || "kgui:op".equalsIgnoreCase(matcher.group(1))) {
                    error(diagnostics, document, "FORBIDDEN_OP_ACTION", path + "[" + index + "]",
                        "L'action [op] est interdite; utiliser une action typée ou [console] validée.");
                }
            }
            index++;
        }
    }

    private void stringList(SourceDocument document, String key, List<MenuDiagnostic> diagnostics) {
        if (document.root.containsKey(key)) validateStringList(document, document.root.get(key), key, diagnostics);
    }

    private void validateStringList(SourceDocument document, Object value, String path,
                                    List<MenuDiagnostic> diagnostics) {
        if (!(value instanceof List)) {
            typeError(diagnostics, document, path, "une liste de chaînes");
            return;
        }
        int index = 0;
        for (Object entry : (List<?>) value) {
            if (!(entry instanceof String)) {
                typeError(diagnostics, document, path + "[" + index + "]", "une chaîne");
            }
            index++;
        }
    }

    private void string(SourceDocument document, String key, List<MenuDiagnostic> diagnostics, boolean required) {
        Object value = document.root.get(key);
        if (value == null) {
            if (required) error(diagnostics, document, "MISSING_KEY", key, "Clé obligatoire absente.");
        } else if (!(value instanceof String)) {
            typeError(diagnostics, document, key, "une chaîne");
        }
    }

    private void integer(SourceDocument document, String key, List<MenuDiagnostic> diagnostics, boolean required) {
        Object value = document.root.get(key);
        if (value == null) {
            if (required) error(diagnostics, document, "MISSING_KEY", key, "Clé obligatoire absente.");
        } else if (!(value instanceof Number)) {
            typeError(diagnostics, document, key, "un entier");
        }
    }

    private void bool(SourceDocument document, String key, List<MenuDiagnostic> diagnostics) {
        Object value = document.root.get(key);
        if (value != null && !(value instanceof Boolean)) typeError(diagnostics, document, key, "un booléen");
    }

    private void typeError(List<MenuDiagnostic> diagnostics, SourceDocument document, String path, String expected) {
        error(diagnostics, document, "INVALID_TYPE", path, "Valeur invalide: " + expected + " est attendu(e)." );
    }

    private void error(List<MenuDiagnostic> diagnostics, SourceDocument document, String code, String path, String message) {
        diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, code, document.file.getName(), path,
            document.index.lineOf(path), message));
    }

    private void warning(List<MenuDiagnostic> diagnostics, SourceDocument document, String code, String path, String message) {
        diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.WARNING, code, document.file.getName(), path,
            document.index.lineOf(path), message));
    }

    private static Map<String, Object> withoutInheritance(Map<String, Object> source) {
        Map<String, Object> copy = mutableMap(source);
        copy.remove("template");
        copy.remove("extends");
        copy.remove("inherit_from");
        return copy;
    }

    private static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            values.put(key, child == null ? normalize(section.get(key)) : toMap(child));
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static Object normalize(Object value) {
        if (value instanceof ConfigurationSection) return toMap((ConfigurationSection) value);
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object entry : (List<?>) value) result.add(normalize(entry));
            return result;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableMap(Map<String, Object> source) {
        return (Map<String, Object>) mutable(source);
    }

    @SuppressWarnings("unchecked")
    private static Object mutable(Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                copy.put(entry.getKey(), mutable(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object entry : (List<?>) value) copy.add(mutable(entry));
            return copy;
        }
        return value;
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static String text(Object value, String fallback) {
        return value instanceof String ? (String) value : fallback;
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static int extractLine(String message) {
        if (message == null) return 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("line (\\d+)").matcher(message);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static String compact(String message) {
        if (message == null) return "YAML invalide.";
        return message.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    private static final class SourceDocument {
        private final String id;
        private final File file;
        private final boolean template;
        private final Map<String, Object> root;
        private final YamlSourceIndex index;

        private SourceDocument(String id, File file, boolean template,
                               Map<String, Object> root, YamlSourceIndex index) {
            this.id = id;
            this.file = file;
            this.template = template;
            this.root = root;
            this.index = index;
        }
    }
}
