/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.java.JavaPlugin
 */
package pk.ajneb97;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pk.ajneb97.MainCommand;
import pk.ajneb97.api.ExpansionPlayerKits;
import pk.ajneb97.api.PlayerKitsAPI;
import pk.ajneb97.configs.ConfigsManager;
import pk.ajneb97.database.MySQLConnection;
import pk.ajneb97.listeners.InventoryEditListener;
import pk.ajneb97.listeners.OtherListener;
import pk.ajneb97.listeners.PlayerListener;
import pk.ajneb97.managers.DependencyManager;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.managers.KitsManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.managers.MigrationManager;
import pk.ajneb97.managers.PlayerDataManager;
import pk.ajneb97.managers.UpdateCheckerManager;
import pk.ajneb97.managers.VerifyManager;
import pk.ajneb97.managers.dependencies.Metrics;
import pk.ajneb97.managers.edit.InventoryEditManager;
import pk.ajneb97.model.internal.UpdateCheckerResult;
import pk.ajneb97.tasks.InventoryUpdateTaskManager;
import pk.ajneb97.tasks.PlayerDataSaveTask;
import pk.ajneb97.utils.ServerVersion;
import pk.ajneb97.versions.NMSManager;

public class PlayerKits2
extends JavaPlugin {
    public String version = this.getDescription().getVersion();
    public static String prefix;
    public static ServerVersion serverVersion;
    private KitItemManager kitItemManager;
    private KitsManager kitsManager;
    private DependencyManager dependencyManager;
    private ConfigsManager configsManager;
    private MessagesManager messagesManager;
    private PlayerDataManager playerDataManager;
    private InventoryManager inventoryManager;
    private InventoryEditManager inventoryEditManager;
    private NMSManager nmsManager;
    private UpdateCheckerManager updateCheckerManager;
    private VerifyManager verifyManager;
    private MigrationManager migrationManager;
    private InventoryUpdateTaskManager inventoryUpdateTaskManager;
    private PlayerDataSaveTask playerDataSaveTask;
    private MySQLConnection mySQLConnection;

    public void onEnable() {
        this.setVersion();
        this.setPrefix();
        this.registerCommands();
        this.registerEvents();
        this.kitItemManager = new KitItemManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.inventoryEditManager = new InventoryEditManager(this);
        this.kitsManager = new KitsManager(this);
        this.dependencyManager = new DependencyManager(this);
        this.nmsManager = new NMSManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.configsManager = new ConfigsManager(this);
        this.configsManager.configure();
        this.migrationManager = new MigrationManager(this);
        this.inventoryUpdateTaskManager = new InventoryUpdateTaskManager(this);
        this.inventoryUpdateTaskManager.start();
        this.verifyManager = new VerifyManager(this);
        this.verifyManager.verify();
        if (this.configsManager.getMainConfigManager().isMySQL()) {
            this.mySQLConnection = new MySQLConnection(this);
            this.mySQLConnection.setupMySql();
        } else {
            this.reloadPlayerDataSaveTask();
        }
        PlayerKitsAPI api = new PlayerKitsAPI(this);
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ExpansionPlayerKits(this).register();
        }
        Metrics metrics = new Metrics((Plugin)this, 19795);
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix + "&eHas been enabled! &fVersion: " + this.version));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix + "&eThanks for using my plugin!   &f~Ajneb97"));
        this.updateCheckerManager = new UpdateCheckerManager(this.version);
        this.updateMessage(this.updateCheckerManager.check());
    }

    public void onDisable() {
        this.configsManager.getPlayersConfigManager().saveConfigs();
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix + "&eHas been disabled! &fVersion: " + this.version));
    }

    public void registerCommands() {
        this.getCommand("kit").setExecutor((CommandExecutor)new MainCommand(this));
    }

    public void registerEvents() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents((Listener)new PlayerListener(this), (Plugin)this);
        pm.registerEvents((Listener)new InventoryEditListener(this), (Plugin)this);
        pm.registerEvents((Listener)new OtherListener(), (Plugin)this);
    }

    public void reloadPlayerDataSaveTask() {
        if (this.playerDataSaveTask != null) {
            this.playerDataSaveTask.end();
        }
        this.playerDataSaveTask = new PlayerDataSaveTask(this);
        this.playerDataSaveTask.start(this.configsManager.getMainConfigManager().getConfig().getInt("player_data_save_time"));
    }

    public void setPrefix() {
        prefix = MessagesManager.getLegacyColoredMessage("&8[&bPlayerKits&a\u00b2&8] ");
    }

    public void setVersion() {
        String bukkitVersion;
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        switch (bukkitVersion = Bukkit.getServer().getBukkitVersion().split("-")[0]) {
            case "1.20.5": 
            case "1.20.6": {
                serverVersion = ServerVersion.v1_20_R4;
                break;
            }
            case "1.21": 
            case "1.21.1": {
                serverVersion = ServerVersion.v1_21_R1;
                break;
            }
            case "1.21.2": 
            case "1.21.3": {
                serverVersion = ServerVersion.v1_21_R2;
                break;
            }
            case "1.21.4": {
                serverVersion = ServerVersion.v1_21_R3;
                break;
            }
            case "1.21.5": {
                serverVersion = ServerVersion.v1_21_R4;
                break;
            }
            case "1.21.6": 
            case "1.21.7": 
            case "1.21.8": {
                serverVersion = ServerVersion.v1_21_R5;
                break;
            }
            case "1.21.9": 
            case "1.21.10": {
                serverVersion = ServerVersion.v1_21_R6;
                break;
            }
            case "1.21.11": {
                serverVersion = ServerVersion.v1_21_R7;
                break;
            }
            case "26.1": {
                serverVersion = ServerVersion.v26_1;
                break;
            }
            default: {
                try {
                    serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
                    break;
                }
                catch (Exception e) {
                    serverVersion = ServerVersion.v26_1;
                }
            }
        }
    }

    public KitItemManager getKitItemManager() {
        return this.kitItemManager;
    }

    public KitsManager getKitsManager() {
        return this.kitsManager;
    }

    public MessagesManager getMessagesManager() {
        return this.messagesManager;
    }

    public void setMessagesManager(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
    }

    public ConfigsManager getConfigsManager() {
        return this.configsManager;
    }

    public DependencyManager getDependencyManager() {
        return this.dependencyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    public InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }

    public InventoryEditManager getInventoryEditManager() {
        return this.inventoryEditManager;
    }

    public MySQLConnection getMySQLConnection() {
        return this.mySQLConnection;
    }

    public NMSManager getNmsManager() {
        return this.nmsManager;
    }

    public UpdateCheckerManager getUpdateCheckerManager() {
        return this.updateCheckerManager;
    }

    public VerifyManager getVerifyManager() {
        return this.verifyManager;
    }

    public MigrationManager getMigrationManager() {
        return this.migrationManager;
    }

    public void updateMessage(UpdateCheckerResult result) {
        if (!result.isError()) {
            String latestVersion = result.getLatestVersion();
            if (latestVersion != null) {
                Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cThere is a new version available. &e(&7" + latestVersion + "&e)"));
                Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cYou can download it at: &fhttps://modrinth.com/plugin/playerkits-2"));
            }
        } else {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix + "&cError while checking update."));
        }
    }
}

