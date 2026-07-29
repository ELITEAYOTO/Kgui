/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.command.CommandSender
 */
package pk.ajneb97.managers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import pk.ajneb97.api.PlayerKitsAPI;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.OtherUtils;

public class MessagesManager {
    private String timeSeconds;
    private String timeMinutes;
    private String timeHours;
    private String timeDays;
    private String requirementsMessageStatusSymbolTrue;
    private String requirementsMessageStatusSymbolFalse;
    private String cooldownPlaceholderReady;
    private String prefix;

    public String getTimeSeconds() {
        return this.timeSeconds;
    }

    public void setTimeSeconds(String timeSeconds) {
        this.timeSeconds = timeSeconds;
    }

    public String getTimeMinutes() {
        return this.timeMinutes;
    }

    public void setTimeMinutes(String timeMinutes) {
        this.timeMinutes = timeMinutes;
    }

    public String getTimeHours() {
        return this.timeHours;
    }

    public void setTimeHours(String timeHours) {
        this.timeHours = timeHours;
    }

    public String getTimeDays() {
        return this.timeDays;
    }

    public void setTimeDays(String timeDays) {
        this.timeDays = timeDays;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getRequirementsMessageStatusSymbolTrue() {
        return this.requirementsMessageStatusSymbolTrue;
    }

    public void setRequirementsMessageStatusSymbolTrue(String requirementsMessageStatusSymbolTrue) {
        this.requirementsMessageStatusSymbolTrue = requirementsMessageStatusSymbolTrue;
    }

    public String getRequirementsMessageStatusSymbolFalse() {
        return this.requirementsMessageStatusSymbolFalse;
    }

    public void setRequirementsMessageStatusSymbolFalse(String requirementsMessageStatusSymbolFalse) {
        this.requirementsMessageStatusSymbolFalse = requirementsMessageStatusSymbolFalse;
    }

    public String getCooldownPlaceholderReady() {
        return this.cooldownPlaceholderReady;
    }

    public void setCooldownPlaceholderReady(String cooldownPlaceholderReady) {
        this.cooldownPlaceholderReady = cooldownPlaceholderReady;
    }

    public void sendMessage(CommandSender sender, String message, boolean prefix) {
        if (!message.isEmpty()) {
            if (PlayerKitsAPI.getPlugin().getConfigsManager().getMainConfigManager().isUseMiniMessage()) {
                MiniMessageUtils.messagePrefix(sender, message, prefix, this.prefix);
            } else if (prefix) {
                sender.sendMessage(MessagesManager.getLegacyColoredMessage(this.prefix + message));
            } else {
                sender.sendMessage(MessagesManager.getLegacyColoredMessage(message));
            }
        }
    }

    public static String getLegacyColoredMessage(String message) {
        if (OtherUtils.isNew()) {
            Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
            Matcher match = pattern.matcher(message);
            while (match.find()) {
                String color = message.substring(match.start(), match.end());
                message = message.replace(color, ChatColor.of((String)color) + "");
                match = pattern.matcher(message);
            }
        }
        message = ChatColor.translateAlternateColorCodes((char)'&', (String)message);
        return message;
    }
}

