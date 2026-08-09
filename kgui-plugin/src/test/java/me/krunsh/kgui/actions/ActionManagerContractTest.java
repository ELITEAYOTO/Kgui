package me.krunsh.kgui.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ActionManagerContractTest {
    @Test
    public void factionMutationsCannotFallBackToCommands() {
        assertTrue(KfactionCommandPolicy.isReserved("f claim"));
        assertTrue(KfactionCommandPolicy.isReserved("/Faction invite player"));
        assertTrue(KfactionCommandPolicy.isReserved("  //kfaction disband"));
        assertFalse(KfactionCommandPolicy.isReserved("fly"));
        assertFalse(KfactionCommandPolicy.isReserved("friends open"));
    }
}
