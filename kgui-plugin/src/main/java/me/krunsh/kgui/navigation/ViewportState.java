package me.krunsh.kgui.navigation;

import java.util.List;

/** Source de vérité unique pour PAGE et ROW_SCROLL dans une session. */
public final class ViewportState {
    private final NavigationMode mode;
    private final ViewportLayout layout;
    private int position;
    private int maxPosition;
    private int totalItems;

    public ViewportState(NavigationMode mode, ViewportLayout layout, int initialPage) {
        this.mode = mode == null ? NavigationMode.NONE : mode;
        this.layout = layout == null ? ViewportLayout.empty() : layout;
        this.position = Math.max(0, initialPage - 1);
        this.maxPosition = this.position;
        this.totalItems = -1;
    }

    public NavigationMode getMode() {
        return mode;
    }

    public ViewportLayout getLayout() {
        return layout;
    }

    /** Position humaine, 1..N, utilisée par %page%. */
    public int getPage() {
        return position + 1;
    }

    public int getMaxPage() {
        return maxPosition + 1;
    }

    /** Offset de ligne, zéro pour PAGE/NONE. */
    public int getRowOffset() {
        return mode == NavigationMode.ROW_SCROLL ? position : 0;
    }

    public int getMaxRowOffset() {
        return mode == NavigationMode.ROW_SCROLL ? maxPosition : 0;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getContentOffset() {
        if (mode == NavigationMode.PAGE) return position * Math.max(1, layout.getCapacity());
        if (mode == NavigationMode.ROW_SCROLL) return position * layout.getRowStride();
        return 0;
    }

    public int getRequestLimit() {
        return Math.max(1, layout.getCapacity());
    }

    /**
     * Réconcilie la fenêtre après lecture du total provider. totalItems < 0
     * signifie qu'il n'existe pas de provider et utilise staticMaxPages.
     */
    public boolean reconcile(int totalItems, int staticMaxPages) {
        int previousPosition = position;
        this.totalItems = totalItems;
        if (mode == NavigationMode.NONE) {
            maxPosition = 0;
        } else if (totalItems >= 0 && !layout.isEmpty()) {
            if (mode == NavigationMode.PAGE) {
                int pages = totalItems == 0 ? 1
                    : (int) (((long) totalItems + layout.getCapacity() - 1) / layout.getCapacity());
                maxPosition = Math.max(0, pages - 1);
            } else {
                int remaining = Math.max(0, totalItems - layout.getCapacity());
                maxPosition = remaining == 0 ? 0
                    : (int) (((long) remaining + layout.getRowStride() - 1) / layout.getRowStride());
            }
        } else {
            maxPosition = Math.max(0, staticMaxPages - 1);
        }
        position = Math.max(0, Math.min(position, maxPosition));
        return previousPosition != position;
    }

    public boolean next() {
        return move(1);
    }

    public boolean previous() {
        return move(-1);
    }

    public boolean move(int delta) {
        if (delta == 0) return false;
        int next = Math.max(0, Math.min(maxPosition, position + delta));
        if (next == position) return false;
        position = next;
        return true;
    }

    public boolean setPage(int page) {
        if (page < 1 || page > getMaxPage()) return false;
        int next = page - 1;
        if (next == position) return false;
        position = next;
        return true;
    }

    public String replacePlaceholders(String text) {
        if (text == null) return null;
        String page = String.valueOf(getPage());
        String maxPage = String.valueOf(getMaxPage());
        String offset = String.valueOf(getRowOffset());
        String maxOffset = String.valueOf(getMaxRowOffset());
        return text.replace("%page%", page)
            .replace("%max_page%", maxPage)
            .replace("%total_pages%", maxPage)
            .replace("%kgui_page%", page)
            .replace("%kgui_max_page%", maxPage)
            .replace("%scroll_offset%", offset)
            .replace("%scroll_max%", maxOffset)
            .replace("%kgui_scroll%", offset)
            .replace("%kgui_scroll_max%", maxOffset);
    }

    public int slotForSliceIndex(int index) {
        List<Integer> slots = layout.getSlots();
        return index < 0 || index >= slots.size() ? -1 : slots.get(index);
    }
}
