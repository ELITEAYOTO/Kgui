package me.krunsh.kgui.input;

import me.krunsh.kgui.Kgui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Gestionnaire d'input utilisateur via Anvil GUI
 * Support pour [input_text] et [input_number]
 */
public class InputManager implements Listener {

    private final Kgui plugin;
    
    // Sessions d'input actives
    private final Map<UUID, InputSession> activeSessions = new HashMap<>();
    
    // Confirmations en attente
    private final Map<UUID, ConfirmSession> confirmSessions = new HashMap<>();

    public InputManager(Kgui plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre un input texte via Anvil GUI
     * 
     * @param player Le joueur
     * @param title Titre affiché sur l'item (placeholder)
     * @param defaultText Texte par défaut
     * @param callback Callback avec le texte entré
     */
    public void openTextInput(Player player, String title, String defaultText, Consumer<String> callback) {
        openInput(player, title, defaultText, InputType.TEXT, callback);
    }

    /**
     * Ouvre un input numérique via Anvil GUI
     * 
     * @param player Le joueur
     * @param title Titre affiché
     * @param defaultValue Valeur par défaut
     * @param min Minimum autorisé
     * @param max Maximum autorisé
     * @param callback Callback avec le nombre entré
     */
    public void openNumberInput(Player player, String title, int defaultValue, int min, int max, BiConsumer<Integer, Boolean> callback) {
        Consumer<String> textCallback = (result) -> {
            if (result == null) {
                callback.accept(null, false);
                return;
            }
            
            try {
                int value = Integer.parseInt(result.trim());
                
                if (value < min) {
                    plugin.getMessageManager().send(player, "input-number-too-low", 
                        "min", String.valueOf(min), "value", String.valueOf(value));
                    callback.accept(null, false);
                    return;
                }
                
                if (value > max) {
                    plugin.getMessageManager().send(player, "input-number-too-high", 
                        "max", String.valueOf(max), "value", String.valueOf(value));
                    callback.accept(null, false);
                    return;
                }
                
                callback.accept(value, true);
                
            } catch (NumberFormatException e) {
                plugin.getMessageManager().send(player, "input-invalid-number", "value", result);
                callback.accept(null, false);
            }
        };
        
        openInput(player, title, String.valueOf(defaultValue), InputType.NUMBER, textCallback);
    }

    /**
     * Ouvre un dialogue de confirmation
     *
     * @param player Le joueur
     * @param title Titre de la confirmation
     * @param description Description de l'action
     * @param onConfirm Action si confirmé
     * @param onCancel Action si annulé
     */
    public void openConfirmDialog(Player player, String title, String description, 
                                  Runnable onConfirm, Runnable onCancel) {
        // Créer une session de confirmation
        ConfirmSession session = new ConfirmSession(title, description, onConfirm, onCancel);
        confirmSessions.put(player.getUniqueId(), session);
        
        // Créer un inventaire de confirmation simple
        String invTitle = plugin.getMessageManager().get("confirm-title", "title", title);
        if (invTitle == null || invTitle.contains("missing:")) invTitle = "§c§lConfirmer: " + title;
        
        Inventory inv = Bukkit.createInventory(null, 27, invTitle);
        
        // Bouton Confirmer (slot 11)
        ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        String confirmText = plugin.getMessageManager().get("confirm-yes");
        confirmMeta.setDisplayName(confirmText.contains("missing:") ? "§a§lConfirmer" : confirmText);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(11, confirm);
        
        // Bouton Annuler (slot 15)
        ItemStack cancel = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancel.getItemMeta();
        String cancelText = plugin.getMessageManager().get("confirm-no");
        cancelMeta.setDisplayName(cancelText.contains("missing:") ? "§c§lAnnuler" : cancelText);
        cancel.setItemMeta(cancelMeta);
        inv.setItem(15, cancel);
        
        // Afficher l'info au centre (slot 4)
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e" + title);
        if (description != null && !description.isEmpty()) {
            infoMeta.setLore(java.util.Arrays.asList("§7" + description));
        }
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);
        
        player.openInventory(inv);
    }

