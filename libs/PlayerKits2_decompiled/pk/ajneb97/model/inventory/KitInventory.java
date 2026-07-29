/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.inventory;

import java.util.ArrayList;
import java.util.List;
import pk.ajneb97.model.inventory.ItemKitInventory;

public class KitInventory {
    private String name;
    private int slots;
    private String title;
    private List<ItemKitInventory> items;

    public KitInventory(String name, int slots, String title, List<ItemKitInventory> items) {
        this.name = name;
        this.slots = slots;
        this.title = title;
        this.items = items;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSlots() {
        return this.slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ItemKitInventory> getItems() {
        return this.items;
    }

    public void setItems(List<ItemKitInventory> items) {
        this.items = items;
    }

    public int addKitItemOnFirstEmptySlot(String kitName) {
        ArrayList<Integer> occupiedSlots = new ArrayList<Integer>();
        for (ItemKitInventory item : this.items) {
            for (int slot : item.getSlots()) {
                occupiedSlots.add(slot);
            }
        }
        for (int i = 0; i < this.slots; ++i) {
            if (occupiedSlots.contains(i)) continue;
            this.items.add(new ItemKitInventory(i + "", null, null, null, "kit: " + kitName));
            return i;
        }
        return -1;
    }

    public void addKitItemOnSlot(String kitName, int slot) {
        ArrayList<Integer> newSlotList = new ArrayList<Integer>();
        newSlotList.add(slot);
        this.items.add(new ItemKitInventory(slot + "", null, null, null, "kit: " + kitName));
    }
}

