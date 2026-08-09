package me.krunsh.kgui.api;

import java.util.Objects;
import java.util.UUID;

/** Requete bornee adressee a un provider de contenu. */
public final class ContentRequest {
    public static final int MAX_LIMIT = 100;
    private final UUID playerId;
    private final String menuId;
    private final String providerId;
    private final int offset;
    private final int limit;
    private final long knownRevision;
    private final MenuArguments arguments;

    public ContentRequest(UUID playerId, String menuId, String providerId, int offset, int limit,
                          long knownRevision, MenuArguments arguments) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.menuId = MenuOpenRequest.requireId(menuId, "menuId");
        this.providerId = MenuOpenRequest.requireId(providerId, "providerId");
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
        if (limit < 1 || limit > MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        this.offset = offset;
        this.limit = limit;
        this.knownRevision = knownRevision;
        this.arguments = arguments == null ? MenuArguments.empty() : arguments;
    }

    public UUID getPlayerId() { return playerId; }
    public String getMenuId() { return menuId; }
    public String getProviderId() { return providerId; }
    public int getOffset() { return offset; }
    public int getLimit() { return limit; }
    public long getKnownRevision() { return knownRevision; }
    public MenuArguments getArguments() { return arguments; }
}
