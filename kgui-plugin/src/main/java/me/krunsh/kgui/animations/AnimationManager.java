package me.krunsh.kgui.animations;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.gui.KguiInventoryHolder;
import me.krunsh.kgui.gui.OpenGui;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Gestionnaire des animations avancées pour les items de menu
 * Supporte les animations de frame, glow et cycle de couleurs
 */
public class AnimationManager {

    private final Kgui plugin;
    private final Map<String, AnimationData> animations = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> runningAnimations = new HashMap<>();

    public AnimationManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge les animations depuis le dossier animations/
     */
    public void loadAnimations() {
        animations.clear();
        
        File animationsDir = new File(plugin.getDataFolder(), "animations");
        if (!animationsDir.exists()) {
            animationsDir.mkdirs();
            saveDefaultAnimations();
        }
        
        File[] files = animationsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "");
                
                AnimationData animation = new AnimationData(
                    id,
                    config.getString("type", "FRAMES"),
                    config.getInt("interval", 20),
                    config.getBoolean("loop", true)
                );
                
                // Charger les frames
                ConfigurationSection framesSection = config.getConfigurationSection("frames");
                if (framesSection != null) {
                    for (String frameKey : framesSection.getKeys(false)) {
                        ConfigurationSection frameConfig = framesSection.getConfigurationSection(frameKey);
                        if (frameConfig != null) {
                            FrameData frame = parseFrame(frameConfig);
                            animation.addFrame(frame);
                        }
                    }
                }
                
