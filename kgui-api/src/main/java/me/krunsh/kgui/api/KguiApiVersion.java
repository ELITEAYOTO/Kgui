package me.krunsh.kgui.api;

/** Version du contrat public Kgui. */
public final class KguiApiVersion {
    public static final String CURRENT = "2.0.0";
    public static final int MAJOR = 2;

    private KguiApiVersion() {
    }

    public static int majorOf(String version) {
        if (version == null || version.trim().isEmpty()) {
            return -1;
        }
        int separator = version.indexOf('.');
        String major = separator < 0 ? version : version.substring(0, separator);
        try {
            return Integer.parseInt(major);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
