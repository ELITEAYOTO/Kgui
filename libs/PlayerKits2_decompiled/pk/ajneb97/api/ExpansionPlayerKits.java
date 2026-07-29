/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.api.PlayerKitsAPI;

public class ExpansionPlayerKits
extends PlaceholderExpansion {
    private PlayerKits2 plugin;

    public ExpansionPlayerKits(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getAuthor() {
        return "Ajneb97";
    }

    public String getIdentifier() {
        return "playerkits";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.startsWith("cooldown_")) {
            String event = identifier.replace("cooldown_", "");
            return PlayerKitsAPI.getKitCooldown(player, event);
        }
        if (identifier.startsWith("onetime_ready_")) {
            String event = identifier.replace("onetime_ready_", "");
            return PlayerKitsAPI.getOneTimeReady(player, event);
        }
        return null;
    }
}

