package me.krunsh.kgui.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.plugin.Plugin;
import org.junit.Test;

public class KguiApiNamespacePolicyTest {
    @Test
    public void shortIdsAreOwnedAndForeignNamespacesAreRejected() throws Exception {
        Method normalize = KguiApiProvider.class.getDeclaredMethod("normalizeOwnedId", Plugin.class, String.class);
        normalize.setAccessible(true);
        Plugin owner = plugin("KjobsUltimate");

        assertEquals("kjobsultimate:members", normalize.invoke(null, owner, "members"));
        assertEquals("kjobsultimate:members", normalize.invoke(null, owner, "KjobsUltimate:Members"));
        try {
            normalize.invoke(null, owner, "kfaction:members");
        } catch (InvocationTargetException error) {
            assertTrue(error.getCause() instanceof IllegalArgumentException);
            return;
        }
        throw new AssertionError("Foreign namespace should have been rejected");
    }

    private static Plugin plugin(String name) {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] { Plugin.class },
            (instance, method, arguments) -> {
                if ("getName".equals(method.getName())) return name;
                if ("isEnabled".equals(method.getName())) return true;
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                return null;
            });
    }
}
