package me.krunsh.kgui.api;

/** Diagnostic explicite pour un soft-depend. */
public final class KguiApiCompatibility {
    public enum Status { ABSENT, INCOMPATIBLE, READY_2_0 }

    private final Status status;
    private final String detectedVersion;

    private KguiApiCompatibility(Status status, String detectedVersion) {
        this.status = status;
        this.detectedVersion = detectedVersion;
    }

    public static KguiApiCompatibility evaluate(KguiApi api) {
        if (api == null) return new KguiApiCompatibility(Status.ABSENT, null);
        try {
            String version = api.getApiVersion();
            int major = api.getApiMajor();
            if (major != KguiApiVersion.MAJOR || KguiApiVersion.majorOf(version) != KguiApiVersion.MAJOR) {
                return new KguiApiCompatibility(Status.INCOMPATIBLE, version);
            }
            return new KguiApiCompatibility(Status.READY_2_0, version);
        } catch (LinkageError | RuntimeException ignored) {
            return new KguiApiCompatibility(Status.INCOMPATIBLE, null);
        }
    }

    public Status getStatus() { return status; }
    public String getDetectedVersion() { return detectedVersion; }
    public boolean isReady() { return status == Status.READY_2_0; }
}
