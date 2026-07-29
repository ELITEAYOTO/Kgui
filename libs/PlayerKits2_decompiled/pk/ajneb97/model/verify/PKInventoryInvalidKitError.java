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

public class PKInventoryInvalidKitError
extends PKBaseError {
    private String kitName;
    private String inventoryName;
    private String slot;

    public PKInventoryInvalidKitError(String file, String errorText, boolean critical, String kitName, String inventoryName, String slot) {
        super(file, errorText, critical);
        this.kitName = kitName;
        this.inventoryName = inventoryName;
        this.slot = slot;
    }

    @Override
    public void sendMessage(Player player) {
        ArrayList<String> hover = new ArrayList<String>();
        JSONMessage jsonMessage = new JSONMessage(player, this.prefix + "&7Invalid kit named &c" + this.kitName + " &7on file &c" + this.file);
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fA kit that doesn't exists is present on");
        hover.add("&finventory &c" + this.inventoryName + " &fand slot &c" + this.slot + "&f.");
        jsonMessage.hover(hover).send();
    }
}

