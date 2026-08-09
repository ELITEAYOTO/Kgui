package me.krunsh.kgui.item;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kgui.Kgui;

/**
 * Registre central des items
 * Charge les items depuis items/ et permet de les réutiliser partout
 */
public class ItemRegistry {

    private final Kgui plugin;
    private final Map<String, ItemData> items = new HashMap<>();

    public ItemRegistry(Kgui plugin) {
        this.plugin = plugin;
        loadItems();
    }

    /**
     * Charge tous les items depuis le dossier items/
     */
    public void loadItems() {
        items.clear();
        
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
        }
        
        // Toujours sauvegarder les fichiers par défaut s'ils n'existent pas (flag false = no overwrite)
        saveDefaultItems();
        
        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No item files found in items/");
            return;
        }
        
        for (File file : files) {
            loadItemFile(file);
        }
        
        plugin.getLogger().info("Loaded " + items.size() + " items from " + files.length + " files");
    }

    /**
     * Charge un fichier d'items
     */
    private void loadItemFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            // Le fichier peut contenir un seul item ou plusieurs
            // Si la racine a "material", c'est un item unique
            if (config.contains("material")) {
                String itemId = file.getName().replace(".yml", "");
                ItemData itemData = parseItem(config, itemId);
                if (itemData != null) {
                    items.put(itemId.toLowerCase(), itemData);
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("[Debug] Loaded item: " + itemId);
                    }
                }
            } else {
                // Sinon, chaque section est un item
                for (String key : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section != null) {
                        ItemData itemData = parseItem(section, key);
                        if (itemData != null) {
                            items.put(key.toLowerCase(), itemData);
                            if (plugin.getConfigManager().isDebug()) {
                                plugin.getLogger().info("[Debug] Loaded item: " + key);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading item file: " + file.getName(), e);
        }
    }

    /**
     * Parse une section de configuration en ItemData
     */
    private ItemData parseItem(ConfigurationSection section, String itemId) {
        try {
            ItemData data = new ItemData(itemId);
            
            // Material (requis)
            String materialStr = section.getString("material", "STONE");
            Material material;
            int materialData = 0;
            
            if (materialStr.contains(":")) {
                String[] parts = materialStr.split(":");
                material = Material.valueOf(parts[0].toUpperCase());
                materialData = Integer.parseInt(parts[1]);
            } else {
                material = Material.valueOf(materialStr.toUpperCase());
            }
            
            data.setMaterial(material);
            data.setData(section.getInt("data", materialData));
            
            // Display name
            data.setDisplayName(section.getString("display_name", section.getString("name", null)));
            
            // Lore
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                data.setLore(lore);
            }
            
            // Amount
            data.setAmount(section.getInt("amount", 1));
            
            // Enchantments
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                Map<Enchantment, Integer> enchants = new HashMap<>();
                for (String enchantKey : enchantSection.getKeys(false)) {
                    try {
                        Enchantment enchant = Enchantment.getByName(enchantKey.toUpperCase());
                        if (enchant != null) {
                            enchants.put(enchant, enchantSection.getInt(enchantKey));
                        }
                    } catch (Exception ignored) {}
                }
                data.setEnchantments(enchants);
            }
            
            // Flags
            data.setHideEnchants(section.getBoolean("hide_enchants", false));
            data.setHideAttributes(section.getBoolean("hide_attributes", false));
            data.setHideAll(section.getBoolean("hide_all", false));
            data.setGlow(section.getBoolean("glow", false));
            data.setUnbreakable(section.getBoolean("unbreakable", false));
            
            // Skull
            data.setSkullOwner(section.getString("skull_owner", section.getString("skull", null)));
            data.setSkullTexture(section.getString("skull_texture", null));
            
            // HeadDatabase
            data.setHeadDatabaseId(section.getString("head_database", section.getString("hdb", null)));
            
            // NBT Custom
            ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
            if (nbtSection != null) {
                Map<String, Object> nbtData = new HashMap<>();
                for (String nbtKey : nbtSection.getKeys(false)) {
                    nbtData.put(nbtKey, nbtSection.get(nbtKey));
                }
                data.setCustomNbt(nbtData);
            }
            
            // CIT key (pour PluginCIT)
            data.setCitKey(section.getString("cit", section.getString("cit_key", null)));
            
            // Protection GUI
            data.setGuiProtected(section.getBoolean("kgui_protected", true));
            
            return data;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing item: " + itemId, e);
            return null;
        }
    }

    /**
     * Sauvegarde les items par défaut
     */
    private void saveDefaultItems() {
        java.io.File exampleFile = new java.io.File(plugin.getDataFolder(), "items/example.yml");
        if (!exampleFile.exists()) {
            plugin.saveResource("items/example.yml", false);
        }
    }

    /**
     * Recharge tous les items
     */
    public void reload() {
        loadItems();
    }

    /**
     * Vérifie si un item existe
     */
    public boolean hasItem(String itemId) {
        return items.containsKey(itemId.toLowerCase());
    }

    /**
     * Obtient les données d'un item
     */
    public ItemData getItemData(String itemId) {
        return items.get(itemId.toLowerCase());
    }

    /**
     * Construit un ItemStack depuis un item_id
     */
    public ItemStack buildItem(String itemId) {
        ItemData data = getItemData(itemId);
        if (data == null) {
            return null;
        }
        return data.build(plugin);
    }

    /**
     * Obtient le nombre d'items enregistrés
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Obtient tous les items enregistrés
     */
    public Map<String, ItemData> getItems() {
        return items;
    }

    /**
     * Obtient tous les IDs d'items
     */
    public java.util.Set<String> getItemIds() {
        return items.keySet();
    }
}
