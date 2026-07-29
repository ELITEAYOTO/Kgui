/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ClickEvent
 *  net.md_5.bungee.api.chat.ClickEvent$Action
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 */
package pk.ajneb97.utils;

import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class JSONMessage {
    private Player player;
    private String text;
    private BaseComponent[] hover;
    private String suggestCommand;
    private String executeCommand;

    public JSONMessage(Player player, String text) {
        this.player = player;
        this.hover = null;
        this.text = text;
    }

    public JSONMessage hover(List<String> list) {
        this.hover = new BaseComponent[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            TextComponent line = new TextComponent();
            if (i == list.size() - 1) {
                line.setText(ChatColor.translateAlternateColorCodes((char)'&', (String)list.get(i)));
            } else {
                line.setText(ChatColor.translateAlternateColorCodes((char)'&', (String)list.get(i)) + "\n");
            }
            this.hover[i] = line;
        }
        return this;
    }

    public JSONMessage setSuggestCommand(String command) {
        this.suggestCommand = command;
        return this;
    }

    public JSONMessage setExecuteCommand(String command) {
        this.executeCommand = command;
        return this;
    }

    public void send() {
        TextComponent message = new TextComponent();
        message.setText(ChatColor.translateAlternateColorCodes((char)'&', (String)this.text));
        if (this.hover != null) {
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, this.hover));
        }
        if (this.suggestCommand != null) {
            message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, this.suggestCommand));
        }
        if (this.executeCommand != null) {
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, this.executeCommand));
        }
        this.player.spigot().sendMessage((BaseComponent)message);
    }
}

