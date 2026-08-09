package me.krunsh.kgui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Représente un item dans un menu
 */
public class MenuItem {

    private final String key;
    
    // Slots où l'item apparaît
    private final List<Integer> slots = new ArrayList<>();
    
    // Item depuis registry
    private String itemId;
    
    // Ou définition inline
    private String inlineMaterial;
    private int inlineData = 0;
    private String inlineName;
    private List<String> inlineLore;
    private boolean inlineGlow = false;
    private String inlineSkull;
    private String inlineHdb;
    private String inlineCit;
    
    // Amount (peut être un placeholder)
    private String amount = "1";
    
    // Priorité (plus haut = affiché en priorité)
    private int priority = 0;
    
    // Requirements
    private List<Map<String, Object>> viewRequirements = new ArrayList<>();
    private List<Map<String, Object>> clickRequirements = new ArrayList<>();
    
    // Actions
    private List<String> clickActions = new ArrayList<>();
    private List<String> leftClickActions = new ArrayList<>();
    private List<String> rightClickActions = new ArrayList<>();
    private List<String> shiftClickActions = new ArrayList<>();
    private List<String> middleClickActions = new ArrayList<>();
    private List<String> denyActions = new ArrayList<>();
    
    // Cooldown
    private int cooldown = 0;
    
    // Update
    private boolean updateOnRefresh = true;
    
    // Animation
    private String animationId;

    public MenuItem(String key) {
        this.key = key;
    }

    /**
     * Clone cet item
     */
    public MenuItem clone() {
        MenuItem copy = new MenuItem(key);
        copy.slots.addAll(this.slots);
        copy.itemId = this.itemId;
        copy.inlineMaterial = this.inlineMaterial;
        copy.inlineData = this.inlineData;
        copy.inlineName = this.inlineName;
        copy.inlineLore = this.inlineLore != null ? new ArrayList<>(this.inlineLore) : null;
        copy.inlineGlow = this.inlineGlow;
        copy.inlineSkull = this.inlineSkull;
        copy.inlineHdb = this.inlineHdb;
        copy.inlineCit = this.inlineCit;
        copy.amount = this.amount;
        copy.priority = this.priority;
        copy.viewRequirements = new ArrayList<>(this.viewRequirements);
        copy.clickRequirements = new ArrayList<>(this.clickRequirements);
        copy.clickActions = new ArrayList<>(this.clickActions);
        copy.leftClickActions = new ArrayList<>(this.leftClickActions);
        copy.rightClickActions = new ArrayList<>(this.rightClickActions);
        copy.shiftClickActions = new ArrayList<>(this.shiftClickActions);
        copy.middleClickActions = new ArrayList<>(this.middleClickActions);
        copy.denyActions = new ArrayList<>(this.denyActions);
        copy.cooldown = this.cooldown;
        copy.updateOnRefresh = this.updateOnRefresh;
        copy.animationId = this.animationId;
        return copy;
    }

    /**
     * Vérifie si c'est un item inline ou depuis le registry
     */
    public boolean isFromRegistry() {
        return itemId != null && !itemId.isEmpty();
    }

    /**
     * Ajoute un slot
     */
    public void addSlot(int slot) {
        if (!slots.contains(slot)) {
            slots.add(slot);
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public String getKey() {
        return key;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getInlineMaterial() {
        return inlineMaterial;
    }

    public void setInlineMaterial(String inlineMaterial) {
        this.inlineMaterial = inlineMaterial;
    }

    public int getInlineData() {
        return inlineData;
    }

    public void setInlineData(int inlineData) {
        this.inlineData = inlineData;
    }

    public String getInlineName() {
        return inlineName;
    }

    public void setInlineName(String inlineName) {
        this.inlineName = inlineName;
    }

    public List<String> getInlineLore() {
        return inlineLore;
    }

    public void setInlineLore(List<String> inlineLore) {
        this.inlineLore = inlineLore;
    }

    public boolean isInlineGlow() {
        return inlineGlow;
    }

    public void setInlineGlow(boolean inlineGlow) {
        this.inlineGlow = inlineGlow;
    }

    public String getInlineSkull() {
        return inlineSkull;
    }

    public void setInlineSkull(String inlineSkull) {
        this.inlineSkull = inlineSkull;
    }

    public String getInlineHdb() {
        return inlineHdb;
    }

    public void setInlineHdb(String inlineHdb) {
        this.inlineHdb = inlineHdb;
    }

    public String getInlineCit() {
        return inlineCit;
    }

    public void setInlineCit(String inlineCit) {
        this.inlineCit = inlineCit;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<Map<String, Object>> getViewRequirements() {
        return viewRequirements;
    }

    public void setViewRequirements(List<Map<String, Object>> viewRequirements) {
        this.viewRequirements = viewRequirements;
    }

    public List<Map<String, Object>> getClickRequirements() {
        return clickRequirements;
    }

    public void setClickRequirements(List<Map<String, Object>> clickRequirements) {
        this.clickRequirements = clickRequirements;
    }

    public List<String> getClickActions() {
        return clickActions;
    }

    public void setClickActions(List<String> clickActions) {
        this.clickActions = clickActions;
    }

    public List<String> getLeftClickActions() {
        return leftClickActions;
    }

    public void setLeftClickActions(List<String> leftClickActions) {
        this.leftClickActions = leftClickActions;
    }

    public List<String> getRightClickActions() {
        return rightClickActions;
    }

    public void setRightClickActions(List<String> rightClickActions) {
        this.rightClickActions = rightClickActions;
    }

    public List<String> getShiftClickActions() {
        return shiftClickActions;
    }

    public void setShiftClickActions(List<String> shiftClickActions) {
        this.shiftClickActions = shiftClickActions;
    }

    public List<String> getMiddleClickActions() {
        return middleClickActions;
    }

    public void setMiddleClickActions(List<String> middleClickActions) {
        this.middleClickActions = middleClickActions;
    }

    public List<String> getDenyActions() {
        return denyActions;
    }

    public void setDenyActions(List<String> denyActions) {
        this.denyActions = denyActions;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isUpdateOnRefresh() {
        return updateOnRefresh;
    }

    public void setUpdateOnRefresh(boolean updateOnRefresh) {
        this.updateOnRefresh = updateOnRefresh;
    }

    public String getAnimationId() {
        return animationId;
    }

    public void setAnimationId(String animationId) {
        this.animationId = animationId;
    }
}
