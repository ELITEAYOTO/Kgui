package me.krunsh.kgui.render;

import org.bukkit.event.inventory.ClickType;

/** Liste blanche explicite : tout clic inconnu est annule sans action. */
public final class GuiClickPolicy {
    private GuiClickPolicy() {
    }

    public static GuiClick actionClick(ClickType click) {
        if (click == null) return GuiClick.UNSUPPORTED;
        switch (click) {
            case LEFT: return GuiClick.LEFT;
            case RIGHT: return GuiClick.RIGHT;
            case SHIFT_LEFT: return GuiClick.SHIFT_LEFT;
            case SHIFT_RIGHT: return GuiClick.SHIFT_RIGHT;
            case MIDDLE: return GuiClick.MIDDLE;
            default: return GuiClick.UNSUPPORTED;
        }
    }
}
