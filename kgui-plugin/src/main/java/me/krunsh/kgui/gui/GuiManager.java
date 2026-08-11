package me.krunsh.kgui.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.ContentItem;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.config.ConfigManager;
import me.krunsh.kgui.menu.MenuData;
import me.krunsh.kgui.menu.MenuItem;
import me.krunsh.kgui.navigation.NavigationMode;
import me.krunsh.kgui.navigation.ViewportLayout;
import me.krunsh.kgui.navigation.ViewportState;
import me.krunsh.kgui.provider.ProviderSnapshot;
import me.krunsh.kgui.refresh.RefreshPriority;
import me.krunsh.kgui.refresh.RefreshRequest;
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
        long openStarted = System.nanoTime();
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
        session.setViewport(createViewport(menuData, page));
        session.inheritNavigationState(previous);
        if (previous != null && !previous.getMenuId().equals(menuId) && !backNavigation) {
            session.getHistory().push(previous.getMenuId());
        }
        if (arguments != null && !arguments.isEmpty()) {
            session.getRuntimeArguments().put(menuId, new HashMap<>(arguments));
        }
        if (previous != null) plugin.getAnimationManager().stopAnimation(player);
        if (previous != null && plugin.getGuiInvalidationBus() != null) {
            plugin.getGuiInvalidationBus().unregister(previous.getToken());
        }
        sessions.activate(session);
        plugin.getGuiInvalidationBus().register(session.getToken(), menuId, menuData.getContentProvider(),
            menuData.getRefreshPolicy().acceptsEvents());
        loadProviderSnapshot(session, menuData, false);
        int currentPageForView = session.getViewport().getPage();
        
        // Créer l'inventaire - parser le titre avec le menuId explicite
        String title = parsePlaceholders(player, menuData.getTitle(), menuId);
        title = ColorUtils.colorize(title);
        
        // Limiter le titre à 32 caractères (limite Minecraft 1.8)
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        
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
        RenderFrame frame = renderInventory(player, menuData, revision, true,
            Collections.<String>emptySet(), null);
        applyFullFrame(inventory, frame);
        session.setFrame(frame);
        
        // Ouvrir l'inventaire
        player.openInventory(inventory);
        
        // Jouer le son d'ouverture
        playOpenSound(player);
        
        // Exécuter les actions d'ouverture
        executeOpenActions(player, menuData);
        plugin.getRefreshScheduler().registerPeriodic(session.getToken(), menuData.getScheduledRefreshInterval());
        plugin.getGuiMetrics().menuOpen(System.nanoTime() - openStarted, menuData.hasContentProvider());
        
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
            true,
            menuData.getId(),
            null
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
    private RenderFrame renderInventory(Player player, MenuData menuData, long revision,
                                        boolean rebuildStatic, java.util.Set<String> targetedItemIds,
                                        ProviderSnapshot previousProvider) {
        long started = System.nanoTime();
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        MenuRenderer renderer = new MenuRenderer(menuData.getSize());
        RenderFrame staticFrame = session == null ? null : session.getStaticFrame();
        if (rebuildStatic || staticFrame == null || staticFrame.size() != menuData.getSize()) {
            staticFrame = renderStaticLayer(player, menuData);
            if (session != null) session.setStaticFrame(staticFrame);
        }
        for (int slot = 0; slot < staticFrame.size(); slot++) {
            RenderedSlot rendered = staticFrame.get(slot);
            if (rendered != null) renderer.place(slot, rendered.getItem(), rendered.getBinding(), 0);
        }

        ProviderSnapshot providerSnapshot = session == null ? null : session.getProviderSnapshot();
        if (providerSnapshot != null) {
            List<ContentItem> items = providerSnapshot.getItems();
            ViewportState viewport = session.getViewport();
            for (int index = 0; index < items.size(); index++) {
                int slot = viewport.slotForSliceIndex(index);
                if (slot < 0 || slot >= menuData.getSize()) continue;
                ContentItem content = items.get(index);
                ItemStack item = reusableProviderItem(session, slot, content, targetedItemIds,
                    previousProvider);
                if (item == null) item = buildProviderItem(player, content, menuData.getId());
                if (item != null) {
                    renderer.place(slot, item, providerBinding(providerSnapshot, content, slot), Integer.MAX_VALUE);
                }
            }
            if (items.isEmpty() && providerSnapshot.getTotalItems() == 0
                    && !viewport.getLayout().isEmpty()) {
                ItemStack empty = buildEmptyProviderItem(player, menuData);
                if (empty != null) renderer.place(viewport.slotForSliceIndex(0), empty, null, Integer.MAX_VALUE);
            }
        }
        RenderFrame result = renderer.finish(revision);
        plugin.getGuiMetrics().render(System.nanoTime() - started, result.occupiedSlots());
        return result;
    }

    private ItemStack reusableProviderItem(PlayerGuiSession session, int slot, ContentItem content,
                                           java.util.Set<String> targetedItemIds,
                                           ProviderSnapshot previousProvider) {
        if (session == null || targetedItemIds == null || targetedItemIds.isEmpty()
                || targetedItemIds.contains(content.getItemId())) return null;
        ProviderSnapshot activeProvider = session.getProviderSnapshot();
        ContentItem previousContent = previousProvider == null ? null
            : previousProvider.getItem(content.getItemId());
        if (activeProvider == null || previousProvider == null
                || !previousProvider.isSlice(activeProvider.getOffset(), activeProvider.getLimit())
                || !sameProviderContent(previousContent, content)) return null;
        RenderFrame previous = session.getFrame();
        RenderedSlot rendered = previous == null ? null : previous.get(slot);
        ClickBinding binding = rendered == null ? null : rendered.getBinding();
        return binding != null && binding.isProviderOwned()
            && content.getItemId().equals(binding.getProviderItemId()) ? rendered.getItem() : null;
    }

    private static boolean sameProviderContent(ContentItem first, ContentItem second) {
        return first != null && second != null
            && first.getData() == second.getData()
            && first.getAmount() == second.getAmount()
            && Objects.equals(first.getMaterial(), second.getMaterial())
            && Objects.equals(first.getDisplayName(), second.getDisplayName())
            && Objects.equals(first.getLore(), second.getLore())
            && Objects.equals(first.getAttributes(), second.getAttributes());
    }

    private RenderFrame renderStaticLayer(Player player, MenuData menuData) {
        MenuRenderer renderer = new MenuRenderer(menuData.getSize());
        Map<Integer, Integer> slotPriorities = new HashMap<>();
        
        // Trier les items par priorité croissante (les plus hauts seront traités en dernier et écraseront)
        List<MenuItem> sortedItems = new ArrayList<>(menuData.getItems().values());
        sortedItems.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        
        for (MenuItem menuItem : sortedItems) {
            // Vérifier les view requirements
            if (!plugin.getRequirementManager().checkRequirements(player, menuItem.getViewRequirements(), false,
                    menuData.getId(), menuItem.getKey())) {
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
        
        return renderer.finish(0L);
    }

    private ViewportState createViewport(MenuData menu, int initialPage) {
        NavigationMode mode = NavigationMode.NONE;
        if (menu.getMenuType() == me.krunsh.kgui.menu.MenuType.PAGINATION) mode = NavigationMode.PAGE;
        else if (menu.getMenuType() == me.krunsh.kgui.menu.MenuType.SCROLL) mode = NavigationMode.ROW_SCROLL;
        ViewportState viewport = new ViewportState(mode, new ViewportLayout(menu.getContentSlots()),
            Math.max(1, initialPage));
        viewport.reconcile(-1, Math.max(1, menu.getStaticMaxPages()));
        return viewport;
    }

    private void loadProviderSnapshot(PlayerGuiSession session, MenuData menu, boolean force) {
        if (!menu.hasContentProvider()) {
            session.setProviderSnapshot(null);
            session.getViewport().reconcile(-1, Math.max(1, menu.getStaticMaxPages()));
            return;
        }
        Map<String, String> merged = new HashMap<>();
        if (menu.getProviderArgs() != null) merged.putAll(menu.getProviderArgs());
        Map<String, String> runtime = session.getRuntimeArguments().get(menu.getId());
        if (runtime != null) merged.putAll(runtime);
        MenuArguments arguments = new MenuArguments(merged);
        ViewportState viewport = session.getViewport();
        int offset = viewport.getContentOffset();
        int limit = viewport.getRequestLimit();
        ProviderSnapshot snapshot = plugin.getProviderEngine().load(session.getPlayerUuid(), menu.getId(),
            menu.getContentProvider(), arguments, offset, limit, session.getProviderSnapshot(), force);
        if (snapshot == null) {
            session.setProviderSnapshot(null);
            viewport.reconcile(0, 0);
            return;
        }
        session.setProviderSnapshot(snapshot);
        viewport.reconcile(snapshot.getTotalItems(), 0);
        if (viewport.getContentOffset() != offset) {
            ProviderSnapshot clamped = plugin.getProviderEngine().load(session.getPlayerUuid(), menu.getId(),
                menu.getContentProvider(), arguments, viewport.getContentOffset(), limit, snapshot, force);
            session.setProviderSnapshot(clamped);
        }
    }

    private ItemStack buildProviderItem(Player player, ContentItem content, String menuId) {
        try {
            Material material = Material.valueOf(content.getMaterial().toUpperCase(Locale.ROOT));
            ItemBuilder builder = new ItemBuilder(material);
            builder.data(content.getData());
            builder.amount(content.getAmount());
            
            if (content.getDisplayName() != null) {
                String name = parsePlaceholders(player, content.getDisplayName());
                for (Map.Entry<String, String> ph : content.getAttributes().entrySet()) {
                    name = name.replace("%" + ph.getKey() + "%", ph.getValue());
                }
                builder.name(name);
            }
            
            if (content.getLore() != null) {
                List<String> lore = new ArrayList<>();
                for (String line : content.getLore()) {
                    String parsedLine = parsePlaceholders(player, line);
                    for (Map.Entry<String, String> ph : content.getAttributes().entrySet()) {
                        parsedLine = parsedLine.replace("%" + ph.getKey() + "%", ph.getValue());
                    }
                    lore.add(parsedLine);
                }
                builder.lore(lore);
            }
            
            if (Boolean.parseBoolean(content.getAttributes().get("glow"))) builder.glow();
            String skullOwner = content.getAttributes().get("skull_owner");
            if (skullOwner != null && !skullOwner.isEmpty()) builder.skull(skullOwner);
            
            ItemStack item = builder.build();
            String headDatabase = content.getAttributes().get("head_database");
            if (headDatabase != null && !headDatabase.isEmpty()
                    && plugin.getHookManager().isHeadDatabaseEnabled()) {
                ItemStack head = plugin.getHookManager().getHeadDatabaseHook().getHead(headDatabase);
                if (head != null) {
                    head.setItemMeta(item.getItemMeta());
                    head.setAmount(content.getAmount());
                    item = head;
                }
            }
            String cit = content.getAttributes().get("cit");
            if (cit != null && !cit.isEmpty()) {
                NBTItem citItem = new NBTItem(item);
                citItem.setString("sparrowmc-item", cit);
                item = citItem.getItem();
            }
            item = tagAsGuiItem(item, menuId);
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Error building provider item " + content.getItemId() + ": " + e.getMessage());
            return null;
        }
    }

    private ClickBinding providerBinding(ProviderSnapshot snapshot, ContentItem item, int slot) {
        Map<String, String> attributes = item.getAttributes();
        return ClickBinding.forProviderItem(snapshot.getProviderId(), item.getItemId(),
            snapshot.getRevision(), snapshot.getGeneration(), slot,
            actions(attributes.get("actions")), actions(attributes.get("left_actions")),
            actions(attributes.get("right_actions")), actions(attributes.get("shift_actions")));
    }

    private ItemStack buildEmptyProviderItem(Player player, MenuData menu) {
        org.bukkit.configuration.ConfigurationSection config = menu.getEmptyItemConfig();
        if (config == null) return null;
        try {
            Material material = Material.valueOf(
                config.getString("material", "BARRIER").toUpperCase(Locale.ROOT));
            ItemBuilder builder = new ItemBuilder(material).data(config.getInt("data", 0));
            String name = config.getString("display_name", config.getString("name", menu.getEmptyMessage()));
            if (name != null) builder.name(parsePlaceholders(player, name));
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("lore")) lore.add(parsePlaceholders(player, line));
            if (!lore.isEmpty()) builder.lore(lore);
            if (config.getBoolean("glow", false)) builder.glow();
            return tagAsGuiItem(builder.build(), menu.getId());
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Invalid empty provider item in " + menu.getId() + ": " + error.getMessage());
            return null;
        }
    }

    private static List<String> actions(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String line : encoded.split("\\r?\\n")) {
            if (!line.trim().isEmpty()) result.add(line.trim());
        }
        return result;
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
        plugin.getGuiMetrics().placeholderResolutions(countPlaceholderTokens(text));
        
        // Placeholders internes (toujours calculés, très rapide)
        text = text.replace("%player%", player.getName())
                   .replace("%player_name%", player.getName())
                   .replace("%player_displayname%", player.getDisplayName());
        
        // Placeholders de pagination
        PlayerGuiSession current = sessions.get(player.getUniqueId());
        if (current != null) text = current.getViewport().replacePlaceholders(text);
        
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
        plugin.getGuiMetrics().placeholderResolutions(countPlaceholderTokens(text));
        
        // Placeholders internes
        text = text.replace("%player%", player.getName())
                   .replace("%player_name%", player.getName())
                   .replace("%player_displayname%", player.getDisplayName());
        
        PlayerGuiSession current = sessions.get(player.getUniqueId());
        if (current != null && current.getMenuId().equalsIgnoreCase(menuId)) {
            text = current.getViewport().replacePlaceholders(text);
        }
        
        // PlaceholderAPI avec cache
        if (plugin.getHookManager().isPlaceholderAPIEnabled()) {
            text = parseWithCache(player, text);
        }
        
        return text;
    }

    private static int countPlaceholderTokens(String text) {
        int count = 0;
        int cursor = 0;
        while (cursor < text.length()) {
            int first = text.indexOf('%', cursor);
            if (first < 0) break;
            int second = text.indexOf('%', first + 1);
            if (second < 0) break;
            count++;
            cursor = second + 1;
        }
        return count;
    }

    public String replaceViewportPlaceholders(Player player, String text, String menuId) {
        if (text == null || player == null) return text;
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        return session != null && (menuId == null || session.getMenuId().equalsIgnoreCase(menuId))
            ? session.getViewport().replacePlaceholders(text) : text;
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
        plugin.getGuiInvalidationBus().unregister(current.getToken());
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
        plugin.getProviderEngine().clearPlayer(uuid);
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
                plugin.getGuiInvalidationBus().unregister(session.getToken());
                sessions.close(session.getPlayerUuid(), session.getSessionId(), reason);
                plugin.getProviderEngine().clearPlayer(session.getPlayerUuid());
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

    /** Ajoute une demande manuelle à la file bornée et coalescée. */
    public void refreshMenu(Player player) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        plugin.getRefreshScheduler().request(new RefreshRequest(session.getToken(),
            RefreshPriority.INVALIDATION, true, true, null));
    }

    public void performScheduledRefresh(RefreshRequest request) {
        if (request == null) return;
        PlayerGuiSession session = sessions.resolve(request.getToken());
        if (session == null) return;
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player != null && player.isOnline()) performRefresh(player, request);
    }

    private void applyFullFrame(Inventory inventory, RenderFrame frame) {
        for (int slot = 0; slot < frame.size(); slot++) {
            RenderedSlot rendered = frame.get(slot);
            inventory.setItem(slot, rendered == null ? null : rendered.getItem());
        }
        plugin.getGuiMetrics().slotsSent(frame.size());
    }

    public boolean navigateNext(Player player) {
        return navigate(player, 1);
    }

    public boolean navigatePrevious(Player player) {
        return navigate(player, -1);
    }

    /** Déplace le viewport d'un nombre borné de pages ou de lignes. */
    public boolean navigateBy(Player player, int delta) {
        if (delta == 0 || delta < -100 || delta > 100) return false;
        return navigate(player, delta);
    }

    public boolean navigateTo(Player player, int page) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.getViewport().setPage(page)) return false;
        requestNavigation(session);
        return true;
    }

    private boolean navigate(Player player, int delta) {
        PlayerGuiSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.getViewport().move(delta)) return false;
        requestNavigation(session);
        return true;
    }

    private void requestNavigation(PlayerGuiSession session) {
        plugin.getRefreshScheduler().request(new RefreshRequest(session.getToken(),
            RefreshPriority.INTERACTION, false, true, null));
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
    private void performRefresh(Player player, RefreshRequest request) {
        PlayerGuiSession session = sessions.resolve(request.getToken());
        if (session == null) return;

        MenuData menuData = plugin.getMenuManager().getMenu(session.getMenuId());
        if (menuData == null) return;

        Inventory currentInventory = session.getInventory();
        Inventory openTop = player.getOpenInventory() == null
            ? null : player.getOpenInventory().getTopInventory();
        if (!player.isOnline() || player.getOpenInventory() == null
                || resolveSession(player, openTop) != session) {
            return;
        }

        if (request.isPlaceholdersDirty()) {
            session.getPlaceholderCache().clear();
            session.setPlaceholderCacheTime(0L);
        }
        ProviderSnapshot currentProvider = session.getProviderSnapshot();
        boolean viewportMiss = menuData.hasContentProvider() && (currentProvider == null
            || !currentProvider.isSlice(session.getViewport().getContentOffset(),
                session.getViewport().getRequestLimit()));
        if (request.isProviderDirty() || viewportMiss) {
            loadProviderSnapshot(session, menuData, request.isProviderDirty());
        }

        int currentPage = session.getViewport().getPage();

        String title = ColorUtils.colorize(parsePlaceholders(player, menuData.getTitle(), session.getMenuId()));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        long revision = session.nextRenderRevision();
        java.util.Set<String> targetedItems = request.isPlaceholdersDirty()
            ? Collections.<String>emptySet() : request.getItemIds();
        RenderFrame nextFrame = renderInventory(player, menuData, revision,
            request.isPlaceholdersDirty(), targetedItems, currentProvider);

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
            plugin.getGuiMetrics().inventoryReopen();
        } else {
            RenderFrame currentFrame = session.getFrame();
            RenderedSlot[] currentSlots = currentFrame == null
                ? new RenderedSlot[nextFrame.size()] : currentFrame.getSlots();
            List<Integer> changed = SlotDiff.changedVisualSlots(currentSlots, nextFrame.getSlots());
            plugin.getGuiMetrics().diff(changed.size());
            for (int slot : changed) {
                RenderedSlot rendered = nextFrame.get(slot);
                currentInventory.setItem(slot, rendered == null ? null : rendered.getItem());
            }
            plugin.getGuiMetrics().slotsSent(changed.size());

            KguiInventoryHolder currentHolder = KguiInventoryHolder.getHolder(currentInventory);
            if (currentHolder != null) {
                currentHolder.setPage(currentPage);
                currentHolder.setRenderRevision(revision);
            }
            session.setFrame(nextFrame);
        }
        session.setLastRefreshTime(System.currentTimeMillis());
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
        // PandaSpigot may expose a distinct CraftInventory wrapper through the
        // InventoryView. The holder still owns the exact inventory registered
        // in the session, which is the stable identity boundary.
        boolean holderOwnsSessionInventory = holder != null && session != null
            && holder.getInventory() == session.getInventory();
        if (holder == null || !SessionAccessPolicy.allows(
                player.getUniqueId(), holder.getPlayerUuid(), holder.getSessionId(),
                holder.getRenderRevision(), session, holderOwnsSessionInventory)) {
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
    
}
