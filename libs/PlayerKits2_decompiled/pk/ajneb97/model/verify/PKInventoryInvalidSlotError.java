/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.model.verify;

import java.util.ArrayList;
import org.bukkit.entity.Player;
import pk.ajneb97.model.verify.PKBaseError;
import pk.ajneb97.utils.JSONMessage;

public class PKInventoryInvalidSlotError
extends PKBaseError {
    private String inventoryName;
    private int slot;
    private int maxSlots;

    public PKInventoryInvalidSlotError(String file, String errorText, boolean critical, int slot, String inventoryName, int maxSlots) {
        super(file, errorText, critical);
        this.inventoryName = inventoryName;
        this.slot = slot;
        this.maxSlots = maxSlots;
    }

    @Override
    public void sendMessage(Player player) {
        ArrayList<String> hover = new ArrayList<String>();
        JSONMessage jsonMessage = new JSONMessage(player, this.prefix + "&7Inventory &c" + this.inventoryName + " &7has an item on an invalid slot");
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fSlot &c" + this.slot + " &fon inventory &c" + this.inventoryName);
        hover.add("&fis out of range. Use a range");
        hover.add("&fbetween 0 and " + (this.maxSlots - 1) + ".");
        jsonMessage.hover(hover).send();
    }
}

