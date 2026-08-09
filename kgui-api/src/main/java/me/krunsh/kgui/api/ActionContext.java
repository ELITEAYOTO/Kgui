package me.krunsh.kgui.api;

import java.util.Objects;
import java.util.UUID;

/** Contexte neutre d'une action enregistree. */
public final class ActionContext {
    private final UUID playerId;
    private final String menuId;
    private final String itemId;
    private final MenuArguments parameters;

    public ActionContext(UUID playerId, String menuId, String itemId, MenuArguments parameters) {
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
