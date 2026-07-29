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
 */
package pk.ajneb97.managers.edit;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.edit.InventoryEditManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.InventoryItem;
import pk.ajneb97.utils.InventoryUtils;
import pk.ajneb97.utils.OtherUtils;

public class InventoryEditDisplayManager {
    private PlayerKits2 plugin;
    private InventoryEditManager inventoryEditManager;

    public InventoryEditDisplayManager(PlayerKits2 plugin, InventoryEditManager inventoryEditManager) {
        this.plugin = plugin;
        this.inventoryEditManager = inventoryEditManager;
    }

    public void openInventory(InventoryPlayer inventoryPlayer, String type) {
        inventoryPlayer.setInventoryName("edit_display_" + type);
        Inventory inv = Bukkit.createInventory(null, (int)27, (String)MessagesManager.getLegacyColoredMessage("&9Editing Kit"));
        ArrayList<Integer> slots = new ArrayList<Integer>();
        OtherUtils.addRangeToList(0, 11, slots);
        OtherUtils.addRangeToList(15, 26, slots);
        for (int i : slots) {
            if (OtherUtils.isLegacy()) {
                new InventoryItem(inv, i, Material.valueOf((String)"STAINED_GLASS_PANE")).dataValue((short)15).name("").ready();
                continue;
            }
            new InventoryItem(inv, i, Material.BLACK_STAINED_GLASS_PANE).name("").ready();
        }
        new InventoryItem(inv, 18, Material.ARROW).name("&eGo Back").ready();
        Material headMaterial = null;
        headMaterial = OtherUtils.isLegacy() ? Material.valueOf((String)"SKULL_ITEM") : Material.PLAYER_HEAD;
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("&7On this empty space you can place an");
        lore.add("&7already created custom item, or you can");
        lore.add("&7do so directly from the config of this kit.");
        new InventoryItem(inv, 12, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19").name("&6Place item here >>").lore(lore).ready();
        new InventoryItem(inv, 14, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==").name("&6<< Place item here").lore(lore).ready();
        Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
        KitItem kitItem = this.getKitDisplayItemFromType(kit, type);
        lore = new ArrayList();
        lore.add("&7You are currently editing the");
        if (type.equals("permission")) {
            lore.add("&6&lNo Permissions Display Item");
        } else if (type.equals("onetime")) {
            lore.add("&6&lOne Time Display Item");
        } else if (type.equals("cooldown")) {
            lore.add("&eSet &6&lCooldown Display Item");
        } else if (type.equals("requirements")) {
            lore.add("&6&lOne Time Requirements Display Item");
        } else if (type.startsWith("action")) {
            String actionType = type.split("_")[1];
            int actionSlot = Integer.parseInt(type.split("_")[2]);
            lore.add("&6&l" + actionType.toUpperCase() + " Action " + actionSlot + " Display Item");
        } else {
            lore.add("&6&lDefault Display Item");
        }
        new InventoryItem(inv, 0, Material.COMPASS).name("&6&lInfo").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click here if you made changes");
        lore.add("&7to the item, before leaving this");
        lore.add("&7inventory.");
        new InventoryItem(inv, 26, Material.EMERALD_BLOCK).name("&6&lSave Item").lore(lore).ready();
        if (kitItem != null && kitItem.getId() != null) {
            inv.setItem(13, this.plugin.getKitItemManager().createItemFromKitItem(kitItem, inventoryPlayer.getPlayer(), kit));
        }
        inventoryPlayer.getPlayer().openInventory(inv);
        this.inventoryEditManager.getPlayers().add(inventoryPlayer);
    }

    public void saveKitItem(InventoryPlayer inventoryPlayer) {
        Inventory inv = InventoryUtils.getTopInventory(inventoryPlayer.getPlayer());
        if (inv != null) {
            ItemStack item = inv.getItem(13);
            KitItem kitItem = null;
            if (item != null && !item.getType().equals((Object)Material.AIR)) {
                kitItem = this.plugin.getKitItemManager().createKitItemFromItemStack(item, false);
            }
            Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
            String type = inventoryPlayer.getInventoryName().replace("edit_display_", "");
            if (type.equals("permission")) {
                kit.setDisplayItemNoPermission(kitItem);
            } else if (type.equals("onetime")) {
                kit.setDisplayItemOneTime(kitItem);
            } else if (type.equals("cooldown")) {
                kit.setDisplayItemCooldown(kitItem);
            } else if (type.equals("requirements")) {
                kit.setDisplayItemOneTimeRequirements(kitItem);
            } else if (type.startsWith("action")) {
                String actionType = type.split("_")[1];
                int actionSlot = Integer.parseInt(type.split("_")[2]);
                KitAction kitAction = this.getKitActionsFromType(kit, actionType).get(actionSlot);
                kitAction.setDisplayItem(kitItem);
            } else {
                kit.setDisplayItemDefault(kitItem);
            }
            this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
        }
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, int slot, ClickType clickType, InventoryClickEvent event) {
        if (event.getSlotType() == null || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().equals((Object)InventoryUtils.getTopInventory(inventoryPlayer.getPlayer())) && slot != 13) {
            event.setCancelled(true);
            if (slot == 18) {
                if (inventoryPlayer.getInventoryName().startsWith("edit_display_action")) {
                    String type = inventoryPlayer.getInventoryName().replace("edit_display_action_", "");
                    String actionType = type.split("_")[0];
                    int actionSlot = Integer.parseInt(type.split("_")[1]);
                    this.inventoryEditManager.getInventoryEditActionsManager().getInventoryEditActionsEditManager().openInventory(inventoryPlayer, actionType, actionSlot);
                } else {
                    this.inventoryEditManager.openInventory(inventoryPlayer);
                }
            } else if (slot == 26) {
                this.saveKitItem(inventoryPlayer);
                inventoryPlayer.getPlayer().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&aDisplay item saved."));
            }
        }
    }

    public KitItem getKitDisplayItemFromType(Kit kit, String type) {
        if (type.startsWith("action")) {
            String actionType = type.split("_")[1];
            int actionSlot = Integer.parseInt(type.split("_")[2]);
            KitAction kitAction = this.getKitActionsFromType(kit, actionType).get(actionSlot);
            return kitAction.getDisplayItem();
        }
        KitItem kitItem = kit.getDisplayItemDefault();
        switch (type) {
            case "permission": {
                return kit.getDisplayItemNoPermission();
            }
            case "onetime": {
                return kit.getDisplayItemOneTime();
            }
            case "cooldown": {
                return kit.getDisplayItemCooldown();
            }
            case "requirements": {
                return kit.getDisplayItemOneTimeRequirements();
            }
        }
        return kitItem;
    }

    public ArrayList<KitAction> getKitActionsFromType(Kit kit, String type) {
        ArrayList<KitAction> actions = null;
        actions = type.equals("claim") ? kit.getClaimActions() : kit.getErrorActions();
        return actions;
    }
}

