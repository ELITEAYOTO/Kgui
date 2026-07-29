package me.krunsh.kgui.hooks;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.listeners.CombatTagListener;
import net.minelink.ctplus.CombatTagPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Hook pour CombatTagPlus
 */
public class CombatTagHook {

    private final Kgui plugin;
    private CombatTagPlus combatTagPlus;

    public CombatTagHook(Kgui plugin) {
        this.plugin = plugin;
        this.combatTagPlus = (CombatTagPlus) Bukkit.getPluginManager().getPlugin("CombatTagPlus");
    }

    /**
     * Vérifie si le joueur est en combat
     */
    public boolean isInCombat(Player player) {
        if (combatTagPlus == null) return false;
        return combatTagPlus.getTagManager().isTagged(player.getUniqueId());
    }

    /**
     * Obtient le temps restant de combat tag (en secondes)
     */
    public long getRemainingTagTime(Player player) {
        if (combatTagPlus == null) return 0;
        
        if (!isInCombat(player)) return 0;
        
        long expireTime = combatTagPlus.getTagManager().getTag(player.getUniqueId()).getExpireTime();
        long remaining = expireTime - System.currentTimeMillis();
        
        return remaining > 0 ? remaining / 1000 : 0;
    }

    /**
     * Enregistre les listeners de combat
     */
    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatTagListener(plugin), plugin);
    }
}
