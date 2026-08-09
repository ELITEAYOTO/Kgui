package me.krunsh.kgui.menu;

/**
 * Types de menus supportés
 */
public enum MenuType {
    
    /**
     * Menu standard sans pagination
     */
    NORMAL,
    
    /**
     * Menu avec pagination automatique
     */
    PAGINATION,
    
    /**
     * Menu avec scroll horizontal
     */
    SCROLL,
    
    /**
     * Menu dynamique qui se met à jour automatiquement
     */
    DYNAMIC;

    /**
     * Parse un type depuis une string
     */
    public static MenuType fromString(String type) {
        if (type == null) return NORMAL;
        
        switch (type.toLowerCase()) {
            case "pagination":
            case "paginated":
            case "paged":
                return PAGINATION;
            case "scroll":
            case "scrolling":
            case "horizontal":
                return SCROLL;
            case "dynamic":
            case "animated":
                return DYNAMIC;
            default:
                return NORMAL;
        }
    }
}
