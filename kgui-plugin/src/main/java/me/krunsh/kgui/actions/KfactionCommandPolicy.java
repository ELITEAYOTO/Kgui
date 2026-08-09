package me.krunsh.kgui.actions;

import java.util.Locale;

/** Interdit le retour silencieux aux commandes pour les mutations Kfaction. */
final class KfactionCommandPolicy {
    private KfactionCommandPolicy() {
    }

    static boolean isReserved(String command) {
        if (command == null) return false;
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) normalized = normalized.substring(1).trim();
        int separator = normalized.indexOf(' ');
        String label = separator < 0 ? normalized : normalized.substring(0, separator);
        return "f".equals(label) || "faction".equals(label) || "kfaction".equals(label);
    }
}
