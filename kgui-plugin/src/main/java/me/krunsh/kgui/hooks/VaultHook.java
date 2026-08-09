package me.krunsh.kgui.hooks;

import me.krunsh.kgui.Kgui;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Hook pour Vault (Economy & Permissions)
 */
public class VaultHook {

    private final Kgui plugin;
    private Economy economy;
    private Permission permission;

    public VaultHook(Kgui plugin) {
        this.plugin = plugin;
        setupEconomy();
        setupPermissions();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }

    private void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            permission = rsp.getProvider();
        }
    }

    public boolean isEconomyEnabled() {
        return economy != null;
    }

    public boolean isPermissionEnabled() {
        return permission != null;
    }

    /**
     * Obtient le solde d'un joueur
     */
    public double getBalance(Player player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    /**
     * Vérifie si le joueur a assez d'argent
     */
    public boolean hasBalance(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    /**
     * Retire de l'argent au joueur
     */
    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Donne de l'argent au joueur
     */
    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Formate un montant selon la configuration de l'économie
     */
    public String format(double amount) {
        if (economy == null) return String.valueOf(amount);
        return economy.format(amount);
    }

    /**
     * Obtient le groupe principal du joueur
     */
    public String getPrimaryGroup(Player player) {
        if (permission == null) return "";
        return permission.getPrimaryGroup(player);
    }
}
