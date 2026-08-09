package me.krunsh.kgui.gui;

import java.util.Arrays;
import java.util.Collections;

import me.krunsh.kgui.render.ClickBinding;
import me.krunsh.kgui.render.RenderedSlot;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void providerRevisionAloneDoesNotResendTheSlot() {
        RenderedSlot oldSlot = new RenderedSlot(0, null, ClickBinding.forProviderItem(
            "test:items", "item:one", 1L, 1L, 0,
            Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList()));
        RenderedSlot newSlot = new RenderedSlot(0, null, ClickBinding.forProviderItem(
            "test:items", "item:one", 2L, 1L, 0,
            Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList()));
        assertTrue(SlotDiff.changedVisualSlots(
            new RenderedSlot[]{oldSlot}, new RenderedSlot[]{newSlot}).isEmpty());
        assertFalse(oldSlot.equals(newSlot));
    }
}
