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

public class PKInvalidItem
extends PKBaseError {
    private String material;

    public PKInvalidItem(String file, String errorText, boolean critical, String material) {
        super(file, errorText, critical);
        this.material = material;
    }

    @Override
    public void sendMessage(Player player) {
        ArrayList<String> hover = new ArrayList<String>();
        JSONMessage jsonMessage = new JSONMessage(player, this.prefix + "&7Item material &c" + this.material + " &7on file &c" + this.file + " &7is not valid.");
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fThe material &c" + this.material + " &fdefined on");
        hover.add("&ffile &c" + this.file + " &fdoesn't exists for your");
        hover.add("&fminecraft version.");
        jsonMessage.hover(hover).send();
    }
}

