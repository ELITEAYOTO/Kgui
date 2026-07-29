/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.managers.edit;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.MainConfigManager;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.KitsManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.edit.InventoryEditManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.internal.KitVariable;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.model.inventory.ItemKitInventory;
import pk.ajneb97.model.inventory.KitInventory;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.InventoryItem;
import pk.ajneb97.utils.InventoryUtils;
import pk.ajneb97.utils.ItemUtils;
import pk.ajneb97.utils.MiniMessageUtils;

public class InventoryEditPositionManager {
    private PlayerKits2 plugin;
    private InventoryEditManager inventoryEditManager;

    public InventoryEditPositionManager(PlayerKits2 plugin, InventoryEditManager inventoryEditManager) {
        this.plugin = plugin;
        this.inventoryEditManager = inventoryEditManager;
    }

    public void openInventory(InventoryPlayer inventoryPlayer, String positionInventory) {
        KitInventory kitInventory = this.plugin.getInventoryManager().getInventory(positionInventory);
        MainConfigManager mainConfigManager = this.plugin.getConfigsManager().getMainConfigManager();
        Inventory inv = mainConfigManager.isUseMiniMessage() ? MiniMessageUtils.createInventory(kitInventory.getSlots(), kitInventory.getTitle()) : Bukkit.createInventory(null, (int)kitInventory.getSlots(), (String)MessagesManager.getLegacyColoredMessage(kitInventory.getTitle()));
        List<ItemKitInventory> items = kitInventory.getItems();
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        KitsManager kitsManager = this.plugin.getKitsManager();
        for (ItemKitInventory itemInventory : items) {
            for (int slot : itemInventory.getSlots()) {
                String type = itemInventory.getType();
                if (type != null) {
                    String kitName;
                    Kit kit;
                    if (!type.startsWith("kit: ") || (kit = kitsManager.getKitByName(kitName = type.replace("kit: ", ""))) == null) continue;
                    KitItem kitItem = kit.getDisplayItemDefault().clone();
                    if (kitName.equals(inventoryPlayer.getKitName())) {
                        ArrayList<String> extraLore = new ArrayList<String>();
                        List<String> currentLore = kitItem.getLore();
                        extraLore.add(" ");
                        if (mainConfigManager.isUseMiniMessage()) {
                            extraLore.add(MessagesManager.getLegacyColoredMessage("<green><b>THIS IS THE CURRENT POSITION OF</b>"));
                            extraLore.add(MessagesManager.getLegacyColoredMessage("<green><b>THE KIT.</b>"));
                        } else {
                            extraLore.add(MessagesManager.getLegacyColoredMessage("&a&lTHIS IS THE CURRENT POSITION OF"));
                            extraLore.add(MessagesManager.getLegacyColoredMessage("&a&lTHE KIT."));
                        }
                        ArrayList<String> itemLore = new ArrayList<String>();
                        if (currentLore != null) {
                            itemLore = new ArrayList<String>(currentLore);
                        }
                        itemLore.addAll(extraLore);
                        kitItem.setLore(itemLore);
                    }
                    ArrayList<KitVariable> variablesToReplace = new ArrayList<KitVariable>();
                    variablesToReplace.add(new KitVariable("%kit_name%", kit.getName()));
                    kitItemManager.replaceVariables(kitItem, variablesToReplace, inventoryPlayer.getPlayer());
                    ItemStack item = kitItemManager.createItemFromKitItem(kitItem, null, kit);
                    inv.setItem(slot, item);
                    continue;
                }
                ItemStack item = kitItemManager.createItemFromKitItem(itemInventory.getItem(), inventoryPlayer.getPlayer(), null);
                String openInventory = itemInventory.getOpenInventory();
                if (openInventory != null) {
                    item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_open_inventory", openInventory);
                }
                inv.setItem(slot, item);
            }
        }
        inventoryPlayer.setInventoryName("edit_position;" + positionInventory + ";false");
        inventoryPlayer.getPlayer().openInventory(inv);
        inventoryPlayer.setInventoryName("edit_position;" + positionInventory + ";true");
        inventoryPlayer.saveInventoryContents();
        inventoryPlayer.getPlayer().getInventory().clear();
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("&7Just click on the position of the");
        lore.add("&7inventory, where you want the new kit");
        lore.add("&7item to be displayed.");
        lore.add("");
        lore.add("&7You can go back by closing this inventory.");
        new InventoryItem((Inventory)inventoryPlayer.getPlayer().getInventory(), 22, Material.COMPASS).name("&6&lInfo").lore(lore).ready();
        this.inventoryEditManager.getPlayers().add(inventoryPlayer);
    }

    public void setPosition(InventoryPlayer inventoryPlayer, int slot) {
        String positionInventory = inventoryPlayer.getInventoryName().split(";")[1];
        InventoryManager inventoryManager = this.plugin.getInventoryManager();
        String kitName = inventoryPlayer.getKitName();
        inventoryManager.removeKitFromInventory(kitName);
        KitInventory kitInventory = inventoryManager.getInventory(positionInventory);
        kitInventory.addKitItemOnSlot(kitName, slot);
        this.plugin.getConfigsManager().getInventoryConfigManager().save();
        this.closeInventory(inventoryPlayer);
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, int slot, ClickType clickType, InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getSlotType() == null || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().equals((Object)InventoryUtils.getTopInventory(inventoryPlayer.getPlayer()))) {
            if (item == null || item.getType().equals((Object)Material.AIR)) {
                this.setPosition(inventoryPlayer, slot);
                inventoryPlayer.getPlayer().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&aKit position updated."));
                return;
            }
            String openInventory = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_open_inventory");
            if (openInventory != null) {
                this.openInventory(inventoryPlayer, openInventory);
                return;
            }
            inventoryPlayer.getPlayer().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&cThis position is occupied."));
        }
    }

    public void closeInventory(final InventoryPlayer inventoryPlayer) {
        boolean mustReturn = Boolean.parseBoolean(inventoryPlayer.getInventoryName().split(";")[2]);
        if (mustReturn) {
            new BukkitRunnable(){

                public void run() {
                    inventoryPlayer.restoreSavedInventoryContents();
                    InventoryEditPositionManager.this.inventoryEditManager.openInventory(inventoryPlayer);
                }
            }.runTaskLater((Plugin)this.plugin, 1L);
        }
    }
}

