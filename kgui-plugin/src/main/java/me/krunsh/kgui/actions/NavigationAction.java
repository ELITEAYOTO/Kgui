package me.krunsh.kgui.actions;

import java.util.Locale;

/** Arguments stricts de l'action kgui:navigate. */
final class NavigationAction {
    enum Kind { MOVE, SET }

    private final Kind kind;
    private final int value;

    private NavigationAction(Kind kind, int value) {
        this.kind = kind;
        this.value = value;
    }

    Kind getKind() {
        return kind;
    }

    int getValue() {
        return value;
    }

    /**
     * Formes acceptées : direction=next|previous [step=1] et page=N.
     * Les valeurs sont bornées pour qu'une configuration erronée ne puisse pas
     * provoquer un déplacement ni un calcul disproportionné.
     */
    static NavigationAction parse(String arguments) {
        if (arguments == null) return null;
        String direction = null;
        Integer step = null;
        Integer page = null;

        String trimmed = arguments.trim();
        if (trimmed.isEmpty()) return null;
        for (String token : trimmed.split("\\s+")) {
            int separator = token.indexOf('=');
            if (separator <= 0 || separator == token.length() - 1) return null;
            String key = token.substring(0, separator).toLowerCase(Locale.ROOT);
            String value = token.substring(separator + 1);
            if ("direction".equals(key) && direction == null) {
                direction = value.toLowerCase(Locale.ROOT);
            } else if ("step".equals(key) && step == null) {
                step = parseBoundedPositive(value, 100);
                if (step == null) return null;
            } else if ("page".equals(key) && page == null) {
                page = parseBoundedPositive(value, Integer.MAX_VALUE);
                if (page == null) return null;
            } else {
                return null;
            }
        }

        if (page != null) {
            return direction == null && step == null ? new NavigationAction(Kind.SET, page) : null;
        }
        if (direction == null) return null;
        int distance = step == null ? 1 : step;
        if ("next".equals(direction) || "down".equals(direction)) {
            return new NavigationAction(Kind.MOVE, distance);
        }
        if ("previous".equals(direction) || "up".equals(direction)) {
            return new NavigationAction(Kind.MOVE, -distance);
        }
        return null;
    }

    private static Integer parseBoundedPositive(String value, int maximum) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 && parsed <= maximum ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
