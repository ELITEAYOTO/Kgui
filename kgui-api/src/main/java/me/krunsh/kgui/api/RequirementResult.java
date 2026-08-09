package me.krunsh.kgui.api;

/** Resultat avec cle de message facultative, sans texte impose. */
public final class RequirementResult {
    private static final RequirementResult ALLOWED = new RequirementResult(true, null);
    private final boolean allowed;
    private final String messageKey;

    private RequirementResult(boolean allowed, String messageKey) {
        this.allowed = allowed;
        this.messageKey = messageKey;
    }

    public static RequirementResult allowed() { return ALLOWED; }
    public static RequirementResult denied(String messageKey) { return new RequirementResult(false, messageKey); }
    public boolean isAllowed() { return allowed; }
    public String getMessageKey() { return messageKey; }
}
