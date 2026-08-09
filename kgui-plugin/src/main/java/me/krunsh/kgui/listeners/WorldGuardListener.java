package me.krunsh.kgui.listeners;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Set;

/**
 * Listener pour les triggers de menu par région WorldGuard
 */
public class WorldGuardListener implements Listener {

    private final Kgui plugin;

    public WorldGuardListener(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Détecte l'entrée dans une région
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimisation: ignorer si pas de déplacement significatif
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Vérifier le cooldown
        if (!plugin.getHookManager().getWorldGuardHook().checkTriggerCooldown(player)) {
            return;
        }
        
        // Mettre à jour les régions et obtenir les nouvelles
        Set<String> enteredRegions = plugin.getHookManager().getWorldGuardHook().updatePlayerRegions(player);
        
        if (enteredRegions.isEmpty()) return;
        
        // Chercher les menus qui se déclenchent pour ces régions
        for (String regionId : enteredRegions) {
            List<MenuData> menus = plugin.getMenuManager().findByRegionEnter(regionId);
            
            for (MenuData menu : menus) {
                // Vérifier que le joueur peut ouvrir ce menu
                if (menu.getPermission() != null && !player.hasPermission(menu.getPermission())) {
                    continue;
                }
                
                // Ouvrir le menu
                plugin.getGuiManager().openMenu(player, menu.getId());
                
                // Un seul menu à la fois
                return;
            }
        }
    }

    /**
     * Nettoie les données à la déconnexion
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getHookManager().getWorldGuardHook().cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
