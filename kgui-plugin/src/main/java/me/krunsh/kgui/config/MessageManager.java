package me.krunsh.kgui.config;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire des messages
 */
public class MessageManager {

    private final Kgui plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private String prefix;
    
    // Cache des messages pour un accès rapide
    private final Map<String, String> messageCache = new HashMap<>();

    public MessageManager(Kgui plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Charge les messages
     */
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Vérifier les valeurs par défaut
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defConfigStream)
            );
            messagesConfig.setDefaults(defConfig);
        }
        
        // Charger le prefix
        prefix = ColorUtils.colorize(messagesConfig.getString("prefix", "&8[&6Kgui&8] "));
        
        // Peupler le cache
        populateCache();
    }

    /**
     * Peuple le cache avec les messages fréquemment utilisés
     */
    private void populateCache() {
        messageCache.clear();
        
        // Messages généraux
        cacheMessage("no-permission");
        cacheMessage("player-not-found");
        cacheMessage("menu-not-found");
        cacheMessage("player-only");
        cacheMessage("usage-open");
        cacheMessage("config-reloaded");
        
        // Messages de menu
        cacheMessage("menu-opened");
        cacheMessage("menu-opened-for");
        cacheMessage("menu-closed");
        cacheMessage("not-allowed-world");
        cacheMessage("not-allowed-region");
        
        // Messages de combat
        cacheMessage("blocked-in-combat");
        cacheMessage("closed-combat");
        cacheMessage("cooldown-after-combat");
        
        // Messages de security
        cacheMessage("item-removed");
    }

    /**
     * Cache un message
     */
    private void cacheMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message != null) {
            messageCache.put(path, ColorUtils.colorize(message));
        }
    }

    /**
     * Recharge les messages
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Obtient un message brut (avec couleurs)
     */
    public String get(String path) {
        // Vérifier le cache d'abord
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }
        
        // Sinon, charger depuis la config
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "&cMessage missing: " + path;
        }
        return ColorUtils.colorize(message);
    }

    /**
     * Obtient un message avec remplacement de placeholders
     */
    public String get(String path, Map<String, String> placeholders) {
        String message = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }

    /**
     * Obtient un message avec un seul placeholder
     */
    public String get(String path, String placeholder, String value) {
        return get(path).replace("%" + placeholder + "%", value);
    }

    /**
     * Obtient une liste de messages
     */
    public List<String> getList(String path) {
        List<String> messages = messagesConfig.getStringList(path);
        messages.replaceAll(ColorUtils::colorize);
        return messages;
    }

    /**
     * Envoie un message à un CommandSender avec prefix
     */
    public void send(CommandSender sender, String path) {
        sender.sendMessage(prefix + get(path));
    }

    /**
     * Envoie un message à un CommandSender avec prefix et placeholders
     */
    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefix + get(path, placeholders));
    }

    /**
     * Envoie un message à un CommandSender avec prefix et un placeholder
     */
    public void send(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(prefix + get(path, placeholder, value));
    }

    /**
     * Envoie un message à un CommandSender avec prefix et plusieurs placeholders
     * Format: placeholder1, value1, placeholder2, value2, ...
     */
    public void send(CommandSender sender, String path, String... replacements) {
        if (replacements.length == 0) {
            send(sender, path);
            return;
        }
        
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < replacements.length - 1; i += 2) {
            placeholders.put(replacements[i], replacements[i + 1]);
        }
        send(sender, path, placeholders);
    }

    /**
     * Envoie un message à un joueur sans prefix
     */
    public void sendRaw(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    /**
     * Envoie une liste de messages
     */
    public void sendList(CommandSender sender, String path) {
        for (String message : getList(path)) {
            sender.sendMessage(message);
        }
    }

    /**
     * Envoie l'aide des commandes
     */
    public void sendHelp(CommandSender sender) {
        sender.sendMessage(get("help.header"));
        sendList(sender, "help.commands");
        sender.sendMessage(get("help.footer"));
    }

    /**
     * Obtient le prefix
     */
    public String getPrefix() {
        return prefix;
    }
}
