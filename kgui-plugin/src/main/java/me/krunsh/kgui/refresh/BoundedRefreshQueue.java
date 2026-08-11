package me.krunsh.kgui.refresh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.krunsh.kgui.session.SessionToken;

/** File bornée, priorisée et coalescée par token de session. */
public final class BoundedRefreshQueue {
    public enum OfferResult { QUEUED, COALESCED, REJECTED }

    private final int maxPending;
    private final Map<SessionToken, RefreshRequest> pending = new LinkedHashMap<>();

    public BoundedRefreshQueue(int maxPending) {
        this.maxPending = Math.max(16, maxPending);
    }

    public synchronized OfferResult offer(RefreshRequest request) {
        RefreshRequest current = pending.get(request.getToken());
        if (current != null) {
            current.merge(request);
            return OfferResult.COALESCED;
        }
        if (pending.size() >= maxPending && !evictPeriodic(request.getPriority())) {
            return OfferResult.REJECTED;
        }
        pending.put(request.getToken(), request);
        return OfferResult.QUEUED;
    }

    public synchronized List<RefreshRequest> drain(int limit) {
        int safeLimit = Math.max(0, limit);
        List<RefreshRequest> result = new ArrayList<>(Math.min(safeLimit, pending.size()));
        drainPriority(result, safeLimit, RefreshPriority.INTERACTION);
        drainPriority(result, safeLimit, RefreshPriority.INVALIDATION);
        drainPriority(result, safeLimit, RefreshPriority.PERIODIC);
        return result;
    }

    public synchronized void remove(SessionToken token) {
        pending.remove(token);
    }

    public synchronized int size() { return pending.size(); }
    public synchronized void clear() { pending.clear(); }

    private void drainPriority(List<RefreshRequest> result, int limit, RefreshPriority priority) {
        Iterator<Map.Entry<SessionToken, RefreshRequest>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext() && result.size() < limit) {
            RefreshRequest request = iterator.next().getValue();
            if (request.getPriority() == priority) {
                result.add(request);
                iterator.remove();
            }
        }
    }

    private boolean evictPeriodic(RefreshPriority incoming) {
        if (incoming == RefreshPriority.PERIODIC) return false;
        Iterator<Map.Entry<SessionToken, RefreshRequest>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().getPriority() == RefreshPriority.PERIODIC) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }
}
