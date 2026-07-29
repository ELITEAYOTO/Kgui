/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 */
package pk.ajneb97.model;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.KitRequirements;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.PlayerUtils;

public class Kit {
    private String name;
    private int cooldown;
    private boolean permissionRequired;
    private boolean clearInventory;
    private String customPermission;
    private boolean oneTime;
    private ArrayList<KitItem> items;
    private ArrayList<KitAction> claimActions;
    private ArrayList<KitAction> errorActions;
    private boolean saveOriginalItems;
    private boolean allowPlaceholdersOnOriginalItems;
    private KitItem displayItemDefault;
    private KitItem displayItemNoPermission;
    private KitItem displayItemCooldown;
    private KitItem displayItemOneTime;
    private KitItem displayItemOneTimeRequirements;
    private KitRequirements requirements;
    private boolean autoArmor;

    public Kit(String name) {
        this.name = name;
        this.cooldown = 0;
        this.autoArmor = false;
        this.oneTime = false;
        this.saveOriginalItems = false;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isPermissionRequired() {
        return this.permissionRequired;
    }

    public void setPermissionRequired(boolean permissionRequired) {
        this.permissionRequired = permissionRequired;
    }

    public ArrayList<KitItem> getItems() {
        return this.items;
    }

    public void setItems(ArrayList<KitItem> items) {
        this.items = items;
    }

    public KitItem getDisplayItemDefault() {
        return this.displayItemDefault;
    }

    public void setDisplayItemDefault(KitItem displayItemDefault) {
        this.displayItemDefault = displayItemDefault;
    }

    public KitItem getDisplayItemNoPermission() {
        return this.displayItemNoPermission;
    }

    public void setDisplayItemNoPermission(KitItem displayItemNoPermission) {
        this.displayItemNoPermission = displayItemNoPermission;
    }

    public KitItem getDisplayItemCooldown() {
        return this.displayItemCooldown;
    }

    public void setDisplayItemCooldown(KitItem displayItemCooldown) {
        this.displayItemCooldown = displayItemCooldown;
    }

    public boolean isAutoArmor() {
        return this.autoArmor;
    }

    public void setAutoArmor(boolean autoArmor) {
        this.autoArmor = autoArmor;
    }

    public boolean isOneTime() {
        return this.oneTime;
    }

    public void setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
    }

    public ArrayList<KitAction> getClaimActions() {
        return this.claimActions;
    }

    public void setClaimActions(ArrayList<KitAction> claimActions) {
        this.claimActions = claimActions;
    }

    public ArrayList<KitAction> getErrorActions() {
        return this.errorActions;
    }

    public void setErrorActions(ArrayList<KitAction> errorActions) {
        this.errorActions = errorActions;
    }

    public KitItem getDisplayItemOneTime() {
        return this.displayItemOneTime;
    }

    public void setDisplayItemOneTime(KitItem displayItemOneTime) {
        this.displayItemOneTime = displayItemOneTime;
    }

    public KitItem getDisplayItemOneTimeRequirements() {
        return this.displayItemOneTimeRequirements;
    }

    public void setDisplayItemOneTimeRequirements(KitItem displayItemOneTimeRequirements) {
        this.displayItemOneTimeRequirements = displayItemOneTimeRequirements;
    }

    public KitRequirements getRequirements() {
        return this.requirements;
    }

    public void setRequirements(KitRequirements requirements) {
        this.requirements = requirements;
    }

    public String getCustomPermission() {
        return this.customPermission;
    }

    public void setCustomPermission(String customPermission) {
        this.customPermission = customPermission;
    }

    public boolean isSaveOriginalItems() {
        return this.saveOriginalItems;
    }

    public void setSaveOriginalItems(boolean saveOriginalItems) {
        this.saveOriginalItems = saveOriginalItems;
    }

    public boolean isClearInventory() {
        return this.clearInventory;
    }

    public void setClearInventory(boolean clearInventory) {
        this.clearInventory = clearInventory;
    }

    public boolean isAllowPlaceholdersOnOriginalItems() {
        return this.allowPlaceholdersOnOriginalItems;
    }

    public void setAllowPlaceholdersOnOriginalItems(boolean allowPlaceholdersOnOriginalItems) {
        this.allowPlaceholdersOnOriginalItems = allowPlaceholdersOnOriginalItems;
    }

    public boolean playerHasPermission(CommandSender player) {
        if (this.permissionRequired) {
            if (this.customPermission != null) {
                return PlayerUtils.isPlayerKitsAdmin(player) || player.hasPermission(this.customPermission);
            }
            return PlayerUtils.isPlayerKitsAdmin(player) || player.hasPermission("playerkits.kit." + this.name);
        }
        return true;
    }

    public void setDefaults(Kit defaultKit) {
        ArrayList<KitAction> actions;
        this.cooldown = defaultKit.getCooldown();
        this.oneTime = defaultKit.isOneTime();
        this.clearInventory = defaultKit.isClearInventory();
        this.permissionRequired = defaultKit.isPermissionRequired();
        if (defaultKit.getClaimActions() != null) {
            actions = new ArrayList<KitAction>();
            for (KitAction action : defaultKit.getClaimActions()) {
                actions.add(action.clone());
            }
            this.claimActions = actions;
        }
        if (defaultKit.getErrorActions() != null) {
            actions = new ArrayList();
            for (KitAction action : defaultKit.getErrorActions()) {
                actions.add(action.clone());
            }
            this.errorActions = actions;
        }
        this.displayItemDefault = defaultKit.getDisplayItemDefault() != null ? defaultKit.getDisplayItemDefault().clone() : null;
        this.displayItemNoPermission = defaultKit.getDisplayItemNoPermission() != null ? defaultKit.getDisplayItemNoPermission().clone() : null;
        this.displayItemCooldown = defaultKit.getDisplayItemCooldown() != null ? defaultKit.getDisplayItemCooldown().clone() : null;
        this.displayItemOneTime = defaultKit.getDisplayItemOneTime() != null ? defaultKit.getDisplayItemOneTime().clone() : null;
        this.displayItemOneTimeRequirements = defaultKit.getDisplayItemOneTimeRequirements() != null ? defaultKit.getDisplayItemOneTimeRequirements().clone() : null;
        this.autoArmor = defaultKit.isAutoArmor();
    }
}

