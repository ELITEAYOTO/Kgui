package me.krunsh.kgui.gui;

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RefreshRequestGateTest {

    @Test
    public void coalescesRequestsUntilCompletion() {
        RefreshRequestGate gate = new RefreshRequestGate();
        UUID player = UUID.randomUUID();

        assertTrue(gate.trySchedule(player, "jobs"));
        assertFalse(gate.trySchedule(player, "jobs"));
        gate.complete(player, "jobs");
        assertTrue(gate.trySchedule(player, "jobs"));
    }

    @Test
    public void staleCompletionCannotClearNewMenuRequest() {
        RefreshRequestGate gate = new RefreshRequestGate();
        UUID player = UUID.randomUUID();

        assertTrue(gate.trySchedule(player, "first"));
        gate.complete(player, "other");
        assertFalse(gate.trySchedule(player, "second"));
        gate.clear(player);
        assertTrue(gate.trySchedule(player, "second"));
    }
}
