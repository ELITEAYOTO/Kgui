/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  net.kyori.adventure.title.Title
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.meta.ItemMeta
 */
package pk.ajneb97.utils;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.InventoryRequirementsManager;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.OtherUtils;

public class MiniMessageUtils {
    public static void messagePrefix(CommandSender sender, String message, boolean isPrefix, String prefix) {
        if (isPrefix) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize((Object)(prefix + message)));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize((Object)message));
        }
    }

    public static void title(Player player, String title, String subtitle, Integer fadeIn, Integer stay, Integer fadeOut) {
        player.showTitle(Title.title((Component)MiniMessage.miniMessage().deserialize((Object)title), (Component)MiniMessage.miniMessage().deserialize((Object)subtitle), (int)fadeIn, (int)stay, (int)fadeOut));
    }

    public static void actionbar(Player player, String message) {
        player.sendActionBar(MiniMessage.miniMessage().deserialize((Object)message));
    }

    public static void message(Player player, String message) {
        player.sendMessage(MiniMessage.miniMessage().deserialize((Object)message));
    }

    public static Inventory createInventory(int slots, String title) {
        return Bukkit.createInventory(null, (int)slots, (Component)MiniMessage.miniMessage().deserialize((Object)title));
    }

    public static void setCommonItemName(KitItem commonItem, ItemMeta meta) {
        commonItem.setName((String)MiniMessage.miniMessage().serialize(meta.displayName()));
    }

    public static void setCommonItemLore(List<String> lore, ItemMeta meta) {
        for (Component line : meta.lore()) {
            lore.add((String)MiniMessage.miniMessage().serialize(line));
        }
    }

    public static void setCommonItemNameLegacy(KitItem commonItem, ItemMeta meta) {
        commonItem.setName(LegacyComponentSerializer.legacyAmpersand().serialize(meta.displayName()));
    }

    public static void setCommonItemLoreLegacy(List<String> lore, ItemMeta meta) {
        for (Component line : meta.lore()) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().serialize(line));
        }
    }

    public static void setItemName(ItemMeta meta, String name) {
        meta.displayName(MiniMessage.miniMessage().deserialize((Object)name).decoration(TextDecoration.ITALIC, false));
    }

    public static void setItemLore(ItemMeta meta, List<String> lore, Player player, PlayerKits2 plugin) {
        ArrayList<Component> loreComponent = new ArrayList<Component>();
        for (int i = 0; i < lore.size(); ++i) {
            String line = OtherUtils.replaceGlobalVariables(lore.get(i), player, plugin);
            loreComponent.add(MiniMessage.miniMessage().deserialize((Object)line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreComponent);
    }

    public static void setRequirementsMessage(ItemMeta meta, String kitName, Player player, InventoryRequirementsManager inventoryRequirementsManager) {
        ArrayList<Component> newLore = new ArrayList<Component>();
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        for (Component line : meta.lore()) {
            String plainText = plainSerializer.serialize(line);
            if (plainText.contains("%kit_requirements_message%")) {
                List<String> message = inventoryRequirementsManager.replaceRequirementsMessageVariable(kitName, player);
                for (String m : message) {
                    newLore.add(MiniMessage.miniMessage().deserialize((Object)m).decoration(TextDecoration.ITALIC, false));
                }
                continue;
            }
            newLore.add(line);
        }
        meta.lore(newLore);
    }
}

