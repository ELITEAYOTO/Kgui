/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.micrometer.core.instrument.MeterRegistry
 */
package pk.ajneb97.libs.hikaricp.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import pk.ajneb97.libs.hikaricp.metrics.IMetricsTracker;
import pk.ajneb97.libs.hikaricp.metrics.MetricsTrackerFactory;
import pk.ajneb97.libs.hikaricp.metrics.PoolStats;
import pk.ajneb97.libs.hikaricp.metrics.micrometer.MicrometerMetricsTracker;

public class MicrometerMetricsTrackerFactory
implements MetricsTrackerFactory {
    private final MeterRegistry registry;

    public MicrometerMetricsTrackerFactory(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public IMetricsTracker create(String poolName, PoolStats poolStats) {
        return new MicrometerMetricsTracker(poolName, poolStats, this.registry);
    }
}

