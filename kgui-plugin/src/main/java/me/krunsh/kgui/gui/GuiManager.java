package me.krunsh.kgui.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.DynamicItem;
import me.krunsh.kgui.config.ConfigManager;
import me.krunsh.kgui.menu.MenuData;
import me.krunsh.kgui.menu.MenuItem;
import me.krunsh.kgui.pagination.PaginationManager.PaginationItem;
import me.krunsh.kgui.utils.ColorUtils;
import me.krunsh.kgui.utils.ItemBuilder;

/**
 * Gestionnaire des GUIs ouverts
 * Gère l'ouverture, fermeture et tracking des menus
 */
public class GuiManager {

    private final Kgui plugin;
    
    // Menus ouverts par joueur
    private final Map<UUID, OpenGui> openGuis = new HashMap<>();
    
    // Historique des menus (pile pour navigation multi-niveaux avec bouton retour)
    private final Map<UUID, Deque<String>> menuHistory = new HashMap<>();
    
    // Joueurs en cours de refresh (pour éviter que closeMenu supprime le joueur)
    private final java.util.Set<UUID> refreshingPlayers = new HashSet<>();
    
    // Cache des placeholders par joueur
    private final Map<UUID, Map<String, String>> placeholderCache = new HashMap<>();
    private final Map<UUID, Long> placeholderCacheTime = new HashMap<>();
    
    // Cooldowns: UUID -> (itemKey -> lastClickTime)
    private final Map<UUID, Map<String, Long>> itemCooldowns = new HashMap<>();
    
    // Args runtime conserves par joueur ET par menu pendant toute la session GUI.
    // Ils doivent rester disponibles lors des refreshs et lors d'un retour arriere.
    private final Map<UUID, Map<String, Map<String, String>>> runtimeArgs = new HashMap<>();

    // Regroupe toutes les demandes de refresh d'un meme tick.
    private final RefreshRequestGate refreshGate = new RefreshRequestGate();

    // Un seul changement de page est accepte par joueur et par tick.
    private final java.util.Set<UUID> navigationLocks = new HashSet<>();
    
    // Auto-refresh task ID
    private int autoRefreshTaskId = -1;
    
    // Flag pour éviter d'ajouter à l'historique (utilisé par [back])
    private final java.util.Set<UUID> skipHistoryPlayers = new HashSet<>();

    public GuiManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Ouvre un menu pour un joueur
     */
    public boolean openMenu(Player player, String menuId) {
        return openMenu(player, menuId, 0);
    }

    /**
     * Ouvre un menu avec des args runtime passés au ContentProvider.
     * Ces args sont fusionnés avec les args statiques du YAML lors de l'appel à getContent().
     * Exemple : kgui.getGuiManager().openMenu(player, "kjobs_detail", Map.of("job_id","mineur"))
     */
    public boolean openMenu(Player player, String menuId, Map<String, String> args) {
        return openMenu(player, menuId, 0, args);
    }

    /**
     * Ouvre une page precise avec des arguments runtime. Ce point d'entree sert
     * au contrat Kgui 2 et evite de muter l'etat par une commande joueur.
     */
    public boolean openMenu(Player player, String menuId, int page, Map<String, String> args) {
        if (args != null && !args.isEmpty()) {
            runtimeArgs.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(menuId, new HashMap<>(args));
        }
        return openMenu(player, menuId, page);
    }

    /**
     * Ouvre un menu en mode "retour" (sans ajouter à l'historique)
     * Utilisé par l'action [back] pour éviter une boucle infinie
     */
    public boolean openMenuBack(Player player, String menuId) {
        skipHistoryPlayers.add(player.getUniqueId());
        return openMenu(player, menuId, 0);
    }

