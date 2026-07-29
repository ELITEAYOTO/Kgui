/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.inventory;

import java.util.ArrayList;
import java.util.List;
import pk.ajneb97.model.item.KitItem;

public class ItemKitInventory {
    private List<Integer> slots = new ArrayList<Integer>();
    private String slotsString;
    private KitItem item;
    private String openInventory;
    private List<String> clickActions;
    private String type;

    public ItemKitInventory(String slotsString, KitItem item, String openInventory, List<String> clickActions, String type) {
        this.slotsString = slotsString;
        String[] slotsSep = slotsString.split(";");
        for (int i = 0; i < slotsSep.length; ++i) {
            if (slotsSep[i].contains("-")) {
                String[] newSep = slotsSep[i].split("-");
                int sMin = Integer.valueOf(newSep[0]);
                int sMax = Integer.valueOf(newSep[1]);
                for (int c = sMin; c <= sMax; ++c) {
                    this.slots.add(c);
                }
                continue;
            }
            this.slots.add(Integer.valueOf(slotsSep[i]));
        }
        this.item = item;
        this.openInventory = openInventory;
        this.clickActions = clickActions;
        this.type = type;
    }

    public List<Integer> getSlots() {
        return this.slots;
    }

    public void setSlots(List<Integer> slots) {
        this.slots = slots;
    }

    public KitItem getItem() {
        return this.item;
    }

    public void setItem(KitItem item) {
        this.item = item;
    }

    public String getOpenInventory() {
        return this.openInventory;
    }

    public void setOpenInventory(String openInventory) {
        this.openInventory = openInventory;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSlotsString() {
        return this.slotsString;
    }

    public void setSlotsString(String slotsString) {
        this.slotsString = slotsString;
    }

    public List<String> getClickActions() {
        return this.clickActions;
    }

    public void setClickActions(List<String> clickActions) {
        this.clickActions = clickActions;
    }
}

