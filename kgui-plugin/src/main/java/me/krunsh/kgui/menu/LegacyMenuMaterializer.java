package me.krunsh.kgui.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.compiler.CompiledMenu;

/** Temporary adapter between the immutable V2 compiler and the Lot 3 renderer. */
final class LegacyMenuMaterializer {
    private final Kgui plugin;

    LegacyMenuMaterializer(Kgui plugin) {
        this.plugin = plugin;
    }

    MenuData materialize(CompiledMenu compiled) {
        YamlConfiguration config = compiled.toYamlConfiguration();
        MenuData menu = new MenuData(compiled.getId(), plugin);
        menu.setTitle(config.getString("title", "&8Menu"));
        menu.setSize(config.getInt("size", 54));
        menu.setMenuType(MenuType.fromString(config.getString("type", "normal")));
        MenuType requestedType = menu.getMenuType();
        menu.setOpenCommands(config.getStringList("open_commands"));
        menu.setOpenCommand(config.getString("open_command"));
        menu.setOpenActions(config.getStringList("open_actions"));
        menu.setCloseActions(config.getStringList("close_actions"));
        menu.setOpenRequirements(parseRequirements(config.get("open_requirements"), "open"));
        menu.setPermission(config.getString("permission"));
        menu.setBlockInCombat(config.getBoolean("block_in_combat", true));
        menu.setAllowedWorlds(config.getStringList("allowed_worlds"));
        menu.setBlockedWorlds(config.getStringList("blocked_worlds"));
        menu.setOpenOnRegionEnter(config.getStringList("open_on_region_enter"));
        menu.setAllowedRegions(config.getStringList("allowed_regions"));
        menu.setBlockedRegions(config.getStringList("blocked_regions"));
        menu.setCooldown(config.getInt("cooldown", 0));
        menu.setUpdateInterval(config.getInt("update_interval", 0));

        ConfigurationSection pagination = config.getConfigurationSection("pagination");
        if (pagination != null && pagination.getBoolean("enabled", true)) {
            menu.setMenuType(requestedType == MenuType.SCROLL ? MenuType.SCROLL : MenuType.PAGINATION);
            applyPagination(menu, pagination);
        } else if (requestedType == MenuType.PAGINATION || requestedType == MenuType.SCROLL) {
            applyPagination(menu, config);
        }

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section != null) menu.addItem(materializeItem(section, key));
            }
        }
        menu.setAnimationConfig(config.getConfigurationSection("animations"));
        menu.setScrollable(menu.getMenuType() == MenuType.SCROLL);
        menu.setTemplate(compiled.getTemplateId());
        menu.setConfig(config);
        return menu;
    }

    private void applyPagination(MenuData menu, ConfigurationSection config) {
        menu.setContentSlots(parseSlots(config.get("content_slots")));
        menu.setPrevButtonSlot(config.getInt("prev_button_slot", -1));
        menu.setNextButtonSlot(config.getInt("next_button_slot", -1));
        menu.setStaticMaxPages(config.getInt("max_pages", 0));
        menu.setContentProvider(config.getString("provider"));
        ConfigurationSection args = config.getConfigurationSection("provider_args");
        if (args != null) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : args.getKeys(false)) values.put(key, String.valueOf(args.get(key)));
            menu.setProviderArgs(values);
        }
        menu.setEmptyMessage(config.getString("empty_message"));
        menu.setEmptyItemConfig(config.getConfigurationSection("empty_item"));
    }

    private MenuItem materializeItem(ConfigurationSection section, String key) {
        MenuItem item = new MenuItem(key);
        for (Integer slot : parseSlots(section.get("slot"))) item.addSlot(slot);
        for (Integer slot : parseSlots(section.get("slots"))) item.addSlot(slot);

        String itemId = section.getString("item_id", section.getString("item"));
        if (itemId != null) {
            item.setItemId(itemId);
        } else {
            item.setInlineMaterial(section.getString("material", "STONE"));
            item.setInlineData(section.getInt("data", 0));
            item.setInlineName(section.getString("display_name", section.getString("name")));
            item.setInlineLore(section.getStringList("lore"));
            item.setInlineGlow(section.getBoolean("glow", false));
            item.setInlineSkull(section.getString("skull", section.getString("skull_owner")));
            item.setInlineHdb(section.getString("head_database", section.getString("hdb")));
            item.setInlineCit(section.getString("cit", section.getString("cit_key")));
        }

        item.setAmount(section.getString("amount", "1"));
        item.setPriority(section.getInt("priority", 0));
        item.setViewRequirements(parseRequirements(first(section, "view_requirements", "view_requirement"), "view"));
        item.setClickRequirements(parseRequirements(first(section, "click_requirements", "click_requirement"), "click"));
        item.setLeftClickActions(section.getStringList("left_click_actions"));
        item.setRightClickActions(section.getStringList("right_click_actions"));
        item.setShiftClickActions(section.getStringList("shift_click_actions"));
        item.setMiddleClickActions(section.getStringList("middle_click_actions"));
        item.setClickActions(section.getStringList("click_actions"));
        item.setDenyActions(section.getStringList("deny_actions"));
        item.setCooldown(section.getInt("cooldown", 0));
        item.setUpdateOnRefresh(section.getBoolean("update", true));
        item.setAnimationId(section.getString("animation"));
        return item;
    }

    private static Object first(ConfigurationSection section, String plural, String singular) {
        Object value = section.get(plural);
        return value == null ? section.get(singular) : value;
    }

    private List<Map<String, Object>> parseRequirements(Object value, String prefix) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) value;
            if (section.contains("type")) {
                result.add(copyRequirement(prefix, section));
            } else {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection child = section.getConfigurationSection(key);
                    if (child != null) result.add(copyRequirement(key, child));
                }
            }
        } else if (value instanceof List) {
            int index = 0;
            for (Object entry : (List<?>) value) {
                if (entry instanceof String) result.add(parseShortRequirement(prefix + '_' + index++, (String) entry));
            }
        }
        return result;
    }

    private static Map<String, Object> copyRequirement(String id, ConfigurationSection section) {
        Map<String, Object> requirement = new LinkedHashMap<>();
        requirement.put("id", id);
        for (String key : section.getKeys(false)) requirement.put(key, section.get(key));
        return requirement;
    }

    private static Map<String, Object> parseShortRequirement(String id, String expression) {
        Map<String, Object> requirement = new LinkedHashMap<>();
        requirement.put("id", id);
        int separator = expression.indexOf(':');
        String type = separator < 0 ? expression.trim() : expression.substring(0, separator).trim();
        String value = separator < 0 ? "true" : expression.substring(separator + 1).trim();
        requirement.put("type", normalizeRequirementType(type));
        if ("permission".equals(requirement.get("type"))) requirement.put("permission", value);
        else if ("money".equals(requirement.get("type")) || "points".equals(requirement.get("type"))) {
            try { requirement.put("amount", Double.parseDouble(value)); }
            catch (NumberFormatException ignored) { requirement.put("amount", 0D); }
        } else if ("world".equals(requirement.get("type"))) requirement.put("world", value);
        else if ("region".equals(requirement.get("type"))) requirement.put("region", value);
        else if ("expression".equals(requirement.get("type"))) requirement.put("expression", value);
        else requirement.put("value", value);
        return requirement;
    }

    private static String normalizeRequirementType(String type) {
        String normalized = type.toLowerCase();
        if ("has_permission".equals(normalized)) return "permission";
        if ("has_money".equals(normalized)) return "money";
        if ("has_points".equals(normalized)) return "points";
        if ("in_world".equals(normalized)) return "world";
        if ("in_region".equals(normalized)) return "region";
        if ("placeholder_number".equals(normalized)) return "expression";
        return normalized;
    }

    private static List<Integer> parseSlots(Object value) {
        List<Integer> slots = new ArrayList<>();
        if (value instanceof Number) {
            slots.add(((Number) value).intValue());
        } else if (value instanceof List) {
            for (Object entry : (List<?>) value) if (entry instanceof Number) slots.add(((Number) entry).intValue());
        } else if (value instanceof String) {
            for (String part : ((String) value).split(",")) {
                String token = part.trim();
                int separator = token.indexOf('-');
                if (separator > 0) {
                    int start = Integer.parseInt(token.substring(0, separator).trim());
                    int end = Integer.parseInt(token.substring(separator + 1).trim());
                    for (int slot = start; slot <= end; slot++) slots.add(slot);
                } else if (!token.isEmpty()) {
                    slots.add(Integer.parseInt(token));
                }
            }
        }
        return slots;
    }
}
