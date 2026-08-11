package me.krunsh.kgui.menu.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AtomicSnapshotTest {
    @Test
    public void invalidCandidateKeepsPreviousValue() {
        AtomicSnapshot<String> snapshot = new AtomicSnapshot<>("known-good");

        assertFalse(snapshot.publishIfValid(false, "broken"));
        assertEquals("known-good", snapshot.get());
        assertTrue(snapshot.publishIfValid(true, "validated"));
        assertEquals("validated", snapshot.get());
    }
}
