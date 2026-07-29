/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.configs;

import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.InventoryConfigManager;
import pk.ajneb97.configs.KitsConfigManager;
import pk.ajneb97.configs.MainConfigManager;
import pk.ajneb97.configs.MessagesConfigManager;
import pk.ajneb97.configs.PlayersConfigManager;

public class ConfigsManager {
    private PlayerKits2 plugin;
    private KitsConfigManager kitsConfigManager;
    private MessagesConfigManager messagesConfigManager;
    private MainConfigManager mainConfigManager;
    private PlayersConfigManager playersConfigManager;
    private InventoryConfigManager inventoryConfigManager;

    public ConfigsManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.kitsConfigManager = new KitsConfigManager(plugin, "kits");
        this.messagesConfigManager = new MessagesConfigManager(plugin);
        this.mainConfigManager = new MainConfigManager(plugin);
        this.playersConfigManager = new PlayersConfigManager(plugin, "players");
        this.inventoryConfigManager = new InventoryConfigManager(plugin);
    }

    public void configure() {
        this.kitsConfigManager.configure();
        this.messagesConfigManager.configure();
        this.mainConfigManager.configure();
        if (!this.mainConfigManager.isMySQL()) {
            this.playersConfigManager.configure();
        }
        this.inventoryConfigManager.configure();
    }

    public KitsConfigManager getKitsConfigManager() {
        return this.kitsConfigManager;
    }

    public MessagesConfigManager getMessagesConfigManager() {
        return this.messagesConfigManager;
    }

    public MainConfigManager getMainConfigManager() {
        return this.mainConfigManager;
    }

    public PlayersConfigManager getPlayersConfigManager() {
        return this.playersConfigManager;
    }

    public InventoryConfigManager getInventoryConfigManager() {
        return this.inventoryConfigManager;
    }

    public boolean reload() {
        if (!this.messagesConfigManager.reloadConfig()) {
            return false;
        }
        if (!this.mainConfigManager.reloadConfig()) {
            return false;
        }
        if (!this.inventoryConfigManager.reloadConfig()) {
            return false;
        }
        this.kitsConfigManager.loadConfigs();
        if (this.plugin.getMySQLConnection() == null) {
            this.plugin.reloadPlayerDataSaveTask();
        }
        this.plugin.getVerifyManager().verify();
        return true;
    }
}

