/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model;

import pk.ajneb97.model.item.KitItem;

public class KitAction {
    private String action;
    private KitItem displayItem;
    private boolean executeBeforeItems;
    private boolean countAsItem;

    public KitAction(String action, KitItem displayItem, boolean executeBeforeItems, boolean countAsItem) {
        this.action = action;
        this.displayItem = displayItem;
        this.executeBeforeItems = executeBeforeItems;
        this.countAsItem = countAsItem;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public KitItem getDisplayItem() {
        return this.displayItem;
    }

    public void setDisplayItem(KitItem displayItem) {
        this.displayItem = displayItem;
    }

    public boolean isExecuteBeforeItems() {
        return this.executeBeforeItems;
    }

    public void setExecuteBeforeItems(boolean executeBeforeItems) {
        this.executeBeforeItems = executeBeforeItems;
    }

    public boolean isCountAsItem() {
        return this.countAsItem;
    }

    public void setCountAsItem(boolean countAsItem) {
        this.countAsItem = countAsItem;
    }

    public KitAction clone() {
        KitItem item = this.displayItem != null ? this.displayItem.clone() : null;
        return new KitAction(this.action, item, this.executeBeforeItems, this.countAsItem);
    }
}

