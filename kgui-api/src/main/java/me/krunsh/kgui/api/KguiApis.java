package me.krunsh.kgui.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Resolution sure de l'API; ne jamais utiliser Kgui.getInstance() depuis un plugin externe. */
public final class KguiApis {
    private KguiApis() {
    }

    public static KguiApi get() {
        try {
            RegisteredServiceProvider<KguiApi> registration = Bukkit.getServicesManager().getRegistration(KguiApi.class);
            return registration == null ? null : registration.getProvider();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }
}
