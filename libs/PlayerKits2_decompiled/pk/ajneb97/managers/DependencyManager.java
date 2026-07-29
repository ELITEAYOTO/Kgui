/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.RegisteredServiceProvider
 */
package pk.ajneb97.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import pk.ajneb97.PlayerKits2;

public class DependencyManager {
    private PlayerKits2 plugin;
    private boolean isPlaceholderAPI;
    private Economy vaultEconomy;
    private boolean isPaper;

    public DependencyManager(PlayerKits2 plugin) {
        RegisteredServiceProvider rsp;
        this.plugin = plugin;
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.isPlaceholderAPI = true;
        }
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null && (rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class)) != null) {
            this.vaultEconomy = (Economy)rsp.getProvider();
        }
        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            this.isPaper = true;
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public boolean isPlaceholderAPI() {
        return this.isPlaceholderAPI;
    }

    public Economy getVaultEconomy() {
        return this.vaultEconomy;
    }

    public boolean isPaper() {
        return this.isPaper;
    }
}

