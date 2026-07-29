/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package pk.ajneb97.managers;

import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.MainConfigManager;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.PlayerDataManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.KitRequirements;
import pk.ajneb97.model.internal.GiveKitInstructions;
import pk.ajneb97.model.internal.PlayerKitsMessageResult;
import pk.ajneb97.model.inventory.KitInventory;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.ActionUtils;
import pk.ajneb97.utils.OtherUtils;
import pk.ajneb97.utils.PlayerUtils;

public class KitsManager {
    private PlayerKits2 plugin;
    private ArrayList<Kit> kits;

    public KitsManager(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public PlayerKits2 getPlugin() {
        return this.plugin;
    }

    public void setPlugin(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public ArrayList<Kit> getKits() {
        return this.kits;
    }

    public void setKits(ArrayList<Kit> kits) {
        this.kits = kits;
    }

    public Kit getKitByName(String name) {
        for (Kit kit : this.kits) {
            if (!kit.getName().equals(name)) continue;
            return kit;
        }
        return null;
    }

    public void removeKit(String name) {
        for (int i = 0; i < this.kits.size(); ++i) {
            if (!this.kits.get(i).getName().equals(name)) continue;
            this.kits.remove(i);
            return;
        }
    }

    public void createKit(String kitName, Player player, boolean saveOriginalItems) {
        Kit kit = this.getKitByName(kitName);
        FileConfiguration messagesFile = this.plugin.getConfigsManager().getMessagesConfigManager().getConfig();
        MainConfigManager mainConfigManager = this.plugin.getConfigsManager().getMainConfigManager();
        MessagesManager msgManager = this.plugin.getMessagesManager();
        if (kit != null) {
            msgManager.sendMessage((CommandSender)player, messagesFile.getString("kitAlreadyExists").replace("%kit%", kitName), true);
            return;
        }
        ItemStack[] inventoryContents = PlayerUtils.getAllInventoryContents(player);
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        ArrayList<KitItem> items = new ArrayList<KitItem>();
        boolean hasArmor = false;
        for (int i = 0; i < inventoryContents.length; ++i) {
            ItemStack item = inventoryContents[i];
            if (item == null || item.getType().equals((Object)Material.AIR)) continue;
            KitItem kitItem = kitItemManager.createKitItemFromItemStack(item, saveOriginalItems);
            if (i >= 36 && i <= 39) {
                hasArmor = true;
            }
            if (i == 40) {
                kitItem.setOffhand(true);
            }
            items.add(kitItem);
        }
        if (items.size() == 0) {
            msgManager.sendMessage((CommandSender)player, messagesFile.getString("inventoryEmpty"), true);
            return;
        }
        kit = new Kit(kitName);
        kit.setItems(items);
        kit.setDefaults(mainConfigManager.getNewKitDefault());
        kit.setAutoArmor(hasArmor);
        kit.setSaveOriginalItems(saveOriginalItems);
        this.kits.add(kit);
        this.plugin.getConfigsManager().getKitsConfigManager().saveConfig(kit);
        msgManager.sendMessage((CommandSender)player, messagesFile.getString("kitCreated").replace("%kit%", kitName), true);
        InventoryManager inventoryManager = this.plugin.getInventoryManager();
        String newKitDefaultInventory = mainConfigManager.getNewKitDefaultInventory();
        KitInventory inventory = inventoryManager.getInventory(newKitDefaultInventory);
        if (inventory != null) {
            int resultSlot = inventory.addKitItemOnFirstEmptySlot(kitName);
            if (resultSlot == -1) {
                msgManager.sendMessage((CommandSender)player, messagesFile.getString("kitNotAddedToInventory"), true);
            } else {
                msgManager.sendMessage((CommandSender)player, messagesFile.getString("kitAddedToInventory").replace("%inventory%", newKitDefaultInventory).replace("%slot%", resultSlot + ""), true);
                this.plugin.getConfigsManager().getInventoryConfigManager().saveKitItemOnConfig(newKitDefaultInventory, resultSlot, kitName);
            }
        }
    }

    public void deleteKit(String kitName, CommandSender sender) {
        FileConfiguration messagesFile = this.plugin.getConfigsManager().getMessagesConfigManager().getConfig();
        MessagesManager msgManager = this.plugin.getMessagesManager();
        if (this.getKitByName(kitName) == null) {
            msgManager.sendMessage(sender, messagesFile.getString("kitDoesNotExists").replace("%kit%", kitName), true);
            return;
        }
        this.removeKit(kitName);
        this.plugin.getConfigsManager().getKitsConfigManager().removeKitFile(kitName);
        this.plugin.getInventoryManager().removeKitFromInventory(kitName);
        this.plugin.getConfigsManager().getInventoryConfigManager().save();
        msgManager.sendMessage(sender, messagesFile.getString("kitDeleted").replace("%kit%", kitName), true);
    }

    public PlayerKitsMessageResult giveKit(Player player, String kitName, GiveKitInstructions giveKitInstructions) {
        Kit kit = this.getKitByName(kitName);
        FileConfiguration messagesFile = this.plugin.getConfigsManager().getMessagesConfigManager().getConfig();
        FileConfiguration configFile = this.plugin.getConfigsManager().getMainConfigManager().getConfig();
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();
        MessagesManager msgManager = this.plugin.getMessagesManager();
        if (kit == null) {
            return PlayerKitsMessageResult.error(messagesFile.getString("kitDoesNotExists").replace("%kit%", kitName));
        }
        if (!giveKitInstructions.isFromCommand()) {
            long currentMillis;
            long millisDif;
            String timeStringMillisDif;
            if (!giveKitInstructions.isIgnorePermission() && !kit.playerHasPermission((CommandSender)player)) {
                this.sendKitActions(kit.getErrorActions(), player, false);
                return PlayerKitsMessageResult.error(messagesFile.getString("kitNoPermissions"));
            }
            if (kit.isOneTime() && !PlayerUtils.isPlayerKitsAdmin((CommandSender)player) && !PlayerUtils.hasOneTimeBypassPermission((CommandSender)player) && playerDataManager.isKitOneTime(player, kit.getName())) {
                this.sendKitActions(kit.getErrorActions(), player, false);
                return PlayerKitsMessageResult.error(messagesFile.getString("oneTimeError"));
            }
            long playerCooldown = playerDataManager.getKitCooldown(player, kit.getName());
            if (!(kit.getCooldown() == 0 || PlayerUtils.isPlayerKitsAdmin((CommandSender)player) || PlayerUtils.hasCooldownBypassPermission((CommandSender)player) || (timeStringMillisDif = OtherUtils.getTime((millisDif = playerCooldown - (currentMillis = System.currentTimeMillis())) / 1000L, msgManager)).isEmpty())) {
                this.sendKitActions(kit.getErrorActions(), player, false);
                return PlayerKitsMessageResult.error(messagesFile.getString("cooldownError").replace("%time%", timeStringMillisDif));
            }
            KitRequirements kitRequirements = kit.getRequirements();
            if (!(giveKitInstructions.isIgnoreRequirements() || kitRequirements == null || kitRequirements.getPrice() == 0.0 && kitRequirements.getExtraRequirements().isEmpty() || kitRequirements.isOneTimeRequirements() && playerDataManager.isKitBought(player, kit.getName()))) {
                if (!giveKitInstructions.isRequirementsSatisfied()) {
                    PlayerKitsMessageResult result = PlayerKitsMessageResult.success();
                    result.setProceedToBuy(true);
                    return result;
                }
                if (!this.passPrice(kitRequirements.getPrice(), player)) {
                    this.sendKitActions(kit.getErrorActions(), player, false);
                    return PlayerKitsMessageResult.error(messagesFile.getString("requirementsError"));
                }
                List<String> requirementsConditions = kitRequirements.getExtraRequirements();
                if (this.plugin.getDependencyManager().isPlaceholderAPI()) {
                    for (String condition : requirementsConditions) {
                        boolean passCondition = PlayerUtils.passCondition(player, condition);
                        if (passCondition) continue;
                        this.sendKitActions(kit.getErrorActions(), player, false);
                        return PlayerKitsMessageResult.error(messagesFile.getString("requirementsError"));
                    }
                }
            }
        }
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        ArrayList<KitItem> items = kit.getItems();
        int usedSlots = PlayerUtils.getUsedSlots(player);
        int freeSlots = 36 - usedSlots;
        int inventoryKitItems = 0;
        KitItem itemHelmet = null;
        KitItem itemChestplate = null;
        KitItem itemLeggings = null;
        KitItem itemBoots = null;
        KitItem itemOffhand = null;
        boolean clearInventory = kit.isClearInventory();
        PlayerInventory playerInventory = player.getInventory();
        for (KitItem kitItem : items) {
            if (kit.isAutoArmor()) {
                String id = kitItem.getId();
                if (kitItem.getOriginalItem() != null) {
                    id = kitItem.getOriginalItem().getType().name();
                }
                if ((id.contains("_HELMET") || id.contains("PLAYER_HEAD") || id.contains("SKULL_ITEM")) && itemHelmet == null) {
                    if (playerInventory.getHelmet() == null || playerInventory.getHelmet().getType().equals((Object)Material.AIR) || clearInventory) {
                        itemHelmet = kitItem;
                        ++freeSlots;
                        continue;
                    }
                } else if ((id.contains("_CHESTPLATE") || id.contains("ELYTRA")) && itemChestplate == null) {
                    if (playerInventory.getChestplate() == null || playerInventory.getChestplate().getType().equals((Object)Material.AIR) || clearInventory) {
                        itemChestplate = kitItem;
                        ++freeSlots;
                        continue;
                    }
                } else if (id.contains("_LEGGINGS") && itemLeggings == null) {
                    if (playerInventory.getLeggings() == null || playerInventory.getLeggings().getType().equals((Object)Material.AIR) || clearInventory) {
                        itemLeggings = kitItem;
                        ++freeSlots;
                        continue;
                    }
                } else if (id.contains("_BOOTS") && itemBoots == null && (playerInventory.getBoots() == null || playerInventory.getBoots().getType().equals((Object)Material.AIR) || clearInventory)) {
                    itemBoots = kitItem;
                    ++freeSlots;
                    continue;
                }
            }
            if (kitItem.isOffhand() && itemOffhand == null && (playerInventory.getItemInOffHand() == null || playerInventory.getItemInOffHand().getType().equals((Object)Material.AIR) || clearInventory)) {
                itemOffhand = kitItem;
                ++freeSlots;
                continue;
            }
            ++inventoryKitItems;
        }
        ArrayList<KitAction> claimActions = kit.getClaimActions();
        for (KitAction action : claimActions) {
            if (!action.isCountAsItem()) continue;
            ++inventoryKitItems;
        }
        boolean bl = freeSlots < inventoryKitItems;
        boolean dropItemsIfFullInventory = configFile.getBoolean("drop_items_if_full_inventory");
        if (bl && !dropItemsIfFullInventory && !clearInventory) {
            this.sendKitActions(kit.getErrorActions(), player, false);
            return PlayerKitsMessageResult.error(messagesFile.getString("noSpaceError"));
        }
        if (clearInventory) {
            player.getInventory().clear();
        }
        this.sendKitActions(kit.getClaimActions(), player, true);
        for (KitItem kitItem : items) {
            ItemStack item = kitItemManager.createItemFromKitItem(kitItem, player, kit);
            if (itemHelmet != null && kitItem.equals(itemHelmet)) {
                playerInventory.setHelmet(item);
                continue;
            }
            if (itemChestplate != null && kitItem.equals(itemChestplate)) {
                playerInventory.setChestplate(item);
                continue;
            }
            if (itemLeggings != null && kitItem.equals(itemLeggings)) {
                playerInventory.setLeggings(item);
                continue;
            }
            if (itemBoots != null && kitItem.equals(itemBoots)) {
                playerInventory.setBoots(item);
                continue;
            }
            if (itemOffhand != null && kitItem.equals(itemOffhand)) {
                playerInventory.setItemInOffHand(item);
                continue;
            }
            if (playerInventory.firstEmpty() == -1 && dropItemsIfFullInventory) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                continue;
            }
            playerInventory.addItem(new ItemStack[]{item});
        }
        this.sendKitActions(kit.getClaimActions(), player, false);
        if (!giveKitInstructions.isFromCommand()) {
            if (kit.isOneTime() && !PlayerUtils.isPlayerKitsAdmin((CommandSender)player) && !PlayerUtils.hasOneTimeBypassPermission((CommandSender)player)) {
                playerDataManager.setKitOneTime(player, kit.getName());
            }
            if (kit.getCooldown() != 0 && !PlayerUtils.isPlayerKitsAdmin((CommandSender)player) && !PlayerUtils.hasCooldownBypassPermission((CommandSender)player)) {
                long millisMax = System.currentTimeMillis() + (long)kit.getCooldown() * 1000L;
                playerDataManager.setKitCooldown(player, kit.getName(), millisMax);
            }
            KitRequirements kitRequirements = kit.getRequirements();
            if (!giveKitInstructions.isIgnoreRequirements() && kitRequirements != null && giveKitInstructions.isRequirementsSatisfied()) {
                double price = kitRequirements.getPrice();
                Economy economy = this.plugin.getDependencyManager().getVaultEconomy();
                if (price > 0.0 && economy != null) {
                    economy.withdrawPlayer((OfflinePlayer)player, price);
                }
                List<String> actions = kitRequirements.getActionsOnBuy();
                for (String action : actions) {
                    this.executeAction(player, action);
                }
                if (kitRequirements.isOneTimeRequirements()) {
                    playerDataManager.setKitBought(player, kitName);
                }
            }
        }
        return PlayerKitsMessageResult.success();
    }

    public void giveFirstJoinKit(Player player) {
        String firstJoinKit = this.plugin.getConfigsManager().getMainConfigManager().getFirstJoinKit();
        if (firstJoinKit.equals("none")) {
            return;
        }
        this.giveKit(player, firstJoinKit, new GiveKitInstructions(false, false, true, true));
    }

    public void executeAction(Player player, String actionText) {
        if (actionText.equals("close_inventory")) {
            ActionUtils.closeInventory(player);
            return;
        }
        int indexFirst = actionText.indexOf(" ");
        String actionType = actionText.substring(0, indexFirst).replace(":", "");
        String actionLine = actionText.substring(indexFirst + 1);
        actionLine = OtherUtils.replaceGlobalVariables(actionLine, player, this.plugin);
        switch (actionType) {
            case "message": {
                ActionUtils.message(player, actionLine);
                break;
            }
            case "console_command": {
                ActionUtils.consoleCommand(actionLine);
                break;
            }
            case "player_command": {
                ActionUtils.playerCommand(player, actionLine);
                break;
            }
            case "playsound": {
                ActionUtils.playSound(player, actionLine);
                break;
            }
            case "playsound_resource_pack": {
                ActionUtils.playSoundResourcePack(player, actionLine);
                break;
            }
            case "actionbar": {
                ActionUtils.actionbar(player, actionLine, this.plugin);
                break;
            }
            case "title": {
                ActionUtils.title(player, actionLine);
                break;
            }
            case "firework": {
                ActionUtils.firework(player, actionLine, this.plugin);
            }
        }
    }

    public void sendKitActions(ArrayList<KitAction> actions, Player player, boolean beforeItems) {
        for (KitAction action : actions) {
            if (action.isExecuteBeforeItems() != beforeItems) continue;
            String actionText = action.getAction();
            this.executeAction(player, actionText);
        }
    }

    public boolean passPrice(double price, Player player) {
        Economy economy;
        return price == 0.0 || (economy = this.plugin.getDependencyManager().getVaultEconomy()) == null || !(economy.getBalance((OfflinePlayer)player) < price);
    }
}

