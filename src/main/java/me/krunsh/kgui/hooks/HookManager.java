package me.krunsh.kgui.hooks;

import org.bukkit.Bukkit;

import me.krunsh.kgui.Kgui;

/**
 * Gestionnaire des hooks avec les plugins externes
 */
public class HookManager {

    private final Kgui plugin;
    
    // Hooks
    private PlaceholderAPIHook placeholderAPIHook;
    private VaultHook vaultHook;
    private PlayerPointsHook playerPointsHook;
    private WorldGuardHook worldGuardHook;
    private CombatTagHook combatTagHook;
    private ZAuctionHouseHook zAuctionHouseHook;
    private ProtocolLibHook protocolLibHook;
    private HeadDatabaseHook headDatabaseHook;
    private KfactionHook kfactionHook;
    
    // Flags
    private boolean placeholderAPIEnabled = false;
    private boolean vaultEnabled = false;
    private boolean playerPointsEnabled = false;
    private boolean worldGuardEnabled = false;
    private boolean combatTagEnabled = false;
    private boolean zAuctionHouseEnabled = false;
    private boolean protocolLibEnabled = false;
    private boolean headDatabaseEnabled = false;
    private boolean kfactionEnabled = false;

    public HookManager(Kgui plugin) {
        this.plugin = plugin;
        initializeHooks();
    }

    /**
     * Initialise tous les hooks disponibles
     */
    private void initializeHooks() {
        // PlaceholderAPI
        if (isPluginEnabled("PlaceholderAPI")) {
            try {
                placeholderAPIHook = new PlaceholderAPIHook(plugin);
                placeholderAPIEnabled = true;
                plugin.getLogger().info("Hooked into PlaceholderAPI");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into PlaceholderAPI: " + e.getMessage());
            }
        }
        
        // Vault
        if (isPluginEnabled("Vault")) {
            try {
                vaultHook = new VaultHook(plugin);
                vaultEnabled = vaultHook.isEconomyEnabled();
                if (vaultEnabled) {
                    plugin.getLogger().info("Hooked into Vault (Economy)");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into Vault: " + e.getMessage());
            }
        }
        
        // PlayerPoints
        if (isPluginEnabled("PlayerPoints")) {
            try {
                playerPointsHook = new PlayerPointsHook(plugin);
                playerPointsEnabled = true;
                plugin.getLogger().info("Hooked into PlayerPoints");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into PlayerPoints: " + e.getMessage());
            }
        }
        
        // WorldGuard
        if (isPluginEnabled("WorldGuard")) {
            try {
                worldGuardHook = new WorldGuardHook(plugin);
                worldGuardEnabled = true;
                plugin.getLogger().info("Hooked into WorldGuard");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into WorldGuard: " + e.getMessage());
            }
        }
        
        // CombatTagPlus
        if (isPluginEnabled("CombatTagPlus")) {
            try {
                combatTagHook = new CombatTagHook(plugin);
                combatTagEnabled = true;
                plugin.getLogger().info("Hooked into CombatTagPlus");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into CombatTagPlus: " + e.getMessage());
            }
        }
        
        // ZAuctionHouse
        if (isPluginEnabled("zAuctionHouse")) {
            try {
                zAuctionHouseHook = new ZAuctionHouseHook(plugin);
                zAuctionHouseEnabled = true;
                plugin.getLogger().info("Hooked into ZAuctionHouse");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into ZAuctionHouse: " + e.getMessage());
            }
        }
        
        // ProtocolLib
        if (isPluginEnabled("ProtocolLib")) {
            try {
                protocolLibHook = new ProtocolLibHook(plugin);
                protocolLibEnabled = true;
                plugin.getLogger().info("Hooked into ProtocolLib");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into ProtocolLib: " + e.getMessage());
            }
        }
        
        // HeadDatabase
        if (isPluginEnabled("HeadDatabase")) {
            try {
                headDatabaseHook = new HeadDatabaseHook(plugin);
                headDatabaseEnabled = true;
                plugin.getLogger().info("Hooked into HeadDatabase");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into HeadDatabase: " + e.getMessage());
            }
        }
        
        // Kfaction (Plugin Factions custom écosystème K)
        if (isPluginEnabled("Kfaction")) {
            try {
                kfactionHook = new KfactionHook(plugin);
                kfactionEnabled = kfactionHook.isInitialized();
                if (kfactionEnabled) {
                    plugin.getLogger().info("Hooked into Kfaction");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into Kfaction: " + e.getMessage());
            }
        }
    }

    /**
     * Vérifie si un plugin est activé
     */
    private boolean isPluginEnabled(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    /**
     * Retourne les informations sur les hooks activés
     */
    public String getEnabledHooksInfo() {
        StringBuilder sb = new StringBuilder();
        if (placeholderAPIEnabled) sb.append("PAPI, ");
        if (vaultEnabled) sb.append("Vault, ");
        if (playerPointsEnabled) sb.append("PlayerPoints, ");
        if (worldGuardEnabled) sb.append("WorldGuard, ");
        if (combatTagEnabled) sb.append("CombatTag, ");
        if (zAuctionHouseEnabled) sb.append("ZAH, ");
        if (protocolLibEnabled) sb.append("ProtocolLib, ");
        if (headDatabaseEnabled) sb.append("HDB, ");
        if (kfactionEnabled) sb.append("Kfaction, ");
        
        if (sb.length() == 0) {
            return "None";
        }
        
        return sb.substring(0, sb.length() - 2);
    }

    // ==================== GETTERS ====================

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public boolean isPlayerPointsEnabled() {
        return playerPointsEnabled;
    }

    public PlayerPointsHook getPlayerPointsHook() {
        return playerPointsHook;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public boolean isCombatTagEnabled() {
        return combatTagEnabled;
    }

    public CombatTagHook getCombatTagHook() {
        return combatTagHook;
    }

    public boolean isZAuctionHouseEnabled() {
        return zAuctionHouseEnabled;
    }

    public ZAuctionHouseHook getZAuctionHouseHook() {
        return zAuctionHouseHook;
    }

    public boolean isProtocolLibEnabled() {
        return protocolLibEnabled;
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

    public boolean isHeadDatabaseEnabled() {
        return headDatabaseEnabled;
    }

    public HeadDatabaseHook getHeadDatabaseHook() {
        return headDatabaseHook;
    }

    public boolean isKfactionEnabled() {
        return kfactionEnabled;
    }

    public KfactionHook getKfactionHook() {
        return kfactionHook;
    }
    
    // Alias de compatibilité
    public boolean isFactionsEnabled() {
        return kfactionEnabled;
    }
}
