package me.krunsh.kgui.hooks;

import java.lang.reflect.Method;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Adaptateur PlaceholderAPI sans lien statique vers son classloader. */
public final class PlaceholderAPIHook implements AutoCloseable {
    private Method setPlaceholders;
    private Method containsPlaceholders;

    public PlaceholderAPIHook(Plugin dependency) throws ReflectiveOperationException {
        Class<?> api = ReflectionAccess.load(dependency.getClass().getClassLoader(),
            "me.clip.placeholderapi.PlaceholderAPI");
        setPlaceholders = ReflectionAccess.compatibleMethod(api, "setPlaceholders", null, "");
        try {
            containsPlaceholders = ReflectionAccess.method(api, "containsPlaceholders", 1);
        } catch (NoSuchMethodException ignored) {
            containsPlaceholders = null;
        }
    }

    public String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty() || setPlaceholders == null) return text;
        try {
            Object result = ReflectionAccess.invoke(null, setPlaceholders, player, text);
            return result instanceof String ? (String) result : text;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return text;
        }
    }

    public boolean containsPlaceholders(String text) {
        if (text == null || containsPlaceholders == null) return false;
        try {
            return Boolean.TRUE.equals(ReflectionAccess.invoke(null, containsPlaceholders, text));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public void close() {
        setPlaceholders = null;
        containsPlaceholders = null;
    }
}
