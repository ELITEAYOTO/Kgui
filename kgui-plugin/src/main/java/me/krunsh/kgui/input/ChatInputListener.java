package me.krunsh.kgui.input;

import me.krunsh.kgui.Kgui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Écoute les inputs chat des joueurs pour le système d'input
 */
public class ChatInputListener implements Listener {

    private final Kgui plugin;
    
    // Callbacks d'input en attente
    private final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();

    public ChatInputListener(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Enregistre un callback d'input pour un joueur
     */
    public void registerInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }

    /**
     * Vérifie si un joueur a un input en attente
     */
    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    /**
     * Annule un input en attente
     */
    public void cancelInput(Player player) {
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
        if (callback != null) {
            callback.accept(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
        
        if (callback == null) return;
        
        // Annuler l'événement pour ne pas envoyer dans le chat
        event.setCancelled(true);
        
        String message = event.getMessage();
        
        // Exécuter le callback sur le thread principal
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            callback.accept(message);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Nettoyer les inputs en attente
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    public void cleanup() {
        pendingInputs.clear();
    }
}
