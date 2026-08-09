package me.krunsh.kgui.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.krunsh.kgui.Kgui;
import org.bukkit.entity.Player;

/**
 * Hook pour PlaceholderAPI
 */
public class PlaceholderAPIHook {

    private final Kgui plugin;

    public PlaceholderAPIHook(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Remplace les placeholders dans une string
     */
    public String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Vérifie si une string contient des placeholders
     */
    public boolean containsPlaceholders(String text) {
        return text != null && PlaceholderAPI.containsPlaceholders(text);
    }
}
