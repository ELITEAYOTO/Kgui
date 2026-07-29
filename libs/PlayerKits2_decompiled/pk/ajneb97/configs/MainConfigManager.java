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
import pk.ajneb97.configs.KitsConfigManager;
import pk.ajneb97.configs.model.CommonConfig;
import pk.ajneb97.model.Kit;

public class MainConfigManager {
    private PlayerKits2 plugin;
    private CommonConfig configFile;
    private Kit newKitDefault;
    private boolean kitPreview;
    private boolean closeInventoryOnClaim;
    private boolean claimKitShortCommand;
    private boolean kitPreviewRequiresKitPermission;
    private boolean newKitDefaultSaveModeOriginal;
    private String firstJoinKit;
    private String newKitDefaultInventory;
    private boolean isMySQL;
    private boolean updateNotify;
    private boolean useMiniMessage;

    public MainConfigManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.configFile = new CommonConfig("config.yml", plugin, null, false);
        this.configFile.registerConfig();
        this.checkUpdate();
    }

    public void configure() {
        FileConfiguration config = this.configFile.getConfig();
        this.newKitDefault = KitsConfigManager.getKitFromConfig(config, this.plugin, null, "new_kit_default_values.");
        this.kitPreview = config.getBoolean("kit_preview");
        this.closeInventoryOnClaim = config.getBoolean("close_inventory_on_claim");
        this.kitPreviewRequiresKitPermission = config.getBoolean("kit_preview_requires_kit_permission");
        this.firstJoinKit = config.getString("first_join_kit");
        this.newKitDefaultInventory = config.getString("new_kit_default_inventory");
        this.isMySQL = config.getBoolean("mysql_database.enabled");
        this.updateNotify = config.getBoolean("update_notify");
        this.claimKitShortCommand = config.getBoolean("claim_kit_short_command");
        this.useMiniMessage = config.getBoolean("use_minimessage");
        this.newKitDefaultSaveModeOriginal = config.getBoolean("new_kit_default_save_mode_original");
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
            if (!text.contains("use_minimessage:")) {
                this.getConfig().set("use_minimessage", (Object)false);
                this.configFile.saveConfig();
            }
            if (!text.contains("verifyServerCertificate:")) {
                this.getConfig().set("mysql_database.pool.connectionTimeout", (Object)5000);
                this.getConfig().set("mysql_database.advanced.verifyServerCertificate", (Object)false);
                this.getConfig().set("mysql_database.advanced.useSSL", (Object)true);
                this.getConfig().set("mysql_database.advanced.allowPublicKeyRetrieval", (Object)true);
                this.configFile.saveConfig();
            }
            if (!text.contains("new_kit_default_save_mode_original:")) {
                this.getConfig().set("new_kit_default_save_mode_original", (Object)true);
                this.configFile.saveConfig();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Kit getNewKitDefault() {
        return this.newKitDefault;
    }

    public boolean isKitPreview() {
        return this.kitPreview;
    }

    public boolean isCloseInventoryOnClaim() {
        return this.closeInventoryOnClaim;
    }

    public boolean isKitPreviewRequiresKitPermission() {
        return this.kitPreviewRequiresKitPermission;
    }

    public String getFirstJoinKit() {
        return this.firstJoinKit;
    }

    public String getNewKitDefaultInventory() {
        return this.newKitDefaultInventory;
    }

    public boolean isMySQL() {
        return this.isMySQL;
    }

    public boolean isUpdateNotify() {
        return this.updateNotify;
    }

    public boolean isClaimKitShortCommand() {
        return this.claimKitShortCommand;
    }

    public boolean isNewKitDefaultSaveModeOriginal() {
        return this.newKitDefaultSaveModeOriginal;
    }

    public boolean isUseMiniMessage() {
        return this.useMiniMessage;
    }
}

