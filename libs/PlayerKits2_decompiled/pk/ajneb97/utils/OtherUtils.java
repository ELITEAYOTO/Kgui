/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.Color
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.utils;

import java.util.ArrayList;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.utils.ServerVersion;

public class OtherUtils {
    public static boolean isNew() {
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        return serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_16_R1);
    }

    public static boolean isLegacy() {
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        return !serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_13_R1);
    }

    public static boolean isTrimNew() {
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        return serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R1);
    }

    public static String getTime(long seconds, MessagesManager msgManager) {
        long totalMinWait = seconds / 60L;
        long totalHourWait = totalMinWait / 60L;
        long totalDayWait = totalHourWait / 24L;
        String time = "";
        if (seconds > 59L) {
            seconds -= 60L * totalMinWait;
        }
        if (seconds > 0L) {
            time = seconds + msgManager.getTimeSeconds();
        }
        if (totalMinWait > 59L) {
            totalMinWait -= 60L * totalHourWait;
        }
        if (totalMinWait > 0L) {
            time = totalMinWait + msgManager.getTimeMinutes() + " " + time;
        }
        if (totalHourWait > 23L) {
            totalHourWait -= 24L * totalDayWait;
        }
        if (totalHourWait > 0L) {
            time = totalHourWait + msgManager.getTimeHours() + " " + time;
        }
        if (totalDayWait > 0L) {
            time = totalDayWait + msgManager.getTimeDays() + " " + time;
        }
        if (time.endsWith(" ")) {
            time = time.substring(0, time.length() - 1);
        }
        return time;
    }

    public static Color getFireworkColorFromName(String colorName) {
        try {
            return (Color)Color.class.getDeclaredField(colorName).get(Color.class);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String replaceGlobalVariables(String text, Player player, PlayerKits2 plugin) {
        if (player == null) {
            return text;
        }
        text = text.replace("%player%", player.getName());
        if (plugin.getDependencyManager().isPlaceholderAPI()) {
            text = PlaceholderAPI.setPlaceholders((Player)player, (String)text);
        }
        return text;
    }

    public static void addRangeToList(int min, int max, ArrayList<Integer> list) {
        for (int i = min; i <= max; ++i) {
            list.add(i);
        }
    }
}

