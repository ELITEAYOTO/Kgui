package me.krunsh.kgui.hooks;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;

/** Adaptateur CombatTagPlus sans type externe dans les signatures Kgui. */
public final class CombatTagHook implements AutoCloseable {
    private final Kgui plugin;
    private Object tagManager;
    private Method isTagged;
    private Method getTag;
    private Method eventGetPlayer;
    private Class<? extends Event> combatEventType;
    private Listener listener;

    @SuppressWarnings("unchecked")
    public CombatTagHook(Kgui plugin, Plugin dependency) throws ReflectiveOperationException {
        this.plugin = plugin;
        Method getTagManager = ReflectionAccess.method(dependency.getClass(), "getTagManager", 0);
        tagManager = ReflectionAccess.invoke(dependency, getTagManager);
        if (tagManager == null) throw new IllegalStateException("CombatTagPlus TagManager unavailable");
        isTagged = ReflectionAccess.compatibleMethod(tagManager.getClass(), "isTagged", UUID.randomUUID());
        getTag = ReflectionAccess.compatibleMethod(tagManager.getClass(), "getTag", UUID.randomUUID());
        Class<?> eventType = ReflectionAccess.load(dependency.getClass().getClassLoader(),
            "net.minelink.ctplus.event.PlayerCombatTagEvent");
        if (!Event.class.isAssignableFrom(eventType)) throw new IllegalStateException("Invalid CombatTagPlus event type");
        combatEventType = (Class<? extends Event>) eventType;
        eventGetPlayer = ReflectionAccess.method(eventType, "getPlayer", 0);
    }

    public boolean isInCombat(Player player) {
        if (tagManager == null || isTagged == null || player == null) return false;
        try { return Boolean.TRUE.equals(ReflectionAccess.invoke(tagManager, isTagged, player.getUniqueId())); }
        catch (ReflectiveOperationException | RuntimeException ignored) { return false; }
    }

    public long getRemainingTagTime(Player player) {
        if (!isInCombat(player) || getTag == null) return 0L;
        try {
            Object tag = ReflectionAccess.invoke(tagManager, getTag, player.getUniqueId());
            if (tag == null) return 0L;
            Object expire = ReflectionAccess.invoke(tag, ReflectionAccess.method(tag.getClass(), "getExpireTime", 0));
            long remaining = expire instanceof Number ? ((Number) expire).longValue() - System.currentTimeMillis() : 0L;
            return Math.max(0L, remaining / 1000L);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0L;
        }
    }

    public void registerListeners() {
        if (listener != null || combatEventType == null || eventGetPlayer == null) return;
        listener = new Listener() { };
        EventExecutor executor = (ignored, event) -> {
            try {
                Object result = ReflectionAccess.invoke(event, eventGetPlayer);
                if (result instanceof Player) onCombatTag((Player) result);
            } catch (ReflectiveOperationException | RuntimeException error) {
                throw new EventException(error);
            }
        };
        Bukkit.getPluginManager().registerEvent(combatEventType, listener, EventPriority.MONITOR,
            executor, plugin, true);
    }

    private void onCombatTag(Player player) {
        if (!plugin.getConfigManager().isCloseOnCombat() || !plugin.getGuiManager().hasOpenMenu(player)) return;
        player.closeInventory();
        plugin.getGuiManager().closeMenu(player, false);
        plugin.getMessageManager().send(player, "closed-combat");
    }

    @Override
    public void close() {
        if (listener != null) HandlerList.unregisterAll(listener);
        listener = null;
        combatEventType = null;
        eventGetPlayer = null;
        isTagged = null;
        getTag = null;
        tagManager = null;
    }
}
