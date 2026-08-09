package me.krunsh.kgui.api;

/** Handle idempotent d'une extension detenue par un plugin. */
public interface OwnedRegistration extends AutoCloseable {
    String getId();
    boolean isRegistered();
    @Override
    void close();
}
