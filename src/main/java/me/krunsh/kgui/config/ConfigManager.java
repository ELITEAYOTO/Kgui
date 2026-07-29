package me.krunsh.kgui.config;

import me.krunsh.kgui.Kgui;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de la configuration principale
 */
public class ConfigManager {

    private final Kgui plugin;
    private FileConfiguration config;
    
    // Cache des valeurs fréquemment utilisées
    private boolean debug;
    private String nbtTag;
    private boolean cleanOnClose;
    private boolean cleanOnWorldChange;
    private boolean cleanOnQuit;
    private boolean blockAuctionHouse;
    private boolean blockInCombat;
    private boolean closeOnCombat;
    private int cooldownAfterCombat;
    private String worldMode;
    private List<String> worldList;
    private int placeholderCacheTicks;
    private boolean useProtocolLib;
    private int maxCachedMenus;
    private boolean animationsEnabled;
    private int minAnimationInterval;
    private boolean regionEnabled;
    private int regionTriggerCooldown;
    private double regionCheckDistance;
    private boolean hoverEnabled;
    private String hoverMode;
    private int hoverTimerInterval;

    public ConfigManager(Kgui plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Charge ou recharge la configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        cacheValues();
    }

    /**
     * Cache les valeurs pour un accès rapide
     */
    private void cacheValues() {
        // Debug
        this.debug = config.getBoolean("debug", false);
        
        // Sécurité
        this.nbtTag = config.getString("security.nbt_tag", "KGUI_ITEM");
        this.cleanOnClose = config.getBoolean("security.clean_on_close", true);
        this.cleanOnWorldChange = config.getBoolean("security.clean_on_world_change", true);
        this.cleanOnQuit = config.getBoolean("security.clean_on_quit", true);
        this.blockAuctionHouse = config.getBoolean("security.block_auction_house", true);
        
        // Combat
        this.blockInCombat = config.getBoolean("combat.block_in_combat", true);
        this.closeOnCombat = config.getBoolean("combat.close_on_combat", true);
        this.cooldownAfterCombat = config.getInt("combat.cooldown_after_combat", 0);
        
        // Mondes
        this.worldMode = config.getString("worlds.mode", "blacklist");
        this.worldList = config.getStringList("worlds.list");
        if (this.worldList == null) {
            this.worldList = new ArrayList<>();
        }
        
        // Performance
        this.placeholderCacheTicks = config.getInt("performance.placeholder_cache_ticks", 20);
        this.useProtocolLib = config.getBoolean("performance.use_protocol_lib", true);
        this.maxCachedMenus = config.getInt("performance.max_cached_menus", 100);
        
        // Animations
        this.animationsEnabled = config.getBoolean("animations.enabled", true);
        this.minAnimationInterval = config.getInt("animations.min_interval", 5);
        
        // Région
        this.regionEnabled = config.getBoolean("region.enabled", true);
        this.regionTriggerCooldown = config.getInt("region.trigger_cooldown", 5);
        this.regionCheckDistance = config.getDouble("region.check_distance", 1.5);
        
        // Hover
        this.hoverEnabled = config.getBoolean("hover.enabled", true);
        this.hoverMode = config.getString("hover.mode", "packet");
        this.hoverTimerInterval = config.getInt("hover.timer_interval", 2);
    }

    /**
     * Recharge la configuration
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Obtient les données de son depuis la config
     */
    public SoundData getSoundData(String path) {
        if (!config.getBoolean(path + ".enabled", true)) {
            return null;
        }
        
        String soundName = config.getString(path + ".sound", "CLICK");
        float volume = (float) config.getDouble(path + ".volume", 1.0);
        float pitch = (float) config.getDouble(path + ".pitch", 1.0);
        
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            return new SoundData(sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
            return null;
        }
    }

    /**
     * Vérifie si un monde est autorisé pour les menus
     */
    public boolean isWorldAllowed(String worldName) {
        if (worldMode.equalsIgnoreCase("whitelist")) {
            return worldList.contains(worldName);
        } else { // blacklist
            return !worldList.contains(worldName);
        }
    }

    // ==================== GETTERS ====================

    public boolean isDebug() {
        return debug;
    }

    public String getNbtTag() {
        return nbtTag;
    }

    public boolean isCleanOnClose() {
        return cleanOnClose;
    }

    public boolean isCleanOnWorldChange() {
        return cleanOnWorldChange;
    }

    public boolean isCleanOnQuit() {
        return cleanOnQuit;
    }

    public boolean isBlockAuctionHouse() {
        return blockAuctionHouse;
    }

    public boolean isBlockInCombat() {
        return blockInCombat;
    }

    public boolean isCloseOnCombat() {
        return closeOnCombat;
    }

    public int getCooldownAfterCombat() {
        return cooldownAfterCombat;
    }

    public int getPlaceholderCacheTicks() {
        return placeholderCacheTicks;
    }

    public boolean isUseProtocolLib() {
        return useProtocolLib;
    }

    public int getMaxCachedMenus() {
        return maxCachedMenus;
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public int getMinAnimationInterval() {
        return minAnimationInterval;
    }

    public boolean isRegionEnabled() {
        return regionEnabled;
    }

    public int getRegionTriggerCooldown() {
        return regionTriggerCooldown;
    }

    public double getRegionCheckDistance() {
        return regionCheckDistance;
    }

    public boolean isHoverEnabled() {
        return hoverEnabled;
    }

    public String getHoverMode() {
        return hoverMode;
    }

    public int getHoverTimerInterval() {
        return hoverTimerInterval;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Classe interne pour les données de son
     */
    public static class SoundData {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundData(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Sound getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }
}
