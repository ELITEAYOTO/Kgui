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

public class PKKitActionError
extends PKBaseError {
    private String kitName;
    private String actionGroup;
    private String actionId;

    public PKKitActionError(String file, String errorText, boolean critical, String kitName, String actionGroup, String actionId) {
        super(file, errorText, critical);
        this.kitName = kitName;
        this.actionId = actionId;
        this.actionGroup = actionGroup;
    }

    @Override
    public void sendMessage(Player player) {
        ArrayList<String> hover = new ArrayList<String>();
        JSONMessage jsonMessage = new JSONMessage(player, this.prefix + "&7Action (&c" + this.actionGroup + "&7,&c" + this.actionId + "&7) &7on kit &c" + this.kitName + " &7is not valid.");
        hover.add("&eTHIS IS A WARNING!");
        hover.add("&fThe action defined for this event");
        hover.add("&fis probably not formatted correctly:");
        for (String m : this.getFixedErrorText()) {
            hover.add("&c" + m);
        }
        hover.add(" ");
        hover.add("&fRemember to use a valid action from this list:");
        hover.add("&ahttps://ajneb97.gitbook.io/playerkits-2/actions");
        jsonMessage.hover(hover).send();
    }
}

