/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package pk.ajneb97.tasks;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pk.ajneb97.PlayerKits2;

public class PlayerDataSaveTask {
    private PlayerKits2 plugin;
    private boolean end;

    public PlayerDataSaveTask(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.end = false;
    }

    public void end() {
        this.end = true;
    }

    public void start(int seconds) {
        long ticks = (long)seconds * 20L;
        new BukkitRunnable(){

            public void run() {
                if (PlayerDataSaveTask.this.end) {
                    this.cancel();
                } else {
                    PlayerDataSaveTask.this.execute();
                }
            }
        }.runTaskTimerAsynchronously((Plugin)this.plugin, 0L, ticks);
    }

    public void execute() {
        this.plugin.getConfigsManager().getPlayersConfigManager().saveConfigs();
    }
}

