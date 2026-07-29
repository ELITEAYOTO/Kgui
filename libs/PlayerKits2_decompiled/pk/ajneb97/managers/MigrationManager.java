/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.KitsConfigManager;
import pk.ajneb97.configs.PlayersConfigManager;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitAction;
import pk.ajneb97.model.KitRequirements;
import pk.ajneb97.model.PlayerData;
import pk.ajneb97.model.PlayerDataKit;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.model.item.KitItemSkullData;

public class MigrationManager {
    private PlayerKits2 plugin;

    public MigrationManager(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public void migrate(final CommandSender sender) {
        new BukkitRunnable(){

            public void run() {
                MigrationManager.this.migrateKits(sender);
                MigrationManager.this.migratePlayers(sender);
                sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &aMigration completed."));
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void migrateKits(CommandSender sender) {
        File bStatsFolder = new File(this.plugin.getDataFolder().getParentFile(), "PlayerKits");
        File configFile = new File(bStatsFolder, "kits.yml");
        if (!configFile.exists()) {
            sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &cPlayerKits1 kits.yml file not found."));
            return;
        }
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)configFile);
        Kit defaultValues = this.plugin.getConfigsManager().getMainConfigManager().getNewKitDefault();
        KitsConfigManager kitsConfigManager = this.plugin.getConfigsManager().getKitsConfigManager();
        if (config.contains("Kits")) {
            for (String kitName : config.getConfigurationSection("Kits").getKeys(false)) {
                try {
                    String path = "Kits." + kitName;
                    Kit kit = new Kit(kitName);
                    boolean oneTime = config.contains(path + ".one_time") ? config.getBoolean(path + ".one_time") : false;
                    int cooldown = config.contains(path + ".cooldown") ? config.getInt(path + ".cooldown") : 0;
                    boolean permissionRequired = config.contains(path + ".permission");
                    boolean autoArmor = config.contains(path + ".auto_armor") ? config.getBoolean(path + ".auto_armor") : false;
                    ArrayList<KitAction> claimActions = new ArrayList<KitAction>();
                    if (config.contains(path + ".Commands")) {
                        for (Object command : config.getStringList(path + ".Commands")) {
                            claimActions.add(new KitAction("console_command: " + (String)command, null, false, false));
                        }
                    }
                    ArrayList<KitItem> items = new ArrayList<KitItem>();
                    if (config.contains(path + ".Items")) {
                        Object command;
                        command = config.getConfigurationSection(path + ".Items").getKeys(false).iterator();
                        while (command.hasNext()) {
                            String key = (String)command.next();
                            String pathItem = path + ".Items." + key;
                            KitItem kitItem = kitItemManager.getKitItemFromV1Config((FileConfiguration)config, pathItem);
                            items.add(kitItem);
                        }
                    }
                    KitRequirements kitRequirements = null;
                    if (config.contains(path + ".price")) {
                        kitRequirements = new KitRequirements();
                        kitRequirements.setPrice(config.getInt(path + ".price"));
                        boolean oneTimeBuy = config.contains(path + ".one_time_buy") ? config.getBoolean(path + ".one_time_buy") : false;
                        kitRequirements.setOneTimeRequirements(oneTimeBuy);
                    }
                    kit.setDisplayItemDefault(this.getDisplayItem(config, path));
                    if (config.contains(path + ".noPermissionsItem")) {
                        kit.setDisplayItemNoPermission(this.getDisplayItem(config, path + ".noPermissionsItem"));
                    }
                    kit.setDisplayItemCooldown(defaultValues.getDisplayItemCooldown());
                    kit.setDisplayItemOneTime(defaultValues.getDisplayItemOneTime());
                    kit.setCooldown(cooldown);
                    kit.setAutoArmor(autoArmor);
                    kit.setOneTime(oneTime);
                    kit.setPermissionRequired(permissionRequired);
                    kit.setItems(items);
                    kit.setClaimActions(claimActions);
                    kit.setErrorActions(new ArrayList<KitAction>());
                    kit.setRequirements(kitRequirements);
                    kitsConfigManager.saveConfig(kit);
                    sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &aKit &7" + kitName + " &amigrated."));
                }
                catch (Exception e) {
                    sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &cError while trying to migrate kit &7" + kitName + "&c, check console."));
                    e.printStackTrace();
                }
            }
        } else {
            sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &cNo kits found."));
            return;
        }
        kitsConfigManager.loadConfigs();
        this.plugin.getVerifyManager().verify();
    }

    public void migratePlayers(CommandSender sender) {
        File bStatsFolder = new File(this.plugin.getDataFolder().getParentFile(), "PlayerKits");
        File configFile = new File(bStatsFolder, "players.yml");
        if (!configFile.exists()) {
            sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &cPlayerKits1 players.yml file not found."));
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)configFile);
        PlayersConfigManager playersConfigManager = this.plugin.getConfigsManager().getPlayersConfigManager();
        if (config.contains("Players")) {
            for (String uuid : config.getConfigurationSection("Players").getKeys(false)) {
                try {
                    String path = "Players." + uuid;
                    String name = config.getString(path + ".name");
                    PlayerData playerData = new PlayerData(UUID.fromString(uuid), name);
                    for (String kitName : config.getConfigurationSection(path).getKeys(false)) {
                        if (kitName.equals("name")) continue;
                        PlayerDataKit playerDataKit = new PlayerDataKit(kitName);
                        playerDataKit.setBought(config.getBoolean(path + "." + kitName + ".buyed"));
                        playerDataKit.setCooldown(config.getLong(path + "." + kitName + ".cooldown"));
                        playerDataKit.setOneTime(config.getBoolean(path + "." + kitName + ".one_time"));
                        playerData.addKit(playerDataKit);
                    }
                    playersConfigManager.saveConfig(playerData);
                    sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &aPlayer &7" + name + " &amigrated."));
                }
                catch (Exception e) {
                    sender.sendMessage(PlayerKits2.prefix + MessagesManager.getLegacyColoredMessage(" &cError while trying to migrate player data with uuid &7" + uuid + "&c, check console."));
                    e.printStackTrace();
                }
            }
        }
        playersConfigManager.configure();
    }

    public KitItem getDisplayItem(YamlConfiguration config, String path) {
        KitItem kitItem = new KitItem(config.getString(path + ".display_item"));
        String name = config.contains(path + ".display_name") ? config.getString(path + ".display_name") : null;
        List lore = config.contains(path + ".display_lore") ? config.getStringList(path + ".display_lore") : null;
        int customModelData = config.contains(path + ".display_item_custom_model_data") ? config.getInt(path + ".display_item_custom_model_data") : 0;
        KitItemSkullData skullData = null;
        if (config.contains(path + ".display_item_skulldata")) {
            String[] fullSkull = config.getString(path + ".display_item_skulldata").split(";");
            skullData = new KitItemSkullData(null, fullSkull[1], fullSkull[0]);
        }
        kitItem.setName(name);
        kitItem.setLore(lore);
        kitItem.setCustomModelData(customModelData);
        kitItem.setSkullData(skullData);
        return kitItem;
    }
}

