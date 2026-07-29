package me.krunsh.kgui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.kgui.actions.ActionManager;
import me.krunsh.kgui.animations.AnimationManager;
import me.krunsh.kgui.api.ContentProviderManager;
import me.krunsh.kgui.commands.DynamicCommandManager;
import me.krunsh.kgui.commands.KguiCommand;
import me.krunsh.kgui.commands.KguiTabCompleter;
import me.krunsh.kgui.conditional.ConditionalManager;
import me.krunsh.kgui.config.ConfigManager;
import me.krunsh.kgui.config.MessageManager;
import me.krunsh.kgui.data.PlayerDataManager;
import me.krunsh.kgui.gui.GuiManager;
import me.krunsh.kgui.hooks.HookManager;
import me.krunsh.kgui.input.ChatInputListener;
import me.krunsh.kgui.input.InputManager;
import me.krunsh.kgui.item.ItemRegistry;
import me.krunsh.kgui.listeners.GuiListener;
import me.krunsh.kgui.listeners.SecurityListener;
import me.krunsh.kgui.menu.MenuManager;
import me.krunsh.kgui.pagination.PaginationManager;
import me.krunsh.kgui.pagination.ScrollManager;
import me.krunsh.kgui.requirements.RequirementManager;
import me.krunsh.kgui.template.TemplateManager;

/**
 * Kgui - Moteur de GUI avancé pour serveurs 1.8.8 Faction/PvP
 * Remplace DeluxeMenus avec des fonctionnalités supérieures
 * 
 * @author Krunsh
 * @version 1.0.0
 */
public class Kgui extends JavaPlugin {

    private static Kgui instance;
    
    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private ItemRegistry itemRegistry;
    private MenuManager menuManager;
    private GuiManager guiManager;
    private HookManager hookManager;
    private AnimationManager animationManager;
    private RequirementManager requirementManager;
    private ActionManager actionManager;
    
    // Phase 2 Managers
    private PaginationManager paginationManager;
    private ScrollManager scrollManager;
    private ConditionalManager conditionalManager;
    private TemplateManager templateManager;
    
    // Phase 3 Managers - Input System
    private InputManager inputManager;
    private ChatInputListener chatInputListener;
    private PlayerDataManager playerDataManager;
    
    // Dynamic Commands
    private DynamicCommandManager dynamicCommandManager;
    
    // API: Content Providers for external plugins
    private ContentProviderManager contentProviderManager;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        
        // Créer les dossiers nécessaires
        createDirectories();
        
        // Initialiser les managers dans l'ordre
        initializeManagers();
        
        // Enregistrer les listeners
        registerListeners();
        
        // Enregistrer les commandes
        registerCommands();
        
        // Enregistrer les commandes dynamiques des menus
        registerDynamicCommands();
        
