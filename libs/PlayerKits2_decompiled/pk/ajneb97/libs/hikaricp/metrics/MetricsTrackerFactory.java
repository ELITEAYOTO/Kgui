/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.libs.hikaricp.metrics;

import pk.ajneb97.libs.hikaricp.metrics.IMetricsTracker;
import pk.ajneb97.libs.hikaricp.metrics.PoolStats;

public interface MetricsTrackerFactory {
    public IMetricsTracker create(String var1, PoolStats var2);
}

