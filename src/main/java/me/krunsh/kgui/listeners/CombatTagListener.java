package me.krunsh.kgui.listeners;

import me.krunsh.kgui.Kgui;
import net.minelink.ctplus.event.PlayerCombatTagEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener pour CombatTagPlus
 * Ferme les menus quand un joueur entre en combat
 */
public class CombatTagListener implements Listener {

    private final Kgui plugin;

    public CombatTagListener(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Ferme le menu quand le joueur entre en combat
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCombatTag(PlayerCombatTagEvent event) {
        if (!plugin.getConfigManager().isCloseOnCombat()) return;
        
        Player player = event.getPlayer();
        
        // Vérifier si le joueur a un menu ouvert
        if (plugin.getGuiManager().hasOpenMenu(player)) {
            // Fermer le menu
            player.closeInventory();
            plugin.getGuiManager().closeMenu(player, false);
            
            // Envoyer le message
            plugin.getMessageManager().send(player, "closed-combat");
        }
    }
}
