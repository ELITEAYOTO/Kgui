/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.item;

import java.util.ArrayList;
import java.util.List;

public class KitItemCustomModelComponentData {
    private List<String> flags;
    private List<String> colors;
    private List<String> floats;
    private List<String> strings;

    public KitItemCustomModelComponentData(List<String> flags, List<String> colors, List<String> floats, List<String> strings) {
        this.flags = flags;
        this.colors = colors;
        this.floats = floats;
        this.strings = strings;
    }

    public List<String> getFlags() {
        return this.flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags;
    }

    public List<String> getColors() {
        return this.colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public List<String> getFloats() {
        return this.floats;
    }

    public void setFloats(List<String> floats) {
        this.floats = floats;
    }

    public List<String> getStrings() {
        return this.strings;
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
    }

    public KitItemCustomModelComponentData clone() {
        return new KitItemCustomModelComponentData(new ArrayList<String>(this.flags), new ArrayList<String>(this.colors), new ArrayList<String>(this.floats), new ArrayList<String>(this.strings));
    }
}

