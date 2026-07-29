/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package pk.ajneb97.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.bukkit.configuration.file.FileConfiguration;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.model.CommonConfig;
import pk.ajneb97.managers.MessagesManager;

public class MessagesConfigManager {
    private PlayerKits2 plugin;
    private CommonConfig configFile;

    public MessagesConfigManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.configFile = new CommonConfig("messages.yml", plugin, null, false);
        this.configFile.registerConfig();
        this.checkUpdate();
    }

    public void configure() {
        FileConfiguration config = this.configFile.getConfig();
        MessagesManager msgManager = new MessagesManager();
        msgManager.setTimeSeconds(config.getString("seconds"));
        msgManager.setTimeMinutes(config.getString("minutes"));
        msgManager.setTimeHours(config.getString("hours"));
        msgManager.setTimeDays(config.getString("days"));
        msgManager.setPrefix(config.getString("prefix"));
        msgManager.setRequirementsMessageStatusSymbolTrue(config.getString("requirementsMessageStatusSymbolTrue"));
        msgManager.setRequirementsMessageStatusSymbolFalse(config.getString("requirementsMessageStatusSymbolFalse"));
        msgManager.setCooldownPlaceholderReady(config.getString("cooldownPlaceholderReady"));
        this.plugin.setMessagesManager(msgManager);
    }

    public void saveConfig() {
        this.configFile.saveConfig();
    }

    public boolean reloadConfig() {
        if (!this.configFile.reloadConfig()) {
            return false;
        }
        this.configure();
        return true;
    }

    public FileConfiguration getConfig() {
        return this.configFile.getConfig();
    }

    public void checkUpdate() {
        Path pathConfig = Paths.get(this.configFile.getRoute(), new String[0]);
        try {
            String text = new String(Files.readAllBytes(pathConfig));
            if (!text.contains("commandPreviewOtherCorrect:")) {
                this.getConfig().set("onlyPlayerCommand", (Object)"&cOnly a player can use this command.");
                this.getConfig().set("commandPreviewOtherCorrect", (Object)"&aPreviewing kit &7%kit% &ato &e%player%&a.");
                this.saveConfig();
            }
            if (!text.contains("kitResetCorrectAll:")) {
                this.getConfig().set("kitResetCorrectAll", (Object)"&aKit &7%kit% &areset for &7all players&a!");
                this.saveConfig();
            }
            if (!text.contains("commandOpenError:")) {
                this.getConfig().set("commandOpenError", (Object)"&cYou need to use: &7/kit open <inventory> <player>");
                this.getConfig().set("inventoryNotExists", (Object)"&cThat inventory doesn't exists.");
                this.saveConfig();
            }
            if (!text.contains("commandPreviewError:")) {
                this.getConfig().set("commandPreviewError", (Object)"&cYou need to use: &7/kit preview <kit>");
                this.getConfig().set("kitPreviewDisabled", (Object)"&cKit preview is disabled.");
                this.saveConfig();
            }
            if (!text.contains("pluginCriticalErrors:")) {
                this.getConfig().set("pluginCriticalErrors", (Object)"&cThe plugin has detected some errors. Check them using &7/kit verify");
                this.saveConfig();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

