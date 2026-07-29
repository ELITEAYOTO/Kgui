/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.database.HikariConnection;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.PlayerData;
import pk.ajneb97.model.PlayerDataKit;
import pk.ajneb97.model.internal.GenericCallback;
import pk.ajneb97.model.internal.SimpleCallback;

public class MySQLConnection {
    private PlayerKits2 plugin;
    private HikariConnection connection;

    public MySQLConnection(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public void setupMySql() {
        FileConfiguration config = this.plugin.getConfigsManager().getMainConfigManager().getConfig();
        try {
            this.connection = new HikariConnection(config);
            this.connection.getHikari().getConnection();
            this.createTables();
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + " &aSuccessfully connected to the Database."));
        }
        catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(PlayerKits2.prefix + " &cError while connecting to the Database."));
        }
    }

    public Connection getConnection() {
        try {
            return this.connection.getHikari().getConnection();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void createTables() {
        try (Connection connection = this.getConnection();){
            PreparedStatement statement1 = connection.prepareStatement("CREATE TABLE IF NOT EXISTS playerkits_players (UUID varchar(200) NOT NULL,  PLAYER_NAME varchar(50),  PRIMARY KEY ( UUID ))");
            statement1.executeUpdate();
            PreparedStatement statement2 = connection.prepareStatement("CREATE TABLE IF NOT EXISTS playerkits_players_kits (ID int NOT NULL AUTO_INCREMENT,  UUID varchar(200) NOT NULL,  NAME varchar(100),  COOLDOWN BIGINT,  ONE_TIME BOOLEAN,  BOUGHT BOOLEAN,  PRIMARY KEY ( ID ),  FOREIGN KEY (UUID) REFERENCES playerkits_players(UUID))");
            statement2.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getPlayer(final String uuid, final GenericCallback<PlayerData> callback) {
        new BukkitRunnable(){

            public void run() {
                PlayerData player = null;
                try (Connection connection = MySQLConnection.this.getConnection();){
                    PreparedStatement statement = connection.prepareStatement("SELECT playerkits_players.UUID, playerkits_players.PLAYER_NAME, playerkits_players_kits.NAME, playerkits_players_kits.COOLDOWN, playerkits_players_kits.ONE_TIME, playerkits_players_kits.BOUGHT FROM playerkits_players LEFT JOIN playerkits_players_kits ON playerkits_players.UUID = playerkits_players_kits.UUID WHERE playerkits_players.UUID = ?");
                    statement.setString(1, uuid);
                    ResultSet result = statement.executeQuery();
                    boolean firstFind = true;
                    while (result.next()) {
                        UUID uuid2 = UUID.fromString(result.getString("UUID"));
                        String playerName = result.getString("PLAYER_NAME");
                        String kitName = result.getString("NAME");
                        long cooldown = result.getLong("COOLDOWN");
                        boolean oneTime = result.getBoolean("ONE_TIME");
                        boolean bought = result.getBoolean("BOUGHT");
                        if (player == null) {
                            player = new PlayerData(uuid2, playerName);
                        }
                        if (kitName == null) continue;
                        PlayerDataKit playerDataKit = new PlayerDataKit(kitName);
                        playerDataKit.setCooldown(cooldown);
                        playerDataKit.setOneTime(oneTime);
                        playerDataKit.setBought(bought);
                        player.addKit(playerDataKit);
                    }
                    final PlayerData finalPlayer = player;
                    new BukkitRunnable(){

                        public void run() {
                            callback.onDone(finalPlayer);
                        }
                    }.runTask((Plugin)MySQLConnection.this.plugin);
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void createPlayer(final PlayerData player, final SimpleCallback callback) {
        new BukkitRunnable(){

            public void run() {
                try (Connection connection = MySQLConnection.this.getConnection();){
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO playerkits_players (UUID, PLAYER_NAME) VALUE (?,?)");
                    statement.setString(1, player.getUuid().toString());
                    statement.setString(2, player.getName());
                    statement.executeUpdate();
                    new BukkitRunnable(){

                        public void run() {
                            callback.onDone();
                        }
                    }.runTask((Plugin)MySQLConnection.this.plugin);
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void updatePlayerName(final PlayerData player) {
        new BukkitRunnable(){

            public void run() {
                try (Connection connection = MySQLConnection.this.getConnection();){
                    PreparedStatement statement = connection.prepareStatement("UPDATE playerkits_players SET PLAYER_NAME=? WHERE UUID=?");
                    statement.setString(1, player.getName());
                    statement.setString(2, player.getUuid().toString());
                    statement.executeUpdate();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void updateKit(final PlayerData player, final PlayerDataKit kit, final boolean mustCreate) {
        new BukkitRunnable(){

            public void run() {
                try (Connection connection = MySQLConnection.this.getConnection();){
                    PreparedStatement statement = null;
                    if (mustCreate) {
                        statement = connection.prepareStatement("INSERT INTO playerkits_players_kits (UUID, NAME, COOLDOWN, ONE_TIME, BOUGHT) VALUE (?,?,?,?,?)");
                        statement.setString(1, player.getUuid().toString());
                        statement.setString(2, kit.getName());
                        statement.setLong(3, kit.getCooldown());
                        statement.setBoolean(4, kit.isOneTime());
                        statement.setBoolean(5, kit.isBought());
                    } else {
                        statement = connection.prepareStatement("UPDATE playerkits_players_kits SET COOLDOWN=?, ONE_TIME=?, BOUGHT=? WHERE UUID=? AND NAME=?");
                        statement.setLong(1, kit.getCooldown());
                        statement.setBoolean(2, kit.isOneTime());
                        statement.setBoolean(3, kit.isBought());
                        statement.setString(4, player.getUuid().toString());
                        statement.setString(5, kit.getName());
                    }
                    statement.executeUpdate();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    public void resetKit(final String uuid, final String kitName, final boolean all) {
        new BukkitRunnable(){

            public void run() {
                try (Connection connection = MySQLConnection.this.getConnection();){
                    PreparedStatement statement;
                    if (all) {
                        statement = connection.prepareStatement("DELETE FROM playerkits_players_kits WHERE NAME=?");
                        statement.setString(1, kitName);
                    } else {
                        statement = connection.prepareStatement("DELETE FROM playerkits_players_kits WHERE UUID=? AND NAME=?");
                        statement.setString(1, uuid);
                        statement.setString(2, kitName);
                    }
                    statement.executeUpdate();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }
}

