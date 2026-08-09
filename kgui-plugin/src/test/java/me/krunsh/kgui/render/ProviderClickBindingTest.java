package me.krunsh.kgui.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class ProviderClickBindingTest {
    @Test
    public void bindingCarriesServerSideProviderIdentityAndRevision() {
        ClickBinding binding = ClickBinding.forProviderItem("kfaction:members", "member-42",
            9L, 3L, 22, Collections.singletonList("[kgui:message] ok"),
            null, null, null);
        assertTrue(binding.isProviderOwned());
        assertEquals("kfaction:members", binding.getProviderId());
        assertEquals("member-42", binding.getProviderItemId());
        assertEquals(9L, binding.getProviderRevision());
        assertEquals(3L, binding.getProviderGeneration());
        assertEquals(22, binding.getRenderedSlot());
        assertNotEquals(binding, ClickBinding.forProviderItem("kfaction:members", "member-42",
            10L, 3L, 22, null, null, null, null));
    }
}
