package me.krunsh.kgui.api;

import java.util.Objects;
import java.util.UUID;

/** Contexte neutre d'une condition enregistree. */
public final class RequirementContext {
    private final UUID playerId;
    private final String menuId;
    private final String itemId;
    private final MenuArguments parameters;

    public RequirementContext(UUID playerId, String menuId, String itemId, MenuArguments parameters) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.menuId = MenuOpenRequest.requireId(menuId, "menuId");
        this.itemId = itemId;
        this.parameters = parameters == null ? MenuArguments.empty() : parameters;
    }

    public UUID getPlayerId() { return playerId; }
    public String getMenuId() { return menuId; }
    public String getItemId() { return itemId; }
    public MenuArguments getParameters() { return parameters; }
}
