package me.krunsh.kgui.render;

/** Regles pures appliquees avant toute action ou mutation d'inventaire. */
public final class InventoryInteractionPolicy {
    private InventoryInteractionPolicy() {
    }

    public static boolean cancelClick(boolean kguiOnTop) {
        return kguiOnTop;
    }

    public static boolean cancelDrag(boolean kguiOnTop) {
        return kguiOnTop;
    }

    public static boolean cancelMove(boolean kguiSource, boolean kguiDestination) {
        return kguiSource || kguiDestination;
    }

    public static boolean routeAction(int rawSlot, int topSize, boolean clickedTop, GuiClick click) {
        return clickedTop && rawSlot >= 0 && rawSlot < topSize
            && click != null && click != GuiClick.UNSUPPORTED;
    }
}
