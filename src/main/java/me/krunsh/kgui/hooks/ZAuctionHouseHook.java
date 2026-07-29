package me.krunsh.kgui.hooks;

import me.krunsh.kgui.Kgui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Hook pour ZAuctionHouse
 * Bloque la vente d'items GUI
 */
public class ZAuctionHouseHook implements Listener {

    private final Kgui plugin;

    public ZAuctionHouseHook(Kgui plugin) {
        this.plugin = plugin;
        
        // Enregistrer le listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Vérifie si un item peut être vendu
     */
    public boolean canSellItem(ItemStack item) {
        // Bloquer les items GUI
        return !plugin.getGuiManager().isGuiItem(item);
    }

    /**
     * Bloque la mise en vente d'items GUI
     * Note: L'event exact dépend de la version de ZAuctionHouse
     * Cette implémentation tente de couvrir les cas courants
     */
    // @EventHandler(priority = EventPriority.LOWEST)
    // public void onAuctionCreate(AuctionCreateEvent event) {
    //     if (!canSellItem(event.getItem())) {
    //         event.setCancelled(true);
    //         event.getPlayer().sendMessage(plugin.getMessageManager().get("security.auction_blocked"));
    //     }
    // }
    
    /**
     * Alternative: vérifier via commande
     * On peut aussi intercepter PlayerCommandPreprocessEvent pour /ah sell
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigManager().isBlockAuctionHouse()) return;
        
        String command = event.getMessage().toLowerCase();
        
        // Vérifier si c'est une commande de vente
        if (command.startsWith("/ah sell") || command.startsWith("/auc sell") || 
            command.startsWith("/auction sell") || command.startsWith("/zauctionhouse sell")) {
            
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getItemInHand();
            
            if (itemInHand != null && plugin.getGuiManager().isGuiItem(itemInHand)) {
                event.setCancelled(true);
                plugin.getMessageManager().send(player, "auction-blocked");
            }
        }
    }
}
