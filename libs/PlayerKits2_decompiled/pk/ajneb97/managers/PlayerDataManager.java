/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.PlayersConfigManager;
import pk.ajneb97.database.MySQLConnection;
import pk.ajneb97.model.PlayerData;
import pk.ajneb97.model.internal.GenericCallback;
import pk.ajneb97.model.internal.PlayerKitsMessageResult;
import pk.ajneb97.utils.OtherUtils;

public class PlayerDataManager {
    private PlayerKits2 plugin;
    private Map<UUID, PlayerData> players;
    private Map<String, UUID> playerNames;

    public PlayerDataManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.players = new HashMap<UUID, PlayerData>();
        this.playerNames = new HashMap<String, UUID>();
    }

    public Map<UUID, PlayerData> getPlayers() {
        return this.players;
    }

    public void addPlayer(PlayerData p) {
        this.players.put(p.getUuid(), p);
        this.playerNames.put(p.getName(), p.getUuid());
    }

    public PlayerData getPlayer(Player player, boolean create) {
        PlayerData playerData = this.players.get(player.getUniqueId());
        if (playerData == null && create) {
            playerData = new PlayerData(player.getUniqueId(), player.getName());
            this.addPlayer(playerData);
        }
        return playerData;
    }

    private void updatePlayerName(String oldName, String newName, UUID uuid) {
        if (oldName != null) {
            this.playerNames.remove(oldName);
        }
        this.playerNames.put(newName, uuid);
    }

    public PlayerData getPlayerByUUID(UUID uuid) {
        return this.players.get(uuid);
    }

    private UUID getPlayerUUID(String name) {
        return this.playerNames.get(name);
    }

    public PlayerData getPlayerByName(String name) {
        UUID uuid = this.getPlayerUUID(name);
        return this.players.get(uuid);
    }

    public void removePlayer(PlayerData playerData) {
        this.players.remove(playerData.getUuid());
        this.playerNames.remove(playerData.getName());
    }

    public void removePlayerByUUID(UUID uuid) {
        this.players.remove(uuid);
    }

    public void setKitCooldown(Player player, String kitName, long cooldown) {
        PlayerData playerData = this.getPlayer(player, true);
        boolean creating = playerData.setKitCooldown(kitName, cooldown);
        playerData.setModified(true);
        if (this.plugin.getMySQLConnection() != null) {
            this.plugin.getMySQLConnection().updateKit(playerData, playerData.getKit(kitName), creating);
        }
    }

    public long getKitCooldown(Player player, String kitName) {
        PlayerData playerData = this.getPlayerByUUID(player.getUniqueId());
        if (playerData == null) {
            return 0L;
        }
        return playerData.getKitCooldown(kitName);
    }

    public String getKitCooldownString(long playerCooldown) {
        long currentMillis = System.currentTimeMillis();
        long millisDif = playerCooldown - currentMillis;
        String timeStringMillisDif = OtherUtils.getTime(millisDif / 1000L, this.plugin.getMessagesManager());
        return timeStringMillisDif;
    }

    public void setKitOneTime(Player player, String kitName) {
        PlayerData playerData = this.getPlayer(player, true);
        boolean creating = playerData.setKitOneTime(kitName);
        playerData.setModified(true);
        if (this.plugin.getMySQLConnection() != null) {
            this.plugin.getMySQLConnection().updateKit(playerData, playerData.getKit(kitName), creating);
        }
    }

    public boolean isKitOneTime(Player player, String kitName) {
        PlayerData playerData = this.getPlayerByUUID(player.getUniqueId());
        if (playerData == null) {
            return false;
        }
        return playerData.getKitOneTime(kitName);
    }

    public void setKitBought(Player player, String kitName) {
        PlayerData playerData = this.getPlayer(player, true);
        boolean creating = playerData.setKitBought(kitName);
        playerData.setModified(true);
        if (this.plugin.getMySQLConnection() != null) {
            this.plugin.getMySQLConnection().updateKit(playerData, playerData.getKit(kitName), creating);
        }
    }

    public boolean isKitBought(Player player, String kitName) {
        PlayerData playerData = this.getPlayerByUUID(player.getUniqueId());
        if (playerData == null) {
            return false;
        }
        return playerData.getKitHasBought(kitName);
    }

    public PlayerKitsMessageResult resetKitForPlayer(String playerName, String kitName) {
        FileConfiguration messagesConfig = this.plugin.getConfigsManager().getMessagesConfigManager().getConfig();
        if (Bukkit.getPlayer((String)playerName) == null) {
            return PlayerKitsMessageResult.error(messagesConfig.getString("playerNotOnline").replace("%player%", playerName));
        }
        PlayerData playerData = this.getPlayerByName(playerName);
        if (playerData == null) {
            return PlayerKitsMessageResult.error(messagesConfig.getString("playerDataNotFound").replace("%player%", playerName));
        }
        playerData.resetKit(kitName);
        if (this.plugin.getMySQLConnection() != null) {
            this.plugin.getMySQLConnection().resetKit(playerData.getUuid().toString(), kitName, false);
        }
        return PlayerKitsMessageResult.success();
    }

    public void resetKitForAllPlayers(final String kitName, final GenericCallback<PlayerKitsMessageResult> callback) {
        new BukkitRunnable(){

            public void run() {
                MySQLConnection mySQLConnection = PlayerDataManager.this.plugin.getMySQLConnection();
                if (mySQLConnection == null) {
                    PlayersConfigManager playerConfigsManager = PlayerDataManager.this.plugin.getConfigsManager().getPlayersConfigManager();
                    playerConfigsManager.resetKitForAllPlayers(kitName);
                }
                new BukkitRunnable(){

                    public void run() {
                        PlayerDataManager.this.players.values().forEach(p -> p.resetKit(kitName));
                        if (PlayerDataManager.this.plugin.getMySQLConnection() != null) {
                            PlayerDataManager.this.plugin.getMySQLConnection().resetKit(null, kitName, true);
                        }
                        callback.onDone(PlayerKitsMessageResult.success());
                    }
                }.runTask((Plugin)PlayerDataManager.this.plugin);
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void manageJoin(Player player) {
        if (this.plugin.getMySQLConnection() != null) {
            MySQLConnection mySQLConnection = this.plugin.getMySQLConnection();
            UUID uuid = player.getUniqueId();
            mySQLConnection.getPlayer(uuid.toString(), playerData -> {
                if (playerData != null) {
                    this.addPlayer((PlayerData)playerData);
                    if (!playerData.getName().equals(player.getName())) {
                        this.updatePlayerName(playerData.getName(), player.getName(), player.getUniqueId());
                        playerData.setName(player.getName());
                        mySQLConnection.updatePlayerName((PlayerData)playerData);
                    }
                } else {
                    playerData = new PlayerData(uuid, player.getName());
                    this.addPlayer((PlayerData)playerData);
                    mySQLConnection.createPlayer((PlayerData)playerData, () -> this.plugin.getKitsManager().giveFirstJoinKit(player));
                }
            });
        } else {
            this.plugin.getConfigsManager().getPlayersConfigManager().loadConfig(player.getUniqueId(), playerData -> {
                if (playerData != null) {
                    this.addPlayer((PlayerData)playerData);
                    if (playerData.getName() == null || !playerData.getName().equals(player.getName())) {
                        this.updatePlayerName(playerData.getName(), player.getName(), player.getUniqueId());
                        playerData.setName(player.getName());
                        playerData.setModified(true);
                    }
                } else {
                    playerData = new PlayerData(player.getUniqueId(), player.getName());
                    playerData.setModified(true);
                    this.addPlayer((PlayerData)playerData);
                    this.plugin.getKitsManager().giveFirstJoinKit(player);
                }
            });
        }
    }

    public void manageLeave(Player player) {
        final PlayerData playerData = this.getPlayer(player, false);
        if (playerData != null) {
            if (this.plugin.getMySQLConnection() == null && playerData.isModified()) {
                new BukkitRunnable(){

                    public void run() {
                        PlayerDataManager.this.plugin.getConfigsManager().getPlayersConfigManager().saveConfig(playerData);
                    }
                }.runTaskAsynchronously((Plugin)this.plugin);
            }
            this.removePlayer(playerData);
        }
    }
}

