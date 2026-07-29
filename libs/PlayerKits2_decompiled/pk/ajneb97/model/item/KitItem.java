/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package pk.ajneb97.model.item;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.model.item.KitItemBannerData;
import pk.ajneb97.model.item.KitItemBookData;
import pk.ajneb97.model.item.KitItemCustomModelComponentData;
import pk.ajneb97.model.item.KitItemFireworkData;
import pk.ajneb97.model.item.KitItemPotionData;
import pk.ajneb97.model.item.KitItemSkullData;
import pk.ajneb97.model.item.KitItemTrimData;
import pk.ajneb97.utils.ItemUtils;

public class KitItem {
    private String id;
    private int amount;
    private String name;
    private List<String> lore;
    private short durability;
    private int customModelData;
    private List<String> enchants;
    private List<String> flags;
    private List<String> bookEnchants;
    private int color;
    private List<String> nbt;
    private List<String> attributes;
    private List<String> canDestroy;
    private List<String> canPlace;
    private KitItemSkullData skullData;
    private KitItemPotionData potionData;
    private KitItemFireworkData fireworkData;
    private KitItemBannerData bannerData;
    private KitItemBookData bookData;
    private KitItemTrimData trimData;
    private KitItemCustomModelComponentData customModelComponentData;
    private boolean hideTooltip;
    private String tooltipStyle;
    private String model;
    private boolean offhand;
    private int previewSlot;
    private ItemStack originalItem;

    public KitItem(String id) {
        this.id = id;
        this.amount = 1;
        this.durability = 0;
        this.customModelData = 0;
        this.color = 0;
        this.previewSlot = -1;
    }

    public KitItem(ItemStack item) {
        this.previewSlot = -1;
        this.originalItem = item;
    }

    public ItemStack getOriginalItem() {
        return this.originalItem;
    }

