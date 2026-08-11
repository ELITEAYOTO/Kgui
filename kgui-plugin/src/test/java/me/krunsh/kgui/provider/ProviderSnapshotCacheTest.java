package me.krunsh.kgui.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import me.krunsh.kgui.api.ContentItem;
import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.api.MenuArguments;
import org.junit.Test;

public class ProviderSnapshotCacheTest {
    @Test
    public void cacheKeyIsStableAcrossArgumentInsertionOrder() {
        UUID player = UUID.randomUUID();
        Map<String, String> first = new LinkedHashMap<>();
        first.put("role", "member"); first.put("sort", "power");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("sort", "power"); second.put("role", "member");
        ProviderSubject a = new ProviderSubject("kfaction:members", 7L, player, "members",
            new MenuArguments(first));
        ProviderSubject b = new ProviderSubject("kfaction:members", 7L, player, "members",
            new MenuArguments(second));
        ProviderSnapshotCache cache = new ProviderSnapshotCache(16);
        cache.put(snapshot(a, 12L, 0));
        assertNotNull(cache.get(b, 0, 5));
        assertEquals(12L, cache.knownRevision(b));
    }

    @Test
    public void targetedInvalidationEvictsSliceButKeepsKnownRevision() {
        UUID player = UUID.randomUUID();
        ProviderSubject subject = subject(player, 1L, "members");
        ProviderSnapshotCache cache = new ProviderSnapshotCache(16);
        cache.put(snapshot(subject, 4L, 0));
        cache.invalidate(InvalidationRequest.playerMenu(player, "members",
            Collections.singleton("member-1"), "changed"));
        assertNull(cache.get(subject, 0, 5));
        assertEquals(4L, cache.knownRevision(subject));
    }

    @Test
    public void registrationGenerationAndLruBoundPreventStaleRetention() {
        UUID player = UUID.randomUUID();
        ProviderSnapshotCache cache = new ProviderSnapshotCache(16);
        ProviderSubject old = subject(player, 1L, "members");
        cache.put(snapshot(old, 1L, 0));
        assertNull(cache.get(subject(player, 2L, "members"), 0, 5));
        for (int index = 0; index < 24; index++) {
            ProviderSubject value = subject(UUID.randomUUID(), 2L, "menu-" + index);
            cache.put(snapshot(value, index, 0));
        }
        assertEquals(16, cache.size());
    }

    private static ProviderSubject subject(UUID player, long generation, String menu) {
        return new ProviderSubject("kfaction:members", generation, player, menu, MenuArguments.empty());
    }

    private static ProviderSnapshot snapshot(ProviderSubject subject, long revision, int offset) {
        ContentItem item = new ContentItem("member-1", "PAPER", (short) 0, 1,
            "Member", Collections.<String>emptyList(), Collections.<String, String>emptyMap());
        return new ProviderSnapshot(subject, revision, offset, 5, 1, Collections.singletonList(item));
    }
}
