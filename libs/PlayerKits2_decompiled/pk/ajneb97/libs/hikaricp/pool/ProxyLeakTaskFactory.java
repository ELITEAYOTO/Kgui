/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.libs.hikaricp.pool;

import java.util.concurrent.ScheduledExecutorService;
import pk.ajneb97.libs.hikaricp.pool.PoolEntry;
import pk.ajneb97.libs.hikaricp.pool.ProxyLeakTask;

class ProxyLeakTaskFactory {
    private ScheduledExecutorService executorService;
    private long leakDetectionThreshold;

    ProxyLeakTaskFactory(long leakDetectionThreshold, ScheduledExecutorService executorService) {
        this.executorService = executorService;
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    ProxyLeakTask schedule(PoolEntry poolEntry) {
        return this.leakDetectionThreshold == 0L ? ProxyLeakTask.NO_LEAK : this.scheduleNewTask(poolEntry);
    }

    void updateLeakDetectionThreshold(long leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    private ProxyLeakTask scheduleNewTask(PoolEntry poolEntry) {
        ProxyLeakTask task = new ProxyLeakTask(poolEntry);
        task.schedule(this.executorService, this.leakDetectionThreshold);
        return task;
    }
}

