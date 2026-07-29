/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.tasks;

import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.KitsManager;
import pk.ajneb97.managers.PlayerDataManager;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.utils.InventoryUtils;
import pk.ajneb97.utils.ItemUtils;

public class InventoryUpdateTaskManager {
    private PlayerKits2 plugin;

    public InventoryUpdateTaskManager(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable(){

            public void run() {
                InventoryUpdateTaskManager.this.execute();
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
    }

    public void execute() {
        InventoryManager inventoryManager = this.plugin.getInventoryManager();
        KitsManager kitsManager = this.plugin.getKitsManager();
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        ArrayList<InventoryPlayer> players = inventoryManager.getPlayers();
        for (InventoryPlayer player : players) {
            Inventory inv = InventoryUtils.getTopInventory(player.getPlayer());
            if (inv == null) continue;
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; ++i) {
                String kitName;
                ItemStack item = contents[i];
                if (item == null || item.getType().equals((Object)Material.AIR) || (kitName = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_kit")) == null) continue;
                inventoryManager.setKit(kitName, player.getPlayer(), inv, i, kitsManager, playerDataManager, kitItemManager, item);
            }
        }
    }
}

