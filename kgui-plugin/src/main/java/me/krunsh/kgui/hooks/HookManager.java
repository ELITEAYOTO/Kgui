package me.krunsh.kgui.hooks;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import me.krunsh.kgui.Kgui;

/** Cycle de vie reactif des soft-dependencies. */
public final class HookManager implements Listener, AutoCloseable {
    private final Kgui plugin;
    private final AtomicBoolean closed = new AtomicBoolean();

    private PlaceholderAPIHook placeholderAPIHook;
    private VaultHook vaultHook;
    private PlayerPointsHook playerPointsHook;
    private WorldGuardHook worldGuardHook;
    private CombatTagHook combatTagHook;
    private ProtocolLibHook protocolLibHook;
    private HeadDatabaseHook headDatabaseHook;

    public HookManager(Kgui plugin) {
        this.plugin = plugin;
        initializeEnabledHooks();
    }

    private void initializeEnabledHooks() {
        for (Plugin dependency : Bukkit.getPluginManager().getPlugins()) {
            if (dependency.isEnabled()) attach(dependency);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event == null || closed.get()) return;
        attach(event.getPlugin());
        if (vaultHook == null || !vaultHook.isEconomyEnabled()) refreshVault();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event == null || closed.get()) return;
        Plugin dependency = event.getPlugin();
        detach(dependency.getName());
        if (vaultHook != null) {
            closeQuietly(vaultHook);
            vaultHook = null;
            if (!"vault".equalsIgnoreCase(dependency.getName())) {
                Bukkit.getScheduler().runTask(plugin, this::refreshVault);
            }
        }
    }

    private synchronized void attach(Plugin dependency) {
        if (dependency == null || closed.get() || !dependency.isEnabled()) return;
        String name = dependency.getName().toLowerCase(Locale.ROOT);
        try {
            switch (name) {
                case "placeholderapi":
                    if (placeholderAPIHook == null) placeholderAPIHook = new PlaceholderAPIHook(dependency);
                    break;
                case "vault":
                    if (vaultHook == null) vaultHook = new VaultHook(dependency);
                    break;
                case "playerpoints":
                    if (playerPointsHook == null) playerPointsHook = new PlayerPointsHook(dependency);
                    break;
                case "worldguard":
                    if (worldGuardHook == null) {
                        worldGuardHook = new WorldGuardHook(plugin, dependency);
                        worldGuardHook.registerListeners();
                    }
                    break;
                case "combattagplus":
                    if (combatTagHook == null) {
                        combatTagHook = new CombatTagHook(plugin, dependency);
                        combatTagHook.registerListeners();
                    }
                    break;
                case "protocollib":
                    if (protocolLibHook == null) protocolLibHook = new ProtocolLibHook(plugin, dependency);
                    break;
                case "headdatabase":
                    if (headDatabaseHook == null) headDatabaseHook = new HeadDatabaseHook(plugin, dependency);
                    break;
                default:
                    return;
            }
            plugin.getLogger().info("Hook attached: " + dependency.getName());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            detach(name);
            plugin.getLogger().warning("Failed to attach " + dependency.getName() + ": " + error.getMessage());
        }
    }

    private synchronized void detach(String pluginName) {
        if (pluginName == null) return;
        switch (pluginName.toLowerCase(Locale.ROOT)) {
            case "placeholderapi": closeQuietly(placeholderAPIHook); placeholderAPIHook = null; break;
            case "vault": closeQuietly(vaultHook); vaultHook = null; break;
            case "playerpoints": closeQuietly(playerPointsHook); playerPointsHook = null; break;
            case "worldguard": closeQuietly(worldGuardHook); worldGuardHook = null; break;
            case "combattagplus": closeQuietly(combatTagHook); combatTagHook = null; break;
            case "protocollib": closeQuietly(protocolLibHook); protocolLibHook = null; break;
            case "headdatabase": closeQuietly(headDatabaseHook); headDatabaseHook = null; break;
            default:
        }
    }

    private synchronized void refreshVault() {
        if (closed.get()) return;
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) return;
        closeQuietly(vaultHook);
        vaultHook = null;
        attach(vault);
    }

    public String getEnabledHooksInfo() {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (isPlaceholderAPIEnabled()) names.add("PAPI");
        if (isVaultEnabled()) names.add("Vault");
        if (isPlayerPointsEnabled()) names.add("PlayerPoints");
        if (isWorldGuardEnabled()) names.add("WorldGuard");
        if (isCombatTagEnabled()) names.add("CombatTagPlus");
        if (isProtocolLibEnabled()) names.add("ProtocolLib");
        if (isHeadDatabaseEnabled()) names.add("HeadDatabase");
        return names.isEmpty() ? "None" : String.join(", ", names);
    }

    public boolean isPlaceholderAPIEnabled() { return placeholderAPIHook != null; }
    public PlaceholderAPIHook getPlaceholderAPIHook() { return placeholderAPIHook; }
    public boolean isVaultEnabled() { return vaultHook != null && vaultHook.isEconomyEnabled(); }
    public VaultHook getVaultHook() { return vaultHook; }
    public boolean isPlayerPointsEnabled() { return playerPointsHook != null && playerPointsHook.isEnabled(); }
    public PlayerPointsHook getPlayerPointsHook() { return playerPointsHook; }
    public boolean isWorldGuardEnabled() { return worldGuardHook != null; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public boolean isCombatTagEnabled() { return combatTagHook != null; }
    public CombatTagHook getCombatTagHook() { return combatTagHook; }
    public boolean isProtocolLibEnabled() { return protocolLibHook != null; }
    public ProtocolLibHook getProtocolLibHook() { return protocolLibHook; }
    public boolean isHeadDatabaseEnabled() { return headDatabaseHook != null; }
    public HeadDatabaseHook getHeadDatabaseHook() { return headDatabaseHook; }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        closeQuietly(placeholderAPIHook); placeholderAPIHook = null;
        closeQuietly(vaultHook); vaultHook = null;
        closeQuietly(playerPointsHook); playerPointsHook = null;
        closeQuietly(worldGuardHook); worldGuardHook = null;
        closeQuietly(combatTagHook); combatTagHook = null;
        closeQuietly(protocolLibHook); protocolLibHook = null;
        closeQuietly(headDatabaseHook); headDatabaseHook = null;
    }

    private static void closeQuietly(Object value) {
        if (!(value instanceof AutoCloseable)) return;
        try { ((AutoCloseable) value).close(); }
        catch (Exception ignored) { }
    }
}
