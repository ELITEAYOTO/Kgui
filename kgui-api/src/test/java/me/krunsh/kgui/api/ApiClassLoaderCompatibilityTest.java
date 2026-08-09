package me.krunsh.kgui.api;

import static org.junit.Assert.assertSame;
import org.junit.Test;

public class ApiClassLoaderCompatibilityTest {
    @Test
    public void dependentChildResolvesTheParentsApiIdentity() throws Exception {
        ClassLoader parent = KguiApi.class.getClassLoader();
        ClassLoader dependentPlugin = new ClassLoader(parent) { };
        Class<?> resolved = Class.forName(KguiApi.class.getName(), true, dependentPlugin);
        assertSame(KguiApi.class, resolved);
        assertSame(parent, resolved.getClassLoader());
    }
}
