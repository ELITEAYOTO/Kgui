package me.krunsh.kgui.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/** Compteurs et histogrammes bornes, utilisables en continu sans conserver d'echantillons. */
public final class GuiMetrics {
    private final LongAdder providerCalls = new LongAdder();
    private final LongAdder providerCacheHits = new LongAdder();
    private final LongAdder providerErrors = new LongAdder();
    private final LongAdder providerNanos = new LongAdder();
    private final LongAdder renders = new LongAdder();
    private final LongAdder renderNanos = new LongAdder();
    private final LongAdder renderedSlots = new LongAdder();
    private final LongAdder slotsSent = new LongAdder();
    private final LongAdder placeholderResolutions = new LongAdder();
    private final LongAdder diffRuns = new LongAdder();
    private final LongAdder changedSlots = new LongAdder();
    private final LongAdder inventoryReopens = new LongAdder();
    private final LongAdder invalidations = new LongAdder();
    private final LongAdder refreshQueued = new LongAdder();
    private final LongAdder refreshCoalesced = new LongAdder();
    private final LongAdder refreshRejected = new LongAdder();
    private final LongAdder refreshExecuted = new LongAdder();
    private final LongAdder schedulerTicks = new LongAdder();
    private final LongAdder schedulerNanos = new LongAdder();
    private final LongAdder clicksObserved = new LongAdder();
    private final LongAdder clicksReceived = new LongAdder();
    private final LongAdder clicksRouteRejected = new LongAdder();
    private final LongAdder clicksAuthorityRejected = new LongAdder();
    private final LongAdder clicksSessionRejected = new LongAdder();
    private final LongAdder clicksItemRejected = new LongAdder();
    private final LongAdder clickActions = new LongAdder();
    private final AtomicLong maxRefreshQueue = new AtomicLong();
    private final LatencyHistogram providerLatency = new LatencyHistogram();
    private final LatencyHistogram renderLatency = new LatencyHistogram();
    private final LatencyHistogram staticOpenLatency = new LatencyHistogram();
    private final LatencyHistogram dynamicOpenLatency = new LatencyHistogram();
    private final LatencyHistogram schedulerLatency = new LatencyHistogram();
    private final ConcurrentHashMap<String, ProviderCounters> perProvider = new ConcurrentHashMap<>();

    public void providerCall(String id, long nanos) {
        long safeNanos = Math.max(0L, nanos);
        providerCalls.increment();
        providerNanos.add(safeNanos);
        providerLatency.record(safeNanos);
        perProvider.computeIfAbsent(id, ignored -> new ProviderCounters()).call(safeNanos);
    }

    public void providerCacheHit(String id) {
        providerCacheHits.increment();
        perProvider.computeIfAbsent(id, ignored -> new ProviderCounters()).cacheHit();
    }

    public void providerError(String id) {
        providerErrors.increment();
        perProvider.computeIfAbsent(id, ignored -> new ProviderCounters()).error();
    }

    public void render(long nanos) { render(nanos, 0); }

    public void render(long nanos, int slots) {
        long safeNanos = Math.max(0L, nanos);
        renders.increment();
        renderNanos.add(safeNanos);
        renderedSlots.add(Math.max(0, slots));
        renderLatency.record(safeNanos);
    }

    public void menuOpen(long nanos, boolean dynamic) {
        (dynamic ? dynamicOpenLatency : staticOpenLatency).record(Math.max(0L, nanos));
    }

    public void slotsSent(int slots) { slotsSent.add(Math.max(0, slots)); }
    public void placeholderResolutions(int count) { placeholderResolutions.add(Math.max(0, count)); }
    public void diff(int slots) { diffRuns.increment(); changedSlots.add(Math.max(0, slots)); }
    public void inventoryReopen() { inventoryReopens.increment(); }
    public void invalidation() { invalidations.increment(); }
    public void refreshQueued() { refreshQueued.increment(); }
    public void refreshCoalesced() { refreshCoalesced.increment(); }
    public void refreshRejected() { refreshRejected.increment(); }
    public void refreshExecuted() { refreshExecuted.increment(); }

    public void observeRefreshQueue(int size) {
        updateMaximum(maxRefreshQueue, Math.max(0, size));
    }

