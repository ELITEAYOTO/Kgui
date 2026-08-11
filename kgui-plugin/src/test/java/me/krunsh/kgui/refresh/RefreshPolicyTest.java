package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RefreshPolicyTest {
    @Test
    public void policiesHaveDistinctEventAndIntervalSemantics() {
        assertFalse(RefreshPolicy.MANUAL.acceptsEvents());
        assertFalse(RefreshPolicy.MANUAL.hasInterval());
        assertTrue(RefreshPolicy.EVENT.acceptsEvents());
        assertFalse(RefreshPolicy.EVENT.hasInterval());
        assertFalse(RefreshPolicy.INTERVAL.acceptsEvents());
        assertTrue(RefreshPolicy.INTERVAL.hasInterval());
        assertTrue(RefreshPolicy.HYBRID.acceptsEvents());
        assertTrue(RefreshPolicy.HYBRID.hasInterval());
    }

    @Test
    public void legacyIntervalMapsToPollingOnly() {
        assertEquals(RefreshPolicy.INTERVAL, RefreshPolicy.parse(null, 20));
        assertEquals(RefreshPolicy.EVENT, RefreshPolicy.parse(null, 0));
    }
}
