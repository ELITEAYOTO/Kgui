/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model;

import java.util.ArrayList;
import java.util.List;

public class KitRequirements {
    private boolean oneTimeRequirements;
    private double price;
    private List<String> extraRequirements;
    private List<String> message;
    private List<String> actionsOnBuy;

    public KitRequirements(boolean oneTimeRequirements, List<String> extraRequirements, List<String> message, List<String> actionsOnBuy, double price) {
        this.oneTimeRequirements = oneTimeRequirements;
        this.extraRequirements = extraRequirements;
        this.message = message;
        this.actionsOnBuy = actionsOnBuy;
        this.price = price;
    }

    public KitRequirements() {
        this.extraRequirements = new ArrayList<String>();
        this.message = new ArrayList<String>();
        this.message.add("&6You need $5000 to get this kit.");
        this.message.add("&8Status: &7$%vault_eco_balance% &8- %status_symbol_price%");
        this.actionsOnBuy = new ArrayList<String>();
    }

    public boolean isOneTimeRequirements() {
        return this.oneTimeRequirements;
    }

    public void setOneTimeRequirements(boolean oneTimeRequirements) {
        this.oneTimeRequirements = oneTimeRequirements;
    }

    public List<String> getMessage() {
        return this.message;
    }

    public void setMessage(List<String> message) {
        this.message = message;
    }

    public List<String> getActionsOnBuy() {
        return this.actionsOnBuy;
    }

    public void setActionsOnBuy(List<String> actionsOnBuy) {
        this.actionsOnBuy = actionsOnBuy;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<String> getExtraRequirements() {
        return this.extraRequirements;
    }

    public void setExtraRequirements(List<String> extraRequirements) {
        this.extraRequirements = extraRequirements;
    }
}

