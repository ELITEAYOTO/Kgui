package me.krunsh.kgui.session;

import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Registre borne : au plus une session active par joueur. */
public final class SessionRegistry {
    private final AtomicLong sequence = new AtomicLong();
    private final Map<UUID, PlayerGuiSession> sessions = new HashMap<>();

    public synchronized PlayerGuiSession create(UUID playerId, String menuId, int page, Inventory inventory) {
        return new PlayerGuiSession(sequence.incrementAndGet(), playerId, menuId, page, inventory);
    }

    public synchronized PlayerGuiSession activate(PlayerGuiSession session) {
        PlayerGuiSession previous = sessions.put(session.getPlayerUuid(), session);
        if (previous != null && previous != session) {
            previous.close(CloseReason.REPLACED);
        }
        return previous;
    }

    public synchronized PlayerGuiSession get(UUID playerId) {
        PlayerGuiSession session = sessions.get(playerId);
        return session != null && session.isActive() ? session : null;
    }

    public synchronized PlayerGuiSession resolve(SessionToken token) {
        if (token == null) return null;
        PlayerGuiSession session = get(token.getPlayerId());
        return session != null && session.getSessionId() == token.getSessionId() ? session : null;
    }

    public synchronized boolean isActive(SessionToken token) {
        return resolve(token) != null;
    }

    public synchronized PlayerGuiSession close(UUID playerId, long sessionId, CloseReason reason) {
        PlayerGuiSession current = sessions.get(playerId);
        if (current == null || current.getSessionId() != sessionId) return null;
        sessions.remove(playerId);
        current.close(reason);
        return current;
    }

    public synchronized PlayerGuiSession close(UUID playerId, CloseReason reason) {
        PlayerGuiSession current = sessions.remove(playerId);
        if (current != null) current.close(reason);
        return current;
    }

    public synchronized Collection<PlayerGuiSession> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(sessions.values()));
    }

    public synchronized void closeAll(CloseReason reason) {
        for (PlayerGuiSession session : new ArrayList<>(sessions.values())) {
            session.close(reason);
        }
        sessions.clear();
    }

    public synchronized int size() {
        return sessions.size();
    }
}
