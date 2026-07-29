/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 */
package pk.ajneb97.libs.titles;

import java.lang.reflect.Constructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import pk.ajneb97.api.PlayerKitsAPI;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.OtherUtils;

public class TitleAPI
implements Listener {
    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle", new Class[0]).invoke(player, new Object[0]);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", TitleAPI.getNMSClass("Packet")).invoke(playerConnection, packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Class<?> getNMSClass(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        if (OtherUtils.isNew()) {
            if (title.isEmpty()) {
                title = " ";
            }
            if (subtitle.isEmpty()) {
                subtitle = " ";
            }
            if (PlayerKitsAPI.getPlugin().getConfigsManager().getMainConfigManager().isUseMiniMessage()) {
                MiniMessageUtils.title(player, title, subtitle, fadeIn, stay, fadeOut);
            } else {
                player.sendTitle(MessagesManager.getLegacyColoredMessage(title), MessagesManager.getLegacyColoredMessage(subtitle), fadeIn.intValue(), stay.intValue(), fadeOut.intValue());
            }
            return;
        }
        try {
            Constructor<?> subtitleConstructor;
            Object e;
            if (title != null) {
                title = ChatColor.translateAlternateColorCodes((char)'&', (String)title);
                title = title.replaceAll("%player%", player.getDisplayName());
                e = TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
                Object chatTitle = TitleAPI.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + title + "\"}");
                subtitleConstructor = TitleAPI.getNMSClass("PacketPlayOutTitle").getConstructor(TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], TitleAPI.getNMSClass("IChatBaseComponent"), Integer.TYPE, Integer.TYPE, Integer.TYPE);
                Object titlePacket = subtitleConstructor.newInstance(e, chatTitle, fadeIn, stay, fadeOut);
                TitleAPI.sendPacket(player, titlePacket);
                e = TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
                chatTitle = TitleAPI.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + title + "\"}");
                subtitleConstructor = TitleAPI.getNMSClass("PacketPlayOutTitle").getConstructor(TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], TitleAPI.getNMSClass("IChatBaseComponent"));
                titlePacket = subtitleConstructor.newInstance(e, chatTitle);
                TitleAPI.sendPacket(player, titlePacket);
            }
            if (subtitle != null) {
                subtitle = ChatColor.translateAlternateColorCodes((char)'&', (String)subtitle);
                subtitle = subtitle.replaceAll("%player%", player.getDisplayName());
                e = TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
                Object chatSubtitle = TitleAPI.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + title + "\"}");
                subtitleConstructor = TitleAPI.getNMSClass("PacketPlayOutTitle").getConstructor(TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], TitleAPI.getNMSClass("IChatBaseComponent"), Integer.TYPE, Integer.TYPE, Integer.TYPE);
                Object subtitlePacket = subtitleConstructor.newInstance(e, chatSubtitle, fadeIn, stay, fadeOut);
                TitleAPI.sendPacket(player, subtitlePacket);
                e = TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);
                chatSubtitle = TitleAPI.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + subtitle + "\"}");
                subtitleConstructor = TitleAPI.getNMSClass("PacketPlayOutTitle").getConstructor(TitleAPI.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], TitleAPI.getNMSClass("IChatBaseComponent"), Integer.TYPE, Integer.TYPE, Integer.TYPE);
                subtitlePacket = subtitleConstructor.newInstance(e, chatSubtitle, fadeIn, stay, fadeOut);
                TitleAPI.sendPacket(player, subtitlePacket);
            }
        }
        catch (Exception var11) {
            var11.printStackTrace();
        }
    }
}

