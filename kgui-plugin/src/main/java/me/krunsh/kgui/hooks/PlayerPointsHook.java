package me.krunsh.kgui.hooks;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Adaptateur PlayerPoints dechargeable et fail-closed. */
public final class PlayerPointsHook implements AutoCloseable {
    private Object api;
    private Method look;
    private Method take;
    private Method give;
    private Method set;

    public PlayerPointsHook(Plugin dependency) throws ReflectiveOperationException {
        Method getApi = ReflectionAccess.method(dependency.getClass(), "getAPI", 0);
        api = ReflectionAccess.invoke(dependency, getApi);
        if (api == null) throw new IllegalStateException("PlayerPoints API unavailable");
        Class<?> type = api.getClass();
        look = ReflectionAccess.compatibleMethod(type, "look", UUID.randomUUID());
        take = ReflectionAccess.compatibleMethod(type, "take", UUID.randomUUID(), 1);
        give = ReflectionAccess.compatibleMethod(type, "give", UUID.randomUUID(), 1);
        set = ReflectionAccess.compatibleMethod(type, "set", UUID.randomUUID(), 1);
    }

    public boolean isEnabled() { return api != null; }
    public int getPoints(Player player) { return number(invoke(look, player.getUniqueId())).intValue(); }
    public boolean hasPoints(Player player, int amount) { return amount >= 0 && getPoints(player) >= amount; }
    public boolean takePoints(Player player, int amount) { return amount > 0 && bool(invoke(take, player.getUniqueId(), amount)); }
    public boolean givePoints(Player player, int amount) { return amount > 0 && bool(invoke(give, player.getUniqueId(), amount)); }
    public boolean setPoints(Player player, int amount) { return amount >= 0 && bool(invoke(set, player.getUniqueId(), amount)); }

    private Object invoke(Method method, Object... arguments) {
        if (api == null || method == null) return null;
        try { return ReflectionAccess.invoke(api, method, arguments); }
        catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
    }

    private static Number number(Object value) { return value instanceof Number ? (Number) value : Integer.valueOf(0); }
    private static boolean bool(Object value) { return Boolean.TRUE.equals(value); }

    @Override
    public void close() {
        look = null;
        take = null;
        give = null;
        set = null;
        api = null;
    }
}
