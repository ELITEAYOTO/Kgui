package me.krunsh.kgui.commands;

import me.krunsh.kgui.Kgui;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer pour /kgui
 */
public class KguiTabCompleter implements TabCompleter {

    private final Kgui plugin;

    public KguiTabCompleter(Kgui plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Sous-commandes
            List<String> subCommands = new ArrayList<>();
            
            if (sender.hasPermission("kgui.open")) subCommands.add("open");
            if (sender.hasPermission("kgui.reload")) subCommands.add("reload");
            if (sender.hasPermission("kgui.debug")) subCommands.add("debug");
            if (sender.hasPermission("kgui.list")) subCommands.add("list");
            if (sender.hasPermission("kgui.info")) subCommands.add("info");
            
            return filterCompletions(subCommands, args[0]);
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "open":
                case "info":
                    // Liste des menus
                    if (sender.hasPermission("kgui.open") || sender.hasPermission("kgui.info")) {
                        return filterCompletions(new ArrayList<>(plugin.getMenuManager().getMenus().keySet()), args[1]);
                    }
                    break;
                    
                case "debug":
                    // Liste des joueurs
                    if (sender.hasPermission("kgui.debug")) {
                        return filterCompletions(getOnlinePlayerNames(), args[1]);
                    }
                    break;
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("open") && sender.hasPermission("kgui.open.others")) {
                // Liste des joueurs pour /kgui open <menu> <player>
                return filterCompletions(getOnlinePlayerNames(), args[2]);
            }
        }

        return completions;
    }

    /**
     * Filtre les complétions selon le début tapé
     */
    private List<String> filterCompletions(List<String> options, String input) {
        if (input.isEmpty()) return options;
        
        String lowerInput = input.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lowerInput))
            .collect(Collectors.toList());
    }

    /**
     * Récupère les noms des joueurs en ligne
     */
    private List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }
}
