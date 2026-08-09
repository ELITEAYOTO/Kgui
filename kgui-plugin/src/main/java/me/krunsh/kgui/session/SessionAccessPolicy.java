package me.krunsh.kgui.session;

import java.util.UUID;

/** Validation pure partagee par le routeur de clic et ses tests de securite. */
public final class SessionAccessPolicy {
    private SessionAccessPolicy() {
    }

    public static boolean allows(UUID actor, UUID holderOwner,
                                 long holderSessionId, long holderRevision,
                                 PlayerGuiSession active, boolean sameInventory) {
        return actor != null && actor.equals(holderOwner)
            && active != null && active.isActive()
            && active.getPlayerUuid().equals(actor)
            && active.getSessionId() == holderSessionId
            && active.getRenderRevision() == holderRevision
            && sameInventory;
    }
}
