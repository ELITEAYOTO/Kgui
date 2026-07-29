/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.micrometer.core.instrument.Counter
 *  io.micrometer.core.instrument.Gauge
 *  io.micrometer.core.instrument.Meter
 *  io.micrometer.core.instrument.MeterRegistry
 *  io.micrometer.core.instrument.Timer
 */
package pk.ajneb97.libs.hikaricp.metrics.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import pk.ajneb97.libs.hikaricp.metrics.IMetricsTracker;
import pk.ajneb97.libs.hikaricp.metrics.PoolStats;

public class MicrometerMetricsTracker
implements IMetricsTracker {
    public static final String HIKARI_METRIC_NAME_PREFIX = "hikaricp";
    private static final String METRIC_CATEGORY = "pool";
    private static final String METRIC_NAME_WAIT = "hikaricp.connections.acquire";
    private static final String METRIC_NAME_USAGE = "hikaricp.connections.usage";
    private static final String METRIC_NAME_CONNECT = "hikaricp.connections.creation";
    private static final String METRIC_NAME_TIMEOUT_RATE = "hikaricp.connections.timeout";
    private static final String METRIC_NAME_TOTAL_CONNECTIONS = "hikaricp.connections";
    private static final String METRIC_NAME_IDLE_CONNECTIONS = "hikaricp.connections.idle";
    private static final String METRIC_NAME_ACTIVE_CONNECTIONS = "hikaricp.connections.active";
    private static final String METRIC_NAME_PENDING_CONNECTIONS = "hikaricp.connections.pending";
    private static final String METRIC_NAME_MAX_CONNECTIONS = "hikaricp.connections.max";
    private static final String METRIC_NAME_MIN_CONNECTIONS = "hikaricp.connections.min";
    private final Timer connectionObtainTimer;
    private final Counter connectionTimeoutCounter;
    private final Timer connectionUsage;
    private final Timer connectionCreation;
    private final Gauge totalConnectionGauge;
    private final Gauge idleConnectionGauge;
    private final Gauge activeConnectionGauge;
    private final Gauge pendingConnectionGauge;
    private final Gauge maxConnectionGauge;
    private final Gauge minConnectionGauge;
    private final MeterRegistry meterRegistry;
    private final PoolStats poolStats;

    MicrometerMetricsTracker(String poolName, PoolStats poolStats, MeterRegistry meterRegistry) {
        this.poolStats = poolStats;
        this.meterRegistry = meterRegistry;
        this.connectionObtainTimer = Timer.builder((String)METRIC_NAME_WAIT).description("Connection acquire time").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.connectionCreation = Timer.builder((String)METRIC_NAME_CONNECT).description("Connection creation time").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.connectionUsage = Timer.builder((String)METRIC_NAME_USAGE).description("Connection usage time").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.connectionTimeoutCounter = Counter.builder((String)METRIC_NAME_TIMEOUT_RATE).description("Connection timeout total count").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.totalConnectionGauge = Gauge.builder((String)METRIC_NAME_TOTAL_CONNECTIONS, (Object)poolStats, PoolStats::getTotalConnections).description("Total connections").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.idleConnectionGauge = Gauge.builder((String)METRIC_NAME_IDLE_CONNECTIONS, (Object)poolStats, PoolStats::getIdleConnections).description("Idle connections").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.activeConnectionGauge = Gauge.builder((String)METRIC_NAME_ACTIVE_CONNECTIONS, (Object)poolStats, PoolStats::getActiveConnections).description("Active connections").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.pendingConnectionGauge = Gauge.builder((String)METRIC_NAME_PENDING_CONNECTIONS, (Object)poolStats, PoolStats::getPendingThreads).description("Pending threads").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.maxConnectionGauge = Gauge.builder((String)METRIC_NAME_MAX_CONNECTIONS, (Object)poolStats, PoolStats::getMaxConnections).description("Max connections").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
        this.minConnectionGauge = Gauge.builder((String)METRIC_NAME_MIN_CONNECTIONS, (Object)poolStats, PoolStats::getMinConnections).description("Min connections").tags(new String[]{METRIC_CATEGORY, poolName}).register(meterRegistry);
    }

    @Override
    public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
        this.connectionObtainTimer.record(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
        this.connectionUsage.record(elapsedBorrowedMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordConnectionTimeout() {
        this.connectionTimeoutCounter.increment();
    }

    @Override
    public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
        this.connectionCreation.record(connectionCreatedMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        this.meterRegistry.remove((Meter)this.connectionObtainTimer);
        this.meterRegistry.remove((Meter)this.connectionTimeoutCounter);
        this.meterRegistry.remove((Meter)this.connectionUsage);
        this.meterRegistry.remove((Meter)this.connectionCreation);
        this.meterRegistry.remove((Meter)this.totalConnectionGauge);
        this.meterRegistry.remove((Meter)this.idleConnectionGauge);
        this.meterRegistry.remove((Meter)this.activeConnectionGauge);
        this.meterRegistry.remove((Meter)this.pendingConnectionGauge);
        this.meterRegistry.remove((Meter)this.maxConnectionGauge);
        this.meterRegistry.remove((Meter)this.minConnectionGauge);
    }
}

