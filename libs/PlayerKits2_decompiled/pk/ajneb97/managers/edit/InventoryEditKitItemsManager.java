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
 *  org.bukkit.inventory.meta.ItemMeta
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
import org.bukkit.inventory.meta.ItemMeta;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.edit.InventoryEditManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.InventoryItem;
import pk.ajneb97.utils.InventoryUtils;
import pk.ajneb97.utils.ItemUtils;
import pk.ajneb97.utils.OtherUtils;
import pk.ajneb97.utils.ServerVersion;

public class InventoryEditKitItemsManager {
    private PlayerKits2 plugin;
    private InventoryEditManager inventoryEditManager;

    public InventoryEditKitItemsManager(PlayerKits2 plugin, InventoryEditManager inventoryEditManager) {
        this.plugin = plugin;
        this.inventoryEditManager = inventoryEditManager;
    }

    public void openInventory(InventoryPlayer inventoryPlayer) {
        inventoryPlayer.setInventoryName("edit_items");
        Inventory inv = Bukkit.createInventory(null, (int)54, (String)MessagesManager.getLegacyColoredMessage("&9Editing Kit"));
        for (int i = 46; i <= 52; ++i) {
            if (OtherUtils.isLegacy()) {
                new InventoryItem(inv, i, Material.valueOf((String)"STAINED_GLASS_PANE")).dataValue((short)15).name("").ready();
                continue;
            }
            new InventoryItem(inv, i, Material.BLACK_STAINED_GLASS_PANE).name("").ready();
        }
        new InventoryItem(inv, 45, Material.ARROW).name("&eGo Back").ready();
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("&7If you make any changes in this inventory");
        lore.add("&7it is very important to click this item");
        lore.add("&7before closing it or going back.");
        new InventoryItem(inv, 53, Material.EMERALD_BLOCK).name("&6&lSave Kit Items").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Here you can edit the items of the kit.");
        lore.add("");
        lore.add("&7If you want to set an item on the offhand");
        lore.add("&7just RIGHT CLICK it.");
        lore.add("");
        lore.add("&7You can move the items to define a custom");
        lore.add("&7position on the PREVIEW inventory.");
        new InventoryItem(inv, 49, Material.COMPASS).name("&6&lInfo").lore(lore).ready();
        Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
        ArrayList<KitItem> items = kit.getItems();
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        int slot = 0;
        for (KitItem kitItem : items) {
            int finalSlot;
            ItemStack item = kitItemManager.createItemFromKitItem(kitItem, null, kit);
            int n = finalSlot = kitItem.getPreviewSlot() != -1 ? kitItem.getPreviewSlot() : slot;
            if (kitItem.isOffhand()) {
                this.setItemOffHand(inv, item, finalSlot);
            } else {
                inv.setItem(finalSlot, item);
            }
            ++slot;
        }
        inventoryPlayer.getPlayer().openInventory(inv);
        this.inventoryEditManager.getPlayers().add(inventoryPlayer);
    }

    public void clickOffHand(InventoryPlayer inventoryPlayer, Inventory inv, ItemStack clickedItem, int clickedSlot) {
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        if (!serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_9_R1)) {
            inventoryPlayer.getPlayer().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&cOffhand only works on 1.9+."));
            return;
        }
        String offhand = ItemUtils.getTagStringItem(this.plugin, clickedItem, "playerkits_offhand");
        if (offhand != null) {
            this.removeItemOffHand(inv, clickedItem, clickedSlot);
        } else {
            this.setItemOffHand(inv, clickedItem, clickedSlot);
        }
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; ++i) {
            String offhandCurrentItem;
            ItemStack item = contents[i];
            if (item == null || item.getType().equals((Object)Material.AIR) || clickedSlot == i || (offhandCurrentItem = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_offhand")) == null) continue;
            this.removeItemOffHand(inv, item, i);
        }
    }

    public void setItemOffHand(Inventory inv, ItemStack item, int slot) {
        ArrayList<String> offHandLore = new ArrayList<String>();
        offHandLore.add(" ");
        offHandLore.add(MessagesManager.getLegacyColoredMessage("&c&lRIGHT CLICK &cto remove from offhand"));
        ItemMeta meta = item.getItemMeta();
        List<String> itemLore = new ArrayList<String>();
        if (meta.hasLore()) {
            itemLore = meta.getLore();
        }
        itemLore.addAll(offHandLore);
        meta.setLore(itemLore);
        item.setItemMeta(meta);
        item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_offhand", "yes");
        inv.setItem(slot, item);
    }

    public void removeItemOffHand(Inventory inv, ItemStack item, int slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta.hasLore()) {
            List itemLore = meta.getLore();
            itemLore.remove(itemLore.size() - 1);
            itemLore.remove(itemLore.size() - 1);
            meta.setLore(itemLore);
        }
        item.setItemMeta(meta);
        item = ItemUtils.removeTagItem(this.plugin, item, "playerkits_offhand");
        inv.setItem(slot, item);
    }

    public void saveKitItems(InventoryPlayer inventoryPlayer) {
        Inventory inv = InventoryUtils.getTopInventory(inventoryPlayer.getPlayer());
        if (inv != null) {
            Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
            KitItemManager kitItemManager = this.plugin.getKitItemManager();
            ArrayList<KitItem> kitItems = new ArrayList<KitItem>();
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i <= 44; ++i) {
                ItemStack item = contents[i];
                if (item == null || item.getType().equals((Object)Material.AIR)) continue;
                KitItem kitItem = kitItemManager.createKitItemFromItemStack(item, kit.isSaveOriginalItems());
                String offhand = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_offhand");
                if (offhand != null) {
                    kitItem.setOffhand(true);
                    kitItem.removeOffHandFromEditInventory(this.plugin);
                }
                kitItem.setPreviewSlot(i);
                kitItems.add(kitItem);
            }
            kit.setItems(kitItems);
            this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
        }
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, int slot, ClickType clickType, InventoryClickEvent event) {
        if (event.getSlotType() == null || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().equals((Object)InventoryUtils.getTopInventory(inventoryPlayer.getPlayer()))) {
            if (slot >= 45 && slot <= 53) {
                event.setCancelled(true);
                if (slot == 45) {
                    this.inventoryEditManager.openInventory(inventoryPlayer);
                } else if (slot == 53) {
                    this.saveKitItems(inventoryPlayer);
                    inventoryPlayer.getPlayer().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&aKit Items saved."));
                }
            } else if (clickType.isRightClick()) {
                event.setCancelled(true);
                if (item != null) {
                    this.clickOffHand(inventoryPlayer, event.getClickedInventory(), item, slot);
                }
            }
        }
    }
}

