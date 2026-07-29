/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 */
package pk.ajneb97.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class InventoryUtils {
    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            InventoryView view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory", new Class[0]);
            getTopInventory.setAccessible(true);
            return (Inventory)getTopInventory.invoke(view, new Object[0]);
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Inventory getTopInventory(Player player) {
        try {
            InventoryView view = player.getOpenInventory();
            Method getTopInventory = view.getClass().getMethod("getTopInventory", new Class[0]);
            getTopInventory.setAccessible(true);
            return (Inventory)getTopInventory.invoke(view, new Object[0]);
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

