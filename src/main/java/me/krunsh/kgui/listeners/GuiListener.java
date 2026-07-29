package me.krunsh.kgui.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.config.ConfigManager;
import me.krunsh.kgui.gui.KguiInventoryHolder;
import me.krunsh.kgui.gui.OpenGui;
import me.krunsh.kgui.menu.MenuData;
import me.krunsh.kgui.menu.MenuItem;

/**
 * Listener principal pour les interactions GUI
 */
public class GuiListener implements Listener {

    private final Kgui plugin;

    public GuiListener(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère les clics dans l'inventaire
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        
        // Vérifier si c'est un inventaire Kgui
        if (!KguiInventoryHolder.isKguiInventory(topInventory)) return;
        
        // Toujours annuler pour les GUIs
        event.setCancelled(true);
        
        // Ignorer les clics en dehors
        if (event.getClickedInventory() == null) return;
        
        // Ignorer les clics dans l'inventaire du joueur
        if (event.getClickedInventory().equals(player.getInventory())) return;
        
        // Obtenir le slot cliqué
        int slot = event.getSlot();
        
        // Obtenir les infos du menu
        KguiInventoryHolder holder = KguiInventoryHolder.getHolder(topInventory);
        if (holder == null) return;
        
        MenuData menuData = plugin.getMenuManager().getMenu(holder.getMenuId());
        if (menuData == null) return;
        
        // === D'abord vérifier si c'est un PaginationItem (item dynamique) ===
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR) {
            try {
                NBTItem nbtItem = new NBTItem(clickedItem);
                if (nbtItem.hasKey("kgui_pagination_actions")) {
                    String actionsStr = nbtItem.getString("kgui_pagination_actions");
                    if (actionsStr != null && !actionsStr.isEmpty()) {
                        playClickSound(player);
                        // Parser les actions (séparées par ||)
                        List<String> actions = new ArrayList<>(Arrays.asList(actionsStr.split("\\|\\|")));
                        plugin.getActionManager().executeActions(player, actions);
                        return;
                    }
                }
            } catch (Throwable e) {
                // Item non-NBT, erreur ou Error NBT-API, continuer avec la logique normale
            }
        }
        
        // === Logique normale pour les MenuItem statiques YAML ===
        // Trouver les items pour ce slot
        List<MenuItem> menuItems = menuData.getItemsForSlot(slot);
        if (menuItems.isEmpty()) return;
        
        // Prendre l'item avec la plus haute priorité qui passe les view requirements
        MenuItem menuItem = null;
        for (MenuItem item : menuItems) {
            if (plugin.getRequirementManager().checkRequirements(player, item.getViewRequirements(), false)) {
                menuItem = item;
                break;
            }
        }
        
        if (menuItem == null) return;
        
        // Jouer le son de clic
        playClickSound(player);
        
        // Vérifier le cooldown de l'item
        int itemCooldownTicks = menuItem.getCooldown();
        if (itemCooldownTicks > 0) {
            long cooldownMs = itemCooldownTicks * 50L; // ticks -> ms
            if (plugin.getGuiManager().isOnCooldown(player, holder.getMenuId(), menuItem.getKey(), cooldownMs)) {
                long remaining = plugin.getGuiManager().getCooldownRemaining(player, holder.getMenuId(), menuItem.getKey(), cooldownMs);
                plugin.getMessageManager().send(player, "item-cooldown", "time", String.format("%.1f", remaining / 1000.0));
                return;
            }
        }
        
        // Vérifier les click requirements
        if (!plugin.getRequirementManager().checkRequirements(player, menuItem.getClickRequirements(), true)) {
            // Exécuter les deny actions
            plugin.getActionManager().executeActions(player, menuItem.getDenyActions());
            return;
        }
        
        // Enregistrer le cooldown avant d'exécuter les actions
        if (itemCooldownTicks > 0) {
            plugin.getGuiManager().setItemCooldown(player, holder.getMenuId(), menuItem.getKey());
        }
        
        // Déterminer les actions à exécuter selon le type de clic
        List<String> actions = getActionsForClick(menuItem, event.getClick());
        
        // Exécuter les actions
        plugin.getActionManager().executeActions(player, actions);
    }

    /**
     * Obtient les actions pour un type de clic
     */
    private List<String> getActionsForClick(MenuItem menuItem, ClickType clickType) {
        List<String> actions;
        
        switch (clickType) {
            case LEFT:
                actions = menuItem.getLeftClickActions();
                break;
            case RIGHT:
                actions = menuItem.getRightClickActions();
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                actions = menuItem.getShiftClickActions();
                break;
            case MIDDLE:
                actions = menuItem.getMiddleClickActions();
                break;
            default:
                actions = menuItem.getClickActions();
        }
        
        // Si pas d'actions spécifiques, utiliser les actions générales
        if (actions.isEmpty()) {
            actions = menuItem.getClickActions();
        }
        
        return actions;
    }

    /**
     * Joue le son de clic
     */
    private void playClickSound(Player player) {
        ConfigManager.SoundData soundData = plugin.getConfigManager().getSoundData("sounds.button_click");
        if (soundData != null) {
            player.playSound(player.getLocation(), soundData.getSound(), soundData.getVolume(), soundData.getPitch());
        }
    }

    /**
     * Gère la fermeture de l'inventaire
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory topInventory = event.getView().getTopInventory();
        
        // Vérifier si c'est un inventaire Kgui
        if (!KguiInventoryHolder.isKguiInventory(topInventory)) return;
        
        KguiInventoryHolder closingHolder = KguiInventoryHolder.getHolder(topInventory);
        if (closingHolder == null) return;
        
        // Vérifier si le menu fermé est toujours le menu actuel
        // (évite de supprimer le nouveau menu quand on switch de menu via [open])
        OpenGui currentGui = plugin.getGuiManager().getOpenGui(player);
        if (currentGui != null && currentGui.getInventory() != topInventory) {
            // Un autre inventaire (autre menu ou nouveau titre/page) est deja actif.
            return;
        }
        
        // Nettoyer
        plugin.getGuiManager().closeMenu(player);
    }

    /**
     * Bloque le drag d'items
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Inventory topInventory = event.getView().getTopInventory();
        
        if (KguiInventoryHolder.isKguiInventory(topInventory)) {
            event.setCancelled(true);
        }
    }

    /**
     * Bloque le déplacement d'items entre inventaires
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (KguiInventoryHolder.isKguiInventory(event.getSource()) ||
            KguiInventoryHolder.isKguiInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }
}
