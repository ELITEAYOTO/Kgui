package me.krunsh.kgui.gui;

import java.util.ArrayList;
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
import me.krunsh.kgui.render.ClickBinding;
import me.krunsh.kgui.render.MenuRenderer;
import me.krunsh.kgui.render.RenderFrame;
import me.krunsh.kgui.render.RenderedSlot;
import me.krunsh.kgui.session.CloseReason;
import me.krunsh.kgui.session.PlayerGuiSession;
import me.krunsh.kgui.session.SessionRegistry;
import me.krunsh.kgui.session.SessionToken;
import me.krunsh.kgui.session.SessionAccessPolicy;
import me.krunsh.kgui.utils.ColorUtils;
import me.krunsh.kgui.utils.ItemBuilder;

/**
 * Gestionnaire des GUIs ouverts
 * Gère l'ouverture, fermeture et tracking des menus
 */
public class GuiManager {

    private final Kgui plugin;
    
    // Menus ouverts par joueur
    private final SessionRegistry sessions = new SessionRegistry();
    
    // Historique des menus (pile pour navigation multi-niveaux avec bouton retour)
    // Navigation, caches, cooldowns et arguments vivent dans PlayerGuiSession.
    
    // Joueurs en cours de refresh (pour éviter que closeMenu supprime le joueur)
    
    // Cache des placeholders par joueur
    
    // Cooldowns: UUID -> (itemKey -> lastClickTime)
    
    // Args runtime conserves par joueur ET par menu pendant toute la session GUI.
    // Ils doivent rester disponibles lors des refreshs et lors d'un retour arriere.

    // Regroupe toutes les demandes de refresh d'un meme tick.
    private final RefreshRequestGate refreshGate = new RefreshRequestGate();

    // Un seul changement de page est accepte par joueur et par tick.
    
    // Auto-refresh task ID
    private int autoRefreshTaskId = -1;
    
    // Flag pour éviter d'ajouter à l'historique (utilisé par [back])

    public GuiManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Ouvre un menu pour un joueur
     */
    public boolean openMenu(Player player, String menuId) {
        return openMenuInternal(player, menuId, 0, null, false);
    }

    /**
     * Ouvre un menu avec des args runtime passés au ContentProvider.
     * Ces args sont fusionnés avec les args statiques du YAML lors de l'appel à getContent().
     * Exemple : kgui.getGuiManager().openMenu(player, "kjobs_detail", Map.of("job_id","mineur"))
     */
    public boolean openMenu(Player player, String menuId, Map<String, String> args) {
        return openMenuInternal(player, menuId, 0, args, false);
    }

    /**
     * Ouvre une page precise avec des arguments runtime. Ce point d'entree sert
     * au contrat Kgui 2 et evite de muter l'etat par une commande joueur.
     */
    public boolean openMenu(Player player, String menuId, int page, Map<String, String> args) {
        return openMenuInternal(player, menuId, page, args, false);
    }

    /**
     * Ouvre un menu en mode "retour" (sans ajouter à l'historique)
     * Utilisé par l'action [back] pour éviter une boucle infinie
     */
    public boolean openMenuBack(Player player, String menuId) {
        return openMenuInternal(player, menuId, 0, null, true);
    }

    public boolean openMenuBack(Player player, String menuId, Map<String, String> arguments) {
        return openMenuInternal(player, menuId, 0, arguments, true);
    }

    /**
     * Ouvre un menu pour un joueur à une page spécifique
     */
    public boolean openMenu(Player player, String menuId, int page) {
        return openMenuInternal(player, menuId, page, null, false);
    }

