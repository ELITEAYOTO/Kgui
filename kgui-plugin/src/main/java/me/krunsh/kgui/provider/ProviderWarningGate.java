package me.krunsh.kgui.provider;

import java.util.concurrent.ConcurrentHashMap;

/** Anti-spam borné par les providers actifs pour les avertissements de lenteur. */
final class ProviderWarningGate {
    private final long intervalNanos;
    private final ConcurrentHashMap<String, Long> lastWarnings = new ConcurrentHashMap<>();

    ProviderWarningGate(long intervalNanos) {
        this.intervalNanos = Math.max(0L, intervalNanos);
    }

    boolean tryAcquire(String providerId, long nowNanos) {
        if (providerId == null) return false;
        Long previous = lastWarnings.get(providerId);
        if (previous != null && nowNanos - previous < intervalNanos) return false;
        if (previous == null) return lastWarnings.putIfAbsent(providerId, nowNanos) == null;
        return lastWarnings.replace(providerId, previous, nowNanos);
    }

    void remove(String providerId) {
        if (providerId != null) lastWarnings.remove(providerId);
    }

    void clear() {
        lastWarnings.clear();
    }
}
