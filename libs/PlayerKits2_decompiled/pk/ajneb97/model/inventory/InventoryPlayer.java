/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package pk.ajneb97.model.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryPlayer {
    private Player player;
    private String inventoryName;
    private String previousInventoryName;
    private String kitName;
    private ItemStack[] savedInventoryContents;

    public InventoryPlayer(Player player, String inventoryName) {
        this.player = player;
        this.inventoryName = inventoryName;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getInventoryName() {
        return this.inventoryName;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }

    public String getPreviousInventoryName() {
        return this.previousInventoryName;
    }

    public void setPreviousInventoryName(String previousInventoryName) {
        this.previousInventoryName = previousInventoryName;
    }

    public String getKitName() {
        return this.kitName;
    }

    public void setKitName(String kitName) {
        this.kitName = kitName;
    }

    public void restoreSavedInventoryContents() {
        if (this.savedInventoryContents != null) {
            this.player.getInventory().setContents(this.savedInventoryContents);
            this.savedInventoryContents = null;
        }
    }

    public void saveInventoryContents() {
        if (this.savedInventoryContents == null) {
            this.savedInventoryContents = this.player.getInventory().getContents();
        }
    }
}

