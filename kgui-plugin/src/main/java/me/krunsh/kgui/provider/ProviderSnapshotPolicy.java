package me.krunsh.kgui.provider;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import me.krunsh.kgui.api.ContentItem;
import me.krunsh.kgui.api.ContentSnapshot;
import org.bukkit.Material;

/** Validation pure du contrat de tranche et de révision provider. */
public final class ProviderSnapshotPolicy {
    private ProviderSnapshotPolicy() {}

    public static boolean accepts(ContentSnapshot snapshot, long knownRevision, int offset, int limit) {
        if (snapshot == null || offset < 0 || limit < 1
                || snapshot.getRevision() < 0L || snapshot.getRevision() < knownRevision) return false;
        List<ContentItem> items = snapshot.getItems();
        if (items == null || items.size() > limit || snapshot.getTotalItems() < 0) return false;
        if (offset >= snapshot.getTotalItems() && !items.isEmpty()) return false;
        if (offset < snapshot.getTotalItems()
                && (long) offset + items.size() > snapshot.getTotalItems()) return false;
        Set<String> ids = new HashSet<>();
        for (ContentItem item : items) {
            if (item == null || item.getItemId().length() > 128 || item.getMaterial().length() > 64
                    || Material.getMaterial(item.getMaterial().toUpperCase(Locale.ROOT)) == null
                    || (item.getDisplayName() != null && item.getDisplayName().length() > 512)
                    || item.getLore().size() > 64 || item.getAttributes().size() > 64
                    || !ids.add(item.getItemId())) return false;
            for (String line : item.getLore()) if (line == null || line.length() > 1024) return false;
            for (java.util.Map.Entry<String, String> attribute : item.getAttributes().entrySet()) {
                if (attribute.getKey().length() > 64 || !attribute.getKey().matches("[A-Za-z0-9_.-]+")
                        || attribute.getValue().length() > 4096) return false;
            }
        }
        return true;
    }
}
