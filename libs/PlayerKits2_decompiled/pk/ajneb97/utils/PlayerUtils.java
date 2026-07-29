/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package pk.ajneb97.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.utils.ServerVersion;

public class PlayerUtils {
    public static ItemStack[] getAllInventoryContents(Player player) {
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        if (!serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_9_R1)) {
            ItemStack[] armorContents;
            int n;
            ItemStack[] normalContents;
            ItemStack[] contents = new ItemStack[40];
            int slot = 0;
            ItemStack[] itemStackArray = normalContents = player.getInventory().getContents();
            int n2 = itemStackArray.length;
            for (n = 0; n < n2; ++n) {
                ItemStack item;
                contents[slot] = item = itemStackArray[n];
                ++slot;
            }
            ItemStack[] itemStackArray2 = armorContents = player.getInventory().getArmorContents();
            n = itemStackArray2.length;
            for (int i = 0; i < n; ++i) {
                ItemStack item;
                contents[slot] = item = itemStackArray2[i];
                ++slot;
            }
            return contents;
        }
        return player.getInventory().getContents();
    }

    public static int getUsedSlots(Player player) {
        ItemStack[] contents = null;
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        contents = !serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_9_R1) ? player.getInventory().getContents() : player.getInventory().getStorageContents();
        int usedSlots = 0;
        for (int i = 0; i < contents.length; ++i) {
            if (contents[i] == null || contents[i].getType().equals((Object)Material.AIR)) continue;
            ++usedSlots;
        }
        return usedSlots;
    }

    public static boolean isPlayerKitsAdmin(CommandSender sender) {
        return sender.hasPermission("playerkits.admin");
    }

    public static boolean hasCooldownBypassPermission(CommandSender sender) {
        return sender.hasPermission("playerkits.bypass.cooldown");
    }

    public static boolean hasOneTimeBypassPermission(CommandSender sender) {
        return sender.hasPermission("playerkits.bypass.onetime");
    }

    public static boolean passCondition(Player player, String condition) {
        block22: {
            String[] sep = condition.split(" ");
            String variable = sep[0];
            variable = PlaceholderAPI.setPlaceholders((Player)player, (String)variable);
            String conditional = sep[1];
            if (conditional.equals(">=")) {
                String[] conditionMiniSep = condition.split(" >= ");
                String value = conditionMiniSep[1];
                try {
                    double valueFinal = Double.valueOf(value);
                    double valueFinalVariable = Double.valueOf(variable);
                    if (valueFinalVariable >= valueFinal) {
                        return true;
                    }
                    break block22;
                }
                catch (NumberFormatException e) {
                    return true;
                }
            }
            if (conditional.equals("<=")) {
                String[] conditionMiniSep = condition.split(" <= ");
                String value = conditionMiniSep[1];
                try {
                    double valueFinal = Double.valueOf(value);
                    double valueFinalVariable = Double.valueOf(variable);
                    if (valueFinalVariable <= valueFinal) {
                        return true;
                    }
                    break block22;
                }
                catch (NumberFormatException e) {
                    return true;
                }
            }
            if (conditional.equals("==")) {
                String[] conditionMiniSep = condition.split(" == ");
                String value = conditionMiniSep[1];
                if (value.equals(variable)) {
                    return true;
                }
            } else if (conditional.equals("!=")) {
                String[] conditionMiniSep = condition.split(" != ");
                String value = conditionMiniSep[1];
                if (!value.equals(variable)) {
                    return true;
                }
            } else {
                if (conditional.equals(">")) {
                    String[] conditionMiniSep = condition.split(" > ");
                    String value = conditionMiniSep[1];
                    try {
                        double valueFinal = Double.valueOf(value);
                        double valueFinalVariable = Double.valueOf(variable);
                        if (valueFinalVariable > valueFinal) {
                            return true;
                        }
                        break block22;
                    }
                    catch (NumberFormatException e) {
                        return true;
                    }
                }
                if (conditional.equals("<")) {
                    String[] conditionMiniSep = condition.split(" < ");
                    String value = conditionMiniSep[1];
                    try {
                        double valueFinal = Double.valueOf(value);
                        double valueFinalVariable = Double.valueOf(variable);
                        if (valueFinalVariable < valueFinal) {
                            return true;
                        }
                    }
                    catch (NumberFormatException e) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

