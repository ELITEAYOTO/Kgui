/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.codahale.metrics.MetricRegistry
 */
package pk.ajneb97.libs.hikaricp.metrics.dropwizard;

import com.codahale.metrics.MetricRegistry;
import pk.ajneb97.libs.hikaricp.metrics.IMetricsTracker;
import pk.ajneb97.libs.hikaricp.metrics.MetricsTrackerFactory;
import pk.ajneb97.libs.hikaricp.metrics.PoolStats;
import pk.ajneb97.libs.hikaricp.metrics.dropwizard.CodaHaleMetricsTracker;

public final class CodahaleMetricsTrackerFactory
implements MetricsTrackerFactory {
    private final MetricRegistry registry;

    public CodahaleMetricsTrackerFactory(MetricRegistry registry) {
        this.registry = registry;
    }

    public MetricRegistry getRegistry() {
        return this.registry;
    }

    @Override
    public IMetricsTracker create(String poolName, PoolStats poolStats) {
        return new CodaHaleMetricsTracker(poolName, poolStats, this.registry);
    }
}

