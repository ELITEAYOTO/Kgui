package me.krunsh.kgui.api;

import java.util.Objects;
import java.util.UUID;

/** Demande d'ouverture d'un menu pour un joueur. */
public final class MenuOpenRequest {
    private final UUID playerId;
    private final String menuId;
    private final int page;
    private final MenuArguments arguments;

    public MenuOpenRequest(UUID playerId, String menuId) {
        this(playerId, menuId, 0, MenuArguments.empty());
    }

    public MenuOpenRequest(UUID playerId, String menuId, int page, MenuArguments arguments) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.menuId = requireId(menuId, "menuId");
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        this.page = page;
        this.arguments = arguments == null ? MenuArguments.empty() : arguments;
    }

    public UUID getPlayerId() { return playerId; }
    public String getMenuId() { return menuId; }
    public int getPage() { return page; }
    public MenuArguments getArguments() { return arguments; }

    static String requireId(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
