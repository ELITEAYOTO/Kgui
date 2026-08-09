package me.krunsh.kgui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.kgui.actions.ActionManager;
import me.krunsh.kgui.animations.AnimationManager;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.commands.DynamicCommandManager;
import me.krunsh.kgui.commands.KguiCommand;
import me.krunsh.kgui.commands.KguiTabCompleter;
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
import me.krunsh.kgui.menu.MenuReloadResult;
import me.krunsh.kgui.metrics.GuiMetrics;
import me.krunsh.kgui.provider.ProviderEngine;
import me.krunsh.kgui.refresh.GuiInvalidationBus;
import me.krunsh.kgui.refresh.RefreshScheduler;
import me.krunsh.kgui.requirements.RequirementManager;
import me.krunsh.kgui.service.KguiApiProvider;
import me.krunsh.kgui.session.CloseReason;

/**
 * Kgui - Moteur de GUI avancé pour serveurs 1.8.8 Faction/PvP
 * Remplace DeluxeMenus avec des fonctionnalités supérieures
 * 
 * @author Krunsh
 * @version 2.0.0
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
    
    // Phase 3 Managers - Input System
    private InputManager inputManager;
    private ChatInputListener chatInputListener;
    private PlayerDataManager playerDataManager;
    
    // Dynamic Commands
    private DynamicCommandManager dynamicCommandManager;
    
    // API publique V2 publiee via le ServicesManager Bukkit
    private KguiApiProvider apiProvider;
    private GuiMetrics guiMetrics;
    private ProviderEngine providerEngine;
    private RefreshScheduler refreshScheduler;
    private GuiInvalidationBus guiInvalidationBus;

    @Override
    public void onEnable() {
        instance = this;
        this.apiProvider = new KguiApiProvider(this);
        long startTime = System.currentTimeMillis();
        
        // Créer les dossiers nécessaires
        createDirectories();
        
        // Initialiser les managers dans l'ordre
        initializeManagers();

        // Publier l'API seulement quand tous ses services sont disponibles
        registerApi();
        
        // Enregistrer les listeners
        registerListeners();
        
        // Enregistrer les commandes
        registerCommands();
        
        // Enregistrer les commandes dynamiques des menus
        registerDynamicCommands();
        
        refreshScheduler.start();
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("v" + getDescription().getVersion() + " enabled in " + loadTime + "ms | " 
            + itemRegistry.getItemCount() + " items, " + menuManager.getMenuCount() + " menus | Hooks: " + hookManager.getEnabledHooksInfo());
    }

    @Override
    public void onDisable() {
        shutdownApi();

        if (refreshScheduler != null) refreshScheduler.close();
        
        // Fermer tous les menus ouverts
        if (guiManager != null) {
            guiManager.closeAllMenus(CloseReason.DISABLE);
        }
        
        // Arrêter les animations
        if (animationManager != null) {
            animationManager.stopAll();
        }
        if (inputManager != null) inputManager.cleanup();
        if (chatInputListener != null) chatInputListener.cleanup();
        if (playerDataManager != null) playerDataManager.cleanup();
        if (guiInvalidationBus != null) guiInvalidationBus.close();
        if (providerEngine != null) providerEngine.close();
        if (hookManager != null) hookManager.close();
        
        getLogger().info("Kgui disabled.");
        instance = null;
    }

    private void registerApi() {
        Bukkit.getServicesManager().register(KguiApi.class, apiProvider, this, ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(apiProvider, this);
        getLogger().info("Kgui API " + apiProvider.getApiVersion() + " registered");
    }

    private void shutdownApi() {
        Bukkit.getServicesManager().unregisterAll(this);
        if (apiProvider != null) {
            apiProvider.close();
            apiProvider = null;
        }
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
        this.requirementManager = new RequirementManager(this, apiProvider);
        this.actionManager = new ActionManager(this, apiProvider);
        
        // 4. Item Registry
        this.itemRegistry = new ItemRegistry(this);
        
        // 5. Menu Manager
        this.menuManager = new MenuManager(this);

        this.guiMetrics = new GuiMetrics();
        int providerCacheEntries = configManager.getConfig().getInt("performance.provider_cache_entries", 2048);
        long slowProviderNanos = (long) (configManager.getConfig()
            .getDouble("performance.provider_slow_warning_ms", 2.0D) * 1_000_000.0D);
        this.providerEngine = new ProviderEngine(this, apiProvider, guiMetrics,
            providerCacheEntries, slowProviderNanos);
        
        // 6. GUI Manager (gestion des inventaires ouverts)
        this.guiManager = new GuiManager(this);
        this.refreshScheduler = new RefreshScheduler(this, guiManager::performScheduledRefresh, guiMetrics,
            configManager.getConfig().getInt("performance.max_pending_refreshes", 2048),
            configManager.getConfig().getInt("performance.max_refreshes_per_tick", 32),
            configManager.getConfig().getLong("performance.refresh_budget_nanos", 2_000_000L));
        this.guiInvalidationBus = new GuiInvalidationBus(refreshScheduler, providerEngine, guiMetrics);
        
        // 7. Animation Manager
        this.animationManager = new AnimationManager(this);
        
        // 9. Phase 3 - Input System
        this.playerDataManager = new PlayerDataManager(this);
        this.chatInputListener = new ChatInputListener(this);
        this.inputManager = new InputManager(this);
        
    }

    /**
     * Enregistre les listeners d'événements
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SecurityListener(this), this);
        Bukkit.getPluginManager().registerEvents(chatInputListener, this);
        Bukkit.getPluginManager().registerEvents(hookManager, this);
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
    public MenuReloadResult reload() {
        getLogger().info("Reloading Kgui...");

        // Refuser avant tout effet de bord si le graphe de menus est invalide.
        MenuReloadResult preflight = menuManager.validate();
        if (!preflight.isSuccess()) {
            getLogger().warning("Reload aborted: invalid menu configuration; live state retained");
            return preflight;
        }
        
        // Fermer tous les menus
        guiManager.closeAllMenus();
        inputManager.cleanup();
        chatInputListener.cleanup();
        playerDataManager.cleanup();
        
        // Arrêter les animations
        animationManager.stopAll();
        
        // Recharger les configs
        configManager.reload();
        messageManager.reload();
        
        // Recharger les items et menus
        itemRegistry.reload();
        MenuReloadResult menuResult = menuManager.reload();
        
        // Recharger les animations
        animationManager.reload();
        
        // Recharger les commandes dynamiques
        if (dynamicCommandManager != null) {
            dynamicCommandManager.reload();
        }
        
        getLogger().info("Reload complete! " + itemRegistry.getItemCount() + " items, " + menuManager.getMenuCount() + " menus");
        return menuResult;
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

    public KguiApi getApi() {
        return apiProvider;
    }

    public KguiApiProvider getApiProvider() { return apiProvider; }
    public GuiMetrics getGuiMetrics() { return guiMetrics; }
    public ProviderEngine getProviderEngine() { return providerEngine; }
    public RefreshScheduler getRefreshScheduler() { return refreshScheduler; }
    public GuiInvalidationBus getGuiInvalidationBus() { return guiInvalidationBus; }
}
