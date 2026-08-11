package me.krunsh.kgui.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.krunsh.kgui.api.ContentItem;

/** Snapshot interne signé par inscription, sujet, révision et tranche. */
public final class ProviderSnapshot {
    private final ProviderSubject subject;
    private final long revision;
    private final int offset;
    private final int limit;
    private final int totalItems;
    private final List<ContentItem> items;
    private final Map<String, ContentItem> itemsById;

    public ProviderSnapshot(ProviderSubject subject, long revision, int offset, int limit,
                            int totalItems, List<ContentItem> items) {
        this.subject = subject;
        this.revision = revision;
        this.offset = offset;
        this.limit = limit;
        this.totalItems = totalItems;
        List<ContentItem> copy = items == null
            ? Collections.<ContentItem>emptyList() : new ArrayList<>(items);
        this.items = Collections.unmodifiableList(copy);
        Map<String, ContentItem> byId = new LinkedHashMap<>();
        for (ContentItem item : copy) byId.put(item.getItemId(), item);
        this.itemsById = Collections.unmodifiableMap(byId);
    }

    public ProviderSubject getSubject() { return subject; }
    public String getProviderId() { return subject.getProviderId(); }
    public long getGeneration() { return subject.getGeneration(); }
    public long getRevision() { return revision; }
    public int getOffset() { return offset; }
    public int getLimit() { return limit; }
    public int getTotalItems() { return totalItems; }
    public List<ContentItem> getItems() { return items; }
    public ContentItem getItem(String itemId) { return itemsById.get(itemId); }

    public boolean isSlice(int expectedOffset, int expectedLimit) {
        return offset == expectedOffset && limit == expectedLimit;
    }
}
