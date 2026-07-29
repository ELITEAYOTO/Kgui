/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.item;

public class KitItemSkullData {
    private String owner;
    private String texture;
    private String id;

    public KitItemSkullData(String owner, String texture, String id) {
        this.owner = owner;
        this.texture = texture;
        this.id = id;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTexture() {
        return this.texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public KitItemSkullData clone() {
        return new KitItemSkullData(this.owner, this.texture, this.id);
    }
}

