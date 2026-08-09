package me.krunsh.kgui.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.api.ActionContext;
import me.krunsh.kgui.api.ActionHandler;
import me.krunsh.kgui.api.ActionResult;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.menu.MenuData;
import me.krunsh.kgui.menu.MenuType;
import me.krunsh.kgui.pagination.PaginationManager;
import me.krunsh.kgui.pagination.ScrollManager;
import me.krunsh.kgui.service.KguiApiProvider;
import me.krunsh.kgui.utils.ColorUtils;

/**
 * Gestionnaire des actions
 * Exécute les actions définies dans les menus
 */
public class ActionManager {

    private final Kgui plugin;
    private final KguiApiProvider apiProvider;
    private final Map<String, ActionExecutor> executors = new HashMap<>();
    
    // Pattern pour parser les actions: [type] argument
    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\s*(.*)");
    
    // Version NMS détectée dynamiquement
    private static final String NMS_VERSION;
    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        NMS_VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    public ActionManager(Kgui plugin, KguiApiProvider apiProvider) {
        this.plugin = plugin;
        this.apiProvider = apiProvider;
        registerDefaultExecutors();
    }

    /**
     * Enregistre les executors par défaut
     */
    private void registerDefaultExecutors() {
        // [player] - Exécute une commande en tant que joueur
        executors.put("player", (player, args) -> {
            String command = plugin.getGuiManager().parsePlaceholders(player, args);
            if (KfactionCommandPolicy.isReserved(command)) {
                rejectLegacyFactionCommand(player, command);
                return;
            }
            player.performCommand(command);
        });
        
        // [console] - Exécute une commande depuis la console
        executors.put("console", (player, args) -> {
            String command = plugin.getGuiManager().parsePlaceholders(player, args);
            if (KfactionCommandPolicy.isReserved(command)) {
                rejectLegacyFactionCommand(player, command);
                return;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
        
        // [op] - Exécute une commande avec OP temporaire (SÉCURISÉ: nécessite kgui.admin)
        executors.put("op", (player, args) -> {
            if (!player.hasPermission("kgui.admin")) {
                plugin.getLogger().warning("[SECURITY] Player " + player.getName() + " tried to use [op] action without kgui.admin permission!");
                return;
            }
            String command = plugin.getGuiManager().parsePlaceholders(player, args);
            if (KfactionCommandPolicy.isReserved(command)) {
                rejectLegacyFactionCommand(player, command);
                return;
            }
            boolean wasOp = player.isOp();
            try {
                if (!wasOp) player.setOp(true);
                player.performCommand(command);
            } finally {
                if (!wasOp) player.setOp(false);
            }
        });
        
        // [message] - Envoie un message au joueur
        executors.put("message", (player, args) -> {
            String message = plugin.getGuiManager().parsePlaceholders(player, args);
            player.sendMessage(ColorUtils.colorize(message));
        });
        
        // [broadcast] - Broadcast un message
        executors.put("broadcast", (player, args) -> {
            String message = plugin.getGuiManager().parsePlaceholders(player, args);
            Bukkit.broadcastMessage(ColorUtils.colorize(message));
        });
        
        // [sound] - Joue un son FORMAT: SOUND_NAME volume pitch OU SOUND_NAME
        executors.put("sound", (player, args) -> {
            String[] parts = args.split("\\s+");
            String soundName = parts[0];
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound: " + soundName);
            }
        });
        
        // [title] - Affiche un title FORMAT: title;subtitle;fadeIn;stay;fadeOut
        executors.put("title", (player, args) -> {
            String[] parts = args.split(";");
            String title = parts.length > 0 ? plugin.getGuiManager().parsePlaceholders(player, parts[0]) : "";
            String subtitle = parts.length > 1 ? plugin.getGuiManager().parsePlaceholders(player, parts[1]) : "";
            int fadeIn = parts.length > 2 ? parseIntSafe(parts[2], 10) : 10;
            int stay = parts.length > 3 ? parseIntSafe(parts[3], 70) : 70;
            int fadeOut = parts.length > 4 ? parseIntSafe(parts[4], 20) : 20;
            
            sendTitle(player, ColorUtils.colorize(title), ColorUtils.colorize(subtitle), fadeIn, stay, fadeOut);
        });
        
        // [actionbar] - Affiche un message dans l'actionbar
        executors.put("actionbar", (player, args) -> {
            String message = plugin.getGuiManager().parsePlaceholders(player, args);
            // Pour 1.8, on utilise le chat de type actionbar via packets
            sendActionBar(player, ColorUtils.colorize(message));
        });
        
        // [close] - Ferme le menu
        executors.put("close", (player, args) -> {
            player.closeInventory();
        });
        
        // [open] - Ouvre un autre menu
        executors.put("open", (player, args) -> {
            String menuId = args.trim();
            // Délai d'un tick pour éviter les conflits
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getGuiManager().openMenu(player, menuId);
            }, 1L);
        });
        
        // [open_menu] - Alias pour [open]
        executors.put("open_menu", (player, args) -> {
            String menuId = args.trim();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getGuiManager().openMenu(player, menuId);
            }, 1L);
        });
        
        // [back] - Retour au menu précédent (multi-niveaux)
        executors.put("back", (player, args) -> {
            String previousMenu = plugin.getGuiManager().getPlayerPreviousMenu(player);
            if (previousMenu != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openMenuBack(player, previousMenu);
                }, 1L);
            } else {
                player.closeInventory();
            }
        });
        
        // [prev_page] - Page précédente (pagination)
        executors.put("prev_page", (player, args) -> {
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null && plugin.getGuiManager().tryLockNavigation(player)
                    && plugin.getPaginationManager().previousPage(player, currentMenu)) {
                plugin.getGuiManager().refreshMenu(player);
            }
        });
        
        // [next_page] - Page suivante (pagination)
        executors.put("next_page", (player, args) -> {
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null && plugin.getGuiManager().tryLockNavigation(player)
                    && plugin.getPaginationManager().nextPage(player, currentMenu)) {
                plugin.getGuiManager().refreshMenu(player);
            }
        });
        
        // [set_page] - Aller à une page spécifique
        executors.put("set_page", (player, args) -> {
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null && plugin.getGuiManager().tryLockNavigation(player)) {
                try {
                    int page = Integer.parseInt(args.trim());
                    MenuData menuData = plugin.getMenuManager().getMenu(currentMenu);
                    if (menuData != null && menuData.getMenuType() == MenuType.SCROLL) {
                        if (plugin.getScrollManager().setScrollOffset(player, currentMenu, Math.max(0, page - 1))) {
                            syncPaginationWithScroll(player, currentMenu);
                            plugin.getGuiManager().refreshMenu(player);
                        }
                    } else if (plugin.getPaginationManager().setPage(player, currentMenu, page)) {
                        plugin.getGuiManager().refreshMenu(player);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
        
        // [scroll_up] - Scroll vers le haut
        executors.put("scroll_up", (player, args) -> {
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null && plugin.getGuiManager().tryLockNavigation(player)
                    && plugin.getScrollManager().scrollUp(player, currentMenu)) {
                syncPaginationWithScroll(player, currentMenu);
                plugin.getGuiManager().refreshMenu(player);
            }
        });
        
        // [scroll_down] - Scroll vers le bas
        executors.put("scroll_down", (player, args) -> {
            String currentMenu = plugin.getGuiManager().getPlayerCurrentMenu(player);
            if (currentMenu != null && plugin.getGuiManager().tryLockNavigation(player)
                    && plugin.getScrollManager().scrollDown(player, currentMenu)) {
                syncPaginationWithScroll(player, currentMenu);
                plugin.getGuiManager().refreshMenu(player);
            }
        });
        
        // [refresh] - Rafraîchit le menu actuel
        executors.put("refresh", (player, args) -> {
            plugin.getGuiManager().refreshMenu(player);
        });
        
        // [take_money] - Retire de l'argent au joueur
        executors.put("take_money", (player, args) -> {
            if (!plugin.getHookManager().isVaultEnabled()) return;
            
            double amount = Double.parseDouble(plugin.getGuiManager().parsePlaceholders(player, args));
            plugin.getHookManager().getVaultHook().withdraw(player, amount);
        });
        
        // [give_money] - Donne de l'argent au joueur
        executors.put("give_money", (player, args) -> {
            if (!plugin.getHookManager().isVaultEnabled()) return;
            
            double amount = Double.parseDouble(plugin.getGuiManager().parsePlaceholders(player, args));
            plugin.getHookManager().getVaultHook().deposit(player, amount);
        });
        
        // [take_points] - Retire des points au joueur
        executors.put("take_points", (player, args) -> {
            if (!plugin.getHookManager().isPlayerPointsEnabled()) return;
            
            int amount = Integer.parseInt(plugin.getGuiManager().parsePlaceholders(player, args));
            plugin.getHookManager().getPlayerPointsHook().takePoints(player, amount);
        });
        
        // [give_points] - Donne des points au joueur
        executors.put("give_points", (player, args) -> {
            if (!plugin.getHookManager().isPlayerPointsEnabled()) return;
            
            int amount = Integer.parseInt(plugin.getGuiManager().parsePlaceholders(player, args));
            plugin.getHookManager().getPlayerPointsHook().givePoints(player, amount);
        });
        
        // [teleport] - Téléporte le joueur FORMAT: world;x;y;z OU x;y;z
        executors.put("teleport", (player, args) -> {
            String[] parts = args.split(";");
            org.bukkit.Location loc;
            
            if (parts.length >= 4) {
                org.bukkit.World world = Bukkit.getWorld(parts[0]);
                if (world == null) world = player.getWorld();
                loc = new org.bukkit.Location(world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
            } else if (parts.length >= 3) {
                loc = new org.bukkit.Location(player.getWorld(),
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
            } else {
                return;
            }
            
            player.closeInventory();
            player.teleport(loc);
        });
        
        // [delay] - Délai avant les prochaines actions (en ticks)
        // Note: Ceci nécessite une gestion spéciale dans executeActions
        executors.put("delay", (player, args) -> {
            // Géré séparément
        });
        
        // [chat] - Fait parler le joueur
        executors.put("chat", (player, args) -> {
            String message = plugin.getGuiManager().parsePlaceholders(player, args);
            player.chat(message);
        });
        
        // [input_text] - Demande une saisie texte FORMAT: variable_name;Titre;valeur_defaut
        executors.put("input_text", (player, args) -> {
            String[] parts = args.split(";", 3);
            String varName = parts.length > 0 ? parts[0].trim() : "input";
            String title = parts.length > 1 ? plugin.getGuiManager().parsePlaceholders(player, parts[1]) : "Entrez une valeur";
            String defaultValue = parts.length > 2 ? parts[2].trim() : "";
            
            player.closeInventory();
            
            plugin.getInputManager().openTextInput(player, title, defaultValue, (result) -> {
                if (result != null) {
                    // Stocker le résultat pour utilisation ultérieure
                    plugin.getPlayerDataManager().setData(player, varName, result);
                }
            });
        });
        
        // [input_number] - Demande une saisie numérique FORMAT: variable_name;Titre;min;max;valeur_defaut
        executors.put("input_number", (player, args) -> {
            String[] parts = args.split(";", 5);
            String varName = parts.length > 0 ? parts[0].trim() : "input";
            String title = parts.length > 1 ? plugin.getGuiManager().parsePlaceholders(player, parts[1]) : "Entrez un nombre";
            int min = parts.length > 2 ? parseInt(parts[2], 0) : 0;
            int max = parts.length > 3 ? parseInt(parts[3], Integer.MAX_VALUE) : Integer.MAX_VALUE;
            int defaultValue = parts.length > 4 ? parseInt(parts[4], 0) : 0;
            
            player.closeInventory();
            
            plugin.getInputManager().openNumberInput(player, title, defaultValue, min, max, (result, success) -> {
                if (success && result != null) {
                    // Stocker le résultat
                    plugin.getPlayerDataManager().setData(player, varName, String.valueOf(result));
                }
            });
        });
        
        // [confirm] - Ouvre un dialogue de confirmation FORMAT: action_si_confirme;titre;description
        executors.put("confirm", (player, args) -> {
            String[] parts = args.split(";", 3);
            String confirmAction = parts.length > 0 ? parts[0].trim() : "";
            String title = parts.length > 1 ? plugin.getGuiManager().parsePlaceholders(player, parts[1]) : "Confirmer?";
            String description = parts.length > 2 ? plugin.getGuiManager().parsePlaceholders(player, parts[2]) : "";
            
            player.closeInventory();
            
            plugin.getInputManager().openConfirmDialog(player, title, description,
                // Si confirmé
                () -> {
                    // Exécuter l'action de confirmation
                    if (!confirmAction.isEmpty()) {
                        java.util.List<String> confirmActions = java.util.Arrays.asList(confirmAction);
                        executeActions(player, confirmActions);
                    }
                },
                // Si annulé
                () -> {
                    // Rien faire ou rouvrir le menu précédent
                    String previousMenu = plugin.getGuiManager().getPlayerPreviousMenu(player);
                    if (previousMenu != null) {
                        plugin.getGuiManager().openMenuBack(player, previousMenu);
                    }
                }
            );
        });
        
        // ========== ACTIONS KFACTION ==========
        
        // [f_claim] - Claim le chunk actuel
        executors.put("f_claim", (player, args) -> {
            executeExtensionById("kfaction:claim", player, args);
        });
        
        // [f_unclaim] - Unclaim le chunk actuel
        executors.put("f_unclaim", (player, args) -> {
            executeExtensionById("kfaction:unclaim", player, args);
        });
        
        // [f_home] - Téléportation au home faction
        executors.put("f_home", (player, args) -> {
            executeExtensionById("kfaction:teleport_home", player, args);
        });
        
        // [f_sethome] - Définir le home faction
        executors.put("f_sethome", (player, args) -> {
            executeExtensionById("kfaction:set_home", player, args);
        });
        
        // [f_invite] - Inviter un joueur FORMAT: nom_joueur
        executors.put("f_invite", (player, args) -> {
            String target = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!target.isEmpty()) {
                executeExtensionById("kfaction:invite", player, "target=" + target);
            }
        });
        
        // [f_kick] - Expulser un membre FORMAT: nom_joueur
        executors.put("f_kick", (player, args) -> {
            String target = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!target.isEmpty()) {
                executeExtensionById("kfaction:kick", player, "target=" + target);
            }
        });
        
        // [f_promote] - Promouvoir un membre FORMAT: nom_joueur
        executors.put("f_promote", (player, args) -> {
            String target = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!target.isEmpty()) {
                executeExtensionById("kfaction:promote", player, "target=" + target);
            }
        });
        
        // [f_demote] - Rétrograder un membre FORMAT: nom_joueur
        executors.put("f_demote", (player, args) -> {
            String target = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!target.isEmpty()) {
                executeExtensionById("kfaction:demote", player, "target=" + target);
            }
        });
        
        // [f_ally] - Proposer une alliance FORMAT: nom_faction
        executors.put("f_ally", (player, args) -> {
            String faction = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!faction.isEmpty()) {
                executeExtensionById("kfaction:request_relation", player, "relation=ALLY;faction=" + faction);
            }
        });
        
        // [f_enemy] - Déclarer ennemi FORMAT: nom_faction
        executors.put("f_enemy", (player, args) -> {
            String faction = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!faction.isEmpty()) {
                executeExtensionById("kfaction:request_relation", player, "relation=ENEMY;faction=" + faction);
            }
        });
        
        // [f_neutral] - Relation neutre FORMAT: nom_faction
        executors.put("f_neutral", (player, args) -> {
            String faction = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!faction.isEmpty()) {
                executeExtensionById("kfaction:set_neutral", player, "faction=" + faction);
            }
        });
        
        // [f_deposit] - Déposer de l'argent FORMAT: montant
        executors.put("f_deposit", (player, args) -> {
            String amount = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!amount.isEmpty()) {
                executeExtensionById("kfaction:deposit", player, "amount=" + amount);
            }
        });
        
        // [f_withdraw] - Retirer de l'argent FORMAT: montant
        executors.put("f_withdraw", (player, args) -> {
            String amount = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!amount.isEmpty()) {
                executeExtensionById("kfaction:withdraw", player, "amount=" + amount);
            }
        });
        
        // [f_leave] - Quitter la faction
        executors.put("f_leave", (player, args) -> {
            executeExtensionById("kfaction:leave", player, args);
        });
        
        // [f_disband] - Dissoudre la faction
        executors.put("f_disband", (player, args) -> {
            executeExtensionById("kfaction:disband", player, args);
        });
        
        // [f_chat] - Changer de mode chat FORMAT: mode (f/a/p)
        executors.put("f_chat", (player, args) -> {
            String mode = args.trim();
            executeExtensionById("kfaction:set_chat_mode", player,
                    "mode=" + (mode.isEmpty() ? "f" : mode));
        });
        
        // [f_warp] - Téléportation à un warp FORMAT: nom_warp
        executors.put("f_warp", (player, args) -> {
            String warp = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!warp.isEmpty()) {
                executeExtensionById("kfaction:teleport_warp", player, "warp=" + warp);
            }
        });
        
        // [f_setwarp] - Créer un warp FORMAT: nom_warp
        executors.put("f_setwarp", (player, args) -> {
            String warp = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!warp.isEmpty()) {
                executeExtensionById("kfaction:create_warp", player, "warp=" + warp);
            }
        });
        
        // [f_delwarp] - Supprimer un warp FORMAT: nom_warp
        executors.put("f_delwarp", (player, args) -> {
            String warp = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            if (!warp.isEmpty()) {
                executeExtensionById("kfaction:delete_warp", player, "warp=" + warp);
            }
        });
        
        // [f_show] - Afficher les infos de faction FORMAT: nom_faction (optionnel)
        executors.put("f_show", (player, args) -> {
            String faction = plugin.getGuiManager().parsePlaceholders(player, args).trim();
            executeExtensionById("kfaction:show", player, "faction=" + faction);
        });
        
        // [f_menu] - Ouvrir le menu principal de faction
        executors.put("f_menu", (player, args) -> {
            String menuType = args.trim();
            if (menuType.isEmpty()) {
                // Ouvrir le menu principal via Kgui
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openMenu(player, "faction_menu");
                }, 1L);
            } else {
                // Ouvrir un sous-menu spécifique
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openMenu(player, "faction_" + menuType);
                }, 1L);
            }
        });
    }
    
    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Aligne les placeholders de pagination (%page%/%max_page%) avec l'état du scroll.
     */
    private void syncPaginationWithScroll(Player player, String menuId) {
        ScrollManager.ScrollData scrollData = plugin.getScrollManager().getScrollData(player, menuId);
        if (scrollData == null) {
            return;
        }

        PaginationManager.PageData pageData = plugin.getPaginationManager().getPageData(player, menuId);
        if (pageData == null) {
            plugin.getPaginationManager().initPlayer(player, menuId, 1, Math.max(1, scrollData.getMaxOffset() + 1));
            pageData = plugin.getPaginationManager().getPageData(player, menuId);
        }

        if (pageData != null) {
            int maxPage = Math.max(1, scrollData.getMaxOffset() + 1);
            int currentPage = Math.min(maxPage, scrollData.getScrollOffset() + 1);
            pageData.setMaxPage(maxPage);
            pageData.setCurrentPage(currentPage);
        }
    }

    /**
     * Exécute une liste d'actions
     */
    public void executeActions(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) return;
        
        int delay = 0;
        
        for (String action : actions) {
            if (action == null || action.isEmpty()) continue;
            
            Matcher matcher = ACTION_PATTERN.matcher(action);
            if (!matcher.matches()) {
                plugin.getLogger().warning("Invalid action format: " + action);
                continue;
            }
            
            String type = matcher.group(1).toLowerCase();
            String args = matcher.group(2);
            
            // Gérer le delay
            if (type.equals("delay")) {
                try {
                    delay += Integer.parseInt(args.trim());
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid delay value: " + args);
                }
                continue;
            }
            
            ActionExecutor executor = executors.get(type);
            if (executor == null) {
                ActionHandler handler = apiProvider.findAction(type);
                if (handler == null) {
                    plugin.getLogger().warning("Unknown action type: " + type);
                    continue;
                }
                executor = (target, raw) -> executeRegisteredAction(handler, target, raw);
            }
            final ActionExecutor selectedExecutor = executor;
            
            // Exécuter avec ou sans delay
            if (delay > 0) {
                final String finalArgs = args;
                final int currentDelay = delay;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        selectedExecutor.execute(player, finalArgs);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error executing action [" + type + "]: " + e.getMessage());
                    }
                }, currentDelay);
            } else {
                try {
                    selectedExecutor.execute(player, args);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error executing action [" + type + "]: " + e.getMessage());
                }
            }
            
            // Debug
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[Debug] Action: [" + type + "] " + args);
            }
        }
    }

    /**
     * Enregistre un executor personnalisé
     */
    public void registerExecutor(String type, ActionExecutor executor) {
        executors.put(type.toLowerCase(), executor);
    }

    private void executeRegisteredAction(ActionHandler handler, Player player, String rawArguments) {
        String menuId = plugin.getGuiManager().getPlayerCurrentMenu(player);
        if (menuId == null) return;

        String parsed = plugin.getGuiManager().parsePlaceholders(player,
                rawArguments == null ? "" : rawArguments).trim();
        Map<String, String> values = new HashMap<>();
        values.put("raw", parsed);
        if (!parsed.isEmpty()) {
            for (String token : parsed.split("[;\\s]+")) {
                int separator = token.indexOf('=');
                if (separator > 0 && separator < token.length() - 1) {
                    values.put(token.substring(0, separator), token.substring(separator + 1));
                }
            }
        }

        ActionResult result = handler.execute(new ActionContext(
                player.getUniqueId(), menuId, null, new MenuArguments(values)));
        if (result == null) {
            plugin.getLogger().warning("Registered action returned null");
            return;
        }
        if (result.getMessageKey() != null) {
            plugin.getMessageManager().send(player, result.getMessageKey());
        }
        if (result.shouldInvalidate()) {
            plugin.getGuiManager().refreshMenu(player);
        }
    }

    private void executeExtensionById(String id, Player player, String arguments) {
        ActionHandler handler = apiProvider.findAction(id);
        if (handler == null) {
            plugin.getLogger().warning("Required action is not registered: " + id);
            return;
        }
        executeRegisteredAction(handler, player, arguments);
    }

    private void rejectLegacyFactionCommand(Player player, String command) {
        plugin.getLogger().warning("Blocked legacy Kfaction command action for " + player.getName()
                + "; register and use a typed [kfaction:...] action instead: " + command);
    }

    /**
     * Envoie un message actionbar (compatible toutes versions NMS)
     */
    private void sendActionBar(Player player, String message) {
        try {
            // Utiliser reflection avec version NMS détectée dynamiquement
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".entity.CraftPlayer");
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".PacketPlayOutChat");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".IChatBaseComponent$ChatSerializer");
            
            Object chatComponent = chatSerializerClass.getMethod("a", String.class)
                .invoke(null, "{\"text\":\"" + escapeJson(message) + "\"}");
            Object packet = packetPlayOutChatClass.getConstructor(chatComponentClass, byte.class)
                .newInstance(chatComponent, (byte) 2);
            
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + NMS_VERSION + ".Packet"))
                .invoke(connection, packet);
        } catch (Exception e) {
            // Fallback: envoyer comme message normal
            player.sendMessage(message);
        }
    }
    
    /**
     * Envoie un title avec timings (compatible toutes versions NMS via reflection)
     */
    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            // Classes NMS avec version détectée dynamiquement
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".entity.CraftPlayer");
            Class<?> packetTitleClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".PacketPlayOutTitle");
            Class<?> enumTitleActionClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".IChatBaseComponent$ChatSerializer");
            
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            
            // 1. Envoyer les timings
            Object timesAction = enumTitleActionClass.getField("TIMES").get(null);
            Object timesPacket = packetTitleClass.getConstructor(enumTitleActionClass, chatComponentClass, int.class, int.class, int.class)
                .newInstance(timesAction, null, fadeIn, stay, fadeOut);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + NMS_VERSION + ".Packet"))
                .invoke(connection, timesPacket);
            
            // 2. Envoyer le title principal
            if (title != null && !title.isEmpty()) {
                Object titleComponent = chatSerializerClass.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + escapeJson(title) + "\"}");
                Object titleAction = enumTitleActionClass.getField("TITLE").get(null);
                Object titlePacket = packetTitleClass.getConstructor(enumTitleActionClass, chatComponentClass)
                    .newInstance(titleAction, titleComponent);
                connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + NMS_VERSION + ".Packet"))
                    .invoke(connection, titlePacket);
            }
            
            // 3. Envoyer le subtitle
            if (subtitle != null && !subtitle.isEmpty()) {
                Object subtitleComponent = chatSerializerClass.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + escapeJson(subtitle) + "\"}");
                Object subtitleAction = enumTitleActionClass.getField("SUBTITLE").get(null);
                Object subtitlePacket = packetTitleClass.getConstructor(enumTitleActionClass, chatComponentClass)
                    .newInstance(subtitleAction, subtitleComponent);
                connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + NMS_VERSION + ".Packet"))
                    .invoke(connection, subtitlePacket);
            }
        } catch (Exception e) {
            // Fallback: utiliser la méthode standard Bukkit (sans timings)
            player.sendTitle(title, subtitle);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[Debug] sendTitle fallback: " + e.getMessage());
            }
        }
    }
    
    /**
     * Échappe les caractères JSON dans une string
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Parse un int de manière sécurisée avec valeur par défaut
     */
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Interface pour les executors d'actions
     */
    @FunctionalInterface
    public interface ActionExecutor {
        void execute(Player player, String args);
    }
}
