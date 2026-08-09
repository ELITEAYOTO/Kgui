package me.krunsh.kgui.pagination;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaginationStateTest {

    @Test
    public void computesPageCountAtBoundaries() {
        assertEquals(1, PaginationState.maxPage(0, 9));
        assertEquals(1, PaginationState.maxPage(9, 9));
        assertEquals(2, PaginationState.maxPage(10, 9));
        assertEquals(3, PaginationState.maxPage(27, 9));
    }

    @Test
    public void keepsExistingPageWhenStillAvailable() {
        assertEquals(3, PaginationState.clampPage(3, 5));
    }

    @Test
    public void clampsPageWhenContentShrinks() {
        assertEquals(2, PaginationState.clampPage(5, 2));
        assertEquals(1, PaginationState.clampPage(0, 4));
    }
}
