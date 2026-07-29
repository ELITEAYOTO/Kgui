package me.krunsh.kgui.gui;

import java.util.UUID;

import org.bukkit.inventory.Inventory;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;

/**
 * Représente un menu ouvert pour un joueur
 */
public class OpenGui {

    private final UUID playerUuid;
    private final String menuId;
    private int page;
    private int totalPages = 1;
    private Inventory inventory;  // Non-final pour permettre refresh avec nouveau titre
    private final long openTime;
    private long lastRefreshTime;
    
    // Pour la pagination/scroll
    private int scrollOffset = 0;

    public OpenGui(UUID playerUuid, String menuId, int page, Inventory inventory) {
        this.playerUuid = playerUuid;
        this.menuId = menuId;
        this.page = page;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public long getOpenTime() {
        return openTime;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    /**
     * Incrémente le scroll offset
     */
    public void scrollRight() {
        scrollOffset++;
    }

    /**
     * Décrémente le scroll offset
     */
    public void scrollLeft() {
        if (scrollOffset > 0) {
            scrollOffset--;
        }
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
    public int getCurrentPage() {
        return page;
    }

    /**
     * Obtient le nombre total de pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Définit le nombre total de pages
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * Obtient la position de scroll
     */
    public int getScrollPosition() {
        return scrollOffset;
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
