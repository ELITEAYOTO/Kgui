package me.krunsh.kgui.refresh;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class RefreshArchitectureContractTest {
    @Test
    public void globalSessionPollingAndV1StateManagersAreGone() throws Exception {
        Path root = source();
        String gui = read(root.resolve(Paths.get("gui", "GuiManager.java")));
        String plugin = read(root.resolve("Kgui.java"));
        assertFalse(gui.contains("startAutoRefreshTask"));
        assertFalse(gui.contains("refreshMatchingMenus"));
        assertFalse(gui.contains("getPaginationManager"));
        assertFalse(gui.contains("getScrollManager"));
        assertTrue(plugin.contains("RefreshScheduler"));
        assertFalse(Files.exists(root.resolve(Paths.get("pagination", "PaginationManager.java"))));
        assertFalse(Files.exists(root.resolve(Paths.get("pagination", "ScrollManager.java"))));
    }

    @Test
    public void stableRefreshCreatesInventoryOnlyBehindReopenGuard() throws Exception {
        String gui = read(source().resolve(Paths.get("gui", "GuiManager.java")));
        int refresh = gui.indexOf("private void performRefresh");
        int guard = gui.indexOf("if (mustReopen)", refresh);
        int creation = gui.indexOf("Bukkit.createInventory", guard);
        int stableBranch = gui.indexOf("} else {", creation);
        assertTrue(refresh >= 0 && guard > refresh && creation > guard && stableBranch > creation);
        assertFalse(gui.substring(stableBranch, gui.indexOf("session.setLastRefreshTime", stableBranch))
            .contains("Bukkit.createInventory"));
    }

    private static Path source() {
        return Paths.get(System.getProperty("basedir"), "src", "main", "java", "me", "krunsh", "kgui");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
