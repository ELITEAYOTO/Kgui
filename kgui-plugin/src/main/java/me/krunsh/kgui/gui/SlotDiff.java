package me.krunsh.kgui.gui;

import java.util.ArrayList;
import java.util.List;
import me.krunsh.kgui.render.RenderedSlot;

/** Calcule les slots reellement modifies sans dependre de Bukkit. */
public final class SlotDiff {

    private SlotDiff() {
    }

    public static <T> List<Integer> changedSlots(T[] current, T[] next) {
        List<Integer> changed = new ArrayList<>();
        if (current == null || next == null || current.length != next.length) {
            return changed;
        }

        for (int slot = 0; slot < current.length; slot++) {
            T oldValue = current[slot];
            T newValue = next[slot];
            if (oldValue == null ? newValue != null : !oldValue.equals(newValue)) {
                changed.add(slot);
            }
        }
        return changed;
    }

    /** Diff visuel : un changement d'autorité serveur seul ne renvoie aucun packet de slot. */
    public static List<Integer> changedVisualSlots(RenderedSlot[] current, RenderedSlot[] next) {
        List<Integer> changed = new ArrayList<>();
        if (current == null || next == null || current.length != next.length) return changed;
        for (int slot = 0; slot < current.length; slot++) {
            RenderedSlot oldValue = current[slot];
            RenderedSlot newValue = next[slot];
            if (oldValue == null ? newValue != null : !oldValue.visuallyEquals(newValue)) changed.add(slot);
        }
        return changed;
    }
}
