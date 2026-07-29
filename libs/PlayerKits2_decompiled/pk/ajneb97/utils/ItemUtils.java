/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Multimap
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.Property
 *  com.mojang.authlib.properties.PropertyMap
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.chat.ComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.DyeColor
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.block.Banner
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.banner.Pattern
 *  org.bukkit.block.banner.PatternType
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.EquipmentSlotGroup
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ArmorMeta
 *  org.bukkit.inventory.meta.BannerMeta
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.BookMeta
 *  org.bukkit.inventory.meta.BookMeta$Generation
 *  org.bukkit.inventory.meta.FireworkEffectMeta
 *  org.bukkit.inventory.meta.FireworkMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.inventory.meta.trim.ArmorTrim
 *  org.bukkit.inventory.meta.trim.TrimMaterial
 *  org.bukkit.inventory.meta.trim.TrimPattern
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionData
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.potion.PotionType
 *  org.bukkit.profile.PlayerProfile
 *  org.bukkit.profile.PlayerTextures
 */
package pk.ajneb97.utils;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.model.item.KitItemBannerData;
import pk.ajneb97.model.item.KitItemBookData;
import pk.ajneb97.model.item.KitItemFireworkData;
import pk.ajneb97.model.item.KitItemPotionData;
import pk.ajneb97.model.item.KitItemSkullData;
import pk.ajneb97.model.item.KitItemTrimData;
import pk.ajneb97.utils.OtherUtils;
import pk.ajneb97.utils.ServerVersion;

public class ItemUtils {
    public static ItemStack createItemFromID(String id) {
        String[] idsplit = new String[2];
        int DataValue = 0;
        ItemStack stack = null;
        if (id.contains(":")) {
            idsplit = id.split(":");
            String stringDataValue = idsplit[1];
            DataValue = Integer.valueOf(stringDataValue);
            Material mat = Material.getMaterial((String)idsplit[0].toUpperCase());
            stack = new ItemStack(mat, 1, (short)DataValue);
        } else {
            Material mat = Material.getMaterial((String)id.toUpperCase());
            stack = new ItemStack(mat, 1);
        }
        return stack;
    }

    public static ItemStack setTagStringItem(PlayerKits2 plugin, ItemStack item, String key, String value) {
        return plugin.getNmsManager().setTagStringItem(item, key, value);
    }

    public static String getTagStringItem(PlayerKits2 plugin, ItemStack item, String key) {
        if (item == null || item.getType().equals((Object)Material.AIR)) {
            return null;
        }
        return plugin.getNmsManager().getTagStringItem(item, key);
    }

    public static ItemStack removeTagItem(PlayerKits2 plugin, ItemStack item, String key) {
        return plugin.getNmsManager().removeTagItem(item, key);
    }

    public static List<String> getNBT(PlayerKits2 plugin, ItemStack item) {
        return plugin.getNmsManager().getNBT(item);
    }

    public static ItemStack setNBT(PlayerKits2 plugin, ItemStack item, List<String> nbtList) {
        return plugin.getNmsManager().setNBT(item, nbtList);
    }

