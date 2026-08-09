package me.krunsh.kgui.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public class KfactionSoftDependBoundaryTest {

    @Test
    public void lifecycleManagerDoesNotLinkKfactionTypesEagerly() throws Exception {
        String resource = "/" + KfactionIntegrationManager.class.getName().replace('.', '/') + ".class";
        InputStream input = KfactionIntegrationManager.class.getResourceAsStream(resource);
        assertTrue(input != null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = input.read(buffer)) >= 0) bytes.write(buffer, 0, read);
        input.close();

        String constantPool = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
        assertFalse(constantPool.contains("me/krunsh/kfaction/"));
    }

    @Test
    public void pluginMetadataKeepsKfactionOptional() {
        YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/plugin.yml"));
        assertTrue(plugin.getStringList("softdepend").contains("Kfaction"));
        assertFalse(plugin.getStringList("depend").contains("Kfaction"));
    }
}
