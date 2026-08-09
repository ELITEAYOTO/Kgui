package me.krunsh.kgui.gui;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SlotDiffTest {

    @Test
    public void returnsOnlyChangedSlots() {
        String[] current = {"same", "old", null, "removed"};
        String[] next = {"same", "new", "added", null};
        assertEquals(Arrays.asList(1, 2, 3), SlotDiff.changedSlots(current, next));
    }

    @Test
    public void returnsNothingForIdenticalSnapshots() {
        Integer[] current = {1, null, 3};
        Integer[] next = {1, null, 3};
        assertEquals(Collections.emptyList(), SlotDiff.changedSlots(current, next));
    }
}
