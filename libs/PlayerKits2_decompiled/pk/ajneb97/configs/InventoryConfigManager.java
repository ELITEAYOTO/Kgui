/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package pk.ajneb97.configs;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.configs.model.CommonConfig;
import pk.ajneb97.managers.KitItemManager;
import pk.ajneb97.model.inventory.ItemKitInventory;
import pk.ajneb97.model.inventory.KitInventory;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.utils.OtherUtils;

public class InventoryConfigManager {
    private PlayerKits2 plugin;
    private CommonConfig configFile;

    public InventoryConfigManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.configFile = new CommonConfig("inventory.yml", plugin, null, false);
        this.configFile.registerConfig();
        if (this.configFile.isFirstTime() && OtherUtils.isLegacy()) {
            this.checkAndFix();
        }
        this.checkClickCommands();
    }

    public void checkAndFix() {
        FileConfiguration config = this.configFile.getConfig();
        config.set("inventories.main_inventory.0;1;7;8;9;17;36;44;45;46;52;53.item.id", (Object)"STAINED_GLASS_PANE:15");
        config.set("inventories.premium_kits_inventory.0;1;7;8;9;17;36;44;45;46;52;53.item.id", (Object)"STAINED_GLASS_PANE:4");
        config.set("inventories.buy_requirements_inventory.10.item.id", (Object)"STAINED_GLASS_PANE:14");
        config.set("inventories.buy_requirements_inventory.16.item.id", (Object)"STAINED_GLASS_PANE:5");
        this.configFile.saveConfig();
    }

    public void checkClickCommands() {
        boolean needsSave = false;
        FileConfiguration config = this.configFile.getConfig();
        if (config.contains("inventories")) {
            for (String key : config.getConfigurationSection("inventories").getKeys(false)) {
                for (String slotString : config.getConfigurationSection("inventories." + key).getKeys(false)) {
                    String path = "inventories." + key + "." + slotString;
                    if (!config.contains(path + ".click_commands")) continue;
                    ArrayList<String> clickActions = new ArrayList<String>();
                    List clickCommands = config.getStringList(path + ".click_commands");
                    for (String c : clickCommands) {
                        if (c.startsWith("msg %player% ")) {
                            String text = c.replace("msg %player% ", "");
                            clickActions.add("message: " + text);
                            continue;
                        }
                        if (c.equals("close_inventory")) {
                            clickActions.add(c);
                            continue;
                        }
                        clickActions.add("console_command: " + c);
                    }
                    config.set(path + ".click_actions", clickActions);
                    needsSave = true;
                }
            }
        }
        if (needsSave) {
            this.configFile.saveConfig();
        }
    }

    public void configure() {
        FileConfiguration config = this.configFile.getConfig();
        ArrayList<KitInventory> inventories = new ArrayList<KitInventory>();
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        if (config.contains("inventories")) {
            for (String key : config.getConfigurationSection("inventories").getKeys(false)) {
                int slots = config.getInt("inventories." + key + ".slots");
                String title = config.getString("inventories." + key + ".title");
                ArrayList<ItemKitInventory> items = new ArrayList<ItemKitInventory>();
                for (String slotString : config.getConfigurationSection("inventories." + key).getKeys(false)) {
                    if (slotString.equals("slots") || slotString.equals("title")) continue;
                    String path = "inventories." + key + "." + slotString;
                    KitItem item = null;
                    if (config.contains(path + ".item")) {
                        item = kitItemManager.getKitItemFromConfig(config, path + ".item");
                    }
                    String openInventory = config.contains(path + ".open_inventory") ? config.getString(path + ".open_inventory") : null;
                    List clickActions = config.contains(path + ".click_actions") ? config.getStringList(path + ".click_actions") : null;
                    String type = config.contains(path + ".type") ? config.getString(path + ".type") : null;
                    ItemKitInventory itemCraft = new ItemKitInventory(slotString, item, openInventory, clickActions, type);
                    items.add(itemCraft);
                }
                KitInventory inv = new KitInventory(key, slots, title, items);
                inventories.add(inv);
            }
        }
        this.plugin.getInventoryManager().setInventories(inventories);
    }

    public void saveKitItemOnConfig(String inventoryName, int slot, String kitName) {
        FileConfiguration config = this.configFile.getConfig();
        config.set("inventories." + inventoryName + "." + slot + ".type", (Object)("kit: " + kitName));
        this.configFile.saveConfig();
    }

    public void save() {
        FileConfiguration config = this.configFile.getConfig();
        config.set("inventories", null);
        KitItemManager kitItemManager = this.plugin.getKitItemManager();
        for (KitInventory kitInventory : this.plugin.getInventoryManager().getInventories()) {
            String name = kitInventory.getName();
            config.set("inventories." + name + ".slots", (Object)kitInventory.getSlots());
            config.set("inventories." + name + ".title", (Object)kitInventory.getTitle());
            for (ItemKitInventory item : kitInventory.getItems()) {
                String slotsString = item.getSlotsString();
                String path = "inventories." + name + "." + slotsString;
                if (item.getType() != null) {
                    config.set(path + ".type", (Object)item.getType());
                }
                if (item.getItem() != null) {
                    kitItemManager.saveKitItemOnConfig(item.getItem(), config, path + ".item");
                }
                if (item.getOpenInventory() != null) {
                    config.set(path + ".open_inventory", (Object)item.getOpenInventory());
                }
                if (item.getClickActions() == null || item.getClickActions().isEmpty()) continue;
                config.set(path + ".click_actions", item.getClickActions());
            }
        }
        this.configFile.saveConfig();
    }

    public boolean reloadConfig() {
        if (!this.configFile.reloadConfig()) {
            return false;
        }
        this.configure();
        return true;
    }

    public FileConfiguration getConfig() {
        return this.configFile.getConfig();
    }
}

