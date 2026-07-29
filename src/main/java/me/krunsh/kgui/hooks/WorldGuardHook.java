package me.krunsh.kgui.hooks;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.listeners.WorldGuardListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Hook pour WorldGuard
 */
public class WorldGuardHook {

    private final Kgui plugin;
    private WorldGuardPlugin worldGuard;
    
    // Cache des régions par joueur
    private final Map<UUID, Set<String>> playerRegions = new HashMap<>();
    private final Map<UUID, Long> regionCooldowns = new HashMap<>();

    public WorldGuardHook(Kgui plugin) {
        this.plugin = plugin;
        this.worldGuard = WGBukkit.getPlugin();
    }

    /**
     * Obtient les régions à une location
     */
    public Set<String> getRegions(Location location) {
        Set<String> regions = new HashSet<>();
        
        if (worldGuard == null) return regions;
        
        RegionManager regionManager = WGBukkit.getRegionManager(location.getWorld());
        if (regionManager == null) return regions;
        
        ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(location);
        for (ProtectedRegion region : applicableRegions) {
            regions.add(region.getId());
        }
        
        return regions;
    }

    /**
     * Vérifie si un joueur est dans une région
     */
    public boolean isInRegion(Player player, String regionId) {
        Set<String> regions = getRegions(player.getLocation());
        return regions.contains(regionId.toLowerCase());
    }

    /**
     * Obtient les régions actuelles d'un joueur
     */
    public Set<String> getPlayerRegions(Player player) {
        return getRegions(player.getLocation());
    }

    /**
     * Met à jour le cache des régions d'un joueur
     * Retourne les nouvelles régions entrées
     */
    public Set<String> updatePlayerRegions(Player player) {
        Set<String> currentRegions = getPlayerRegions(player);
        Set<String> previousRegions = playerRegions.getOrDefault(player.getUniqueId(), new HashSet<>());
        
        // Trouver les nouvelles régions
        Set<String> enteredRegions = new HashSet<>(currentRegions);
        enteredRegions.removeAll(previousRegions);
        
        // Mettre à jour le cache
        playerRegions.put(player.getUniqueId(), currentRegions);
        
        return enteredRegions;
    }

    /**
     * Vérifie le cooldown de trigger pour un joueur
     */
    public boolean checkTriggerCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long lastTrigger = regionCooldowns.get(player.getUniqueId());
        
        if (lastTrigger != null) {
            int cooldownSeconds = plugin.getConfigManager().getRegionTriggerCooldown();
            if (now - lastTrigger < cooldownSeconds * 1000L) {
                return false;
            }
        }
        
        regionCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanupPlayer(UUID uuid) {
        playerRegions.remove(uuid);
        regionCooldowns.remove(uuid);
    }

    /**
     * Enregistre le listener WorldGuard
     */
    public void registerListeners() {
        if (plugin.getConfigManager().isRegionEnabled()) {
            Bukkit.getPluginManager().registerEvents(new WorldGuardListener(plugin), plugin);
        }
    }

    /**
     * Vérifie si une région existe
     */
    public boolean regionExists(String worldName, String regionId) {
        if (worldGuard == null) return false;
        
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return false;
        
        RegionManager regionManager = WGBukkit.getRegionManager(world);
        if (regionManager == null) return false;
        
        return regionManager.hasRegion(regionId);
    }
}
