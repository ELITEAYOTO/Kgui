package me.krunsh.kgui.metrics;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GuiMetricsTest {
    @Test
    public void providerRenderDiffAndQueueCountersAreObservable() {
        GuiMetrics metrics = new GuiMetrics();
        metrics.providerCall("test:items", 100L);
        metrics.providerCacheHit("test:items");
        metrics.providerError("test:items");
        metrics.render(200L);
        metrics.render(8_000_000L, 27);
        metrics.menuOpen(4_000_000L, false);
        metrics.menuOpen(9_000_000L, true);
        metrics.placeholderResolutions(12);
        metrics.slotsSent(54);
        metrics.diff(3);
        metrics.refreshQueued();
        metrics.refreshCoalesced();
        metrics.refreshRejected();
        metrics.refreshExecuted();
        metrics.observeRefreshQueue(42);
        metrics.schedulerTick(500_000L);
        metrics.clickReceived();
        metrics.clickRouteRejected();
        metrics.clickAuthorityRejected();
        metrics.clickAction();
        GuiMetrics.Snapshot snapshot = metrics.snapshot();
        assertEquals(1L, snapshot.providerCalls);
        assertEquals(1L, snapshot.providerCacheHits);
        assertEquals(1L, snapshot.providerErrors);
        assertEquals(2L, snapshot.renders);
        assertEquals(27L, snapshot.renderedSlots);
        assertEquals(54L, snapshot.slotsSent);
        assertEquals(12L, snapshot.placeholderResolutions);
        assertEquals(3L, snapshot.changedSlots);
        assertEquals(1L, snapshot.refreshQueued);
        assertEquals(1L, snapshot.refreshCoalesced);
        assertEquals(1L, snapshot.refreshRejected);
        assertEquals(1L, snapshot.refreshExecuted);
        assertEquals(42L, snapshot.maxRefreshQueue);
        assertEquals(1L, snapshot.staticOpenLatency.count);
        assertEquals(1L, snapshot.dynamicOpenLatency.count);
        assertEquals(1L, snapshot.schedulerLatency.count);
        assertEquals(1L, snapshot.clicksReceived);
        assertEquals(1L, snapshot.clicksRouteRejected);
        assertEquals(1L, snapshot.clicksAuthorityRejected);
        assertEquals(1L, snapshot.clickActions);
        org.junit.Assert.assertTrue(snapshot.renderLatency.p95Nanos >= 8_000_000L);
        assertEquals(100L, snapshot.providers.get("test:items").nanos);
    }
}
