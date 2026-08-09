package me.krunsh.kgui.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente un item dynamique pour les menus paginés.
 * 
 * Utilisé par les DynamicContentProvider pour retourner du contenu
 * qui sera affiché dans les slots de pagination.
 * 
 * Exemple:
 * <pre>
 * DynamicItem item = new DynamicItem.Builder()
 *     .material("DIAMOND_SWORD")
 *     .name("&6Mon Item")
 *     .lore("&7Description", "&eLigne 2")
 *     .glow(true)
 *     .data("item_id", "123")
 *     .clickAction("[player] f teleport 123")
 *     .build();
 * </pre>
 */
public class DynamicItem {
    
    private final String material;
    private final short data;
    private final String name;
    private final List<String> lore;
    private final boolean glow;
    private final String skullOwner;
    private final String headDatabaseId;
    private final Map<String, String> customData;
    private final List<String> clickActions;
    private final List<String> leftClickActions;
    private final List<String> rightClickActions;
    private final List<String> shiftClickActions;
    
    private DynamicItem(Builder builder) {
        this.material = builder.material;
        this.data = builder.data;
        this.name = builder.name;
        this.lore = new ArrayList<>(builder.lore);
        this.glow = builder.glow;
        this.skullOwner = builder.skullOwner;
        this.headDatabaseId = builder.headDatabaseId;
        this.customData = new HashMap<>(builder.customData);
        this.clickActions = new ArrayList<>(builder.clickActions);
        this.leftClickActions = new ArrayList<>(builder.leftClickActions);
        this.rightClickActions = new ArrayList<>(builder.rightClickActions);
        this.shiftClickActions = new ArrayList<>(builder.shiftClickActions);
    }
    
    // === Getters ===
    
    public String getMaterial() { return material; }
    public short getData() { return data; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public boolean isGlow() { return glow; }
    public String getSkullOwner() { return skullOwner; }
    public String getHeadDatabaseId() { return headDatabaseId; }
    public Map<String, String> getCustomData() { return customData; }
    public List<String> getClickActions() { return clickActions; }
    public List<String> getLeftClickActions() { return leftClickActions; }
    public List<String> getRightClickActions() { return rightClickActions; }
    public List<String> getShiftClickActions() { return shiftClickActions; }
    
    /**
     * Récupère une donnée custom par sa clé.
     */
    public String getData(String key) {
        return customData.get(key);
    }
    
    /**
     * Récupère une donnée custom avec valeur par défaut.
     */
    public String getData(String key, String defaultValue) {
        return customData.getOrDefault(key, defaultValue);
    }
    
    // === Builder ===
    
    public static class Builder {
        private String material = "STONE";
        private short data = 0;
        private String name = null;
        private List<String> lore = new ArrayList<>();
        private boolean glow = false;
        private String skullOwner = null;
        private String headDatabaseId = null;
        private Map<String, String> customData = new HashMap<>();
        private List<String> clickActions = new ArrayList<>();
        private List<String> leftClickActions = new ArrayList<>();
        private List<String> rightClickActions = new ArrayList<>();
        private List<String> shiftClickActions = new ArrayList<>();
        
        public Builder material(String material) {
            this.material = material;
            return this;
        }
        
        public Builder data(int data) {
            this.data = (short) data;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder lore(String... lines) {
            for (String line : lines) {
                this.lore.add(line);
            }
            return this;
        }
        
        public Builder lore(List<String> lines) {
            this.lore.addAll(lines);
            return this;
        }
        
        public Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }
        
        public Builder skullOwner(String owner) {
            this.skullOwner = owner;
            return this;
        }
        
        public Builder headDatabase(String hdbId) {
            this.headDatabaseId = hdbId;
            return this;
        }
        
        /**
         * Ajoute une donnée custom (récupérable via getData()).
         * Utile pour stocker l'ID de l'objet, etc.
         */
        public Builder data(String key, String value) {
            this.customData.put(key, value);
            return this;
        }
        
        public Builder clickAction(String action) {
            this.clickActions.add(action);
            return this;
        }
        
        public Builder clickActions(List<String> actions) {
            this.clickActions.addAll(actions);
            return this;
        }
        
        public Builder leftClickAction(String action) {
            this.leftClickActions.add(action);
            return this;
        }
        
        public Builder rightClickAction(String action) {
            this.rightClickActions.add(action);
            return this;
        }
        
        public Builder shiftClickAction(String action) {
            this.shiftClickActions.add(action);
            return this;
        }
        
        public DynamicItem build() {
            return new DynamicItem(this);
        }
    }
}
