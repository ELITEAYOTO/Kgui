package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.provider.ProviderSnapshot;
import me.krunsh.kgui.provider.ProviderSnapshotCache;
import me.krunsh.kgui.provider.ProviderSubject;
import me.krunsh.kgui.render.RenderFrame;
import me.krunsh.kgui.render.RenderedSlot;
import me.krunsh.kgui.session.CloseReason;
import me.krunsh.kgui.session.PlayerGuiSession;
import me.krunsh.kgui.session.SessionRegistry;
import me.krunsh.kgui.session.SessionToken;
import org.junit.Test;

/** Profil reproductible du coeur Kgui, sans serveur ni simulation trompeuse de Bukkit. */
public class KguiCoreLoadProfileTest {
    private static final int[] LEVELS = {20, 100, 300, 400, 700};

    @Test
    public void sessionIndexQueueAndCacheRemainBoundedAtReleaseLevels() {
        for (int level : LEVELS) profile(level);
    }

    private static void profile(int level) {
        SessionRegistry sessions = new SessionRegistry();
        InvalidationIndex index = new InvalidationIndex();
        BoundedRefreshQueue queue = new BoundedRefreshQueue(2048);
        ProviderSnapshotCache cache = new ProviderSnapshotCache(2048);
        long[] registerNanos = new long[level];
        long[] enqueueNanos = new long[level];
        long heapBefore = usedHeap();

        for (int position = 0; position < level; position++) {
            UUID playerId = new UUID(0x4b475549L, position + 1L);
            PlayerGuiSession session = sessions.create(playerId, "load_menu", 1, null);
            session.setFrame(new RenderFrame(1L, new RenderedSlot[54]));
            sessions.activate(session);
            SessionToken token = session.getToken();

            long started = System.nanoTime();
            index.register(token, "load_menu", "load:provider", true);
            registerNanos[position] = System.nanoTime() - started;

            started = System.nanoTime();
            assertEquals(BoundedRefreshQueue.OfferResult.QUEUED, queue.offer(new RefreshRequest(
                token, RefreshPriority.INVALIDATION, true, true, null)));
            enqueueNanos[position] = System.nanoTime() - started;
            assertEquals(BoundedRefreshQueue.OfferResult.COALESCED, queue.offer(new RefreshRequest(
                token, RefreshPriority.INTERACTION, false, false, Collections.singleton("slot"))));

            ProviderSubject subject = new ProviderSubject("load:provider", 1L, playerId,
                "load_menu", MenuArguments.empty());
            cache.put(new ProviderSnapshot(subject, 1L, 0, 45, 0, Collections.emptyList()));
        }

        assertEquals(level, sessions.size());
        assertEquals(level, index.size());
        assertEquals(level, queue.size());
        assertEquals(level, cache.size());
        assertEquals(level, index.resolve(InvalidationRequest.all("lot8-load-profile")).size());

        long allocated = Math.max(0L, usedHeap() - heapBefore);
        System.out.println("KGUI_CORE_LOAD level=" + level
            + " register.p50_us=" + micros(percentile(registerNanos, 50))
            + " register.p95_us=" + micros(percentile(registerNanos, 95))
            + " register.p99_us=" + micros(percentile(registerNanos, 99))
            + " enqueue.p50_us=" + micros(percentile(enqueueNanos, 50))
            + " enqueue.p95_us=" + micros(percentile(enqueueNanos, 95))
            + " enqueue.p99_us=" + micros(percentile(enqueueNanos, 99))
            + " heap.delta_bytes=" + allocated
            + " sessions=" + sessions.size() + " queue=" + queue.size() + " cache=" + cache.size());

        sessions.closeAll(CloseReason.DISABLE);
        index.clear();
        queue.clear();
        cache.clear();
        assertEquals(0, sessions.size());
        assertEquals(0, index.size());
        assertEquals(0, queue.size());
        assertEquals(0, cache.size());
    }

    private static long percentile(long[] values, int percentile) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        int index = Math.max(0, (int) Math.ceil(copy.length * percentile / 100.0D) - 1);
        return copy[index];
    }

    private static String micros(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000.0D);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
