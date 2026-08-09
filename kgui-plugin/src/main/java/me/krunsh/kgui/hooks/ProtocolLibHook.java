package me.krunsh.kgui.hooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;

/** Adaptateur ProtocolLib optionnel sans dependance Maven ni lien de classloader permanent. */
public final class ProtocolLibHook implements AutoCloseable {
    private final Kgui plugin;
    private Object protocolManager;
    private Object setSlotPacketType;
    private Method createPacket;

    public ProtocolLibHook(Kgui plugin, Plugin dependency) throws ReflectiveOperationException {
        this.plugin = plugin;
        ClassLoader loader = dependency.getClass().getClassLoader();
        Class<?> protocolLibrary = ReflectionAccess.load(loader, "com.comphenix.protocol.ProtocolLibrary");
        protocolManager = ReflectionAccess.invoke(null, ReflectionAccess.method(protocolLibrary, "getProtocolManager", 0));
        if (protocolManager == null) throw new IllegalStateException("ProtocolManager unavailable");
        Class<?> serverTypes = ReflectionAccess.load(loader, "com.comphenix.protocol.PacketType$Play$Server");
        Field setSlot = serverTypes.getField("SET_SLOT");
        setSlotPacketType = setSlot.get(null);
        createPacket = ReflectionAccess.compatibleMethod(protocolManager.getClass(), "createPacket", setSlotPacketType);
    }

    public void sendSlotUpdate(Player player, int slot, ItemStack item) {
        if (protocolManager == null || createPacket == null || player == null) return;
        try {
            Object packet = ReflectionAccess.invoke(protocolManager, createPacket, setSlotPacketType);
            Object integers = ReflectionAccess.invoke(packet, ReflectionAccess.method(packet.getClass(), "getIntegers", 0));
            write(integers, 0, getWindowId(player));
            write(integers, 1, slot);
            Object itemModifier = ReflectionAccess.invoke(packet,
                ReflectionAccess.method(packet.getClass(), "getItemModifier", 0));
            write(itemModifier, 0, item);
            Method send = ReflectionAccess.compatibleMethod(protocolManager.getClass(), "sendServerPacket", player, packet);
            ReflectionAccess.invoke(protocolManager, send, player, packet);
        } catch (ReflectiveOperationException | RuntimeException error) {
            player.updateInventory();
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[Debug] ProtocolLib slot update failed: " + error.getMessage());
            }
        }
    }

    public void sendMultipleSlotUpdates(Player player, int[] slots, ItemStack[] items) {
        if (slots == null || items == null || slots.length != items.length) return;
        for (int index = 0; index < slots.length; index++) sendSlotUpdate(player, slots[index], items[index]);
    }

    private static void write(Object modifier, int index, Object value) throws ReflectiveOperationException {
        Method write = ReflectionAccess.compatibleMethod(modifier.getClass(), "write", index, value);
        ReflectionAccess.invoke(modifier, write, index, value);
    }

    private int getWindowId(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object activeContainer = handle.getClass().getField("activeContainer").get(handle);
            return activeContainer.getClass().getField("windowId").getInt(activeContainer);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    public Object getProtocolManager() { return protocolManager; }

    @Override
    public void close() {
        createPacket = null;
        setSlotPacketType = null;
        protocolManager = null;
    }
}
