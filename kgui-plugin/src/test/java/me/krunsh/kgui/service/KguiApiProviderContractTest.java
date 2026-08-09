package me.krunsh.kgui.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Modifier;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.KguiApiVersion;
import org.bukkit.event.Listener;
import org.junit.Test;

public class KguiApiProviderContractTest {
    @Test
    public void providerImplementsThePublicContractWithoutSubclassSurface() {
        assertTrue(KguiApi.class.isAssignableFrom(KguiApiProvider.class));
        assertTrue(Listener.class.isAssignableFrom(KguiApiProvider.class));
        assertTrue(Modifier.isFinal(KguiApiProvider.class.getModifiers()));
        assertEquals("2.0.0", KguiApiVersion.CURRENT);
    }
}
