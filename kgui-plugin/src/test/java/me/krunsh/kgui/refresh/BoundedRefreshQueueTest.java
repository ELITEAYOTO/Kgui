package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import me.krunsh.kgui.session.SessionToken;
import org.junit.Test;

public class BoundedRefreshQueueTest {
    @Test
    public void coalescesDirtyFlagsItemsAndHighestPriority() {
        BoundedRefreshQueue queue = new BoundedRefreshQueue(16);
        SessionToken token = token();
        assertEquals(BoundedRefreshQueue.OfferResult.QUEUED, queue.offer(
            new RefreshRequest(token, RefreshPriority.PERIODIC, true, false, null)));
        assertEquals(BoundedRefreshQueue.OfferResult.COALESCED, queue.offer(
            new RefreshRequest(token, RefreshPriority.INTERACTION, false, true,
                Collections.singleton("member-1"))));
        RefreshRequest request = queue.drain(1).get(0);
        assertEquals(RefreshPriority.INTERACTION, request.getPriority());
        assertTrue(request.isProviderDirty());
        assertTrue(request.isPlaceholdersDirty());
        assertTrue(request.getItemIds().contains("member-1"));
    }

    @Test
    public void drainsInteractionBeforePeriodicRegardlessOfInsertionOrder() {
        BoundedRefreshQueue queue = new BoundedRefreshQueue(16);
        SessionToken periodic = token();
        SessionToken interaction = token();
        queue.offer(new RefreshRequest(periodic, RefreshPriority.PERIODIC, true, true, null));
        queue.offer(new RefreshRequest(interaction, RefreshPriority.INTERACTION, false, true, null));
        List<RefreshRequest> drained = queue.drain(2);
        assertEquals(interaction, drained.get(0).getToken());
        assertEquals(periodic, drained.get(1).getToken());
    }

    @Test
    public void highPriorityWorkCanEvictPeriodicAtCapacity() {
        BoundedRefreshQueue queue = new BoundedRefreshQueue(16);
        for (int index = 0; index < 16; index++) {
            queue.offer(new RefreshRequest(token(), RefreshPriority.PERIODIC, true, true, null));
        }
        assertEquals(BoundedRefreshQueue.OfferResult.QUEUED, queue.offer(
            new RefreshRequest(token(), RefreshPriority.INTERACTION, false, true, null)));
        assertEquals(16, queue.size());
    }

    private static SessionToken token() { return new SessionToken(UUID.randomUUID(), 1L); }
}
