package me.krunsh.kgui.api;

import java.util.Set;
import org.bukkit.plugin.Plugin;

/** Point d'entree public Kgui 2.0, publie dans le ServicesManager Bukkit. */
public interface KguiApi {
    String getApiVersion();
    int getApiMajor();
    MenuOpenResult openMenu(MenuOpenRequest request);
    ProviderRegistration registerProvider(Plugin owner, String providerId, ContentProvider provider);
    ActionRegistration registerAction(Plugin owner, String actionId, ActionHandler handler);
    RequirementRegistration registerRequirement(Plugin owner, String requirementId, RequirementHandler handler);
    MenuPackRegistration registerMenuPack(Plugin owner, String packId, Set<String> menuIds);
    void invalidate(InvalidationRequest request);
    void unregisterAll(Plugin owner);
}
