package me.krunsh.kgui.hooks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class HookLifecycleContractTest {
    @Test
    public void softDependencyAdaptersDoNotLinkExternalTypes() throws Exception {
        Path hooks = source("hooks");
        String all = "";
        for (String file : new String[] {"PlaceholderAPIHook.java", "VaultHook.java", "PlayerPointsHook.java",
                "HeadDatabaseHook.java", "WorldGuardHook.java", "CombatTagHook.java", "ProtocolLibHook.java"}) {
            String source = read(hooks.resolve(file));
            all += source;
            assertTrue(file, source.contains("implements AutoCloseable"));
        }
        assertFalse(all.contains("import me.clip.placeholderapi"));
        assertFalse(all.contains("import net.milkbowl.vault"));
        assertFalse(all.contains("import org.black_ixx.playerpoints"));
        assertFalse(all.contains("import me.arcaniax.hdb"));
        assertFalse(all.contains("import com.sk89q.worldguard"));
        assertFalse(all.contains("import net.minelink.ctplus"));
        assertFalse(all.contains("import com.comphenix.protocol"));
    }

    @Test
    public void managerTracksEnableDisableAndLegacyKfactionHookIsGone() throws Exception {
        String manager = read(source("hooks").resolve("HookManager.java"));
        assertTrue(manager.contains("PluginEnableEvent"));
        assertTrue(manager.contains("PluginDisableEvent"));
        assertTrue(manager.contains("implements Listener, AutoCloseable"));
        assertFalse(Files.exists(source("hooks").resolve("KfactionHook.java")));
        assertFalse(Files.exists(source("hooks").resolve("ZAuctionHouseHook.java")));
    }

    @Test
    public void actionEngineNeverTemporarilyGrantsOperator() throws Exception {
        String actions = read(source("actions").resolve("ActionManager.java"));
        assertFalse(actions.contains("setOp(true)"));
        assertTrue(actions.contains("Deprecated [op] action denied"));
        assertTrue(actions.contains("ActionOrigin.PROVIDER" ) || read(source("listeners").resolve("GuiListener.java"))
            .contains("ActionOrigin.PROVIDER"));
    }

    private static Path source(String child) {
        return Paths.get(System.getProperty("basedir"), "src", "main", "java", "me", "krunsh", "kgui", child);
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