    public void schedulerTick(long nanos) {
        long safeNanos = Math.max(0L, nanos);
        schedulerTicks.increment();
        schedulerNanos.add(safeNanos);
        schedulerLatency.record(safeNanos);
    }

    public void clickObserved() { clicksObserved.increment(); }
    public void clickReceived() { clicksReceived.increment(); }
    public void clickRouteRejected() { clicksRouteRejected.increment(); }
    public void clickAuthorityRejected() { clicksAuthorityRejected.increment(); }
    public void clickSessionRejected() { clicksAuthorityRejected.increment(); clicksSessionRejected.increment(); }
    public void clickItemRejected() { clicksAuthorityRejected.increment(); clicksItemRejected.increment(); }
    public void clickAction() { clickActions.increment(); }

    public void removeProvider(String id) { if (id != null) perProvider.remove(id); }

    public Snapshot snapshot() {
        Map<String, ProviderSnapshot> providers = new LinkedHashMap<>();
        for (Map.Entry<String, ProviderCounters> entry : perProvider.entrySet()) {
            providers.put(entry.getKey(), entry.getValue().snapshot());
        }
        return new Snapshot(providerCalls.sum(), providerCacheHits.sum(), providerErrors.sum(),
            providerNanos.sum(), renders.sum(), renderNanos.sum(), renderedSlots.sum(), slotsSent.sum(),
            placeholderResolutions.sum(), diffRuns.sum(), changedSlots.sum(), inventoryReopens.sum(),
            invalidations.sum(), refreshQueued.sum(), refreshCoalesced.sum(), refreshRejected.sum(),
            refreshExecuted.sum(), schedulerTicks.sum(), schedulerNanos.sum(), maxRefreshQueue.get(),
            clicksObserved.sum(), clicksReceived.sum(), clicksRouteRejected.sum(),
            clicksAuthorityRejected.sum(), clicksSessionRejected.sum(), clicksItemRejected.sum(),
            clickActions.sum(),
            providerLatency.snapshot(), renderLatency.snapshot(), staticOpenLatency.snapshot(),
            dynamicOpenLatency.snapshot(), schedulerLatency.snapshot(), providers);
    }

    private static void updateMaximum(AtomicLong maximum, long value) {
        long current = maximum.get();
        while (value > current && !maximum.compareAndSet(current, value)) current = maximum.get();
    }

    public static final class Snapshot {
        public final long providerCalls, providerCacheHits, providerErrors, providerNanos;
        public final long renders, renderNanos, renderedSlots, slotsSent, placeholderResolutions;
        public final long diffRuns, changedSlots, inventoryReopens;
        public final long invalidations, refreshQueued, refreshCoalesced, refreshRejected, refreshExecuted;
        public final long schedulerTicks, schedulerNanos, maxRefreshQueue;
        public final long clicksObserved, clicksReceived, clicksRouteRejected, clicksAuthorityRejected;
        public final long clicksSessionRejected, clicksItemRejected, clickActions;
        public final LatencySnapshot providerLatency, renderLatency, staticOpenLatency;
        public final LatencySnapshot dynamicOpenLatency, schedulerLatency;
        public final Map<String, ProviderSnapshot> providers;

