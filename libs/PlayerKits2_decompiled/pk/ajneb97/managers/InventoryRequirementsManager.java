/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package pk.ajneb97.managers;

import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.InventoryManager;
import pk.ajneb97.managers.KitsManager;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.KitRequirements;
import pk.ajneb97.model.internal.GiveKitInstructions;
import pk.ajneb97.model.internal.PlayerKitsMessageResult;
import pk.ajneb97.model.inventory.InventoryPlayer;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.PlayerUtils;

public class InventoryRequirementsManager {
    private PlayerKits2 plugin;
    private InventoryManager inventoryManager;

    public InventoryRequirementsManager(PlayerKits2 plugin, InventoryManager inventoryManager) {
        this.plugin = plugin;
        this.inventoryManager = inventoryManager;
    }

    public void configureRequirementsItem(ItemStack item, String kitName, Player player) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasLore()) {
                return;
            }
            if (this.plugin.getConfigsManager().getMainConfigManager().isUseMiniMessage()) {
                MiniMessageUtils.setRequirementsMessage(meta, kitName, player, this);
            } else {
                ArrayList<String> lore = new ArrayList<String>();
                for (String line : meta.getLore()) {
                    if (line.equals("%kit_requirements_message%")) {
                        List<String> message = this.replaceRequirementsMessageVariable(kitName, player);
                        for (String m : message) {
                            lore.add(MessagesManager.getLegacyColoredMessage(m));
                        }
                        continue;
                    }
                    lore.add(line);
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
    }

    public List<String> replaceRequirementsMessageVariable(String kitName, Player player) {
        Kit kit = this.plugin.getKitsManager().getKitByName(kitName);
        KitRequirements requirements = kit.getRequirements();
        MessagesManager msgManager = this.plugin.getMessagesManager();
        ArrayList<String> requirementsMessage = new ArrayList<String>(requirements.getMessage());
        List<String> extraRequirements = requirements.getExtraRequirements();
        ArrayList<Boolean> requirementsBooleans = new ArrayList<Boolean>();
        boolean passPrice = this.plugin.getKitsManager().passPrice(requirements.getPrice(), player);
        String priceRequirementStatusSymbol = passPrice ? msgManager.getRequirementsMessageStatusSymbolTrue() : msgManager.getRequirementsMessageStatusSymbolFalse();
        boolean isPlaceholderAPI = this.plugin.getDependencyManager().isPlaceholderAPI();
        for (String condition : extraRequirements) {
            boolean pass = false;
            if (isPlaceholderAPI) {
                pass = PlayerUtils.passCondition(player, condition);
            }
            requirementsBooleans.add(pass);
        }
        for (int i = 0; i < requirementsMessage.size(); ++i) {
            String line = (String)requirementsMessage.get(i);
            if (line.contains("%status_symbol_requirement_")) {
                int lastPos;
                int pos = line.indexOf("%status_symbol_requirement_");
                String variable = line.substring(pos, lastPos = line.indexOf("%", pos + 1) + 1);
                int posNumber = Integer.valueOf(variable.replace("%status_symbol_requirement_", "").replace("%", ""));
                boolean passCondition = (Boolean)requirementsBooleans.get(posNumber - 1);
                line = passCondition ? line.replace(variable, msgManager.getRequirementsMessageStatusSymbolTrue()) : line.replace(variable, msgManager.getRequirementsMessageStatusSymbolFalse());
            }
            line = line.replace("%status_symbol_price%", priceRequirementStatusSymbol);
            if (isPlaceholderAPI) {
                line = PlaceholderAPI.setPlaceholders((Player)player, (String)line);
            }
            requirementsMessage.set(i, line);
        }
        return requirementsMessage;
    }

    public void requirementsInventoryBuy(InventoryPlayer inventoryPlayer) {
        Kit kit = this.plugin.getKitsManager().getKitByName(inventoryPlayer.getKitName());
        Player player = inventoryPlayer.getPlayer();
        MessagesManager msgManager = this.plugin.getMessagesManager();
        KitsManager kitsManager = this.plugin.getKitsManager();
        PlayerKitsMessageResult result = kitsManager.giveKit(player, inventoryPlayer.getKitName(), new GiveKitInstructions(false, true, false, false));
        if (!result.isError()) {
            if (this.plugin.getConfigsManager().getMainConfigManager().isCloseInventoryOnClaim()) {
                player.closeInventory();
                return;
            }
            this.requirementsInventoryCancel(inventoryPlayer);
        } else {
            msgManager.sendMessage((CommandSender)player, result.getMessage(), true);
        }
    }

    public void requirementsInventoryCancel(InventoryPlayer inventoryPlayer) {
        inventoryPlayer.setInventoryName(inventoryPlayer.getPreviousInventoryName());
        inventoryPlayer.setKitName(null);
        this.inventoryManager.openInventory(inventoryPlayer);
    }
}

