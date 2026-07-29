/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package pk.ajneb97.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.MainConfigManager;
import pk.ajneb97.managers.InventoryRequirementsManager;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.KitsManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.PlayerDataManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.internal.GiveKitInstructions;
import pk.ajneb97.model.internal.KitPosition;
import pk.ajneb97.model.internal.KitVariable;
import pk.ajneb97.model.internal.PlayerKitsMessageResult;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.model.inventory.ItemKitInventory;
import pk.ajneb97.model.inventory.KitInventory;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.ItemUtils;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.OtherUtils;
import pk.ajneb97.utils.PlayerUtils;

public class InventoryManager {
    private PlayerKits2 plugin;
    private ArrayList<KitInventory> inventories;
    private ArrayList<InventoryPlayer> players;
    private InventoryRequirementsManager inventoryRequirementsManager;

    public InventoryManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.players = new ArrayList();
        this.inventoryRequirementsManager = new InventoryRequirementsManager(plugin, this);
    }

    public ArrayList<InventoryPlayer> getPlayers() {
        return this.players;
    }

    public ArrayList<KitInventory> getInventories() {
        return this.inventories;
    }

    public InventoryRequirementsManager getInventoryRequirementsManager() {
        return this.inventoryRequirementsManager;
    }

    public void setInventories(ArrayList<KitInventory> inventories) {
        this.inventories = inventories;
    }

    public KitInventory getInventory(String name) {
        for (KitInventory kitInventory : this.inventories) {
            if (!kitInventory.getName().equals(name)) continue;
            return kitInventory;
        }
        return null;
    }

    public InventoryPlayer getInventoryPlayer(Player player) {
        for (InventoryPlayer inventoryPlayer : this.players) {
            if (!inventoryPlayer.getPlayer().equals((Object)player)) continue;
            return inventoryPlayer;
        }
        return null;
    }

    public void removeInventoryPlayer(Player player) {
        for (int i = 0; i < this.players.size(); ++i) {
            if (!this.players.get(i).getPlayer().equals((Object)player)) continue;
            this.players.remove(i);
        }
    }

    public void openInventory(InventoryPlayer inventoryPlayer) {
        KitInventory kitInventory = this.getInventory(inventoryPlayer.getInventoryName());
        MainConfigManager mainConfigManager = this.plugin.getConfigsManager().getMainConfigManager();
        String title = kitInventory.getTitle();
        if (inventoryPlayer.getInventoryName().equals("buy_requirements_inventory") || inventoryPlayer.getInventoryName().equals("preview_inventory")) {
            title = title.replace("%kit%", inventoryPlayer.getKitName());
        }
        Inventory inv = mainConfigManager.isUseMiniMessage() ? MiniMessageUtils.createInventory(kitInventory.getSlots(), title) : Bukkit.createInventory(null, (int)kitInventory.getSlots(), (String)MessagesManager.getLegacyColoredMessage(title));
        List<ItemKitInventory> items = kitInventory.getItems();
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        KitsManager kitsManager = this.plugin.getKitsManager();
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();
        for (ItemKitInventory itemInventory : items) {
            for (int slot : itemInventory.getSlots()) {
                String openInventory;
                String type = itemInventory.getType();
                if (type != null && type.startsWith("kit: ")) {
                    this.setKit(type.replace("kit: ", ""), inventoryPlayer.getPlayer(), inv, slot, kitsManager, playerDataManager, kitItemManager, null);
                    continue;
                }
                ItemStack item = kitItemManager.createItemFromKitItem(itemInventory.getItem(), inventoryPlayer.getPlayer(), null);
                if (inventoryPlayer.getInventoryName().equals("buy_requirements_inventory")) {
                    this.inventoryRequirementsManager.configureRequirementsItem(item, inventoryPlayer.getKitName(), inventoryPlayer.getPlayer());
                    if (type != null) {
                        item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_buy", type);
                    }
                }
                if ((openInventory = itemInventory.getOpenInventory()) != null) {
                    item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_open_inventory", openInventory);
                }
                item = this.setItemActions(itemInventory, item);
                inv.setItem(slot, item);
            }
        }
        if (inventoryPlayer.getInventoryName().equals("preview_inventory")) {
            this.setKitPreviewItems(inv, inventoryPlayer, kitInventory);
        }
        inventoryPlayer.getPlayer().openInventory(inv);
        this.players.add(inventoryPlayer);
    }

    private ItemStack setItemActions(ItemKitInventory itemInventory, ItemStack item) {
        List<String> clickActions = itemInventory.getClickActions();
        if (clickActions != null && !clickActions.isEmpty()) {
            String actionsList = "";
            for (int i = 0; i < clickActions.size(); ++i) {
                actionsList = i == clickActions.size() - 1 ? actionsList + clickActions.get(i) : actionsList + clickActions.get(i) + "|";
            }
            item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_item_actions", actionsList);
        }
        return item;
    }

    public void setKitPreviewItems(Inventory inv, InventoryPlayer inventoryPlayer, KitInventory kitInventory) {
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        KitsManager kitsManager = this.plugin.getKitsManager();
        Kit kit = kitsManager.getKitByName(inventoryPlayer.getKitName());
        if (kit == null) {
            return;
        }
        ArrayList<KitItem> allItems = new ArrayList<KitItem>();
        allItems.addAll(kit.getItems());
        for (KitAction kitAction : kit.getClaimActions()) {
            KitItem kitItem = kitAction.getDisplayItem();
            if (kitItem == null) continue;
            allItems.add(kitItem);
        }
        int slot = 0;
        for (KitItem kitItem : allItems) {
            ItemStack item = kitItemManager.createItemFromKitItem(kitItem, inventoryPlayer.getPlayer(), kit);
            if (kitItem.getPreviewSlot() != -1) {
                inv.setItem(kitItem.getPreviewSlot(), item);
            } else {
                inv.setItem(slot, item);
            }
            if (++slot < kitInventory.getSlots()) continue;
            break;
        }
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, ClickType clickType) {
        String openInventory;
        String kitName = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_kit");
        if (kitName != null) {
            this.clickOnKitItem(inventoryPlayer, kitName, clickType);
            return;
        }
        String itemActions = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_item_actions");
        if (itemActions != null) {
            this.clickOnActionItem(inventoryPlayer, itemActions);
        }
        if ((openInventory = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_open_inventory")) != null) {
            this.clickOnOpenInventoryItem(inventoryPlayer, openInventory);
            return;
        }
        if (inventoryPlayer.getInventoryName().equals("buy_requirements_inventory")) {
            String buyTag = ItemUtils.getTagStringItem(this.plugin, item, "playerkits_buy");
            if (buyTag == null) {
                return;
            }
            if (buyTag.equals("yes") || buyTag.equals("buy_yes")) {
                this.inventoryRequirementsManager.requirementsInventoryBuy(inventoryPlayer);
            } else {
                this.inventoryRequirementsManager.requirementsInventoryCancel(inventoryPlayer);
            }
        }
    }

    public void clickOnKitItem(InventoryPlayer inventoryPlayer, String kitName, ClickType clickType) {
        MainConfigManager mainConfigManager = this.plugin.getConfigsManager().getMainConfigManager();
        Player player = inventoryPlayer.getPlayer();
        FileConfiguration messagesConfig = this.plugin.getConfigsManager().getMessagesConfigManager().getConfig();
        MessagesManager msgManager = this.plugin.getMessagesManager();
        if (clickType.equals((Object)ClickType.RIGHT)) {
            if (!mainConfigManager.isKitPreview()) {
                return;
            }
            Kit kit = this.plugin.getKitsManager().getKitByName(kitName);
            if (kit.isPermissionRequired() && mainConfigManager.isKitPreviewRequiresKitPermission() && !kit.playerHasPermission((CommandSender)player)) {
                msgManager.sendMessage((CommandSender)player, messagesConfig.getString("cantPreviewError"), true);
                return;
            }
            inventoryPlayer.setPreviousInventoryName(inventoryPlayer.getInventoryName());
            inventoryPlayer.setInventoryName("preview_inventory");
            inventoryPlayer.setKitName(kitName);
            this.openInventory(inventoryPlayer);
            return;
        }
        PlayerKitsMessageResult result = this.plugin.getKitsManager().giveKit(player, kitName, new GiveKitInstructions());
        if (result.isError()) {
            msgManager.sendMessage((CommandSender)player, result.getMessage(), true);
            return;
        }
        if (result.isProceedToBuy()) {
            inventoryPlayer.setPreviousInventoryName(inventoryPlayer.getInventoryName());
            inventoryPlayer.setInventoryName("buy_requirements_inventory");
            inventoryPlayer.setKitName(kitName);
            this.openInventory(inventoryPlayer);
            return;
        }
        msgManager.sendMessage((CommandSender)player, messagesConfig.getString("kitReceived").replace("%kit%", kitName), true);
        if (mainConfigManager.isCloseInventoryOnClaim()) {
            player.closeInventory();
            return;
        }
        this.openInventory(inventoryPlayer);
    }

    public void clickOnOpenInventoryItem(InventoryPlayer inventoryPlayer, String openInventory) {
        if (openInventory.equals("previous")) {
            inventoryPlayer.setInventoryName(inventoryPlayer.getPreviousInventoryName());
        } else {
            inventoryPlayer.setPreviousInventoryName(inventoryPlayer.getInventoryName());
            inventoryPlayer.setInventoryName(openInventory);
        }
        this.openInventory(inventoryPlayer);
    }

    public void clickOnActionItem(InventoryPlayer inventoryPlayer, String itemActions) {
        String[] sep = itemActions.split("\\|");
        KitsManager kitsManager = this.plugin.getKitsManager();
        Player player = inventoryPlayer.getPlayer();
        for (String action : sep) {
            kitsManager.executeAction(player, action);
        }
    }

    public void setKit(String kitName, Player player, Inventory inv, int slot, KitsManager kitsManager, PlayerDataManager playerDataManager, KitItemManager kitItemManager, ItemStack currentItem) {
        Kit kit = kitsManager.getKitByName(kitName);
        if (kit == null) {
            return;
        }
        String currentStatus = "default";
        if (currentItem != null && (currentStatus = ItemUtils.getTagStringItem(this.plugin, currentItem, "playerkits_kit_status")) == null) {
            currentStatus = "default";
        }
        String newStatus = "default";
        ArrayList<KitVariable> variablesToReplace = new ArrayList<KitVariable>();
        variablesToReplace.add(new KitVariable("%kit_name%", kit.getName()));
        KitItem kitItem = null;
        if (!kit.playerHasPermission((CommandSender)player)) {
            kitItem = kit.getDisplayItemNoPermission();
            newStatus = "no_permission";
        } else {
            String timeStringMillisDif;
            if (kit.isOneTime() && !PlayerUtils.isPlayerKitsAdmin((CommandSender)player) && playerDataManager.isKitOneTime(player, kit.getName())) {
                kitItem = kit.getDisplayItemOneTime();
                newStatus = "one_time";
            }
            if (kit.getRequirements() != null && kit.getRequirements().isOneTimeRequirements() && playerDataManager.isKitBought(player, kit.getName())) {
                kitItem = kit.getDisplayItemOneTimeRequirements();
                newStatus = "one_time_requirements";
            }
            long playerCooldown = playerDataManager.getKitCooldown(player, kit.getName());
            if (kit.getCooldown() != 0 && !PlayerUtils.isPlayerKitsAdmin((CommandSender)player) && !(timeStringMillisDif = playerDataManager.getKitCooldownString(playerCooldown)).isEmpty()) {
                kitItem = kit.getDisplayItemCooldown();
                variablesToReplace.add(new KitVariable("%time%", timeStringMillisDif));
                newStatus = "cooldown";
            }
        }
        if (kitItem == null) {
            kitItem = kit.getDisplayItemDefault();
            newStatus = "default";
        }
        kitItem = kitItem.clone();
        boolean useMiniMessage = this.plugin.getConfigsManager().getMainConfigManager().isUseMiniMessage();
        if (newStatus.equals(currentStatus) && currentItem != null) {
            List<String> lore;
            kitItemManager.replaceVariables(kitItem, variablesToReplace, player);
            ItemMeta meta = currentItem.getItemMeta();
            String name = kitItem.getName();
            if (name != null) {
                if (useMiniMessage) {
                    MiniMessageUtils.setItemName(meta, name);
                } else {
                    meta.setDisplayName(MessagesManager.getLegacyColoredMessage(name));
                }
            }
            if ((lore = kitItem.getLore()) != null) {
                ArrayList<String> loreCopy = new ArrayList<String>(lore);
                if (useMiniMessage) {
                    MiniMessageUtils.setItemLore(meta, lore, player, this.plugin);
                } else {
                    for (int i = 0; i < loreCopy.size(); ++i) {
                        String line = OtherUtils.replaceGlobalVariables((String)loreCopy.get(i), player, this.plugin);
                        loreCopy.set(i, MessagesManager.getLegacyColoredMessage(line));
                    }
                    meta.setLore(loreCopy);
                }
            }
            currentItem.setItemMeta(meta);
        } else {
            kitItemManager.replaceVariables(kitItem, variablesToReplace, player);
            ItemStack item = kitItemManager.createItemFromKitItem(kitItem, player, kit);
            item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_kit", kitName);
            item = ItemUtils.setTagStringItem(this.plugin, item, "playerkits_kit_status", newStatus);
            inv.setItem(slot, item);
        }
    }

    public KitPosition getKitPositionByKitName(String kitName) {
        for (KitInventory kitInventory : this.inventories) {
            for (ItemKitInventory itemKitInventory : kitInventory.getItems()) {
                if (itemKitInventory.getType() == null || !itemKitInventory.getType().equals("kit: " + kitName)) continue;
                return new KitPosition(itemKitInventory.getSlots().get(0), kitInventory.getName());
            }
        }
        return null;
    }

    public void removeKitFromInventory(String kitName) {
        for (KitInventory inventory : this.inventories) {
            List<ItemKitInventory> items = inventory.getItems();
            for (int i = 0; i < items.size(); ++i) {
                ItemKitInventory item = items.get(i);
                if (item.getType() == null || !item.getType().equals("kit: " + kitName)) continue;
                items.remove(i);
                --i;
            }
        }
    }
}

