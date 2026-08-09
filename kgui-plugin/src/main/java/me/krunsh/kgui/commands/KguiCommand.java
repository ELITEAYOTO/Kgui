package me.krunsh.kgui.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.gui.OpenGui;
import me.krunsh.kgui.menu.MenuData;

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
                handleReload(sender);
                break;
            case "debug":
                handleDebug(sender, args);
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
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("kgui.reload")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }

        long start = System.currentTimeMillis();
        
        // Recharger les configurations
        plugin.reload();
        
        long time = System.currentTimeMillis() - start;
        
        plugin.getMessageManager().send(sender, "config-reloaded",
            "items", String.valueOf(plugin.getItemRegistry().getItems().size()),
            "menus", String.valueOf(plugin.getMenuManager().getMenus().size()),
            "time", String.valueOf(time));
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
        sender.sendMessage("§7- Factions: " + (plugin.getHookManager().isFactionsEnabled() ? "§a✓" : "§c✗"));
        
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
        
        // Kfaction
        if (plugin.getHookManager().isKfactionEnabled()) {
            sender.sendMessage("");
            sender.sendMessage("§6§lFaction:");
            sender.sendMessage("§7- Faction: §a" + plugin.getHookManager().getKfactionHook().getFactionName(target));
            sender.sendMessage("§7- Rôle: §a" + plugin.getHookManager().getKfactionHook().getRole(target));
            sender.sendMessage("§7- Power: §a" + plugin.getHookManager().getKfactionHook().getFactionPower(target));
        }
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
