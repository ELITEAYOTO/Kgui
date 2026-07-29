/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.internal;

public class KitPosition {
    private int slot;
    private String inventoryName;

    public KitPosition(int slot, String inventoryName) {
        this.slot = slot;
        this.inventoryName = inventoryName;
    }

    public int getSlot() {
        return this.slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getInventoryName() {
        return this.inventoryName;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }
}

