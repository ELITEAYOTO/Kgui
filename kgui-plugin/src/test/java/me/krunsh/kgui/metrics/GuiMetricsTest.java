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
        metrics.diff(3);
        metrics.refreshQueued();
        metrics.refreshCoalesced();
        GuiMetrics.Snapshot snapshot = metrics.snapshot();
        assertEquals(1L, snapshot.providerCalls);
        assertEquals(1L, snapshot.providerCacheHits);
        assertEquals(1L, snapshot.providerErrors);
        assertEquals(1L, snapshot.renders);
        assertEquals(3L, snapshot.changedSlots);
        assertEquals(1L, snapshot.refreshQueued);
        assertEquals(1L, snapshot.refreshCoalesced);
        assertEquals(100L, snapshot.providers.get("test:items").nanos);
    }
}
