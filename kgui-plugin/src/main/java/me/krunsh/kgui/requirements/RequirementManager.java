package me.krunsh.kgui.requirements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;

/**
 * Gestionnaire des requirements
 * Vérifie les conditions pour ouvrir/cliquer dans les menus
 */
public class RequirementManager {

    private final Kgui plugin;
    private final Map<String, RequirementChecker> checkers = new HashMap<>();

    public RequirementManager(Kgui plugin) {
        this.plugin = plugin;
        registerDefaultCheckers();
    }

    /**
     * Enregistre les checkers par défaut
     */
    private void registerDefaultCheckers() {
        // Permission
        checkers.put("permission", (player, data) -> {
            String permission = (String) data.get("permission");
            return permission == null || player.hasPermission(permission);
        });
        
        // Has Permission (alias)
        checkers.put("has_permission", checkers.get("permission"));
        
        // Money (Vault)
        checkers.put("money", (player, data) -> {
            if (!plugin.getHookManager().isVaultEnabled()) return true;
            
            double amount = getDouble(data.get("amount"), 0);
            return plugin.getHookManager().getVaultHook().hasBalance(player, amount);
        });
        
        // Has Money (alias)
        checkers.put("has_money", checkers.get("money"));
        
        // Points (PlayerPoints)
        checkers.put("points", (player, data) -> {
            if (!plugin.getHookManager().isPlayerPointsEnabled()) return true;
            
            int amount = getInt(data.get("amount"), 0);
            return plugin.getHookManager().getPlayerPointsHook().hasPoints(player, amount);
        });
        
        // Has Points (alias)
        checkers.put("has_points", checkers.get("points"));
        
        // World
        checkers.put("world", (player, data) -> {
            String world = (String) data.get("world");
            if (world == null) return true;
            return player.getWorld().getName().equalsIgnoreCase(world);
        });
        
        // In World (alias)
        checkers.put("in_world", checkers.get("world"));
        
        // Region (WorldGuard)
        checkers.put("region", (player, data) -> {
            if (!plugin.getHookManager().isWorldGuardEnabled()) return true;
            
            String region = (String) data.get("region");
            if (region == null) return true;
            return plugin.getHookManager().getWorldGuardHook().isInRegion(player, region);
        });
        
        // In Region (alias)
        checkers.put("in_region", checkers.get("region"));
        
        // Faction (Kfaction)
        checkers.put("has_faction", (player, data) -> {
            if (!plugin.getHookManager().isKfactionEnabled()) return true;
            return plugin.getHookManager().getKfactionHook().hasFaction(player);
        });
        
        // In Faction (alias)
        checkers.put("in_faction", checkers.get("has_faction"));
        
        // Faction Role (Kfaction)
        checkers.put("faction_role", (player, data) -> {
            if (!plugin.getHookManager().isKfactionEnabled()) return true;
            
            String role = (String) data.get("role");
            if (role == null) return true;
            return plugin.getHookManager().getKfactionHook().hasMinRole(player, role);
        });
        
        // Kfaction Role (alias)
        checkers.put("kfaction_role", checkers.get("faction_role"));
        
        // Faction Power (Kfaction)
        checkers.put("faction_power", (player, data) -> {
            if (!plugin.getHookManager().isKfactionEnabled()) return true;
            
            double power = getDouble(data.get("power"), 0);
            return plugin.getHookManager().getKfactionHook().getFactionPower(player) >= power;
        });
        
        // Placeholder Equals
        checkers.put("placeholder_equals", (player, data) -> {
            String placeholder = (String) data.get("placeholder");
            String value = (String) data.get("value");
            if (placeholder == null || value == null) return true;
            
            String parsed = plugin.getGuiManager().parsePlaceholders(player, placeholder);
            return parsed.equalsIgnoreCase(value);
        });
        
        // Placeholder Contains
        checkers.put("placeholder_contains", (player, data) -> {
            String placeholder = (String) data.get("placeholder");
            String value = (String) data.get("value");
            if (placeholder == null || value == null) return true;
            
            String parsed = plugin.getGuiManager().parsePlaceholders(player, placeholder);
            return parsed.toLowerCase().contains(value.toLowerCase());
        });
        
        // Placeholder Number Comparison
        checkers.put("placeholder_number", (player, data) -> {
            String placeholder = (String) data.get("placeholder");
            String operator = (String) data.get("operator");
            double compareValue = getDouble(data.get("value"), 0);
            if (placeholder == null) return true;
            
            String parsed = plugin.getGuiManager().parsePlaceholders(player, placeholder);
            try {
                double value = Double.parseDouble(parsed.replaceAll("[^0-9.-]", ""));
                return compareNumbers(value, operator, compareValue);
            } catch (NumberFormatException e) {
                return false;
            }
        });
        
        // Combat Tag
        checkers.put("not_in_combat", (player, data) -> {
            if (!plugin.getHookManager().isCombatTagEnabled()) return true;
            return !plugin.getHookManager().getCombatTagHook().isInCombat(player);
        });
        
        // Level (Experience Level)
        checkers.put("level", (player, data) -> {
            int level = getInt(data.get("level"), 0);
            return player.getLevel() >= level;
        });
        
        // Exp (Total Experience)
        checkers.put("exp", (player, data) -> {
            int exp = getInt(data.get("exp"), 0);
            return player.getTotalExperience() >= exp;
        });
        
        // Gamemode
        checkers.put("gamemode", (player, data) -> {
            String gamemode = (String) data.get("gamemode");
            if (gamemode == null) return true;
            return player.getGameMode().name().equalsIgnoreCase(gamemode);
        });
        
        // Is Op
        checkers.put("is_op", (player, data) -> player.isOp());
        
        // String Equals (for compare requirements)
        checkers.put("string_equals", (player, data) -> {
            String input = (String) data.get("input");
            String output = (String) data.get("output");
            if (input == null || output == null) return true;
            
            input = plugin.getGuiManager().parsePlaceholders(player, input);
            output = plugin.getGuiManager().parsePlaceholders(player, output);
            
            boolean ignoreCase = (boolean) data.getOrDefault("ignore_case", true);
            return ignoreCase ? input.equalsIgnoreCase(output) : input.equals(output);
        });
        
        // Expression - Évalue des expressions conditionnelles simples
        // Formats supportés: "%page% > 1", "%vault_eco_balance% >= 100", etc.
        checkers.put("expression", (player, data) -> {
            String expression = (String) data.get("expression");
            if (expression == null || expression.isEmpty()) return true;
            
            // Parser les placeholders dans l'expression
            expression = plugin.getGuiManager().parsePlaceholders(player, expression);
            
            // Parser les placeholders de pagination aussi
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null) {
                expression = plugin.getPaginationManager().replacePlaceholders(expression, player, currentMenu);
            }
            
            return evaluateSimpleExpression(expression);
        });
        
