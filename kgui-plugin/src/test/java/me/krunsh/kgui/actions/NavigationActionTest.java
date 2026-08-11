package me.krunsh.kgui.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class NavigationActionTest {
    @Test
    public void parsesBoundedRelativeNavigation() {
        NavigationAction action = NavigationAction.parse("direction=down step=3");
        assertNotNull(action);
        assertEquals(NavigationAction.Kind.MOVE, action.getKind());
        assertEquals(3, action.getValue());

        NavigationAction previous = NavigationAction.parse("direction=previous");
        assertNotNull(previous);
        assertEquals(-1, previous.getValue());
    }

    @Test
    public void parsesAbsolutePosition() {
        NavigationAction action = NavigationAction.parse("page=12");
        assertNotNull(action);
        assertEquals(NavigationAction.Kind.SET, action.getKind());
        assertEquals(12, action.getValue());
    }

    @Test
    public void rejectsAmbiguousOrUnboundedArguments() {
        assertNull(NavigationAction.parse("direction=next page=2"));
        assertNull(NavigationAction.parse("direction=next step=101"));
        assertNull(NavigationAction.parse("direction=sideways"));
        assertNull(NavigationAction.parse("page=0"));
        assertNull(NavigationAction.parse("direction=next direction=next"));
    }
}
