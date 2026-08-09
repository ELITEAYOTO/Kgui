package me.krunsh.kgui.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Proxy;
import org.junit.Test;

public class KguiApiCompatibilityTest {
    @Test
    public void absentApiIsExplicit() {
        KguiApiCompatibility result = KguiApiCompatibility.evaluate(null);
        assertEquals(KguiApiCompatibility.Status.ABSENT, result.getStatus());
        assertFalse(result.isReady());
    }

    @Test
    public void incompatibleMajorIsRejected() {
        KguiApi api = proxy("3.0.0", 3);
        assertEquals(KguiApiCompatibility.Status.INCOMPATIBLE,
                KguiApiCompatibility.evaluate(api).getStatus());
    }

    @Test
    public void currentMajorIsReady() {
        KguiApiCompatibility result = KguiApiCompatibility.evaluate(proxy("2.7.4", 2));
        assertEquals(KguiApiCompatibility.Status.READY_2_0, result.getStatus());
        assertTrue(result.isReady());
    }

    private static KguiApi proxy(String version, int major) {
        return (KguiApi) Proxy.newProxyInstance(KguiApi.class.getClassLoader(),
                new Class<?>[] { KguiApi.class }, (instance, method, args) -> {
                    if ("getApiVersion".equals(method.getName())) return version;
                    if ("getApiMajor".equals(method.getName())) return major;
                    return null;
                });
    }
}
