package me.krunsh.kgui.pagination;

/**
 * Calculs purs de pagination, partages entre l'initialisation et les refreshs.
 */
public final class PaginationState {

    private PaginationState() {
    }

    public static int maxPage(int itemCount, int itemsPerPage) {
        if (itemCount <= 0 || itemsPerPage <= 0) {
            return 1;
        }
        return Math.max(1, (itemCount + itemsPerPage - 1) / itemsPerPage);
    }

    public static int clampPage(int requestedPage, int maxPage) {
        int safeMax = Math.max(1, maxPage);
        return Math.max(1, Math.min(requestedPage, safeMax));
    }
}
