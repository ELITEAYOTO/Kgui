/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.prometheus.client.Collector
 *  io.prometheus.client.CollectorRegistry
 */
package pk.ajneb97.libs.hikaricp.metrics.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import pk.ajneb97.libs.hikaricp.metrics.IMetricsTracker;
import pk.ajneb97.libs.hikaricp.metrics.MetricsTrackerFactory;
import pk.ajneb97.libs.hikaricp.metrics.PoolStats;
import pk.ajneb97.libs.hikaricp.metrics.prometheus.HikariCPCollector;
import pk.ajneb97.libs.hikaricp.metrics.prometheus.PrometheusHistogramMetricsTracker;
import pk.ajneb97.libs.hikaricp.metrics.prometheus.PrometheusMetricsTrackerFactory;

public class PrometheusHistogramMetricsTrackerFactory
implements MetricsTrackerFactory {
    private static final Map<CollectorRegistry, PrometheusMetricsTrackerFactory.RegistrationStatus> registrationStatuses = new ConcurrentHashMap<CollectorRegistry, PrometheusMetricsTrackerFactory.RegistrationStatus>();
    private final HikariCPCollector collector = new HikariCPCollector();
    private final CollectorRegistry collectorRegistry;

    public PrometheusHistogramMetricsTrackerFactory() {
        this(CollectorRegistry.defaultRegistry);
    }

    public PrometheusHistogramMetricsTrackerFactory(CollectorRegistry collectorRegistry) {
        this.collectorRegistry = collectorRegistry;
    }

    @Override
    public IMetricsTracker create(String poolName, PoolStats poolStats) {
        this.registerCollector(this.collector, this.collectorRegistry);
        this.collector.add(poolName, poolStats);
        return new PrometheusHistogramMetricsTracker(poolName, this.collectorRegistry, this.collector);
    }

    private void registerCollector(Collector collector, CollectorRegistry collectorRegistry) {
        if (registrationStatuses.putIfAbsent(collectorRegistry, PrometheusMetricsTrackerFactory.RegistrationStatus.REGISTERED) == null) {
            collector.register(collectorRegistry);
        }
    }
}

