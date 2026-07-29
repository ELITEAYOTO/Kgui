/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package pk.ajneb97.utils;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.item.KitItemSkullData;
import pk.ajneb97.utils.ItemUtils;
import pk.ajneb97.utils.OtherUtils;

public class InventoryItem {
    private Inventory inventory;
    private int slot;
    private ItemStack item;
    private ItemMeta meta;
    private String headTexture;

    public InventoryItem(Inventory inventory, int slot, Material material) {
        this.inventory = inventory;
        this.item = new ItemStack(material);
        this.meta = this.item.getItemMeta();
        this.slot = slot;
    }

    public InventoryItem dataValue(short datavalue) {
        this.item.setDurability(datavalue);
        return this;
    }

    public InventoryItem amount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    public InventoryItem name(String name) {
        this.meta.setDisplayName(MessagesManager.getLegacyColoredMessage(name));
        return this;
    }

    public InventoryItem lore(List<String> lore) {
        for (int i = 0; i < lore.size(); ++i) {
            lore.set(i, MessagesManager.getLegacyColoredMessage(lore.get(i)));
        }
        this.meta.setLore(lore);
        return this;
    }

    public InventoryItem enchanted(boolean enchanted) {
        if (enchanted) {
            this.meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
            this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        }
        return this;
    }

    public InventoryItem setSkull(String texture) {
        if (OtherUtils.isLegacy()) {
            this.item.setDurability((short)3);
        }
        this.headTexture = texture;
        return this;
    }

    public void ready() {
        this.item.setItemMeta(this.meta);
        if (this.headTexture != null) {
            ItemUtils.setSkullData(this.item, new KitItemSkullData(null, this.headTexture, null), null);
        }
        this.inventory.setItem(this.slot, this.item);
    }
}

