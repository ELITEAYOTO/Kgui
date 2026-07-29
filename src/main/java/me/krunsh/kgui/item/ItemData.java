package me.krunsh.kgui.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.utils.ColorUtils;

/**
 * Données d'un item enregistré dans le registry
 */
public class ItemData {

    private final String id;
    
    // Propriétés de base
    private Material material = Material.STONE;
    private int data = 0;
    private int amount = 1;
    private String displayName;
    private List<String> lore;
    
    // Enchantements
    private Map<Enchantment, Integer> enchantments;
    
    // Flags
    private boolean hideEnchants = false;
    private boolean hideAttributes = false;
    private boolean hideAll = false;
    private boolean glow = false;
    private boolean unbreakable = false;
    
    // Skull
    private String skullOwner;
    private String skullTexture;
    
    // HeadDatabase
    private String headDatabaseId;
    
    // NBT Custom
    private Map<String, Object> customNbt;
    
    // CIT
    private String citKey;
    
    // Protection GUI
    private boolean guiProtected = true;

    public ItemData(String id) {
        this.id = id;
    }

    /**
     * Construit l'ItemStack final avec toutes les propriétés
     */
    public ItemStack build(Kgui plugin) {
        ItemStack item;
        
        // HeadDatabase support
        if (headDatabaseId != null && plugin.getHookManager().isHeadDatabaseEnabled()) {
            item = plugin.getHookManager().getHeadDatabaseHook().getHead(headDatabaseId);
            if (item == null) {
                item = new ItemStack(Material.SKULL_ITEM, amount, (short) 3);
            }
        } else {
            item = new ItemStack(material, amount, (short) data);
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        // Display name
        if (displayName != null) {
            meta.setDisplayName(ColorUtils.colorize(displayName));
        }
        
        // Lore
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtils.colorize(lore));
        }
        
        // Skull owner
        if (skullOwner != null && meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(skullOwner);
        }
        
        // Enchantements
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        
        // Glow effect
        if (glow && (enchantments == null || enchantments.isEmpty())) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Flags
        if (hideAll) {
            meta.addItemFlags(ItemFlag.values());
        } else {
            if (hideEnchants) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (hideAttributes) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
        }
        
        // Unbreakable
        if (unbreakable) {
            meta.spigot().setUnbreakable(true);
        }
        
        item.setItemMeta(meta);
        
        // NBT Custom (via NBT-API)
        if (customNbt != null && !customNbt.isEmpty()) {
            NBTItem nbtItem = new NBTItem(item);
            for (Map.Entry<String, Object> entry : customNbt.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    nbtItem.setString(entry.getKey(), (String) value);
                } else if (value instanceof Integer) {
                    nbtItem.setInteger(entry.getKey(), (Integer) value);
                } else if (value instanceof Boolean) {
                    nbtItem.setBoolean(entry.getKey(), (Boolean) value);
                } else if (value instanceof Double) {
                    nbtItem.setDouble(entry.getKey(), (Double) value);
                }
            }
            item = nbtItem.getItem();
        }
        
        // CIT key - Compatible avec le resource pack SparrowMC
        if (citKey != null) {
            NBTItem nbtItem = new NBTItem(item);
            nbtItem.setString("sparrowmc-item", citKey);
            item = nbtItem.getItem();
        }
        
        return item;
    }

    /**
     * Clone cet ItemData
     */
    public ItemData clone() {
        ItemData copy = new ItemData(id);
        copy.material = this.material;
        copy.data = this.data;
        copy.amount = this.amount;
        copy.displayName = this.displayName;
        copy.lore = this.lore != null ? new ArrayList<>(this.lore) : null;
        copy.enchantments = this.enchantments != null ? new HashMap<>(this.enchantments) : null;
        copy.hideEnchants = this.hideEnchants;
        copy.hideAttributes = this.hideAttributes;
        copy.hideAll = this.hideAll;
        copy.glow = this.glow;
        copy.unbreakable = this.unbreakable;
        copy.skullOwner = this.skullOwner;
        copy.skullTexture = this.skullTexture;
        copy.headDatabaseId = this.headDatabaseId;
        copy.customNbt = this.customNbt != null ? new HashMap<>(this.customNbt) : null;
        copy.citKey = this.citKey;
        copy.guiProtected = this.guiProtected;
        return copy;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public boolean isHideEnchants() {
        return hideEnchants;
    }

    public void setHideEnchants(boolean hideEnchants) {
        this.hideEnchants = hideEnchants;
    }

    public boolean isHideAttributes() {
        return hideAttributes;
    }

    public void setHideAttributes(boolean hideAttributes) {
        this.hideAttributes = hideAttributes;
    }

    public boolean isHideAll() {
        return hideAll;
    }

    public void setHideAll(boolean hideAll) {
        this.hideAll = hideAll;
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public String getSkullOwner() {
        return skullOwner;
    }

    public void setSkullOwner(String skullOwner) {
        this.skullOwner = skullOwner;
    }

    public String getSkullTexture() {
        return skullTexture;
    }

    public void setSkullTexture(String skullTexture) {
        this.skullTexture = skullTexture;
    }

    public String getHeadDatabaseId() {
        return headDatabaseId;
    }

    public void setHeadDatabaseId(String headDatabaseId) {
        this.headDatabaseId = headDatabaseId;
    }

    public Map<String, Object> getCustomNbt() {
        return customNbt;
    }

    public void setCustomNbt(Map<String, Object> customNbt) {
        this.customNbt = customNbt;
    }

    public String getCitKey() {
        return citKey;
    }

    public void setCitKey(String citKey) {
        this.citKey = citKey;
    }

    public boolean isGuiProtected() {
        return guiProtected;
    }

    public void setGuiProtected(boolean guiProtected) {
        this.guiProtected = guiProtected;
    }
}
