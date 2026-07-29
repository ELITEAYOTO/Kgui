/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.configs;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.DataFolderConfigManager;
import pk.ajneb97.configs.model.CommonConfig;
import pk.ajneb97.model.PlayerData;
import pk.ajneb97.model.PlayerDataKit;
import pk.ajneb97.model.internal.GenericCallback;

public class PlayersConfigManager
extends DataFolderConfigManager {
    public PlayersConfigManager(PlayerKits2 plugin, String folderName) {
        super(plugin, folderName);
    }

    @Override
    public void createFiles() {
    }

    @Override
    public void loadConfigs() {
    }

    public void loadConfig(final UUID uuid, final GenericCallback<PlayerData> callback) {
        new BukkitRunnable(){

            public void run() {
                PlayerData playerData = null;
                CommonConfig playerConfig = PlayersConfigManager.this.getConfigFile(uuid + ".yml", false);
                if (playerConfig != null) {
                    FileConfiguration config = playerConfig.getConfig();
                    String name = config.getString("name");
                    ArrayList<PlayerDataKit> playerDataKits = new ArrayList<PlayerDataKit>();
                    if (config.contains("kits")) {
                        for (String key : config.getConfigurationSection("kits").getKeys(false)) {
                            long cooldown = config.getLong("kits." + key + ".cooldown");
                            boolean oneTime = config.getBoolean("kits." + key + ".one_time");
                            boolean bought = config.getBoolean("kits." + key + ".bought");
                            PlayerDataKit playerDataKit = new PlayerDataKit(key);
                            playerDataKit.setCooldown(cooldown);
                            playerDataKit.setOneTime(oneTime);
                            playerDataKit.setBought(bought);
                            playerDataKits.add(playerDataKit);
                        }
                    }
                    playerData = new PlayerData(uuid, name);
                    playerData.setKits(playerDataKits);
                }
                final PlayerData finalPlayer = playerData;
                new BukkitRunnable(){

                    public void run() {
                        callback.onDone(finalPlayer);
                    }
                }.runTask((Plugin)PlayersConfigManager.this.plugin);
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void saveConfig(PlayerData playerData) {
        String playerName = playerData.getName();
        CommonConfig playerConfig = this.getConfigFile(playerData.getUuid() + ".yml", true);
        FileConfiguration config = playerConfig.getConfig();
        config.set("name", (Object)playerName);
        config.set("kits", null);
        for (PlayerDataKit playerDataKit : playerData.getKits()) {
            String kitName = playerDataKit.getName();
            config.set("kits." + kitName + ".cooldown", (Object)playerDataKit.getCooldown());
            config.set("kits." + kitName + ".one_time", (Object)playerDataKit.isOneTime());
            config.set("kits." + kitName + ".bought", (Object)playerDataKit.isBought());
        }
        playerConfig.saveConfig();
    }

    @Override
    public void saveConfigs() {
        Map<UUID, PlayerData> players = this.plugin.getPlayerDataManager().getPlayers();
        boolean isMySQL = this.plugin.getConfigsManager().getMainConfigManager().isMySQL();
        if (!isMySQL) {
            for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
                PlayerData playerData = entry.getValue();
                if (playerData.isModified()) {
                    this.saveConfig(playerData);
                }
                playerData.setModified(false);
            }
        }
    }

    public void resetKitForAllPlayers(String kitName) {
        ArrayList<CommonConfig> configs = this.getConfigs();
        for (CommonConfig commonConfig : configs) {
            FileConfiguration config = commonConfig.getConfig();
            if (!config.contains("kits." + kitName)) continue;
            config.set("kits." + kitName, null);
            commonConfig.saveConfig();
        }
    }
}

