package me.krunsh.kgui.api;

/** Resultat detaille d'une action externe. */
public final class ActionResult {
    public enum Status { HANDLED, IGNORED, DENIED, ERROR }

    private final Status status;
    private final boolean invalidate;
    private final String messageKey;
    private final String detail;

    public ActionResult(Status status, boolean invalidate, String messageKey, String detail) {
        if (status == null) throw new IllegalArgumentException("status must not be null");
        this.status = status;
        this.invalidate = invalidate;
        this.messageKey = normalize(messageKey);
        this.detail = normalize(detail);
    }

    public static ActionResult handled() { return new ActionResult(Status.HANDLED, false, null, null); }
    public static ActionResult handledAndInvalidate() { return new ActionResult(Status.HANDLED, true, null, null); }
    public static ActionResult ignored() { return new ActionResult(Status.IGNORED, false, null, null); }
    public static ActionResult denied(String messageKey) { return new ActionResult(Status.DENIED, false, messageKey, null); }
    public static ActionResult error(String messageKey, String detail) { return new ActionResult(Status.ERROR, false, messageKey, detail); }

    public Status getStatus() { return status; }
    public boolean shouldInvalidate() { return invalidate; }
    public String getMessageKey() { return messageKey; }
    public String getDetail() { return detail; }
    public boolean isHandled() { return status == Status.HANDLED; }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
