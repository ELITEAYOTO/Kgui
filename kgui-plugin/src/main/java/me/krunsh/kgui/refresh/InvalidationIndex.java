package me.krunsh.kgui.refresh;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.session.SessionToken;

/** Index direct joueur/menu/provider : aucune invalidation ne scanne SessionRegistry. */
public final class InvalidationIndex {
    private final Map<UUID, Entry> players = new HashMap<>();
    private final Map<String, Set<SessionToken>> menus = new HashMap<>();
    private final Map<String, Set<SessionToken>> providers = new HashMap<>();

    public synchronized void register(SessionToken token, String menuId, String providerId,
                                      boolean acceptsEvents) {
        unregisterPlayer(token.getPlayerId());
        Entry entry = new Entry(token, normalize(menuId), normalizeNullable(providerId), acceptsEvents);
        players.put(token.getPlayerId(), entry);
        add(menus, entry.menuId, token);
        if (entry.providerId != null) add(providers, entry.providerId, token);
    }

    public synchronized void unregister(SessionToken token) {
        if (token == null) return;
        Entry current = players.get(token.getPlayerId());
        if (current != null && current.token.equals(token)) unregisterPlayer(token.getPlayerId());
    }

    public synchronized Set<SessionToken> resolve(InvalidationRequest request) {
        if (request == null) return Collections.emptySet();
        switch (request.getScope()) {
            case PLAYER:
                return single(players.get(request.getPlayerId()), true);
            case PLAYER_MENU:
                Entry entry = players.get(request.getPlayerId());
                return entry != null && entry.menuId.equals(normalize(request.getMenuId()))
                    ? single(entry, true) : Collections.<SessionToken>emptySet();
            case MENU:
                return copyActive(menus.get(normalize(request.getMenuId())));
            case PROVIDER:
                return copyActive(providers.get(normalize(request.getProviderId())));
            case ALL:
                Set<SessionToken> all = new LinkedHashSet<>();
                for (Entry value : players.values()) if (value.acceptsEvents) all.add(value.token);
                return all;
            default:
                return Collections.emptySet();
        }
    }

    public synchronized int size() { return players.size(); }
    public synchronized void clear() { players.clear(); menus.clear(); providers.clear(); }

    private void unregisterPlayer(UUID playerId) {
        Entry removed = players.remove(playerId);
        if (removed == null) return;
        remove(menus, removed.menuId, removed.token);
        if (removed.providerId != null) remove(providers, removed.providerId, removed.token);
    }

    private static void add(Map<String, Set<SessionToken>> index, String key, SessionToken token) {
        index.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(token);
    }

    private static void remove(Map<String, Set<SessionToken>> index, String key, SessionToken token) {
        Set<SessionToken> values = index.get(key);
        if (values == null) return;
        values.remove(token);
        if (values.isEmpty()) index.remove(key);
    }

    private static Set<SessionToken> single(Entry entry, boolean requireEvents) {
        return entry == null || (requireEvents && !entry.acceptsEvents) ? Collections.<SessionToken>emptySet()
            : Collections.singleton(entry.token);
    }

    private Set<SessionToken> copyActive(Set<SessionToken> values) {
        if (values == null) return Collections.emptySet();
        Set<SessionToken> result = new LinkedHashSet<>();
        for (SessionToken token : values) {
            Entry entry = players.get(token.getPlayerId());
            if (entry != null && entry.token.equals(token) && entry.acceptsEvents) result.add(token);
        }
        return result;
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("id must not be blank");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : normalize(value);
    }

    private static final class Entry {
        private final SessionToken token;
        private final String menuId;
        private final String providerId;
        private final boolean acceptsEvents;
        private Entry(SessionToken token, String menuId, String providerId, boolean acceptsEvents) {
            this.token = token; this.menuId = menuId; this.providerId = providerId;
            this.acceptsEvents = acceptsEvents;
        }
    }
}
