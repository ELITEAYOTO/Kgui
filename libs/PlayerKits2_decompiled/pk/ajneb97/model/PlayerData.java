/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.model;

import java.util.ArrayList;
import java.util.UUID;
import pk.ajneb97.model.PlayerDataKit;

public class PlayerData {
    private String name;
    private UUID uuid;
    private ArrayList<PlayerDataKit> kits;
    private boolean modified;

    public PlayerData(UUID uuid, String name) {
        this.name = name;
        this.uuid = uuid;
        this.kits = new ArrayList();
        this.modified = false;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<PlayerDataKit> getKits() {
        return this.kits;
    }

    public void setKits(ArrayList<PlayerDataKit> kits) {
        this.kits = kits;
    }

    public void addKit(PlayerDataKit kit) {
        this.kits.add(kit);
    }

    public boolean isModified() {
        return this.modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public PlayerDataKit getKit(String kitName) {
        for (PlayerDataKit kit : this.kits) {
            if (!kit.getName().equals(kitName)) continue;
            return kit;
        }
        return null;
    }

    public boolean setKitCooldown(String kitName, long cooldown) {
        PlayerDataKit playerDataKit = this.getKit(kitName);
        boolean creating = false;
        if (playerDataKit == null) {
            playerDataKit = new PlayerDataKit(kitName);
            this.kits.add(playerDataKit);
            creating = true;
        }
        playerDataKit.setCooldown(cooldown);
        return creating;
    }

    public long getKitCooldown(String kitName) {
        PlayerDataKit playerDataKit = this.getKit(kitName);
        if (playerDataKit == null) {
            return 0L;
        }
        return playerDataKit.getCooldown();
    }

    public boolean setKitOneTime(String kitName) {
        PlayerDataKit playerDataKit = this.getKit(kitName);
        boolean creating = false;
        if (playerDataKit == null) {
            playerDataKit = new PlayerDataKit(kitName);
            this.kits.add(playerDataKit);
            creating = true;
        }
        playerDataKit.setOneTime(true);
        return creating;
    }

    public boolean getKitOneTime(String kitName) {
        PlayerDataKit playerDataKit = this.getKit(kitName);
        if (playerDataKit == null) {
            return false;
        }
        return playerDataKit.isOneTime();
    }

    public boolean setKitBought(String kitName) {
        PlayerDataKit playerDataKit = this.getKit(kitName);
        boolean creating = false;
        if (playerDataKit == null) {
            playerDataKit = new PlayerDataKit(kitName);
            this.kits.add(playerDataKit);
            creating = true;
        }
        playerDataKit.setBought(true);
        return creating;
    }

    public boolean getKitHasBought(String kitName) {
        PlayerDataKit playerDataKit = this.getKit(kitName);
        if (playerDataKit == null) {
            return false;
        }
        return playerDataKit.isBought();
    }

    public void resetKit(String kitName) {
        for (int i = 0; i < this.kits.size(); ++i) {
            if (!this.kits.get(i).getName().equals(kitName)) continue;
            this.kits.remove(i);
            this.modified = true;
        }
    }
}

