package me.krunsh.kgui.requirements;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FailClosedRequirementPolicyTest {
    @Test
    public void unavailableOrMalformedOutcomesAreAlwaysDenied() {
        assertFalse(RequirementDecision.resolve(null, false));
        assertFalse(RequirementDecision.resolve(true, null));
        assertFalse(RequirementDecision.resolve(false, false));
        assertTrue(RequirementDecision.resolve(true, false));
        assertTrue(RequirementDecision.resolve(false, true));
        assertFalse(RequirementDecision.resolve(false, true, false));
        assertFalse(RequirementDecision.resolve(true, false, false));
    }

    @Test
    public void regexSubsetRejectsCommonBacktrackingAndReflectionConstructs() {
        assertTrue(SafeRegex.matches("Krunsh_42", "[A-Za-z0-9_]{1,16}"));
        assertFalse(SafeRegex.matches("aaaaaaaaaaaaaaaa!", "(a+)+$"));
        assertFalse(SafeRegex.matches("abc", "(?=abc)abc"));
        assertFalse(SafeRegex.matches("abc", "["));
    }
}
