package me.krunsh.kgui.requirements;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SafeExpressionEvaluatorTest {
    private final SafeExpressionEvaluator evaluator = new SafeExpressionEvaluator();

    @Test
    public void evaluatesOnlyBoundedBooleanNumericAndStringExpressions() {
        assertTrue(evaluator.evaluate("true"));
        assertTrue(evaluator.evaluate("10.5 >= 10"));
        assertTrue(evaluator.evaluate("Volkaria contains kari"));
        assertTrue(evaluator.evaluate("'officer' not_equals 'member'"));
        assertFalse(evaluator.evaluate("3 > 9"));
        assertFalse(evaluator.evaluate("false"));
    }

    @Test
    public void malformedOrScriptLikeInputFailsClosed() {
        assertFalse(evaluator.evaluate(null));
        assertFalse(evaluator.evaluate(""));
        assertFalse(evaluator.evaluate("12 emeralds > 3"));
        assertFalse(evaluator.evaluate("Runtime.getRuntime() equals allowed"));
        assertFalse(evaluator.evaluate("true || true"));
        assertFalse(evaluator.evaluate("javascript: player.setOp(true)"));
    }
}
