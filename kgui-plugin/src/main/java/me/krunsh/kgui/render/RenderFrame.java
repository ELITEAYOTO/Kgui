package me.krunsh.kgui.render;

/** Tableau immutable de slots produit pour une revision de session. */
public final class RenderFrame {
    private final long revision;
    private final RenderedSlot[] slots;

    public RenderFrame(long revision, RenderedSlot[] slots) {
        this.revision = revision;
        this.slots = slots == null ? new RenderedSlot[0] : slots.clone();
    }

    public long getRevision() {
        return revision;
    }

    public int size() {
        return slots.length;
    }

    public RenderedSlot get(int slot) {
        return slot >= 0 && slot < slots.length ? slots[slot] : null;
    }

    public RenderedSlot[] getSlots() {
        return slots.clone();
    }

    public int occupiedSlots() {
        int occupied = 0;
        for (RenderedSlot slot : slots) if (slot != null) occupied++;
        return occupied;
    }
}
