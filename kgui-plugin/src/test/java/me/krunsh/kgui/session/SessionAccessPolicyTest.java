package me.krunsh.kgui.session;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionAccessPolicyTest {
    @Test
    public void acceptsOnlyOwnerCurrentSessionRevisionAndInventory() {
        UUID owner = UUID.randomUUID();
        PlayerGuiSession session = new PlayerGuiSession(7L, owner, "menu", 1, null);
        long revision = session.nextRenderRevision();

        assertTrue(SessionAccessPolicy.allows(owner, owner, 7L, revision, session, true));
        assertFalse(SessionAccessPolicy.allows(UUID.randomUUID(), owner, 7L, revision, session, true));
        assertFalse(SessionAccessPolicy.allows(owner, UUID.randomUUID(), 7L, revision, session, true));
        assertFalse(SessionAccessPolicy.allows(owner, owner, 6L, revision, session, true));
        assertFalse(SessionAccessPolicy.allows(owner, owner, 7L, revision + 1, session, true));
        assertFalse(SessionAccessPolicy.allows(owner, owner, 7L, revision, session, false));

        session.close(CloseReason.PLAYER_CLOSE);
        assertFalse(SessionAccessPolicy.allows(owner, owner, 7L, revision, session, true));
    }
}
