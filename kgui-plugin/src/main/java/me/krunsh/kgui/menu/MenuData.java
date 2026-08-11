package me.krunsh.kgui.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.refresh.RefreshPolicy;

/**
 * Données d'un menu
 */
public class MenuData {

    private final String id;
    private final Kgui plugin;
    
    // Propriétés de base
    private String title = "&8Menu";
    private int size = 54;
    private MenuType menuType = MenuType.NORMAL;
    
    // Commandes
    private String openCommand;
    private List<String> openCommands = new ArrayList<>();
    
    // Actions
    private List<String> openActions = new ArrayList<>();
    private List<String> closeActions = new ArrayList<>();
    
    // Requirements
    private List<Map<String, Object>> openRequirements = new ArrayList<>();
    private String permission;
    
    // Combat/Monde
    private boolean blockInCombat = true;
    private List<String> allowedWorlds = new ArrayList<>();
    private List<String> blockedWorlds = new ArrayList<>();
    
    // WorldGuard
    private List<String> openOnRegionEnter = new ArrayList<>();
    private List<String> allowedRegions = new ArrayList<>();
    private List<String> blockedRegions = new ArrayList<>();
    
    // Cooldown et update
    private int cooldown = 0;
    private int updateInterval = 0;
    private RefreshPolicy refreshPolicy = RefreshPolicy.EVENT;
    
    // Pagination
    private List<Integer> contentSlots = new ArrayList<>();
    private int prevButtonSlot = -1;
    private int nextButtonSlot = -1;
    private int staticMaxPages = 0; // 0 = auto, sinon valeur fixe
    
    // Content Provider (API for external plugins)
    private String contentProvider;  // Provider ID namespacé (e.g., "kfaction:logs")
    private Map<String, String> providerArgs = new HashMap<>();  // Args to pass to provider
    private String emptyMessage;  // Message when no content
    private ConfigurationSection emptyItemConfig;  // Item to show when empty
    
    // Items
    private final Map<String, MenuItem> items = new HashMap<>();
    
    // Animations
    private ConfigurationSection animationConfig;
    
    // Configuration source
    private ConfigurationSection sourceConfig;
    
    // Template
    private String templateId;
    
    // Scroll
    private boolean scrollable = false;

    public MenuData(String id, Kgui plugin) {
        this.id = id;
        this.plugin = plugin;
    }

    /**
     * Hérite les propriétés d'un template
     */
    public void inheritFrom(MenuData template) {
        // Copier les items du template
        for (MenuItem item : template.getItems().values()) {
            items.put(item.getKey(), item.clone());
        }
        
        // Copier les propriétés par défaut si non définies
        if (this.size == 54 && template.size != 54) {
            this.size = template.size;
        }
    }

    /**
     * Ajoute un item au menu
     */
    public void addItem(MenuItem item) {
        items.put(item.getKey(), item);
    }

    /**
     * Obtient un item par sa clé
     */
    public MenuItem getItem(String key) {
        return items.get(key);
    }

    /**
     * Obtient tous les items
     */
    public Map<String, MenuItem> getItems() {
        return items;
    }

    /**
     * Obtient les items pour un slot donné
     */
    public List<MenuItem> getItemsForSlot(int slot) {
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : items.values()) {
            if (item.getSlots().contains(slot)) {
                result.add(item);
            }
        }
        // Trier par priorité
        result.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return result;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        // Valider que c'est un multiple de 9
        if (size % 9 == 0 && size >= 9 && size <= 54) {
            this.size = size;
        }
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public void setMenuType(MenuType menuType) {
        this.menuType = menuType;
        this.scrollable = (menuType == MenuType.SCROLL);
    }

    public String getOpenCommand() {
        return openCommand;
    }

    public void setOpenCommand(String openCommand) {
        this.openCommand = openCommand;
    }

    public List<String> getOpenCommands() {
        return openCommands;
    }

    public void setOpenCommands(List<String> openCommands) {
        this.openCommands = openCommands;
    }

    public List<String> getOpenActions() {
        return openActions;
    }

    public void setOpenActions(List<String> openActions) {
        this.openActions = openActions;
    }

    public List<String> getCloseActions() {
        return closeActions;
    }

    public void setCloseActions(List<String> closeActions) {
        this.closeActions = closeActions;
    }

    public List<Map<String, Object>> getOpenRequirements() {
        return openRequirements;
    }

