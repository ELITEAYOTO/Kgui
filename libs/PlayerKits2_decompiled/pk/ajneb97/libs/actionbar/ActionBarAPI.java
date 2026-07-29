/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.libs.actionbar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.api.PlayerKitsAPI;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.OtherUtils;

public class ActionBarAPI {
    public static void sendActionBar(Player player, String message) {
        if (OtherUtils.isNew()) {
            if (PlayerKitsAPI.getPlugin().getConfigsManager().getMainConfigManager().isUseMiniMessage()) {
                MiniMessageUtils.actionbar(player, message);
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)MessagesManager.getLegacyColoredMessage(message)));
            }
            return;
        }
        message = MessagesManager.getLegacyColoredMessage(message);
        boolean useOldMethods = false;
        String nmsver = Bukkit.getServer().getClass().getPackage().getName();
        if ((nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1)).equalsIgnoreCase("v1_8_R1") || nmsver.startsWith("v1_7_")) {
            useOldMethods = true;
        }
        if (!player.isOnline()) {
            return;
        }
        try {
            Object packet;
            Class<?> iChatBaseComponentClass;
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsver + ".PacketPlayOutChat");
            Class<?> packetClass = Class.forName("net.minecraft.server." + nmsver + ".Packet");
            if (useOldMethods) {
                Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + nmsver + ".ChatSerializer");
                iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
                Method m3 = chatSerializerClass.getDeclaredMethod("a", String.class);
                Object cbc = iChatBaseComponentClass.cast(m3.invoke(chatSerializerClass, "{\"text\": \"" + message + "\"}"));
                packet = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, Byte.TYPE).newInstance(cbc, (byte)2);
            } else {
                Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + nmsver + ".ChatComponentText");
                iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
                try {
                    Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + nmsver + ".ChatMessageType");
                    ?[] chatMessageTypes = chatMessageTypeClass.getEnumConstants();
                    Object chatMessageType = null;
                    for (Object obj : chatMessageTypes) {
                        if (!obj.toString().equals("GAME_INFO")) continue;
                        chatMessageType = obj;
                    }
                    Object chatCompontentText = chatComponentTextClass.getConstructor(String.class).newInstance(message);
                    packet = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, chatMessageTypeClass).newInstance(chatCompontentText, chatMessageType);
                }
                catch (ClassNotFoundException cnfe) {
                    Object chatCompontentText = chatComponentTextClass.getConstructor(String.class).newInstance(message);
                    packet = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, Byte.TYPE).newInstance(chatCompontentText, (byte)2);
                }
            }
            Method craftPlayerHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle", new Class[0]);
            Object craftPlayerHandle = craftPlayerHandleMethod.invoke(craftPlayer, new Object[0]);
            Field playerConnectionField = craftPlayerHandle.getClass().getDeclaredField("playerConnection");
            Object playerConnection = playerConnectionField.get(craftPlayerHandle);
            Method sendPacketMethod = playerConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
            sendPacketMethod.invoke(playerConnection, packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendActionBar(final Player player, final String message, int duration, PlayerKits2 plugin) {
        ActionBarAPI.sendActionBar(player, message);
        if (duration > 0) {
            new BukkitRunnable(){

                public void run() {
                    ActionBarAPI.sendActionBar(player, "");
                }
            }.runTaskLater((Plugin)plugin, (long)(duration + 1));
        }
        while (duration > 40) {
            new BukkitRunnable(){

                public void run() {
                    ActionBarAPI.sendActionBar(player, message);
                }
            }.runTaskLater((Plugin)plugin, (long)(duration -= 40));
        }
    }
}

