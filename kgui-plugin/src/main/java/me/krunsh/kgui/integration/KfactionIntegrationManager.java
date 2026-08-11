package me.krunsh.kgui.integration;

import java.lang.reflect.Constructor;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;

/**
 * Frontière de classloader du softdepend Kfaction.
 *
 * Cette classe n'importe aucun type Kfaction. L'adaptateur typé n'est chargé
 * que lorsque Bukkit confirme que Kfaction est activé, afin que Kgui puisse
 * démarrer seul sans NoClassDefFoundError.
 */
public final class KfactionIntegrationManager implements Listener, AutoCloseable {

    private static final String PLUGIN_NAME = "Kfaction";
    private static final String ADAPTER_CLASS =
            "me.krunsh.kgui.integration.kfaction.KfactionIntegration";

    private final Kgui plugin;
    private AutoCloseable adapter;
    private String state = "ABSENT";
    private boolean started;

    public KfactionIntegrationManager(Kgui plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin must not be null");
        this.plugin = plugin;
    }

    public void start() {
        if (started) return;
        started = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Plugin candidate = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (candidate != null && candidate.isEnabled()) attach(candidate);
        else plugin.getLogger().info("Kfaction integration: ABSENT (optional)");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event != null && isKfaction(event.getPlugin())) attach(event.getPlugin());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event != null && isKfaction(event.getPlugin())) detach("ABSENT");
    }

    public boolean isReady() {
        return adapter != null && "READY_2_3".equals(state);
    }

    public String getState() {
        return state;
    }

    private void attach(Plugin kfaction) {
        if (adapter != null || kfaction == null || !kfaction.isEnabled()) return;
        try {
            Class<?> type = Class.forName(ADAPTER_CLASS, true, plugin.getClass().getClassLoader());
            Constructor<?> constructor = type.getConstructor(Kgui.class, Plugin.class);
            Object created = constructor.newInstance(plugin, kfaction);
            if (!(created instanceof AutoCloseable)) {
                throw new IllegalStateException("Kfaction adapter is not closeable");
            }
            adapter = (AutoCloseable) created;
            state = "READY_2_3";
            plugin.getLogger().info("Kfaction integration: READY_2_3");
        } catch (Throwable failure) {
            adapter = null;
            state = "INCOMPATIBLE";
            plugin.getLogger().warning("Kfaction integration disabled: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    private void detach(String nextState) {
        AutoCloseable current = adapter;
        adapter = null;
        state = nextState;
        if (current == null) return;
        try {
            current.close();
        } catch (Exception failure) {
            plugin.getLogger().warning("Kfaction integration shutdown failed: " + failure.getMessage());
        }
    }

    private static boolean isKfaction(Plugin candidate) {
        return candidate != null && PLUGIN_NAME.equalsIgnoreCase(candidate.getName());
    }

    @Override
    public void close() {
        detach("CLOSED");
        if (started) HandlerList.unregisterAll(this);
        started = false;
    }
}
