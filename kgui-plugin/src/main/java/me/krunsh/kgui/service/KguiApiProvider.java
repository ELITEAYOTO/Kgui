package me.krunsh.kgui.service;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.ActionHandler;
import me.krunsh.kgui.api.ActionRegistration;
import me.krunsh.kgui.api.ContentProvider;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.KguiApiVersion;
import me.krunsh.kgui.api.MenuOpenRequest;
import me.krunsh.kgui.api.MenuOpenResult;
import me.krunsh.kgui.api.MenuPackRegistration;
import me.krunsh.kgui.api.OwnedRegistration;
import me.krunsh.kgui.api.ProviderRegistration;
import me.krunsh.kgui.api.RequirementHandler;
import me.krunsh.kgui.api.RequirementRegistration;

/** Implementation Bukkit du contrat public Kgui 2.0. */
public final class KguiApiProvider implements KguiApi, AutoCloseable, Listener {
    private final Kgui plugin;
    private final Map<String, Extension<ContentProvider>> providers = new ConcurrentHashMap<>();
    private final Map<String, Extension<ActionHandler>> actions = new ConcurrentHashMap<>();
    private final Map<String, Extension<RequirementHandler>> requirements = new ConcurrentHashMap<>();
    private final Map<String, Extension<Set<String>>> menuPacks = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public KguiApiProvider(Kgui plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin must not be null");
        this.plugin = plugin;
    }

    @Override
    public String getApiVersion() {
        return KguiApiVersion.CURRENT;
    }

    @Override
    public int getApiMajor() {
        return KguiApiVersion.MAJOR;
    }

    @Override
    public MenuOpenResult openMenu(MenuOpenRequest request) {
        if (request == null) return MenuOpenResult.INVALID_REQUEST;
        if (closed.get() || !plugin.isEnabled()) return MenuOpenResult.UNAVAILABLE;
        if (!Bukkit.isPrimaryThread()) return MenuOpenResult.WRONG_THREAD;
        try {
            Player player = Bukkit.getPlayer(request.getPlayerId());
            if (player == null || !player.isOnline()) return MenuOpenResult.PLAYER_OFFLINE;
            if (plugin.getMenuManager().getMenu(request.getMenuId()) == null) return MenuOpenResult.MENU_NOT_FOUND;
            boolean opened = plugin.getGuiManager().openMenu(player, request.getMenuId(), request.getPage(),
                    request.getArguments().asMap());
            return opened ? MenuOpenResult.OPENED : MenuOpenResult.REJECTED;
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Kgui API openMenu failed: " + error.getMessage());
            return MenuOpenResult.ERROR;
        }
    }

    @Override
    public ProviderRegistration registerProvider(Plugin owner, String providerId, ContentProvider provider) {
        String id = register(providers, owner, providerId, provider);
        return new ProviderHandle(id, owner, providers, this::remove);
    }

    @Override
    public ActionRegistration registerAction(Plugin owner, String actionId, ActionHandler handler) {
        String id = register(actions, owner, actionId, handler);
        return new ActionHandle(id, owner, actions, this::remove);
    }

    @Override
    public RequirementRegistration registerRequirement(Plugin owner, String requirementId, RequirementHandler handler) {
        String id = register(requirements, owner, requirementId, handler);
        return new RequirementHandle(id, owner, requirements, this::remove);
    }

