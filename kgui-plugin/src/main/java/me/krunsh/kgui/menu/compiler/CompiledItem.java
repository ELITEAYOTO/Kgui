package me.krunsh.kgui.menu.compiler;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Immutable item definition produced by the menu compiler. */
public final class CompiledItem {
    private final String id;
    private final List<Integer> slots;
    private final Map<String, Object> config;

    CompiledItem(String id, List<Integer> slots, Map<String, Object> config) {
        this.id = id;
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        this.config = ImmutableConfig.freezeMap(config);
    }

    public String getId() { return id; }
    public List<Integer> getSlots() { return slots; }
    public Map<String, Object> getConfig() { return config; }
}
