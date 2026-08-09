package me.krunsh.kgui.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.lang.reflect.Method;
import org.junit.Test;

public class KguiApiContractTest {
    @Test
    public void versionSurfaceIsFrozenAtTwoZero() {
        assertEquals("2.0.0", KguiApiVersion.CURRENT);
        assertEquals(2, KguiApiVersion.MAJOR);
        assertEquals(9, KguiApi.class.getDeclaredMethods().length);
    }

    @Test
    public void publicContractDoesNotExposeEngineTypes() {
        for (Method method : KguiApi.class.getDeclaredMethods()) {
            assertSafe(method.getReturnType());
            for (Class<?> parameter : method.getParameterTypes()) assertSafe(parameter);
        }
        for (Method method : ContentProvider.class.getDeclaredMethods()) {
            assertSafe(method.getReturnType());
            for (Class<?> parameter : method.getParameterTypes()) assertSafe(parameter);
        }
    }

    private static void assertSafe(Class<?> type) {
        String name = type.getName();
        if (!name.startsWith("me.krunsh.kgui.")) return;
        assertFalse("internal type leaked: " + name, !name.startsWith("me.krunsh.kgui.api."));
    }
}
