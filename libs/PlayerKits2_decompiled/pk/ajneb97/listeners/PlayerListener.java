/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package pk.ajneb97.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.utils.InventoryUtils;

public class PlayerListener
implements Listener {
    private PlayerKits2 plugin;

    public PlayerListener(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.getPlayerDataManager().manageJoin(player);
        String latestVersion = this.plugin.getUpdateCheckerManager().getLatestVersion();
        if (player.isOp() && this.plugin.getConfigsManager().getMainConfigManager().isUpdateNotify() && !this.plugin.version.equals(latestVersion)) {
            player.sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&cThere is a new version available. &e(&7" + latestVersion + "&e)"));
            player.sendMessage(MessagesManager.getLegacyColoredMessage("&cYou can download it at: &ahttps://modrinth.com/plugin/playerkits-2"));
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        this.plugin.getPlayerDataManager().manageLeave(event.getPlayer());
    }

    @EventHandler
    public void closeInventory(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        this.plugin.getInventoryManager().removeInventoryPlayer(player);
    }

    @EventHandler
    public void clickInventory(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        InventoryManager invManager = this.plugin.getInventoryManager();
        InventoryPlayer inventoryPlayer = invManager.getInventoryPlayer(player);
        if (inventoryPlayer != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getSlotType() == null) {
                return;
            }
            if (event.getClickedInventory().equals((Object)InventoryUtils.getTopInventory(player))) {
                ClickType clickType = event.getClick();
                invManager.clickInventory(inventoryPlayer, event.getCurrentItem(), clickType);
            }
        }
    }
}