    @Override
    public MenuPackRegistration registerMenuPack(Plugin owner, String packId, Set<String> menuIds) {
        Set<String> safeMenus = menuIds == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new java.util.LinkedHashSet<>(menuIds));
        String id = register(menuPacks, owner, packId, safeMenus);
        return new MenuPackHandle(id, owner, menuPacks, this::remove);
    }

    @Override
    public void invalidate(InvalidationRequest request) {
        if (request == null || closed.get() || !plugin.isEnabled()) return;
        Runnable invalidation = () -> performInvalidation(request);
        if (Bukkit.isPrimaryThread()) {
            invalidation.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, invalidation);
        }
    }

    private void performInvalidation(InvalidationRequest request) {
        switch (request.getScope()) {
            case PLAYER_MENU:
            case PLAYER:
                Player player = Bukkit.getPlayer(request.getPlayerId());
                if (player == null || !player.isOnline()) return;
                if (request.getScope() == InvalidationRequest.Scope.PLAYER_MENU) {
                    String current = plugin.getGuiManager().getPlayerCurrentMenu(player);
                    if (current == null || !current.equalsIgnoreCase(request.getMenuId())) return;
                }
                plugin.getGuiManager().refreshMenu(player);
                return;
            case MENU:
                plugin.getGuiManager().refreshMatchingMenus(request.getMenuId(), null);
                return;
            case PROVIDER:
                plugin.getGuiManager().refreshMatchingMenus(null, request.getProviderId());
                return;
            case ALL:
                plugin.getGuiManager().refreshMatchingMenus(null, null);
                return;
            default:
        }
    }

    @Override
    public void unregisterAll(Plugin owner) {
        if (owner == null) return;
        removeOwned(providers, owner);
        removeOwned(actions, owner);
        removeOwned(requirements, owner);
        removeOwned(menuPacks, owner);
    }

    /** Filet de securite contre les references de classloader oubliees. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event != null) unregisterAll(event.getPlugin());
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        providers.clear();
        actions.clear();
        requirements.clear();
        menuPacks.clear();
    }

    public ContentProvider findProvider(String id) {
        Extension<ContentProvider> extension = providers.get(normalizeLookup(id));
        return extension == null ? null : extension.value;
    }

    public ActionHandler findAction(String id) {
        Extension<ActionHandler> extension = actions.get(normalizeLookup(id));
        return extension == null ? null : extension.value;
    }

    public RequirementHandler findRequirement(String id) {
        Extension<RequirementHandler> extension = requirements.get(normalizeLookup(id));
        return extension == null ? null : extension.value;
    }

    private <T> String register(Map<String, Extension<T>> registry, Plugin owner, String requestedId, T value) {
        if (closed.get()) throw new IllegalStateException("Kgui API is closed");
        if (owner == null || value == null) throw new IllegalArgumentException("owner and extension must not be null");
        String id = normalizeOwnedId(owner, requestedId);
        Extension<T> previous = registry.putIfAbsent(id, new Extension<>(owner, value));
        if (previous != null) throw new IllegalStateException("Extension already registered: " + id);
        return id;
    }

    private <T> void remove(Map<String, Extension<T>> registry, String id, Plugin owner) {
        Extension<T> current = registry.get(id);
        if (current != null && current.owner == owner) registry.remove(id, current);
    }

    private static <T> void removeOwned(Map<String, Extension<T>> registry, Plugin owner) {
        for (Map.Entry<String, Extension<T>> entry : registry.entrySet()) {
            if (entry.getValue().owner == owner) registry.remove(entry.getKey(), entry.getValue());
        }
    }

    private static String normalizeOwnedId(Plugin owner, String requestedId) {
        String id = normalizeLookup(requestedId);
        if (id.indexOf(':') < 0) {
            id = owner.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_") + ":" + id;
        }
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid namespaced extension id: " + id);
        }
        return id;
    }

    private static String normalizeLookup(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("id must not be blank");
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Extension<T> {
        private final Plugin owner;
        private final T value;
        private Extension(Plugin owner, T value) { this.owner = owner; this.value = value; }
    }

    private abstract static class AbstractHandle<T> implements OwnedRegistration {
        private final String id;
        private final Plugin owner;
        private final Map<String, Extension<T>> registry;
        private final BiConsumer<Map<String, Extension<T>>, Removal> remover;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private AbstractHandle(String id, Plugin owner, Map<String, Extension<T>> registry,
                               BiConsumer<Map<String, Extension<T>>, Removal> remover) {
            this.id = id;
            this.owner = owner;
            this.registry = registry;
            this.remover = remover;
        }

        @Override public String getId() { return id; }
        @Override public boolean isRegistered() {
            Extension<T> extension = registry.get(id);
            return active.get() && extension != null && extension.owner == owner;
        }
        @Override public void close() {
            if (active.compareAndSet(true, false)) remover.accept(registry, new Removal(id, owner));
        }
    }

    private static final class Removal {
        private final String id;
        private final Plugin owner;
        private Removal(String id, Plugin owner) { this.id = id; this.owner = owner; }
    }

    private <T> void remove(Map<String, Extension<T>> registry, Removal removal) {
        remove(registry, removal.id, removal.owner);
    }

    private static final class ProviderHandle extends AbstractHandle<ContentProvider> implements ProviderRegistration {
        private ProviderHandle(String id, Plugin owner, Map<String, Extension<ContentProvider>> map,
                               BiConsumer<Map<String, Extension<ContentProvider>>, Removal> remover) {
            super(id, owner, map, remover);
        }
    }
    private static final class ActionHandle extends AbstractHandle<ActionHandler> implements ActionRegistration {
        private ActionHandle(String id, Plugin owner, Map<String, Extension<ActionHandler>> map,
                             BiConsumer<Map<String, Extension<ActionHandler>>, Removal> remover) {
            super(id, owner, map, remover);
        }
    }
    private static final class RequirementHandle extends AbstractHandle<RequirementHandler> implements RequirementRegistration {
        private RequirementHandle(String id, Plugin owner, Map<String, Extension<RequirementHandler>> map,
                                  BiConsumer<Map<String, Extension<RequirementHandler>>, Removal> remover) {
            super(id, owner, map, remover);
        }
    }
    private static final class MenuPackHandle extends AbstractHandle<Set<String>> implements MenuPackRegistration {
        private MenuPackHandle(String id, Plugin owner, Map<String, Extension<Set<String>>> map,
                               BiConsumer<Map<String, Extension<Set<String>>>, Removal> remover) {
            super(id, owner, map, remover);
        }
    }
}
