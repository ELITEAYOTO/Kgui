package me.krunsh.kgui.provider;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import me.krunsh.kgui.api.InvalidationRequest;

/** LRU borné par sujet et tranche ; aucune donnée provider n'est globale sans limite. */
public final class ProviderSnapshotCache {
    private final int maxEntries;
    private final LinkedHashMap<SliceKey, ProviderSnapshot> slices;
    private final LinkedHashMap<ProviderSubject, Long> revisions;

    public ProviderSnapshotCache(int maxEntries) {
        this.maxEntries = Math.max(16, maxEntries);
        this.slices = new LinkedHashMap<SliceKey, ProviderSnapshot>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<SliceKey, ProviderSnapshot> eldest) {
                return size() > ProviderSnapshotCache.this.maxEntries;
            }
        };
        this.revisions = new LinkedHashMap<ProviderSubject, Long>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<ProviderSubject, Long> eldest) {
                return size() > ProviderSnapshotCache.this.maxEntries;
            }
        };
    }

    public synchronized ProviderSnapshot get(ProviderSubject subject, int offset, int limit) {
        return slices.get(new SliceKey(subject, offset, limit));
    }

    public synchronized void put(ProviderSnapshot snapshot) {
        slices.put(new SliceKey(snapshot.getSubject(), snapshot.getOffset(), snapshot.getLimit()), snapshot);
        revisions.put(snapshot.getSubject(), snapshot.getRevision());
    }

    public synchronized long knownRevision(ProviderSubject subject) {
        Long revision = revisions.get(subject);
        return revision == null ? -1L : revision;
    }

    public synchronized void invalidate(InvalidationRequest request) {
        if (request == null) return;
        Iterator<Map.Entry<SliceKey, ProviderSnapshot>> slicesIterator = slices.entrySet().iterator();
        while (slicesIterator.hasNext()) {
            if (matches(slicesIterator.next().getKey().subject, request)) slicesIterator.remove();
        }
        // La révision connue est conservée pour ContentRequest.knownRevision.
    }

    public synchronized void invalidateProvider(String providerId) {
        removeSlices(null, null, providerId);
        Iterator<ProviderSubject> iterator = revisions.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getProviderId().equalsIgnoreCase(providerId)) iterator.remove();
        }
    }

    public synchronized void clearPlayer(UUID playerId) {
        removeSlices(playerId, null, null);
        Iterator<ProviderSubject> iterator = revisions.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getPlayerId().equals(playerId)) iterator.remove();
        }
    }

    public synchronized void clear() {
        slices.clear();
        revisions.clear();
    }

    public synchronized int size() { return slices.size(); }

    private void removeSlices(UUID playerId, String menuId, String providerId) {
        Iterator<SliceKey> iterator = slices.keySet().iterator();
        while (iterator.hasNext()) {
            ProviderSubject subject = iterator.next().subject;
            if ((playerId == null || playerId.equals(subject.getPlayerId()))
                    && (menuId == null || menuId.equalsIgnoreCase(subject.getMenuId()))
                    && (providerId == null || providerId.equalsIgnoreCase(subject.getProviderId()))) {
                iterator.remove();
            }
        }
    }

    private static boolean matches(ProviderSubject subject, InvalidationRequest request) {
        switch (request.getScope()) {
            case PLAYER_MENU:
                return subject.getPlayerId().equals(request.getPlayerId())
                    && subject.getMenuId().equalsIgnoreCase(request.getMenuId());
            case PLAYER:
                return subject.getPlayerId().equals(request.getPlayerId());
            case MENU:
                return subject.getMenuId().equalsIgnoreCase(request.getMenuId());
            case PROVIDER:
                return subject.getProviderId().equalsIgnoreCase(request.getProviderId());
            case ALL:
                return true;
            default:
                return false;
        }
    }

    private static final class SliceKey {
        private final ProviderSubject subject;
        private final int offset;
        private final int limit;

        private SliceKey(ProviderSubject subject, int offset, int limit) {
            this.subject = subject;
            this.offset = offset;
            this.limit = limit;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof SliceKey)) return false;
            SliceKey that = (SliceKey) other;
            return offset == that.offset && limit == that.limit && subject.equals(that.subject);
        }

        @Override public int hashCode() {
            int result = subject.hashCode();
            result = 31 * result + offset;
            result = 31 * result + limit;
            return result;
        }
    }
}
