package me.krunsh.kgui.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import me.krunsh.kgui.api.ContentItem;
import me.krunsh.kgui.api.ContentSnapshot;
import org.junit.Test;

public class ProviderSnapshotPolicyTest {
    @Test
    public void acceptsBoundedMonotonicSlice() {
        assertTrue(ProviderSnapshotPolicy.accepts(new ContentSnapshot(5L,
            Collections.singletonList(item("one")), 3), 4L, 0, 2));
    }

    @Test
    public void rejectsRegressiveDuplicateAndOutOfRangeSnapshots() {
        assertFalse(ProviderSnapshotPolicy.accepts(new ContentSnapshot(3L,
            Collections.singletonList(item("one")), 1), 4L, 0, 2));
        assertFalse(ProviderSnapshotPolicy.accepts(new ContentSnapshot(5L,
            Arrays.asList(item("same"), item("same")), 2), 4L, 0, 2));
        assertFalse(ProviderSnapshotPolicy.accepts(new ContentSnapshot(5L,
            Collections.singletonList(item("one")), 1), 4L, 2, 2));
        assertFalse(ProviderSnapshotPolicy.accepts(new ContentSnapshot(5L,
            Collections.singletonList(item(repeat('x', 129))), 1), 4L, 0, 2));
        assertFalse(ProviderSnapshotPolicy.accepts(new ContentSnapshot(5L,
            Collections.singletonList(new ContentItem("one", "NOT_A_MATERIAL", (short) 0, 1,
                "bad", Collections.<String>emptyList(), Collections.<String, String>emptyMap())), 1),
            4L, 0, 2));
    }

    private static ContentItem item(String id) {
        return new ContentItem(id, "PAPER", (short) 0, 1, id,
            Collections.<String>emptyList(), Collections.<String, String>emptyMap());
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