    /**
     * Ouvre un menu pour un joueur à une page spécifique
     */
    public boolean openMenu(Player player, String menuId, int page) {
        MenuData menuData = plugin.getMenuManager().getMenu(menuId);
        if (menuData == null) {
            plugin.getMessageManager().send(player, "menu-not-found", "menu", menuId);
            return false;
        }
        
        // Vérifier les requirements globaux
        if (!checkOpenRequirements(player, menuData)) {
            return false;
        }
        
        // Vérifier le blocage en combat
        if (menuData.isBlockInCombat() && plugin.getHookManager().isCombatTagEnabled()) {
            if (plugin.getHookManager().getCombatTagHook().isInCombat(player)) {
                if (!player.hasPermission("kgui.bypass.combat")) {
                    plugin.getMessageManager().send(player, "blocked-in-combat");
                    return false;
                }
            }
        }
        
        // Vérifier le monde
        if (!isWorldAllowed(player, menuData)) {
            plugin.getMessageManager().send(player, "not-allowed-world");
            return false;
        }
        
        // Initialiser pagination/scroll AVANT le parsing du titre pour que %page% fonctionne.
        switch (menuData.getMenuType()) {
            case PAGINATION:
                initializePaginationMenu(player, menuId, page, menuData);
                break;
            case SCROLL:
                initializeScrollMenu(player, menuId, page, menuData);
                break;
            default:
                break;
        }

        int currentPageForView = (menuData.getMenuType() == me.krunsh.kgui.menu.MenuType.PAGINATION
                || menuData.getMenuType() == me.krunsh.kgui.menu.MenuType.SCROLL)
                ? plugin.getPaginationManager().getCurrentPage(player, menuId)
                : page;
        
        // Créer l'inventaire - parser le titre avec le menuId explicite
        String title = parsePlaceholders(player, menuData.getTitle(), menuId);
        title = ColorUtils.colorize(title);
        
        // Limiter le titre à 32 caractères (limite Minecraft 1.8)
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        
        KguiInventoryHolder holder = new KguiInventoryHolder(menuId, currentPageForView, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(
            holder,
            menuData.getSize(),
            title
        );
        holder.setInventory(inventory);
        
        // Enregistrer la GUI AVANT de remplir l'inventaire (pour que getPlayerCurrentMenu fonctionne)
        OpenGui openGui = new OpenGui(player.getUniqueId(), menuId, currentPageForView, inventory);
        openGui.setTotalPages(plugin.getPaginationManager().getMaxPage(player, menuId));
        openGui.setScrollOffset(plugin.getScrollManager().getScrollOffset(player, menuId));
        
        // Sauvegarder le menu précédent dans la pile (navigation multi-niveaux)
        OpenGui previousGui = openGuis.get(player.getUniqueId());
        if (previousGui != null && !previousGui.getMenuId().equals(menuId)) {
            if (!skipHistoryPlayers.remove(player.getUniqueId())) {
                menuHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(previousGui.getMenuId());
            }
        }
        
        openGuis.put(player.getUniqueId(), openGui);
        
        // Remplir l'inventaire (maintenant getPlayerCurrentMenu fonctionne pour les requirements)
        fillInventory(player, inventory, menuData, currentPageForView);
        
        // Ouvrir l'inventaire
        player.openInventory(inventory);
        
        // Jouer le son d'ouverture
        playOpenSound(player);
        
        // Exécuter les actions d'ouverture
        executeOpenActions(player, menuData);
        
        return true;
    }

    /**
     * Vérifie les requirements d'ouverture
     */
    private boolean checkOpenRequirements(Player player, MenuData menuData) {
        // Permission simple
        if (menuData.getPermission() != null && !player.hasPermission(menuData.getPermission())) {
            plugin.getMessageManager().send(player, "no-permission");
            return false;
        }
        
        // Requirements avancés
        return plugin.getRequirementManager().checkRequirements(
            player, 
            menuData.getOpenRequirements(), 
            true
        );
    }

    /**
     * Vérifie si le monde est autorisé
     */
    private boolean isWorldAllowed(Player player, MenuData menuData) {
        if (player.hasPermission("kgui.bypass.world")) {
            return true;
        }
        
        String worldName = player.getWorld().getName();
        
        // Allowed worlds (whitelist)
        if (!menuData.getAllowedWorlds().isEmpty()) {
            return menuData.getAllowedWorlds().contains(worldName);
        }
        
        // Blocked worlds (blacklist)
        if (!menuData.getBlockedWorlds().isEmpty()) {
            return !menuData.getBlockedWorlds().contains(worldName);
        }
        
        // Par défaut, utiliser la config globale
        return plugin.getConfigManager().isWorldAllowed(worldName);
    }

    /**
     * Remplit l'inventaire avec les items du menu
     * Les items sont triés par priorité (plus haute = affiché en priorité sur le même slot)
     * Supporte les menus paginés avec PaginationItems dynamiques
     */
    private void fillInventory(Player player, Inventory inventory, MenuData menuData, int page) {
        // Map pour tracker la priorité des items déjà placés sur chaque slot
        Map<Integer, Integer> slotPriorities = new HashMap<>();
        
        // Trier les items par priorité croissante (les plus hauts seront traités en dernier et écraseront)
        List<MenuItem> sortedItems = new ArrayList<>(menuData.getItems().values());
        sortedItems.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        
        for (MenuItem menuItem : sortedItems) {
            // Vérifier les view requirements
            if (!plugin.getRequirementManager().checkRequirements(player, menuItem.getViewRequirements(), false)) {
                continue;
            }
            
            // Construire l'item
            ItemStack item = buildMenuItem(player, menuItem);
            if (item == null) continue;
            
            // Ajouter le tag NBT de protection
            item = tagAsGuiItem(item, menuData.getId());
            
            // Placer l'item dans les slots (en respectant les priorités)
            for (int slot : menuItem.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    int existingPriority = slotPriorities.getOrDefault(slot, Integer.MIN_VALUE);
                    // Placer si priorité >= à l'existante
                    if (menuItem.getPriority() >= existingPriority) {
                        inventory.setItem(slot, item);
                        slotPriorities.put(slot, menuItem.getPriority());
                    }
                }
            }
        }
        
        // === CONTENU DYNAMIQUE (pagination/scroll) ===
        // Ces items sont injectés par des plugins externes (ex: Kfaction pour les logs).
        List<Integer> contentSlots = menuData.getContentSlots();
        if (!contentSlots.isEmpty()) {
            if (menuData.getMenuType() == me.krunsh.kgui.menu.MenuType.SCROLL) {
                renderScrollDynamicContent(player, inventory, menuData, slotPriorities);
            } else {
                Map<Integer, me.krunsh.kgui.pagination.PaginationManager.PaginationItem> paginatedItems = 
                    plugin.getPaginationManager().mapItemsToSlots(player, menuData);
                
                for (Map.Entry<Integer, me.krunsh.kgui.pagination.PaginationManager.PaginationItem> entry : paginatedItems.entrySet()) {
                    int slot = entry.getKey();
                    me.krunsh.kgui.pagination.PaginationManager.PaginationItem pagItem = entry.getValue();
                    
                    if (slot >= 0 && slot < inventory.getSize()) {
                        // Construire l'item depuis le PaginationItem
                        ItemStack item = buildPaginationItem(player, pagItem, menuData.getId());
                        if (item != null) {
                            inventory.setItem(slot, item);
                            // Les items paginés ont une priorité maximale
                            slotPriorities.put(slot, Integer.MAX_VALUE);
                        }
                    }
                }
            }
        }
    }

