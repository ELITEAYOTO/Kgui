/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.edit.InventoryEditManager;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.utils.InventoryUtils;

public class InventoryEditListener
implements Listener {
    private PlayerKits2 plugin;

    public InventoryEditListener(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void closeInventory(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        InventoryEditManager invManager = this.plugin.getInventoryEditManager();
        InventoryPlayer inventoryPlayer = invManager.getInventoryPlayer(player);
        if (inventoryPlayer != null) {
            if (inventoryPlayer.getInventoryName().startsWith("edit_position")) {
                invManager.getInventoryEditPositionManager().closeInventory(inventoryPlayer);
            }
            this.plugin.getVerifyManager().verify();
        }
        this.plugin.getInventoryEditManager().removeInventoryPlayer(player);
    }

    @EventHandler
    public void clickInventory(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        InventoryEditManager invManager = this.plugin.getInventoryEditManager();
        InventoryPlayer inventoryPlayer = invManager.getInventoryPlayer(player);
        if (inventoryPlayer != null) {
            ClickType clickType = event.getClick();
            if (inventoryPlayer.getInventoryName().startsWith("edit_display_")) {
                invManager.getInventoryEditDisplayManager().clickInventory(inventoryPlayer, event.getCurrentItem(), event.getSlot(), clickType, event);
                return;
            }
            if (inventoryPlayer.getInventoryName().equals("edit_items")) {
                invManager.getInventoryEditKitItemsManager().clickInventory(inventoryPlayer, event.getCurrentItem(), event.getSlot(), clickType, event);
                return;
            }
            if (inventoryPlayer.getInventoryName().startsWith("edit_position")) {
                invManager.getInventoryEditPositionManager().clickInventory(inventoryPlayer, event.getCurrentItem(), event.getSlot(), clickType, event);
                return;
            }
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getSlotType() == null) {
                return;
            }
            if (event.getClickedInventory().equals((Object)InventoryUtils.getTopInventory(player))) {
                invManager.clickInventory(inventoryPlayer, event.getCurrentItem(), event.getSlot(), clickType);
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onChat(final AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        final InventoryEditManager invManager = this.plugin.getInventoryEditManager();
        final InventoryPlayer inventoryPlayer = invManager.getInventoryPlayer(player);
        if (inventoryPlayer != null) {
            event.setCancelled(true);
            new BukkitRunnable(){

                public void run() {
                    invManager.writeChat(inventoryPlayer, ChatColor.stripColor((String)event.getMessage()));
                }
            }.runTaskLater((Plugin)this.plugin, 1L);
        }
    }
}

