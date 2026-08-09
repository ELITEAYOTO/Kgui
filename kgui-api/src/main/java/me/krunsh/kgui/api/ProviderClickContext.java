package me.krunsh.kgui.api;

import java.util.Objects;
import java.util.UUID;

/** Contexte signe logiquement par itemId/revision pour rejeter les clics obsoletes. */
public final class ProviderClickContext {
    private final UUID playerId;
    private final String menuId;
    private final String providerId;
    private final String itemId;
    private final long renderedRevision;
    private final int rawSlot;
    private final ProviderClickType clickType;
    private final MenuArguments arguments;

    public ProviderClickContext(UUID playerId, String menuId, String providerId, String itemId,
                                long renderedRevision, int rawSlot, ProviderClickType clickType,
                                MenuArguments arguments) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.menuId = MenuOpenRequest.requireId(menuId, "menuId");
        this.providerId = MenuOpenRequest.requireId(providerId, "providerId");
        this.itemId = MenuOpenRequest.requireId(itemId, "itemId");
        this.renderedRevision = renderedRevision;
        this.rawSlot = rawSlot;
        this.clickType = clickType == null ? ProviderClickType.UNKNOWN : clickType;
        this.arguments = arguments == null ? MenuArguments.empty() : arguments;
    }

    public UUID getPlayerId() { return playerId; }
    public String getMenuId() { return menuId; }
    public String getProviderId() { return providerId; }
    public String getItemId() { return itemId; }
    public long getRenderedRevision() { return renderedRevision; }
    public int getRawSlot() { return rawSlot; }
    public ProviderClickType getClickType() { return clickType; }
    public MenuArguments getArguments() { return arguments; }
}
