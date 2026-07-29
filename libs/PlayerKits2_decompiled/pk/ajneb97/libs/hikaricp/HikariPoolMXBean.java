/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.libs.hikaricp;

public interface HikariPoolMXBean {
    public int getIdleConnections();

    public int getActiveConnections();

    public int getTotalConnections();

    public int getThreadsAwaitingConnection();

    public void softEvictConnections();

    public void suspendPool();

    public void resumePool();
}