    /**
     * Charge les items dynamiques d'un provider et les convertit en PaginationItem.
     */
    private List<PaginationItem> loadProviderItems(Player player, String menuId, MenuData menuData) {
        List<PaginationItem> paginationItems = new ArrayList<>();
        String providerId = menuData.getContentProvider();

        if (!plugin.getContentProviderManager().hasProvider(providerId)) {
            plugin.getLogger().warning("Content provider '" + providerId + "' not found for menu '" + menuId + "'");
            return paginationItems;
        }

        // Fusionner les args YAML statiques avec les args runtime passés via openMenu(player, id, args)
        Map<String, String> yamlArgs = menuData.getProviderArgs();
        Map<String, String> mergedArgs = new java.util.HashMap<>(yamlArgs != null ? yamlArgs : java.util.Collections.emptyMap());
        Map<String, Map<String, String>> playerRuntimeArgs = runtimeArgs.get(player.getUniqueId());
        Map<String, String> runtime = playerRuntimeArgs != null ? playerRuntimeArgs.get(menuId) : null;
        if (runtime != null) mergedArgs.putAll(runtime);

        List<DynamicItem> dynamicItems = plugin.getContentProviderManager()
            .getContent(providerId, player, mergedArgs);

        for (DynamicItem dynItem : dynamicItems) {
            paginationItems.add(convertToPaginationItem(dynItem));
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[DEBUG] Provider '" + providerId + "' returned "
                + dynamicItems.size() + " items for " + player.getName());
        }