                animations.put(id, animation);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load animation: " + file.getName() + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + animations.size() + " animations");
    }

    /**
     * Parse une frame d'animation
     */
    private FrameData parseFrame(ConfigurationSection config) {
        FrameData frame = new FrameData();
        frame.setMaterial(config.getString("material"));
        frame.setData((short) config.getInt("data", 0));
        frame.setName(config.getString("name"));
        frame.setLore(config.getStringList("lore"));
        frame.setGlow(config.getBoolean("glow", false));
        frame.setDuration(config.getInt("duration", 20));
        return frame;
    }

    /**
     * Sauvegarde les animations par défaut
     */
    private void saveDefaultAnimations() {
        String content = 
            "# Animation de clignotement\n" +
            "type: FRAMES\n" +
            "interval: 10\n" +
            "loop: true\n" +
            "\n" +
            "frames:\n" +
            "  frame_1:\n" +
            "    material: WOOL\n" +
            "    data: 5\n" +
            "    name: '&a&lActif'\n" +
            "    glow: true\n" +
            "    duration: 10\n" +
            "  frame_2:\n" +
            "    material: WOOL\n" +
            "    data: 13\n" +
            "    name: '&2&lActif'\n" +
            "    glow: false\n" +
            "    duration: 10\n";
        
        try {
            File exampleFile = new File(plugin.getDataFolder(), "animations/blink_green.yml");
            Files.write(exampleFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create example animation: " + e.getMessage());
        }
    }

    /**
     * Démarre une animation pour un joueur
     */
    public void startAnimation(Player player, OpenGui gui, String animationId) {
        AnimationData animation = animations.get(animationId);
        if (animation == null) return;
        
        // Vérifier que l'animation a des frames
        if (animation.getFrames().isEmpty()) return;
        
        UUID uuid = player.getUniqueId();
        
        // Créer la tâche d'animation
        BukkitTask task = new BukkitRunnable() {
            private int frameIndex = 0;
            private int ticksInFrame = 0;
            
            @Override
            public void run() {
                // Vérifier que le joueur est toujours connecté
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Vérifier que l'inventaire est toujours ouvert
                Inventory topInventory = player.getOpenInventory().getTopInventory();
                if (!KguiInventoryHolder.isKguiInventory(topInventory)) {
                    cancel();
                    return;
                }
                
                // Obtenir la frame actuelle
                FrameData frame = animation.getFrames().get(frameIndex);
                
                // Créer l'item de la frame
                ItemStack item = createFrameItem(frame, player);
                if (item != null) {
                    // Appliquer aux slots de l'item animé
                    // Note: les slots sont stockés dans gui.getAnimatedSlots(animationId)
                    // Pour l'instant, on utilise les slots configurés
                }
                
                // Avancer dans la frame
                ticksInFrame += animation.getInterval();
                
                // Passer à la frame suivante si nécessaire
                if (ticksInFrame >= frame.getDuration()) {
                    ticksInFrame = 0;
                    frameIndex++;
                    
                    if (frameIndex >= animation.getFrames().size()) {
                        if (animation.isLoop()) {
                            frameIndex = 0;
                        } else {
                            cancel();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, animation.getInterval());
        
        // Stocker la tâche
        runningAnimations.computeIfAbsent(uuid, k -> new ArrayList<>()).add(task);
    }

    /**
     * Démarre les animations pour des slots spécifiques
     */
    public void startAnimationForSlots(Player player, String animationId, List<Integer> slots) {
        AnimationData animation = animations.get(animationId);
        if (animation == null || animation.getFrames().isEmpty()) return;
        
        UUID uuid = player.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            private int frameIndex = 0;
            private int ticksInFrame = 0;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                Inventory topInventory = player.getOpenInventory().getTopInventory();
                if (!KguiInventoryHolder.isKguiInventory(topInventory)) {
                    cancel();
                    return;
                }
                
                FrameData frame = animation.getFrames().get(frameIndex);
                ItemStack item = createFrameItem(frame, player);
                
                if (item != null) {
                    for (int slot : slots) {
                        if (slot >= 0 && slot < topInventory.getSize()) {
                            topInventory.setItem(slot, item);
                        }
                    }
                }
                
                ticksInFrame += animation.getInterval();
                
                if (ticksInFrame >= frame.getDuration()) {
                    ticksInFrame = 0;
                    frameIndex++;
                    
                    if (frameIndex >= animation.getFrames().size()) {
                        if (animation.isLoop()) {
                            frameIndex = 0;
                        } else {
                            cancel();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, animation.getInterval());
        
        runningAnimations.computeIfAbsent(uuid, k -> new ArrayList<>()).add(task);
    }

    /**
     * Crée un ItemStack pour une frame d'animation
     */
    @SuppressWarnings("deprecation")
    private ItemStack createFrameItem(FrameData frame, Player player) {
        if (frame.getMaterial() == null) return null;
        
        try {
            Material material = Material.valueOf(frame.getMaterial().toUpperCase());
            ItemStack item = new ItemStack(material, 1, frame.getData());
            
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (frame.getName() != null) {
                    String name = plugin.getHookManager().getPlaceholderAPIHook()
                                       .setPlaceholders(player, frame.getName());
                    name = ChatColor.translateAlternateColorCodes('&', name);
                    meta.setDisplayName(name);
                }
                
                if (frame.getLore() != null && !frame.getLore().isEmpty()) {
                    List<String> lore = new ArrayList<>();
                    for (String line : frame.getLore()) {
                        line = plugin.getHookManager().getPlaceholderAPIHook()
                                    .setPlaceholders(player, line);
                        line = ChatColor.translateAlternateColorCodes('&', line);
                        lore.add(line);
                    }
                    meta.setLore(lore);
                }
                
                if (frame.isGlow()) {
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                
                item.setItemMeta(meta);
            }
            
            // Ajouter le tag NBT GUI
            NBTItem nbtItem = new NBTItem(item);
            nbtItem.setBoolean(plugin.getConfigManager().getNbtTag(), true);
            return nbtItem.getItem();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material in animation frame: " + frame.getMaterial());
            return null;
        }
    }

    /**
     * Arrête l'animation pour un joueur
     */
    public void stopAnimation(Player player) {
        List<BukkitTask> tasks = runningAnimations.remove(player.getUniqueId());
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                if (task != null) {
                    task.cancel();
                }
            }
        }
    }

    /**
     * Arrête toutes les animations
     */
    public void stopAllAnimations() {
        for (List<BukkitTask> tasks : runningAnimations.values()) {
            for (BukkitTask task : tasks) {
                if (task != null) {
                    task.cancel();
                }
            }
        }
        runningAnimations.clear();
    }

    /**
     * Vérifie si une animation existe
     */
    public boolean hasAnimation(String id) {
        return animations.containsKey(id);
    }

    /**
     * Récupère une animation
     */
    public AnimationData getAnimation(String id) {
        return animations.get(id);
    }

    /**
     * Arrête toutes les animations (alias)
     */
    public void stopAll() {
        stopAllAnimations();
    }

    /**
     * Recharge les animations
     */
    public void reload() {
        stopAllAnimations();
        loadAnimations();
    }

    /**
     * Classe de données pour les animations
     */
    public static class AnimationData {
        private final String id;
        private final String type;
        private final int interval;
        private final boolean loop;
        private final List<FrameData> frames = new ArrayList<>();

        public AnimationData(String id, String type, int interval, boolean loop) {
            this.id = id;
            this.type = type;
            this.interval = interval;
            this.loop = loop;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public int getInterval() { return interval; }
        public boolean isLoop() { return loop; }
        public List<FrameData> getFrames() { return frames; }
        public void addFrame(FrameData frame) { frames.add(frame); }
    }

    /**
     * Données d'une frame d'animation
     */
    public static class FrameData {
        private String material;
        private short data = 0;
        private String name;
        private List<String> lore;
        private boolean glow = false;
        private int duration = 20;
        
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public short getData() { return data; }
        public void setData(short data) { this.data = data; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }
        public boolean isGlow() { return glow; }
        public void setGlow(boolean glow) { this.glow = glow; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
}
