/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.KitsManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.inventory.ItemKitInventory;
import pk.ajneb97.model.inventory.KitInventory;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.model.verify.PKBaseError;
import pk.ajneb97.model.verify.PKInvalidItem;
import pk.ajneb97.model.verify.PKInventoryDefaultNotExistsError;
import pk.ajneb97.model.verify.PKInventoryInvalidKitError;
import pk.ajneb97.model.verify.PKInventoryInvalidSlotError;
import pk.ajneb97.model.verify.PKInventoryNotExistsError;
import pk.ajneb97.model.verify.PKKitActionError;
import pk.ajneb97.model.verify.PKKitDisplayItemError;
import pk.ajneb97.utils.ItemUtils;

public class VerifyManager {
    private PlayerKits2 plugin;
    private ArrayList<PKBaseError> errors;
    private boolean criticalErrors;

    public VerifyManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.errors = new ArrayList();
        this.criticalErrors = false;
    }

    public void sendVerification(Player player) {
        player.sendMessage(MessagesManager.getLegacyColoredMessage("&f&l- - - - - - - - &b&lPLAYERKITS 2 VERIFY &f&l- - - - - - - -"));
        player.sendMessage(MessagesManager.getLegacyColoredMessage(""));
        if (this.errors.isEmpty()) {
            player.sendMessage(MessagesManager.getLegacyColoredMessage("&aThere are no errors in the plugin ;)"));
        } else {
            player.sendMessage(MessagesManager.getLegacyColoredMessage("&e&oHover on the errors to see more information."));
            for (PKBaseError error : this.errors) {
                error.sendMessage(player);
            }
        }
        player.sendMessage(MessagesManager.getLegacyColoredMessage(""));
        player.sendMessage(MessagesManager.getLegacyColoredMessage("&f&l- - - - - - - - &b&lPLAYERKITS 2 VERIFY &f&l- - - - - - - -"));
    }

    public void verify() {
        this.errors = new ArrayList();
        this.criticalErrors = false;
        ArrayList<Kit> kits = this.plugin.getKitsManager().getKits();
        for (Kit kit : kits) {
            this.verifyKit(kit);
        }
        InventoryManager inventoryManager = this.plugin.getInventoryManager();
        ArrayList<KitInventory> inventories = inventoryManager.getInventories();
        for (KitInventory inventory : inventories) {
            this.verifyInventory(inventory);
        }
        if (inventoryManager.getInventory("main_inventory") == null) {
            this.errors.add(new PKInventoryDefaultNotExistsError("inventory.yml", null, true, "main_inventory"));
            this.criticalErrors = true;
        }
        if (inventoryManager.getInventory("preview_inventory") == null) {
            this.errors.add(new PKInventoryDefaultNotExistsError("inventory.yml", null, true, "preview_inventory"));
            this.criticalErrors = true;
        }
        if (inventoryManager.getInventory("buy_requirements_inventory") == null) {
            this.errors.add(new PKInventoryDefaultNotExistsError("inventory.yml", null, true, "buy_requirements_inventory"));
            this.criticalErrors = true;
        }
    }

    public void verifyKit(Kit kit) {
        String kitName = kit.getName();
        if (kit.getDisplayItemDefault() == null || kit.getDisplayItemDefault().getId() == null) {
            this.errors.add(new PKKitDisplayItemError(kitName + ".yml", null, true, kitName));
            this.criticalErrors = true;
        }
        this.verifyActions(kit.getClaimActions(), "claim", kitName);
        this.verifyActions(kit.getErrorActions(), "error", kitName);
        ArrayList<KitItem> allKitItems = new ArrayList<KitItem>();
        allKitItems.add(kit.getDisplayItemDefault());
        allKitItems.add(kit.getDisplayItemCooldown());
        allKitItems.add(kit.getDisplayItemNoPermission());
        allKitItems.add(kit.getDisplayItemOneTime());
        allKitItems.add(kit.getDisplayItemOneTimeRequirements());
        allKitItems.addAll(kit.getItems());
        for (KitAction kitAction : kit.getClaimActions()) {
            allKitItems.add(kitAction.getDisplayItem());
        }
        for (KitAction kitAction : kit.getErrorActions()) {
            allKitItems.add(kitAction.getDisplayItem());
        }
        for (KitItem kitItem : allKitItems) {
            if (kitItem == null || kitItem.getOriginalItem() != null || this.verifyItem(kitItem.getId())) continue;
            this.errors.add(new PKInvalidItem(kit.getName() + ".yml", null, true, kitItem.getId()));
            this.criticalErrors = true;
        }
    }

    public void verifyActions(ArrayList<KitAction> actions, String actionGroup, String kitName) {
        for (int i = 0; i < actions.size(); ++i) {
            KitAction action = actions.get(i);
            String[] actionText = action.getAction().split(" ");
            String actionName = actionText[0];
            if (actionName.equals("console_command:") || actionName.equals("player_command:") || actionName.equals("playsound:") || actionName.equals("playsound_resource_pack:") || actionName.equals("actionbar:") || actionName.equals("title:") || actionName.equals("firework:") || actionName.equals("close_inventory") || actionName.equals("message:")) continue;
            this.errors.add(new PKKitActionError(kitName + ".yml", action.getAction(), false, kitName, actionGroup, i + 1 + ""));
        }
    }

    public void verifyInventory(KitInventory inventory) {
        KitsManager kitsManager = this.plugin.getKitsManager();
        List<ItemKitInventory> items = inventory.getItems();
        InventoryManager inventoryManager = this.plugin.getInventoryManager();
        int maxSlots = inventory.getSlots();
        for (ItemKitInventory item : items) {
            KitItem kitItem;
            String openInventory;
            String kitName;
            String type = item.getType();
            if (type != null && type.startsWith("kit: ") && kitsManager.getKitByName(kitName = type.replace("kit: ", "")) == null) {
                this.errors.add(new PKInventoryInvalidKitError("inventory.yml", null, true, kitName, inventory.getName(), item.getSlotsString()));
                this.criticalErrors = true;
            }
            if ((openInventory = item.getOpenInventory()) != null && !openInventory.equals("previous") && inventoryManager.getInventory(openInventory) == null) {
                this.errors.add(new PKInventoryNotExistsError("inventory.yml", null, true, inventory.getName(), item.getSlotsString(), openInventory));
                this.criticalErrors = true;
            }
            if ((kitItem = item.getItem()) != null && !this.verifyItem(kitItem.getId())) {
                this.errors.add(new PKInvalidItem("inventory.yml", null, true, kitItem.getId()));
                this.criticalErrors = true;
            }
            for (int slot : item.getSlots()) {
                if (slot < maxSlots) continue;
                this.errors.add(new PKInventoryInvalidSlotError("inventory.yml", null, true, slot, inventory.getName(), maxSlots));
                this.criticalErrors = true;
            }
        }
    }

    public boolean isCriticalErrors() {
        return this.criticalErrors;
    }

    public boolean verifyItem(String material) {
        try {
            ItemUtils.createItemFromID(material);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}

