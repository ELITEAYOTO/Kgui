package me.krunsh.kgui.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vue immutable et revisionnee d'une tranche de contenu. */
public final class ContentSnapshot {
    private final long revision;
    private final List<ContentItem> items;
    private final int totalItems;

    public ContentSnapshot(long revision, List<ContentItem> items, int totalItems) {
        if (totalItems < 0) throw new IllegalArgumentException("totalItems must be >= 0");
        this.revision = revision;
        this.items = Collections.unmodifiableList(items == null
                ? Collections.<ContentItem>emptyList() : new ArrayList<>(items));
        this.totalItems = totalItems;
    }

    public static ContentSnapshot empty(long revision) {
        return new ContentSnapshot(revision, Collections.<ContentItem>emptyList(), 0);
    }

    public long getRevision() { return revision; }
    public List<ContentItem> getItems() { return items; }
    public int getTotalItems() { return totalItems; }
}
