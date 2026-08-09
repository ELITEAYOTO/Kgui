package me.krunsh.kgui.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Description neutre d'un item dynamique, sans objet Bukkit mutable. */
public final class ContentItem {
    private final String itemId;
    private final String material;
    private final short data;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final Map<String, String> attributes;

    public ContentItem(String itemId, String material, short data, int amount, String displayName,
                       List<String> lore, Map<String, String> attributes) {
        this.itemId = MenuOpenRequest.requireId(itemId, "itemId");
        this.material = MenuOpenRequest.requireId(material, "material");
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("amount must be between 1 and 64");
        this.data = data;
        this.amount = amount;
        this.displayName = displayName;
        this.lore = Collections.unmodifiableList(lore == null
                ? Collections.<String>emptyList() : new ArrayList<>(lore));
        Map<String, String> copy = new LinkedHashMap<>();
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) copy.put(entry.getKey(), entry.getValue());
            }
        }
        this.attributes = Collections.unmodifiableMap(copy);
    }

    public String getItemId() { return itemId; }
    public String getMaterial() { return material; }
    public short getData() { return data; }
    public int getAmount() { return amount; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public Map<String, String> getAttributes() { return attributes; }
}
