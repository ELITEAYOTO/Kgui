package me.krunsh.kgui.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;

/**
 * Gestionnaire de scroll pour les menus
 * Permet le défilement vertical ou horizontal du contenu
 */
public class ScrollManager {

    private final Kgui plugin;
    
    // Cache des données de scroll par joueur et menu
    private final Map<UUID, Map<String, ScrollData>> playerScrolls = new HashMap<>();

    public ScrollManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Type de scroll
     */
    public enum ScrollType {
        VERTICAL,   // Défilement haut/bas
        HORIZONTAL  // Défilement gauche/droite
    }

    /**
     * Données de scroll d'un joueur
     */
    public static class ScrollData {
        private int scrollOffset = 0;
        private int maxOffset = 0;
        private ScrollType scrollType = ScrollType.VERTICAL;
        private List<Integer> visibleSlots = new ArrayList<>();
        private int columnsPerRow = 9;
        private int totalRows = 0;
        private int visibleRows = 0;
        
        public int getScrollOffset() { return scrollOffset; }
        public void setScrollOffset(int offset) { this.scrollOffset = offset; }
        public int getMaxOffset() { return maxOffset; }
        public void setMaxOffset(int maxOffset) { this.maxOffset = maxOffset; }
        public ScrollType getScrollType() { return scrollType; }
        public void setScrollType(ScrollType type) { this.scrollType = type; }
        public List<Integer> getVisibleSlots() { return visibleSlots; }
        public void setVisibleSlots(List<Integer> slots) { this.visibleSlots = slots; }
        public int getColumnsPerRow() { return columnsPerRow; }
        public void setColumnsPerRow(int columns) { this.columnsPerRow = columns; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int rows) { this.totalRows = rows; }
        public int getVisibleRows() { return visibleRows; }
        public void setVisibleRows(int rows) { this.visibleRows = rows; }
    }

    /**
     * Initialise le scroll pour un menu
     */
    public void initializeForPlayer(Player player, MenuData menu, int totalContentItems) {
        initializeForPlayer(player, menu, totalContentItems, -1);
    }

    /**
     * Initialise le scroll pour un menu
     * @param forcedMaxOffset Si >= 0, force la valeur max d'offset (utile pour menus statiques)
     */
    public void initializeForPlayer(Player player, MenuData menu, int totalContentItems, int forcedMaxOffset) {
        initializeForPlayer(player, menu, totalContentItems, forcedMaxOffset, false);
    }

    /** Recalcule les bornes apres un refresh sans renvoyer le joueur au debut. */
    public void refreshForPlayer(Player player, MenuData menu, int totalContentItems, int forcedMaxOffset) {
        initializeForPlayer(player, menu, totalContentItems, forcedMaxOffset, true);
    }

    private void initializeForPlayer(Player player, MenuData menu, int totalContentItems,
                                     int forcedMaxOffset, boolean preserveOffset) {
        UUID uuid = player.getUniqueId();
        String menuId = menu.getId();
        
        playerScrolls.computeIfAbsent(uuid, k -> new HashMap<>());

        ScrollData previous = playerScrolls.get(uuid).get(menuId);
        int previousOffset = previous != null ? previous.getScrollOffset() : 0;
        
        ScrollData scrollData = new ScrollData();
        scrollData.setScrollOffset(0);
        scrollData.setVisibleSlots(menu.getContentSlots());
        scrollData.setColumnsPerRow(inferColumnsPerRow(menu.getContentSlots()));
        scrollData.setVisibleRows(inferVisibleRows(menu.getContentSlots()));
        
        // Calculer le scroll vertical (par défaut)
        if (!menu.getContentSlots().isEmpty()) {
            int columnsPerRow = Math.max(1, scrollData.getColumnsPerRow());
            int visibleRows = Math.max(1, scrollData.getVisibleRows());
            int totalRows = (int) Math.ceil((double) Math.max(0, totalContentItems) / columnsPerRow);
            
            scrollData.setVisibleRows(visibleRows);
            scrollData.setTotalRows(totalRows);
            if (forcedMaxOffset >= 0) {
                scrollData.setMaxOffset(Math.max(0, forcedMaxOffset));
            } else {
                scrollData.setMaxOffset(Math.max(0, totalRows - visibleRows));
            }
        }

        if (preserveOffset) {
            scrollData.setScrollOffset(Math.max(0, Math.min(previousOffset, scrollData.getMaxOffset())));
        }
        
        playerScrolls.get(uuid).put(menuId, scrollData);
    }

    /**
     * Détermine le nombre de colonnes par ligne à partir des slots visibles.
     * Ex: "10-16,19-25,28-34" -> 7 colonnes.
     */
    private int inferColumnsPerRow(List<Integer> visibleSlots) {
        if (visibleSlots == null || visibleSlots.isEmpty()) return 9;

        Map<Integer, Integer> rowCounts = new HashMap<>();
        for (int slot : visibleSlots) {
            int row = slot / 9;
            rowCounts.put(row, rowCounts.getOrDefault(row, 0) + 1);
        }

        int maxColumns = 0;
        for (int count : rowCounts.values()) {
            if (count > maxColumns) {
                maxColumns = count;
            }
        }

        return Math.max(1, maxColumns);
    }

    /**
     * Détermine le nombre de lignes visibles réelles à partir des slots.
     */
    private int inferVisibleRows(List<Integer> visibleSlots) {
        if (visibleSlots == null || visibleSlots.isEmpty()) return 0;

        Set<Integer> rows = new HashSet<>();
        for (int slot : visibleSlots) {
            rows.add(slot / 9);
        }

        return rows.size();
    }

