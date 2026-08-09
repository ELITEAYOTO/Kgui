package me.krunsh.kgui.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Compteurs peu coûteux pour providers, rendu, diff et scheduler. */
public final class GuiMetrics {
    private final LongAdder providerCalls = new LongAdder();
    private final LongAdder providerCacheHits = new LongAdder();
    private final LongAdder providerErrors = new LongAdder();
    private final LongAdder providerNanos = new LongAdder();
    private final LongAdder renders = new LongAdder();
    private final LongAdder renderNanos = new LongAdder();
    private final LongAdder diffRuns = new LongAdder();
    private final LongAdder changedSlots = new LongAdder();
    private final LongAdder inventoryReopens = new LongAdder();
    private final LongAdder invalidations = new LongAdder();
    private final LongAdder refreshQueued = new LongAdder();
    private final LongAdder refreshCoalesced = new LongAdder();
    private final ConcurrentHashMap<String, ProviderCounters> perProvider = new ConcurrentHashMap<>();

    public void providerCall(String id, long nanos) {
        providerCalls.increment(); providerNanos.add(Math.max(0L, nanos));
        perProvider.computeIfAbsent(id, ignored -> new ProviderCounters()).call(nanos);
    }
    public void providerCacheHit(String id) {
        providerCacheHits.increment();
        perProvider.computeIfAbsent(id, ignored -> new ProviderCounters()).cacheHit();
    }
    public void providerError(String id) {
        providerErrors.increment();
        perProvider.computeIfAbsent(id, ignored -> new ProviderCounters()).error();
    }
    public void render(long nanos) { renders.increment(); renderNanos.add(Math.max(0L, nanos)); }
    public void diff(int slots) { diffRuns.increment(); changedSlots.add(Math.max(0, slots)); }
    public void inventoryReopen() { inventoryReopens.increment(); }
    public void invalidation() { invalidations.increment(); }
    public void refreshQueued() { refreshQueued.increment(); }
    public void refreshCoalesced() { refreshCoalesced.increment(); }
    public void removeProvider(String id) { if (id != null) perProvider.remove(id); }

    public Snapshot snapshot() {
        Map<String, ProviderSnapshot> providers = new LinkedHashMap<>();
        for (Map.Entry<String, ProviderCounters> entry : perProvider.entrySet()) {
            providers.put(entry.getKey(), entry.getValue().snapshot());
        }
        return new Snapshot(providerCalls.sum(), providerCacheHits.sum(), providerErrors.sum(),
            providerNanos.sum(), renders.sum(), renderNanos.sum(), diffRuns.sum(), changedSlots.sum(),
            inventoryReopens.sum(), invalidations.sum(), refreshQueued.sum(), refreshCoalesced.sum(), providers);
    }

    public static final class Snapshot {
        public final long providerCalls, providerCacheHits, providerErrors, providerNanos;
        public final long renders, renderNanos, diffRuns, changedSlots, inventoryReopens;
        public final long invalidations, refreshQueued, refreshCoalesced;
        public final Map<String, ProviderSnapshot> providers;
        private Snapshot(long providerCalls, long providerCacheHits, long providerErrors, long providerNanos,
                         long renders, long renderNanos, long diffRuns, long changedSlots, long inventoryReopens,
                         long invalidations, long refreshQueued, long refreshCoalesced,
                         Map<String, ProviderSnapshot> providers) {
            this.providerCalls = providerCalls; this.providerCacheHits = providerCacheHits;
            this.providerErrors = providerErrors; this.providerNanos = providerNanos;
            this.renders = renders; this.renderNanos = renderNanos; this.diffRuns = diffRuns;
            this.changedSlots = changedSlots; this.inventoryReopens = inventoryReopens;
            this.invalidations = invalidations; this.refreshQueued = refreshQueued;
            this.refreshCoalesced = refreshCoalesced;
            this.providers = Collections.unmodifiableMap(providers);
        }
    }

    public static final class ProviderSnapshot {
        public final long calls, cacheHits, errors, nanos;
        private ProviderSnapshot(long calls, long cacheHits, long errors, long nanos) {
            this.calls = calls; this.cacheHits = cacheHits; this.errors = errors; this.nanos = nanos;
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
