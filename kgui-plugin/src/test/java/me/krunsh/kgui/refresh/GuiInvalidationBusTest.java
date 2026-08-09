package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;
import me.krunsh.kgui.api.InvalidationRequest;
import org.junit.Test;

public class GuiInvalidationBusTest {
    @Test
    public void fullProviderInvalidationRebuildsPagePlaceholders() {
        assertTrue(GuiInvalidationBus.placeholdersAffected(
            InvalidationRequest.provider("kfaction:members", "total-changed")));
    }

    @Test
    public void targetedItemInvalidationKeepsStaticLayer() {
        assertFalse(GuiInvalidationBus.placeholdersAffected(
            InvalidationRequest.playerMenu(UUID.randomUUID(), "members",
                Collections.singleton("member:abc"), "role-changed")));
    }
}
