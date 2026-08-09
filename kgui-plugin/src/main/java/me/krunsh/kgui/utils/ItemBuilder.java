package me.krunsh.kgui.utils;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builder fluide pour créer des ItemStacks
 */
public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    /**
     * Crée un ItemBuilder avec un Material
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /**
     * Crée un ItemBuilder avec un Material et data value
     */
    public ItemBuilder(Material material, int data) {
        this.item = new ItemStack(material, 1, (short) data);
        this.meta = item.getItemMeta();
    }

    /**
     * Crée un ItemBuilder depuis une string "MATERIAL" ou "MATERIAL:DATA"
     */
    public ItemBuilder(String materialString) {
        Material material = Material.STONE;
        int data = 0;
        
        if (materialString != null && !materialString.isEmpty()) {
            String[] parts = materialString.split(":");
            try {
                material = Material.valueOf(parts[0].toUpperCase());
                if (parts.length > 1) {
                    data = Integer.parseInt(parts[1]);
                }
            } catch (IllegalArgumentException e) {
                // Garder STONE par défaut
            }
        }
        
        this.item = new ItemStack(material, 1, (short) data);
        this.meta = item.getItemMeta();
    }

    /**
     * Crée un ItemBuilder depuis un ItemStack existant
     */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    /**
     * Définit la quantité
     */
    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * Définit le data value
     */
    public ItemBuilder data(int data) {
        item.setDurability((short) data);
        return this;
    }

    /**
     * Définit le nom d'affichage
     */
    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
        }
        return this;
    }

    /**
     * Définit le lore
     */
    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            meta.setLore(ColorUtils.colorize(lore));
        }
        return this;
    }

    /**
     * Définit le lore depuis un varargs
     */
    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    /**
     * Ajoute une ligne au lore
     */
    public ItemBuilder addLore(String line) {
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(ColorUtils.colorize(line));
            meta.setLore(lore);
        }
        return this;
    }

    /**
     * Ajoute un enchantement
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    /**
     * Ajoute des enchantements depuis une map
     */
    public ItemBuilder enchants(Map<Enchantment, Integer> enchantments) {
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                enchant(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * Ajoute un flag d'item
     */
    public ItemBuilder flag(ItemFlag flag) {
        if (meta != null) {
            meta.addItemFlags(flag);
        }
        return this;
    }

    /**
     * Ajoute tous les flags (cache tout)
     */
    public ItemBuilder hideAll() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    /**
     * Cache les enchantements
     */
    public ItemBuilder hideEnchants() {
        return flag(ItemFlag.HIDE_ENCHANTS);
    }

    /**
     * Cache les attributs
     */
    public ItemBuilder hideAttributes() {
        return flag(ItemFlag.HIDE_ATTRIBUTES);
    }

    /**
     * Ajoute un effet de glow (enchant + hide)
     */
    public ItemBuilder glow() {
        enchant(Enchantment.DURABILITY, 1);
        hideEnchants();
        return this;
    }

    /**
     * Rend l'item indestructible
     */
    public ItemBuilder unbreakable() {
        if (meta != null) {
            meta.spigot().setUnbreakable(true);
        }
        return this;
    }

    /**
     * Définit le propriétaire d'un skull
     */
    public ItemBuilder skull(String owner) {
        if (meta instanceof SkullMeta && owner != null) {
            ((SkullMeta) meta).setOwner(owner);
        }
        return this;
    }

    /**
     * Définit la couleur d'une armure en cuir
     */
    public ItemBuilder leatherColor(int red, int green, int blue) {
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(red, green, blue));
        }
        return this;
    }

    /**
     * Ajoute un tag NBT (string)
     */
    public ItemBuilder nbt(String key, String value) {
        item.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString(key, value);
        this.item = nbtItem.getItem();
        this.meta = item.getItemMeta();
        return this;
    }

    /**
     * Ajoute un tag NBT (boolean)
     */
    public ItemBuilder nbt(String key, boolean value) {
        item.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setBoolean(key, value);
        this.item = nbtItem.getItem();
        this.meta = item.getItemMeta();
        return this;
    }

    /**
     * Ajoute un tag NBT (int)
     */
    public ItemBuilder nbt(String key, int value) {
        item.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger(key, value);
        this.item = nbtItem.getItem();
        this.meta = item.getItemMeta();
        return this;
    }

    /**
     * Construit l'ItemStack final
     */
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
