/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.configs;

import java.io.File;
import java.util.ArrayList;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.model.CommonConfig;

public abstract class DataFolderConfigManager {
    protected String folderName;
    protected PlayerKits2 plugin;

    public DataFolderConfigManager(PlayerKits2 plugin, String folderName) {
        this.plugin = plugin;
        this.folderName = folderName;
    }

    public void configure() {
        this.createFolder();
        this.loadConfigs();
    }

    public void createFolder() {
        try {
            File folder = new File(this.plugin.getDataFolder() + File.separator + this.folderName);
            if (!folder.exists()) {
                folder.mkdirs();
                this.createFiles();
            }
        }
        catch (SecurityException e) {
            Object folder = null;
        }
    }

    public CommonConfig getConfigFile(String pathName, boolean create) {
        String pathFile = this.plugin.getDataFolder() + File.separator + this.folderName;
        File folder = new File(pathFile);
        File file = new File(folder, pathName);
        if (!file.exists() && !create) {
            return null;
        }
        CommonConfig commonConfig = new CommonConfig(pathName, this.plugin, this.folderName, true);
        commonConfig.registerConfig();
        return commonConfig;
    }

    public ArrayList<CommonConfig> getConfigs() {
        File[] listOfFiles;
        ArrayList<CommonConfig> configs = new ArrayList<CommonConfig>();
        String pathFile = this.plugin.getDataFolder() + File.separator + this.folderName;
        File folder = new File(pathFile);
        for (File file : listOfFiles = folder.listFiles()) {
            if (!file.isFile()) continue;
            String pathName = file.getName();
            CommonConfig commonConfig = new CommonConfig(pathName, this.plugin, this.folderName, true);
            commonConfig.registerConfig();
            configs.add(commonConfig);
        }
        return configs;
    }

    public abstract void createFiles();

    public abstract void loadConfigs();

    public abstract void saveConfigs();
}

