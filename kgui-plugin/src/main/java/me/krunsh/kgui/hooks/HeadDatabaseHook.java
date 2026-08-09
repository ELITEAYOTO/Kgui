package me.krunsh.kgui.hooks;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.krunsh.kgui.Kgui;
import org.bukkit.inventory.ItemStack;

/**
 * Hook pour HeadDatabase
 */
public class HeadDatabaseHook {

    private final Kgui plugin;
    private HeadDatabaseAPI api;

    public HeadDatabaseHook(Kgui plugin) {
        this.plugin = plugin;
        this.api = new HeadDatabaseAPI();
    }

    /**
     * Obtient une tête depuis HeadDatabase
     */
    public ItemStack getHead(String id) {
        try {
            return api.getItemHead(id);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get head from HeadDatabase: " + id);
            return null;
        }
    }

    /**
     * Vérifie si un ID de tête existe
     */
    public boolean isHead(String id) {
        try {
            return api.getItemHead(id) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
