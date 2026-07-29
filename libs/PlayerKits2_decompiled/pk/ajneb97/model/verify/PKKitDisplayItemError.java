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

public class PKKitDisplayItemError
extends PKBaseError {
    private String kitName;

    public PKKitDisplayItemError(String file, String errorText, boolean critical, String kitName) {
        super(file, errorText, critical);
        this.kitName = kitName;
    }

    @Override
    public void sendMessage(Player player) {
        ArrayList<String> hover = new ArrayList<String>();
        JSONMessage jsonMessage = new JSONMessage(player, this.prefix + "&7Kit &c" + this.kitName + " &7doesn't have a default display item.");
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fAll kits must have a default display");
        hover.add("&fitem. Set one using /kit edit " + this.kitName);
        jsonMessage.hover(hover).send();
    }
}

