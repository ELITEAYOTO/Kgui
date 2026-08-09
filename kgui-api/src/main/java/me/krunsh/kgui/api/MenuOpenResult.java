package me.krunsh.kgui.api;

/** Resultat explicite d'une tentative d'ouverture. */
public enum MenuOpenResult {
    OPENED,
    PLAYER_OFFLINE,
    MENU_NOT_FOUND,
    REJECTED,
    INVALID_REQUEST,
    WRONG_THREAD,
    UNAVAILABLE,
    ERROR
}
