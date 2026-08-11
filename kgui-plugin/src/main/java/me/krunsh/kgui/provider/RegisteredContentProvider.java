package me.krunsh.kgui.provider;

import me.krunsh.kgui.api.ContentProvider;
import org.bukkit.plugin.Plugin;

/** Vue interne et éphémère d'une inscription provider possédée. */
public final class RegisteredContentProvider {
    private final String id;
    private final Plugin owner;
    private final ContentProvider provider;
    private final long generation;

    public RegisteredContentProvider(String id, Plugin owner, ContentProvider provider, long generation) {
        this.id = id;
        this.owner = owner;
        this.provider = provider;
        this.generation = generation;
    }

    public String getId() { return id; }
    public Plugin getOwner() { return owner; }
    public ContentProvider getProvider() { return provider; }
    public long getGeneration() { return generation; }

    public boolean isActive() {
        return owner != null && owner.isEnabled();
    }
}
