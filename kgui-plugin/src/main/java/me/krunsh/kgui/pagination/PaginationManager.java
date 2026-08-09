package me.krunsh.kgui.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;

/**
 * Gestionnaire de pagination pour les menus dynamiques
 * Gère les contenus paginés avec navigation prev/next
 */
public class PaginationManager {

    private final Kgui plugin;
    
    // Cache des pages par joueur et menu
    // UUID -> MenuId -> PageData
    private final Map<UUID, Map<String, PageData>> playerPages = new HashMap<>();
    
    public PaginationManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialise les données de pagination de base pour un joueur
     * Appelé lors de l'ouverture d'un menu paginé
     */
    public void initPlayer(Player player, String menuId, int page) {
        initPlayer(player, menuId, page, 0);
    }
    
    /**
     * Initialise les données de pagination avec un nombre de pages défini
     * @param staticMaxPages Si > 0, utilise cette valeur comme max_page (pour items statiques conditionnels)
     */
    public void initPlayer(Player player, String menuId, int page, int staticMaxPages) {
        UUID uuid = player.getUniqueId();
        
        playerPages.computeIfAbsent(uuid, k -> new HashMap<>());
        
        PageData pageData = playerPages.get(uuid).get(menuId);
        if (pageData == null) {
            pageData = new PageData();
            pageData.setCurrentPage(Math.max(1, page));
            // Utilise staticMaxPages si défini (> 0), sinon 1 par défaut
            pageData.setMaxPage(staticMaxPages > 0 ? staticMaxPages : 1);
            playerPages.get(uuid).put(menuId, pageData);
        } else {
            // Mettre à jour maxPage si staticMaxPages est spécifié
            if (staticMaxPages > 0) {
                pageData.setMaxPage(staticMaxPages);
            }
            // Mettre à jour la page si spécifiée
            if (page > 0) {
                pageData.setCurrentPage(Math.min(page, pageData.getMaxPage()));
            }
        }
    }

