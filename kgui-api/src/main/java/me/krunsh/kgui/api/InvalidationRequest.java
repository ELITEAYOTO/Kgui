package me.krunsh.kgui.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Invalidation ciblee; les champs non pertinents pour le scope restent nuls. */
public final class InvalidationRequest {
    private static final int MAX_ITEM_IDS = 256;
    public enum Scope { PLAYER_MENU, PLAYER, MENU, PROVIDER, ALL }

    private final Scope scope;
    private final UUID playerId;
    private final String menuId;
    private final String providerId;
    private final Set<String> itemIds;
    private final String reason;

    private InvalidationRequest(Scope scope, UUID playerId, String menuId, String providerId,
                                Set<String> itemIds, String reason) {
        this.scope = scope;
        this.playerId = playerId;
        this.menuId = menuId;
        this.providerId = providerId;
        this.itemIds = boundedItemIds(itemIds);
        this.reason = reason;
    }

    public static InvalidationRequest playerMenu(UUID playerId, String menuId, Set<String> itemIds, String reason) {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        return new InvalidationRequest(Scope.PLAYER_MENU, playerId,
                MenuOpenRequest.requireId(menuId, "menuId"), null, itemIds, reason);
    }

    public static InvalidationRequest player(UUID playerId, String reason) {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        return new InvalidationRequest(Scope.PLAYER, playerId, null, null, null, reason);
    }

    public static InvalidationRequest menu(String menuId, String reason) {
        return new InvalidationRequest(Scope.MENU, null, MenuOpenRequest.requireId(menuId, "menuId"), null, null, reason);
    }

    public static InvalidationRequest provider(String providerId, String reason) {
        return new InvalidationRequest(Scope.PROVIDER, null, null,
                MenuOpenRequest.requireId(providerId, "providerId"), null, reason);
    }

    public static InvalidationRequest all(String reason) {
        return new InvalidationRequest(Scope.ALL, null, null, null, null, reason);
    }

    public Scope getScope() { return scope; }
    public UUID getPlayerId() { return playerId; }
    public String getMenuId() { return menuId; }
    public String getProviderId() { return providerId; }
    public Set<String> getItemIds() { return itemIds; }
    public String getReason() { return reason; }

    private static Set<String> boundedItemIds(Set<String> values) {
        if (values == null || values.isEmpty()) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && value.trim().length() <= 128) {
                result.add(value.trim());
            }
            if (result.size() == MAX_ITEM_IDS) break;
        }
        return Collections.unmodifiableSet(result);
    }
}
