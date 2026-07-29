/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.item;

import java.util.ArrayList;
import java.util.List;

public class KitItemPotionData {
    private boolean upgraded;
    private boolean extended;
    private String potionType;
    private int potionColor;
    private List<String> potionEffects;

    public KitItemPotionData(boolean upgraded, boolean extended, String potionType, int potionColor, List<String> potionEffects) {
        this.upgraded = upgraded;
        this.extended = extended;
        this.potionType = potionType;
        this.potionColor = potionColor;
        this.potionEffects = potionEffects;
    }

    public boolean isUpgraded() {
        return this.upgraded;
    }

    public void setUpgraded(boolean upgraded) {
        this.upgraded = upgraded;
    }

    public boolean isExtended() {
        return this.extended;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public String getPotionType() {
        return this.potionType;
    }

    public void setPotionType(String potionType) {
        this.potionType = potionType;
    }

    public int getPotionColor() {
        return this.potionColor;
    }

    public void setPotionColor(int potionColor) {
        this.potionColor = potionColor;
    }

    public List<String> getPotionEffects() {
        return this.potionEffects;
    }

    public void setPotionEffects(List<String> potionEffects) {
        this.potionEffects = potionEffects;
    }

    public KitItemPotionData clone() {
        return new KitItemPotionData(this.upgraded, this.extended, this.potionType, this.potionColor, new ArrayList<String>(this.potionEffects));
    }
}

