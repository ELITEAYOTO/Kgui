package me.krunsh.kgui.menu.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ImmutableConfig {
    private ImmutableConfig() {}

    static Map<String, Object> freezeMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), freeze(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    static Object freeze(Object value) {
        if (value instanceof Map) return freezeMap((Map<String, Object>) value);
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object element : (List<?>) value) copy.add(freeze(element));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Object mutableCopy(Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                copy.put(entry.getKey(), mutableCopy(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object element : (List<?>) value) copy.add(mutableCopy(element));
            return copy;
        }
        return value;
    }
}
