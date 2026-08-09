package me.krunsh.kgui.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Regroupe les demandes de refresh d'un meme menu pendant un tick serveur.
 * Cette classe est utilisee uniquement depuis le thread principal Bukkit.
 */
public final class RefreshRequestGate {

    private final Map<UUID, Request> pendingMenus = new HashMap<>();

    public boolean trySchedule(UUID playerUuid, String menuId) {
        return trySchedule(playerUuid, menuId, 0L);
    }

    public boolean trySchedule(UUID playerUuid, String menuId, long sessionId) {
        if (pendingMenus.containsKey(playerUuid)) {
            return false;
        }
        pendingMenus.put(playerUuid, new Request(menuId, sessionId));
        return true;
    }

    public void complete(UUID playerUuid, String menuId) {
        complete(playerUuid, menuId, 0L);
    }

    public void complete(UUID playerUuid, String menuId, long sessionId) {
        Request pending = pendingMenus.get(playerUuid);
        if (pending != null && pending.sessionId == sessionId && menuId.equals(pending.menuId)) {
            pendingMenus.remove(playerUuid);
        }
    }

    public void clear(UUID playerUuid) {
        pendingMenus.remove(playerUuid);
    }

    private static final class Request {
        private final String menuId;
        private final long sessionId;

        private Request(String menuId, long sessionId) {
            this.menuId = menuId;
            this.sessionId = sessionId;
        }
    }
}
