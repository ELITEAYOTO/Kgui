package me.krunsh.kgui.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;

/**
 * Gestionnaire des fournisseurs de contenu dynamique.
 * 
 * Permet aux plugins externes d'enregistrer des providers qui seront
 * appelés automatiquement quand un menu paginé est ouvert.
 * 
 * Utilisation dans un plugin externe:
 * <pre>
 * // Récupérer Kgui
 * Plugin kguiPlugin = Bukkit.getPluginManager().getPlugin("Kgui");
 * Kgui kgui = (Kgui) kguiPlugin;
 * 
 * // Enregistrer un provider
 * kgui.getContentProviderManager().register("mon_provider", (player, args) -> {
 *     List<DynamicItem> items = new ArrayList<>();
 *     // ... construire les items ...
 *     return items;
 * });
 * </pre>
 * 
 * Dans le YAML du menu:
 * <pre>
 * pagination:
 *   provider: "mon_provider"
 *   args:
 *     filter: "all"
 * </pre>
 */
public class ContentProviderManager {
    
    private final Kgui plugin;
    private final Map<String, DynamicContentProvider> providers = new ConcurrentHashMap<>();
    
    public ContentProviderManager(Kgui plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Enregistre un provider de contenu dynamique.
     * 
     * @param providerId ID unique du provider (utilisé dans le YAML: provider: "xxx")
     * @param provider Le provider à enregistrer
     * @return true si enregistré, false si l'ID était déjà utilisé
     */
    public boolean register(String providerId, DynamicContentProvider provider) {
        if (providerId == null || provider == null) {
            return false;
        }
        
        String id = providerId.toLowerCase();
        if (providers.containsKey(id)) {
            plugin.getLogger().warning("ContentProvider '" + id + "' is already registered!");
            return false;
        }
        
        providers.put(id, provider);
        plugin.getLogger().info("ContentProvider registered: " + id);
        return true;
    }
    
    /**
     * Désenregistre un provider.
     * 
     * @param providerId ID du provider
     * @return true si désenregistré
     */
    public boolean unregister(String providerId) {
        if (providerId == null) return false;
        return providers.remove(providerId.toLowerCase()) != null;
    }
    
    /**
     * Vérifie si un provider existe.
     * 
     * @param providerId ID du provider
     * @return true si le provider existe
     */
    public boolean hasProvider(String providerId) {
        if (providerId == null) return false;
        return providers.containsKey(providerId.toLowerCase());
    }
    
    /**
     * Récupère le contenu d'un provider.
     * 
     * @param providerId ID du provider
     * @param player Le joueur qui ouvre le menu
     * @param args Arguments du YAML
     * @return Liste des items, ou liste vide si provider non trouvé
     */
    public List<DynamicItem> getContent(String providerId, Player player, Map<String, String> args) {
        if (providerId == null) {
            return Collections.emptyList();
        }
        
        DynamicContentProvider provider = providers.get(providerId.toLowerCase());
        if (provider == null) {
            plugin.getLogger().warning("ContentProvider not found: " + providerId);
            return Collections.emptyList();
        }
        
        try {
            List<DynamicItem> content = provider.getContent(player, args != null ? args : Collections.emptyMap());
            return content != null ? content : Collections.emptyList();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting content from provider: " + providerId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Notifie un provider d'un clic.
     */
    public void notifyClick(String providerId, Player player, DynamicItem item, String clickType) {
        if (providerId == null) return;
        
        DynamicContentProvider provider = providers.get(providerId.toLowerCase());
        if (provider != null) {
            try {
                provider.onClick(player, item, clickType);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in onClick for provider: " + providerId, e);
            }
        }
    }
    
    /**
     * Liste tous les providers enregistrés.
     */
    public Set<String> getRegisteredProviders() {
        return Collections.unmodifiableSet(providers.keySet());
    }
    
    /**
     * Nettoie tous les providers (appelé au disable).
     */
    public void clear() {
        providers.clear();
    }
}
