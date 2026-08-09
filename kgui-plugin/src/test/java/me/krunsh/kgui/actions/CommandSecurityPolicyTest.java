package me.krunsh.kgui.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommandSecurityPolicyTest {
    @Test
    public void localConsoleKeepsValidatedConfigurationFreedom() {
        assertTrue(CommandSecurityPolicy.isSafeConsoleTemplate(
            "give %player_name% diamond 1", ActionOrigin.LOCAL_MENU));
        assertTrue(CommandSecurityPolicy.isSafeResolvedCommand("give Krunsh diamond 1", true));
    }

    @Test
    public void providersAndRawPlayerDataCannotGainConsoleAuthority() {
        assertFalse(CommandSecurityPolicy.isSafeConsoleTemplate("give %player% diamond", ActionOrigin.PROVIDER));
        assertFalse(CommandSecurityPolicy.isSafeConsoleTemplate(
            "eco give %player% {kgui_data_amount}", ActionOrigin.LOCAL_MENU));
        assertFalse(CommandSecurityPolicy.isSafeConsoleTemplate(
            "eco give %player% %some_player_input%", ActionOrigin.LOCAL_MENU));
    }

    @Test
    public void separatorsControlCharactersAndAdministrativeRootsAreDenied() {
        assertFalse(CommandSecurityPolicy.isSafeConsoleTemplate("op %player%", ActionOrigin.LOCAL_MENU));
        assertFalse(CommandSecurityPolicy.isSafePlayerTemplate("warp spawn; stop"));
        assertFalse(CommandSecurityPolicy.isSafePlayerTemplate("warp spawn\nstop"));
        assertFalse(CommandSecurityPolicy.isSafePlayerTemplate("warp spawn && stop"));
    }
}
