/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model.item;

import java.util.ArrayList;
import java.util.List;

public class KitItemFireworkData {
    private List<String> fireworkRocketEffects;
    private String fireworkStarEffect;
    private int fireworkPower;

    public KitItemFireworkData(List<String> fireworkRocketEffects, String fireworkStarEffect, int fireworkPower) {
        this.fireworkRocketEffects = fireworkRocketEffects;
        this.fireworkStarEffect = fireworkStarEffect;
        this.fireworkPower = fireworkPower;
    }

    public List<String> getFireworkRocketEffects() {
        return this.fireworkRocketEffects;
    }

    public void setFireworkRocketEffects(List<String> fireworkRocketEffects) {
        this.fireworkRocketEffects = fireworkRocketEffects;
    }

    public String getFireworkStarEffect() {
        return this.fireworkStarEffect;
    }

    public void setFireworkStarEffect(String fireworkStarEffect) {
        this.fireworkStarEffect = fireworkStarEffect;
    }

    public int getFireworkPower() {
        return this.fireworkPower;
    }

    public void setFireworkPower(int fireworkPower) {
        this.fireworkPower = fireworkPower;
    }

    public KitItemFireworkData clone() {
        return new KitItemFireworkData(new ArrayList<String>(this.fireworkRocketEffects), this.fireworkStarEffect, this.fireworkPower);
    }
}

