package me.krunsh.kgui.data;

import me.krunsh.kgui.Kgui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des données temporaires des joueurs
 * Pour stocker les résultats d'inputs et autres données de session
 */
public class PlayerDataManager implements Listener {

    private final Kgui plugin;
    
    // Structure: UUID -> (key -> value)
    private final Map<UUID, Map<String, String>> playerData = new ConcurrentHashMap<>();

    public PlayerDataManager(Kgui plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Définit une donnée pour un joueur
     */
    public void setData(Player player, String key, String value) {
        playerData.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(key, value);
    }

    /**
     * Obtient une donnée d'un joueur
     */
    public String getData(Player player, String key) {
        Map<String, String> data = playerData.get(player.getUniqueId());
        return data != null ? data.get(key) : null;
    }

    /**
     * Obtient une donnée avec valeur par défaut
     */
    public String getData(Player player, String key, String defaultValue) {
        String value = getData(player, key);
        return value != null ? value : defaultValue;
    }

    /**
     * Vérifie si une donnée existe
     */
    public boolean hasData(Player player, String key) {
        Map<String, String> data = playerData.get(player.getUniqueId());
        return data != null && data.containsKey(key);
    }

    /**
     * Supprime une donnée
     */
    public void removeData(Player player, String key) {
        Map<String, String> data = playerData.get(player.getUniqueId());
        if (data != null) {
            data.remove(key);
        }
    }

    /**
     * Supprime toutes les données d'un joueur
     */
    public void clearData(Player player) {
        playerData.remove(player.getUniqueId());
    }

    /**
     * Remplace les placeholders de données dans un texte
     * Format: {kgui_data_key}
     */
    public String replacePlaceholders(Player player, String text) {
        if (text == null || !text.contains("{kgui_data_")) return text;
        
        Map<String, String> data = playerData.get(player.getUniqueId());
        if (data == null) return text;
        
        for (Map.Entry<String, String> entry : data.entrySet()) {
            text = text.replace("{kgui_data_" + entry.getKey() + "}", entry.getValue());
        }
        
        return text;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Nettoyer les données du joueur quand il quitte
        playerData.remove(event.getPlayer().getUniqueId());
    }

    public void cleanup() {
        playerData.clear();
    }
}
