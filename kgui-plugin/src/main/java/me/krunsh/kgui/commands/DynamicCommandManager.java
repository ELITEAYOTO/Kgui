package me.krunsh.kgui.commands;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;

/**
 * Gestionnaire de commandes dynamiques pour ouvrir les menus
 * Permet d'utiliser des commandes personnalisées définies dans open_commands
 */
public class DynamicCommandManager {

    private final Kgui plugin;
    private final Map<String, String> commandToMenu = new HashMap<>();
    private final Map<String, DynamicMenuCommand> registeredCommands = new LinkedHashMap<>();
    private CommandMap commandMap;

    public DynamicCommandManager(Kgui plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
    }

    /**
     * Obtient la CommandMap de Bukkit via reflection
     */
    private CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not get CommandMap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Enregistre toutes les commandes dynamiques depuis les menus chargés
     */
    public void registerAllCommands() {
        if (commandMap == null) {
            plugin.getLogger().warning("CommandMap not available, dynamic commands disabled");
            return;
        }

        // Clear previous
        unregisterAllCommands();

        int count = 0;

        // Parcourir tous les menus
        for (MenuData menu : plugin.getMenuManager().getMenus().values()) {
            // Commande unique (open_command)
            String singleCmd = menu.getOpenCommand();
            if (singleCmd != null && !singleCmd.isEmpty()) {
                if (registerCommand(singleCmd, menu.getId())) {
                    count++;
                }
            }

            // Liste de commandes (open_commands)
            List<String> cmds = menu.getOpenCommands();
            if (cmds != null) {
                for (String cmd : cmds) {
                    if (cmd != null && !cmd.isEmpty()) {
                        if (registerCommand(cmd, menu.getId())) {
                            count++;
                        }
                    }
                }
            }
        }

        if (count > 0) {
            plugin.getLogger().info("Registered " + count + " dynamic menu commands");
        }
    }

    /**
     * Enregistre une commande dynamique
     */
    private boolean registerCommand(String commandName, String menuId) {
        // Nettoyer le nom de commande (enlever le / si présent)
        commandName = commandName.toLowerCase().trim();
        if (commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }

        // Vérifier si déjà enregistrée
        if (commandToMenu.containsKey(commandName)) {
            plugin.getLogger().warning("Command /" + commandName + " is already mapped to menu: " + commandToMenu.get(commandName));
            return false;
        }

        // Ne jamais voler une commande ou un alias appartenant déjà à un autre plugin.
        Command existing = commandMap.getCommand(commandName);
        if (existing != null) {
            plugin.getLogger().warning("Command /" + commandName
                    + " already belongs to another plugin; menu " + menuId
                    + " must use a unique open command");
            return false;
        }

        try {
            // Créer une commande personnalisée
            DynamicMenuCommand dynamicCommand = new DynamicMenuCommand(commandName, menuId);
            
            // Enregistrer dans la CommandMap
            commandMap.register("kgui", dynamicCommand);
            
            // Tracker
            commandToMenu.put(commandName, menuId);
            registeredCommands.put(commandName, dynamicCommand);
            
            plugin.getLogger().info("Registered command /" + commandName + " -> menu " + menuId);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register command /" + commandName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Désenregistre toutes les commandes dynamiques
     */
    public void unregisterAllCommands() {
        if (commandMap == null) return;

        try {
            // Accéder aux commandes connues
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            // Supprimer nos commandes
            for (Map.Entry<String, DynamicMenuCommand> registration : registeredCommands.entrySet()) {
                String cmd = registration.getKey();
                DynamicMenuCommand owned = registration.getValue();
                if (knownCommands.get(cmd) == owned) knownCommands.remove(cmd);
                if (knownCommands.get("kgui:" + cmd) == owned) knownCommands.remove("kgui:" + cmd);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not unregister commands: " + e.getMessage());
        }

        commandToMenu.clear();
        registeredCommands.clear();
    }

    /**
     * Recharge les commandes dynamiques
     */
    public void reload() {
        registerAllCommands();
    }

    /**
     * Obtient le menuId pour une commande
     */
    public String getMenuForCommand(String command) {
        return commandToMenu.get(command.toLowerCase());
    }

    /**
     * Commande dynamique qui ouvre un menu
     */
    private class DynamicMenuCommand extends Command {

        private final String menuId;

        protected DynamicMenuCommand(String name, String menuId) {
            super(name);
            this.menuId = menuId;
            this.setDescription("Opens the " + menuId + " menu");
            this.setUsage("/" + name);
            this.setPermission(null); // Permission gérée par le menu lui-même
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;

            // Vérifier que le menu existe toujours
            MenuData menu = plugin.getMenuManager().getMenu(menuId);
            if (menu == null) {
                player.sendMessage("§cMenu not found: " + menuId);
                return true;
            }

            // Vérifier la permission du menu si définie
            String permission = menu.getPermission();
            if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                plugin.getMessageManager().send(player, "no-permission");
                return true;
            }

            // Ouvrir le menu
            plugin.getGuiManager().openMenu(player, menuId);
            return true;
        }
    }
}
