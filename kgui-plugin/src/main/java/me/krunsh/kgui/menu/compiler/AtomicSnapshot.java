package me.krunsh.kgui.menu.compiler;

import java.util.Objects;

/** Publishes a complete immutable state through one volatile write. */
public final class AtomicSnapshot<T> {
    private volatile T value;

    public AtomicSnapshot(T initialValue) {
        this.value = Objects.requireNonNull(initialValue, "initialValue");
    }

    public T get() {
        return value;
    }

    public boolean publishIfValid(boolean valid, T candidate) {
        if (!valid) return false;
        value = Objects.requireNonNull(candidate, "candidate");
        return true;
    }
}
