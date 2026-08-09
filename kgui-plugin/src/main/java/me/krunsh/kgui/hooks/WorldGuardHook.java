package me.krunsh.kgui.hooks;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.listeners.WorldGuardListener;

/** Adaptateur WorldGuard 6 dechargeable. */
public final class WorldGuardHook implements AutoCloseable {
    private final Kgui plugin;
    private final Map<UUID, Set<String>> playerRegions = new HashMap<>();
    private final Map<UUID, Long> regionCooldowns = new HashMap<>();
    private Method getRegionManager;
    private Listener listener;

    public WorldGuardHook(Kgui plugin, Plugin dependency) throws ReflectiveOperationException {
        this.plugin = plugin;
        Class<?> wgBukkit = ReflectionAccess.load(dependency.getClass().getClassLoader(),
            "com.sk89q.worldguard.bukkit.WGBukkit");
        getRegionManager = ReflectionAccess.method(wgBukkit, "getRegionManager", 1);
    }

    public Set<String> getRegions(Location location) {
        if (getRegionManager == null || location == null || location.getWorld() == null) return Collections.emptySet();
        try {
            Object manager = ReflectionAccess.invoke(null, getRegionManager, location.getWorld());
            if (manager == null) return Collections.emptySet();
            Method applicableMethod = ReflectionAccess.compatibleMethod(manager.getClass(), "getApplicableRegions", location);
            Object applicable = ReflectionAccess.invoke(manager, applicableMethod, location);
            if (!(applicable instanceof Iterable)) return Collections.emptySet();
            Set<String> regions = new HashSet<>();
            for (Object region : (Iterable<?>) applicable) {
                if (region == null) continue;
                Object id = ReflectionAccess.invoke(region, ReflectionAccess.method(region.getClass(), "getId", 0));
                if (id != null) regions.add(String.valueOf(id).toLowerCase(Locale.ROOT));
            }
            return regions;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Collections.emptySet();
        }
    }

    public boolean isInRegion(Player player, String regionId) {
        return player != null && regionId != null
            && getRegions(player.getLocation()).contains(regionId.toLowerCase(Locale.ROOT));
    }
    public Set<String> getPlayerRegions(Player player) { return getRegions(player.getLocation()); }

    public Set<String> updatePlayerRegions(Player player) {
        Set<String> current = getPlayerRegions(player);
        Set<String> previous = playerRegions.get(player.getUniqueId());
        Set<String> entered = new HashSet<>(current);
        if (previous != null) entered.removeAll(previous);
        playerRegions.put(player.getUniqueId(), new HashSet<>(current));
        return entered;
    }

    public boolean checkTriggerCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long last = regionCooldowns.get(player.getUniqueId());
        if (last != null && now - last < plugin.getConfigManager().getRegionTriggerCooldown() * 1000L) return false;
        regionCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    public void cleanupPlayer(UUID uuid) {
        playerRegions.remove(uuid);
        regionCooldowns.remove(uuid);
    }

    public void registerListeners() {
        if (listener == null && plugin.getConfigManager().isRegionEnabled()) {
            listener = new WorldGuardListener(plugin);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }

    public boolean regionExists(String worldName, String regionId) {
        if (getRegionManager == null || worldName == null || regionId == null) return false;
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return false;
        try {
            Object manager = ReflectionAccess.invoke(null, getRegionManager, world);
            return manager != null && Boolean.TRUE.equals(ReflectionAccess.invoke(manager,
                ReflectionAccess.compatibleMethod(manager.getClass(), "hasRegion", regionId), regionId));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public void close() {
        if (listener != null) HandlerList.unregisterAll(listener);
        listener = null;
        playerRegions.clear();
        regionCooldowns.clear();
        getRegionManager = null;
    }
}
