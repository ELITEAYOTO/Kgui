package me.krunsh.kgui.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Adaptateur Vault par reflection, sans reference forte apres close(). */
public final class VaultHook implements AutoCloseable {
    private Object economy;
    private Object permission;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public VaultHook(Plugin dependency) throws ReflectiveOperationException {
        ClassLoader loader = dependency.getClass().getClassLoader();
        Class<?> economyType = ReflectionAccess.load(loader, "net.milkbowl.vault.economy.Economy");
        RegisteredServiceProvider<?> economyRegistration = Bukkit.getServicesManager().getRegistration((Class) economyType);
        economy = economyRegistration == null ? null : economyRegistration.getProvider();
        try {
            Class<?> permissionType = ReflectionAccess.load(loader, "net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider<?> permissionRegistration = Bukkit.getServicesManager().getRegistration((Class) permissionType);
            permission = permissionRegistration == null ? null : permissionRegistration.getProvider();
        } catch (ClassNotFoundException ignored) {
            permission = null;
        }
    }

    public boolean isEconomyEnabled() { return economy != null; }
    public boolean isPermissionEnabled() { return permission != null; }
    public double getBalance(Player player) { return number(invoke(economy, "getBalance", player)).doubleValue(); }
    public boolean hasBalance(Player player, double amount) {
        return amount >= 0D && Boolean.TRUE.equals(invoke(economy, "has", player, amount));
    }
    public boolean withdraw(Player player, double amount) { return transaction("withdrawPlayer", player, amount); }
    public boolean deposit(Player player, double amount) { return transaction("depositPlayer", player, amount); }
    public String format(double amount) {
        Object result = invoke(economy, "format", amount);
        return result instanceof String ? (String) result : String.valueOf(amount);
    }
    public String getPrimaryGroup(Player player) {
        Object result = invoke(permission, "getPrimaryGroup", player);
        return result instanceof String ? (String) result : "";
    }

    private boolean transaction(String method, Player player, double amount) {
        if (economy == null || !Double.isFinite(amount) || amount <= 0D) return false;
        Object response = invoke(economy, method, player, amount);
        if (response == null) return false;
        Object success = invoke(response, "transactionSuccess");
        return Boolean.TRUE.equals(success);
    }

    private static Object invoke(Object target, String method, Object... arguments) {
        if (target == null) return null;
        try {
            return ReflectionAccess.invoke(target,
                ReflectionAccess.compatibleMethod(target.getClass(), method, arguments), arguments);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Number number(Object value) { return value instanceof Number ? (Number) value : Double.valueOf(0D); }

    @Override
    public void close() {
        economy = null;
        permission = null;
    }
}
