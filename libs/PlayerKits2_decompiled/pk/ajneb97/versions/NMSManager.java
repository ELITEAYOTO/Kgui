/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package pk.ajneb97.versions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pk.ajneb97.PlayerKits2;
import pk.ajneb97.utils.ServerVersion;
import pk.ajneb97.versions.Version;

public class NMSManager {
    private Version version;
    private ServerVersion serverVersion;
    private PlayerKits2 plugin;

    public NMSManager(PlayerKits2 plugin) {
        this.plugin = plugin;
        this.version = new Version();
        this.serverVersion = PlayerKits2.serverVersion;
        if (this.serverVersionGreaterEqualThan(ServerVersion.v1_20_R4)) {
            return;
        }
        try {
            this.version.addClass("CraftItemStack", Class.forName("org.bukkit.craftbukkit." + (Object)((Object)this.serverVersion) + ".inventory.CraftItemStack"));
            if (this.serverVersionGreaterEqualThan(ServerVersion.v1_17_R1)) {
                this.version.addClass("ItemStackNMS", Class.forName("net.minecraft.world.item.ItemStack"));
                this.version.addClass("NBTTagCompound", Class.forName("net.minecraft.nbt.NBTTagCompound"));
                this.version.addClass("MojangsonParser", Class.forName("net.minecraft.nbt.MojangsonParser"));
                this.version.addClass("NBTBase", Class.forName("net.minecraft.nbt.NBTBase"));
                this.version.addClass("NBTTagList", Class.forName("net.minecraft.nbt.NBTTagList"));
            } else {
                this.version.addClass("ItemStackNMS", Class.forName("net.minecraft.server." + (Object)((Object)this.serverVersion) + ".ItemStack"));
                this.version.addClass("NBTTagCompound", Class.forName("net.minecraft.server." + (Object)((Object)this.serverVersion) + ".NBTTagCompound"));
                this.version.addClass("MojangsonParser", Class.forName("net.minecraft.server." + (Object)((Object)this.serverVersion) + ".MojangsonParser"));
                this.version.addClass("NBTBase", Class.forName("net.minecraft.server." + (Object)((Object)this.serverVersion) + ".NBTBase"));
                this.version.addClass("NBTTagList", Class.forName("net.minecraft.server." + (Object)((Object)this.serverVersion) + ".NBTTagList"));
            }
            this.version.addMethod("asNMSCopy", this.version.getClassRef("CraftItemStack").getMethod("asNMSCopy", ItemStack.class));
            this.version.addMethod("asBukkitCopy", this.version.getClassRef("CraftItemStack").getMethod("asBukkitCopy", this.version.getClassRef("ItemStackNMS")));
            if (!this.serverVersionGreaterEqualThan(ServerVersion.v1_13_R1)) {
                this.version.addMethod("listSize", this.version.getClassRef("NBTTagList").getMethod("size", new Class[0]));
                this.version.addMethod("listGet", this.version.getClassRef("NBTTagList").getMethod("get", Integer.TYPE));
                this.version.addMethod("listAdd", this.version.getClassRef("NBTTagList").getMethod("add", this.version.getClassRef("NBTBase")));
            }
            if (!this.serverVersionGreaterEqualThan(ServerVersion.v1_18_R1)) {
                this.version.addMethod("hasTag", this.version.getClassRef("ItemStackNMS").getMethod("hasTag", new Class[0]));
                this.version.addMethod("getTag", this.version.getClassRef("ItemStackNMS").getMethod("getTag", new Class[0]));
                this.version.addMethod("setTag", this.version.getClassRef("ItemStackNMS").getMethod("setTag", this.version.getClassRef("NBTTagCompound")));
                this.version.addMethod("setString", this.version.getClassRef("NBTTagCompound").getMethod("setString", String.class, String.class));
                this.version.addMethod("setBoolean", this.version.getClassRef("NBTTagCompound").getMethod("setBoolean", String.class, Boolean.TYPE));
                this.version.addMethod("setDouble", this.version.getClassRef("NBTTagCompound").getMethod("setDouble", String.class, Double.TYPE));
                this.version.addMethod("setInt", this.version.getClassRef("NBTTagCompound").getMethod("setInt", String.class, Integer.TYPE));
                this.version.addMethod("set", this.version.getClassRef("NBTTagCompound").getMethod("set", String.class, this.version.getClassRef("NBTBase")));
                this.version.addMethod("hasKey", this.version.getClassRef("NBTTagCompound").getMethod("hasKey", String.class));
                this.version.addMethod("getString", this.version.getClassRef("NBTTagCompound").getMethod("getString", String.class));
                this.version.addMethod("getBoolean", this.version.getClassRef("NBTTagCompound").getMethod("getBoolean", String.class));
                this.version.addMethod("getInt", this.version.getClassRef("NBTTagCompound").getMethod("getInt", String.class));
                this.version.addMethod("getDouble", this.version.getClassRef("NBTTagCompound").getMethod("getDouble", String.class));
                this.version.addMethod("getCompound", this.version.getClassRef("NBTTagCompound").getMethod("getCompound", String.class));
                this.version.addMethod("getList", this.version.getClassRef("NBTTagCompound").getMethod("getList", String.class, Integer.TYPE));
                this.version.addMethod("get", this.version.getClassRef("NBTTagCompound").getMethod("get", String.class));
                this.version.addMethod("remove", this.version.getClassRef("NBTTagCompound").getMethod("remove", String.class));
                if (this.serverVersionGreaterEqualThan(ServerVersion.v1_13_R1)) {
                    this.version.addMethod("getKeys", this.version.getClassRef("NBTTagCompound").getMethod("getKeys", new Class[0]));
                } else {
                    this.version.addMethod("getKeys", this.version.getClassRef("NBTTagCompound").getMethod("c", new Class[0]));
                }
                this.version.addMethod("hasKeyOfType", this.version.getClassRef("NBTTagCompound").getMethod("hasKeyOfType", String.class, Integer.TYPE));
                this.version.addMethod("parse", this.version.getClassRef("MojangsonParser").getMethod("parse", String.class));
            } else {
                String methodName = null;
                switch (this.serverVersion) {
                    case v1_18_R1: {
                        methodName = "r";
                        break;
                    }
                    case v1_18_R2: {
                        methodName = "s";
                        break;
                    }
                    case v1_19_R1: 
                    case v1_19_R2: 
                    case v1_19_R3: {
                        methodName = "t";
                        break;
                    }
                    case v1_20_R1: 
                    case v1_20_R2: 
                    case v1_20_R3: {
                        methodName = "u";
                    }
                }
                this.version.addMethod("hasTag", this.version.getClassRef("ItemStackNMS").getMethod(methodName, new Class[0]));
                methodName = null;
                switch (this.serverVersion) {
                    case v1_18_R1: {
                        methodName = "s";
                        break;
                    }
                    case v1_18_R2: {
                        methodName = "t";
                        break;
                    }
                    case v1_19_R1: 
                    case v1_19_R2: 
                    case v1_19_R3: {
                        methodName = "u";
                        break;
                    }
                    case v1_20_R1: 
                    case v1_20_R2: 
                    case v1_20_R3: {
                        methodName = "v";
                    }
                }
                this.version.addMethod("getTag", this.version.getClassRef("ItemStackNMS").getMethod(methodName, new Class[0]));
                this.version.addMethod("setTag", this.version.getClassRef("ItemStackNMS").getMethod("c", this.version.getClassRef("NBTTagCompound")));
                this.version.addMethod("setString", this.version.getClassRef("NBTTagCompound").getMethod("a", String.class, String.class));
                this.version.addMethod("setBoolean", this.version.getClassRef("NBTTagCompound").getMethod("a", String.class, Boolean.TYPE));
                this.version.addMethod("setDouble", this.version.getClassRef("NBTTagCompound").getMethod("a", String.class, Double.TYPE));
                this.version.addMethod("setInt", this.version.getClassRef("NBTTagCompound").getMethod("a", String.class, Integer.TYPE));
                this.version.addMethod("set", this.version.getClassRef("NBTTagCompound").getMethod("a", String.class, this.version.getClassRef("NBTBase")));
                this.version.addMethod("hasKey", this.version.getClassRef("NBTTagCompound").getMethod("e", String.class));
                this.version.addMethod("getString", this.version.getClassRef("NBTTagCompound").getMethod("l", String.class));
                this.version.addMethod("getBoolean", this.version.getClassRef("NBTTagCompound").getMethod("q", String.class));
                this.version.addMethod("getInt", this.version.getClassRef("NBTTagCompound").getMethod("h", String.class));
                this.version.addMethod("getDouble", this.version.getClassRef("NBTTagCompound").getMethod("k", String.class));
                this.version.addMethod("getCompound", this.version.getClassRef("NBTTagCompound").getMethod("p", String.class));
                this.version.addMethod("getList", this.version.getClassRef("NBTTagCompound").getMethod("c", String.class, Integer.TYPE));
                this.version.addMethod("get", this.version.getClassRef("NBTTagCompound").getMethod("c", String.class));
                this.version.addMethod("remove", this.version.getClassRef("NBTTagCompound").getMethod("r", String.class));
                methodName = null;
                switch (this.serverVersion) {
                    case v1_18_R1: 
                    case v1_18_R2: 
                    case v1_19_R1: {
                        methodName = "d";
                        break;
                    }
                    case v1_19_R2: 
                    case v1_19_R3: 
                    case v1_20_R1: 
                    case v1_20_R2: 
                    case v1_20_R3: {
                        methodName = "e";
                    }
                }
                this.version.addMethod("getKeys", this.version.getClassRef("NBTTagCompound").getMethod(methodName, new Class[0]));
                this.version.addMethod("hasKeyOfType", this.version.getClassRef("NBTTagCompound").getMethod("b", String.class, Integer.TYPE));
                this.version.addMethod("parse", this.version.getClassRef("MojangsonParser").getMethod("a", String.class));
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public ItemStack setTagStringItem(ItemStack item, String key, String value) {
        if (this.serverVersionGreaterEqualThan(ServerVersion.v1_20_R4)) {
            ItemMeta meta = item.getItemMeta();
            NamespacedKey namespacedKey = new NamespacedKey((Plugin)this.plugin, key);
            PersistentDataContainer p = meta.getPersistentDataContainer();
            p.set(namespacedKey, PersistentDataType.STRING, (Object)value);
            item.setItemMeta(meta);
            return item;
        }
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            Object compound = this.getNBTCompound(newItem);
            this.version.getMethodRef("setString").invoke(compound, key, value);
            this.version.getMethodRef("setTag").invoke(newItem, compound);
            return (ItemStack)this.version.getMethodRef("asBukkitCopy").invoke(null, newItem);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return item;
        }
    }

    public String getTagStringItem(ItemStack item, String key) {
        if (this.serverVersionGreaterEqualThan(ServerVersion.v1_20_R4)) {
            ItemMeta meta = item.getItemMeta();
            NamespacedKey namespacedKey = new NamespacedKey((Plugin)this.plugin, key);
            PersistentDataContainer p = meta.getPersistentDataContainer();
            if (p.has(namespacedKey)) {
                return (String)p.get(namespacedKey, PersistentDataType.STRING);
            }
            return null;
        }
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            Object compound = this.getNBTCompound(newItem);
            if (((Boolean)this.version.getMethodRef("hasKey").invoke(compound, key)).booleanValue()) {
                return (String)this.version.getMethodRef("getString").invoke(compound, key);
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ItemStack removeTagItem(ItemStack item, String key) {
        if (this.serverVersionGreaterEqualThan(ServerVersion.v1_20_R4)) {
            ItemMeta meta = item.getItemMeta();
            NamespacedKey namespacedKey = new NamespacedKey((Plugin)this.plugin, key);
            PersistentDataContainer p = meta.getPersistentDataContainer();
            if (p.has(namespacedKey)) {
                p.remove(namespacedKey);
            }
            item.setItemMeta(meta);
            return item;
        }
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            Object compound = this.getNBTCompound(newItem);
            this.version.getMethodRef("remove").invoke(compound, key);
            this.version.getMethodRef("setTag").invoke(newItem, compound);
            return (ItemStack)this.version.getMethodRef("asBukkitCopy").invoke(null, newItem);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return item;
        }
    }

    public List<String> getNBT(ItemStack item) {
        ArrayList<String> nbtList = new ArrayList<String>();
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            if (((Boolean)this.version.getMethodRef("hasTag").invoke(newItem, null)).booleanValue()) {
                Object compound = this.version.getMethodRef("getTag").invoke(newItem, null);
                Set tags = (Set)this.version.getMethodRef("getKeys").invoke(compound, null);
                HashSet<String> notTags = new HashSet<String>(Arrays.asList("ench", "HideFlags", "display", "SkullOwner", "AttributeModifiers", "Enchantments", "Damage", "CustomModelData", "Potion", "StoredEnchantments", "CustomPotionColor", "CustomPotionEffects", "Fireworks", "Explosion", "pages", "title", "author", "resolved", "generation", "Trim", "custom_potion_effects", "CanPlaceOn", "CanDestroy"));
                for (String t : tags) {
                    if (notTags.contains(t) || t.equals("BlockEntityTag") && !item.getType().name().contains("SHULKER") && !item.getType().name().contains("CHEST")) continue;
                    if (((Boolean)this.version.getMethodRef("hasKeyOfType").invoke(compound, t, 1)).booleanValue()) {
                        nbtList.add(t + "|" + this.version.getMethodRef("getBoolean").invoke(compound, t) + "|boolean");
                        continue;
                    }
                    if (((Boolean)this.version.getMethodRef("hasKeyOfType").invoke(compound, t, 3)).booleanValue()) {
                        nbtList.add(t + "|" + this.version.getMethodRef("getInt").invoke(compound, t) + "|int");
                        continue;
                    }
                    if (((Boolean)this.version.getMethodRef("hasKeyOfType").invoke(compound, t, 6)).booleanValue()) {
                        nbtList.add(t + "|" + this.version.getMethodRef("getDouble").invoke(compound, t) + "|double");
                        continue;
                    }
                    if (((Boolean)this.version.getMethodRef("hasKeyOfType").invoke(compound, t, 10)).booleanValue()) {
                        nbtList.add(t + "|" + this.version.getMethodRef("getCompound").invoke(compound, t) + "|compound");
                        continue;
                    }
                    if (((Boolean)this.version.getMethodRef("hasKeyOfType").invoke(compound, t, 8)).booleanValue()) {
                        nbtList.add(t + "|" + this.version.getMethodRef("getString").invoke(compound, t));
                        continue;
                    }
                    nbtList.add(t + "|" + this.version.getMethodRef("get").invoke(compound, t));
                }
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return nbtList;
    }

    public ItemStack setNBT(ItemStack item, List<String> nbtList) {
        if (nbtList == null || nbtList.isEmpty()) {
            return item;
        }
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            Object compound = this.getNBTCompound(newItem);
            for (int i = 0; i < nbtList.size(); ++i) {
                String nbt = nbtList.get(i);
                String[] sep = nbtList.get(i).split("\\|");
                String id = sep[0];
                String type = sep[sep.length - 1];
                if (type.equals("boolean")) {
                    this.version.getMethodRef("setBoolean").invoke(compound, sep[0], Boolean.parseBoolean(sep[1]));
                    continue;
                }
                if (type.equals("double")) {
                    this.version.getMethodRef("setDouble").invoke(compound, sep[0], Double.parseDouble(sep[1]));
                    continue;
                }
                if (type.equals("int")) {
                    this.version.getMethodRef("setInt").invoke(compound, sep[0], Integer.parseInt(sep[1]));
                    continue;
                }
                if (type.equals("compound")) {
                    String finalNBT = nbt.replace(id + "|", "").replace("|compound", "");
                    Object compoundNew = this.version.getMethodRef("parse").invoke(null, finalNBT);
                    this.version.getMethodRef("set").invoke(compound, sep[0], compoundNew);
                    continue;
                }
                if (type.equals("list")) continue;
                this.version.getMethodRef("setString").invoke(compound, sep[0], nbt.replace(id + "|", ""));
            }
            this.version.getMethodRef("setTag").invoke(newItem, compound);
            return (ItemStack)this.version.getMethodRef("asBukkitCopy").invoke(null, newItem);
        }
        catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getAttributes(ItemStack item) {
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            if (((Boolean)this.version.getMethodRef("hasTag").invoke(newItem, null)).booleanValue()) {
                Object compound = this.version.getMethodRef("getTag").invoke(newItem, null);
                if (((Boolean)this.version.getMethodRef("hasKey").invoke(compound, "AttributeModifiers")).booleanValue()) {
                    ArrayList<String> attributeList = new ArrayList<String>();
                    Object attributes = this.version.getMethodRef("getList").invoke(compound, "AttributeModifiers", 10);
                    for (int i = 0; i < (Integer)this.version.getMethodRef("listSize").invoke(attributes, new Object[0]); ++i) {
                        Object compoundI = this.version.getMethodRef("listGet").invoke(attributes, i);
                        String attributeName = (String)this.version.getMethodRef("getString").invoke(compoundI, "AttributeName");
                        String name = (String)this.version.getMethodRef("getString").invoke(compoundI, "Name");
                        double amount = (Double)this.version.getMethodRef("getDouble").invoke(compoundI, "Amount");
                        int operation = (Integer)this.version.getMethodRef("getInt").invoke(compoundI, "Operation");
                        int uuidLeast = (Integer)this.version.getMethodRef("getInt").invoke(compoundI, "UUIDLeast");
                        int uuidMost = (Integer)this.version.getMethodRef("getInt").invoke(compoundI, "UUIDMost");
                        String all = attributeName + ";" + name + ";" + amount + ";" + operation + ";" + uuidLeast + ";" + uuidMost;
                        if (((Boolean)this.version.getMethodRef("hasKey").invoke(compoundI, "Slot")).booleanValue()) {
                            all = all + ";" + this.version.getMethodRef("getString").invoke(compoundI, "Slot");
                        }
                        attributeList.add(all);
                    }
                    return attributeList;
                }
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ItemStack setAttributes(ItemStack item, List<String> attributeList) {
        try {
            Object newItem = this.version.getMethodRef("asNMSCopy").invoke(null, item);
            Object compound = this.getNBTCompound(newItem);
            Object attributesList = this.version.getClassRef("NBTTagList").newInstance();
            for (int i = 0; i < attributeList.size(); ++i) {
                Object attributeCompound = this.version.getClassRef("NBTTagCompound").newInstance();
                String[] data = attributeList.get(i).split(";");
                String attributeName = data[0];
                String name = data[1];
                double amount = Double.valueOf(data[2]);
                int operation = Integer.valueOf(data[3]);
                int uuidLeast = Integer.valueOf(data[4]);
                int uuidMost = Integer.valueOf(data[5]);
                this.version.getMethodRef("setString").invoke(attributeCompound, "AttributeName", attributeName);
                this.version.getMethodRef("setString").invoke(attributeCompound, "Name", name);
                this.version.getMethodRef("setDouble").invoke(attributeCompound, "Amount", amount);
                this.version.getMethodRef("setInt").invoke(attributeCompound, "Operation", operation);
                this.version.getMethodRef("setInt").invoke(attributeCompound, "UUIDLeast", uuidLeast);
                this.version.getMethodRef("setInt").invoke(attributeCompound, "UUIDMost", uuidMost);
                if (data.length >= 7) {
                    String slot = data[6];
                    this.version.getMethodRef("setString").invoke(attributeCompound, "Slot", slot);
                }
                this.version.getMethodRef("listAdd").invoke(attributesList, attributeCompound);
            }
            this.version.getMethodRef("set").invoke(compound, "AttributeModifiers", attributesList);
            this.version.getMethodRef("setTag").invoke(newItem, compound);
            return (ItemStack)this.version.getMethodRef("asBukkitCopy").invoke(null, newItem);
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object getNBTCompound(Object newItem) {
        try {
            boolean hasTag = (Boolean)this.version.getMethodRef("hasTag").invoke(newItem, null);
            Object compound = hasTag ? this.version.getMethodRef("getTag").invoke(newItem, null) : this.version.getClassRef("NBTTagCompound").newInstance();
            return compound;
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean serverVersionGreaterEqualThan(ServerVersion version) {
        return this.serverVersion.ordinal() >= version.ordinal();
    }
}

