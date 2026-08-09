package me.krunsh.kgui.refresh;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.metrics.GuiMetrics;
import me.krunsh.kgui.session.SessionToken;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/** Une seule horloge, des échéances indexées et un budget strict par tick. */
public final class RefreshScheduler implements AutoCloseable {
    public interface Executor { void execute(RefreshRequest request); }

    private final Kgui plugin;
    private final Executor executor;
    private final GuiMetrics metrics;
    private final BoundedRefreshQueue queue;
    private final int maxRefreshesPerTick;
    private final long budgetNanos;
    private final Map<SessionToken, Periodic> periodic = new HashMap<>();
    private final PriorityQueue<Due> due = new PriorityQueue<>((a, b) -> Long.compare(a.tick, b.tick));
    private BukkitTask task;
    private long tick;

    public RefreshScheduler(Kgui plugin, Executor executor, GuiMetrics metrics, int maxPending,
                            int maxRefreshesPerTick, long budgetNanos) {
        this.plugin = plugin;
        this.executor = executor;
        this.metrics = metrics;
        this.queue = new BoundedRefreshQueue(maxPending);
        this.maxRefreshesPerTick = Math.max(1, maxRefreshesPerTick);
        this.budgetNanos = Math.max(100_000L, budgetNanos);
    }

    public void start() {
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public BoundedRefreshQueue.OfferResult request(RefreshRequest request) {
        BoundedRefreshQueue.OfferResult result = queue.offer(request);
        if (result == BoundedRefreshQueue.OfferResult.QUEUED) metrics.refreshQueued();
        else if (result == BoundedRefreshQueue.OfferResult.COALESCED) metrics.refreshCoalesced();
        return result;
    }

    public void registerPeriodic(SessionToken token, int intervalTicks) {
        removePeriodic(token);
        if (token == null || intervalTicks <= 0) return;
        Periodic value = new Periodic(Math.max(1, intervalTicks));
        periodic.put(token, value);
        due.add(new Due(token, value, tick + value.interval));
    }

    public void unregister(SessionToken token) {
        if (token == null) return;
        removePeriodic(token);
        queue.remove(token);
    }

    public int pendingCount() { return queue.size(); }
    public int periodicCount() { return periodic.size(); }

    private void removePeriodic(SessionToken token) {
        if (token == null) return;
        periodic.remove(token);
        java.util.Iterator<Due> iterator = due.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().token.equals(token)) iterator.remove();
        }
    }

    private void tick() {
        tick++;
        int dueBudget = maxRefreshesPerTick;
        while (dueBudget-- > 0 && !due.isEmpty() && due.peek().tick <= tick) {
            Due entry = due.poll();
            Periodic current = periodic.get(entry.token);
            if (current != entry.periodic) continue;
            request(new RefreshRequest(entry.token, RefreshPriority.PERIODIC, true, true, null));
            due.add(new Due(entry.token, current, tick + current.interval));
        }

        long started = System.nanoTime();
        java.util.List<RefreshRequest> batch = queue.drain(maxRefreshesPerTick);
        for (int index = 0; index < batch.size(); index++) {
            try {
                executor.execute(batch.get(index));
            } catch (RuntimeException error) {
                plugin.getLogger().log(Level.WARNING, "Kgui refresh failed for "
                    + batch.get(index).getToken(), error);
            }
            if (System.nanoTime() - started >= budgetNanos) {
                for (int remaining = index + 1; remaining < batch.size(); remaining++) {
                    queue.offer(batch.get(remaining));
                }
                break;
            }
        }
    }

    @Override
    public void close() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        periodic.clear();
        due.clear();
        queue.clear();
    }

    private static final class Periodic {
        private final int interval;
        private Periodic(int interval) { this.interval = interval; }
    }

    private static final class Due {
        private final SessionToken token;
        private final Periodic periodic;
        private final long tick;
        private Due(SessionToken token, Periodic periodic, long tick) {
            this.token = token; this.periodic = periodic; this.tick = tick;
        }
    }
}