        // JavaScript - Alias pour expression (compatibilité)
        checkers.put("javascript", checkers.get("expression"));
    }

    /**
     * Compare deux nombres
     */
    private boolean compareNumbers(double value, String operator, double compareValue) {
        if (operator == null) operator = ">=";
        
        switch (operator.toLowerCase()) {
            case ">=":
            case "gte":
                return value >= compareValue;
            case ">":
            case "gt":
                return value > compareValue;
            case "<=":
            case "lte":
                return value <= compareValue;
            case "<":
            case "lt":
                return value < compareValue;
            case "==":
            case "=":
            case "equals":
                return value == compareValue;
            case "!=":
            case "not_equals":
                return value != compareValue;
            default:
                return value >= compareValue;
        }
    }

    /**
     * Évalue une expression conditionnelle simple
     * Formats supportés:
     *   - "5 > 1" -> true
     *   - "10 >= 10" -> true
     *   - "3 < 5" -> true
     *   - "5 == 5" -> true
     *   - "5 != 3" -> true
     *   - "hello equals hello" -> true (comparaison de strings)
     *   - "hello contains ell" -> true
     */
    private boolean evaluateSimpleExpression(String expression) {
        if (expression == null || expression.isEmpty()) return true;
        
        // Opérateurs supportés (ordre important : >= avant >)
        String[][] operators = {
            {">=", "gte"},
            {"<=", "lte"},
            {"!=", "not_equals"},
            {"==", "equals_num"},
            {"=", "equals_num"},
            {">", "gt"},
            {"<", "lt"},
            {" equals ", "equals_str"},
            {" contains ", "contains"}
        };
        
        for (String[] opPair : operators) {
            String op = opPair[0];
            String opType = opPair[1];
            
            int opIndex = expression.indexOf(op);
            if (opIndex > 0) {
                String left = expression.substring(0, opIndex).trim();
                String right = expression.substring(opIndex + op.length()).trim();
                
                // Comparaison de strings
                if (opType.equals("equals_str")) {
                    return left.equalsIgnoreCase(right);
                }
                if (opType.equals("contains")) {
                    return left.toLowerCase().contains(right.toLowerCase());
                }
                
                // Comparaison numérique
                try {
                    double leftNum = parseNumber(left);
                    double rightNum = parseNumber(right);
                    
                    switch (opType) {
                        case "gte": return leftNum >= rightNum;
                        case "lte": return leftNum <= rightNum;
                        case "gt": return leftNum > rightNum;
                        case "lt": return leftNum < rightNum;
                        case "equals_num": return leftNum == rightNum;
                        case "not_equals": return leftNum != rightNum;
                    }
                } catch (NumberFormatException e) {
                    // Si parsing échoue, comparer comme strings
                    if (opType.equals("equals_num")) {
                        return left.equalsIgnoreCase(right);
                    }
                    if (opType.equals("not_equals")) {
                        return !left.equalsIgnoreCase(right);
                    }
                    return false;
                }
            }
        }
        
        // Pas d'opérateur trouvé - évaluer comme boolean
        return "true".equalsIgnoreCase(expression.trim()) || 
               "yes".equalsIgnoreCase(expression.trim()) ||
               "1".equals(expression.trim());
    }

    /**
     * Parse un nombre depuis une string, en nettoyant les caractères non numériques
     */
    private double parseNumber(String str) throws NumberFormatException {
        // Nettoyer les caractères non-numériques sauf . et -
        String cleaned = str.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty()) throw new NumberFormatException("Empty number: " + str);
        return Double.parseDouble(cleaned);
    }

    /**
     * Vérifie une liste de requirements
     */
    public boolean checkRequirements(Player player, List<Map<String, Object>> requirements, boolean sendMessage) {
        if (requirements == null || requirements.isEmpty()) return true;
        
        for (Map<String, Object> requirement : requirements) {
            String type = (String) requirement.get("type");
            if (type == null) continue;
            
            RequirementChecker checker = checkers.get(type.toLowerCase());
            if (checker == null) {
                plugin.getLogger().warning("Unknown requirement type: " + type);
                continue;
            }
            
            if (!checker.check(player, requirement)) {
                if (sendMessage) {
                    sendDenyMessage(player, requirement);
                }
                return false;
            }
        }
        
        return true;
    }

    /**
     * Envoie le message de refus
     */
    private void sendDenyMessage(Player player, Map<String, Object> requirement) {
        String denyMessage = (String) requirement.get("deny_message");
        if (denyMessage != null) {
            player.sendMessage(plugin.getGuiManager().parsePlaceholders(player, 
                plugin.getMessageManager().getPrefix() + denyMessage));
            return;
        }
        
        // Messages par défaut selon le type
        String type = (String) requirement.get("type");
        switch (type.toLowerCase()) {
            case "permission":
            case "has_permission":
                String perm = (String) requirement.get("permission");
                plugin.getMessageManager().send(player, "requirement-no-permission", "permission", perm != null ? perm : "unknown");
                break;
            case "money":
            case "has_money":
                double money = getDouble(requirement.get("amount"), 0);
                plugin.getMessageManager().send(player, "requirement-no-money", "amount", String.valueOf(money));
                break;
            case "points":
            case "has_points":
                int points = getInt(requirement.get("amount"), 0);
                plugin.getMessageManager().send(player, "requirement-no-points", "amount", String.valueOf(points));
                break;
            case "has_faction":
                plugin.getMessageManager().send(player, "requirement-faction-required");
                break;
            default:
                plugin.getMessageManager().send(player, "requirement-placeholder-check-failed");
        }
    }

    /**
     * Enregistre un checker personnalisé
     */
    public void registerChecker(String type, RequirementChecker checker) {
        checkers.put(type.toLowerCase(), checker);
    }

    /**
     * Utilitaire pour obtenir un double
     */
    private double getDouble(Object obj, double def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Utilitaire pour obtenir un int
     */
    private int getInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Interface pour les checkers de requirements
     */
    @FunctionalInterface
    public interface RequirementChecker {
        boolean check(Player player, Map<String, Object> data);
    }
}
