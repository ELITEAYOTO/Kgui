package me.krunsh.kgui.render;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class RenderedSlotTest {
    @Test
    public void dynamicActionsRemainInServerSideBinding() {
        ClickBinding binding = ClickBinding.forProviderItem("test:logs", "42", 1L, 1L, 4,
            Arrays.asList("[message] general"), Arrays.asList("[message] left"),
            Arrays.asList("[message] right"), Arrays.asList("[message] shift"));
        RenderedSlot slot = new RenderedSlot(4, new ItemStack(Material.PAPER), binding);

        assertEquals(Collections.singletonList("[message] left"),
            slot.getBinding().actionsFor(GuiClick.LEFT));
        assertEquals(Collections.singletonList("[message] right"),
            slot.getBinding().actionsFor(GuiClick.RIGHT));
        assertEquals(Collections.singletonList("[message] shift"),
            slot.getBinding().actionsFor(GuiClick.SHIFT_LEFT));
        assertTrue(slot.getBinding().actionsFor(GuiClick.UNSUPPORTED).isEmpty());
    }

    @Test
    public void itemSnapshotsCannotBeMutatedByCallers() {
        RenderedSlot slot = new RenderedSlot(0, new ItemStack(Material.DIAMOND, 2), null);
        ItemStack exposed = slot.getItem();
        exposed.setAmount(64);

        assertEquals(2, slot.getItem().getAmount());
    }
}
