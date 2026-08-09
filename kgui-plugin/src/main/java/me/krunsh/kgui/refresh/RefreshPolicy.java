package me.krunsh.kgui.refresh;

import java.util.Locale;

public enum RefreshPolicy {
    MANUAL,
    EVENT,
    INTERVAL,
    HYBRID;

    public boolean acceptsEvents() { return this == EVENT || this == HYBRID; }
    public boolean hasInterval() { return this == INTERVAL || this == HYBRID; }

    public static RefreshPolicy parse(String value, int legacyInterval) {
        if (value == null || value.trim().isEmpty()) {
            return legacyInterval > 0 ? INTERVAL : EVENT;
        }
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return legacyInterval > 0 ? INTERVAL : EVENT; }
    }
}
