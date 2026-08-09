package me.krunsh.kgui.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class ViewportStateTest {
    @Test
    public void pageModeUsesCompleteSlicesAndClampsAfterShrink() {
        ViewportState state = new ViewportState(NavigationMode.PAGE,
            new ViewportLayout(regularSlots()), 1);
        state.reconcile(50, 0);
        assertEquals(3, state.getMaxPage());
        assertEquals(0, state.getContentOffset());
        assertTrue(state.setPage(3));
        assertEquals(42, state.getContentOffset());
        assertTrue(state.reconcile(8, 0));
        assertEquals(1, state.getPage());
        assertEquals(1, state.getMaxPage());
    }

    @Test
    public void regularRowScrollAdvancesOneVisualRowAndReachesTail() {
        ViewportState state = new ViewportState(NavigationMode.ROW_SCROLL,
            new ViewportLayout(regularSlots()), 1);
        state.reconcile(40, 0);
        assertEquals(7, state.getLayout().getRowStride());
        assertEquals(4, state.getMaxPage());
        assertTrue(state.next());
        assertEquals(7, state.getContentOffset());
        assertTrue(state.setPage(4));
        assertEquals(21, state.getContentOffset());
        assertEquals(34, state.slotForSliceIndex(20));
    }

    @Test
    public void irregularRowsUseExplicitCapacityWithoutLosingLastItem() {
        ViewportLayout layout = new ViewportLayout(Arrays.asList(10, 11, 12, 19, 20));
        ViewportState state = new ViewportState(NavigationMode.ROW_SCROLL, layout, 1);
        state.reconcile(10, 0);
        assertEquals(Arrays.asList(10, 11, 12), layout.getRows().get(0));
        assertEquals(Arrays.asList(19, 20), layout.getRows().get(1));
        assertEquals(3, layout.getRowStride());
        assertEquals(3, state.getMaxPage());
        assertTrue(state.setPage(3));
        assertEquals(6, state.getContentOffset());
        assertEquals(20, state.slotForSliceIndex(4));
    }

    @Test
    public void irregularRowsAdvanceByTheActualTopRow() {
        ViewportLayout layout = new ViewportLayout(Arrays.asList(10, 11, 19, 20, 21));
        ViewportState state = new ViewportState(NavigationMode.ROW_SCROLL, layout, 1);
        state.reconcile(10, 0);
        assertEquals(2, layout.getRowStride());
        assertEquals(4, state.getMaxPage());
        assertTrue(state.next());
        assertEquals(2, state.getContentOffset());
        assertTrue(state.setPage(4));
        assertEquals(6, state.getContentOffset());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateContentSlotIsRejected() {
        new ViewportLayout(Arrays.asList(10, 11, 10));
    }

    @Test
    public void placeholdersComeFromTheSingleViewport() {
        ViewportState state = new ViewportState(NavigationMode.ROW_SCROLL,
            new ViewportLayout(Arrays.asList(10, 11, 19, 20)), 1);
        state.reconcile(12, 0);
        assertTrue(state.next());
        assertEquals("2/5 offset=1/4", state.replacePlaceholders(
            "%page%/%max_page% offset=%scroll_offset%/%scroll_max%"));
        assertFalse(state.setPage(9));
    }

    private static java.util.List<Integer> regularSlots() {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int value = 10; value <= 16; value++) result.add(value);
        for (int value = 19; value <= 25; value++) result.add(value);
        for (int value = 28; value <= 34; value++) result.add(value);
        return result;
    }
}
