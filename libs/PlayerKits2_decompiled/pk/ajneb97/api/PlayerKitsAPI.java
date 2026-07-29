/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.PlayerDataManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.utils.PlayerUtils;

public class PlayerKitsAPI {
    private static PlayerKits2 plugin;

    public PlayerKitsAPI(PlayerKits2 plugin) {
        PlayerKitsAPI.plugin = plugin;
    }

    public static PlayerKits2 getPlugin() {
        return plugin;
    }

    public static String getKitCooldown(Player player, String kitName) {
        String timeStringMillisDif;
        Kit kit = plugin.getKitsManager().getKitByName(kitName);
        MessagesManager messagesManager = plugin.getMessagesManager();
        if (kit == null) {
            return null;
        }
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        long playerCooldown = playerDataManager.getKitCooldown(player, kit.getName());
        if (kit.getCooldown() != 0 && !PlayerUtils.isPlayerKitsAdmin((CommandSender)player) && !(timeStringMillisDif = playerDataManager.getKitCooldownString(playerCooldown)).isEmpty()) {
            return timeStringMillisDif;
        }
        return messagesManager.getCooldownPlaceholderReady();
    }

    public static String getOneTimeReady(Player player, String kitName) {
        Kit kit = plugin.getKitsManager().getKitByName(kitName);
        if (kit == null) {
            return null;
        }
        boolean oneTime = plugin.getPlayerDataManager().isKitOneTime(player, kitName);
        if (oneTime) {
            return "yes";
        }
        return "no";
    }
}