        return paginationItems;
    }

    /**
     * Initialisation d'un menu paginé classique.
     */
    private void initializePaginationMenu(Player player, String menuId, int page, MenuData menuData) {
        plugin.getPaginationManager().initPlayer(player, menuId, page, menuData.getStaticMaxPages());

        if (menuData.hasContentProvider()) {
            List<PaginationItem> paginationItems = loadProviderItems(player, menuId, menuData);
            plugin.getPaginationManager().initializeForPlayer(player, menuData, paginationItems);
        }
    }

    /**
     * Initialisation d'un menu scroll.
     * - Prépare ScrollManager (offset/max)
     * - Synchronise %page%/%max_page% via PaginationManager
     */
    private void initializeScrollMenu(Player player, String menuId, int page, MenuData menuData) {
        int startPage = Math.max(1, page);
        int staticPages = Math.max(1, menuData.getStaticMaxPages());

        // Garantit la présence des placeholders %page%/%max_page%.
        plugin.getPaginationManager().initPlayer(player, menuId, startPage, staticPages);

        int forcedMaxOffset = staticPages - 1;
        int totalContentItems;

        if (menuData.hasContentProvider()) {
            List<PaginationItem> providerItems = loadProviderItems(player, menuId, menuData);
            me.krunsh.kgui.pagination.PaginationManager.PageData pageData = plugin.getPaginationManager().getPageData(player, menuId);
            if (pageData != null) {
                pageData.setItems(providerItems);
            }

            totalContentItems = providerItems.size();
            forcedMaxOffset = -1; // calcul automatique basé sur le contenu dynamique.
        } else {
            // Menus statiques: simule un nombre de lignes suffisant pour atteindre max_pages.
            int visibleRows = getVisibleRows(menuData.getContentSlots());
            int rowSize = getColumnsPerRow(menuData.getContentSlots());
            int totalRows = visibleRows + forcedMaxOffset;
            totalContentItems = Math.max(rowSize * totalRows, menuData.getContentSlots().size());
        }

        plugin.getScrollManager().initializeForPlayer(player, menuData, totalContentItems, forcedMaxOffset);
        syncPaginationFromScroll(player, menuId);
    }

    /**
     * Synchronise les placeholders %page%/%max_page% avec l'état du scroll.
     */
    private void syncPaginationFromScroll(Player player, String menuId) {
        me.krunsh.kgui.pagination.ScrollManager.ScrollData scrollData = plugin.getScrollManager().getScrollData(player, menuId);
        if (scrollData == null) {
            return;
        }

        me.krunsh.kgui.pagination.PaginationManager.PageData pageData = plugin.getPaginationManager().getPageData(player, menuId);
        if (pageData == null) {
            plugin.getPaginationManager().initPlayer(player, menuId, 1, Math.max(1, scrollData.getMaxOffset() + 1));
            pageData = plugin.getPaginationManager().getPageData(player, menuId);
        }

        if (pageData != null) {
            int maxPage = Math.max(1, scrollData.getMaxOffset() + 1);
            int currentPage = Math.min(maxPage, scrollData.getScrollOffset() + 1);
            pageData.setMaxPage(maxPage);
            pageData.setCurrentPage(currentPage);
        }
    }

    /**
     * Rendu des items dynamiques en mode scroll (offset ligne par ligne).
     */
    private void renderScrollDynamicContent(Player player, Inventory inventory, MenuData menuData, Map<Integer, Integer> slotPriorities) {
        me.krunsh.kgui.pagination.PaginationManager.PageData pageData = plugin.getPaginationManager().getPageData(player, menuData.getId());
        if (pageData == null || pageData.getItems().isEmpty()) {
            return;
        }

        List<Integer> contentSlots = menuData.getContentSlots();
        for (int i = 0; i < contentSlots.size(); i++) {
            int slot = contentSlots.get(i);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            int contentIndex = plugin.getScrollManager().getContentIndexForSlot(player, menuData.getId(), i);
            if (contentIndex < 0 || contentIndex >= pageData.getItems().size()) {
                continue;
            }

            PaginationItem pagItem = pageData.getItems().get(contentIndex);
            ItemStack item = buildPaginationItem(player, pagItem, menuData.getId());
            if (item != null) {
                inventory.setItem(slot, item);
                slotPriorities.put(slot, Integer.MAX_VALUE);
            }
        }
    }

    private int getColumnsPerRow(List<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            return 9;
        }

        Map<Integer, Integer> rowCounts = new HashMap<>();
        for (int slot : slots) {
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

    private int getVisibleRows(List<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            return 1;
        }

        java.util.Set<Integer> rows = new HashSet<>();
        for (int slot : slots) {
            rows.add(slot / 9);
        }

        return Math.max(1, rows.size());
    }
    
    /**
     * Convertit un DynamicItem (API) en PaginationItem (interne)
     * Utilisé pour les menus utilisant le Content Provider API
     */
    private PaginationItem convertToPaginationItem(DynamicItem dynItem) {
        // Extraire les propriétés depuis le DynamicItem
        String material = dynItem.getMaterial() != null ? dynItem.getMaterial() : "PAPER";
        short data = dynItem.getData();
        String name = dynItem.getName();
        List<String> lore = dynItem.getLore() != null ? dynItem.getLore() : new ArrayList<>();
        boolean glow = dynItem.isGlow();
        
        // Les placeholders custom de l'item
        Map<String, String> placeholders = dynItem.getCustomData() != null ? 
            new HashMap<>(dynItem.getCustomData()) : new HashMap<>();
        
        // Les actions de clic
        List<String> clickActions = dynItem.getClickActions() != null ? 
            dynItem.getClickActions() : new ArrayList<>();
        
        return new PaginationItem(material, data, name, lore, glow, placeholders, clickActions,
            dynItem.getSkullOwner(), dynItem.getHeadDatabaseId());
    }
    
    /**
     * Construit un ItemStack depuis un PaginationItem dynamique
     * Utilisé pour les menus paginés avec contenu injecté (logs faction, etc.)
     */
    private ItemStack buildPaginationItem(Player player, me.krunsh.kgui.pagination.PaginationManager.PaginationItem pagItem, String menuId) {
        try {
            Material material = Material.valueOf(pagItem.getMaterial().toUpperCase());
            ItemBuilder builder = new ItemBuilder(material);
            builder.data(pagItem.getData());
            
            // Nom avec placeholders
            if (pagItem.getName() != null) {
                String name = parsePlaceholders(player, pagItem.getName());
                // Appliquer les placeholders custom de l'item
                for (Map.Entry<String, String> ph : pagItem.getPlaceholders().entrySet()) {
                    name = name.replace("%" + ph.getKey() + "%", ph.getValue());
                }
                builder.name(name);
            }
            
            // Lore avec placeholders
            if (pagItem.getLore() != null) {
                List<String> lore = new ArrayList<>();
                for (String line : pagItem.getLore()) {
                    String parsedLine = parsePlaceholders(player, line);
                    // Appliquer les placeholders custom
                    for (Map.Entry<String, String> ph : pagItem.getPlaceholders().entrySet()) {
                        parsedLine = parsedLine.replace("%" + ph.getKey() + "%", ph.getValue());
                    }
                    lore.add(parsedLine);
                }
                builder.lore(lore);
            }
            
            // Glow effect
            if (pagItem.isGlow()) {
                builder.glow();
            }

            // Skull owner pour les têtes de joueurs
            if (pagItem.getSkullOwner() != null) {
                builder.skull(pagItem.getSkullOwner());
            }
            
            ItemStack item = builder.build();
            
            // Ajouter le tag NBT de protection + stocker les click_actions
            item = tagAsGuiItem(item, menuId);
            
            // Stocker les click_actions dans NBT pour récupération lors du clic
            if (pagItem.getClickActions() != null && !pagItem.getClickActions().isEmpty()) {
                NBTItem nbtItem = new NBTItem(item);
                // Sérialiser les actions en JSON
                StringBuilder actions = new StringBuilder();
                for (int i = 0; i < pagItem.getClickActions().size(); i++) {
                    if (i > 0) actions.append("||");
                    actions.append(pagItem.getClickActions().get(i));
                }
                nbtItem.setString("kgui_pagination_actions", actions.toString());
                item = nbtItem.getItem();
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Error building pagination item: " + e.getMessage());
            return null;
        }
    }

    /**
     * Construit un ItemStack depuis un MenuItem
     */
    private ItemStack buildMenuItem(Player player, MenuItem menuItem) {
        ItemStack item;
        
        if (menuItem.isFromRegistry()) {
            // Item depuis le registry
            item = plugin.getItemRegistry().buildItem(menuItem.getItemId());
            if (item == null) {
                plugin.getLogger().warning("Item not found in registry: " + menuItem.getItemId());
                return null;
            }
        } else {
            // Item inline
            ItemBuilder builder = new ItemBuilder(menuItem.getInlineMaterial());
            builder.data(menuItem.getInlineData());
            
            if (menuItem.getInlineName() != null) {
                builder.name(parsePlaceholders(player, menuItem.getInlineName()));
            }
            
            if (menuItem.getInlineLore() != null) {
                List<String> lore = new ArrayList<>();
                for (String line : menuItem.getInlineLore()) {
                    lore.add(parsePlaceholders(player, line));
                }
                builder.lore(lore);
            }
            
            if (menuItem.isInlineGlow()) {
                builder.glow();
            }
            
            if (menuItem.getInlineSkull() != null) {
                builder.skull(menuItem.getInlineSkull());
            }
            
            // HeadDatabase support
            if (menuItem.getInlineHdb() != null && !menuItem.getInlineHdb().isEmpty()) {
                if (plugin.getHookManager().isHeadDatabaseEnabled()) {
                    ItemStack hdbItem = plugin.getHookManager().getHeadDatabaseHook().getHead(menuItem.getInlineHdb());
                    if (hdbItem != null) {
                        // Copier les meta du builder sur l'item HDB
                        ItemStack builtItem = builder.build();
                        hdbItem.setItemMeta(builtItem.getItemMeta());
                        item = hdbItem;
                    } else {
                        item = builder.build();
                    }
                } else {
                    item = builder.build();
                }
            } else {
                item = builder.build();
            }
            
            // Appliquer le CIT si défini
            if (menuItem.getInlineCit() != null && !menuItem.getInlineCit().isEmpty()) {
                NBTItem nbtItem = new NBTItem(item);
                nbtItem.setString("sparrowmc-item", menuItem.getInlineCit());
                item = nbtItem.getItem();
            }
        }
        
        // Appliquer la quantité
        try {
            String amountStr = parsePlaceholders(player, menuItem.getAmount());
            int amount = Integer.parseInt(amountStr);
            item.setAmount(Math.max(1, Math.min(64, amount)));
        } catch (NumberFormatException e) {
            item.setAmount(1);
        }
        
        return item;
    }

    /**
     * Ajoute le tag NBT de protection GUI
     */
    public ItemStack tagAsGuiItem(ItemStack item, String menuId) {
        if (item == null || item.getType() == Material.AIR) return item;
        
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setBoolean(plugin.getConfigManager().getNbtTag(), true);
        nbtItem.setString(plugin.getConfigManager().getNbtTag() + "_menu", menuId);
        return nbtItem.getItem();
    }

    /**
     * Vérifie si un item est un item de GUI
     */
    public boolean isGuiItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(plugin.getConfigManager().getNbtTag());
    }

    /**
     * Parse les placeholders dans une string
     * Utilise un cache pour optimiser les performances (serveur 1K joueurs)
     */
    public String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        
        // Placeholders internes (toujours calculés, très rapide)
        text = text.replace("%player%", player.getName())
                   .replace("%player_name%", player.getName())
                   .replace("%player_displayname%", player.getDisplayName());
        
        // Placeholders de pagination
        String currentMenu = getPlayerCurrentMenu(player);
        if (currentMenu != null) {
            text = plugin.getPaginationManager().replacePlaceholders(text, player, currentMenu);
            text = plugin.getScrollManager().replacePlaceholders(text, player, currentMenu);
        }
        
        // PlaceholderAPI avec cache
        if (plugin.getHookManager().isPlaceholderAPIEnabled()) {
            text = parseWithCache(player, text);
        }
        
        return text;
    }

    /**
     * Parse les placeholders avec un menu spécifique (pour les menus pas encore ouverts)
     */
    public String parsePlaceholders(Player player, String text, String menuId) {
        if (text == null) return "";
        
        // Placeholders internes
        text = text.replace("%player%", player.getName())
                   .replace("%player_name%", player.getName())
                   .replace("%player_displayname%", player.getDisplayName());
        
        // Placeholders de pagination - utiliser le menuId fourni
        text = plugin.getPaginationManager().replacePlaceholders(text, player, menuId);
        text = plugin.getScrollManager().replacePlaceholders(text, player, menuId);
        
        // PlaceholderAPI avec cache
        if (plugin.getHookManager().isPlaceholderAPIEnabled()) {
            text = parseWithCache(player, text);
        }
        
        return text;
    }
    
    /**
     * Parse les placeholders PlaceholderAPI avec mise en cache
     * TTL configurable dans config.yml: placeholder-cache-ttl (ms)
     */
    private String parseWithCache(Player player, String text) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cacheTTL = plugin.getConfigManager().getConfig().getLong("placeholder-cache-ttl", 500L);
        
        // Vérifier si le cache est encore valide
        Long lastCacheTime = placeholderCacheTime.get(uuid);
        if (lastCacheTime != null && (now - lastCacheTime) < cacheTTL) {
            // Utiliser le cache
            Map<String, String> playerCache = placeholderCache.get(uuid);
            if (playerCache != null && playerCache.containsKey(text)) {
                return playerCache.get(text);
            }
        } else {
            // Cache expiré, le nettoyer
            placeholderCache.remove(uuid);
            placeholderCacheTime.remove(uuid);
        }
        
        // Parser via PlaceholderAPI
        String result = plugin.getHookManager().getPlaceholderAPIHook().setPlaceholders(player, text);
        
        // Stocker dans le cache
        placeholderCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(text, result);
        placeholderCacheTime.put(uuid, now);
        
        return result;
    }
    
    /**
     * Invalide le cache de placeholders pour un joueur
     * À appeler lors du refresh forcé ou fermeture du menu
     */
    public void invalidatePlaceholderCache(Player player) {
        if (player != null) {
            placeholderCache.remove(player.getUniqueId());
            placeholderCacheTime.remove(player.getUniqueId());
        }
    }
    
    /**
     * Invalide tous les caches de placeholders
     */
    public void invalidateAllPlaceholderCaches() {
        placeholderCache.clear();
        placeholderCacheTime.clear();
    }

    /**
     * Joue le son d'ouverture
     */
    private void playOpenSound(Player player) {
        ConfigManager.SoundData soundData = plugin.getConfigManager().getSoundData("sounds.menu_open");
        if (soundData != null) {
            player.playSound(player.getLocation(), soundData.getSound(), soundData.getVolume(), soundData.getPitch());
        }
    }

    /**
     * Exécute les actions d'ouverture
     */
    private void executeOpenActions(Player player, MenuData menuData) {
        plugin.getActionManager().executeActions(player, menuData.getOpenActions());
    }

    /**
     * Ferme le menu d'un joueur
     */
    public void closeMenu(Player player) {
        closeMenu(player, true);
    }

    /**
     * Ferme le menu d'un joueur
     */
    public void closeMenu(Player player, boolean executeActions) {
        // Ignorer si le joueur est en train de refresh (changement de page)
        if (refreshingPlayers.contains(player.getUniqueId())) {
            return;
        }
        
        OpenGui openGui = openGuis.remove(player.getUniqueId());
        if (openGui != null && executeActions) {
            MenuData menuData = plugin.getMenuManager().getMenu(openGui.getMenuId());
            if (menuData != null) {
                plugin.getActionManager().executeActions(player, menuData.getCloseActions());
            }
        }
        
        // Nettoyer le cache de placeholders
        invalidatePlaceholderCache(player);
        runtimeArgs.remove(player.getUniqueId());
        refreshGate.clear(player.getUniqueId());
        navigationLocks.remove(player.getUniqueId());
        
        // Nettoyer l'inventaire du joueur des items GUI
        cleanPlayerInventory(player);
    }

    /**
     * Ferme tous les menus
     */
    public void closeAllMenus() {
        for (UUID uuid : new HashSet<>(openGuis.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.closeInventory();
                closeMenu(player, false);
            }
        }
        openGuis.clear();
    }

    /**
     * Nettoie l'inventaire du joueur des items GUI
     */
    public void cleanPlayerInventory(Player player) {
        // Curseur
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && isGuiItem(cursor)) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }
        
        // Inventaire
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isGuiItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        
        // Armure
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isGuiItem(armor[i])) {
                armor[i] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setArmorContents(armor);
        }
        
        player.updateInventory();
    }

    /** Regroupe les demandes du meme tick avant d'effectuer le refresh. */
    public void refreshMenu(Player player) {
        OpenGui openGui = openGuis.get(player.getUniqueId());
        if (openGui == null) return;

        final UUID playerUuid = player.getUniqueId();
        final String expectedMenuId = openGui.getMenuId();
        if (!refreshGate.trySchedule(playerUuid, expectedMenuId)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                performRefresh(player, expectedMenuId);
            } finally {
                refreshGate.complete(playerUuid, expectedMenuId);
            }
        });
    }

    /**
     * Invalidation evenementielle des seules vues correspondant au menu ou au
     * provider. Le scan ne se produit qu'a la reception d'un evenement, jamais
     * dans une boucle de polling supplementaire.
     */
    public void refreshMatchingMenus(String menuId, String providerId) {
        for (Map.Entry<UUID, OpenGui> entry : new HashMap<>(openGuis).entrySet()) {
            OpenGui openGui = entry.getValue();
            if (menuId != null && !menuId.equalsIgnoreCase(openGui.getMenuId())) {
                continue;
            }
            if (providerId != null) {
                MenuData menu = plugin.getMenuManager().getMenu(openGui.getMenuId());
                if (menu == null || !providerId.equalsIgnoreCase(menu.getContentProvider())) {
                    continue;
                }
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                refreshMenu(player);
            }
        }
    }

    /**
     * Verrou court utilise uniquement pour empecher deux traitements de navigation
     * pendant le meme tick. Il ne bloque jamais un auto-refresh deja planifie.
     */
    public boolean tryLockNavigation(Player player) {
        final UUID uuid = player.getUniqueId();
        if (!navigationLocks.add(uuid)) {
            return false;
        }
        Bukkit.getScheduler().runTask(plugin, () -> navigationLocks.remove(uuid));
        return true;
    }

    /**
     * Recharge les donnees puis modifie seulement les slots qui ont change. Une
     * reouverture n'est faite que si le titre ou la taille doivent reellement changer.
     */
    private void performRefresh(Player player, String expectedMenuId) {
        OpenGui openGui = openGuis.get(player.getUniqueId());
        if (openGui == null || !openGui.getMenuId().equals(expectedMenuId)) return;

        MenuData menuData = plugin.getMenuManager().getMenu(openGui.getMenuId());
        if (menuData == null) return;

        Inventory currentInventory = openGui.getInventory();
        if (!player.isOnline() || player.getOpenInventory() == null
                || player.getOpenInventory().getTopInventory() != currentInventory) {
            return;
        }

        placeholderCache.remove(player.getUniqueId());
        refreshDynamicContent(player, menuData);

        int currentPage = plugin.getPaginationManager().getCurrentPage(player, openGui.getMenuId());
        openGui.setPage(currentPage);
        openGui.setTotalPages(plugin.getPaginationManager().getMaxPage(player, openGui.getMenuId()));
        openGui.setScrollOffset(plugin.getScrollManager().getScrollOffset(player, openGui.getMenuId()));

        String title = ColorUtils.colorize(parsePlaceholders(player, menuData.getTitle(), openGui.getMenuId()));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        KguiInventoryHolder nextHolder = new KguiInventoryHolder(
            openGui.getMenuId(), currentPage, player.getUniqueId());
        Inventory nextInventory = Bukkit.createInventory(nextHolder, menuData.getSize(), title);
        nextHolder.setInventory(nextInventory);
        fillInventory(player, nextInventory, menuData, currentPage);

        String currentTitle = player.getOpenInventory().getTitle();
        boolean mustReopen = currentInventory.getSize() != nextInventory.getSize()
            || !title.equals(currentTitle);

        if (mustReopen) {
            refreshingPlayers.add(player.getUniqueId());
            try {
                openGui.setInventory(nextInventory);
                player.openInventory(nextInventory);
            } finally {
                refreshingPlayers.remove(player.getUniqueId());
            }
        } else {
            ItemStack[] currentContents = currentInventory.getContents();
            ItemStack[] nextContents = nextInventory.getContents();
            for (int slot : SlotDiff.changedSlots(currentContents, nextContents)) {
                currentInventory.setItem(slot, nextContents[slot]);
            }

            KguiInventoryHolder currentHolder = KguiInventoryHolder.getHolder(currentInventory);
            if (currentHolder != null) {
                currentHolder.setPage(currentPage);
            }
        }
        openGui.setLastRefreshTime(System.currentTimeMillis());
    }

    /** Recharge un provider sans perdre la page ou l'offset courants. */
    private void refreshDynamicContent(Player player, MenuData menuData) {
        if (!menuData.hasContentProvider()) {
            return;
        }

        List<PaginationItem> items = loadProviderItems(player, menuData.getId(), menuData);
        if (menuData.getMenuType() == me.krunsh.kgui.menu.MenuType.PAGINATION) {
            plugin.getPaginationManager().initializeForPlayer(player, menuData, items);
            return;
        }

        if (menuData.getMenuType() == me.krunsh.kgui.menu.MenuType.SCROLL) {
            me.krunsh.kgui.pagination.PaginationManager.PageData pageData =
                plugin.getPaginationManager().getPageData(player, menuData.getId());
            if (pageData != null) {
                pageData.setItems(items);
            }
            plugin.getScrollManager().refreshForPlayer(player, menuData, items.size(), -1);
            syncPaginationFromScroll(player, menuData.getId());
        }
    }

    /**
     * Vérifie si un joueur a un menu ouvert
     */
    public boolean hasOpenMenu(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    /**
     * Obtient la GUI ouverte d'un joueur
     */
    public OpenGui getOpenGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    /**
     * Met à jour un slot spécifique via ProtocolLib (si disponible)
     */
    public void updateSlot(Player player, int slot, ItemStack item) {
        OpenGui openGui = openGuis.get(player.getUniqueId());
        if (openGui == null) return;
        
        // Mettre à jour l'item
        item = tagAsGuiItem(item, openGui.getMenuId());
        openGui.getInventory().setItem(slot, item);
        
        // Utiliser ProtocolLib si disponible pour un update plus propre
        if (plugin.getConfigManager().isUseProtocolLib() && plugin.getHookManager().isProtocolLibEnabled()) {
            plugin.getHookManager().getProtocolLibHook().sendSlotUpdate(player, slot, item);
        } else {
            player.updateInventory();
        }
    }

    /**
     * Obtient le menu actuellement ouvert par un joueur
     */
    public String getPlayerCurrentMenu(Player player) {
        OpenGui openGui = openGuis.get(player.getUniqueId());
        return openGui != null ? openGui.getMenuId() : null;
    }

    /**
     * Obtient et retire le menu précédent de la pile (pour bouton retour)
     * Supporte la navigation multi-niveaux: faction_menu → logs → logs_economy → [back] → logs → [back] → faction_menu
     */
    public String getPlayerPreviousMenu(Player player) {
        Deque<String> history = menuHistory.get(player.getUniqueId());
        if (history == null || history.isEmpty()) return null;
        return history.poll();
    }

    /**
     * Nettoie l'historique des menus d'un joueur
     */
    public void cleanupPlayerHistory(UUID uuid) {
        menuHistory.remove(uuid);
    }
    
    // === COOLDOWN SYSTEM ===
    
    /**
     * Vérifie si un joueur est en cooldown pour un item
     * @param player Le joueur
     * @param menuId L'ID du menu
     * @param itemId L'ID de l'item
     * @param cooldownMs Durée du cooldown en ms
     * @return true si en cooldown (ne peut pas cliquer)
     */
    public boolean isOnCooldown(Player player, String menuId, String itemId, long cooldownMs) {
        if (cooldownMs <= 0) return false;
        
        String key = menuId + ":" + itemId;
        Map<String, Long> playerCooldowns = itemCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;
        
        Long lastClick = playerCooldowns.get(key);
        if (lastClick == null) return false;
        
        return (System.currentTimeMillis() - lastClick) < cooldownMs;
    }
    
    /**
     * Obtient le temps restant de cooldown en ms
     */
    public long getCooldownRemaining(Player player, String menuId, String itemId, long cooldownMs) {
        if (cooldownMs <= 0) return 0;
        
        String key = menuId + ":" + itemId;
        Map<String, Long> playerCooldowns = itemCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        
        Long lastClick = playerCooldowns.get(key);
        if (lastClick == null) return 0;
        
        long elapsed = System.currentTimeMillis() - lastClick;
        return Math.max(0, cooldownMs - elapsed);
    }
    
    /**
     * Enregistre un clic (démarre le cooldown)
     */
    public void setItemCooldown(Player player, String menuId, String itemId) {
        String key = menuId + ":" + itemId;
        itemCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                     .put(key, System.currentTimeMillis());
    }
    
    /**
     * Nettoie les cooldowns d'un joueur
     */
    public void cleanupCooldowns(UUID uuid) {
        itemCooldowns.remove(uuid);
    }
    
    // === AUTO-REFRESH SYSTEM ===
    
    /**
     * Démarre la tâche d'auto-refresh
     */
    public void startAutoRefreshTask() {
        if (autoRefreshTaskId != -1) return;
        
        // Interval fixe pour la vérification (20 ticks = 1 seconde)
        // Le refresh réel dépend du updateInterval de chaque menu
        int checkIntervalTicks = 20;
        
        autoRefreshTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, OpenGui> entry : new HashMap<>(openGuis).entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;
                
                OpenGui openGui = entry.getValue();
                MenuData menuData = plugin.getMenuManager().getMenu(openGui.getMenuId());
                if (menuData == null || menuData.getUpdateInterval() <= 0) continue;
                
                // Vérifier si c'est le moment de refresh
                long now = System.currentTimeMillis();
                long lastRefresh = openGui.getLastRefreshTime();
                long refreshIntervalMs = menuData.getUpdateInterval() * 50L; // ticks -> ms
                
                if (now - lastRefresh >= refreshIntervalMs) {
                    refreshMenu(player);
                }
            }
        }, checkIntervalTicks, checkIntervalTicks).getTaskId();
    }
    
    /**
     * Arrête la tâche d'auto-refresh
     */
    public void stopAutoRefreshTask() {
        if (autoRefreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoRefreshTaskId);
            autoRefreshTaskId = -1;
        }
    }
}
