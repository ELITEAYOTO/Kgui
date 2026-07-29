/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.InvalidConfigurationException
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package pk.ajneb97.configs.model;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pk.ajneb97.PlayerKits2;

public class CommonConfig {
    private String fileName;
    private FileConfiguration fileConfiguration = null;
    private File file = null;
    private String route;
    private PlayerKits2 plugin;
    private String folderName;
    private boolean newFile;
    private boolean isFirstTime;

    public CommonConfig(String fileName, PlayerKits2 plugin, String folderName, boolean newFile) {
        this.fileName = fileName;
        this.plugin = plugin;
        this.newFile = newFile;
        this.folderName = folderName;
        this.isFirstTime = false;
    }

    public String getPath() {
        return this.fileName;
    }

    public void registerConfig() {
        this.file = this.folderName != null ? new File(this.plugin.getDataFolder() + File.separator + this.folderName, this.fileName) : new File(this.plugin.getDataFolder(), this.fileName);
        this.route = this.file.getPath();
        if (!this.file.exists()) {
            this.isFirstTime = true;
            if (this.newFile) {
                try {
                    this.file.createNewFile();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (this.folderName != null) {
                this.plugin.saveResource(this.folderName + File.separator + this.fileName, false);
            } else {
                this.plugin.saveResource(this.fileName, false);
            }
        }
        this.fileConfiguration = new YamlConfiguration();
        try {
            this.fileConfiguration.load(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            this.fileConfiguration.save(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        if (this.fileConfiguration == null) {
            this.reloadConfig();
        }
        return this.fileConfiguration;
    }

    public boolean reloadConfig() {
        if (this.fileConfiguration == null) {
            this.file = this.folderName != null ? new File(this.plugin.getDataFolder() + File.separator + this.folderName, this.fileName) : new File(this.plugin.getDataFolder(), this.fileName);
        }
        this.fileConfiguration = YamlConfiguration.loadConfiguration((File)this.file);
        if (this.file != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration((File)this.file);
            this.fileConfiguration.setDefaults((Configuration)defConfig);
        }
        return true;
    }

    public String getRoute() {
        return this.route;
    }

    public boolean isFirstTime() {
        return this.isFirstTime;
    }

    public void setFirstTime(boolean firstTime) {
        this.isFirstTime = firstTime;
    }
}

