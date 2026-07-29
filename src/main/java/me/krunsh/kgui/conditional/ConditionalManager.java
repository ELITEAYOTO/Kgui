package me.krunsh.kgui.conditional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuItem;

/**
 * Gestionnaire des conditions pour l'affichage dynamique des items
 * Permet d'afficher/masquer des items selon des conditions
 */
public class ConditionalManager {

    private final Kgui plugin;

    public ConditionalManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Types de conditions supportées
     */
    public enum ConditionType {
        HAS_PERMISSION,         // Joueur a la permission
        NO_PERMISSION,          // Joueur n'a PAS la permission
        HAS_MONEY,              // Joueur a assez d'argent
        HAS_POINTS,             // Joueur a assez de points
        HAS_ITEM,               // Joueur a l'item dans son inventaire
        PLACEHOLDER_EQUALS,     // Placeholder == valeur
        PLACEHOLDER_NOT_EQUALS, // Placeholder != valeur
        PLACEHOLDER_GREATER,    // Placeholder > valeur
        PLACEHOLDER_LESS,       // Placeholder < valeur
        PLACEHOLDER_CONTAINS,   // Placeholder contient valeur
        IN_REGION,              // Joueur dans une région WorldGuard
        IN_WORLD,               // Joueur dans un monde
        IN_FACTION,             // Joueur a une faction
        FACTION_ROLE,           // Joueur a un rôle de faction
        IS_ONLINE,              // Un joueur est en ligne
        JAVASCRIPT              // Condition JavaScript (avancé)
    }

    /**
     * Structure d'une condition
     */
    public static class Condition {
        private ConditionType type;
        private String value;
        private String placeholder;
        private String compareValue;
        private boolean negate = false;
        
        public ConditionType getType() { return type; }
        public void setType(ConditionType type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getPlaceholder() { return placeholder; }
        public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
        public String getCompareValue() { return compareValue; }
        public void setCompareValue(String compareValue) { this.compareValue = compareValue; }
        public boolean isNegate() { return negate; }
        public void setNegate(boolean negate) { this.negate = negate; }
    }

    /**
     * Parse une condition depuis une map YAML
     */
    public Condition parseCondition(Map<String, Object> config) {
        Condition condition = new Condition();
        
        String typeStr = (String) config.get("type");
        if (typeStr == null) return null;
        
        try {
            condition.setType(ConditionType.valueOf(typeStr.toUpperCase().replace("-", "_")));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown condition type: " + typeStr);
            return null;
        }
        
        condition.setValue((String) config.get("value"));
        condition.setPlaceholder((String) config.get("placeholder"));
        condition.setCompareValue((String) config.get("compare"));
        
        Object negateObj = config.get("negate");
        if (negateObj instanceof Boolean) {
            condition.setNegate((Boolean) negateObj);
        }
        
        return condition;
    }

    /**
     * Évalue une condition pour un joueur
     */
    public boolean evaluate(Player player, Condition condition) {
        if (condition == null) return true;
        
        boolean result = evaluateInternal(player, condition);
        return condition.isNegate() ? !result : result;
    }