    public void setOriginalItem(ItemStack originalItem) {
        this.originalItem = originalItem;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public short getDurability() {
        return this.durability;
    }

    public void setDurability(short durability) {
        this.durability = durability;
    }

    public int getCustomModelData() {
        return this.customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public List<String> getEnchants() {
        return this.enchants;
    }

    public void setEnchants(List<String> enchants) {
        this.enchants = enchants;
    }

    public List<String> getFlags() {
        return this.flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public KitItemSkullData getSkullData() {
        return this.skullData;
    }

    public void setSkullData(KitItemSkullData skullData) {
        this.skullData = skullData;
    }

    public KitItemPotionData getPotionData() {
        return this.potionData;
    }

    public void setPotionData(KitItemPotionData potionData) {
        this.potionData = potionData;
    }

    public List<String> getBookEnchants() {
        return this.bookEnchants;
    }

    public void setBookEnchants(List<String> bookEnchants) {
        this.bookEnchants = bookEnchants;
    }

    public List<String> getNbt() {
        return this.nbt;
    }

    public void setNbt(List<String> nbt) {
        this.nbt = nbt;
    }

    public List<String> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getCanDestroy() {
        return this.canDestroy;
    }

    public void setCanDestroy(List<String> canDestroy) {
        this.canDestroy = canDestroy;
    }

    public List<String> getCanPlace() {
        return this.canPlace;
    }

    public void setCanPlace(List<String> canPlace) {
        this.canPlace = canPlace;
    }

    public KitItemFireworkData getFireworkData() {
        return this.fireworkData;
    }

    public void setFireworkData(KitItemFireworkData fireworkData) {
        this.fireworkData = fireworkData;
    }

    public KitItemBannerData getBannerData() {
        return this.bannerData;
    }

    public void setBannerData(KitItemBannerData bannerData) {
        this.bannerData = bannerData;
    }

    public KitItemBookData getBookData() {
        return this.bookData;
    }

    public void setBookData(KitItemBookData bookData) {
        this.bookData = bookData;
    }

    public KitItemTrimData getTrimData() {
        return this.trimData;
    }

    public void setTrimData(KitItemTrimData trimData) {
        this.trimData = trimData;
    }

    public KitItemCustomModelComponentData getCustomModelComponentData() {
        return this.customModelComponentData;
    }

    public void setCustomModelComponentData(KitItemCustomModelComponentData customModelComponentData) {
        this.customModelComponentData = customModelComponentData;
    }

    public boolean isHideTooltip() {
        return this.hideTooltip;
    }

    public void setHideTooltip(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
    }

    public String getTooltipStyle() {
        return this.tooltipStyle;
    }

    public void setTooltipStyle(String tooltipStyle) {
        this.tooltipStyle = tooltipStyle;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isOffhand() {
        return this.offhand;
    }

    public void setOffhand(boolean offhand) {
        this.offhand = offhand;
    }

    public int getPreviewSlot() {
        return this.previewSlot;
    }

    public void setPreviewSlot(int previewSlot) {
        this.previewSlot = previewSlot;
    }

    public void removeOffHandFromEditInventory(PlayerKits2 plugin) {
        if (this.originalItem != null) {
            ItemMeta meta = this.originalItem.getItemMeta();
            List lore = meta.getLore();
            lore.remove(lore.size() - 1);
            lore.remove(lore.size() - 1);
            meta.setLore(lore);
            this.originalItem.setItemMeta(meta);
            this.originalItem = ItemUtils.removeTagItem(plugin, this.originalItem, "playerkits_offhand");
            return;
        }
        this.lore.remove(this.lore.size() - 1);
        this.lore.remove(this.lore.size() - 1);
        if (this.nbt != null) {
            for (int i = 0; i < this.nbt.size(); ++i) {
                if (!this.nbt.get(i).startsWith("playerkits_offhand")) continue;
                this.nbt.remove(i);
                return;
            }
        }
    }

    public KitItem clone() {
        KitItem kitItem = new KitItem(this.id);
        kitItem.setAmount(this.amount);
        kitItem.setName(this.name);
        kitItem.setLore((List<String>)(this.lore != null ? new ArrayList<String>(this.lore) : null));
        kitItem.setDurability(this.durability);
        kitItem.setCustomModelData(this.customModelData);
        kitItem.setEnchants((List<String>)(this.enchants != null ? new ArrayList<String>(this.enchants) : null));
        kitItem.setFlags((List<String>)(this.flags != null ? new ArrayList<String>(this.flags) : null));
        kitItem.setBookEnchants((List<String>)(this.bookEnchants != null ? new ArrayList<String>(this.bookEnchants) : null));
        kitItem.setColor(this.color);
        kitItem.setNbt((List<String>)(this.nbt != null ? new ArrayList<String>(this.nbt) : null));
        kitItem.setAttributes((List<String>)(this.attributes != null ? new ArrayList<String>(this.attributes) : null));
        kitItem.setCanDestroy((List<String>)(this.canDestroy != null ? new ArrayList<String>(this.canDestroy) : null));
        kitItem.setCanDestroy((List<String>)(this.canPlace != null ? new ArrayList<String>(this.canPlace) : null));
        kitItem.setSkullData(this.skullData != null ? this.skullData.clone() : null);
        kitItem.setPotionData(this.potionData != null ? this.potionData.clone() : null);
        kitItem.setFireworkData(this.fireworkData != null ? this.fireworkData.clone() : null);
        kitItem.setBannerData(this.bannerData != null ? this.bannerData.clone() : null);
        kitItem.setBookData(this.bookData != null ? this.bookData.clone() : null);
        kitItem.setTrimData(this.trimData != null ? this.trimData.clone() : null);
        kitItem.setCustomModelComponentData(this.customModelComponentData != null ? this.customModelComponentData.clone() : null);
        kitItem.setHideTooltip(this.hideTooltip);
        kitItem.setTooltipStyle(this.tooltipStyle);
        kitItem.setPreviewSlot(this.previewSlot);
        kitItem.setOffhand(this.offhand);
        kitItem.setModel(this.model);
        return kitItem;
    }
}

