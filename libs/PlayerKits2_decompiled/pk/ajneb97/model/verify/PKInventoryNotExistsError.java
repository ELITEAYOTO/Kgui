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

public class PKInventoryNotExistsError
extends PKBaseError {
    private String inventoryName;
    private String inventoryPath;
    private String slot;

    public PKInventoryNotExistsError(String file, String errorText, boolean critical, String inventoryPath, String slot, String inventoryName) {
        super(file, errorText, critical);
        this.inventoryName = inventoryName;
        this.inventoryPath = inventoryPath;
        this.slot = slot;
    }

    @Override
    public void sendMessage(Player player) {
        ArrayList<String> hover = new ArrayList<String>();
        JSONMessage jsonMessage = new JSONMessage(player, this.prefix + "&7Inventory &c" + this.inventoryName + " &7not valid");
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fThe &c" + this.inventoryName + " &fopenened inventory used");
        hover.add("&fon slot &c" + this.slot + " &fon inventory &c" + this.inventoryPath);
        hover.add("&fis not valid. Create it on the inventory.yml file.");
        jsonMessage.hover(hover).send();
    }
}

