package me.krunsh.kgui.menu;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.krunsh.kgui.Kgui;

/**
 * Gestionnaire des menus
 * Charge les menus depuis menus/ et templates/
 */
public class MenuManager {

    private final Kgui plugin;
    private final Map<String, MenuData> menus = new HashMap<>();
    private final Map<String, MenuData> templates = new HashMap<>();

    public MenuManager(Kgui plugin) {
        this.plugin = plugin;
        loadAll();
    }

    /**
     * Charge tous les menus et templates
     */
    public void loadAll() {
        menus.clear();
        templates.clear();
        
        // Charger les templates d'abord
        loadTemplates();
        
        // Puis les menus
        loadMenus();
    }

    /**
     * Charge les templates
     */
    private void loadTemplates() {
        File templatesFolder = new File(plugin.getDataFolder(), "templates");
        if (!templatesFolder.exists()) {
            templatesFolder.mkdirs();
        }
        
        // Toujours sauvegarder les fichiers par défaut s'ils n'existent pas (flag false = no overwrite)
        saveDefaultTemplates();
        
        File[] files = templatesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                loadTemplateFile(file);
            }
        }
        
        plugin.getLogger().info("Loaded " + templates.size() + " templates");
    }

    /**
     * Charge les menus
     */
    private void loadMenus() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }
        
        // Toujours sauvegarder les fichiers par défaut s'ils n'existent pas (flag false = no overwrite)
        saveDefaultMenus();
        
        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                loadMenuFile(file);
            }
        }
        
        plugin.getLogger().info("Loaded " + menus.size() + " menus");
    }

    /**
     * Charge un fichier template
     */
    private void loadTemplateFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String templateId = file.getName().replace(".yml", "");
            MenuData template = parseMenu(config, templateId, true);
            if (template != null) {
                templates.put(templateId.toLowerCase(), template);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading template: " + file.getName(), e);
        }
    }

    /**
     * Charge un fichier menu
     */
    private void loadMenuFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String menuId = file.getName().replace(".yml", "");
            MenuData menu = parseMenu(config, menuId, false);
            if (menu != null) {
                menus.put(menuId.toLowerCase(), menu);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading menu: " + file.getName(), e);
        }
    }

    /**
     * Parse une configuration en MenuData
     */
    private MenuData parseMenu(ConfigurationSection config, String menuId, boolean isTemplate) {
        try {
            MenuData menu = new MenuData(menuId, plugin);
            
            // Héritage (support pour "extends" et "inherit_from")
            String extendsTemplate = config.getString("extends", config.getString("inherit_from", null));
            if (extendsTemplate != null && templates.containsKey(extendsTemplate.toLowerCase())) {
                menu.inheritFrom(templates.get(extendsTemplate.toLowerCase()));
            }
            
            // Titre
            menu.setTitle(config.getString("title", "&8Menu"));
            
            // Taille
            menu.setSize(config.getInt("size", 54));
            
            // Type de menu (normal, pagination, scroll)
            menu.setMenuType(MenuType.fromString(config.getString("type", "normal")));
            MenuType requestedMenuType = menu.getMenuType();
            
            // Commandes d'ouverture
            menu.setOpenCommands(config.getStringList("open_commands"));
            menu.setOpenCommand(config.getString("open_command", null));
            
            // Actions à l'ouverture
            menu.setOpenActions(config.getStringList("open_actions"));
            
            // Actions à la fermeture
            menu.setCloseActions(config.getStringList("close_actions"));
            
            // Requirements globaux
            ConfigurationSection reqSection = config.getConfigurationSection("open_requirements");
            if (reqSection != null) {
                menu.setOpenRequirements(parseRequirements(reqSection));
            }
            
            // Permission
            menu.setPermission(config.getString("permission", null));
            
            // Options de combat/monde
            menu.setBlockInCombat(config.getBoolean("block_in_combat", true));
            menu.setAllowedWorlds(config.getStringList("allowed_worlds"));
            menu.setBlockedWorlds(config.getStringList("blocked_worlds"));
            
            // Options WorldGuard
            menu.setOpenOnRegionEnter(config.getStringList("open_on_region_enter"));
            menu.setAllowedRegions(config.getStringList("allowed_regions"));
            menu.setBlockedRegions(config.getStringList("blocked_regions"));
            
            // Cooldown
            menu.setCooldown(config.getInt("cooldown", 0));
            
            // Update interval pour placeholders
            menu.setUpdateInterval(config.getInt("update_interval", 0));
            
            // Pagination settings - support pour section "pagination" ou racine
            ConfigurationSection paginationSection = config.getConfigurationSection("pagination");
            if (paginationSection != null && paginationSection.getBoolean("enabled", true)) {
                // Si le menu est explicitement en scroll, conserver ce type.
                menu.setMenuType(requestedMenuType == MenuType.SCROLL ? MenuType.SCROLL : MenuType.PAGINATION);
                menu.setContentSlots(parseSlotString(paginationSection.getString("content_slots", "")));
                menu.setPrevButtonSlot(paginationSection.getInt("prev_button_slot", -1));
                menu.setNextButtonSlot(paginationSection.getInt("next_button_slot", -1));
                menu.setStaticMaxPages(paginationSection.getInt("max_pages", 0));
                
                // Content provider (API for external plugins like Kfaction)
                if (paginationSection.contains("provider")) {
                    menu.setContentProvider(paginationSection.getString("provider"));
                    
                    // Provider arguments
                    ConfigurationSection argsSection = paginationSection.getConfigurationSection("provider_args");
                    if (argsSection != null) {
                        Map<String, String> args = new HashMap<>();
                        for (String key : argsSection.getKeys(false)) {
                            args.put(key, argsSection.getString(key, ""));
                        }
                        menu.setProviderArgs(args);
                    }
                    
                    // Empty content settings
                    menu.setEmptyMessage(paginationSection.getString("empty_message", null));
                    menu.setEmptyItemConfig(paginationSection.getConfigurationSection("empty_item"));
                }
            } else if (menu.getMenuType() == MenuType.PAGINATION || menu.getMenuType() == MenuType.SCROLL) {
                // Fallback: slots à la racine
                if (config.contains("content_slots")) {
                    if (config.isString("content_slots")) {
                        menu.setContentSlots(parseSlotString(config.getString("content_slots")));
                    } else {
                        menu.setContentSlots(config.getIntegerList("content_slots"));
                    }
                }
                menu.setPrevButtonSlot(config.getInt("prev_button_slot", -1));
                menu.setNextButtonSlot(config.getInt("next_button_slot", -1));
                menu.setStaticMaxPages(config.getInt("max_pages", 0));
                
                // Content provider at root level
                if (config.contains("provider")) {
                    menu.setContentProvider(config.getString("provider"));
                    ConfigurationSection argsSection = config.getConfigurationSection("provider_args");
                    if (argsSection != null) {
                        Map<String, String> args = new HashMap<>();
                        for (String key : argsSection.getKeys(false)) {
                            args.put(key, argsSection.getString(key, ""));
                        }
                        menu.setProviderArgs(args);
                    }
                    menu.setEmptyMessage(config.getString("empty_message", null));
                    menu.setEmptyItemConfig(config.getConfigurationSection("empty_item"));
                }
            }
            
            // Items
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        MenuItem menuItem = parseMenuItem(itemSection, itemKey);
                        if (menuItem != null) {
                            menu.addItem(menuItem);
                        }
                    }
                }
            }
            
            // Animations
            ConfigurationSection animSection = config.getConfigurationSection("animations");
            if (animSection != null) {
                menu.setAnimationConfig(animSection);
            }

            // Conserver un flag explicite pour les commandes/debug legacy.
            menu.setScrollable(menu.getMenuType() == MenuType.SCROLL);
            
            return menu;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing menu: " + menuId, e);
            return null;
        }
    }

    /**
     * Parse un item de menu
     */
    private MenuItem parseMenuItem(ConfigurationSection section, String key) {
        try {
            MenuItem item = new MenuItem(key);
            
            // Slots - support pour entier, liste ou string "0-8,45-53"
            if (section.contains("slot")) {
                item.addSlot(section.getInt("slot"));
            }
            if (section.contains("slots")) {
                if (section.isString("slots")) {
                    // Format string: "0-8,45-53"
                    for (int slot : parseSlotString(section.getString("slots"))) {
                        item.addSlot(slot);
                    }
                } else if (section.isInt("slots")) {
                    // Format entier simple: slots: 20
                    item.addSlot(section.getInt("slots"));
                } else {
                    // Format liste: [0, 1, 2]
                    for (int slot : section.getIntegerList("slots")) {
                        item.addSlot(slot);
                    }
                }
            }
            
            // Item (peut être un item_id ou une définition inline)
            String itemId = section.getString("item_id", section.getString("item", null));
            if (itemId != null) {
                item.setItemId(itemId);
            } else {
                // Définition inline
                item.setInlineMaterial(section.getString("material", "STONE"));
                item.setInlineData(section.getInt("data", 0));
                item.setInlineName(section.getString("display_name", section.getString("name", null)));
                item.setInlineLore(section.getStringList("lore"));
                item.setInlineGlow(section.getBoolean("glow", false));
                item.setInlineSkull(section.getString("skull", section.getString("skull_owner", null)));
                item.setInlineHdb(section.getString("head_database", section.getString("hdb", null)));
                item.setInlineCit(section.getString("cit", section.getString("cit_key", null)));
            }
            
            // Amount (peut contenir des placeholders)
            item.setAmount(section.getString("amount", "1"));
            
            // Priority
            item.setPriority(section.getInt("priority", 0));
            
            // Requirements - support pour 3 formats:
            // 1. ConfigurationSection avec IDs: view_requirements: { req1: {type: permission, ...} }
            // 2. ConfigurationSection singulier: view_requirement: {type: permission, ...}
            // 3. String list: view_requirements: ["permission: xxx", "money: 100"]
            ConfigurationSection reqSection = section.getConfigurationSection("view_requirements");
            if (reqSection != null) {
                item.setViewRequirements(parseRequirements(reqSection));
            } else if (section.isList("view_requirements")) {
                // Format string list
                item.setViewRequirements(parseStringListRequirements(section.getStringList("view_requirements")));
            } else {
                // Support pour view_requirement (singulier) - format direct
                ConfigurationSection singleReq = section.getConfigurationSection("view_requirement");
                if (singleReq != null) {
                    item.setViewRequirements(parseSingleRequirement(singleReq));
                }
            }
            
            ConfigurationSection clickReqSection = section.getConfigurationSection("click_requirements");
            if (clickReqSection != null) {
                item.setClickRequirements(parseRequirements(clickReqSection));
            } else if (section.isList("click_requirements")) {
                // Format string list
                item.setClickRequirements(parseStringListRequirements(section.getStringList("click_requirements")));
            } else {
                ConfigurationSection singleClickReq = section.getConfigurationSection("click_requirement");
                if (singleClickReq != null) {
                    item.setClickRequirements(parseSingleRequirement(singleClickReq));
                }
            }
            
            // Actions par type de clic
            item.setLeftClickActions(section.getStringList("left_click_actions"));
            item.setRightClickActions(section.getStringList("right_click_actions"));
            item.setShiftClickActions(section.getStringList("shift_click_actions"));
            item.setMiddleClickActions(section.getStringList("middle_click_actions"));
            
            // Actions générales (tous les clics)
            item.setClickActions(section.getStringList("click_actions"));
            
            // Deny actions (si requirements non remplis)
            item.setDenyActions(section.getStringList("deny_actions"));
            
            // Cooldown
            item.setCooldown(section.getInt("cooldown", 0));
            
            // Update avec le menu
            item.setUpdateOnRefresh(section.getBoolean("update", true));
            
            // Animation
            if (section.contains("animation")) {
                item.setAnimationId(section.getString("animation"));
            }
            
            return item;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing menu item: " + key, e);
            return null;
        }
    }

    /**
     * Parse une string de slots en liste d'entiers
     * Format supporté: "0-8,45-53,22" ou "10-16,19-25,28-34"
     */
    private List<Integer> parseSlotString(String slotsStr) {
        List<Integer> slots = new ArrayList<>();
        if (slotsStr == null || slotsStr.isEmpty()) return slots;
        
        for (String part : slotsStr.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    for (int i = start; i <= end; i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    slots.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {}
            }
        }
        return slots;
    }

    /**
     * Parse un requirement singulier (format direct sans ID)
     */
    private List<Map<String, Object>> parseSingleRequirement(ConfigurationSection section) {
        List<Map<String, Object>> requirements = new ArrayList<>();
        Map<String, Object> req = new HashMap<>();
        req.put("id", "single");
        req.put("type", section.getString("type", "permission"));
        
        for (String key : section.getKeys(false)) {
            req.put(key, section.get(key));
        }
        
        requirements.add(req);
        return requirements;
    }

    /**
     * Parse une liste de requirements au format string simplifié
     * Formats supportés:
     *   - "permission: kgui.admin"
     *   - "money: 100"
     *   - "points: 50"
     *   - "has_faction: true"
     *   - "placeholder_number: %placeholder% > 10"
     *   - "placeholder_equals: %placeholder% equals value"
     *   - "expression: %page% > 1"
     */
    private List<Map<String, Object>> parseStringListRequirements(List<String> stringList) {
        List<Map<String, Object>> requirements = new ArrayList<>();
        
        int index = 0;
        for (String str : stringList) {
            if (str == null || str.isEmpty()) continue;
            
            Map<String, Object> req = new HashMap<>();
            req.put("id", "req_" + index++);
            
            // Format: "type: value" ou "type: expression"
            int colonIndex = str.indexOf(':');
            if (colonIndex <= 0) continue;
            
            String type = str.substring(0, colonIndex).trim().toLowerCase();
            String value = str.substring(colonIndex + 1).trim();
            
            req.put("type", type);
            
            switch (type) {
                case "permission":
                case "has_permission":
                    req.put("type", "permission");
                    req.put("permission", value);
                    break;
                    
                case "money":
                case "has_money":
                    req.put("type", "money");
                    try {
                        req.put("amount", Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        req.put("amount", 0.0);
                    }
                    break;
                    
                case "points":
                case "has_points":
                    req.put("type", "points");
                    try {
                        req.put("amount", Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        req.put("amount", 0);
                    }
                    break;
                    
                case "has_faction":
                case "in_faction":
                    req.put("type", "has_faction");
                    req.put("value", Boolean.parseBoolean(value));
                    break;
                    
                case "world":
                case "in_world":
                    req.put("type", "world");
                    req.put("world", value);
                    break;
                    
                case "region":
                case "in_region":
                    req.put("type", "region");
                    req.put("region", value);
                    break;
                    
                case "placeholder_number":
                case "expression":
                    // Format: "%placeholder% > 10" ou "%placeholder% < 5"
                    req.put("type", "expression");
                    req.put("expression", value);
                    break;
                    
                case "placeholder_equals":
                    // Format: "%placeholder% equals value" ou "%ph% = val"
                    req.put("type", "placeholder_equals");
                    parseComparisonExpression(value, req);
                    break;
                    
                case "level":
                    req.put("type", "level");
                    try {
                        req.put("level", Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        req.put("level", 0);
                    }
                    break;
                    
                case "gamemode":
                    req.put("type", "gamemode");
                    req.put("gamemode", value.toUpperCase());
                    break;
                    
                default:
                    // Type inconnu, garder tel quel
                    req.put("value", value);
            }
            
            requirements.add(req);
        }
        
        return requirements;
    }

    /**
     * Parse une expression de comparaison simple
     * Exemples: "%page% > 1", "%vault_eco_balance% >= 100"
     */
    private void parseComparisonExpression(String expr, Map<String, Object> req) {
        // Patterns: >, <, >=, <=, ==, =, !=, equals, contains
        String[] operators = {">=", "<=", "!=", "==", "=", ">", "<", " equals ", " contains "};
        
        for (String op : operators) {
            int opIndex = expr.indexOf(op);
            if (opIndex > 0) {
                String placeholder = expr.substring(0, opIndex).trim();
                String value = expr.substring(opIndex + op.length()).trim();
                
                req.put("placeholder", placeholder);
                req.put("value", value);
                
                // Normaliser l'opérateur
                String normalizedOp = op.trim();
                if (normalizedOp.equals("equals") || normalizedOp.equals("=") || normalizedOp.equals("==")) {
                    req.put("operator", "==");
                } else if (normalizedOp.equals("contains")) {
                    req.put("operator", "contains");
                } else {
                    req.put("operator", normalizedOp);
                }
                return;
            }
        }
        
        // Pas d'opérateur trouvé, considérer comme placeholder == value
        req.put("placeholder", expr);
        req.put("value", "true");
        req.put("operator", "==");
    }

    /**
     * Parse des requirements
     */
    private List<Map<String, Object>> parseRequirements(ConfigurationSection section) {
        List<Map<String, Object>> requirements = new ArrayList<>();
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection reqSection = section.getConfigurationSection(key);
            if (reqSection != null) {
                Map<String, Object> req = new HashMap<>();
                req.put("id", key);
                req.put("type", reqSection.getString("type", "permission"));
                
                // Copier toutes les valeurs
                for (String subKey : reqSection.getKeys(false)) {
                    req.put(subKey, reqSection.get(subKey));
                }
                
                requirements.add(req);
            }
        }
        
        return requirements;
    }

    /**
     * Sauvegarde les templates par défaut
     */
    private void saveDefaultTemplates() {
        extractResourceFolder("templates");
    }

    /**
     * Sauvegarde les menus par défaut
     * Auto-découverte: extrait automatiquement TOUS les .yml du dossier menus/ dans le JAR
     * Plus besoin d'ajouter manuellement chaque fichier ici !
     */
    private void saveDefaultMenus() {
        extractResourceFolder("menus");
    }

    /**
     * Extrait automatiquement tous les fichiers .yml d'un dossier du JAR
     * vers le dossier data du plugin (sans écraser les fichiers existants)
     * 
     * @param folderName Le nom du dossier dans le JAR (ex: "menus", "templates")
     */
    private void extractResourceFolder(String folderName) {
        try {
            // Obtenir le chemin du JAR via ProtectionDomain
            File jarPath = new File(plugin.getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            
            if (!jarPath.isFile()) {
                plugin.getLogger().warning("Cannot auto-extract " + folderName + ": not running from JAR");
                return;
            }
            
            int extracted = 0;
            JarFile jar = new JarFile(jarPath);
            try {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    // Matcher les fichiers .yml dans le dossier ciblé (pas les sous-dossiers)
                    if (name.startsWith(folderName + "/") && name.endsWith(".yml") && !entry.isDirectory()) {
                        // saveResource ne remplace pas si le fichier existe déjà (flag false)
                        File target = new File(plugin.getDataFolder(), name);
                        if (!target.exists()) {
                            plugin.saveResource(name, false);
                            extracted++;
                        }
                    }
                }
            } finally {
                jar.close();
            }
            
            if (extracted > 0) {
                plugin.getLogger().info("Auto-extracted " + extracted + " default " + folderName + " files from JAR");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not auto-extract " + folderName + " from JAR: " + e.getMessage());
            // Fallback: ne rien faire, les menus seront juste absents du dossier data
            // L'admin devra les copier manuellement ou supprimer le dossier data pour régénérer
        }
    }

    /**
     * Recharge tous les menus
     */
    public void reload() {
        loadAll();
    }

    /**
     * Vérifie si un menu existe
     */
    public boolean hasMenu(String menuId) {
        return menus.containsKey(menuId.toLowerCase());
    }

    /**
     * Obtient un menu
     */
    public MenuData getMenu(String menuId) {
        return menus.get(menuId.toLowerCase());
    }

    /**
     * Obtient le nombre de menus
     */
    public int getMenuCount() {
        return menus.size();
    }

    /**
     * Obtient tous les menus enregistrés
     */
    public Map<String, MenuData> getMenus() {
        return menus;
    }

    /**
     * Obtient tous les IDs de menus
     */
    public Set<String> getMenuIds() {
        return menus.keySet();
    }

    /**
     * Trouve un menu par sa commande d'ouverture
     */
    public MenuData findByOpenCommand(String command) {
        for (MenuData menu : menus.values()) {
            if (command.equalsIgnoreCase(menu.getOpenCommand())) {
                return menu;
            }
            for (String cmd : menu.getOpenCommands()) {
                if (command.equalsIgnoreCase(cmd)) {
                    return menu;
                }
            }
        }
        return null;
    }

    /**
     * Trouve les menus qui s'ouvrent à l'entrée d'une région
     */
    public List<MenuData> findByRegionEnter(String regionId) {
        List<MenuData> result = new ArrayList<>();
        for (MenuData menu : menus.values()) {
            if (menu.getOpenOnRegionEnter().contains(regionId)) {
                result.add(menu);
            }
        }
        return result;
    }
}
