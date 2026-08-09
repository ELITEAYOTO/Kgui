package me.krunsh.kgui.render;

import org.bukkit.event.inventory.ClickType;
import org.junit.Test;

import static org.junit.Assert.*;

public class InventoryInteractionPolicyTest {
    @Test
    public void everyMovementPathIsCancelledForKgui() {
        assertTrue(InventoryInteractionPolicy.cancelClick(true));
        assertTrue(InventoryInteractionPolicy.cancelDrag(true));
        assertTrue(InventoryInteractionPolicy.cancelMove(true, false));
        assertTrue(InventoryInteractionPolicy.cancelMove(false, true));
        assertFalse(InventoryInteractionPolicy.cancelMove(false, false));
    }

    @Test
    public void actionsRouteOnlyForWhitelistedTopSlots() {
        assertTrue(InventoryInteractionPolicy.routeAction(0, 54, true, GuiClick.LEFT));
        assertFalse(InventoryInteractionPolicy.routeAction(54, 54, true, GuiClick.LEFT));
        assertFalse(InventoryInteractionPolicy.routeAction(-999, 54, true, GuiClick.LEFT));
        assertFalse(InventoryInteractionPolicy.routeAction(0, 54, false, GuiClick.LEFT));
        assertFalse(InventoryInteractionPolicy.routeAction(0, 54, true, GuiClick.UNSUPPORTED));
    }

    @Test
    public void dangerousBukkitClicksNeverBecomeActions() {
        assertEquals(GuiClick.UNSUPPORTED, GuiClickPolicy.actionClick(ClickType.NUMBER_KEY));
        assertEquals(GuiClick.UNSUPPORTED, GuiClickPolicy.actionClick(ClickType.DOUBLE_CLICK));
        assertEquals(GuiClick.UNSUPPORTED, GuiClickPolicy.actionClick(ClickType.DROP));
        assertEquals(GuiClick.UNSUPPORTED, GuiClickPolicy.actionClick(ClickType.CONTROL_DROP));
        assertEquals(GuiClick.UNSUPPORTED, GuiClickPolicy.actionClick(ClickType.CREATIVE));
        assertEquals(GuiClick.UNSUPPORTED, GuiClickPolicy.actionClick(ClickType.UNKNOWN));
    }
}
