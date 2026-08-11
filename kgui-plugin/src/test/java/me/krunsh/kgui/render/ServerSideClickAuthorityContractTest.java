package me.krunsh.kgui.render;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerSideClickAuthorityContractTest {
    @Test
    public void executableActionsAreNeverStoredOrReadFromItemNbt() throws Exception {
        Path sources = Paths.get(System.getProperty("basedir"), "src", "main", "java",
            "me", "krunsh", "kgui");
        String guiManager = read(sources.resolve(Paths.get("gui", "GuiManager.java")));
        String listener = read(sources.resolve(Paths.get("listeners", "GuiListener.java")));

        assertFalse(guiManager.contains("kgui_pagination_actions"));
        assertFalse(listener.contains("kgui_pagination_actions"));
        assertFalse(listener.contains("new NBTItem"));
        assertTrue(listener.contains("resolveClickedSlot"));
    }

    private String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
