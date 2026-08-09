package me.krunsh.kgui.input;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.render.GuiClick;
import me.krunsh.kgui.render.GuiClickPolicy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Saisies et confirmations bornees par des jetons monotones et consommables une fois. */
public final class InputManager implements Listener {
    private final Kgui plugin;
    private final AtomicLong tokens = new AtomicLong();
    private final Map<UUID, InputSession> activeSessions = new HashMap<>();
    private final Map<UUID, ConfirmSession> confirmSessions = new HashMap<>();
    private final Map<UUID, Long> pendingCallbacks = new HashMap<>();

    public InputManager(Kgui plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openTextInput(Player player, String title, String defaultText, Consumer<String> callback) {
        openInput(player, title, defaultText, InputType.TEXT, callback);
    }

    public void openNumberInput(Player player, String title, int defaultValue, int min, int max,
                                BiConsumer<Integer, Boolean> callback) {
        openInput(player, title, String.valueOf(defaultValue), InputType.NUMBER, result -> {
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
                } else if (value > max) {
                    plugin.getMessageManager().send(player, "input-number-too-high",
                        "max", String.valueOf(max), "value", String.valueOf(value));
                    callback.accept(null, false);
                } else {
                    callback.accept(value, true);
                }
            } catch (NumberFormatException exception) {
                plugin.getMessageManager().send(player, "input-invalid-number", "value", result);
                callback.accept(null, false);
            }
        });
    }

    public void openConfirmDialog(Player player, String title, String description,
                                  Runnable onConfirm, Runnable onCancel) {
        discard(player);
        long token = tokens.incrementAndGet();
        String inventoryTitle = plugin.getMessageManager().get("confirm-title", "title", title);
        if (inventoryTitle == null || inventoryTitle.contains("missing:")) {
            inventoryTitle = "§c§lConfirmer: " + title;
        }

        ConfirmHolder holder = new ConfirmHolder(player.getUniqueId(), token);
        Inventory inventory = Bukkit.createInventory(holder, 27, inventoryTitle);
        holder.inventory = inventory;
        inventory.setItem(11, named(Material.EMERALD_BLOCK,
            messageOr("confirm-yes", "§a§lConfirmer"), null));
        inventory.setItem(15, named(Material.REDSTONE_BLOCK,
            messageOr("confirm-no", "§c§lAnnuler"), null));
        inventory.setItem(4, named(Material.PAPER, "§e" + title,
            description == null || description.isEmpty() ? null : "§7" + description));

        confirmSessions.put(player.getUniqueId(),
            new ConfirmSession(token, inventory, onConfirm, onCancel));
        player.openInventory(inventory);
    }

    private ItemStack named(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String messageOr(String key, String fallback) {
        String value = plugin.getMessageManager().get(key);
        return value == null || value.contains("missing:") ? fallback : value;
    }

    private void openInput(Player player, String title, String defaultText,
                           InputType type, Consumer<String> callback) {
        discard(player);
        long token = tokens.incrementAndGet();
        activeSessions.put(player.getUniqueId(),
            new InputSession(token, title, defaultText, type, callback));

        player.closeInventory();
        String prompt = plugin.getMessageManager().get("input-prompt", "title", title);
        if (prompt == null || prompt.contains("missing:")) {
            prompt = "§e§l" + title + " §7»§f Entrez votre reponse dans le chat:";
        }
        player.sendMessage(prompt);
        if (defaultText != null && !defaultText.isEmpty()) {
            player.sendMessage("§7Valeur actuelle: §f" + defaultText);
        }
        player.sendMessage("§7(Tapez '§ccancel§7' pour annuler)");
        plugin.getChatInputListener().registerInput(player, token);
    }

    public void acceptChat(Player player, long token, String message) {
        InputSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.token != token) return;
        activeSessions.remove(player.getUniqueId());
        if (message.equalsIgnoreCase("cancel")) {
            session.callback.accept(null);
            plugin.getMessageManager().send(player, "input-cancelled");
        } else {
            session.callback.accept(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ConfirmSession session = confirmSessions.get(player.getUniqueId());
        Inventory top = event.getView().getTopInventory();
        ConfirmHolder holder = holder(top);
        if (session == null || holder == null || holder.inventory != session.inventory) return;
        if (holder.token != session.token
                || !holder.playerId.equals(player.getUniqueId())) return;

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        int rawSlot = event.getRawSlot();
        GuiClick click = GuiClickPolicy.actionClick(event.getClick());
        if ((click != GuiClick.LEFT && click != GuiClick.RIGHT)
                || event.getClickedInventory() != top
                || (rawSlot != 11 && rawSlot != 15)) return;

        confirmSessions.remove(player.getUniqueId());
        player.closeInventory();
        Runnable callback = rawSlot == 11 ? session.onConfirm : session.onCancel;
        scheduleCallback(player, session.token, callback);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        ConfirmHolder holder = holder(event.getView().getTopInventory());
        if (holder == null || !holder.playerId.equals(player.getUniqueId())) return;

        ConfirmSession current = confirmSessions.get(player.getUniqueId());
        if (current == null || current.token != holder.token
                || current.inventory != holder.inventory) return;
        confirmSessions.remove(player.getUniqueId());
        scheduleCallback(player, current.token, current.onCancel);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (holder(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (holder(event.getSource()) != null || holder(event.getDestination()) != null) {
            event.setCancelled(true);
        }
    }

    public boolean hasActiveInput(Player player) {
        UUID id = player.getUniqueId();
        return activeSessions.containsKey(id) || confirmSessions.containsKey(id);
    }

    public void cancelInput(Player player) {
        InputSession input = activeSessions.remove(player.getUniqueId());
        plugin.getChatInputListener().discard(player.getUniqueId());
        if (input != null && input.callback != null) input.callback.accept(null);

        ConfirmSession confirm = confirmSessions.remove(player.getUniqueId());
        if (confirm != null) scheduleCallback(player, confirm.token, confirm.onCancel);
    }

    /** Purge silencieuse utilisee par close/quit/kick/reload/disable. */
    public void discard(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        activeSessions.remove(id);
        confirmSessions.remove(id);
        pendingCallbacks.remove(id);
        if (plugin.getChatInputListener() != null) plugin.getChatInputListener().discard(id);
    }

    public void cleanup() {
        activeSessions.clear();
        confirmSessions.clear();
        pendingCallbacks.clear();
    }

    int activeCount() {
        return activeSessions.size() + confirmSessions.size() + pendingCallbacks.size();
    }

    private void scheduleCallback(Player player, long token, Runnable callback) {
        if (callback == null) return;
        UUID playerId = player.getUniqueId();
        pendingCallbacks.put(playerId, token);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Long pending = pendingCallbacks.get(playerId);
            if (pending == null || pending.longValue() != token) return;
            pendingCallbacks.remove(playerId);
            if (player.isOnline()) callback.run();
        });
    }

    private ConfirmHolder holder(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ConfirmHolder
            ? (ConfirmHolder) inventory.getHolder() : null;
    }

    public enum InputType { TEXT, NUMBER }

    private static final class InputSession {
        private final long token;
        private final String title;
        private final String defaultValue;
        private final InputType type;
        private final Consumer<String> callback;

        private InputSession(long token, String title, String defaultValue,
                             InputType type, Consumer<String> callback) {
            this.token = token;
            this.title = title;
            this.defaultValue = defaultValue;
            this.type = type;
            this.callback = callback;
        }
    }

    private static final class ConfirmSession {
        private final long token;
        private final Inventory inventory;
        private final Runnable onConfirm;
        private final Runnable onCancel;

        private ConfirmSession(long token, Inventory inventory, Runnable onConfirm, Runnable onCancel) {
            this.token = token;
            this.inventory = inventory;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
        }
    }

    private static final class ConfirmHolder implements InventoryHolder {
        private final UUID playerId;
        private final long token;
        private Inventory inventory;

        private ConfirmHolder(UUID playerId, long token) {
            this.playerId = playerId;
            this.token = token;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
