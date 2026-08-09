package me.krunsh.kgui.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * InventoryHolder personnalisé pour identifier les inventaires de Kgui
 * Permet de sécuriser les GUIs et de tracker les menus ouverts
 */
public class KguiInventoryHolder implements InventoryHolder {

    private final String menuId;
    private int page;
    private final UUID playerUuid;
    private Inventory inventory;

    public KguiInventoryHolder(String menuId, int page, UUID playerUuid) {
        this.menuId = menuId;
        this.page = page;
        this.playerUuid = playerUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getMenuId() {
        return menuId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Vérifie si un inventaire est un inventaire Kgui
     */
    public static boolean isKguiInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof KguiInventoryHolder;
    }

    /**
     * Obtient le holder Kgui d'un inventaire
     */
    public static KguiInventoryHolder getHolder(Inventory inventory) {
        if (isKguiInventory(inventory)) {
            return (KguiInventoryHolder) inventory.getHolder();
        }
        return null;
    }
}