    /**
     * Données de pagination d'un joueur pour un menu
     */
    public static class PageData {
        private int currentPage = 1;
        private int maxPage = 1;
        private List<PaginationItem> items = new ArrayList<>();
        
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int page) { this.currentPage = page; }
        public int getMaxPage() { return maxPage; }
        public void setMaxPage(int maxPage) { this.maxPage = maxPage; }
        public List<PaginationItem> getItems() { return items; }
        public void setItems(List<PaginationItem> items) { this.items = items; }
    }

    /**
     * Item de pagination (item dynamique à afficher)
     */
    public static class PaginationItem {
        private final String material;
        private final short data;
        private final String name;
        private final List<String> lore;
        private final boolean glow;
        private final Map<String, String> placeholders;
        private final List<String> clickActions;
        private final List<String> leftClickActions;
        private final List<String> rightClickActions;
        private final List<String> shiftClickActions;
        private final String bindingId;
        private final String skullOwner;
        private final String headDatabaseId;
        
        public PaginationItem(String material, short data, String name, List<String> lore, 
                              boolean glow, Map<String, String> placeholders, List<String> clickActions) {
            this(material, data, name, lore, glow, placeholders, clickActions, null, null);
        }
        
        public PaginationItem(String material, short data, String name, List<String> lore,
                              boolean glow, Map<String, String> placeholders, List<String> clickActions,
                              String skullOwner, String headDatabaseId) {
            this(material, data, name, lore, glow, placeholders, clickActions,
                Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), "legacy", skullOwner, headDatabaseId);
        }

        public PaginationItem(String material, short data, String name, List<String> lore,
                              boolean glow, Map<String, String> placeholders, List<String> clickActions,
                              List<String> leftClickActions, List<String> rightClickActions,
                              List<String> shiftClickActions, String bindingId,
                              String skullOwner, String headDatabaseId) {
            this.material = material;
            this.data = data;
            this.name = name;
            this.lore = lore == null ? Collections.<String>emptyList() : new ArrayList<>(lore);
            this.glow = glow;
            this.placeholders = placeholders == null ? Collections.<String, String>emptyMap() : new HashMap<>(placeholders);
            this.clickActions = clickActions == null ? Collections.<String>emptyList() : new ArrayList<>(clickActions);
            this.leftClickActions = leftClickActions == null ? Collections.<String>emptyList() : new ArrayList<>(leftClickActions);
            this.rightClickActions = rightClickActions == null ? Collections.<String>emptyList() : new ArrayList<>(rightClickActions);
            this.shiftClickActions = shiftClickActions == null ? Collections.<String>emptyList() : new ArrayList<>(shiftClickActions);
            this.bindingId = bindingId == null ? "dynamic" : bindingId;
            this.skullOwner = skullOwner;
            this.headDatabaseId = headDatabaseId;
        }
        
        public String getMaterial() { return material; }
        public short getData() { return data; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public boolean isGlow() { return glow; }
        public Map<String, String> getPlaceholders() { return placeholders; }
        public List<String> getClickActions() { return clickActions; }
        public List<String> getLeftClickActions() { return leftClickActions; }
        public List<String> getRightClickActions() { return rightClickActions; }
        public List<String> getShiftClickActions() { return shiftClickActions; }
        public String getBindingId() { return bindingId; }
        public String getSkullOwner() { return skullOwner; }
        public String getHeadDatabaseId() { return headDatabaseId; }
    }

    /**
     * Initialise la pagination pour un joueur et menu
     */
    public void initializeForPlayer(Player player, MenuData menu, List<PaginationItem> items) {
        UUID uuid = player.getUniqueId();
        String menuId = menu.getId();
        
        playerPages.computeIfAbsent(uuid, k -> new HashMap<>());
        
        PageData pageData = playerPages.get(uuid).get(menuId);
        if (pageData == null) {
            pageData = new PageData();
        }
        int previousPage = pageData.getCurrentPage();
        pageData.setItems(items);
        
        int contentSlots = menu.getContentSlots().size();
        int maxPage = PaginationState.maxPage(items.size(), contentSlots);
        pageData.setMaxPage(maxPage);
        // Un refresh conserve la page demandee. Si le contenu a diminue, elle est
        // seulement bornee a la derniere page encore disponible.
        pageData.setCurrentPage(PaginationState.clampPage(previousPage, maxPage));
        
        playerPages.get(uuid).put(menuId, pageData);
    }

    /**
     * Obtient les items pour la page actuelle
     */
    public List<PaginationItem> getItemsForCurrentPage(Player player, MenuData menu) {
        PageData pageData = getPageData(player, menu.getId());
        if (pageData == null) return Collections.emptyList();
        
        List<Integer> contentSlots = menu.getContentSlots();
        if (contentSlots.isEmpty()) return Collections.emptyList();
        
        int perPage = contentSlots.size();
        int startIndex = (pageData.getCurrentPage() - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, pageData.getItems().size());
        
        if (startIndex >= pageData.getItems().size()) {
            return Collections.emptyList();
        }
        
        return pageData.getItems().subList(startIndex, endIndex);
    }

    /**
     * Retourne le slot associé à un item de pagination
     */
    public Map<Integer, PaginationItem> mapItemsToSlots(Player player, MenuData menu) {
        List<PaginationItem> items = getItemsForCurrentPage(player, menu);
        List<Integer> contentSlots = menu.getContentSlots();
        
        Map<Integer, PaginationItem> result = new HashMap<>();
        for (int i = 0; i < items.size() && i < contentSlots.size(); i++) {
            result.put(contentSlots.get(i), items.get(i));
        }
        
        return result;
    }

    /**
     * Page suivante
     */
    public boolean nextPage(Player player, String menuId) {
        PageData pageData = getPageData(player, menuId);
        if (pageData == null) return false;
        
        if (pageData.getCurrentPage() < pageData.getMaxPage()) {
            pageData.setCurrentPage(pageData.getCurrentPage() + 1);
            return true;
        }
        return false;
    }

    /**
     * Page précédente
     */
    public boolean previousPage(Player player, String menuId) {
        PageData pageData = getPageData(player, menuId);
        if (pageData == null) return false;
        
        if (pageData.getCurrentPage() > 1) {
            pageData.setCurrentPage(pageData.getCurrentPage() - 1);
            return true;
        }
        return false;
    }

    /**
     * Aller à une page spécifique
     */
    public boolean setPage(Player player, String menuId, int page) {
        PageData pageData = getPageData(player, menuId);
        if (pageData == null) return false;
        
        if (page >= 1 && page <= pageData.getMaxPage()) {
            pageData.setCurrentPage(page);
            return true;
        }
        return false;
    }

    /**
     * Obtient la page actuelle
     */
    public int getCurrentPage(Player player, String menuId) {
        PageData pageData = getPageData(player, menuId);
        return pageData != null ? pageData.getCurrentPage() : 1;
    }

    /**
     * Obtient le nombre max de pages
     */
    public int getMaxPage(Player player, String menuId) {
        PageData pageData = getPageData(player, menuId);
        return pageData != null ? pageData.getMaxPage() : 1;
    }

    /**
     * Vérifie si on peut aller à la page suivante
     */
    public boolean hasNextPage(Player player, String menuId) {
        PageData pageData = getPageData(player, menuId);
        return pageData != null && pageData.getCurrentPage() < pageData.getMaxPage();
    }

    /**
     * Vérifie si on peut aller à la page précédente
     */
    public boolean hasPreviousPage(Player player, String menuId) {
        PageData pageData = getPageData(player, menuId);
        return pageData != null && pageData.getCurrentPage() > 1;
    }

    /**
     * Obtient les données de page d'un joueur
     */
    public PageData getPageData(Player player, String menuId) {
        Map<String, PageData> menus = playerPages.get(player.getUniqueId());
        if (menus == null) return null;
        return menus.get(menuId);
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanupPlayer(UUID uuid) {
        playerPages.remove(uuid);
    }

    /**
     * Nettoie les données d'un menu pour un joueur
     */
    public void cleanupMenu(UUID uuid, String menuId) {
        Map<String, PageData> menus = playerPages.get(uuid);
        if (menus != null) {
            menus.remove(menuId);
        }
    }

    /**
     * Remplace les placeholders de pagination dans un texte
     */
    public String replacePlaceholders(String text, Player player, String menuId) {
        if (text == null) return null;
        
        int currentPage = getCurrentPage(player, menuId);
        int maxPage = getMaxPage(player, menuId);
        
        text = text.replace("%page%", String.valueOf(currentPage));
        text = text.replace("%max_page%", String.valueOf(maxPage));
        text = text.replace("%total_pages%", String.valueOf(maxPage));
        text = text.replace("%kgui_page%", String.valueOf(currentPage));
        text = text.replace("%kgui_max_page%", String.valueOf(maxPage));
        
        return text;
    }

    /**
     * Actualise le contenu paginé avec de nouveaux items
     */
    public void updateContent(Player player, String menuId, List<PaginationItem> items) {
        PageData pageData = getPageData(player, menuId);
        if (pageData == null) return;
        
        pageData.setItems(items);
        
        // Recalculer max pages (on ne connait pas les slots ici)
        // Cette méthode est appelée quand on connait déjà les slots
    }
}
