package me.krunsh.kgui.provider;

import java.util.UUID;
import java.util.logging.Level;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.ContentRequest;
import me.krunsh.kgui.api.ContentSnapshot;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.api.ProviderClickContext;
import me.krunsh.kgui.api.ProviderClickResult;
import me.krunsh.kgui.api.ProviderClickType;
import me.krunsh.kgui.metrics.GuiMetrics;
import me.krunsh.kgui.render.ClickBinding;
import me.krunsh.kgui.render.GuiClick;
import me.krunsh.kgui.service.KguiApiProvider;
import me.krunsh.kgui.session.PlayerGuiSession;
import org.bukkit.entity.Player;

/** Exécute le contrat ContentProvider V2, valide et met en cache ses snapshots. */
public final class ProviderEngine implements AutoCloseable {
    private static final long SLOW_WARNING_INTERVAL_NANOS = 30_000_000_000L;
    private final Kgui plugin;
    private final KguiApiProvider registry;
    private final ProviderSnapshotCache cache;
    private final GuiMetrics metrics;
    private final long slowWarningNanos;
    private final ProviderWarningGate warningGate = new ProviderWarningGate(SLOW_WARNING_INTERVAL_NANOS);
    private final ProviderWarningGate errorWarningGate = new ProviderWarningGate(SLOW_WARNING_INTERVAL_NANOS);

    public ProviderEngine(Kgui plugin, KguiApiProvider registry, GuiMetrics metrics,
                          int maxCacheEntries, long slowWarningNanos) {
        this.plugin = plugin;
        this.registry = registry;
        this.metrics = metrics;
        this.cache = new ProviderSnapshotCache(maxCacheEntries);
        this.slowWarningNanos = Math.max(0L, slowWarningNanos);
    }

    public ProviderSnapshot load(UUID playerId, String menuId, String providerId,
                                 MenuArguments arguments, int offset, int limit,
                                 ProviderSnapshot previous, boolean force) {
        RegisteredContentProvider registration = registry.resolveProvider(providerId);
        if (registration == null || !registration.isActive()) return null;
        ProviderSubject subject = new ProviderSubject(registration.getId(), registration.getGeneration(),
            playerId, menuId, arguments);

        if (!force) {
            ProviderSnapshot cached = cache.get(subject, offset, limit);
            if (cached != null) {
                metrics.providerCacheHit(registration.getId());
                return cached;
            }
        }

        long knownRevision = cache.knownRevision(subject);
        if (previous != null && previous.getSubject().equals(subject)) {
            knownRevision = Math.max(knownRevision, previous.getRevision());
        }

        long started = System.nanoTime();
        ContentSnapshot returned;
        try {
            returned = registration.getProvider().getContent(new ContentRequest(
                playerId, menuId, registration.getId(), offset, limit, knownRevision, arguments));
        } catch (Throwable error) {
            rethrowFatal(error);
            long elapsed = System.nanoTime() - started;
            metrics.providerCall(registration.getId(), elapsed);
            metrics.providerError(registration.getId());
            if (errorWarningGate.tryAcquire(registration.getId(), System.nanoTime())) {
                plugin.getLogger().log(Level.WARNING, "Provider " + registration.getId() + " failed", error);
            }
            return fallback(previous, subject, offset, limit);
        }
        long elapsed = System.nanoTime() - started;
        metrics.providerCall(registration.getId(), elapsed);
        if (slowWarningNanos > 0L && elapsed >= slowWarningNanos
                && warningGate.tryAcquire(registration.getId(), System.nanoTime())) {
            plugin.getLogger().warning("Provider " + registration.getId() + " took "
                + String.format("%.3f", elapsed / 1_000_000.0D) + " ms");
        }

        if (!ProviderSnapshotPolicy.accepts(returned, knownRevision, offset, limit)) {
            metrics.providerError(registration.getId());
            if (errorWarningGate.tryAcquire(registration.getId(), System.nanoTime())) {
                plugin.getLogger().warning("Provider " + registration.getId()
                    + " returned an invalid or regressive snapshot");
            }
            return fallback(previous, subject, offset, limit);
        }

        if (previous != null && previous.getSubject().equals(subject)
                && previous.isSlice(offset, limit)
                && previous.getRevision() == returned.getRevision()) {
            cache.put(previous);
            return previous;
        }

        ProviderSnapshot snapshot = new ProviderSnapshot(subject, returned.getRevision(), offset, limit,
            returned.getTotalItems(), returned.getItems());
        cache.put(snapshot);
        return snapshot;
    }

