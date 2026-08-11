package me.krunsh.kgui.input;

import me.krunsh.kgui.Kgui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Capture seulement un jeton ; les callbacks restent possedes par InputManager. */
public final class ChatInputListener implements Listener {
    private final Kgui plugin;
    private final Map<UUID, Long> pendingTokens = new ConcurrentHashMap<>();

    public ChatInputListener(Kgui plugin) {
        this.plugin = plugin;
    }

    public void registerInput(Player player, long token) {
        pendingTokens.put(player.getUniqueId(), token);
    }

    public boolean hasPendingInput(Player player) {
        return pendingTokens.containsKey(player.getUniqueId());
    }

    public void discard(UUID playerId) {
        pendingTokens.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Long token = pendingTokens.remove(player.getUniqueId());
        if (token == null) return;

        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin,
            () -> plugin.getInputManager().acceptChat(player, token, message));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        discard(event.getPlayer().getUniqueId());
        plugin.getInputManager().discard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        discard(event.getPlayer().getUniqueId());
        plugin.getInputManager().discard(event.getPlayer());
    }

    public void cleanup() {
        pendingTokens.clear();
    }

    int pendingCount() {
        return pendingTokens.size();
    }
}
