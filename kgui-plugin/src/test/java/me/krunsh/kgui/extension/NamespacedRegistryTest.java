package me.krunsh.kgui.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NamespacedRegistryTest {
    @Test
    public void shortAliasesResolveToOneCanonicalEntry() {
        NamespacedRegistry<String> registry = new NamespacedRegistry<>("kgui");
        registry.register("kgui:console_command", "handler", "console");

        assertEquals("handler", registry.get("console"));
        assertEquals("handler", registry.get("kgui:console_command"));
        assertEquals("kgui:console_command", registry.canonicalId("console"));
        assertEquals(1, registry.size());
    }

    @Test
    public void unknownAndForeignShortIdsNeverFallThrough() {
        NamespacedRegistry<String> registry = new NamespacedRegistry<>("kgui");
        registry.register("vault:balance", "vault", "money");

        assertEquals("vault", registry.get("money"));
        assertEquals("vault", registry.get("vault:balance"));
        assertNull(registry.get("balance"));
        assertNull(registry.get("unknown:balance"));
    }

    @Test(expected = IllegalStateException.class)
    public void aliasCollisionsAreRejected() {
        NamespacedRegistry<String> registry = new NamespacedRegistry<>("kgui");
        registry.register("kgui:first", "first", "legacy");
        registry.register("kgui:second", "second", "legacy");
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedIdsAreRejected() {
        new NamespacedRegistry<String>("kgui").register("bad namespace:value", "value");
    }
}
