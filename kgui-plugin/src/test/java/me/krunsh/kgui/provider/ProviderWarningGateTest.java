package me.krunsh.kgui.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProviderWarningGateTest {
    @Test
    public void throttlesEachProviderIndependently() {
        ProviderWarningGate gate = new ProviderWarningGate(100L);
        assertTrue(gate.tryAcquire("one:items", 1_000L));
        assertFalse(gate.tryAcquire("one:items", 1_050L));
        assertTrue(gate.tryAcquire("two:items", 1_050L));
        assertTrue(gate.tryAcquire("one:items", 1_100L));
        gate.remove("one:items");
        assertTrue(gate.tryAcquire("one:items", 1_101L));
    }
}
