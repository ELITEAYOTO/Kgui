package me.krunsh.kgui.session;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;

public class SessionRegistryTest {
    @Test
    public void tenThousandOpenCloseCyclesRetainNoPlayerState() {
        SessionRegistry registry = new SessionRegistry();
        UUID player = UUID.randomUUID();

        for (int index = 0; index < 10_000; index++) {
            PlayerGuiSession session = registry.create(player, "menu", 1, null);
            session.getHistory().push("previous");
            session.getRuntimeArguments().put("menu", Collections.singletonMap("id", "42"));
            session.getPlaceholderCache().put("%value%", "cached");
            session.getCooldowns().put("menu:item", 1L);
            registry.activate(session);

            assertNotNull(registry.close(player, session.getSessionId(), CloseReason.PLAYER_CLOSE));
            assertFalse(session.isActive());
            assertEquals(0, session.retainedStateSize());
            assertNull(registry.close(player, session.getSessionId(), CloseReason.PLAYER_CLOSE));
        }

        assertEquals(0, registry.size());
        assertNull(registry.get(player));
    }

    @Test
    public void closeCancelsTrackedDelayedWorkAndInvalidatesToken() {
        SessionRegistry registry = new SessionRegistry();
        UUID player = UUID.randomUUID();
        PlayerGuiSession session = registry.create(player, "menu", 1, null);
        FakeTask task = new FakeTask();
        registry.activate(session);
        assertTrue(session.trackTask(task));

        registry.close(player, session.getSessionId(), CloseReason.QUIT);

        assertTrue(task.cancelled);
        assertFalse(registry.isActive(session.getToken()));
        assertFalse(session.close(CloseReason.DISABLE));
    }

    private static final class FakeTask implements BukkitTask {
        private boolean cancelled;

        @Override public int getTaskId() { return 1; }
        @Override public Plugin getOwner() { return null; }
        @Override public boolean isSync() { return true; }
        @Override public void cancel() { cancelled = true; }
    }
}
