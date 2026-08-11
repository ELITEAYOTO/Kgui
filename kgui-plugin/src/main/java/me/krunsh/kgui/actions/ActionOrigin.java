package me.krunsh.kgui.actions;

/** Provenance d'une action, utilisee pour borner les capacites privilegiees. */
public enum ActionOrigin {
    LOCAL_MENU(true),
    PROVIDER(false),
    INTERNAL(false);

    private final boolean privilegedCommandsAllowed;

    ActionOrigin(boolean privilegedCommandsAllowed) {
        this.privilegedCommandsAllowed = privilegedCommandsAllowed;
    }

    public boolean allowsPrivilegedCommands() {
        return privilegedCommandsAllowed;
    }
}
