package me.krunsh.kgui.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Regroupe les demandes de refresh d'un meme menu pendant un tick serveur.
 * Cette classe est utilisee uniquement depuis le thread principal Bukkit.
 */
public final class RefreshRequestGate {

    private final Map<UUID, String> pendingMenus = new HashMap<>();

    public boolean trySchedule(UUID playerUuid, String menuId) {
        if (pendingMenus.containsKey(playerUuid)) {
            return false;
        }
        pendingMenus.put(playerUuid, menuId);
        return true;
    }

    public void complete(UUID playerUuid, String menuId) {
        String pendingMenu = pendingMenus.get(playerUuid);
        if (menuId.equals(pendingMenu)) {
            pendingMenus.remove(playerUuid);
        }
    }

    public void clear(UUID playerUuid) {
        pendingMenus.remove(playerUuid);
    }
}