        private Snapshot(long providerCalls, long providerCacheHits, long providerErrors, long providerNanos,
                         long renders, long renderNanos, long renderedSlots, long slotsSent,
                         long placeholderResolutions, long diffRuns, long changedSlots, long inventoryReopens,
                         long invalidations, long refreshQueued, long refreshCoalesced, long refreshRejected,
                         long refreshExecuted, long schedulerTicks, long schedulerNanos, long maxRefreshQueue,
                         long clicksObserved, long clicksReceived, long clicksRouteRejected, long clicksAuthorityRejected,
                         long clicksSessionRejected, long clicksItemRejected, long clickActions,
                         LatencySnapshot providerLatency, LatencySnapshot renderLatency,
                         LatencySnapshot staticOpenLatency, LatencySnapshot dynamicOpenLatency,
                         LatencySnapshot schedulerLatency, Map<String, ProviderSnapshot> providers) {
            this.providerCalls = providerCalls;
            this.providerCacheHits = providerCacheHits;
            this.providerErrors = providerErrors;
            this.providerNanos = providerNanos;
            this.renders = renders;
            this.renderNanos = renderNanos;
            this.renderedSlots = renderedSlots;
            this.slotsSent = slotsSent;
            this.placeholderResolutions = placeholderResolutions;
            this.diffRuns = diffRuns;
            this.changedSlots = changedSlots;
            this.inventoryReopens = inventoryReopens;
            this.invalidations = invalidations;
            this.refreshQueued = refreshQueued;
            this.refreshCoalesced = refreshCoalesced;
            this.refreshRejected = refreshRejected;
            this.refreshExecuted = refreshExecuted;
            this.schedulerTicks = schedulerTicks;
            this.schedulerNanos = schedulerNanos;
            this.maxRefreshQueue = maxRefreshQueue;
            this.clicksObserved = clicksObserved;
            this.clicksReceived = clicksReceived;
            this.clicksRouteRejected = clicksRouteRejected;
            this.clicksAuthorityRejected = clicksAuthorityRejected;
            this.clicksSessionRejected = clicksSessionRejected;
            this.clicksItemRejected = clicksItemRejected;
            this.clickActions = clickActions;
            this.providerLatency = providerLatency;
            this.renderLatency = renderLatency;
            this.staticOpenLatency = staticOpenLatency;
            this.dynamicOpenLatency = dynamicOpenLatency;
            this.schedulerLatency = schedulerLatency;
            this.providers = Collections.unmodifiableMap(providers);
        }
    }

    public static final class LatencySnapshot {
        public final long count, p50Nanos, p95Nanos, p99Nanos, maxNanos;

        private LatencySnapshot(long count, long p50Nanos, long p95Nanos, long p99Nanos, long maxNanos) {
            this.count = count;
            this.p50Nanos = p50Nanos;
            this.p95Nanos = p95Nanos;
            this.p99Nanos = p99Nanos;
            this.maxNanos = maxNanos;
        }
    }

    public static final class ProviderSnapshot {
        public final long calls, cacheHits, errors, nanos;
        private ProviderSnapshot(long calls, long cacheHits, long errors, long nanos) {
            this.calls = calls; this.cacheHits = cacheHits; this.errors = errors; this.nanos = nanos;
        }
    }

    /** Histogramme logarithmique de 64 compteurs : memoire fixe, aucune retention d'echantillon. */
    private static final class LatencyHistogram {
        private final AtomicLongArray buckets = new AtomicLongArray(64);
        private final LongAdder count = new LongAdder();
        private final AtomicLong maximum = new AtomicLong();

        void record(long nanos) {
            long safeNanos = Math.max(0L, nanos);
            int bucket = safeNanos == 0L ? 0 : 64 - Long.numberOfLeadingZeros(safeNanos);
            buckets.incrementAndGet(Math.min(63, bucket));
            count.increment();
            updateMaximum(maximum, safeNanos);
        }

        LatencySnapshot snapshot() {
            long samples = count.sum();
            long max = maximum.get();
            return new LatencySnapshot(samples, Math.min(max, percentile(samples, 50)),
                Math.min(max, percentile(samples, 95)), Math.min(max, percentile(samples, 99)), max);
        }

        private long percentile(long samples, int percentile) {
            if (samples <= 0L) return 0L;
            long target = (samples * percentile + 99L) / 100L;
            long seen = 0L;
            for (int bucket = 0; bucket < buckets.length(); bucket++) {
                seen += buckets.get(bucket);
                if (seen >= target) return bucket == 0 ? 0L : upperBound(bucket);
            }
            return maximum.get();
        }

        private static long upperBound(int bucket) {
            if (bucket >= 63) return Long.MAX_VALUE;
            return (1L << bucket) - 1L;
        }
    }

    private static final class ProviderCounters {
        private final LongAdder calls = new LongAdder(), cacheHits = new LongAdder();
        private final LongAdder errors = new LongAdder(), nanos = new LongAdder();
        void call(long time) { calls.increment(); nanos.add(Math.max(0L, time)); }
        void cacheHit() { cacheHits.increment(); }
        void error() { errors.increment(); }
        ProviderSnapshot snapshot() { return new ProviderSnapshot(calls.sum(), cacheHits.sum(), errors.sum(), nanos.sum()); }
    }
}
