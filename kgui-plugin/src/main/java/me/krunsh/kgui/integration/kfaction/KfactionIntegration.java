package me.krunsh.kgui.integration.kfaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import me.krunsh.kfaction.api.v2.FactionView;
import me.krunsh.kfaction.api.v2.KfactionApiCompatibility;
import me.krunsh.kfaction.api.v2.KfactionApiV2;
import me.krunsh.kfaction.api.v2.KfactionApiV23;
import me.krunsh.kfaction.api.v2.KfactionApis;
import me.krunsh.kfaction.api.v2.KfactionPlayerActions;
import me.krunsh.kfaction.api.v2.MemberView;
import me.krunsh.kfaction.api.v2.event.FactionField;
import me.krunsh.kfaction.api.v2.event.FactionSnapshotChangedEvent;
import me.krunsh.kfaction.api.v2.event.PlayerFactionChangedEvent;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.OwnedRegistration;

/** Adaptateur officiel Kfaction 2.3, entièrement possédé par Kgui. */
public final class KfactionIntegration implements Listener, AutoCloseable {

    static final Set<String> PACK_MENUS = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
            "faction_menu", "faction_members", "faction_claims", "faction_logs",
            "faction_warps", "faction_quests", "faction_rewards", "faction_relations",
            "faction_invites", "faction_zones")));

    private static final Map<FactionField, Set<String>> FIELD_MENUS = fieldMenus();

    private final Kgui kguiPlugin;
    private final Plugin kfactionPlugin;
    private final KguiApi kgui;
    private final KfactionApiV23 api;
    private final KfactionContentProviders contentProviders;
    private final List<OwnedRegistration> handles = new ArrayList<OwnedRegistration>();
    private boolean closed;

    public KfactionIntegration(Kgui kguiPlugin, Plugin kfactionPlugin) {
        if (kguiPlugin == null || kfactionPlugin == null || !kfactionPlugin.isEnabled()) {
            throw new IllegalArgumentException("enabled plugins are required");
        }
        KfactionApiV2 base = KfactionApis.get();
        KfactionApiCompatibility compatibility = KfactionApiCompatibility.evaluate(base);
        KfactionApiV23 api23 = KfactionApis.getV23();
        KfactionPlayerActions playerActions = KfactionApis.getPlayerActions();
        if (compatibility.getStatus() != KfactionApiCompatibility.Status.READY_2_3
                || api23 == null || playerActions == null || api23.getApiMajor() != 2) {
            throw new IllegalStateException("Kfaction API 2.3 and player actions are required (state="
                    + compatibility.getStatus() + ")");
        }
        this.kguiPlugin = kguiPlugin;
        this.kfactionPlugin = kfactionPlugin;
        this.kgui = kguiPlugin.getApi();
        this.api = api23;
        this.contentProviders = new KfactionContentProviders(api23, playerActions);

        try {
            contentProviders.register(kgui, kfactionPlugin, handles);
            new KfactionActionHandlers(playerActions).register(kgui, kfactionPlugin, handles);
            new KfactionRequirements(api23).register(kgui, kfactionPlugin, handles);
            handles.add(kgui.registerMenuPack(kfactionPlugin, "kfaction:default", PACK_MENUS));
            Bukkit.getPluginManager().registerEvents(this, kguiPlugin);
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFactionChanged(FactionSnapshotChangedEvent event) {
        if (closed || event == null) return;
        contentProviders.invalidateRevision();
        Set<UUID> viewers = new LinkedHashSet<UUID>(event.getAffectedPlayers());
        FactionView faction = api.getFaction(event.getFactionId());
        if (faction != null) {
            for (MemberView member : faction.getMembers()) {
                if (member != null && member.getUuid() != null) viewers.add(member.getUuid());
            }
        }
        Set<String> menus = menusFor(event.getFields());
        for (UUID playerId : viewers) {
            for (String menuId : menus) {
                kgui.invalidate(InvalidationRequest.playerMenu(playerId, menuId,
                        Collections.<String>emptySet(), "kfaction:" + fields(event.getFields())));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFactionChanged(PlayerFactionChangedEvent event) {
        if (!closed && event != null) {
            contentProviders.invalidateRevision();
            kgui.invalidate(InvalidationRequest.player(event.getPlayerId(),
                    "kfaction:player-faction-changed"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        schedulePresenceInvalidation(event == null ? null : event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        schedulePresenceInvalidation(event == null ? null : event.getPlayer().getUniqueId());
    }

    private void schedulePresenceInvalidation(UUID playerId) {
        if (closed || playerId == null) return;
        FactionView faction = api.getPlayerFaction(playerId);
        if (faction == null) return;
        final String factionId = faction.getId();
        Bukkit.getScheduler().runTask(kguiPlugin, new Runnable() {
            @Override
            public void run() {
                if (closed) return;
                contentProviders.invalidateRevision();
                FactionView current = api.getFaction(factionId);
                if (current == null) return;
                for (MemberView member : current.getMembers()) {
                    if (member == null || member.getUuid() == null) continue;
                    kgui.invalidate(InvalidationRequest.playerMenu(member.getUuid(), "faction_members",
                            Collections.<String>emptySet(), "kfaction:presence"));
                    kgui.invalidate(InvalidationRequest.playerMenu(member.getUuid(), "faction_menu",
                            Collections.<String>emptySet(), "kfaction:presence"));
                }
            }
        });
    }

    private static Set<String> menusFor(Set<FactionField> fields) {
        if (fields == null || fields.isEmpty() || fields.contains(FactionField.SNAPSHOT)) {
            return PACK_MENUS;
        }
        Set<String> result = new LinkedHashSet<String>();
        for (FactionField field : fields) {
            Set<String> mapped = FIELD_MENUS.get(field);
            if (mapped != null) result.addAll(mapped);
        }
        if (result.isEmpty()) result.add("faction_menu");
        return result;
    }

    private static Map<FactionField, Set<String>> fieldMenus() {
        Map<FactionField, Set<String>> result = new EnumMap<FactionField, Set<String>>(FactionField.class);
        put(result, FactionField.MEMBERS, "faction_menu", "faction_members", "faction_invites");
        put(result, FactionField.TERRITORY, "faction_menu", "faction_claims");
        put(result, FactionField.BANK, "faction_menu");
        put(result, FactionField.RELATIONS, "faction_menu", "faction_relations");
        put(result, FactionField.WARPS, "faction_menu", "faction_warps");
        put(result, FactionField.PROGRESSION, "faction_menu", "faction_quests", "faction_rewards");
        put(result, FactionField.LOGS, "faction_logs");
        put(result, FactionField.PERMISSIONS, "faction_menu");
        put(result, FactionField.SETTINGS, "faction_menu", "faction_warps");
        return Collections.unmodifiableMap(result);
    }

    private static void put(Map<FactionField, Set<String>> target, FactionField field, String... menus) {
        target.put(field, Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(menus))));
    }

    private static String fields(Set<FactionField> fields) {
        if (fields == null || fields.isEmpty()) return "snapshot";
        StringBuilder value = new StringBuilder();
        for (FactionField field : fields) {
            if (value.length() > 0) value.append(',');
            value.append(field.name().toLowerCase(java.util.Locale.ROOT));
        }
        return value.toString();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        HandlerList.unregisterAll(this);
        for (int index = handles.size() - 1; index >= 0; index--) {
            try {
                handles.get(index).close();
            } catch (RuntimeException failure) {
                kguiPlugin.getLogger().warning("Could not unregister Kfaction extension: "
                        + failure.getMessage());
            }
        }
        handles.clear();
        kgui.unregisterAll(kfactionPlugin);
    }
}
