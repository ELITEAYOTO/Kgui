/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Firework
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.meta.FireworkMeta
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package pk.ajneb97.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.api.PlayerKitsAPI;
import pk.ajneb97.libs.actionbar.ActionBarAPI;
import pk.ajneb97.libs.titles.TitleAPI;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.OtherUtils;
import pk.ajneb97.utils.ServerVersion;

public class ActionUtils {
    public static void message(Player player, String actionLine) {
        if (PlayerKitsAPI.getPlugin().getConfigsManager().getMainConfigManager().isUseMiniMessage()) {
            MiniMessageUtils.message(player, actionLine);
        } else {
            player.sendMessage(MessagesManager.getLegacyColoredMessage(actionLine));
        }
    }

    public static void consoleCommand(String actionLine) {
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand((CommandSender)sender, (String)actionLine);
    }

    public static void playerCommand(Player player, String actionLine) {
        player.performCommand(actionLine);
    }

    public static void playSound(Player player, String actionLine) {
        String[] sep = actionLine.split(";");
        Sound sound = null;
        int volume = 0;
        float pitch = 0.0f;
        try {
            sound = ActionUtils.getSoundByName(sep[0]);
            volume = Integer.valueOf(sep[1]);
            pitch = Float.valueOf(sep[2]).floatValue();
        }
        catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage("&7Sound Name: &c" + sep[0] + " &7is not valid. Change it in the config!"));
            return;
        }
        player.playSound(player.getLocation(), sound, (float)volume, pitch);
    }

    public static void playSoundResourcePack(Player player, String soundLine) {
        String[] sep = soundLine.split(";");
        String sound = sep[0];
        float volume = Float.parseFloat(sep[1]);
        float pitch = Float.parseFloat(sep[2]);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Sound getSoundByName(String name) {
        try {
            Class<?> soundTypeClass = Class.forName("org.bukkit.Sound");
            Method valueOf = soundTypeClass.getMethod("valueOf", String.class);
            return (Sound)valueOf.invoke(null, name);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void actionbar(Player player, String actionLine, PlayerKits2 plugin) {
        String[] sep = actionLine.split(";");
        String text = sep[0];
        int duration = Integer.valueOf(sep[1]);
        ActionBarAPI.sendActionBar(player, text, duration, plugin);
    }

    public static void title(Player player, String actionLine) {
        String[] sep = actionLine.split(";");
        int fadeIn = Integer.valueOf(sep[0]);
        int stay = Integer.valueOf(sep[1]);
        int fadeOut = Integer.valueOf(sep[2]);
        String title = sep[3];
        String subtitle = sep[4];
        if (title.equals("none")) {
            title = "";
        }
        if (subtitle.equals("none")) {
            subtitle = "";
        }
        TitleAPI.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
    }

    public static void firework(Player player, String actionLine, PlayerKits2 plugin) {
        String[] sep;
        ArrayList<Color> colors = new ArrayList<Color>();
        FireworkEffect.Type type = null;
        ArrayList<Color> fadeColors = new ArrayList<Color>();
        int power = 0;
        for (String s : sep = actionLine.split(" ")) {
            String[] colorsSep;
            if (s.startsWith("colors:")) {
                s = s.replace("colors:", "");
                for (String colorSep : colorsSep = s.split(",")) {
                    colors.add(OtherUtils.getFireworkColorFromName(colorSep));
                }
                continue;
            }
            if (s.startsWith("type:")) {
                s = s.replace("type:", "");
                type = FireworkEffect.Type.valueOf((String)s);
                continue;
            }
            if (s.startsWith("fade:")) {
                s = s.replace("fade:", "");
                for (String colorSep : colorsSep = s.split(",")) {
                    fadeColors.add(OtherUtils.getFireworkColorFromName(colorSep));
                }
                continue;
            }
            if (!s.startsWith("power:")) continue;
            s = s.replace("power:", "");
            power = Integer.valueOf(s);
        }
        Location location = player.getLocation();
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        EntityType entityType = null;
        entityType = serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R4) ? EntityType.FIREWORK_ROCKET : EntityType.valueOf((String)"FIREWORK");
        Firework firework = (Firework)location.getWorld().spawnEntity(location, entityType);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder().flicker(false).withColor(colors).with(type).withFade(fadeColors).build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(power);
        firework.setFireworkMeta(fireworkMeta);
        firework.setMetadata("playerkits", (MetadataValue)new FixedMetadataValue((Plugin)plugin, (Object)"no_damage"));
    }

    public static void closeInventory(Player player) {
        player.closeInventory();
    }
}

