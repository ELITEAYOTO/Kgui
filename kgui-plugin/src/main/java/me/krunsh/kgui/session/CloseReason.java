package me.krunsh.kgui.session;

/** Identifie le chemin qui a termine une session GUI. */
public enum CloseReason {
    PLAYER_CLOSE,
    NAVIGATION,
    QUIT,
    KICK,
    WORLD_CHANGE,
    RELOAD,
    DISABLE,
    REPLACED,
    INVALID_STATE
}