    /**
     * Évalue une liste de conditions (toutes doivent être vraies)
     */
    public boolean evaluateAll(Player player, List<Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        
        for (Condition condition : conditions) {
            if (!evaluate(player, condition)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Évalue une liste de conditions (au moins une doit être vraie)
     */
    public boolean evaluateAny(Player player, List<Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        
        for (Condition condition : conditions) {
            if (evaluate(player, condition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Évaluation interne d'une condition
     */
    private boolean evaluateInternal(Player player, Condition condition) {
        switch (condition.getType()) {
            case HAS_PERMISSION:
                return player.hasPermission(condition.getValue());
                
            case NO_PERMISSION:
                return !player.hasPermission(condition.getValue());
                
            case HAS_MONEY:
                return evaluateMoney(player, condition.getValue());
                
            case HAS_POINTS:
                return evaluatePoints(player, condition.getValue());
                
            case HAS_ITEM:
                return evaluateHasItem(player, condition.getValue());
                
            case PLACEHOLDER_EQUALS:
                return evaluatePlaceholderEquals(player, condition);
                
            case PLACEHOLDER_NOT_EQUALS:
                return !evaluatePlaceholderEquals(player, condition);
                
            case PLACEHOLDER_GREATER:
                return evaluatePlaceholderComparison(player, condition, true);
                
            case PLACEHOLDER_LESS:
                return evaluatePlaceholderComparison(player, condition, false);
                
            case PLACEHOLDER_CONTAINS:
                return evaluatePlaceholderContains(player, condition);
                
            case IN_REGION:
                return evaluateInRegion(player, condition.getValue());
                
            case IN_WORLD:
                return player.getWorld().getName().equalsIgnoreCase(condition.getValue());
                
            case IN_FACTION:
                return evaluateInFaction(player);
                
            case FACTION_ROLE:
                return evaluateFactionRole(player, condition.getValue());
                
            case IS_ONLINE:
                return org.bukkit.Bukkit.getPlayerExact(condition.getValue()) != null;
                
            default:
                return true;
        }
    }

    /**
     * Évalue si le joueur a assez d'argent
     */
    private boolean evaluateMoney(Player player, String value) {
        if (!plugin.getHookManager().isVaultEnabled()) return false;
        
        try {
            double required = Double.parseDouble(value);
            return plugin.getHookManager().getVaultHook().hasBalance(player, required);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Évalue si le joueur a assez de points
     */
    private boolean evaluatePoints(Player player, String value) {
        if (!plugin.getHookManager().isPlayerPointsEnabled()) return false;
        
        try {
            int required = Integer.parseInt(value);
            return plugin.getHookManager().getPlayerPointsHook().hasPoints(player, required);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Évalue si le joueur a un item (format: MATERIAL:amount ou MATERIAL)
     */
    private boolean evaluateHasItem(Player player, String value) {
        if (value == null) return false;
        
        String[] parts = value.split(":");
        String materialName = parts[0].toUpperCase();
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
            return player.getInventory().containsAtLeast(
                new org.bukkit.inventory.ItemStack(material), amount
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Évalue l'égalité d'un placeholder
     */
    private boolean evaluatePlaceholderEquals(Player player, Condition condition) {
        String placeholder = condition.getPlaceholder();
        String expected = condition.getCompareValue();
        
        if (placeholder == null || expected == null) return false;
        
        String actual = plugin.getHookManager().getPlaceholderAPIHook()
                             .setPlaceholders(player, placeholder);
        
        return actual.equalsIgnoreCase(expected);
    }

    /**
     * Évalue une comparaison numérique de placeholder
     */
    private boolean evaluatePlaceholderComparison(Player player, Condition condition, boolean greater) {
        String placeholder = condition.getPlaceholder();
        String expected = condition.getCompareValue();
        
        if (placeholder == null || expected == null) return false;
        
        String actual = plugin.getHookManager().getPlaceholderAPIHook()
                             .setPlaceholders(player, placeholder);
        
        try {
            double actualValue = Double.parseDouble(actual.replaceAll("[^0-9.-]", ""));
            double expectedValue = Double.parseDouble(expected.replaceAll("[^0-9.-]", ""));
            
            return greater ? actualValue > expectedValue : actualValue < expectedValue;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Évalue si un placeholder contient une valeur
     */
    private boolean evaluatePlaceholderContains(Player player, Condition condition) {
        String placeholder = condition.getPlaceholder();
        String expected = condition.getCompareValue();
        
        if (placeholder == null || expected == null) return false;
        
        String actual = plugin.getHookManager().getPlaceholderAPIHook()
                             .setPlaceholders(player, placeholder);
        
        return actual.toLowerCase().contains(expected.toLowerCase());
    }

    /**
     * Évalue si le joueur est dans une région WorldGuard
     */
    private boolean evaluateInRegion(Player player, String regionId) {
        if (!plugin.getHookManager().isWorldGuardEnabled()) return false;
        
        Set<String> regions = plugin.getHookManager().getWorldGuardHook()
                                   .getRegions(player.getLocation());
        return regions.contains(regionId.toLowerCase());
    }

    /**
     * Évalue si le joueur a une faction
     */
    private boolean evaluateInFaction(Player player) {
        if (!plugin.getHookManager().isKfactionEnabled()) return false;
        return plugin.getHookManager().getKfactionHook().hasFaction(player);
    }

    /**
     * Évalue le rôle faction du joueur
     */
    private boolean evaluateFactionRole(Player player, String role) {
        if (!plugin.getHookManager().isKfactionEnabled()) return false;
        
        String playerRole = plugin.getHookManager().getKfactionHook().getRole(player);
        return role.equalsIgnoreCase(playerRole);
    }

    /**
     * Parse les conditions de view d'un item
     */
    public List<Condition> parseViewConditions(List<Map<String, Object>> configList) {
        if (configList == null) return Collections.emptyList();
        
        List<Condition> conditions = new ArrayList<>();
        for (Map<String, Object> config : configList) {
            Condition condition = parseCondition(config);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        return conditions;
    }

    /**
     * Détermine si un item doit être affiché pour un joueur
     */
    public boolean shouldDisplay(Player player, MenuItem item) {
        List<Map<String, Object>> viewReqs = item.getViewRequirements();
        if (viewReqs == null || viewReqs.isEmpty()) return true;
        
        List<Condition> conditions = parseViewConditions(viewReqs);
        return evaluateAll(player, conditions);
    }
}