    private boolean openMenuInternal(Player player, String menuId, int page,
                                     Map<String, String> arguments, boolean backNavigation) {
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

        PlayerGuiSession previous = sessions.get(player.getUniqueId());
        PlayerGuiSession session = sessions.create(player.getUniqueId(), menuId, page, null);
        session.inheritNavigationState(previous);
        if (previous != null && !previous.getMenuId().equals(menuId) && !backNavigation) {
            session.getHistory().push(previous.getMenuId());
        }
        if (arguments != null && !arguments.isEmpty()) {
            session.getRuntimeArguments().put(menuId, new HashMap<>(arguments));
        }
        refreshGate.clear(player.getUniqueId());
        if (previous != null) plugin.getAnimationManager().stopAnimation(player);
        sessions.activate(session);
        
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
        
        session.setPage(currentPageForView);
        long revision = session.nextRenderRevision();
        KguiInventoryHolder holder = new KguiInventoryHolder(
            menuId, currentPageForView, player.getUniqueId(), session.getSessionId(), revision);
        Inventory inventory = Bukkit.createInventory(
            holder,
            menuData.getSize(),
            title
        );
        holder.setInventory(inventory);
        
        // Enregistrer la GUI AVANT de remplir l'inventaire (pour que getPlayerCurrentMenu fonctionne)
        session.setInventory(inventory);
        session.setTotalPages(plugin.getPaginationManager().getMaxPage(player, menuId));
        session.setScrollOffset(plugin.getScrollManager().getScrollOffset(player, menuId));
        
        // Sauvegarder le menu précédent dans la pile (navigation multi-niveaux)
        RenderFrame frame = renderInventory(player, menuData, revision);
        applyFullFrame(inventory, frame);
        session.setFrame(frame);
        
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
    private RenderFrame renderInventory(Player player, MenuData menuData, long revision) {
        // Map pour tracker la priorité des items déjà placés sur chaque slot
        MenuRenderer renderer = new MenuRenderer(menuData.getSize());
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
                if (slot >= 0 && slot < menuData.getSize()) {
                    int existingPriority = slotPriorities.getOrDefault(slot, Integer.MIN_VALUE);
                    // Placer si priorité >= à l'existante
                    if (menuItem.getPriority() >= existingPriority) {
                        renderer.place(slot, item, ClickBinding.forMenuItem(menuItem), menuItem.getPriority());
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
                renderScrollDynamicContent(player, renderer, menuData, slotPriorities);
            } else {
                Map<Integer, me.krunsh.kgui.pagination.PaginationManager.PaginationItem> paginatedItems = 
                    plugin.getPaginationManager().mapItemsToSlots(player, menuData);
                
                for (Map.Entry<Integer, me.krunsh.kgui.pagination.PaginationManager.PaginationItem> entry : paginatedItems.entrySet()) {
                    int slot = entry.getKey();
                    me.krunsh.kgui.pagination.PaginationManager.PaginationItem pagItem = entry.getValue();
                    
                    if (slot >= 0 && slot < menuData.getSize()) {
                        // Construire l'item depuis le PaginationItem
                        ItemStack item = buildPaginationItem(player, pagItem, menuData.getId());
                        if (item != null) {
                            renderer.place(slot, item, dynamicBinding(pagItem), Integer.MAX_VALUE);
                            // Les items paginés ont une priorité maximale
                            slotPriorities.put(slot, Integer.MAX_VALUE);
                        }
                    }
                }
            }
        }
        return renderer.finish(revision);
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        Map<String, Map<String, String>> playerRuntimeArgs = session == null
            ? null : session.getRuntimeArguments();
        Map<String, String> runtime = playerRuntimeArgs != null ? playerRuntimeArgs.get(menuId) : null;
        if (runtime != null) mergedArgs.putAll(runtime);

        List<DynamicItem> dynamicItems = plugin.getContentProviderManager()
            .getContent(providerId, player, mergedArgs);

        for (int index = 0; index < dynamicItems.size(); index++) {
            paginationItems.add(convertToPaginationItem(dynamicItems.get(index), providerId, index));
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
    private void renderScrollDynamicContent(Player player, MenuRenderer renderer, MenuData menuData, Map<Integer, Integer> slotPriorities) {
        me.krunsh.kgui.pagination.PaginationManager.PageData pageData = plugin.getPaginationManager().getPageData(player, menuData.getId());
        if (pageData == null || pageData.getItems().isEmpty()) {
            return;
        }

        List<Integer> contentSlots = menuData.getContentSlots();
        for (int i = 0; i < contentSlots.size(); i++) {
            int slot = contentSlots.get(i);
            if (slot < 0 || slot >= menuData.getSize()) {
                continue;
            }

            int contentIndex = plugin.getScrollManager().getContentIndexForSlot(player, menuData.getId(), i);
            if (contentIndex < 0 || contentIndex >= pageData.getItems().size()) {
                continue;
            }

            PaginationItem pagItem = pageData.getItems().get(contentIndex);
            ItemStack item = buildPaginationItem(player, pagItem, menuData.getId());
            if (item != null) {
                renderer.place(slot, item, dynamicBinding(pagItem), Integer.MAX_VALUE);
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
    private PaginationItem convertToPaginationItem(DynamicItem dynItem, String providerId, int index) {
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
        
        String stableId = placeholders.get("item_id");
        if (stableId == null || stableId.trim().isEmpty()) stableId = placeholders.get("id");
        if (stableId == null || stableId.trim().isEmpty()) stableId = String.valueOf(index);

        return new PaginationItem(material, data, name, lore, glow, placeholders, clickActions,
            dynItem.getLeftClickActions(), dynItem.getRightClickActions(), dynItem.getShiftClickActions(),
            providerId + ":" + stableId, dynItem.getSkullOwner(), dynItem.getHeadDatabaseId());
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
            
            // Ajouter uniquement le marqueur NBT anti-vol.
            item = tagAsGuiItem(item, menuId);
            
            // Les actions exécutables restent exclusivement dans RenderedSlot.
            
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
        try {
            NBTItem nbtItem = new NBTItem(item);
            return nbtItem.hasKey(plugin.getConfigManager().getNbtTag());
        } catch (Throwable ignored) {
            return false;
        }
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return plugin.getHookManager().getPlaceholderAPIHook().setPlaceholders(player, text);
        }
        long now = System.currentTimeMillis();
        long cacheTTL = plugin.getConfigManager().getConfig().getLong("placeholder-cache-ttl", 500L);
        
        // Vérifier si le cache est encore valide
        long lastCacheTime = session.getPlaceholderCacheTime();
        if (lastCacheTime > 0L && (now - lastCacheTime) < cacheTTL) {
            // Utiliser le cache
            Map<String, String> playerCache = session.getPlaceholderCache();
            if (playerCache != null && playerCache.containsKey(text)) {
                return playerCache.get(text);
            }
        } else {
            // Cache expiré, le nettoyer
            session.getPlaceholderCache().clear();
            session.setPlaceholderCacheTime(0L);
        }
        
        // Parser via PlaceholderAPI
        String result = plugin.getHookManager().getPlaceholderAPIHook().setPlaceholders(player, text);
        
        // Stocker dans le cache
        session.getPlaceholderCache().put(text, result);
        session.setPlaceholderCacheTime(now);
        
        return result;
    }
    
    /**
     * Invalide le cache de placeholders pour un joueur
     * À appeler lors du refresh forcé ou fermeture du menu
     */
    public void invalidatePlaceholderCache(Player player) {
        if (player != null) {
            PlayerGuiSession session = sessions.get(player.getUniqueId());
            if (session != null) {
                session.getPlaceholderCache().clear();
                session.setPlaceholderCacheTime(0L);
            }
        }
    }

    /**
     * Invalide tous les caches de placeholders
     */
    public void invalidateAllPlaceholderCaches() {
        for (PlayerGuiSession session : sessions.snapshot()) {
            session.getPlaceholderCache().clear();
            session.setPlaceholderCacheTime(0L);
        }
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
        closeMenu(player, true, CloseReason.PLAYER_CLOSE);
    }

    /**
     * Ferme le menu d'un joueur
     */
    public void closeMenu(Player player, boolean executeActions) {
        closeMenu(player, executeActions, CloseReason.PLAYER_CLOSE);
    }

    public boolean closeMenu(Player player, boolean executeActions, CloseReason reason) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        return session != null && closeSession(player, session.getSessionId(), executeActions, reason);
    }

    public boolean closeSession(Player player, long sessionId, boolean executeActions, CloseReason reason) {
        PlayerGuiSession current = sessions.get(player.getUniqueId());
        if (current == null || current.getSessionId() != sessionId) return false;

        MenuData menuData = plugin.getMenuManager().getMenu(current.getMenuId());
        PlayerGuiSession closed = sessions.close(player.getUniqueId(), sessionId, reason);
        if (closed == null) return false;

        cleanupPlayerState(player, reason);
        if (executeActions && menuData != null) {
            plugin.getActionManager().executeActions(player, menuData.getCloseActions());
        }
        return true;
    }

    private void cleanupPlayerState(Player player, CloseReason reason) {
        UUID uuid = player.getUniqueId();
        refreshGate.clear(uuid);
        plugin.getPaginationManager().cleanupPlayer(uuid);
        plugin.getScrollManager().cleanupPlayer(uuid);
        plugin.getAnimationManager().stopAnimation(player);
        plugin.getActionManager().cancelPending(uuid);
        plugin.getInputManager().discard(player);
        plugin.getChatInputListener().discard(uuid);
        if (reason == CloseReason.QUIT || reason == CloseReason.KICK
                || reason == CloseReason.RELOAD || reason == CloseReason.DISABLE) {
            plugin.getPlayerDataManager().clearData(player);
        }
        cleanPlayerInventory(player);
    }

    /**
     * Ferme tous les menus
     */
    public void closeAllMenus() {
        closeAllMenus(CloseReason.RELOAD);
    }

    public void closeAllMenus(CloseReason reason) {
        for (PlayerGuiSession session : sessions.snapshot()) {
            Player player = Bukkit.getPlayer(session.getPlayerUuid());
            if (player != null && player.isOnline()) {
                closeSession(player, session.getSessionId(), false, reason);
                player.closeInventory();
            } else {
                sessions.close(session.getPlayerUuid(), session.getSessionId(), reason);
                refreshGate.clear(session.getPlayerUuid());
                plugin.getPaginationManager().cleanupPlayer(session.getPlayerUuid());
                plugin.getScrollManager().cleanupPlayer(session.getPlayerUuid());
            }
        }
        sessions.closeAll(reason);
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        final UUID playerUuid = player.getUniqueId();
        final String expectedMenuId = session.getMenuId();
        final SessionToken token = session.getToken();
        if (!refreshGate.trySchedule(playerUuid, expectedMenuId, token.getSessionId())) {
            return;
        }

        final org.bukkit.scheduler.BukkitTask[] taskRef = new org.bukkit.scheduler.BukkitTask[1];
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                performRefresh(player, token);
            } finally {
                refreshGate.complete(playerUuid, expectedMenuId, token.getSessionId());
                PlayerGuiSession active = sessions.resolve(token);
                if (active != null) active.untrackTask(taskRef[0]);
            }
        });
        taskRef[0] = task;
        if (!session.trackTask(task)) task.cancel();
    }

    /**
     * Invalidation evenementielle des seules vues correspondant au menu ou au
     * provider. Le scan ne se produit qu'a la reception d'un evenement, jamais
     * dans une boucle de polling supplementaire.
     */
    public void refreshMatchingMenus(String menuId, String providerId) {
        for (PlayerGuiSession session : sessions.snapshot()) {
            if (menuId != null && !menuId.equalsIgnoreCase(session.getMenuId())) {
                continue;
            }
            if (providerId != null) {
                MenuData menu = plugin.getMenuManager().getMenu(session.getMenuId());
                if (menu == null || !providerId.equalsIgnoreCase(menu.getContentProvider())) {
                    continue;
                }
            }
            Player player = Bukkit.getPlayer(session.getPlayerUuid());
            if (player != null && player.isOnline()) {
                refreshMenu(player);
            }
        }
    }

    private void applyFullFrame(Inventory inventory, RenderFrame frame) {
        for (int slot = 0; slot < frame.size(); slot++) {
            RenderedSlot rendered = frame.get(slot);
            inventory.setItem(slot, rendered == null ? null : rendered.getItem());
        }
    }

    private ClickBinding dynamicBinding(PaginationItem item) {
        return ClickBinding.forDynamicItem(item.getBindingId(), item.getClickActions(),
            item.getLeftClickActions(), item.getRightClickActions(), item.getShiftClickActions());
    }

    /**
     * Verrou court utilise uniquement pour empecher deux traitements de navigation
     * pendant le meme tick. Il ne bloque jamais un auto-refresh deja planifie.
     */
    public boolean tryLockNavigation(Player player) {
        final PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.tryLockNavigation()) return false;
        final SessionToken token = session.getToken();
        final org.bukkit.scheduler.BukkitTask[] taskRef = new org.bukkit.scheduler.BukkitTask[1];
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerGuiSession active = sessions.resolve(token);
            if (active != null) {
                active.unlockNavigation();
                active.untrackTask(taskRef[0]);
            }
        });
        taskRef[0] = task;
        if (!session.trackTask(task)) task.cancel();
        return true;
    }

    /**
     * Recharge les donnees puis modifie seulement les slots qui ont change. Une
     * reouverture n'est faite que si le titre ou la taille doivent reellement changer.
     */
    private void performRefresh(Player player, SessionToken token) {
        PlayerGuiSession session = sessions.resolve(token);
        if (session == null) return;

        MenuData menuData = plugin.getMenuManager().getMenu(session.getMenuId());
        if (menuData == null) return;

        Inventory currentInventory = session.getInventory();
        if (!player.isOnline() || player.getOpenInventory() == null
                || player.getOpenInventory().getTopInventory() != currentInventory) {
            return;
        }

        session.getPlaceholderCache().clear();
        session.setPlaceholderCacheTime(0L);
        refreshDynamicContent(player, menuData);

        int currentPage = plugin.getPaginationManager().getCurrentPage(player, session.getMenuId());
        session.setPage(currentPage);
        session.setTotalPages(plugin.getPaginationManager().getMaxPage(player, session.getMenuId()));
        session.setScrollOffset(plugin.getScrollManager().getScrollOffset(player, session.getMenuId()));

        String title = ColorUtils.colorize(parsePlaceholders(player, menuData.getTitle(), session.getMenuId()));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        long revision = session.nextRenderRevision();
        RenderFrame nextFrame = renderInventory(player, menuData, revision);

        String currentTitle = player.getOpenInventory().getTitle();
        boolean mustReopen = currentInventory.getSize() != nextFrame.size()
            || !title.equals(currentTitle);

        if (mustReopen) {
            KguiInventoryHolder nextHolder = new KguiInventoryHolder(
                session.getMenuId(), currentPage, player.getUniqueId(), session.getSessionId(), revision);
            Inventory nextInventory = Bukkit.createInventory(nextHolder, nextFrame.size(), title);
            nextHolder.setInventory(nextInventory);
            applyFullFrame(nextInventory, nextFrame);
            session.setInventory(nextInventory);
            session.setFrame(nextFrame);
            player.openInventory(nextInventory);
        } else {
            RenderFrame currentFrame = session.getFrame();
            RenderedSlot[] currentSlots = currentFrame == null
                ? new RenderedSlot[nextFrame.size()] : currentFrame.getSlots();
            for (int slot : SlotDiff.changedSlots(currentSlots, nextFrame.getSlots())) {
                RenderedSlot rendered = nextFrame.get(slot);
                currentInventory.setItem(slot, rendered == null ? null : rendered.getItem());
            }

            KguiInventoryHolder currentHolder = KguiInventoryHolder.getHolder(currentInventory);
            if (currentHolder != null) {
                currentHolder.setPage(currentPage);
                currentHolder.setRenderRevision(revision);
            }
            session.setFrame(nextFrame);
        }
        session.setLastRefreshTime(System.currentTimeMillis());
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
        return sessions.get(player.getUniqueId()) != null;
    }

    public SessionToken captureSession(Player player) {
        PlayerGuiSession session = player == null ? null : sessions.get(player.getUniqueId());
        return session == null ? null : session.getToken();
    }

    public boolean isSessionActive(SessionToken token) {
        return sessions.isActive(token);
    }

    public boolean trackSessionTask(SessionToken token, org.bukkit.scheduler.BukkitTask task) {
        PlayerGuiSession session = sessions.resolve(token);
        return session != null && session.trackTask(task);
    }

    public void untrackSessionTask(SessionToken token, org.bukkit.scheduler.BukkitTask task) {
        PlayerGuiSession session = sessions.resolve(token);
        if (session != null) session.untrackTask(task);
    }

    public PlayerGuiSession resolveSession(Player player, Inventory inventory) {
        if (player == null || inventory == null) return null;
        KguiInventoryHolder holder = KguiInventoryHolder.getHolder(inventory);
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (holder == null || !SessionAccessPolicy.allows(
                player.getUniqueId(), holder.getPlayerUuid(), holder.getSessionId(),
                holder.getRenderRevision(), session, session != null && session.getInventory() == inventory)) {
            return null;
        }
        RenderFrame frame = session.getFrame();
        return frame != null && frame.getRevision() == holder.getRenderRevision() ? session : null;
    }

    public RenderedSlot resolveClickedSlot(Player player, Inventory inventory, int rawSlot, ItemStack clicked) {
        PlayerGuiSession session = resolveSession(player, inventory);
        if (session == null || rawSlot < 0 || rawSlot >= inventory.getSize()) return null;
        RenderedSlot rendered = session.getFrame().get(rawSlot);
        return rendered != null && rendered.matches(clicked) ? rendered : null;
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Obtient la GUI ouverte d'un joueur
     */
    public PlayerGuiSession getOpenGui(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Met à jour un slot spécifique via ProtocolLib (si disponible)
     */
    public void updateSlot(Player player, int slot, ItemStack item) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null || slot < 0 || slot >= session.getInventory().getSize()) return;
        
        // Mettre à jour l'item
        item = tagAsGuiItem(item, session.getMenuId());
        session.getInventory().setItem(slot, item);
        RenderFrame oldFrame = session.getFrame();
        if (oldFrame != null) {
            RenderedSlot[] slots = oldFrame.getSlots();
            ClickBinding binding = slots[slot] == null ? null : slots[slot].getBinding();
            slots[slot] = new RenderedSlot(slot, item, binding);
            long revision = session.nextRenderRevision();
            session.setFrame(new RenderFrame(revision, slots));
            KguiInventoryHolder holder = KguiInventoryHolder.getHolder(session.getInventory());
            if (holder != null) holder.setRenderRevision(revision);
        }
        
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        return session != null ? session.getMenuId() : null;
    }

    public Map<String, String> getRuntimeArguments(Player player, String menuId) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return java.util.Collections.emptyMap();
        Map<String, String> arguments = session.getRuntimeArguments().get(menuId);
        return arguments == null ? java.util.Collections.emptyMap() : new HashMap<>(arguments);
    }

    /**
     * Obtient et retire le menu précédent de la pile (pour bouton retour)
     * Supporte la navigation multi-niveaux: faction_menu → logs → logs_economy → [back] → logs → [back] → faction_menu
     */
    public String getPlayerPreviousMenu(Player player) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        java.util.Deque<String> history = session == null ? null : session.getHistory();
        if (history == null || history.isEmpty()) return null;
        return history.poll();
    }

    /**
     * Nettoie l'historique des menus d'un joueur
     */
    public void cleanupPlayerHistory(UUID uuid) {
        PlayerGuiSession session = sessions.get(uuid);
        if (session != null) session.getHistory().clear();
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        Map<String, Long> playerCooldowns = session == null ? null : session.getCooldowns();
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        Map<String, Long> playerCooldowns = session == null ? null : session.getCooldowns();
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
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session != null) session.getCooldowns().put(key, System.currentTimeMillis());
    }
    
    /**
     * Nettoie les cooldowns d'un joueur
     */
    public void cleanupCooldowns(UUID uuid) {
        PlayerGuiSession session = sessions.get(uuid);
        if (session != null) session.getCooldowns().clear();
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
            for (PlayerGuiSession session : sessions.snapshot()) {
                Player player = Bukkit.getPlayer(session.getPlayerUuid());
                if (player == null || !player.isOnline()) continue;
                
                MenuData menuData = plugin.getMenuManager().getMenu(session.getMenuId());
                if (menuData == null || menuData.getUpdateInterval() <= 0) continue;
                
                // Vérifier si c'est le moment de refresh
                long now = System.currentTimeMillis();
                long lastRefresh = session.getLastRefreshTime();
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
