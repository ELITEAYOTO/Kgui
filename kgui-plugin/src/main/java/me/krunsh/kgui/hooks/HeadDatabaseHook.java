package me.krunsh.kgui.hooks;

import java.lang.reflect.Method;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;

/** Adaptateur HeadDatabase dechargeable. */
public final class HeadDatabaseHook implements AutoCloseable {
    private final Kgui plugin;
    private Object api;
    private Method getItemHead;

    public HeadDatabaseHook(Kgui plugin, Plugin dependency) throws ReflectiveOperationException {
        this.plugin = plugin;
        Class<?> apiType = ReflectionAccess.load(dependency.getClass().getClassLoader(),
            "me.arcaniax.hdb.api.HeadDatabaseAPI");
        api = ReflectionAccess.construct(apiType);
        getItemHead = ReflectionAccess.method(apiType, "getItemHead", 1);
    }

    public ItemStack getHead(String id) {
        if (api == null || getItemHead == null || id == null || id.trim().isEmpty()) return null;
        try {
            Object result = ReflectionAccess.invoke(api, getItemHead, id.trim());
            return result instanceof ItemStack ? (ItemStack) result : null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            plugin.getLogger().warning("Failed to get head from HeadDatabase: " + id);
            return null;
        }
    }

    public boolean isHead(String id) {
        return getHead(id) != null;
    }

    @Override
    public void close() {
        getItemHead = null;
        api = null;
    }
}
