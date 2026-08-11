package me.krunsh.kgui.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.gui.OpenGui;
import me.krunsh.kgui.metrics.GuiMetrics;
import me.krunsh.kgui.session.PlayerGuiSession;
import me.krunsh.kgui.menu.MenuData;
import me.krunsh.kgui.menu.MenuReloadResult;
import me.krunsh.kgui.menu.compiler.CompiledMenu;
import me.krunsh.kgui.menu.compiler.MenuDiagnostic;

/**
 * Commande principale /kgui
 */
public class KguiCommand implements CommandExecutor {

    private final Kgui plugin;

    public KguiCommand(Kgui plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Pas d'arguments = aide
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open":
                handleOpen(sender, args);
                break;
            case "reload":
                handleReload(sender, args);
                break;
            case "validate":
                handleValidate(sender);
                break;
            case "dump":
                handleDump(sender, args);
                break;
            case "debug":
                handleDebug(sender, args);
                break;
            case "diagnose":
                handleDiagnose(sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * /kgui open <menu> [player]
     */
    private void handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kgui.open")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            plugin.getMessageManager().send(sender, "usage-open");
            return;
        }

        String menuId = args[1];
        Player target;

        if (args.length >= 3) {
            // Ouvrir pour un autre joueur
            if (!sender.hasPermission("kgui.open.others")) {
                plugin.getMessageManager().send(sender, "no-permission");
                return;
            }
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                plugin.getMessageManager().send(sender, "player-not-found", "player", args[2]);
                return;
            }
        } else {
            // Ouvrir pour soi-même
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().send(sender, "player-only");
                return;
            }
            target = (Player) sender;
        }

        // Vérifier que le menu existe
        MenuData menu = plugin.getMenuManager().getMenu(menuId);
        if (menu == null) {
            plugin.getMessageManager().send(sender, "menu-not-found", "menu", menuId);
            return;
        }

        // Ouvrir le menu
        boolean success = plugin.getGuiManager().openMenu(target, menuId);
        if (success && sender != target) {
            plugin.getMessageManager().send(sender, "menu-opened-for",
                "menu", menuId,
                "player", target.getName());
        }
    }

    /**
     * /kgui reload
     */
    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kgui.reload")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }

        long start = System.currentTimeMillis();
        
        MenuReloadResult result;
        if (args.length >= 2) {
            result = plugin.getMenuManager().reload(args[1]);
            if (result.isSuccess() && plugin.getDynamicCommandManager() != null) {
                plugin.getDynamicCommandManager().reload();
            }
        } else {
            result = plugin.reload();
        }
        
        long time = System.currentTimeMillis() - start;
        
        if (result.isSuccess()) {
            String publication = result.getTarget() == null
                ? "tous les menus ont été publiés" : "le menu '" + result.getTarget() + "' a été publié";
            sender.sendMessage(ChatColor.GREEN + "Kgui: " + publication + " en " + time + " ms. "
                + ChatColor.GRAY + "(" + result.getWarningCount() + " avertissement(s))");
        } else {
            sender.sendMessage(ChatColor.RED + "Rechargement refusé: la configuration précédente reste active.");
            sendDiagnostics(sender, result, 20);
        }
    }

    private void handleValidate(CommandSender sender) {
        if (!sender.hasPermission("kgui.reload")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }
        long start = System.currentTimeMillis();
        MenuReloadResult result = plugin.getMenuManager().validate();
        long time = System.currentTimeMillis() - start;
        if (result.isSuccess()) {
            sender.sendMessage(ChatColor.GREEN + "Validation réussie: " + result.getMenuCount() + " menu(s), "
                + result.getWarningCount() + " avertissement(s), " + time + " ms. Aucun cache modifié.");
        } else {
            sender.sendMessage(ChatColor.RED + "Validation échouée: " + result.getErrorCount() + " erreur(s), "
                + result.getWarningCount() + " avertissement(s). Aucun cache modifié.");
        }
        sendDiagnostics(sender, result, 20);
    }

    private void handleDump(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kgui.debug")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /kgui dump <menu>");
            return;
        }
        CompiledMenu menu = plugin.getMenuManager().getCompiledMenu(args[1]);
        if (menu == null) {
            plugin.getMessageManager().send(sender, "menu-not-found", "menu", args[1]);
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "--- Kgui dump: " + menu.getId() + " (schéma "
            + menu.getSchemaVersion() + ") ---");
        for (String line : menu.dump().split("\\r?\\n")) sender.sendMessage(ChatColor.GRAY + line);
    }

    private void sendDiagnostics(CommandSender sender, MenuReloadResult result, int limit) {
        int emitted = 0;
        for (MenuDiagnostic diagnostic : result.getDiagnostics()) {
            if (emitted++ < limit) {
                ChatColor color = diagnostic.isError() ? ChatColor.RED : ChatColor.YELLOW;
                sender.sendMessage(color + diagnostic.format());
            } else {
                plugin.getLogger().warning(diagnostic.format());
            }
        }
        if (result.getDiagnostics().size() > limit) {
            sender.sendMessage(ChatColor.GRAY + "... " + (result.getDiagnostics().size() - limit)
                + " diagnostic(s) supplémentaire(s) dans la console.");
        }
    }

    /**
     * /kgui debug [player]
     */
    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kgui.debug")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.getMessageManager().send(sender, "player-not-found", "player", args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }

        // Afficher les informations de debug
        sender.sendMessage("§6§l[Kgui Debug] §7Informations pour §e" + target.getName());
        sender.sendMessage("");
        
        // Menu ouvert
        OpenGui openGui = plugin.getGuiManager().getOpenGui(target);
        if (openGui != null) {
            sender.sendMessage("§7Menu ouvert: §a" + openGui.getMenuData().getId());
            sender.sendMessage("§7Page: §a" + openGui.getCurrentPage() + "/" + openGui.getTotalPages());
            sender.sendMessage("§7Position scroll: §a" + openGui.getScrollPosition());
            if (openGui instanceof PlayerGuiSession) {
                PlayerGuiSession session = (PlayerGuiSession) openGui;
                sender.sendMessage("§7Mode viewport: §a" + session.getViewport().getMode());
                sender.sendMessage("§7Révision provider: §a" + (session.getProviderSnapshot() == null
                    ? "aucune" : session.getProviderSnapshot().getRevision()));
            }
        } else {
            sender.sendMessage("§7Menu ouvert: §cAucun");
        }
        
        sender.sendMessage("");
        
        // Hooks
        sender.sendMessage("§6§lHooks:");
        sender.sendMessage("§7- PlaceholderAPI: " + (plugin.getHookManager().isPlaceholderAPIEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- Vault: " + (plugin.getHookManager().isVaultEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- PlayerPoints: " + (plugin.getHookManager().isPlayerPointsEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- WorldGuard: " + (plugin.getHookManager().isWorldGuardEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- CombatTag: " + (plugin.getHookManager().isCombatTagEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- ProtocolLib: " + (plugin.getHookManager().isProtocolLibEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- HeadDatabase: " + (plugin.getHookManager().isHeadDatabaseEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7- Kfaction API: " + (plugin.getKfactionIntegrationManager().isReady()
            ? "§aREADY_2_3" : "§c" + plugin.getKfactionIntegrationManager().getState()));

        GuiMetrics.Snapshot metrics = plugin.getGuiMetrics().snapshot();
        sender.sendMessage("");
        sender.sendMessage("§6§lRuntime V2:");
        sender.sendMessage("§7- Providers: §a" + metrics.providerCalls + " appels, "
            + metrics.providerCacheHits + " hits, " + metrics.providerErrors + " erreurs");
        double providerAverageMs = metrics.providerCalls == 0L ? 0.0D
            : metrics.providerNanos / 1_000_000.0D / metrics.providerCalls;
        double renderAverageMs = metrics.renders == 0L ? 0.0D
            : metrics.renderNanos / 1_000_000.0D / metrics.renders;
        sender.sendMessage("§7- Temps moyen provider/rendu: §a"
            + String.format("%.3f/%.3f ms", providerAverageMs, renderAverageMs));
        sender.sendMessage("§7- Rendus/diffs: §a" + metrics.renders + "/" + metrics.diffRuns
            + " §7(" + metrics.changedSlots + " slots modifiés)");
        sender.sendMessage("§7- Réouvertures/invalidation: §a" + metrics.inventoryReopens
            + "/" + metrics.invalidations + " §7| cache provider: §a"
            + plugin.getProviderEngine().cacheSize());
        sender.sendMessage("§7- File refresh: §a" + plugin.getRefreshScheduler().pendingCount()
            + " §7| périodiques: §a" + plugin.getRefreshScheduler().periodicCount()
            + " §7| sessions indexées: §a" + plugin.getGuiInvalidationBus().indexedSessions());
        sender.sendMessage("§7- Refresh demandés/coalescés: §a" + metrics.refreshQueued
            + "/" + metrics.refreshCoalesced);
        
        // Économie
        if (plugin.getHookManager().isVaultEnabled()) {
            sender.sendMessage("");
            sender.sendMessage("§6§lÉconomie:");
            sender.sendMessage("§7- Balance: §a" + plugin.getHookManager().getVaultHook().getBalance(target) + "$");
        }
        if (plugin.getHookManager().isPlayerPointsEnabled()) {
            sender.sendMessage("§7- Points: §a" + plugin.getHookManager().getPlayerPointsHook().getPoints(target));
        }
        
        // Combat
        if (plugin.getHookManager().isCombatTagEnabled()) {
            sender.sendMessage("");
            sender.sendMessage("§6§lCombat:");
            sender.sendMessage("§7- En combat: " + (plugin.getHookManager().getCombatTagHook().isInCombat(target) ? "§cOui" : "§aNon"));
        }
        
    }

    /** Etat machine-readable pour le harness et diagnostic console sans joueur connecte. */
    private void handleDiagnose(CommandSender sender) {
        if (!sender.hasPermission("kgui.debug")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }
        MenuReloadResult validation = plugin.getMenuManager().validate();
        GuiMetrics.Snapshot metrics = plugin.getGuiMetrics().snapshot();
        sender.sendMessage("[Kgui Diagnose] status=" + (validation.isSuccess() ? "OK" : "ERROR")
            + " version=" + plugin.getDescription().getVersion()
            + " menus=" + validation.getMenuCount()
            + " warnings=" + validation.getWarningCount()
            + " errors=" + validation.getErrorCount());
        sender.sendMessage("[Kgui Diagnose] sessions=" + plugin.getGuiManager().getActiveSessionCount()
            + " indexed=" + plugin.getGuiInvalidationBus().indexedSessions()
            + " refresh.pending=" + plugin.getRefreshScheduler().pendingCount()
            + " refresh.periodic=" + plugin.getRefreshScheduler().periodicCount()
            + " provider.cache=" + plugin.getProviderEngine().cacheSize());
        sender.sendMessage("[Kgui Diagnose] provider.calls=" + metrics.providerCalls
            + " provider.hits=" + metrics.providerCacheHits
            + " provider.errors=" + metrics.providerErrors
            + " placeholders=" + metrics.placeholderResolutions
            + " render.slots=" + metrics.renderedSlots
            + " sent.slots=" + metrics.slotsSent);
        sender.sendMessage("[Kgui Diagnose] refresh.queued=" + metrics.refreshQueued
            + " refresh.coalesced=" + metrics.refreshCoalesced
            + " refresh.rejected=" + metrics.refreshRejected
            + " refresh.executed=" + metrics.refreshExecuted
            + " refresh.max_queue=" + metrics.maxRefreshQueue);
        sender.sendMessage("[Kgui Diagnose] clicks.observed=" + metrics.clicksObserved
            + " clicks.received=" + metrics.clicksReceived
            + " clicks.route_rejected=" + metrics.clicksRouteRejected
            + " clicks.authority_rejected=" + metrics.clicksAuthorityRejected
            + " clicks.session_rejected=" + metrics.clicksSessionRejected
            + " clicks.item_rejected=" + metrics.clicksItemRejected
            + " clicks.actions=" + metrics.clickActions);
        sender.sendMessage("[Kgui Diagnose] " + latency("open.static", metrics.staticOpenLatency)
            + " " + latency("open.dynamic", metrics.dynamicOpenLatency));
        sender.sendMessage("[Kgui Diagnose] " + latency("provider", metrics.providerLatency)
            + " " + latency("render", metrics.renderLatency));
        sender.sendMessage("[Kgui Diagnose] " + latency("scheduler", metrics.schedulerLatency)
            + " scheduler.avg_ms=" + millis(metrics.schedulerTicks == 0L ? 0L
                : metrics.schedulerNanos / metrics.schedulerTicks));
        sender.sendMessage("[Kgui Diagnose] kfaction=" + plugin.getKfactionIntegrationManager().getState()
            + " papi=" + plugin.getHookManager().isPlaceholderAPIEnabled()
            + " vault=" + plugin.getHookManager().isVaultEnabled()
            + " protocol_lib=" + plugin.getHookManager().isProtocolLibEnabled());
        sendDiagnostics(sender, validation, 20);
    }

    private static String latency(String name, GuiMetrics.LatencySnapshot value) {
        return name + ".count=" + value.count
            + " " + name + ".p50_ms=" + millis(value.p50Nanos)
            + " " + name + ".p95_ms=" + millis(value.p95Nanos)
            + " " + name + ".p99_ms=" + millis(value.p99Nanos)
            + " " + name + ".max_ms=" + millis(value.maxNanos);
    }

    private static String millis(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    /**
     * /kgui list
     */
    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("kgui.list")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }

        sender.sendMessage("§6§l[Kgui] §7Menus disponibles:");
        sender.sendMessage("");
        
        // Si le sender est un Player, on peut parser les placeholders
        Player playerSender = (sender instanceof Player) ? (Player) sender : null;
        
        for (MenuData menu : plugin.getMenuManager().getMenus().values()) {
            String permission = "";
            if (menu.getConfig() != null) {
                permission = menu.getConfig().getString("open-requirements.permission", "");
            } else if (menu.getPermission() != null) {
                permission = menu.getPermission();
            }
            String permDisplay = permission.isEmpty() ? "§aAucune" : "§c" + permission;
            
            // Parser les placeholders dans le titre si c'est un joueur
            String title = menu.getTitle();
            if (playerSender != null) {
                title = plugin.getGuiManager().parsePlaceholders(playerSender, title);
            }
            title = me.krunsh.kgui.utils.ColorUtils.colorize(title);
            
            sender.sendMessage("§7- §e" + menu.getId() + " §7(" + title + "§7) - Permission: " + permDisplay);
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7Total: §e" + plugin.getMenuManager().getMenus().size() + " §7menus");
    }

    /**
     * /kgui info <menu>
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kgui.info")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /kgui info <menu>");
            return;
        }

        String menuId = args[1];
        MenuData menu = plugin.getMenuManager().getMenu(menuId);
        
        if (menu == null) {
            plugin.getMessageManager().send(sender, "menu-not-found", "menu", menuId);
            return;
        }

        sender.sendMessage("§6§l[Kgui] §7Informations sur §e" + menuId);
        sender.sendMessage("");
        sender.sendMessage("§7Titre: §f" + menu.getTitle());
        sender.sendMessage("§7Taille: §a" + menu.getSize() + " §7slots");
        sender.sendMessage("§7Type: §a" + menu.getType().name());
        sender.sendMessage("§7Items: §a" + menu.getItems().size());
        
        if (menu.getTemplate() != null) {
            sender.sendMessage("§7Template: §e" + menu.getTemplate());
        }
        
        if (menu.isPaginated()) {
            sender.sendMessage("§7Pagination: §aActivée §7(slots " + menu.getPaginationSlots().size() + ")");
        }
        
        if (menu.isScrollable()) {
            sender.sendMessage("§7Scroll: §aActivé");
        }
    }

    /**
     * Affiche l'aide
     */
    private void sendHelp(CommandSender sender) {
        plugin.getMessageManager().sendHelp(sender);
    }
}