    /**
     * Scroll vers le bas (ou la droite)
     */
    public boolean scrollDown(Player player, String menuId) {
        return scrollDown(player, menuId, 1);
    }

    /**
     * Scroll vers le bas avec un nombre de lignes spécifique
     */
    public boolean scrollDown(Player player, String menuId, int lines) {
        ScrollData scrollData = getScrollData(player, menuId);
        if (scrollData == null) return false;
        
        int newOffset = scrollData.getScrollOffset() + lines;
        if (newOffset <= scrollData.getMaxOffset()) {
            scrollData.setScrollOffset(newOffset);
            return true;
        }
        return false;
    }

    /**
     * Scroll vers le haut (ou la gauche)
     */
    public boolean scrollUp(Player player, String menuId) {
        return scrollUp(player, menuId, 1);
    }

    /**
     * Scroll vers le haut avec un nombre de lignes spécifique
     */
    public boolean scrollUp(Player player, String menuId, int lines) {
        ScrollData scrollData = getScrollData(player, menuId);
        if (scrollData == null) return false;
        
        int newOffset = scrollData.getScrollOffset() - lines;
        if (newOffset >= 0) {
            scrollData.setScrollOffset(newOffset);
            return true;
        } else if (scrollData.getScrollOffset() > 0) {
            // Aller au début
            scrollData.setScrollOffset(0);
            return true;
        }
        return false;
    }

    /**
     * Définit l'offset de scroll directement
     */
    public boolean setScrollOffset(Player player, String menuId, int offset) {
        ScrollData scrollData = getScrollData(player, menuId);
        if (scrollData == null) return false;
        
        if (offset >= 0 && offset <= scrollData.getMaxOffset()) {
            scrollData.setScrollOffset(offset);
            return true;
        }
        return false;
    }

    /**
     * Obtient l'offset de scroll actuel
     */
    public int getScrollOffset(Player player, String menuId) {
        ScrollData scrollData = getScrollData(player, menuId);
        return scrollData != null ? scrollData.getScrollOffset() : 0;
    }

    /**
     * Vérifie si on peut scroller vers le bas
     */
    public boolean canScrollDown(Player player, String menuId) {
        ScrollData scrollData = getScrollData(player, menuId);
        return scrollData != null && scrollData.getScrollOffset() < scrollData.getMaxOffset();
    }

    /**
     * Vérifie si on peut scroller vers le haut
     */
    public boolean canScrollUp(Player player, String menuId) {
        ScrollData scrollData = getScrollData(player, menuId);
        return scrollData != null && scrollData.getScrollOffset() > 0;
    }

    /**
     * Calcule les indices de contenu visible basé sur l'offset
     */
    public List<Integer> getVisibleContentIndices(Player player, String menuId) {
        ScrollData scrollData = getScrollData(player, menuId);
        if (scrollData == null) return Collections.emptyList();
        
        List<Integer> indices = new ArrayList<>();
        int startRow = scrollData.getScrollOffset();
        int columns = scrollData.getColumnsPerRow();
        
        for (int row = 0; row < scrollData.getVisibleRows(); row++) {
            for (int col = 0; col < columns; col++) {
                int contentIndex = (startRow + row) * columns + col;
                indices.add(contentIndex);
            }
        }
        
        return indices;
    }

    /**
     * Map un slot du menu vers l'index de contenu réel
     */
    public int getContentIndexForSlot(Player player, String menuId, int slotIndex) {
        ScrollData scrollData = getScrollData(player, menuId);
        if (scrollData == null) return slotIndex;
        
        // Pour le scroll vertical, on ajoute l'offset * colonnes
        int offset = scrollData.getScrollOffset();
        int columns = scrollData.getColumnsPerRow();
        
        // Calculer la ligne du slot dans la vue visible
        int slotRow = slotIndex / columns;
        int slotCol = slotIndex % columns;
        
        // Calculer l'index réel dans le contenu
        return (slotRow + offset) * columns + slotCol;
    }

    /**
     * Obtient les données de scroll
     */
    public ScrollData getScrollData(Player player, String menuId) {
        Map<String, ScrollData> menus = playerScrolls.get(player.getUniqueId());
        if (menus == null) return null;
        return menus.get(menuId);
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanupPlayer(UUID uuid) {
        playerScrolls.remove(uuid);
    }

    /**
     * Nettoie les données d'un menu pour un joueur
     */
    public void cleanupMenu(UUID uuid, String menuId) {
        Map<String, ScrollData> menus = playerScrolls.get(uuid);
        if (menus != null) {
            menus.remove(menuId);
        }
    }

    /**
     * Remplace les placeholders de scroll
     */
    public String replacePlaceholders(String text, Player player, String menuId) {
        if (text == null) return null;
        
        ScrollData scrollData = getScrollData(player, menuId);
        if (scrollData == null) return text;
        
        text = text.replace("%scroll_offset%", String.valueOf(scrollData.getScrollOffset()));
        text = text.replace("%scroll_max%", String.valueOf(scrollData.getMaxOffset()));
        text = text.replace("%kgui_scroll%", String.valueOf(scrollData.getScrollOffset()));
        text = text.replace("%kgui_scroll_max%", String.valueOf(scrollData.getMaxOffset()));
        
        return text;
    }
}
