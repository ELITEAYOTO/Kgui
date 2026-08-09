package me.krunsh.kgui.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.api.RequirementContext;
import me.krunsh.kgui.api.RequirementHandler;
import me.krunsh.kgui.api.RequirementResult;
import me.krunsh.kgui.extension.NamespacedRegistry;
import me.krunsh.kgui.service.KguiApiProvider;

/** Moteur de conditions namespacé et fail-closed. */
public final class RequirementManager {
    private final Kgui plugin;
    private final KguiApiProvider apiProvider;
    private final NamespacedRegistry<RequirementChecker> checkers = new NamespacedRegistry<>("kgui");
    private final Map<String, String> delegatedAliases = new HashMap<>();
    private final SafeExpressionEvaluator expressions = new SafeExpressionEvaluator();
    private final ThreadLocal<EvaluationScope> evaluationScope = new ThreadLocal<>();

    public RequirementManager(Kgui plugin, KguiApiProvider apiProvider) {
        if (plugin == null || apiProvider == null) throw new IllegalArgumentException("plugin and apiProvider are required");
        this.plugin = plugin;
        this.apiProvider = apiProvider;
        registerDefaultCheckers();
    }

    private void registerDefaultCheckers() {
        register("permission", (player, data) -> {
            String permission = string(data.get("permission"));
            return permission != null && player.hasPermission(permission);
        }, "has_permission");

        register("permissions", (player, data) -> {
            List<String> permissions = strings(data.get("permissions"));
            Integer minimum = strictInt(data.get("minimum"));
            if (permissions.isEmpty()) return false;
            if (minimum == null) minimum = permissions.size();
            if (minimum < 0 || minimum > permissions.size()) return false;
            int granted = 0;
            for (String permission : permissions) if (player.hasPermission(permission)) granted++;
            return granted >= minimum;
        });

        checkers.register("vault:balance", (player, data) -> {
            Double amount = nonNegativeDouble(data.get("amount"));
            return amount != null && plugin.getHookManager().isVaultEnabled()
                && plugin.getHookManager().getVaultHook().hasBalance(player, amount);
        }, "money", "has_money");

        checkers.register("playerpoints:balance", (player, data) -> {
            Integer amount = nonNegativeInt(data.get("amount"));
            return amount != null && plugin.getHookManager().isPlayerPointsEnabled()
                && plugin.getHookManager().getPlayerPointsHook().hasPoints(player, amount);
        }, "points", "has_points");

        register("world", (player, data) -> {
            String world = string(data.get("world"));
            return world != null && player.getWorld().getName().equalsIgnoreCase(world);
        }, "in_world");

        checkers.register("worldguard:region", (player, data) -> {
            String region = string(data.get("region"));
            return region != null && plugin.getHookManager().isWorldGuardEnabled()
                && plugin.getHookManager().getWorldGuardHook().isInRegion(player, region);
        }, "region", "in_region");

        // Alias V1 uniquement: la decision reste fournie par une requirement Kfaction typee.
        delegateAliases("kfaction:has_faction", "has_faction", "in_faction");
        delegateAliases("kfaction:role", "faction_role", "kfaction_role");
        delegateAliases("kfaction:power", "faction_power");

        register("placeholder_equals", (player, data) -> {
            String source = string(data.get("placeholder"));
            String expected = string(data.get("value"));
            String parsed = parseStrict(player, source);
            return parsed != null && expected != null && parsed.equalsIgnoreCase(expected);
        });

        register("placeholder_contains", (player, data) -> {
            String source = string(data.get("placeholder"));
            String expected = string(data.get("value"));
            String parsed = parseStrict(player, source);
            return parsed != null && expected != null
                && parsed.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
        });

        register("placeholder_number", (player, data) -> {
            String parsed = parseStrict(player, string(data.get("placeholder")));
            Double actual = strictDouble(parsed);
            Double expected = strictDouble(data.get("value"));
            String operator = string(data.get("operator"));
            return actual != null && expected != null && compareNumbers(actual, operator, expected);
        });

        register("placeholder_regex", (player, data) -> {
            String parsed = parseStrict(player, string(data.get("placeholder")));
            String regex = string(data.get("regex"));
            return parsed != null && regex != null && SafeRegex.matches(parsed, regex);
        }, "regex");

        checkers.register("combattagplus:not_in_combat", (player, data) ->
            plugin.getHookManager().isCombatTagEnabled()
                && !plugin.getHookManager().getCombatTagHook().isInCombat(player), "not_in_combat");

        register("level", (player, data) -> {
            Integer level = nonNegativeInt(first(data, "level", "amount"));
            return level != null && player.getLevel() >= level;
        }, "experience_level");

        register("experience", (player, data) -> {
            Integer experience = nonNegativeInt(first(data, "experience", "exp", "amount"));
            return experience != null && player.getTotalExperience() >= experience;
        }, "exp");

        register("gamemode", (player, data) -> {
            String configured = string(data.get("gamemode"));
            if (configured == null) return false;
            try {
                return player.getGameMode() == GameMode.valueOf(configured.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        });

        register("op", (player, data) -> player.isOp(), "is_op");

        register("string_equals", (player, data) -> {
            String input = parseStrict(player, string(data.get("input")));
            String output = parseStrict(player, string(data.get("output")));
            Boolean ignoreCase = strictBoolean(data.get("ignore_case"), true);
            return input != null && output != null && ignoreCase != null
                && (ignoreCase ? input.equalsIgnoreCase(output) : input.equals(output));
        });

        register("expression", (player, data) -> {
            String parsed = parseStrict(player, string(data.get("expression")));
            if (parsed == null) return false;
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null) parsed = plugin.getGuiManager().replaceViewportPlaceholders(player, parsed, currentMenu);
            return !hasUnresolvedPlaceholder(parsed) && expressions.evaluate(parsed);
        }, "javascript");

        register("item", (player, data) -> {
            String materialName = string(first(data, "material", "item"));
            Integer amount = nonNegativeInt(data.get("amount"));
            if (materialName == null) return false;
            if (amount == null) amount = 1;
            Material material = Material.matchMaterial(materialName);
            if (material == null || amount < 1) return false;
            Integer itemData = strictInt(data.get("data"));
            ItemStack expected = new ItemStack(material, amount, itemData == null ? (short) 0 : itemData.shortValue());
            return itemData == null ? player.getInventory().contains(material, amount)
                : player.getInventory().containsAtLeast(expected, amount);
        }, "has_item");

        register("distance", (player, data) -> {
            String worldName = string(data.get("world"));
            Double x = strictDouble(data.get("x"));
            Double y = strictDouble(data.get("y"));
            Double z = strictDouble(data.get("z"));
            Double maximum = nonNegativeDouble(first(data, "max_distance", "distance"));
            if (worldName == null || x == null || y == null || z == null || maximum == null
                    || !player.getWorld().getName().equalsIgnoreCase(worldName)) return false;
            Location target = new Location(player.getWorld(), x, y, z);
            return player.getLocation().distanceSquared(target) <= maximum * maximum;
        }, "location");

        register("value_type", (player, data) -> {
            String value = parseStrict(player, string(data.get("value")));
            String expected = string(first(data, "expected", "value_type"));
            return value != null && expected != null && matchesType(value, expected);
        }, "type");
    }

    public boolean checkRequirements(Player player, List<Map<String, Object>> requirements, boolean sendMessage) {
        String currentMenu = player == null ? null : plugin.getGuiManager().getPlayerCurrentMenu(player);
        return checkRequirements(player, requirements, sendMessage, currentMenu, null);
    }

    public boolean checkRequirements(Player player, List<Map<String, Object>> requirements, boolean sendMessage,
                                     String menuId, String itemId) {
        EvaluationScope previous = evaluationScope.get();
        evaluationScope.set(new EvaluationScope(menuId, itemId));
        try {
            return checkRequirementsNow(player, requirements, sendMessage);
        } finally {
            if (previous == null) evaluationScope.remove(); else evaluationScope.set(previous);
        }
    }

    private boolean checkRequirementsNow(Player player, List<Map<String, Object>> requirements, boolean sendMessage) {
        if (requirements == null || requirements.isEmpty()) return true;
        if (player == null) return false;

        for (Map<String, Object> requirement : requirements) {
            if (requirement == null) return deny(player, Collections.<String, Object>emptyMap(), sendMessage, null);
            String requestedType = string(requirement.get("type"));
            if (requestedType == null) {
                plugin.getLogger().warning("Denied requirement without a type");
                return deny(player, requirement, sendMessage, null);
            }

            String resolvedType = delegatedAliases.getOrDefault(
                requestedType.toLowerCase(Locale.ROOT), requestedType);
            RequirementChecker checker = checkers.get(resolvedType);
            Boolean allowed;
            boolean validEvaluation;
            String messageKey = null;
            if (checker != null) {
                String canonical = checkers.canonicalId(resolvedType);
                validEvaluation = isWellFormed(player, canonical, requirement);
                try {
                    allowed = validEvaluation ? checker.check(player, requirement) : false;
                } catch (RuntimeException error) {
                    plugin.getLogger().warning("Requirement failed closed [" + requestedType + "]: " + error.getMessage());
                    allowed = false;
                    validEvaluation = false;
                }
            } else {
                RequirementResult result = executeRegisteredRequirement(resolvedType, player, requirement);
                validEvaluation = result != null;
                allowed = result != null && result.isAllowed();
                if (result != null) messageKey = result.getMessageKey();
            }

            Boolean inverted = strictBoolean(first(requirement, "invert", "negate"), false);
            allowed = RequirementDecision.resolve(allowed, inverted, validEvaluation);
            if (!allowed) return deny(player, requirement, sendMessage, messageKey);
        }
        return true;
    }

    public void registerChecker(String type, RequirementChecker checker) {
        checkers.register(type, checker);
    }

    public String canonicalType(String type) {
        if (type == null) return null;
        String delegated = delegatedAliases.get(type.toLowerCase(Locale.ROOT));
        return delegated == null ? checkers.canonicalId(type) : delegated;
    }

    private void register(String id, RequirementChecker checker, String... aliases) {
        checkers.register("kgui:" + id, checker, aliases);
    }

    private void delegateAliases(String extensionId, String... aliases) {
        for (String alias : aliases) delegatedAliases.put(alias.toLowerCase(Locale.ROOT), extensionId);
    }

    private boolean isWellFormed(Player player, String canonical, Map<String, Object> data) {
        if (canonical == null) return false;
        switch (canonical) {
            case "kgui:permission":
                return string(data.get("permission")) != null;
            case "kgui:permissions": {
                List<String> permissions = strings(data.get("permissions"));
                Integer minimum = strictInt(data.get("minimum"));
                return !permissions.isEmpty() && (minimum == null || minimum >= 0 && minimum <= permissions.size());
            }
            case "vault:balance":
                return plugin.getHookManager().isVaultEnabled() && nonNegativeDouble(data.get("amount")) != null;
            case "playerpoints:balance":
                return plugin.getHookManager().isPlayerPointsEnabled() && nonNegativeInt(data.get("amount")) != null;
            case "kgui:world":
                return string(data.get("world")) != null;
            case "worldguard:region":
                return plugin.getHookManager().isWorldGuardEnabled() && string(data.get("region")) != null;
            case "combattagplus:not_in_combat":
                return plugin.getHookManager().isCombatTagEnabled();
            case "kgui:placeholder_equals":
            case "kgui:placeholder_contains":
                return parseStrict(player, string(data.get("placeholder"))) != null && string(data.get("value")) != null;
            case "kgui:placeholder_number":
                return strictDouble(parseStrict(player, string(data.get("placeholder")))) != null
                    && strictDouble(data.get("value")) != null && validOperator(string(data.get("operator")));
            case "kgui:placeholder_regex": {
                String input = parseStrict(player, string(data.get("placeholder")));
                String pattern = string(data.get("regex"));
                return input != null && input.length() <= 2048 && SafeRegex.isValidPattern(pattern);
            }
            case "kgui:level":
                return nonNegativeInt(first(data, "level", "amount")) != null;
            case "kgui:experience":
                return nonNegativeInt(first(data, "experience", "exp", "amount")) != null;
            case "kgui:gamemode": {
                String value = string(data.get("gamemode"));
                if (value == null) return false;
                try { GameMode.valueOf(value.toUpperCase(Locale.ROOT)); return true; }
                catch (IllegalArgumentException ignored) { return false; }
            }
            case "kgui:op":
                return true;
            case "kgui:string_equals":
                return parseStrict(player, string(data.get("input"))) != null
                    && parseStrict(player, string(data.get("output"))) != null
                    && strictBoolean(data.get("ignore_case"), true) != null;
            case "kgui:expression": {
                String expression = parseStrict(player, string(data.get("expression")));
                if (expression == null) return false;
                String menuId = plugin.getGuiManager().getPlayerCurrentMenu(player);
                if (menuId != null) expression = plugin.getGuiManager().replaceViewportPlaceholders(player, expression, menuId);
                return !hasUnresolvedPlaceholder(expression) && expressions.evaluateResult(expression) != null;
            }
            case "kgui:item": {
                String materialName = string(first(data, "material", "item"));
                if (materialName == null || Material.matchMaterial(materialName) == null) return false;
                Integer amount = data.containsKey("amount") ? strictInt(data.get("amount")) : Integer.valueOf(1);
                Integer itemData = data.containsKey("data") ? strictInt(data.get("data")) : Integer.valueOf(0);
                return amount != null && amount > 0 && itemData != null;
            }
            case "kgui:distance": {
                Double maximum = nonNegativeDouble(first(data, "max_distance", "distance"));
                return string(data.get("world")) != null && strictDouble(data.get("x")) != null
                    && strictDouble(data.get("y")) != null && strictDouble(data.get("z")) != null
                    && maximum != null && maximum <= 60000000D;
            }
            case "kgui:value_type": {
                String value = parseStrict(player, string(data.get("value")));
                String expected = string(first(data, "expected", "value_type"));
                return value != null && isKnownValueType(expected);
            }
            default:
                // Les checkers ajoutes explicitement par le coeur sont consideres responsables de leur schema.
                return true;
        }
    }

    private static boolean validOperator(String operator) {
        if (operator == null) return false;
        switch (operator.toLowerCase(Locale.ROOT)) {
            case ">=": case "gte": case ">": case "gt": case "<=": case "lte":
            case "<": case "lt": case "==": case "=": case "equals": case "!=": case "not_equals":
                return true;
            default:
                return false;
        }
    }

    private static boolean isKnownValueType(String expected) {
        if (expected == null) return false;
        switch (expected.toLowerCase(Locale.ROOT)) {
            case "int": case "integer": case "double": case "number": case "uuid":
            case "player": case "boolean": case "string":
                return true;
            default:
                return false;
        }
    }

    private RequirementResult executeRegisteredRequirement(String id, Player player, Map<String, Object> data) {
        RequirementHandler handler;
        try {
            handler = apiProvider.findRequirement(id);
        } catch (IllegalArgumentException error) {
            plugin.getLogger().warning("Invalid requirement id denied: " + id);
            return null;
        }
        if (handler == null) {
            plugin.getLogger().warning("Unknown or unavailable requirement denied: " + id);
            return null;
        }

        Map<String, String> parameters = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && isScalar(entry.getValue())) {
                parameters.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        EvaluationScope scope = evaluationScope.get();
        String menuId = scope == null ? plugin.getGuiManager().getPlayerCurrentMenu(player) : scope.menuId;
        if (menuId == null || menuId.trim().isEmpty()) menuId = "unknown";
        String itemId = scope == null ? string(data.get("id")) : scope.itemId;
        try {
            RequirementResult result = handler.evaluate(new RequirementContext(
                player.getUniqueId(), menuId, itemId, new MenuArguments(parameters)));
            if (result == null) plugin.getLogger().warning("Requirement returned null and was denied: " + id);
            return result;
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Registered requirement failed closed [" + id + "]: " + error.getMessage());
            return null;
        }
    }

    private boolean deny(Player player, Map<String, Object> requirement, boolean sendMessage, String messageKey) {
        if (!sendMessage) return false;
        if (messageKey != null) {
            plugin.getMessageManager().send(player, messageKey);
            return false;
        }
        String denyMessage = string(requirement.get("deny_message"));
        if (denyMessage != null) {
            player.sendMessage(plugin.getGuiManager().parsePlaceholders(player,
                plugin.getMessageManager().getPrefix() + denyMessage));
            return false;
        }
        plugin.getMessageManager().send(player, "requirement-placeholder-check-failed");
        return false;
    }

    private String parseStrict(Player player, String source) {
        if (source == null) return null;
        String parsed = plugin.getGuiManager().parsePlaceholders(player, source);
        return hasUnresolvedPlaceholder(parsed) ? null : parsed;
    }

    private static boolean hasUnresolvedPlaceholder(String value) {
        if (value == null) return true;
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("{kgui_data_") || lower.contains("{input")) return true;
        int firstPercent = value.indexOf('%');
        return firstPercent >= 0 && value.indexOf('%', firstPercent + 1) > firstPercent;
    }

