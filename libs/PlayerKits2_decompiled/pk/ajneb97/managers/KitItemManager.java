/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.NamespacedKey
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.EnchantmentStorageMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.LeatherArmorMeta
 *  org.bukkit.inventory.meta.components.CustomModelDataComponent
 */
package pk.ajneb97.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.managers.MessagesManager;
import pk.ajneb97.model.Kit;
import pk.ajneb97.model.internal.KitVariable;
import pk.ajneb97.model.item.KitItem;
import pk.ajneb97.model.item.KitItemBannerData;
import pk.ajneb97.model.item.KitItemBookData;
import pk.ajneb97.model.item.KitItemCustomModelComponentData;
import pk.ajneb97.model.item.KitItemFireworkData;
import pk.ajneb97.model.item.KitItemPotionData;
import pk.ajneb97.model.item.KitItemSkullData;
import pk.ajneb97.model.item.KitItemTrimData;
import pk.ajneb97.utils.ItemUtils;
import pk.ajneb97.utils.MiniMessageUtils;
import pk.ajneb97.utils.OtherUtils;
import pk.ajneb97.utils.ServerVersion;

public class KitItemManager {
    private PlayerKits2 plugin;

    public KitItemManager(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public KitItem createKitItemFromItemStack(ItemStack item, boolean exactItem) {
        List<String> nbtList;
        if (exactItem) {
            return new KitItem(item.clone());
        }
        KitItem kitItem = new KitItem(item.getType().name());
        kitItem.setAmount(item.getAmount());
        if (item.getDurability() != 0) {
            kitItem.setDurability(item.getDurability());
        }
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        boolean useMiniMessage = this.plugin.getConfigsManager().getMainConfigManager().isUseMiniMessage();
        boolean isPaper = this.plugin.getDependencyManager().isPaper();
        if (item.hasItemMeta()) {
            LeatherArmorMeta meta2;
            NamespacedKey key;
            Set flags;
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                if (useMiniMessage) {
                    MiniMessageUtils.setCommonItemName(kitItem, meta);
                } else if (isPaper && serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_19_R1)) {
                    MiniMessageUtils.setCommonItemNameLegacy(kitItem, meta);
                } else {
                    kitItem.setName(meta.getDisplayName().replace("\u00a7", "&"));
                }
            }
            if (meta.hasLore()) {
                ArrayList<String> lore = new ArrayList<String>();
                if (useMiniMessage) {
                    MiniMessageUtils.setCommonItemLore(lore, meta);
                } else if (isPaper && serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_19_R1)) {
                    MiniMessageUtils.setCommonItemLoreLegacy(lore, meta);
                } else {
                    for (String string : meta.getLore()) {
                        lore.add(string.replace("\u00a7", "&"));
                    }
                }
                kitItem.setLore(lore);
            }
            if (meta.hasEnchants()) {
                ArrayList<String> enchants = new ArrayList<String>();
                for (Map.Entry entry : meta.getEnchants().entrySet()) {
                    String enchant = serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1) ? ((Enchantment)entry.getKey()).getKey().getKey().toUpperCase() : ((Enchantment)entry.getKey()).getName();
                    int level = (Integer)entry.getValue();
                    enchants.add(enchant + ";" + level);
                }
                kitItem.setEnchants(enchants);
            }
            if ((flags = meta.getItemFlags()) != null && !flags.isEmpty()) {
                ArrayList<String> flagsList = new ArrayList<String>();
                for (ItemFlag flag : flags) {
                    flagsList.add(flag.name());
                }
                kitItem.setFlags(flagsList);
            }
            if (OtherUtils.isNew() && meta.hasCustomModelData() && !serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R3)) {
                kitItem.setCustomModelData(meta.getCustomModelData());
            }
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R3) && meta.hasCustomModelData()) {
                CustomModelDataComponent customModelDataComponent = meta.getCustomModelDataComponent();
                List<String> list = customModelDataComponent.getFlags().stream().map(String::valueOf).collect(Collectors.toList());
                List<String> customModelDataComponentColorsList = customModelDataComponent.getColors().stream().map(color -> String.valueOf(color.asRGB())).collect(Collectors.toList());
                List<String> customModelDataComponentFloatsList = customModelDataComponent.getFloats().stream().map(String::valueOf).collect(Collectors.toList());
                ArrayList<String> customModelDataComponentStringsList = new ArrayList<String>(customModelDataComponent.getStrings());
                kitItem.setCustomModelComponentData(new KitItemCustomModelComponentData(list, customModelDataComponentColorsList, customModelDataComponentFloatsList, customModelDataComponentStringsList));
            }
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R4) && meta.isHideTooltip()) {
                kitItem.setHideTooltip(true);
            }
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R2) && meta.hasTooltipStyle()) {
                key = meta.getTooltipStyle();
                kitItem.setTooltipStyle(key.getNamespace() + ":" + key.getKey());
            }
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R3) && meta.hasItemModel()) {
                key = meta.getItemModel();
                kitItem.setModel(key.getNamespace() + ":" + key.getKey());
            }
            if (meta instanceof LeatherArmorMeta) {
                meta2 = (LeatherArmorMeta)meta;
                kitItem.setColor(meta2.getColor().asRGB());
            }
            if (meta instanceof EnchantmentStorageMeta) {
                meta2 = (EnchantmentStorageMeta)meta;
                Map map = meta2.getStoredEnchants();
                ArrayList<String> enchantsList = new ArrayList<String>();
                for (Map.Entry entry : map.entrySet()) {
                    String enchant = serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1) ? ((Enchantment)entry.getKey()).getKey().getKey().toUpperCase() : ((Enchantment)entry.getKey()).getName();
                    int level = (Integer)entry.getValue();
                    enchantsList.add(enchant + ";" + level);
                }
                if (!enchantsList.isEmpty()) {
                    kitItem.setBookEnchants(enchantsList);
                }
            }
        }
        if (!serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R4) && !(nbtList = ItemUtils.getNBT(this.plugin, item)).isEmpty()) {
            kitItem.setNbt(nbtList);
        }
        kitItem.setAttributes(ItemUtils.getAttributes(this.plugin, item));
        kitItem.setSkullData(ItemUtils.getSkullData(item));
        kitItem.setPotionData(ItemUtils.getPotionData(item));
        kitItem.setFireworkData(ItemUtils.getFireworkData(item));
        kitItem.setBannerData(ItemUtils.getBannerData(item));
        kitItem.setBookData(ItemUtils.getBookData(item));
        kitItem.setTrimData(ItemUtils.getArmorTrimData(item));
        return kitItem;
    }

    public ItemStack createItemFromKitItem(KitItem kitItem, Player player, Kit kit) {
        List<String> bookEnchants;
        String model;
        String tooltipStyle;
        String[] sep;
        List<String> enchants;
        KitItemCustomModelComponentData kitItemCustomModelComponentData;
        ServerVersion serverVersion;
        int customModelData;
        List<String> lore;
        if (kitItem.getOriginalItem() != null) {
            ItemStack item = kitItem.getOriginalItem().clone();
            if (kit.isAllowPlaceholdersOnOriginalItems()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName()) {
                    String name = OtherUtils.replaceGlobalVariables(meta.getDisplayName(), player, this.plugin);
                    meta.setDisplayName(name);
                }
                if (meta.hasLore()) {
                    List lore2 = meta.getLore();
                    lore2.replaceAll(text -> OtherUtils.replaceGlobalVariables(text, player, this.plugin));
                    meta.setLore(lore2);
                }
                item.setItemMeta(meta);
            }
            return item;
        }
        ItemStack item = ItemUtils.createItemFromID(kitItem.getId());
        item.setAmount(kitItem.getAmount());
        short durability = kitItem.getDurability();
        if (durability != 0) {
            item.setDurability(durability);
        }
        boolean useMiniMessage = this.plugin.getConfigsManager().getMainConfigManager().isUseMiniMessage();
        ItemMeta meta = item.getItemMeta();
        String name = kitItem.getName();
        if (name != null) {
            name = OtherUtils.replaceGlobalVariables(name, player, this.plugin);
            if (useMiniMessage) {
                MiniMessageUtils.setItemName(meta, name);
            } else {
                meta.setDisplayName(MessagesManager.getLegacyColoredMessage(name));
            }
        }
        if ((lore = kitItem.getLore()) != null) {
            ArrayList<String> loreCopy = new ArrayList<String>(lore);
            if (useMiniMessage) {
                MiniMessageUtils.setItemLore(meta, loreCopy, player, this.plugin);
            } else {
                for (int i = 0; i < loreCopy.size(); ++i) {
                    String line = OtherUtils.replaceGlobalVariables((String)loreCopy.get(i), player, this.plugin);
                    loreCopy.set(i, MessagesManager.getLegacyColoredMessage(line));
                }
                meta.setLore(loreCopy);
            }
        }
        if ((customModelData = kitItem.getCustomModelData()) != 0) {
            meta.setCustomModelData(Integer.valueOf(customModelData));
        }
        if ((serverVersion = PlayerKits2.serverVersion).serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R3) && (kitItemCustomModelComponentData = kitItem.getCustomModelComponentData()) != null) {
            CustomModelDataComponent customModelDataComponent = meta.getCustomModelDataComponent();
            List<String> cFloats = kitItemCustomModelComponentData.getFloats();
            List<String> cColors = kitItemCustomModelComponentData.getColors();
            List<String> cFlags = kitItemCustomModelComponentData.getFlags();
            List<String> cStrings = kitItemCustomModelComponentData.getStrings();
            customModelDataComponent.setFlags(cFlags.stream().map(Boolean::parseBoolean).collect(Collectors.toList()));
            customModelDataComponent.setFloats(cFloats.stream().map(Float::parseFloat).collect(Collectors.toList()));
            customModelDataComponent.setColors(cColors.stream().map(rgb -> Color.fromRGB((int)Integer.parseInt(rgb))).collect(Collectors.toList()));
            customModelDataComponent.setStrings(new ArrayList<String>(cStrings));
            meta.setCustomModelDataComponent(customModelDataComponent);
        }
        if ((enchants = kitItem.getEnchants()) != null) {
            for (int i = 0; i < enchants.size(); ++i) {
                sep = enchants.get(i).split(";");
                String enchantName = sep[0];
                int enchantLevel = Integer.parseInt(sep[1]);
                meta.addEnchant(Enchantment.getByName((String)enchantName), enchantLevel, true);
            }
        }
        if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R4) && kitItem.isHideTooltip()) {
            meta.setHideTooltip(true);
        }
        if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R2) && (tooltipStyle = kitItem.getTooltipStyle()) != null) {
            sep = tooltipStyle.split(":");
            meta.setTooltipStyle(new NamespacedKey(sep[0], sep[1]));
        }
        if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R3) && (model = kitItem.getModel()) != null) {
            sep = model.split(":");
            meta.setItemModel(new NamespacedKey(sep[0], sep[1]));
        }
        item.setItemMeta(meta);
        int color = kitItem.getColor();
        if (color != 0) {
            LeatherArmorMeta meta2 = (LeatherArmorMeta)item.getItemMeta();
            meta2.setColor(Color.fromRGB((int)color));
            item.setItemMeta((ItemMeta)meta2);
        }
        if ((bookEnchants = kitItem.getBookEnchants()) != null && !bookEnchants.isEmpty()) {
            EnchantmentStorageMeta meta2 = (EnchantmentStorageMeta)item.getItemMeta();
            for (int i = 0; i < bookEnchants.size(); ++i) {
                String[] sep2 = bookEnchants.get(i).split(";");
                String enchantName = sep2[0];
                int level = Integer.valueOf(sep2[1]);
                meta2.addStoredEnchant(Enchantment.getByName((String)enchantName), level, true);
            }
            item.setItemMeta((ItemMeta)meta2);
        }
        KitItemSkullData skullData = kitItem.getSkullData();
        ItemUtils.setSkullData(item, skullData, player);
        KitItemPotionData potionData = kitItem.getPotionData();
        ItemUtils.setPotionData(item, potionData);
        KitItemFireworkData fireworkData = kitItem.getFireworkData();
        ItemUtils.setFireworkData(item, fireworkData);
        KitItemBannerData bannerData = kitItem.getBannerData();
        ItemUtils.setBannerData(item, bannerData);
        KitItemBookData bookData = kitItem.getBookData();
        ItemUtils.setBookData(item, bookData);
        KitItemTrimData trimData = kitItem.getTrimData();
        ItemUtils.setArmorTrimData(item, trimData);
        List<String> attributes = kitItem.getAttributes();
        item = ItemUtils.setAttributes(this.plugin, item, attributes);
        meta = item.getItemMeta();
        List<String> flags = kitItem.getFlags();
        if (flags != null) {
            for (String flag : flags) {
                if (flag.equals("HIDE_ATTRIBUTES") && this.plugin.getDependencyManager().isPaper() && serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1)) {
                    ItemUtils.addDummyAttribute(meta, this.plugin);
                }
                meta.addItemFlags(new ItemFlag[]{ItemFlag.valueOf((String)flag)});
            }
        }
        item.setItemMeta(meta);
        if (!serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R4)) {
            List<String> nbtList = kitItem.getNbt();
            item = ItemUtils.setNBT(this.plugin, item, nbtList);
        }
        return item;
    }

    public void saveKitItemOnConfig(KitItem item, FileConfiguration config, String path) {
        if (item.getOriginalItem() != null) {
            config.set(path + ".original", (Object)item.getOriginalItem().clone());
        } else {
            KitItemTrimData trimData;
            KitItemBookData bookData;
            KitItemBannerData bannerData;
            KitItemFireworkData fireworkData;
            KitItemPotionData potionData;
            KitItemSkullData skullData;
            KitItemCustomModelComponentData customModelComponentData;
            config.set(path + ".id", (Object)item.getId());
            config.set(path + ".name", (Object)item.getName());
            config.set(path + ".amount", (Object)item.getAmount());
            if (item.getDurability() != 0) {
                config.set(path + ".durability", (Object)item.getDurability());
            }
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                config.set(path + ".lore", item.getLore());
            }
            if (item.getEnchants() != null && !item.getEnchants().isEmpty()) {
                config.set(path + ".enchants", item.getEnchants());
            }
            if (item.getFlags() != null && !item.getFlags().isEmpty()) {
                config.set(path + ".item_flags", item.getFlags());
            }
            if (item.getCustomModelData() != 0) {
                config.set(path + ".custom_model_data", (Object)item.getCustomModelData());
            }
            if ((customModelComponentData = item.getCustomModelComponentData()) != null) {
                if (!customModelComponentData.getFlags().isEmpty()) {
                    config.set(path + ".custom_model_component_data.flags", customModelComponentData.getFlags());
                }
                if (!customModelComponentData.getFloats().isEmpty()) {
                    config.set(path + ".custom_model_component_data.floats", customModelComponentData.getFloats());
                }
                if (!customModelComponentData.getColors().isEmpty()) {
                    config.set(path + ".custom_model_component_data.colors", customModelComponentData.getColors());
                }
                if (!customModelComponentData.getStrings().isEmpty()) {
                    config.set(path + ".custom_model_component_data.strings", customModelComponentData.getStrings());
                }
            }
            if (item.isHideTooltip()) {
                config.set(path + ".hide_tooltip", (Object)true);
            }
            if (item.getTooltipStyle() != null) {
                config.set(path + ".tooltip_style", (Object)item.getTooltipStyle());
            }
            if (item.getModel() != null) {
                config.set(path + ".model", (Object)item.getModel());
            }
            if (item.getColor() != 0) {
                config.set(path + ".color", (Object)item.getColor());
            }
            if (item.getNbt() != null && !item.getNbt().isEmpty()) {
                config.set(path + ".nbt", item.getNbt());
            }
            if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
                config.set(path + ".attributes", item.getAttributes());
            }
            if (item.getBookEnchants() != null && !item.getBookEnchants().isEmpty()) {
                config.set(path + ".book_enchants", item.getBookEnchants());
            }
            if (item.getCanPlace() != null && !item.getCanPlace().isEmpty()) {
                config.set(path + ".can_place", item.getCanPlace());
            }
            if (item.getCanDestroy() != null && !item.getCanDestroy().isEmpty()) {
                config.set(path + ".can_destroy", item.getCanDestroy());
            }
            if ((skullData = item.getSkullData()) != null) {
                config.set(path + ".skull_data.texture", (Object)skullData.getTexture());
                config.set(path + ".skull_data.id", (Object)skullData.getId());
                config.set(path + ".skull_data.owner", (Object)skullData.getOwner());
            }
            if ((potionData = item.getPotionData()) != null) {
                if (potionData.getPotionEffects() != null && !potionData.getPotionEffects().isEmpty()) {
                    config.set(path + ".potion_data.effects", potionData.getPotionEffects());
                }
                config.set(path + ".potion_data.extended", (Object)potionData.isExtended());
                config.set(path + ".potion_data.upgraded", (Object)potionData.isUpgraded());
                config.set(path + ".potion_data.type", (Object)potionData.getPotionType());
                if (potionData.getPotionColor() != 0) {
                    config.set(path + ".potion_data.color", (Object)potionData.getPotionColor());
                }
            }
            if ((fireworkData = item.getFireworkData()) != null) {
                if (fireworkData.getFireworkRocketEffects() != null && !fireworkData.getFireworkRocketEffects().isEmpty()) {
                    config.set(path + ".firework_data.rocket_effects", fireworkData.getFireworkRocketEffects());
                }
                config.set(path + ".firework_data.star_effect", (Object)fireworkData.getFireworkStarEffect());
                if (fireworkData.getFireworkPower() != 0) {
                    config.set(path + ".firework_data.power", (Object)fireworkData.getFireworkPower());
                }
            }
            if ((bannerData = item.getBannerData()) != null) {
                if (bannerData.getPatterns() != null && !bannerData.getPatterns().isEmpty()) {
                    config.set(path + ".banner_data.patterns", bannerData.getPatterns());
                }
                config.set(path + ".banner_data.base_color", (Object)bannerData.getBaseColor());
            }
            if ((bookData = item.getBookData()) != null) {
                config.set(path + ".book_data.author", (Object)bookData.getAuthor());
                config.set(path + ".book_data.title", (Object)bookData.getTitle());
                config.set(path + ".book_data.pages", bookData.getPages());
                config.set(path + ".book_data.generation", (Object)bookData.getGeneration());
            }
            if ((trimData = item.getTrimData()) != null) {
                config.set(path + ".trim_data.pattern", (Object)trimData.getPattern());
                config.set(path + ".trim_data.material", (Object)trimData.getMaterial());
            }
        }
        if (item.isOffhand()) {
            config.set(path + ".offhand", (Object)item.isOffhand());
        }
        if (item.getPreviewSlot() != -1) {
            config.set(path + ".preview_slot", (Object)item.getPreviewSlot());
        }
    }

    public KitItem getKitItemFromConfig(FileConfiguration config, String path) {
        boolean offhand = config.contains(path + ".offhand") ? config.getBoolean(path + ".offhand") : false;
        int previewSlot = config.contains(path + ".preview_slot") ? config.getInt(path + ".preview_slot") : -1;
        KitItem kitItem = null;
        if (config.contains(path + ".original")) {
            kitItem = new KitItem(config.getItemStack(path + ".original"));
        } else {
            String id = config.getString(path + ".id");
            String name = config.contains(path + ".name") ? config.getString(path + ".name") : null;
            List lore = config.contains(path + ".lore") ? config.getStringList(path + ".lore") : null;
            int amount = config.contains(path + ".amount") ? config.getInt(path + ".amount") : 1;
            short durability = config.contains(path + ".durability") ? (short)config.getInt(path + ".durability") : (short)0;
            int customModelData = config.contains(path + ".custom_model_data") ? config.getInt(path + ".custom_model_data") : 0;
            int color = config.contains(path + ".color") ? config.getInt(path + ".color") : 0;
            List enchants = config.contains(path + ".enchants") ? config.getStringList(path + ".enchants") : null;
            List flags = config.contains(path + ".item_flags") ? config.getStringList(path + ".item_flags") : null;
            List bookEnchants = config.contains(path + ".book_enchants") ? config.getStringList(path + ".book_enchants") : null;
            List nbtList = config.contains(path + ".nbt") ? config.getStringList(path + ".nbt") : null;
            List attributes = config.contains(path + ".attributes") ? config.getStringList(path + ".attributes") : null;
            List canPlace = config.contains(path + ".can_place") ? config.getStringList(path + ".can_place") : null;
            List canDestroy = config.contains(path + ".can_destroy") ? config.getStringList(path + ".can_destroy") : null;
            KitItemCustomModelComponentData customModelComponentData = null;
            if (config.contains(path + ".custom_model_component_data")) {
                ArrayList<String> cFlags = new ArrayList();
                ArrayList<String> cFloats = new ArrayList();
                ArrayList<String> cColors = new ArrayList();
                List<String> cStrings = new ArrayList<String>();
                if (config.contains(path + ".custom_model_component_data.flags")) {
                    cFlags = config.getStringList(path + ".custom_model_component_data.flags");
                }
                if (config.contains(path + ".custom_model_component_data.floats")) {
                    cFloats = config.getStringList(path + ".custom_model_component_data.floats");
                }
                if (config.contains(path + ".custom_model_component_data.colors")) {
                    cColors = config.getStringList(path + ".custom_model_component_data.colors");
                }
                if (config.contains(path + ".custom_model_component_data.strings")) {
                    cStrings = config.getStringList(path + ".custom_model_component_data.strings");
                }
                customModelComponentData = new KitItemCustomModelComponentData(cFlags, cColors, cFloats, cStrings);
            }
            boolean hideTooltip = config.getBoolean(path + ".hide_tooltip");
            String tooltipStyle = config.contains(path + ".tooltip_style") ? config.getString(path + ".tooltip_style") : null;
            String model = config.contains(path + ".model") ? config.getString(path + ".model") : null;
            KitItemSkullData skullData = null;
            if (config.contains(path + ".skull_data")) {
                String skullTexture = null;
                String skullId = null;
                String skullOwner = null;
                if (config.contains(path + ".skull_data.texture")) {
                    skullTexture = config.getString(path + ".skull_data.texture");
                }
                if (config.contains(path + ".skull_data.id")) {
                    skullId = config.getString(path + ".skull_data.id");
                }
                if (config.contains(path + ".skull_data.owner")) {
                    skullOwner = config.getString(path + ".skull_data.owner");
                }
                skullData = new KitItemSkullData(skullOwner, skullTexture, skullId);
            }
            KitItemPotionData potionData = null;
            if (config.contains(path + ".potion_data")) {
                List potionEffects = null;
                boolean extended = false;
                boolean upgraded = false;
                String potionType = null;
                int potionColor = 0;
                if (config.contains(path + ".potion_data.effects")) {
                    potionEffects = config.getStringList(path + ".potion_data.effects");
                }
                if (config.contains(path + ".potion_data.extended")) {
                    extended = config.getBoolean(path + ".potion_data.extended");
                }
                if (config.contains(path + ".potion_data.upgraded")) {
                    upgraded = config.getBoolean(path + ".potion_data.upgraded");
                }
                if (config.contains(path + ".potion_data.type")) {
                    potionType = config.getString(path + ".potion_data.type");
                }
                if (config.contains(path + ".potion_data.color")) {
                    potionColor = config.getInt(path + ".potion_data.color");
                }
                potionData = new KitItemPotionData(upgraded, extended, potionType, potionColor, potionEffects);
            }
            KitItemFireworkData fireworkData = null;
            if (config.contains(path + ".firework_data")) {
                List rocketEffects = null;
                String starEffect = null;
                int power = 0;
                if (config.contains(path + ".firework_data.rocket_effects")) {
                    rocketEffects = config.getStringList(path + ".firework_data.rocket_effects");
                }
                if (config.contains(path + ".firework_data.star_effect")) {
                    starEffect = config.getString(path + ".firework_data.star_effect");
                }
                if (config.contains(path + ".firework_data.power")) {
                    power = config.getInt(path + ".firework_data.power");
                }
                fireworkData = new KitItemFireworkData(rocketEffects, starEffect, power);
            }
            KitItemBannerData bannerData = null;
            if (config.contains(path + ".banner_data")) {
                List patterns = null;
                String baseColor = null;
                if (config.contains(path + ".banner_data.patterns")) {
                    patterns = config.getStringList(path + ".banner_data.patterns");
                }
                if (config.contains(path + ".banner_data.base_color")) {
                    baseColor = config.getString(path + ".banner_data.base_color");
                }
                bannerData = new KitItemBannerData(patterns, baseColor);
            }
            KitItemBookData bookData = null;
            if (config.contains(path + ".book_data")) {
                List pages = config.getStringList(path + ".book_data.pages");
                String author = null;
                String generation = null;
                String title = null;
                if (config.contains(path + ".book_data.author")) {
                    author = config.getString(path + ".book_data.author");
                }
                if (config.contains(path + ".book_data.generation")) {
                    generation = config.getString(path + ".book_data.generation");
                }
                if (config.contains(path + ".book_data.title")) {
                    title = config.getString(path + ".book_data.title");
                }
                bookData = new KitItemBookData(pages, author, generation, title);
            }
            KitItemTrimData trimData = null;
            if (config.contains(path + ".trim_data")) {
                String material = config.getString(path + ".trim_data.material");
                String pattern = config.getString(path + ".trim_data.pattern");
                trimData = new KitItemTrimData(pattern, material);
            }
            kitItem = new KitItem(id);
            kitItem.setName(name);
            kitItem.setLore(lore);
            kitItem.setAmount(amount);
            kitItem.setDurability(durability);
            kitItem.setCustomModelData(customModelData);
            kitItem.setColor(color);
            kitItem.setEnchants(enchants);
            kitItem.setFlags(flags);
            kitItem.setBookEnchants(bookEnchants);
            kitItem.setNbt(nbtList);
            kitItem.setAttributes(attributes);
            kitItem.setCanPlace(canPlace);
            kitItem.setCanDestroy(canDestroy);
            kitItem.setSkullData(skullData);
            kitItem.setPotionData(potionData);
            kitItem.setFireworkData(fireworkData);
            kitItem.setBannerData(bannerData);
            kitItem.setBookData(bookData);
            kitItem.setTrimData(trimData);
            kitItem.setCustomModelComponentData(customModelComponentData);
            kitItem.setHideTooltip(hideTooltip);
            kitItem.setTooltipStyle(tooltipStyle);
            kitItem.setModel(model);
        }
        kitItem.setOffhand(offhand);
        kitItem.setPreviewSlot(previewSlot);
        return kitItem;
    }

    public KitItem getKitItemFromV1Config(FileConfiguration config, String path) {
        String id = config.getString(path + ".id");
        String name = config.contains(path + ".name") ? config.getString(path + ".name") : null;
        List lore = config.contains(path + ".lore") ? config.getStringList(path + ".lore") : null;
        int amount = config.contains(path + ".amount") ? Integer.parseInt(config.getString(path + ".amount")) : 1;
        short durability = config.contains(path + ".durability") ? Short.parseShort(config.getString(path + ".durability")) : (short)0;
        int customModelData = config.contains(path + ".custom_model_data") ? Integer.parseInt(config.getString(path + ".custom_model_data")) : 0;
        int color = config.contains(path + ".color") ? Integer.parseInt(config.getString(path + ".color")) : 0;
        List enchants = config.contains(path + ".enchants") ? config.getStringList(path + ".enchants") : null;
        List flags = config.contains(path + ".hide-flags") ? config.getStringList(path + ".hide-flags") : null;
        List bookEnchants = config.contains(path + ".book-enchants") ? config.getStringList(path + ".book-enchants") : null;
        KitItemPotionData potionData = null;
        if (config.contains(path + ".potion-type")) {
            List potionEffects = null;
            boolean extended = false;
            boolean upgraded = false;
            String potionType = null;
            int potionColor = 0;
            if (config.contains(path + ".potion-effects")) {
                potionEffects = config.getStringList(path + ".potion-effects");
            }
            if (config.contains(path + ".potion-extended")) {
                extended = Boolean.parseBoolean(config.getString(path + ".potion-extended"));
            }
            if (config.contains(path + ".potion-upgraded")) {
                upgraded = Boolean.parseBoolean(config.getString(path + ".potion-upgraded"));
            }
            potionType = config.getString(path + ".potion-type");
            if (config.contains(path + ".potion-color")) {
                potionColor = config.getInt(path + ".potion-color");
            }
            potionData = new KitItemPotionData(upgraded, extended, potionType, potionColor, potionEffects);
        }
        KitItemBookData bookData = null;
        if (config.contains(path + ".book-title")) {
            List pages = config.getStringList(path + ".book-pages");
            String author = null;
            String generation = null;
            String title = null;
            if (config.contains(path + ".book-author")) {
                author = config.getString(path + ".book-author");
            }
            if (config.contains(path + ".book-generation")) {
                generation = config.getString(path + ".book-generation");
            }
            title = config.getString(path + ".book-title");
            bookData = new KitItemBookData(pages, author, generation, title);
        }
        KitItemFireworkData fireworkData = null;
        if (config.contains(path + ".firework-effects")) {
            List rocketEffects = null;
            int power = 0;
            rocketEffects = config.getStringList(path + ".firework-effects");
            if (config.contains(path + ".firework-power")) {
                power = Integer.parseInt(config.getString(path + ".firework-power"));
            }
            fireworkData = new KitItemFireworkData(rocketEffects, null, power);
        }
        KitItemBannerData bannerData = null;
        if (config.contains(path + ".banner-pattern")) {
            ArrayList<String> patterns = null;
            String baseColor = null;
            String[] pattern = config.getString(path + ".banner-pattern").split(";");
            patterns = new ArrayList<String>();
            for (String p : pattern) {
                String[] pSep = p.split(":");
                patterns.add(pSep[0] + ";" + pSep[1]);
            }
            if (config.contains(path + ".banner-color")) {
                baseColor = config.getString(path + ".banner-color");
            }
            bannerData = new KitItemBannerData(patterns, baseColor);
        }
        KitItemSkullData skullData = null;
        if (config.contains(path + ".skull-texture")) {
            String skullTexture = null;
            String skullId = null;
            skullTexture = config.getString(path + ".skull-texture");
            skullId = UUID.randomUUID().toString();
            skullData = new KitItemSkullData(null, skullTexture, skullId);
        }
        List<String> nbtList = new ArrayList();
        if (config.contains(path + ".nbt")) {
            nbtList = config.getStringList(path + ".nbt");
            for (int i = 0; i < nbtList.size(); ++i) {
                nbtList.set(i, ((String)nbtList.get(i)).replace(";", "|"));
            }
        }
        ArrayList<String> attributes = new ArrayList<String>();
        if (config.contains(path + ".attributes")) {
            for (String attributeName : config.getConfigurationSection(path + ".attributes").getKeys(false)) {
                String attribute = config.getString(path + ".attributes." + attributeName + ".modifiers");
                attributes.add(attribute);
            }
        }
        boolean offhand = config.contains(path + ".offhand") ? config.getBoolean(path + ".offhand") : false;
        KitItem kitItem = new KitItem(id);
        kitItem.setName(name);
        kitItem.setLore(lore);
        kitItem.setAmount(amount);
        kitItem.setDurability(durability);
        kitItem.setCustomModelData(customModelData);
        kitItem.setColor(color);
        kitItem.setEnchants(enchants);
        kitItem.setFlags(flags);
        kitItem.setBookEnchants(bookEnchants);
        kitItem.setNbt(nbtList);
        kitItem.setAttributes(attributes);
        kitItem.setSkullData(skullData);
        kitItem.setPotionData(potionData);
        kitItem.setFireworkData(fireworkData);
        kitItem.setBannerData(bannerData);
        kitItem.setBookData(bookData);
        kitItem.setOffhand(offhand);
        return kitItem;
    }

    public void replaceVariables(KitItem commonItem, ArrayList<KitVariable> variables, Player player) {
        if (commonItem.getName() != null) {
            String newName = commonItem.getName();
            for (KitVariable variable : variables) {
                newName = newName.replace(variable.getVariable(), variable.getValue());
            }
            newName = OtherUtils.replaceGlobalVariables(newName, player, this.plugin);
            commonItem.setName(newName);
        }
        if (commonItem.getLore() != null) {
            List<String> lore = commonItem.getLore();
            for (int i = 0; i < lore.size(); ++i) {
                for (KitVariable variable : variables) {
                    String line = lore.get(i).replace(variable.getVariable(), variable.getValue());
                    line = OtherUtils.replaceGlobalVariables(line, player, this.plugin);
                    lore.set(i, line);
                }
            }
            commonItem.setLore(lore);
        }
    }
}

