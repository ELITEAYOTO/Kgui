package me.krunsh.kgui.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forme immutable des slots de contenu.
 *
 * Les lignes sont déduites explicitement des rangées Bukkit (slot / 9). Le
 * stride du ROW_SCROLL est la largeur de la première rangée visible : chaque
 * déplacement retire donc exactement la ligne du haut de la fenêtre courante.
 * Les rangées suivantes peuvent être irrégulières et le calcul de fin utilise
 * la capacité réelle afin qu'aucun item ne devienne inaccessible.
 */
public final class ViewportLayout {
    private static final ViewportLayout EMPTY = new ViewportLayout(Collections.<Integer>emptyList());

    private final List<Integer> slots;
    private final List<List<Integer>> rows;
    private final int rowStride;

    public ViewportLayout(List<Integer> configuredSlots) {
        if (configuredSlots == null || configuredSlots.isEmpty()) {
            this.slots = Collections.emptyList();
            this.rows = Collections.emptyList();
            this.rowStride = 1;
            return;
        }

        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer slot : configuredSlots) {
            if (slot == null || slot < 0 || slot > 53) {
                throw new IllegalArgumentException("content slot must be between 0 and 53");
            }
            if (!unique.add(slot)) throw new IllegalArgumentException("duplicate content slot: " + slot);
        }

        List<Integer> ordered = new ArrayList<>(unique);
        Collections.sort(ordered);
        this.slots = Collections.unmodifiableList(ordered);

        Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
        for (Integer slot : ordered) {
            int row = slot / 9;
            List<Integer> rowSlots = grouped.get(row);
            if (rowSlots == null) {
                rowSlots = new ArrayList<>();
                grouped.put(row, rowSlots);
            }
            rowSlots.add(slot);
        }

        List<List<Integer>> immutableRows = new ArrayList<>();
        for (List<Integer> row : grouped.values()) {
            immutableRows.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.rows = Collections.unmodifiableList(immutableRows);
        this.rowStride = immutableRows.get(0).size();
    }

    public static ViewportLayout empty() {
        return EMPTY;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public List<List<Integer>> getRows() {
        return rows;
    }

    public int getCapacity() {
        return slots.size();
    }

    public int getVisibleRows() {
        return rows.size();
    }

    public int getRowStride() {
        return rowStride;
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }
}
