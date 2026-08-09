package me.krunsh.kgui.listeners;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class QuitInventoryCleanupContractTest {
    @Test
    public void quitAndKickResetTheServerContainerAfterClosingTheSession() throws Exception {
        Path source = Paths.get(System.getProperty("basedir"), "src", "main", "java", "me",
            "krunsh", "kgui", "listeners", "SecurityListener.java");
        String code = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        int quit = code.indexOf("CloseReason.QUIT");
        int kick = code.indexOf("CloseReason.KICK");
        assertTrue(quit >= 0 && code.indexOf("closeInventory();", quit) > quit);
        assertTrue(kick >= 0 && code.indexOf("closeInventory();", kick) > kick);
    }
}