        // Démarrer la tâche d'auto-refresh
        guiManager.startAutoRefreshTask();
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("v" + getDescription().getVersion() + " enabled in " + loadTime + "ms | " 
            + itemRegistry.getItemCount() + " items, " + menuManager.getMenuCount() + " menus | Hooks: " + hookManager.getEnabledHooksInfo());
    }

    @Override
    public void onDisable() {
        // Arrêter l'auto-refresh
        if (guiManager != null) {
            guiManager.stopAutoRefreshTask();
        }
        
        // Fermer tous les menus ouverts
        if (guiManager != null) {
            guiManager.closeAllMenus();
        }
        
        // Arrêter les animations
        if (animationManager != null) {
            animationManager.stopAll();
        }
        
        getLogger().info("Kgui disabled.");
        instance = null;
    }

    /**
     * Crée les dossiers nécessaires au plugin
     */
    private void createDirectories() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Créer les sous-dossiers silencieusement
        String[] directories = {"items", "menus", "templates", "animations"};
        for (String dir : directories) {
            java.io.File folder = new java.io.File(getDataFolder(), dir);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    /**
     * Initialise tous les managers dans l'ordre correct
     */
    private void initializeManagers() {
        // 1. Config et messages
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        
        // 2. Hooks externes (avant les autres pour les dépendances)
        this.hookManager = new HookManager(this);
        
        // 3. Requirements et Actions (avant items/menus car ils les utilisent)
        this.requirementManager = new RequirementManager(this);
        this.actionManager = new ActionManager(this);
        
        // 4. Phase 2 Managers - Templates, Conditions, Pagination
        this.templateManager = new TemplateManager(this);
        this.templateManager.loadTemplates();
        this.conditionalManager = new ConditionalManager(this);
        this.paginationManager = new PaginationManager(this);
        this.scrollManager = new ScrollManager(this);
        
        // 5. Item Registry
        this.itemRegistry = new ItemRegistry(this);
        
        // 6. Menu Manager
        this.menuManager = new MenuManager(this);
        
        // 7. GUI Manager (gestion des inventaires ouverts)
        this.guiManager = new GuiManager(this);
        
        // 8. Animation Manager
        this.animationManager = new AnimationManager(this);
        
        // 9. Phase 3 - Input System
        this.playerDataManager = new PlayerDataManager(this);
        this.chatInputListener = new ChatInputListener(this);
        this.inputManager = new InputManager(this);
        
        // 10. API: Content Provider Manager for external plugins
        this.contentProviderManager = new ContentProviderManager(this);
    }

    /**
     * Enregistre les listeners d'événements
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SecurityListener(this), this);
        Bukkit.getPluginManager().registerEvents(chatInputListener, this);
        
        // Listener WorldGuard si activé
        if (hookManager.isWorldGuardEnabled()) {
            hookManager.getWorldGuardHook().registerListeners();
        }
        
        // Listener CombatTag si activé
        if (hookManager.isCombatTagEnabled()) {
            hookManager.getCombatTagHook().registerListeners();
        }
    }

    /**
     * Enregistre les commandes
     */
    private void registerCommands() {
        getCommand("kgui").setExecutor(new KguiCommand(this));
        getCommand("kgui").setTabCompleter(new KguiTabCompleter(this));
    }

    /**
     * Enregistre les commandes dynamiques des menus (open_commands)
     */
    private void registerDynamicCommands() {
        this.dynamicCommandManager = new DynamicCommandManager(this);
        dynamicCommandManager.registerAllCommands();
    }

    /**
     * Recharge toute la configuration
     */
    public void reload() {
        getLogger().info("Reloading Kgui...");
        
        // Fermer tous les menus
        guiManager.closeAllMenus();
        
        // Arrêter les animations
        animationManager.stopAll();
        
        // Recharger les configs
        configManager.reload();
        messageManager.reload();
        
        // Recharger les templates
        templateManager.reload();
        
        // Recharger les items et menus
        itemRegistry.reload();
        menuManager.reload();
        
        // Recharger les animations
        animationManager.reload();
        
        // Recharger les commandes dynamiques
        if (dynamicCommandManager != null) {
            dynamicCommandManager.reload();
        }
        
        getLogger().info("Reload complete! " + itemRegistry.getItemCount() + " items, " + menuManager.getMenuCount() + " menus");
    }

    // ==================== GETTERS ====================

    public static Kgui getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public RequirementManager getRequirementManager() {
        return requirementManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public PaginationManager getPaginationManager() {
        return paginationManager;
    }

    public ScrollManager getScrollManager() {
        return scrollManager;
    }

    public ConditionalManager getConditionalManager() {
        return conditionalManager;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public DynamicCommandManager getDynamicCommandManager() {
        return dynamicCommandManager;
    }

    /**
     * Get the ContentProviderManager for registering dynamic content providers.
     * External plugins can use this to register their own pagination content.
     * @return The ContentProviderManager instance
     */
    public ContentProviderManager getContentProviderManager() {
        return contentProviderManager;
    }
}
