/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 */
package pk.ajneb97.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class OtherListener
implements Listener {
    @EventHandler
    public void fireworkDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (event.getEntity() instanceof Player && damager.getType().name().contains("FIREWORK") && damager.hasMetadata("playerkits")) {
            event.setCancelled(true);
        }
    }
}