    public static KitItemSkullData getSkullData(ItemStack item) {
        KitItemSkullData kitItemSkullData = null;
        String owner = null;
        String texture = null;
        String id = null;
        String typeName = item.getType().name();
        if (!typeName.equals("PLAYER_HEAD") && !typeName.equals("SKULL_ITEM")) {
            return null;
        }
        SkullMeta skullMeta = (SkullMeta)item.getItemMeta();
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1)) {
            PlayerProfile profile = skullMeta.getOwnerProfile();
            if (profile != null) {
                PlayerTextures textures;
                owner = profile.getName();
                if (profile.getUniqueId() != null) {
                    id = profile.getUniqueId().toString();
                }
                if (profile.getTextures() != null && (textures = profile.getTextures()) != null) {
                    JsonObject skinJsonObject = new JsonObject();
                    skinJsonObject.addProperty("url", textures.getSkin().toString());
                    JsonObject texturesJsonObject = new JsonObject();
                    texturesJsonObject.add("SKIN", (JsonElement)skinJsonObject);
                    JsonObject minecraftTexturesJsonObject = new JsonObject();
                    minecraftTexturesJsonObject.add("textures", (JsonElement)texturesJsonObject);
                    texture = new String(Base64.getEncoder().encode(minecraftTexturesJsonObject.toString().getBytes()));
                }
            }
        } else {
            try {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                GameProfile gameProfile = (GameProfile)profileField.get(skullMeta);
                if (gameProfile != null && gameProfile.getProperties() != null) {
                    PropertyMap propertyMap = gameProfile.getProperties();
                    owner = gameProfile.getName();
                    if (gameProfile.getId() != null) {
                        id = gameProfile.getId().toString();
                    }
                    for (Property p : propertyMap.values()) {
                        if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R2)) {
                            String pName = (String)p.getClass().getMethod("name", new Class[0]).invoke(p, new Object[0]);
                            if (!pName.equals("textures")) continue;
                            texture = (String)p.getClass().getMethod("value", new Class[0]).invoke(p, new Object[0]);
                            continue;
                        }
                        if (!p.getName().equals("textures")) continue;
                        texture = p.getValue();
                    }
                }
            }
            catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (texture != null || id != null || owner != null) {
            kitItemSkullData = new KitItemSkullData(owner, texture, id);
        }
        return kitItemSkullData;
    }

    public static void setSkullData(ItemStack item, KitItemSkullData skullData, Player player) {
        String typeName = item.getType().name();
        if (!typeName.equals("PLAYER_HEAD") && !typeName.equals("SKULL_ITEM")) {
            return;
        }
        if (skullData == null) {
            return;
        }
        String texture = skullData.getTexture();
        String owner = skullData.getOwner();
        if (owner != null && player != null) {
            owner = owner.replace("%player%", player.getName());
        }
        String id = skullData.getId();
        SkullMeta skullMeta = (SkullMeta)item.getItemMeta();
        if (owner != null) {
            skullMeta.setOwner(owner);
        }
        if (texture != null) {
            ServerVersion serverVersion = PlayerKits2.serverVersion;
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R2)) {
                URL url;
                UUID uuid = id != null ? UUID.fromString(id) : UUID.randomUUID();
                PlayerProfile profile = Bukkit.createPlayerProfile((UUID)uuid, (String)"playerkits");
                PlayerTextures textures = profile.getTextures();
                try {
                    String decoded = new String(Base64.getDecoder().decode(texture));
                    String decodedFormatted = decoded.replaceAll("\\s", "");
                    JsonObject jsonObject = (JsonObject)new Gson().fromJson(decodedFormatted, JsonObject.class);
                    String urlText = jsonObject.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
                    url = new URL(urlText);
                }
                catch (Exception error) {
                    error.printStackTrace();
                    return;
                }
                textures.setSkin(url);
                profile.setTextures(textures);
                skullMeta.setOwnerProfile(profile);
            } else {
                GameProfile profile = null;
                profile = id == null ? new GameProfile(UUID.randomUUID(), owner != null ? owner : "") : new GameProfile(UUID.fromString(id), owner != null ? owner : "");
                profile.getProperties().put((Object)"textures", (Object)new Property("textures", texture));
                try {
                    Field profileField = skullMeta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(skullMeta, profile);
                }
                catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException error) {
                    error.printStackTrace();
                }
            }
        }
        item.setItemMeta((ItemMeta)skullMeta);
    }

    public static KitItemPotionData getPotionData(ItemStack item) {
        ServerVersion serverVersion;
        KitItemPotionData potionData = null;
        ArrayList<String> potionEffectsList = new ArrayList<String>();
        boolean upgraded = false;
        boolean extended = false;
        String potionType = null;
        int potionColor = 0;
        String typeName = item.getType().name();
        if (!typeName.contains("POTION") && !typeName.equals("TIPPED_ARROW")) {
            return null;
        }
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return null;
        }
        PotionMeta meta = (PotionMeta)item.getItemMeta();
        if (meta.hasCustomEffects()) {
            List potionEffects = meta.getCustomEffects();
            for (int i = 0; i < potionEffects.size(); ++i) {
                String type = ((PotionEffect)potionEffects.get(i)).getType().getName();
                int amplifier = ((PotionEffect)potionEffects.get(i)).getAmplifier();
                int duration = ((PotionEffect)potionEffects.get(i)).getDuration();
                potionEffectsList.add(type + ";" + amplifier + ";" + duration);
            }
        }
        if ((serverVersion = PlayerKits2.serverVersion).serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_9_R1)) {
            if (meta.hasColor()) {
                potionColor = meta.getColor().asRGB();
            }
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R2)) {
                if (meta.getBasePotionType() != null) {
                    potionType = meta.getBasePotionType().name();
                }
            } else {
                PotionData basePotionData = meta.getBasePotionData();
                extended = basePotionData.isExtended();
                upgraded = basePotionData.isUpgraded();
                potionType = basePotionData.getType().name();
            }
        }
        potionData = new KitItemPotionData(upgraded, extended, potionType, potionColor, potionEffectsList);
        return potionData;
    }

    public static void setPotionData(ItemStack item, KitItemPotionData potionData) {
        ServerVersion serverVersion;
        String typeName = item.getType().name();
        if (!typeName.contains("POTION") && !typeName.equals("TIPPED_ARROW")) {
            return;
        }
        if (potionData == null) {
            return;
        }
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return;
        }
        PotionMeta meta = (PotionMeta)item.getItemMeta();
        List<String> potionEffects = potionData.getPotionEffects();
        if (potionEffects != null) {
            for (int i = 0; i < potionEffects.size(); ++i) {
                String[] sep = potionEffects.get(i).split(";");
                String type = sep[0];
                int amplifier = Integer.valueOf(sep[1]);
                int duration = Integer.valueOf(sep[2]);
                meta.addCustomEffect(new PotionEffect(PotionEffectType.getByName((String)type), duration, amplifier), false);
            }
        }
        if ((serverVersion = PlayerKits2.serverVersion).serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_9_R1)) {
            int color = potionData.getPotionColor();
            if (color != 0) {
                meta.setColor(Color.fromRGB((int)color));
            }
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_20_R2)) {
                if (potionData.getPotionType() != null && !potionData.getPotionType().isEmpty()) {
                    meta.setBasePotionType(PotionType.valueOf((String)potionData.getPotionType()));
                }
            } else {
                PotionData basePotionData = new PotionData(PotionType.valueOf((String)potionData.getPotionType()), potionData.isExtended(), potionData.isUpgraded());
                meta.setBasePotionData(basePotionData);
            }
        }
        item.setItemMeta((ItemMeta)meta);
    }

    public static KitItemBannerData getBannerData(ItemStack item) {
        BannerMeta meta;
        KitItemBannerData bannerData = null;
        ArrayList<String> bannerPatterns = new ArrayList<String>();
        String baseColor = null;
        List patterns = new ArrayList();
        String typeName = item.getType().name();
        if (typeName.contains("BANNER") || typeName.contains("PATTERN")) {
            if (item.getItemMeta() instanceof BannerMeta) {
                meta = (BannerMeta)item.getItemMeta();
                patterns = meta.getPatterns();
            }
        } else if (typeName.equals("SHIELD")) {
            meta = (BlockStateMeta)item.getItemMeta();
            if (meta.hasBlockState()) {
                Banner banner = (Banner)meta.getBlockState();
                patterns = banner.getPatterns();
                baseColor = OtherUtils.isLegacy() ? banner.getBaseColor().name() : banner.getType().name();
            }
        } else {
            return null;
        }
        for (Pattern p : patterns) {
            bannerPatterns.add(p.getColor().name() + ";" + ItemUtils.getBannerPatternName(p));
        }
        if (bannerPatterns.isEmpty() && baseColor == null) {
            return null;
        }
        bannerData = new KitItemBannerData(bannerPatterns, baseColor);
        return bannerData;
    }

    public static void setBannerData(ItemStack item, KitItemBannerData bannerData) {
        String typeName = item.getType().name();
        if (bannerData == null) {
            return;
        }
        List<String> bannerPatterns = bannerData.getPatterns();
        String baseColor = bannerData.getBaseColor();
        if (typeName.contains("BANNER") || typeName.contains("PATTERN")) {
            BannerMeta meta = (BannerMeta)item.getItemMeta();
            if (bannerPatterns != null) {
                for (String pattern : bannerPatterns) {
                    String[] patternSplit = pattern.split(";");
                    String patternColor = patternSplit[0];
                    String patternName = patternSplit[1];
                    meta.addPattern(new Pattern(DyeColor.valueOf((String)patternColor), ItemUtils.getBannerPatternByName(patternName)));
                }
                item.setItemMeta((ItemMeta)meta);
            }
        } else if (typeName.equals("SHIELD")) {
            BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
            Banner banner = (Banner)meta.getBlockState();
            if (OtherUtils.isLegacy()) {
                banner.setBaseColor(DyeColor.valueOf((String)baseColor));
            } else {
                String fixedColor = baseColor.replace("_BANNER", "");
                banner.setBaseColor(DyeColor.valueOf((String)fixedColor));
            }
            if (bannerPatterns != null) {
                for (String pattern : bannerPatterns) {
                    String[] patternSplit = pattern.split(";");
                    String patternColor = patternSplit[0];
                    String patternName = patternSplit[1];
                    banner.addPattern(new Pattern(DyeColor.valueOf((String)patternColor), ItemUtils.getBannerPatternByName(patternName)));
                }
            }
            banner.update();
            meta.setBlockState((BlockState)banner);
            item.setItemMeta((ItemMeta)meta);
        }
    }

    public static KitItemFireworkData getFireworkData(ItemStack item) {
        FireworkMeta meta;
        KitItemFireworkData fireworkData = null;
        ArrayList<String> fireworkRocketEffects = new ArrayList<String>();
        String fireworkStarEffect = null;
        int fireworkPower = 0;
        String typeName = item.getType().name();
        boolean isFireworkCharge = false;
        List<FireworkEffect> effects = new ArrayList();
        if (typeName.equals("FIREWORK") || typeName.equals("FIREWORK_ROCKET")) {
            meta = (FireworkMeta)item.getItemMeta();
            fireworkPower = meta.getPower();
            effects = meta.getEffects();
        } else if (typeName.equals("FIREWORK_STAR") || typeName.equals("FIREWORK_CHARGE")) {
            meta = (FireworkEffectMeta)item.getItemMeta();
            FireworkEffect effect = meta.getEffect();
            effects.add(effect);
            isFireworkCharge = true;
        } else {
            return null;
        }
        for (FireworkEffect e : effects) {
            if (e == null) continue;
            String line = e.getType().name() + ";";
            List colors = e.getColors();
            String colorsLine = "";
            for (int i = 0; i < colors.size(); ++i) {
                colorsLine = colors.size() <= i + 1 ? colorsLine + ((Color)colors.get(i)).asRGB() + ";" : colorsLine + ((Color)colors.get(i)).asRGB() + ",";
            }
            List fadeColors = e.getFadeColors();
            String fadeColorsLine = "";
            for (int i = 0; i < fadeColors.size(); ++i) {
                fadeColorsLine = fadeColors.size() <= i + 1 ? fadeColorsLine + ((Color)fadeColors.get(i)).asRGB() : fadeColorsLine + ((Color)fadeColors.get(i)).asRGB() + ",";
            }
            line = line + colorsLine + fadeColorsLine + ";" + e.hasFlicker() + ";" + e.hasTrail();
            if (isFireworkCharge) {
                fireworkStarEffect = line;
                break;
            }
            fireworkRocketEffects.add(line);
        }
        fireworkData = new KitItemFireworkData(fireworkRocketEffects, fireworkStarEffect, fireworkPower);
        return fireworkData;
    }

    public static void setFireworkData(ItemStack item, KitItemFireworkData fireworkData) {
        String typeName = item.getType().name();
        if (fireworkData == null) {
            return;
        }
        int power = fireworkData.getFireworkPower();
        List<String> rocketEffects = fireworkData.getFireworkRocketEffects();
        String starEffect = fireworkData.getFireworkStarEffect();
        if (typeName.equals("FIREWORK") || typeName.equals("FIREWORK_ROCKET")) {
            FireworkMeta meta = (FireworkMeta)item.getItemMeta();
            if (rocketEffects != null) {
                for (int i = 0; i < rocketEffects.size(); ++i) {
                    FireworkEffect effect = ItemUtils.getFireworkEffect(rocketEffects.get(i));
                    meta.addEffect(effect);
                }
            }
            meta.setPower(power);
            item.setItemMeta((ItemMeta)meta);
        } else if (typeName.equals("FIREWORK_STAR") || typeName.equals("FIREWORK_CHARGE")) {
            FireworkEffectMeta meta = (FireworkEffectMeta)item.getItemMeta();
            if (starEffect != null) {
                meta.setEffect(ItemUtils.getFireworkEffect(starEffect));
            }
            item.setItemMeta((ItemMeta)meta);
        }
    }

    private static FireworkEffect getFireworkEffect(String line) {
        String[] sep = line.split(";");
        String type = sep[0];
        String[] colors = sep[1].split(",");
        ArrayList<Color> colorsList = new ArrayList<Color>();
        for (int c = 0; c < colors.length; ++c) {
            colorsList.add(Color.fromRGB((int)Integer.valueOf(colors[c])));
        }
        ArrayList<Color> fadeColorsList = new ArrayList<Color>();
        if (!sep[2].equals("")) {
            String[] fadeColors = sep[2].split(",");
            for (int c = 0; c < fadeColors.length; ++c) {
                fadeColorsList.add(Color.fromRGB((int)Integer.valueOf(fadeColors[c])));
            }
        }
        boolean flicker = Boolean.valueOf(sep[3]);
        boolean trail = Boolean.valueOf(sep[4]);
        return FireworkEffect.builder().flicker(flicker).trail(trail).with(FireworkEffect.Type.valueOf((String)type)).withColor(colorsList).withFade(fadeColorsList).build();
    }

    public static List<String> getAttributes(PlayerKits2 plugin, ItemStack item) {
        if (OtherUtils.isLegacy()) {
            return plugin.getNmsManager().getAttributes(item);
        }
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        boolean newSystem = serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1);
        ItemMeta meta = item.getItemMeta();
        if (meta.hasAttributeModifiers()) {
            Multimap attributes = meta.getAttributeModifiers();
            Set set = attributes.keySet();
            ArrayList<String> attributeList = new ArrayList<String>();
            for (Attribute a : set) {
                Collection listModifiers = attributes.get((Object)a);
                for (AttributeModifier m : listModifiers) {
                    String line;
                    if (newSystem) {
                        line = ItemUtils.getAttributeName(a) + ";" + m.getOperation().name() + ";" + m.getAmount() + ";" + m.getKey().getNamespace() + ":" + m.getKey().getKey();
                        line = line + ";" + m.getSlotGroup().toString();
                    } else {
                        line = ItemUtils.getAttributeName(a) + ";" + m.getOperation().name() + ";" + m.getAmount() + ";" + m.getUniqueId();
                        if (m.getSlot() != null) {
                            line = line + ";" + m.getSlot().name();
                        }
                        line = line + ";custom_name:" + m.getName();
                    }
                    attributeList.add(line);
                }
            }
            return attributeList;
        }
        return null;
    }

    public static ItemStack setAttributes(PlayerKits2 plugin, ItemStack item, List<String> attributes) {
        if (attributes == null) {
            return item;
        }
        if (OtherUtils.isLegacy()) {
            return plugin.getNmsManager().setAttributes(item, attributes);
        }
        ServerVersion serverVersion = PlayerKits2.serverVersion;
        boolean newSystem = serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1);
        try {
            ItemMeta meta = item.getItemMeta();
            for (String a : attributes) {
                String[] sep = a.split(";");
                String attribute = sep[0];
                AttributeModifier.Operation op = AttributeModifier.Operation.valueOf((String)sep[1]);
                double amount = Double.valueOf(sep[2]);
                AttributeModifier modifier = null;
                if (newSystem) {
                    String[] id = sep[3].split(":");
                    NamespacedKey namespacedKey = new NamespacedKey(id[0], id[1]);
                    modifier = new AttributeModifier(namespacedKey, amount, op, EquipmentSlotGroup.getByName((String)sep[4]));
                } else {
                    String customName = attribute;
                    for (int i = 0; i < sep.length; ++i) {
                        if (!sep[i].startsWith("custom_name:")) continue;
                        customName = sep[i].replace("custom_name:", "");
                    }
                    UUID uuid = UUID.fromString(sep[3]);
                    if (sep.length >= 5) {
                        if (!sep[4].startsWith("custom_name:")) {
                            EquipmentSlot slot = EquipmentSlot.valueOf((String)sep[4]);
                            modifier = new AttributeModifier(uuid, customName, amount, op, slot);
                        } else {
                            modifier = new AttributeModifier(uuid, customName, amount, op);
                        }
                    } else {
                        modifier = new AttributeModifier(uuid, customName, amount, op);
                    }
                }
                meta.addAttributeModifier(ItemUtils.getAttributeByName(attribute), modifier);
            }
            item.setItemMeta(meta);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return item;
    }

    public static void addDummyAttribute(ItemMeta meta, PlayerKits2 plugin) {
        try {
            AttributeModifier modifier = new AttributeModifier(new NamespacedKey((Plugin)plugin, "dummy_attribute"), 0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
            ServerVersion serverVersion = PlayerKits2.serverVersion;
            if (serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R2)) {
                meta.addAttributeModifier(Attribute.GRAVITY, modifier);
            } else {
                meta.addAttributeModifier(ItemUtils.getAttributeByName("GENERIC_GRAVITY"), modifier);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static KitItemBookData getBookData(ItemStack item) {
        KitItemBookData bookData = null;
        String typeName = item.getType().name();
        ArrayList<String> pages = new ArrayList<String>();
        String author = null;
        String generation = null;
        String title = null;
        if (typeName.equals("WRITTEN_BOOK")) {
            BookMeta meta = (BookMeta)item.getItemMeta();
            author = meta.getAuthor();
            title = meta.getTitle();
            if (!Bukkit.getVersion().contains("1.12") && OtherUtils.isLegacy()) {
                pages = new ArrayList(meta.getPages());
            } else {
                if (meta.getGeneration() != null) {
                    generation = meta.getGeneration().name();
                }
                for (BaseComponent[] page : meta.spigot().getPages()) {
                    pages.add(ComponentSerializer.toString((BaseComponent[])page));
                }
            }
            bookData = new KitItemBookData(pages, author, generation, title);
            return bookData;
        }
        return null;
    }

    public static void setBookData(ItemStack item, KitItemBookData bookData) {
        String typeName = item.getType().name();
        if (bookData == null) {
            return;
        }
        String author = bookData.getAuthor();
        String generation = bookData.getGeneration();
        String title = bookData.getTitle();
        List<String> pages = bookData.getPages();
        if (typeName.equals("WRITTEN_BOOK")) {
            BookMeta meta = (BookMeta)item.getItemMeta();
            if (!Bukkit.getVersion().contains("1.12") && OtherUtils.isLegacy()) {
                meta.setPages(new ArrayList<String>(pages));
            } else {
                ArrayList<BaseComponent[]> pagesBaseComponent = new ArrayList<BaseComponent[]>();
                for (String page : pages) {
                    pagesBaseComponent.add(ComponentSerializer.parse((String)page));
                }
                meta.spigot().setPages(pagesBaseComponent);
                if (generation != null) {
                    meta.setGeneration(BookMeta.Generation.valueOf((String)generation));
                }
            }
            meta.setAuthor(author);
            meta.setTitle(title);
            item.setItemMeta((ItemMeta)meta);
        }
    }

    public static KitItemTrimData getArmorTrimData(ItemStack item) {
        ArmorMeta meta;
        if (!OtherUtils.isTrimNew()) {
            return null;
        }
        String armorTrimPattern = null;
        String armorTrimMaterial = null;
        if (item.getItemMeta() instanceof ArmorMeta) {
            meta = (ArmorMeta)item.getItemMeta();
            if (!meta.hasTrim()) {
                return null;
            }
        } else {
            return null;
        }
        ArmorTrim armorTrim = meta.getTrim();
        armorTrimPattern = armorTrim.getPattern().getKey().getKey();
        armorTrimMaterial = armorTrim.getMaterial().getKey().getKey();
        return new KitItemTrimData(armorTrimPattern, armorTrimMaterial);
    }

    public static void setArmorTrimData(ItemStack item, KitItemTrimData trimData) {
        if (trimData == null || !OtherUtils.isTrimNew()) {
            return;
        }
        String pattern = trimData.getPattern();
        String material = trimData.getMaterial();
        if (item.getItemMeta() instanceof ArmorMeta) {
            ArmorMeta meta = (ArmorMeta)item.getItemMeta();
            if (pattern != null && material != null) {
                ArmorTrim armorTrim = new ArmorTrim((TrimMaterial)Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft((String)material.toLowerCase())), (TrimPattern)Registry.TRIM_PATTERN.get(NamespacedKey.minecraft((String)pattern.toLowerCase())));
                meta.setTrim(armorTrim);
                item.setItemMeta((ItemMeta)meta);
            }
        }
    }

    private static String getBannerPatternName(Pattern p) {
        try {
            PatternType patternType = p.getPattern();
            Method getPatternName = patternType.getClass().getMethod("name", new Class[0]);
            getPatternName.setAccessible(true);
            return (String)getPatternName.invoke(patternType, new Object[0]);
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static PatternType getBannerPatternByName(String name) {
        try {
            Class<?> patternTypeClass = Class.forName("org.bukkit.block.banner.PatternType");
            Method valueOf = patternTypeClass.getMethod("valueOf", String.class);
            return (PatternType)valueOf.invoke(null, name);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Attribute getAttributeByName(String name) {
        try {
            Class<?> attributeTypeClass = Class.forName("org.bukkit.attribute.Attribute");
            Field field = attributeTypeClass.getField(name);
            return (Attribute)field.get(null);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getAttributeName(Object attribute) {
        try {
            Class<?> attributeTypeClass = Class.forName("org.bukkit.attribute.Attribute");
            return (String)attributeTypeClass.getMethod("name", new Class[0]).invoke(attribute, new Object[0]);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

