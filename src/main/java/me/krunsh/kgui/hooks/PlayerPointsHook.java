package me.krunsh.kgui.hooks;

import me.krunsh.kgui.Kgui;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Hook pour PlayerPoints
 */
public class PlayerPointsHook {

    private final Kgui plugin;
    private PlayerPointsAPI api;

    public PlayerPointsHook(Kgui plugin) {
        this.plugin = plugin;
        
        PlayerPoints playerPoints = (PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (playerPoints != null) {
            api = playerPoints.getAPI();
        }
    }

    /**
     * Vérifie si l'API est disponible
     */
    public boolean isEnabled() {
        return api != null;
    }

    /**
     * Obtient les points d'un joueur
     */
    public int getPoints(Player player) {
        if (api == null) return 0;
        return api.look(player.getUniqueId());
    }

    /**
     * Vérifie si le joueur a assez de points
     */
    public boolean hasPoints(Player player, int amount) {
        if (api == null) return false;
        return api.look(player.getUniqueId()) >= amount;
    }

    /**
     * Retire des points au joueur
     */
    public boolean takePoints(Player player, int amount) {
        if (api == null) return false;
        return api.take(player.getUniqueId(), amount);
    }

    /**
     * Donne des points au joueur
     */
    public boolean givePoints(Player player, int amount) {
        if (api == null) return false;
        return api.give(player.getUniqueId(), amount);
    }

    /**
     * Définit les points d'un joueur
     */
    public boolean setPoints(Player player, int amount) {
        if (api == null) return false;
        return api.set(player.getUniqueId(), amount);
    }
}
