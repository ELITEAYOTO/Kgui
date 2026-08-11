package me.krunsh.kgui.listeners;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.actions.ActionOrigin;
import me.krunsh.kgui.config.ConfigManager;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.api.ProviderClickResult;
import me.krunsh.kgui.gui.KguiInventoryHolder;
import me.krunsh.kgui.render.ClickBinding;
import me.krunsh.kgui.render.GuiClick;
import me.krunsh.kgui.render.GuiClickPolicy;
import me.krunsh.kgui.render.InventoryInteractionPolicy;
import me.krunsh.kgui.render.RenderedSlot;
import me.krunsh.kgui.session.CloseReason;
import me.krunsh.kgui.session.PlayerGuiSession;
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

import java.util.List;
import java.util.Collections;

/** Route les interactions exclusivement depuis la session et le slot rendus. */
public final class GuiListener implements Listener {
    private final Kgui plugin;

    public GuiListener(Kgui plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.getGuiMetrics().clickObserved();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!KguiInventoryHolder.isKguiInventory(top)) return;
        plugin.getGuiMetrics().clickReceived();

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        GuiClick click = GuiClickPolicy.actionClick(event.getClick());
        if (!InventoryInteractionPolicy.routeAction(
                rawSlot, top.getSize(), event.getClickedInventory() == top, click)) {
            plugin.getGuiMetrics().clickRouteRejected();
            return;
        }

        PlayerGuiSession session = plugin.getGuiManager().resolveSession(player, top);
        if (session == null) {
            plugin.getGuiMetrics().clickSessionRejected();
            return;
        }
        RenderedSlot rendered = plugin.getGuiManager()
            .resolveClickedSlot(player, top, rawSlot, event.getCurrentItem());
        if (rendered == null || rendered.getBinding() == null) {
            plugin.getGuiMetrics().clickItemRejected();
            return;
        }

        ClickBinding binding = rendered.getBinding();

        int cooldownTicks = binding.getCooldownTicks();
        long cooldownMs = cooldownTicks * 50L;
        if (cooldownTicks > 0 && plugin.getGuiManager().isOnCooldown(
                player, session.getMenuId(), binding.getId(), cooldownMs)) {
            long remaining = plugin.getGuiManager().getCooldownRemaining(
                player, session.getMenuId(), binding.getId(), cooldownMs);
            plugin.getMessageManager().send(player, "item-cooldown",
                "time", String.format("%.1f", remaining / 1000.0));
            return;
        }

        playClickSound(player);
        if (!plugin.getRequirementManager().checkRequirements(player, binding.getRequirements(), true,
                session.getMenuId(), binding.getId())) {
            plugin.getActionManager().executeActions(player, binding.getDenyActions());
            return;
        }

        if (binding.isProviderOwned()) {
            ProviderClickResult result = plugin.getProviderEngine().click(player, session, binding, click);
            if (result.shouldInvalidate()) {
                plugin.getGuiInvalidationBus().publish(InvalidationRequest.playerMenu(
                    player.getUniqueId(), session.getMenuId(),
                    Collections.singleton(binding.getProviderItemId()), "provider-click"));
            }
            if (result.getStatus() == ProviderClickResult.Status.STALE
                    || result.getStatus() == ProviderClickResult.Status.DENIED
                    || result.getStatus() == ProviderClickResult.Status.ERROR) {
                if (result.getMessageKey() != null) plugin.getMessageManager().send(player, result.getMessageKey());
                return;
            }
        }

        List<String> actions = binding.actionsFor(click);
        if (actions.isEmpty()) return;
        plugin.getGuiMetrics().clickAction();
        if (cooldownTicks > 0) {
            plugin.getGuiManager().setItemCooldown(player, session.getMenuId(), binding.getId());
        }
        plugin.getActionManager().executeActions(player, actions,
            binding.isProviderOwned() ? ActionOrigin.PROVIDER : ActionOrigin.LOCAL_MENU);
    }

    private void playClickSound(Player player) {
        ConfigManager.SoundData sound = plugin.getConfigManager().getSoundData("sounds.button_click");
        if (sound != null) {
            player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        KguiInventoryHolder holder = KguiInventoryHolder.getHolder(top);
        if (holder == null) return;

        Player player = (Player) event.getPlayer();
        PlayerGuiSession current = plugin.getGuiManager().getOpenGui(player);
        if (current == null || current.getInventory() != top
                || current.getSessionId() != holder.getSessionId()) return;
        plugin.getGuiManager().closeSession(
            player, holder.getSessionId(), true, CloseReason.PLAYER_CLOSE);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (InventoryInteractionPolicy.cancelDrag(
                KguiInventoryHolder.isKguiInventory(event.getView().getTopInventory()))) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (InventoryInteractionPolicy.cancelMove(
                KguiInventoryHolder.isKguiInventory(event.getSource()),
                KguiInventoryHolder.isKguiInventory(event.getDestination()))) {
            event.setCancelled(true);
        }
    }
}