    public void setOpenRequirements(List<Map<String, Object>> openRequirements) {
        this.openRequirements = openRequirements;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isBlockInCombat() {
        return blockInCombat;
    }

    public void setBlockInCombat(boolean blockInCombat) {
        this.blockInCombat = blockInCombat;
    }

    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    public void setAllowedWorlds(List<String> allowedWorlds) {
        this.allowedWorlds = allowedWorlds;
    }

    public List<String> getBlockedWorlds() {
        return blockedWorlds;
    }

    public void setBlockedWorlds(List<String> blockedWorlds) {
        this.blockedWorlds = blockedWorlds;
    }

    public List<String> getOpenOnRegionEnter() {
        return openOnRegionEnter;
    }

    public void setOpenOnRegionEnter(List<String> openOnRegionEnter) {
        this.openOnRegionEnter = openOnRegionEnter;
    }

    public List<String> getAllowedRegions() {
        return allowedRegions;
    }

    public void setAllowedRegions(List<String> allowedRegions) {
        this.allowedRegions = allowedRegions;
    }

    public List<String> getBlockedRegions() {
        return blockedRegions;
    }

    public void setBlockedRegions(List<String> blockedRegions) {
        this.blockedRegions = blockedRegions;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public RefreshPolicy getRefreshPolicy() { return refreshPolicy; }
    public void setRefreshPolicy(RefreshPolicy refreshPolicy) {
        this.refreshPolicy = refreshPolicy == null ? RefreshPolicy.EVENT : refreshPolicy;
    }

    public int getScheduledRefreshInterval() {
        return refreshPolicy.hasInterval() ? Math.max(0, updateInterval) : 0;
    }

    public List<Integer> getContentSlots() {
        return contentSlots;
    }

    public void setContentSlots(List<Integer> contentSlots) {
        this.contentSlots = contentSlots;
    }

    public int getPrevButtonSlot() {
        return prevButtonSlot;
    }

    public void setPrevButtonSlot(int prevButtonSlot) {
        this.prevButtonSlot = prevButtonSlot;
    }

    public int getNextButtonSlot() {
        return nextButtonSlot;
    }

    public void setNextButtonSlot(int nextButtonSlot) {
        this.nextButtonSlot = nextButtonSlot;
    }

    public int getStaticMaxPages() {
        return staticMaxPages;
    }

    public void setStaticMaxPages(int staticMaxPages) {
        this.staticMaxPages = staticMaxPages;
    }

    public ConfigurationSection getAnimationConfig() {
        return animationConfig;
    }

    public void setAnimationConfig(ConfigurationSection animationConfig) {
        this.animationConfig = animationConfig;
    }

    public ConfigurationSection getConfig() {
        return sourceConfig;
    }

    public void setConfig(ConfigurationSection config) {
        this.sourceConfig = config;
    }

    public boolean isPaginated() {
        return !contentSlots.isEmpty();
    }

    public List<Integer> getPaginationSlots() {
        return contentSlots;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public String getTemplate() {
        return templateId;
    }

    public void setTemplate(String templateId) {
        this.templateId = templateId;
    }

    public MenuType getType() {
        return menuType;
    }

    // ==================== CONTENT PROVIDER ====================

    /**
     * Get the content provider ID for this paginated menu.
     * @return The provider ID or null if not using a provider
     */
    public String getContentProvider() {
        return contentProvider;
    }

    public void setContentProvider(String contentProvider) {
        this.contentProvider = contentProvider;
    }

    /**
     * Check if this menu uses a content provider.
     * @return true if a provider is configured
     */
    public boolean hasContentProvider() {
        return contentProvider != null && !contentProvider.isEmpty();
    }

    /**
     * Get the arguments to pass to the content provider.
     * @return Map of argument key-value pairs
     */
    public Map<String, String> getProviderArgs() {
        return providerArgs;
    }

    public void setProviderArgs(Map<String, String> providerArgs) {
        this.providerArgs = providerArgs;
    }

    /**
     * Get the message to display when content is empty.
     * @return The empty message or null
     */
    public String getEmptyMessage() {
        return emptyMessage;
    }

    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
    }

    /**
     * Get the item configuration to show when content is empty.
     * @return ConfigurationSection for empty item or null
     */
    public ConfigurationSection getEmptyItemConfig() {
        return emptyItemConfig;
    }

    public void setEmptyItemConfig(ConfigurationSection emptyItemConfig) {
        this.emptyItemConfig = emptyItemConfig;
    }
}
