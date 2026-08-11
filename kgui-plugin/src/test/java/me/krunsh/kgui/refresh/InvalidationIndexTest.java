package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.Collections;
import java.util.UUID;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.session.SessionToken;
import org.junit.Test;

public class InvalidationIndexTest {
    @Test
    public void providerInvalidationTargetsOnlyMatchingEventSessions() {
        InvalidationIndex index = new InvalidationIndex();
        SessionToken first = token();
        SessionToken second = token();
        SessionToken manual = token();
        index.register(first, "members", "kfaction:members", true);
        index.register(second, "jobs", "kjobs:levels", true);
        index.register(manual, "admin", "kfaction:members", false);
        Set<SessionToken> targets = index.resolve(
            InvalidationRequest.provider("kfaction:members", "member-changed"));
        assertEquals(1, targets.size());
        assertTrue(targets.contains(first));
    }

    @Test
    public void replacementAndPlayerMenuScopeCannotReachStaleSession() {
        InvalidationIndex index = new InvalidationIndex();
        UUID player = UUID.randomUUID();
        SessionToken old = new SessionToken(player, 1L);
        SessionToken current = new SessionToken(player, 2L);
        index.register(old, "old", null, true);
        index.register(current, "current", null, true);
        assertTrue(index.resolve(InvalidationRequest.menu("old", "change")).isEmpty());
        assertEquals(Collections.singleton(current), index.resolve(
            InvalidationRequest.playerMenu(player, "current", null, "change")));
        index.unregister(old);
        assertEquals(1, index.size());
    }

    private static SessionToken token() {
        return new SessionToken(UUID.randomUUID(), 1L);
    }
}
