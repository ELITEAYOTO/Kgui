package me.krunsh.kgui.api;

/** Extension dynamique executee par Kgui sur le thread principal sauf contrat ulterieur explicite. */
public interface ContentProvider {
    ContentSnapshot getContent(ContentRequest request);

    default ProviderClickResult onClick(ProviderClickContext context) {
        return ProviderClickResult.ignored();
    }
}
