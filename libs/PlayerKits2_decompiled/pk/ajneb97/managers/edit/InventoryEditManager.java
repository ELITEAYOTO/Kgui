/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package pk.ajneb97.managers.edit;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.edit.InventoryEditActionsManager;
import pk.ajneb97.managers.edit.InventoryEditDisplayManager;
import pk.ajneb97.managers.edit.InventoryEditKitItemsManager;
import pk.ajneb97.managers.edit.InventoryEditPositionManager;
import pk.ajneb97.managers.edit.InventoryEditRequirementsManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.internal.KitPosition;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.InventoryItem;
import pk.ajneb97.utils.OtherUtils;

public class InventoryEditManager {
    private PlayerKits2 plugin;
    private ArrayList<InventoryPlayer> players;
    private InventoryEditActionsManager inventoryEditActionsManager;
    private InventoryEditDisplayManager inventoryEditDisplayManager;
    private InventoryEditKitItemsManager inventoryEditKitItemsManager;
    private InventoryEditPositionManager inventoryEditPositionManager;
    private InventoryEditRequirementsManager inventoryEditRequirementsManager;

    public InventoryEditManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.players = new ArrayList();
        this.inventoryEditActionsManager = new InventoryEditActionsManager(plugin, this);
        this.inventoryEditDisplayManager = new InventoryEditDisplayManager(plugin, this);
        this.inventoryEditKitItemsManager = new InventoryEditKitItemsManager(plugin, this);
        this.inventoryEditPositionManager = new InventoryEditPositionManager(plugin, this);
        this.inventoryEditRequirementsManager = new InventoryEditRequirementsManager(plugin, this);
    }

    public InventoryPlayer getInventoryPlayer(Player player) {
        for (InventoryPlayer inventoryPlayer : this.players) {
            if (!inventoryPlayer.getPlayer().equals((Object)player)) continue;
            return inventoryPlayer;
        }
        return null;
    }

    public InventoryEditActionsManager getInventoryEditActionsManager() {
        return this.inventoryEditActionsManager;
    }

    public InventoryEditDisplayManager getInventoryEditDisplayManager() {
        return this.inventoryEditDisplayManager;
    }

    public InventoryEditKitItemsManager getInventoryEditKitItemsManager() {
        return this.inventoryEditKitItemsManager;
    }

    public InventoryEditPositionManager getInventoryEditPositionManager() {
        return this.inventoryEditPositionManager;
    }

    public ArrayList<InventoryPlayer> getPlayers() {
        return this.players;
    }

    public void removeInventoryPlayer(Player player) {
        for (int i = 0; i < this.players.size(); ++i) {
            if (!this.players.get(i).getPlayer().equals((Object)player)) continue;
            this.players.remove(i);
        }
    }

    public void openInventory(InventoryPlayer inventoryPlayer) {
        inventoryPlayer.setInventoryName("edit_main_inventory");
        Inventory inv = Bukkit.createInventory(null, (int)45, (String)MessagesManager.getLegacyColoredMessage("&9Editing Kit"));
        Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
        List<String> lore = new ArrayList<String>();
        lore.add("&7Click to define the position of the display");
        lore.add("&7item of this kit in the Inventory.");
        lore.add("");
        String slot = "none";
        String inventoryName = "none";
        KitPosition kitPosition = this.plugin.getInventoryManager().getKitPositionByKitName(kit.getName());
        if (kitPosition != null) {
            slot = kitPosition.getSlot() + "";
            inventoryName = kitPosition.getInventoryName();
        }
        lore.add("&7Current Slot: &a" + slot);
        lore.add("&7Current Inventory: &a" + inventoryName);
        new InventoryItem(inv, 10, Material.DROPPER).name("&eSet &6&lSlot").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to define the cooldown of");
        lore.add("&7the kit.");
        lore.add("");
        lore.add("&7Current Cooldown: &a" + kit.getCooldown() + "(s)");
        if (OtherUtils.isLegacy()) {
            new InventoryItem(inv, 11, Material.valueOf((String)"WATCH")).name("&eSet &6&lCooldown").lore(lore).ready();
        } else {
            new InventoryItem(inv, 11, Material.CLOCK).name("&eSet &6&lCooldown").lore(lore).ready();
        }
        lore = new ArrayList();
        lore.add("&7Click to enable/disable whether this kit");
        lore.add("&7needs permissions to be claimed.");
        lore.add("&7The permission will be:");
        lore.add("&eplayerkits.kit." + kit.getName());
        lore.add("");
        String permission = "&cNO";
        if (kit.isPermissionRequired()) {
            permission = "&aYES";
        }
        lore.add("&7Current Status: " + permission);
        new InventoryItem(inv, 12, Material.REDSTONE_BLOCK).name("&eSet &6&lPermission Required").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to enable/disable whether this kit");
        lore.add("&7should be claimed just one time.");
        lore.add("");
        String oneTime = "&cNO";
        if (kit.isOneTime()) {
            oneTime = "&aYES";
        }
        lore.add("&7Current Status: " + oneTime);
        new InventoryItem(inv, 20, Material.GHAST_TEAR).name("&eSet &6&lOne Time").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to enable/disable whether armor");
        lore.add("&7should be equipped automatically when");
        lore.add("&7this kit is claimed.");
        lore.add("");
        String autoArmor = "&cNO";
        if (kit.isAutoArmor()) {
            autoArmor = "&aYES";
        }
        lore.add("&7Current Status: " + autoArmor);
        new InventoryItem(inv, 21, Material.IRON_CHESTPLATE).name("&eSet &6&lAuto Armor").lore(lore).ready();
        Material headMaterial = null;
        headMaterial = OtherUtils.isLegacy() ? Material.valueOf((String)"SKULL_ITEM") : Material.PLAYER_HEAD;
        lore = new ArrayList();
        lore.add("&7Click to edit the default kit display");
        lore.add("&7item.");
        lore.add("");
        lore.add("&7Present: " + (kit.getDisplayItemDefault() != null ? "&aYES" : "&cNO"));
        new InventoryItem(inv, 38, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWNkZTUxNGFmYTE5NGQ1Y2JkMDQ3N2MwNWI3Y2IxODVmZjFkZmZkMGMyZmFkZmFlMWE1YmI4MDY0ODU2Yzg5MiJ9fX0=").name("&eSet &6&lDefault Display Item").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the kit display item when");
        lore.add("&7player doesn't have the permissions to");
        lore.add("&7claim it.");
        lore.add("");
        lore.add("&7Present: " + (kit.getDisplayItemNoPermission() != null ? "&aYES" : "&cNO"));
        new InventoryItem(inv, 39, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzdkNDk5YTlhZjUyOTllZGE0Y2NkYWMyMDE5ZWZlN2YyNDk2MWYzZTFmY2U3Njk0Y2I2ODIwMjlkMjllOWVhMSJ9fX0=").name("&eSet &6&lNo Permissions Display Item").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the kit display item when");
        lore.add("&7one time option is enabled and the player");
        lore.add("&7has already claimed it.");
        lore.add("");
        lore.add("&7Present: " + (kit.getDisplayItemOneTime() != null ? "&aYES" : "&cNO"));
        new InventoryItem(inv, 40, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzU3NDc2NmUzZGVjNDkwNGZhNWFhMTU5MjA4ZGFlYzExYzYzYzVlMzI2MTU3YzM2NWViMTY5MDFiNjFmNjQ1YiJ9fX0=").name("&eSet &6&lOne Time Display Item").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the kit display item when");
        lore.add("&7the player is on cooldown.");
        lore.add("");
        lore.add("&7Present: " + (kit.getDisplayItemCooldown() != null ? "&aYES" : "&cNO"));
        new InventoryItem(inv, 41, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODg3YTc1ZjZjYmNjOGUxN2I4ZmJmYWVlMzM3MGZlZjAyMWMyNWY3MDM1YjI1ZDRjNjU3OTZlZjMzODljZWYwMCJ9fX0=").name("&eSet &6&lCooldown Display Item").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the kit display item when");
        lore.add("&7one time requirements option is enabled and");
        lore.add("&7the player has already accomplished the");
        lore.add("&7requirements.");
        lore.add("");
        lore.add("&7Present: " + (kit.getDisplayItemOneTimeRequirements() != null ? "&aYES" : "&cNO"));
        new InventoryItem(inv, 42, headMaterial).setSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU4NmJlNTQ5OWViNDI0NWE0NjFiZGNiODZmZjE2M2M4NTVjMTgyODFmOTcxMDU0MmIxN2ZkMjhiYWU0MjQzYiJ9fX0=").name("&eSet &6&lOne Time Requirements Display Item").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the kit items.");
        lore.add("");
        lore.add("&7Current Items:");
        int max = 20;
        for (KitItem kitItem : kit.getItems()) {
            if (kitItem.getOriginalItem() != null) {
                ItemStack originalItem = kitItem.getOriginalItem();
                lore.add(MessagesManager.getLegacyColoredMessage("&8- &fx" + originalItem.getAmount() + " " + originalItem.getType()));
            } else {
                lore.add(MessagesManager.getLegacyColoredMessage("&8- &fx" + kitItem.getAmount() + " " + kitItem.getId()));
            }
            if (--max > 0) continue;
            break;
        }
        new InventoryItem(inv, 14, Material.DIAMOND).name("&eSet &6&lKit Items").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the kit requirements.");
        new InventoryItem(inv, 15, Material.REDSTONE).name("&eSet &6&lRequirements").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the actions to execute");
        lore.add("&7when the player claims the kit.");
        lore.add("");
        lore.add("&7Current Actions:");
        lore = this.setActionItemLore(kit.getClaimActions(), lore);
        new InventoryItem(inv, 23, Material.IRON_INGOT).name("&eSet &6&lClaim Actions").lore(lore).ready();
        lore = new ArrayList();
        lore.add("&7Click to edit the actions to execute");
        lore.add("&7when there is an error when claiming");
        lore.add("&7the kit.");
        lore.add("");
        lore.add("&7Current Actions:");
        lore = this.setActionItemLore(kit.getErrorActions(), lore);
        new InventoryItem(inv, 24, Material.NETHER_BRICK).name("&eSet &6&lError Actions").lore(lore).ready();
        inventoryPlayer.getPlayer().openInventory(inv);
        this.players.add(inventoryPlayer);
    }

    public List<String> setActionItemLore(ArrayList<KitAction> actions, List<String> lore) {
        int max = 20;
        if (actions.isEmpty()) {
            lore.add(MessagesManager.getLegacyColoredMessage("&cNONE"));
        } else {
            for (KitAction kitAction : actions) {
                int i;
                String actionLine = kitAction.getAction();
                ArrayList<String> separatedActionLine = new ArrayList<String>();
                int currentPos = 0;
                for (i = 0; i < actionLine.length(); ++i) {
                    String m;
                    if (currentPos >= 35 && actionLine.charAt(i) == ' ') {
                        m = actionLine.substring(i - currentPos, i);
                        currentPos = 0;
                        separatedActionLine.add(m);
                    } else {
                        ++currentPos;
                    }
                    if (i != actionLine.length() - 1) continue;
                    m = actionLine.substring(i - currentPos + 1, actionLine.length());
                    separatedActionLine.add(m);
                }
                for (i = 0; i < separatedActionLine.size(); ++i) {
                    if (i == 0) {
                        lore.add(MessagesManager.getLegacyColoredMessage("&8- &f") + (String)separatedActionLine.get(i));
                        continue;
                    }
                    lore.add(MessagesManager.getLegacyColoredMessage("&f") + (String)separatedActionLine.get(i));
                }
                if (--max > 0) continue;
                break;
            }
        }
        return lore;
    }

    public void setPermissionRequired(InventoryPlayer inventoryPlayer) {
        Kit kit;
        kit.setPermissionRequired(!(kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName())).isPermissionRequired());
        this.openInventory(inventoryPlayer);
        this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
    }

    public void setOneTime(InventoryPlayer inventoryPlayer) {
        Kit kit;
        kit.setOneTime(!(kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName())).isOneTime());
        this.openInventory(inventoryPlayer);
        this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
    }

    public void setAutoArmor(InventoryPlayer inventoryPlayer) {
        Kit kit;
        kit.setAutoArmor(!(kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName())).isAutoArmor());
        this.openInventory(inventoryPlayer);
        this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
    }

    public void clickCooldown(InventoryPlayer inventoryPlayer) {
        Player player = inventoryPlayer.getPlayer();
        player.sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&7Write the new cooldown of the kit (in seconds)."));
        player.closeInventory();
        inventoryPlayer.setInventoryName("edit_chat_cooldown");
        this.players.add(inventoryPlayer);
    }

    public void setCooldown(InventoryPlayer inventoryPlayer, String message) {
        Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
        Player player = inventoryPlayer.getPlayer();
        try {
            int cooldown = Integer.parseInt(message);
            if (cooldown >= 0) {
                kit.setCooldown(cooldown);
                this.removeInventoryPlayer(inventoryPlayer.getPlayer());
                this.openInventory(inventoryPlayer);
                this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
                return;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        player.sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + "&cYou must use a valid number."));
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, int slot, ClickType clickType) {
        String inventory = inventoryPlayer.getInventoryName();
        if (inventory.equals("edit_main_inventory")) {
            switch (slot) {
                case 10: {
                    this.inventoryEditPositionManager.openInventory(inventoryPlayer, "main_inventory");
                    break;
                }
                case 11: {
                    this.clickCooldown(inventoryPlayer);
                    break;
                }
                case 12: {
                    this.setPermissionRequired(inventoryPlayer);
                    break;
                }
                case 14: {
                    this.inventoryEditKitItemsManager.openInventory(inventoryPlayer);
                    break;
                }
                case 15: {
                    this.inventoryEditRequirementsManager.openInventory(inventoryPlayer);
                    break;
                }
                case 20: {
                    this.setOneTime(inventoryPlayer);
                    break;
                }
                case 21: {
                    this.setAutoArmor(inventoryPlayer);
                    break;
                }
                case 23: {
                    this.inventoryEditActionsManager.openInventory(inventoryPlayer, "claim");
                    break;
                }
                case 24: {
                    this.inventoryEditActionsManager.openInventory(inventoryPlayer, "error");
                    break;
                }
                case 38: {
                    this.inventoryEditDisplayManager.openInventory(inventoryPlayer, "default");
                    break;
                }
                case 39: {
                    this.inventoryEditDisplayManager.openInventory(inventoryPlayer, "permission");
                    break;
                }
                case 40: {
                    this.inventoryEditDisplayManager.openInventory(inventoryPlayer, "onetime");
                    break;
                }
                case 41: {
                    this.inventoryEditDisplayManager.openInventory(inventoryPlayer, "cooldown");
                    break;
                }
                case 42: {
                    this.inventoryEditDisplayManager.openInventory(inventoryPlayer, "requirements");
                }
            }
        } else if (inventory.startsWith("edit_actions_")) {
            this.inventoryEditActionsManager.clickInventory(inventoryPlayer, item, slot, clickType);
        } else if (inventory.startsWith("edit_action_slot_")) {
            this.inventoryEditActionsManager.getInventoryEditActionsEditManager().clickInventory(inventoryPlayer, item, slot, clickType);
        } else if (inventory.equals("edit_requirements")) {
            this.inventoryEditRequirementsManager.clickInventory(inventoryPlayer, item, slot, clickType);
        }
    }

    public void writeChat(InventoryPlayer inventoryPlayer, String message) {
        String inventory = inventoryPlayer.getInventoryName();
        if (inventory.equals("edit_chat_cooldown")) {
            this.setCooldown(inventoryPlayer, message);
        } else if (inventory.startsWith("edit_chat_add_action_")) {
            this.inventoryEditActionsManager.addAction(inventoryPlayer, message);
        } else if (inventory.startsWith("edit_chat_action_slot_")) {
            this.inventoryEditActionsManager.getInventoryEditActionsEditManager().editAction(inventoryPlayer, message);
        } else if (inventory.equals("edit_chat_price")) {
            this.inventoryEditRequirementsManager.setPrice(inventoryPlayer, message);
        }
    }
}