    public ProviderClickResult click(Player player, PlayerGuiSession session, ClickBinding binding,
                                     GuiClick click) {
        if (player == null || session == null || binding == null || !binding.isProviderOwned()) {
            return ProviderClickResult.stale();
        }
        ProviderSnapshot snapshot = session.getProviderSnapshot();
        if (snapshot == null || !snapshot.getProviderId().equals(binding.getProviderId())
                || snapshot.getGeneration() != binding.getProviderGeneration()
                || snapshot.getRevision() != binding.getProviderRevision()
                || snapshot.getItem(binding.getProviderItemId()) == null) {
            return ProviderClickResult.stale();
        }
        RegisteredContentProvider registration = registry.resolveProvider(binding.getProviderId());
        if (registration == null || registration.getGeneration() != binding.getProviderGeneration()) {
            return ProviderClickResult.stale();
        }
        try {
            ProviderClickResult result = registration.getProvider().onClick(new ProviderClickContext(
                player.getUniqueId(), session.getMenuId(), registration.getId(),
                binding.getProviderItemId(), binding.getProviderRevision(), binding.getRenderedSlot(),
                toProviderClick(click), snapshot.getSubject().getArguments()));
            return result == null ? ProviderClickResult.error("provider-click-error") : result;
        } catch (Throwable error) {
            rethrowFatal(error);
            metrics.providerError(registration.getId());
            if (errorWarningGate.tryAcquire(registration.getId(), System.nanoTime())) {
                plugin.getLogger().log(Level.WARNING, "Provider click " + registration.getId()
                    + " failed", error);
            }
            return ProviderClickResult.error("provider-click-error");
        }
    }

    public void invalidate(InvalidationRequest request) {
        cache.invalidate(request);
    }

    public void providerRemoved(String providerId) {
        cache.invalidateProvider(providerId);
        metrics.removeProvider(providerId);
        warningGate.remove(providerId);
        errorWarningGate.remove(providerId);
    }

    public void clearPlayer(UUID playerId) {
        cache.clearPlayer(playerId);
    }

    public int cacheSize() {
        return cache.size();
    }

    @Override
    public void close() {
        cache.clear();
        warningGate.clear();
        errorWarningGate.clear();
    }

    private static ProviderSnapshot fallback(ProviderSnapshot previous, ProviderSubject subject,
                                             int offset, int limit) {
        return previous != null && previous.getSubject().equals(subject)
            && previous.isSlice(offset, limit) ? previous : null;
    }

    private static void rethrowFatal(Throwable error) {
        if (error instanceof VirtualMachineError) throw (VirtualMachineError) error;
        if (error instanceof ThreadDeath) throw (ThreadDeath) error;
    }

    private static ProviderClickType toProviderClick(GuiClick click) {
        if (click == null) return ProviderClickType.UNKNOWN;
        switch (click) {
            case LEFT: return ProviderClickType.LEFT;
            case RIGHT: return ProviderClickType.RIGHT;
            case SHIFT_LEFT: return ProviderClickType.SHIFT_LEFT;
            case SHIFT_RIGHT: return ProviderClickType.SHIFT_RIGHT;
            case MIDDLE: return ProviderClickType.MIDDLE;
            default: return ProviderClickType.UNKNOWN;
        }
    }
}
