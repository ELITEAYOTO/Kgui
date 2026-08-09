package me.krunsh.kgui.refresh;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import me.krunsh.kgui.session.SessionToken;

/** Travail de rerendu coalesçable pour une session. */
public final class RefreshRequest {
    private static final int MAX_ITEM_IDS = 256;
    private final SessionToken token;
    private RefreshPriority priority;
    private boolean providerDirty;
    private boolean placeholdersDirty;
    private final Set<String> itemIds = new LinkedHashSet<>();

    public RefreshRequest(SessionToken token, RefreshPriority priority, boolean providerDirty,
                          boolean placeholdersDirty, Set<String> itemIds) {
        this.token = token;
        this.priority = priority == null ? RefreshPriority.INVALIDATION : priority;
        this.providerDirty = providerDirty;
        this.placeholdersDirty = placeholdersDirty;
        addItems(itemIds);
    }

    public SessionToken getToken() { return token; }
    public RefreshPriority getPriority() { return priority; }
    public boolean isProviderDirty() { return providerDirty; }
    public boolean isPlaceholdersDirty() { return placeholdersDirty; }
    public Set<String> getItemIds() { return Collections.unmodifiableSet(itemIds); }

    public void merge(RefreshRequest other) {
        if (other == null) return;
        if (other.priority.ordinal() < priority.ordinal()) priority = other.priority;
        providerDirty |= other.providerDirty;
        placeholdersDirty |= other.placeholdersDirty;
        addItems(other.itemIds);
    }

    private void addItems(Set<String> values) {
        if (values == null) return;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && value.trim().length() <= 128
                    && itemIds.size() < MAX_ITEM_IDS) {
                itemIds.add(value.trim());
            }
        }
    }
}
