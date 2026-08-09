package me.krunsh.kgui.api;

/** Resultat detaille d'un clic delegue a un provider. */
public final class ProviderClickResult {
    public enum Status { HANDLED, IGNORED, STALE, DENIED, ERROR }

    private final Status status;
    private final boolean invalidate;
    private final String messageKey;

    public ProviderClickResult(Status status, boolean invalidate, String messageKey) {
        if (status == null) throw new IllegalArgumentException("status must not be null");
        this.status = status;
        this.invalidate = invalidate;
        this.messageKey = messageKey;
    }

    public static ProviderClickResult handled() { return new ProviderClickResult(Status.HANDLED, false, null); }
    public static ProviderClickResult handledAndInvalidate() { return new ProviderClickResult(Status.HANDLED, true, null); }
    public static ProviderClickResult ignored() { return new ProviderClickResult(Status.IGNORED, false, null); }
    public static ProviderClickResult stale() { return new ProviderClickResult(Status.STALE, true, null); }
    public static ProviderClickResult denied(String messageKey) { return new ProviderClickResult(Status.DENIED, false, messageKey); }
    public static ProviderClickResult error(String messageKey) { return new ProviderClickResult(Status.ERROR, false, messageKey); }

    public Status getStatus() { return status; }
    public boolean shouldInvalidate() { return invalidate; }
    public String getMessageKey() { return messageKey; }
}
