package me.krunsh.kgui.api;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

/**
 * Interface pour les fournisseurs de contenu dynamique des menus paginés.
 * 
 * Les plugins externes (Kfaction, Kspawners, etc.) implémentent cette interface
 * pour fournir du contenu dynamique à Kgui sans reflection.
 * 
 * Exemple d'utilisation dans Kfaction:
 * <pre>
 * // Dans onEnable() de Kfaction:
 * kgui.getContentProviderManager().register("kfaction_logs", new DynamicContentProvider() {
 *     @Override
 *     public List<DynamicItem> getContent(Player player, Map<String, String> args) {
 *         Faction faction = getFaction(player);
 *         List<FactionLog> logs = logManager.getLogs(faction.getId());
 *         return logs.stream()
 *             .map(log -> new DynamicItem.Builder()
 *                 .material(getMaterial(log))
 *                 .name(formatName(log))
 *                 .lore(formatLore(log))
 *                 .build())
 *             .collect(Collectors.toList());
 *     }
 * });
 * </pre>
 * 
 * Dans le YAML du menu:
 * <pre>
 * pagination:
 *   content_slots: "10-16,19-25"
 *   provider: "kfaction_logs"    # ID du provider enregistré
 *   args:                        # Arguments optionnels passés au provider
 *     filter: "all"
 *   empty_item:
 *     material: BARRIER
 *     name: "&cAucun contenu"
 * </pre>
 */
public interface DynamicContentProvider {
    
    /**
     * Fournit le contenu dynamique pour un menu paginé.
     * 
     * Cette méthode est appelée CHAQUE FOIS que le menu est ouvert ou rafraîchi.
     * Elle doit être performante et ne pas bloquer le thread principal.
     * 
     * @param player Le joueur qui ouvre le menu
     * @param args Arguments optionnels définis dans le YAML du menu
     * @return Liste des items dynamiques à afficher
     */
    List<DynamicItem> getContent(Player player, Map<String, String> args);
    
    /**
     * Appelé quand un item dynamique est cliqué.
     * 
     * @param player Le joueur qui a cliqué
     * @param item L'item cliqué (avec ses données)
     * @param clickType Type de clic (LEFT, RIGHT, SHIFT_LEFT, etc.)
     */
    default void onClick(Player player, DynamicItem item, String clickType) {
        // Par défaut, ne fait rien - les click_actions de l'item sont exécutées
    }
    
    /**
     * Retourne l'ID du provider (pour debugging).
     */
    default String getId() {
        return getClass().getSimpleName();
    }
}
