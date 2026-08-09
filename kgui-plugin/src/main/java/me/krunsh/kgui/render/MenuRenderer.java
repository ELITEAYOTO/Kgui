package me.krunsh.kgui.render;

import org.bukkit.inventory.ItemStack;

/** Assemble un RenderedSlot[] en appliquant les priorites sans creer d'Inventory Bukkit. */
public final class MenuRenderer {
    private final RenderedSlot[] slots;
    private final int[] priorities;

    public MenuRenderer(int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
        this.slots = new RenderedSlot[size];
        this.priorities = new int[size];
        java.util.Arrays.fill(priorities, Integer.MIN_VALUE);
    }

    public boolean place(int slot, ItemStack item, ClickBinding binding, int priority) {
        if (slot < 0 || slot >= slots.length || item == null || priority < priorities[slot]) return false;
        slots[slot] = new RenderedSlot(slot, item, binding);
        priorities[slot] = priority;
        return true;
    }

    public RenderFrame finish(long revision) {
        return new RenderFrame(revision, slots);
    }
}
