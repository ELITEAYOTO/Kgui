package me.krunsh.kgui.session;

import java.util.Objects;
import java.util.UUID;

/** Jeton non forgeable cote client qui identifie une ouverture precise. */
public final class SessionToken {
    private final UUID playerId;
    private final long sessionId;

    public SessionToken(UUID playerId, long sessionId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.sessionId = sessionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getSessionId() {
        return sessionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SessionToken)) return false;
        SessionToken that = (SessionToken) other;
        return sessionId == that.sessionId && playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, sessionId);
    }

    @Override
    public String toString() {
        return playerId + ":" + sessionId;
    }
}
