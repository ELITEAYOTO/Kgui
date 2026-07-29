package me.krunsh.kgui.hooks;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;

/**
 * Hook pour Kfaction (Plugin Factions custom de l'écosystème K)
 * Utilise reflection pour éviter les dépendances hardcodées
 */
public class KfactionHook {

    private final Kgui plugin;
    
    // Références pour reflection
    private Object kfactionAPI;
    private Object fPlayerManager;
    private Object powerManager;
    
    // Cache des méthodes pour performance (1K joueurs)
    private Method getFPlayerMethod;
    private Method getPlayerFactionMethod;
    private Method hasFactionMethod;
    
    private boolean initialized = false;

    public KfactionHook(Kgui plugin) {
        this.plugin = plugin;
        initialize();
    }

    /**
     * Initialise le hook via reflection
     */
    private void initialize() {
        try {
            Plugin kfaction = Bukkit.getPluginManager().getPlugin("Kfaction");
            if (kfaction == null || !kfaction.isEnabled()) {
                plugin.getLogger().warning("Kfaction not found or not enabled");
                return;
            }
            
            // Obtenir l'API via getInstance().getAPI()
            Class<?> kfactionClass = kfaction.getClass();
            Method getAPIMethod = kfactionClass.getMethod("getAPI");
            this.kfactionAPI = getAPIMethod.invoke(kfaction);
            
            // Obtenir les managers
            Method getFPlayerManagerMethod = kfactionClass.getMethod("getFPlayerManager");
            this.fPlayerManager = getFPlayerManagerMethod.invoke(kfaction);
            
            Method getPowerManagerMethod = kfactionClass.getMethod("getPowerManager");
            this.powerManager = getPowerManagerMethod.invoke(kfaction);
            
            // Cache les méthodes API fréquemment utilisées
            Class<?> apiClass = kfactionAPI.getClass();
            this.hasFactionMethod = apiClass.getMethod("hasFaction", Player.class);
            this.getPlayerFactionMethod = apiClass.getMethod("getPlayerFaction", Player.class);
            
            // Méthode getFPlayer
            Class<?> fPlayerManagerClass = fPlayerManager.getClass();
            this.getFPlayerMethod = fPlayerManagerClass.getMethod("getFPlayer", Player.class);
            
            initialized = true;
            plugin.getLogger().info("KfactionHook initialized successfully");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize KfactionHook: " + e.getMessage());
            if (plugin.getConfigManager().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Vérifie si le hook est initialisé
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Vérifie si le joueur est dans une faction
     */
    public boolean hasFaction(Player player) {
        if (!initialized) return false;
        
        try {
            return (boolean) hasFactionMethod.invoke(kfactionAPI, player);
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("hasFaction error: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Obtient le FPlayer d'un joueur
     */
    public Object getFPlayer(Player player) {
        if (!initialized) return null;
        
        try {
            return getFPlayerMethod.invoke(fPlayerManager, player);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtient la faction d'un joueur
     */
    public Object getPlayerFaction(Player player) {
        if (!initialized) return null;
        
        try {
            return getPlayerFactionMethod.invoke(kfactionAPI, player);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtient le nom de la faction du joueur
     */
    public String getFactionName(Player player) {
        if (!initialized) return "";
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return "";
            
            Method getNameMethod = faction.getClass().getMethod("getName");
            return (String) getNameMethod.invoke(faction);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Obtient le tag de la faction du joueur
     */
    public String getFactionTag(Player player) {
        if (!initialized) return "";
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return "";
            
            Method getTagMethod = faction.getClass().getMethod("getTag");
            return (String) getTagMethod.invoke(faction);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Obtient le rôle du joueur dans sa faction
     */
    public String getRole(Player player) {
        if (!initialized) return "";
        
        try {
            Object fPlayer = getFPlayer(player);
            if (fPlayer == null) return "";
            
            Method getRoleMethod = fPlayer.getClass().getMethod("getRole");
            Object role = getRoleMethod.invoke(fPlayer);
            if (role == null) return "";
            
            return role.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Vérifie si le joueur a au moins un certain rôle
     * Ordre: RECRUIT < MEMBER < MODERATOR < COLEADER < LEADER
     */
    public boolean hasMinRole(Player player, String roleName) {
        if (!initialized || !hasFaction(player)) return false;
        
        try {
            String currentRole = getRole(player);
            if (currentRole.isEmpty()) return false;
            
            int currentRank = getRoleRank(currentRole);
            int requiredRank = getRoleRank(roleName);
            
            return currentRank >= requiredRank;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convertit un rôle en rang numérique
     */
    private int getRoleRank(String role) {
        switch (role.toUpperCase()) {
            case "RECRUIT": return 1;
            case "MEMBER": return 2;
            case "MODERATOR": return 3;
            case "COLEADER": return 4;
            case "LEADER": return 5;
            default: return 0;
        }
    }

    /**
     * Obtient la puissance de la faction du joueur
     */
    public double getFactionPower(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            // Utiliser PowerManager.getFactionPower(faction)
            Method getPowerMethod = powerManager.getClass().getMethod("getFactionPower", faction.getClass());
            return (double) getPowerMethod.invoke(powerManager, faction);
        } catch (Exception e) {
            // Fallback: essayer faction.getPower()
            try {
                Object faction = getPlayerFaction(player);
                if (faction == null) return 0;
                Method getPowerMethod = faction.getClass().getMethod("getPower");
                return (double) getPowerMethod.invoke(faction);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    /**
     * Obtient la puissance maximale de la faction
     */
    public double getFactionMaxPower(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            Method getMaxPowerMethod = powerManager.getClass().getMethod("getFactionMaxPower", faction.getClass());
            return (double) getMaxPowerMethod.invoke(powerManager, faction);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtient le nombre de membres de la faction
     */
    public int getFactionMemberCount(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            Method getMemberCountMethod = faction.getClass().getMethod("getMemberCount");
            return (int) getMemberCountMethod.invoke(faction);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Vérifie si le joueur est leader de sa faction
     */
    public boolean isLeader(Player player) {
        return "LEADER".equalsIgnoreCase(getRole(player));
    }

    /**
     * Vérifie si le joueur est coleader ou supérieur
     */
    public boolean isColeader(Player player) {
        return hasMinRole(player, "COLEADER");
    }

    /**
     * Vérifie si le joueur est modérateur ou supérieur
     */
    public boolean isModerator(Player player) {
        return hasMinRole(player, "MODERATOR");
    }

    /**
     * Obtient le nombre de claims de la faction
     */
    public int getFactionClaims(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            Method getClaimCountMethod = faction.getClass().getMethod("getClaimCount");
            return (int) getClaimCountMethod.invoke(faction);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtient le solde de la banque de la faction
     */
    public double getFactionBank(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            Method getBankMethod = faction.getClass().getMethod("getBank");
            return (double) getBankMethod.invoke(faction);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtient le nombre d'alliés de la faction
     */
    public int getFactionAlliesCount(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            Method getAlliesMethod = faction.getClass().getMethod("getAllies");
            Object allies = getAlliesMethod.invoke(faction);
            if (allies instanceof java.util.Collection) {
                return ((java.util.Collection<?>) allies).size();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtient le nombre d'ennemis de la faction
     */
    public int getFactionEnemiesCount(Player player) {
        if (!initialized) return 0;
        
        try {
            Object faction = getPlayerFaction(player);
            if (faction == null) return 0;
            
            Method getEnemiesMethod = faction.getClass().getMethod("getEnemies");
            Object enemies = getEnemiesMethod.invoke(faction);
            if (enemies instanceof java.util.Collection) {
                return ((java.util.Collection<?>) enemies).size();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Ouvre un menu GUI pour la faction (via Kfaction)
     */
    public boolean openFactionMenu(Player player, String menuType) {
        if (!initialized) return false;
        
        try {
            // Exécuter la commande faction correspondante
            player.performCommand("f " + menuType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
