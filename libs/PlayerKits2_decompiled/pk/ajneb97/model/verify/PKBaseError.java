/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.model.verify;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public abstract class PKBaseError {
    protected String file;
    protected String errorText;
    protected boolean critical;
    protected String prefix;

    public PKBaseError(String file, String errorText, boolean critical) {
        this.file = file;
        this.errorText = errorText;
        this.critical = critical;
        this.prefix = "&e\u26a0 ";
        if (this.critical) {
            this.prefix = "&c\u26a0 ";
        }
    }

    public List<String> getFixedErrorText() {
        ArrayList<String> sepText = new ArrayList<String>();
        int currentPos = 0;
        for (int i = 0; i < this.errorText.length(); ++i) {
            String m;
            if (currentPos >= 35 && this.errorText.charAt(i) == ' ') {
                m = this.errorText.substring(i - currentPos, i);
                currentPos = 0;
                sepText.add(m);
            } else {
                ++currentPos;
            }
            if (i != this.errorText.length() - 1) continue;
            m = this.errorText.substring(i - currentPos + 1, this.errorText.length());
            sepText.add(m);
        }
        return sepText;
    }

    public abstract void sendMessage(Player var1);
}