    private static boolean compareNumbers(double actual, String operator, double expected) {
        if (operator == null) return false;
        switch (operator.toLowerCase(Locale.ROOT)) {
            case ">=": case "gte": return actual >= expected;
            case ">": case "gt": return actual > expected;
            case "<=": case "lte": return actual <= expected;
            case "<": case "lt": return actual < expected;
            case "==": case "=": case "equals": return Double.compare(actual, expected) == 0;
            case "!=": case "not_equals": return Double.compare(actual, expected) != 0;
            default: return false;
        }
    }

    private static boolean matchesType(String value, String expected) {
        switch (expected.toLowerCase(Locale.ROOT)) {
            case "int": case "integer": return strictInt(value) != null;
            case "double": case "number": return strictDouble(value) != null;
            case "uuid":
                try { UUID.fromString(value); return true; }
                catch (IllegalArgumentException ignored) { return false; }
            case "player": return Bukkit.getPlayerExact(value) != null;
            case "boolean": return strictBoolean(value, null) != null;
            case "string": return !value.isEmpty();
            default: return false;
        }
    }

    private static Object first(Map<String, Object> data, String... keys) {
        for (String key : keys) if (data.containsKey(key)) return data.get(key);
        return null;
    }

    private static String string(Object value) {
        if (value == null || value instanceof Map || value instanceof List) return null;
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object entry : (List<?>) value) {
            String text = string(entry);
            if (text == null) return Collections.emptyList();
            result.add(text);
        }
        return result;
    }

    private static Double strictDouble(Object value) {
        if (value == null) return null;
        try {
            double result = value instanceof Number ? ((Number) value).doubleValue()
                : Double.parseDouble(String.valueOf(value).trim());
            return Double.isFinite(result) ? result : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double nonNegativeDouble(Object value) {
        Double result = strictDouble(value);
        return result != null && result >= 0D ? result : null;
    }

    private static Integer strictInt(Object value) {
        if (value == null) return null;
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) return ((Number) value).intValue();
        if (value instanceof Long) {
            long result = (Long) value;
            return result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE ? (int) result : null;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer nonNegativeInt(Object value) {
        Integer result = strictInt(value);
        return result != null && result >= 0 ? result : null;
    }

    private static Boolean strictBoolean(Object value, Boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        return null;
    }

    private static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character;
    }

    @FunctionalInterface
    public interface RequirementChecker {
        boolean check(Player player, Map<String, Object> data);
    }

    private static final class EvaluationScope {
        private final String menuId;
        private final String itemId;

        private EvaluationScope(String menuId, String itemId) {
            this.menuId = menuId;
            this.itemId = itemId;
        }
    }
}