    /**
     * Ouvre un input générique
     */
    private void openInput(Player player, String title, String defaultText, InputType type, Consumer<String> callback) {
        // En 1.8.8, on utilise une méthode alternative avec chat
        // Car l'Anvil GUI natif est complexe sans NMS
        
        InputSession session = new InputSession(title, defaultText, type, callback);
        activeSessions.put(player.getUniqueId(), session);
        
        // Fermer le menu actuel s'il y en a un
        player.closeInventory();
        
        // Demander l'input via chat
        String prompt = plugin.getMessageManager().get("input-prompt", "title", title);
        if (prompt == null || prompt.contains("missing:")) prompt = "§e§l" + title + " §7»§f Entrez votre réponse dans le chat:";
        player.sendMessage(prompt);
        
        if (defaultText != null && !defaultText.isEmpty()) {
            player.sendMessage("§7Valeur actuelle: §f" + defaultText);
        }
        
        player.sendMessage("§7(Tapez '§ccancel§7' pour annuler)");
        
        // Écouter le chat
        plugin.getChatInputListener().registerInput(player, (message) -> {
            InputSession inputSession = activeSessions.remove(player.getUniqueId());
            if (inputSession == null) return;
            
            if (message.equalsIgnoreCase("cancel")) {
                inputSession.getCallback().accept(null);
                plugin.getMessageManager().send(player, "input-cancelled");
                return;
            }
            
            inputSession.getCallback().accept(message);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Vérifier si c'est une GUI de confirmation
        ConfirmSession session = confirmSessions.get(player.getUniqueId());
        if (session == null) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        if (slot == 11) { // Confirmer
            confirmSessions.remove(player.getUniqueId());
            player.closeInventory();
            
            // Exécuter la confirmation
            if (session.getOnConfirm() != null) {
                Bukkit.getScheduler().runTask(plugin, session.getOnConfirm());
            }
        } else if (slot == 15) { // Annuler
            confirmSessions.remove(player.getUniqueId());
            player.closeInventory();
            
            // Exécuter l'annulation
            if (session.getOnCancel() != null) {
                Bukkit.getScheduler().runTask(plugin, session.getOnCancel());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        // Si une confirmation est fermée sans choix, considérer comme annulée
        ConfirmSession session = confirmSessions.remove(player.getUniqueId());
        if (session != null && session.getOnCancel() != null) {
            Bukkit.getScheduler().runTask(plugin, session.getOnCancel());
        }
    }

    /**
     * Vérifie si un joueur a une session active
     */
    public boolean hasActiveInput(Player player) {
        return activeSessions.containsKey(player.getUniqueId()) 
            || confirmSessions.containsKey(player.getUniqueId());
    }

    /**
     * Annule toute session active
     */
    public void cancelInput(Player player) {
        InputSession inputSession = activeSessions.remove(player.getUniqueId());
        if (inputSession != null && inputSession.getCallback() != null) {
            inputSession.getCallback().accept(null);
        }
        
        ConfirmSession confirmSession = confirmSessions.remove(player.getUniqueId());
        if (confirmSession != null && confirmSession.getOnCancel() != null) {
            Bukkit.getScheduler().runTask(plugin, confirmSession.getOnCancel());
        }
    }

    public void cleanup() {
        activeSessions.clear();
        confirmSessions.clear();
    }

    // ========== Classes internes ==========

    public enum InputType {
        TEXT,
        NUMBER
    }

    private static class InputSession {
        private final String title;
        private final String defaultValue;
        private final InputType type;
        private final Consumer<String> callback;

        public InputSession(String title, String defaultValue, InputType type, Consumer<String> callback) {
            this.title = title;
            this.defaultValue = defaultValue;
            this.type = type;
            this.callback = callback;
        }

        public String getTitle() { return title; }
        public String getDefaultValue() { return defaultValue; }
        public InputType getType() { return type; }
        public Consumer<String> getCallback() { return callback; }
    }

    private static class ConfirmSession {
        private final String title;
        private final String description;
        private final Runnable onConfirm;
        private final Runnable onCancel;

        public ConfirmSession(String title, String description, Runnable onConfirm, Runnable onCancel) {
            this.title = title;
            this.description = description;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Runnable getOnConfirm() { return onConfirm; }
        public Runnable getOnCancel() { return onCancel; }
    }
}
