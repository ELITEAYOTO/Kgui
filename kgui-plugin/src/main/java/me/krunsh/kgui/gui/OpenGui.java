package me.krunsh.kgui.gui;

import java.util.UUID;

import org.bukkit.inventory.Inventory;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;

/**
 * Représente un menu ouvert pour un joueur
 */
public abstract class OpenGui {

    private final UUID playerUuid;
    private final String menuId;
    private Inventory inventory;  // Non-final pour permettre refresh avec nouveau titre
    private final long openTime;
    private long lastRefreshTime;
    
    public OpenGui(UUID playerUuid, String menuId, int page, Inventory inventory) {
        this.playerUuid = playerUuid;
        this.menuId = menuId;
        this.inventory = inventory;
        this.openTime = System.currentTimeMillis();
        this.lastRefreshTime = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getMenuId() {
        return menuId;
    }

    public abstract int getPage();

    public abstract void setPage(int page);

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public long getOpenTime() {
        return openTime;
    }

    public abstract int getScrollOffset();

    public abstract void setScrollOffset(int scrollOffset);

    /**
     * Incrémente le scroll offset
     */
    public void scrollRight() {
        setScrollOffset(getScrollOffset() + 1);
    }

    /**
     * Décrémente le scroll offset
     */
    public void scrollLeft() {
        if (getScrollOffset() > 0) setScrollOffset(getScrollOffset() - 1);
    }

    /**
     * Obtient le MenuData associé
     */
    public MenuData getMenuData() {
        return Kgui.getInstance().getMenuManager().getMenu(menuId);
    }

    /**
     * Obtient la page courante (0-indexed)
     */
    public int getCurrentPage() { return getPage(); }

    /**
     * Obtient le nombre total de pages
     */
    public abstract int getTotalPages();

    /**
     * Définit le nombre total de pages
     */
    public abstract void setTotalPages(int totalPages);

    /**
     * Obtient la position de scroll
     */
    public int getScrollPosition() {
        return getScrollOffset();
    }
    
    /**
     * Obtient le timestamp du dernier refresh
     */
    public long getLastRefreshTime() {
        return lastRefreshTime;
    }
    
    /**
     * Définit le timestamp du dernier refresh
     */
    public void setLastRefreshTime(long lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
    }
}
