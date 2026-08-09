package me.krunsh.kgui.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.gui.KguiInventoryHolder;
import me.krunsh.kgui.session.CloseReason;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener de sécurité pour empêcher le vol d'items GUI
 */
public class SecurityListener implements Listener {

    private final Kgui plugin;
    private final String nbtTag;

    public SecurityListener(Kgui plugin) {
        this.plugin = plugin;
        this.nbtTag = plugin.getConfigManager().getNbtTag();
    }

    /**
     * Protection ABSOLUE - Cancel tous les clics dangereux
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClickLowest(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        
        if (!KguiInventoryHolder.isKguiInventory(topInventory)) return;
        
        // ANNULER TOUT
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);
        
        // Nettoyer le curseur si item GUI
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && isGuiItem(cursor)) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }
    }

    /**
     * Intercepte les drops d'items GUI
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item droppedItem = event.getItemDrop();
        if (droppedItem != null && isGuiItem(droppedItem.getItemStack())) {
            droppedItem.remove();
            event.setCancelled(true);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[Debug] GUI item dropped - REMOVED");
            }
        }
    }

    /**
     * Intercepte les clics sur items GUI en dehors des GUIs Kgui
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnyInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        
        // Ignorer les GUIs Kgui (déjà gérés)
        if (KguiInventoryHolder.isKguiInventory(topInventory)) return;
        
        // Vérifier l'item cliqué
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && isGuiItem(clickedItem)) {
            event.setCurrentItem(null);
            event.setCancelled(true);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[Debug] GUI item clicked outside GUI - REMOVED");
            }
        }
        
        // Vérifier le curseur
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && isGuiItem(cursorItem)) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
            event.setCancelled(true);
        }
        
        // Vérifier hotbar swap
        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && isGuiItem(hotbarItem)) {
                player.getInventory().setItem(event.getHotbarButton(), null);
                event.setCancelled(true);
            }
        }
    }

    /**
     * Nettoie l'inventaire à la déconnexion
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getConfigManager().isCleanOnQuit()) {
            cleanPlayerInventory(event.getPlayer());
        }
        
        // Fermer le menu si ouvert
        if (plugin.getGuiManager().hasOpenMenu(event.getPlayer())) {
            plugin.getGuiManager().closeMenu(event.getPlayer(), false, CloseReason.QUIT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (plugin.getConfigManager().isCleanOnQuit()) {
            cleanPlayerInventory(event.getPlayer());
        }
        plugin.getGuiManager().closeMenu(event.getPlayer(), false, CloseReason.KICK);
    }

    /**
     * Nettoie l'inventaire au changement de monde
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (plugin.getConfigManager().isCleanOnWorldChange()) {
            cleanPlayerInventory(event.getPlayer());
        }
        
        // Fermer le menu si ouvert
        if (plugin.getGuiManager().hasOpenMenu(event.getPlayer())) {
            plugin.getGuiManager().closeMenu(event.getPlayer(), false, CloseReason.WORLD_CHANGE);
            event.getPlayer().closeInventory();
        }
    }

    /**
     * Empêche le pickup d'items GUI
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (isGuiItem(event.getItem().getItemStack())) {
            event.getItem().remove();
            event.setCancelled(true);
        }
    }

    /**
     * Nettoie les items GUI du loot de mort
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isGuiItem);
        cleanPlayerInventory(event.getEntity());
    }

    /**
     * Bloque l'interaction avec des items GUI
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isGuiItem(item)) {
            event.setCancelled(true);
            // Supprimer l'item
            event.getPlayer().setItemInHand(null);
        }
    }

    /**
     * Vérifie si un item est un item GUI
     */
    private boolean isGuiItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            return nbtItem.hasKey(nbtTag);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Nettoie l'inventaire du joueur des items GUI
     */
    private void cleanPlayerInventory(Player player) {
        int removed = 0;
        
        // Curseur
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && isGuiItem(cursor)) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
            removed++;
        }
        
        // Inventaire principal
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isGuiItem(item)) {
                player.getInventory().setItem(i, null);
                removed++;
            }
        }
        
        // Armure
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isGuiItem(armor[i])) {
                armor[i] = null;
                armorChanged = true;
                removed++;
            }
        }
        if (armorChanged) {
            player.getInventory().setArmorContents(armor);
        }
        
        if (removed > 0) {
            player.updateInventory();
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[Debug] Cleaned " + removed + " GUI items from " + player.getName());
            }
        }
    }
}
