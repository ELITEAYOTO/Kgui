/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.item;

public class KitItemTrimData {
    private String pattern;
    private String material;

    public KitItemTrimData(String pattern, String material) {
        this.pattern = pattern;
        this.material = material;
    }

    public String getPattern() {
        return this.pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getMaterial() {
        return this.material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public KitItemTrimData clone() {
        return new KitItemTrimData(this.pattern, this.material);
    }
}

