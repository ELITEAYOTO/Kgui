package me.krunsh.kgui.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Arguments textuels transmis a un menu et a ses providers. */
public final class MenuArguments {
    private static final MenuArguments EMPTY = new MenuArguments(Collections.<String, String>emptyMap());
    private final Map<String, String> values;

    public MenuArguments(Map<String, String> values) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    public static MenuArguments empty() {
        return EMPTY;
    }

    public String get(String key) {
        return values.get(key);
    }

    public String getOrDefault(String key, String fallback) {
        String value = values.get(key);
        return value == null ? fallback : value;
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public Map<String, String> asMap() {
        return values;
    }
}
