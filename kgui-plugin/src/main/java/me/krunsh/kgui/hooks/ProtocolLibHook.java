package me.krunsh.kgui.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import me.krunsh.kgui.Kgui;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

/**
 * Hook pour ProtocolLib
 * Permet des updates d'inventaire plus performants
 */
public class ProtocolLibHook {

    private final Kgui plugin;
    private ProtocolManager protocolManager;

    public ProtocolLibHook(Kgui plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Envoie une mise à jour d'un seul slot au client
     * Plus performant que player.updateInventory()
     */
    public void sendSlotUpdate(Player player, int slot, ItemStack item) {
        try {
            // Packet SET_SLOT pour 1.8
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
            
            // Window ID (0 = inventaire joueur, >0 = autre inventaire)
            // Pour un GUI ouvert, on utilise l'ID de la fenêtre active
            packet.getIntegers().write(0, getWindowId(player));
            
            // Slot
            packet.getIntegers().write(1, slot);
            
            // Item
            packet.getItemModifier().write(0, item);
            
            protocolManager.sendServerPacket(player, packet);
            
        } catch (InvocationTargetException e) {
            // Fallback sur méthode standard
            player.updateInventory();
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[Debug] ProtocolLib slot update failed: " + e.getMessage());
            }
        }
    }

    /**
     * Obtient l'ID de la fenêtre active du joueur
     */
    private int getWindowId(Player player) {
        try {
            // Accès via reflection pour 1.8
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object activeContainer = handle.getClass().getField("activeContainer").get(handle);
            return (int) activeContainer.getClass().getField("windowId").get(activeContainer);
        } catch (Exception e) {
            // Par défaut, utiliser 0
            return 0;
        }
    }

    /**
     * Envoie une mise à jour de plusieurs slots
     */
    public void sendMultipleSlotUpdates(Player player, int[] slots, ItemStack[] items) {
        if (slots.length != items.length) return;
        
        for (int i = 0; i < slots.length; i++) {
            sendSlotUpdate(player, slots[i], items[i]);
        }
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}
