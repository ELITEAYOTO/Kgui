package me.krunsh.kgui.render;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** Snapshot immutable de l'apparence et de l'autorite d'un slot. */
public final class RenderedSlot {
    private final int slot;
    private final ItemStack item;
    private final ClickBinding binding;

    public RenderedSlot(int slot, ItemStack item, ClickBinding binding) {
        this.slot = slot;
        this.item = item == null ? null : item.clone();
        this.binding = binding;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item == null ? null : item.clone();
    }

    public ClickBinding getBinding() {
        return binding;
    }

    public boolean matches(ItemStack candidate) {
        if (item == null || item.getType() == Material.AIR) {
            return candidate == null || candidate.getType() == Material.AIR;
        }
        return candidate != null && candidate.getAmount() == item.getAmount() && item.isSimilar(candidate);
    }

    /** Compare uniquement ce que le client voit, sans l'autorité de clic serveur. */
    public boolean visuallyEquals(RenderedSlot other) {
        return other != null && slot == other.slot && Objects.equals(item, other.item);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RenderedSlot)) return false;
        RenderedSlot that = (RenderedSlot) other;
        return slot == that.slot && Objects.equals(item, that.item) && Objects.equals(binding, that.binding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, item, binding);
    }
}
