package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.krunsh.kgui.session.SessionToken;
import org.junit.Test;

public class RefreshSchedulerPhaseTest {
    @Test
    public void initialPeriodicDeadlinesAreBoundedAndDistributed() {
        Set<Integer> phases = new HashSet<Integer>();
        for (int index = 0; index < 700; index++) {
            SessionToken token = new SessionToken(new UUID(index, index * 31L + 7L), index + 1L);
            int delay = RefreshScheduler.initialDelay(token, 100);
            assertTrue(delay >= 1 && delay <= 100);
            phases.add(delay);
        }
        assertTrue("expected periodic work to use many phases", phases.size() >= 50);
    }
}
