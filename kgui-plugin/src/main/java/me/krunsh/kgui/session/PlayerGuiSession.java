package me.krunsh.kgui.session;

import me.krunsh.kgui.gui.OpenGui;
import me.krunsh.kgui.render.RenderFrame;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Source de verite d'une ouverture Kgui.
 *
 * Tout etat joueur lie a la vue vit ici et est purge une seule fois par close().
 */
public final class PlayerGuiSession extends OpenGui {
    private final long sessionId;
    private final SessionToken token;
    private final Deque<String> history = new ArrayDeque<>();
    private final Map<String, Map<String, String>> runtimeArguments = new HashMap<>();
    private final Map<String, String> placeholderCache = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Set<BukkitTask> tasks = new HashSet<>();

    private long placeholderCacheTime;
    private long renderRevision;
    private RenderFrame frame;
    private boolean navigationLocked;
    private boolean closed;
    private CloseReason closeReason;

    public PlayerGuiSession(long sessionId, UUID playerId, String menuId, int page, Inventory inventory) {
        super(playerId, menuId, page, inventory);
        this.sessionId = sessionId;
        this.token = new SessionToken(playerId, sessionId);
    }

    public long getSessionId() {
        return sessionId;
    }

    public SessionToken getToken() {
        return token;
    }

    public synchronized boolean isActive() {
        return !closed;
    }

    public synchronized long nextRenderRevision() {
        if (closed) return renderRevision;
        return ++renderRevision;
    }

    public synchronized long getRenderRevision() {
        return renderRevision;
    }

    public synchronized RenderFrame getFrame() {
        return frame;
    }

    public synchronized void setFrame(RenderFrame frame) {
        if (!closed) this.frame = frame;
    }

    public Deque<String> getHistory() {
        return history;
    }

    public Map<String, Map<String, String>> getRuntimeArguments() {
        return runtimeArguments;
    }

    public Map<String, String> getPlaceholderCache() {
        return placeholderCache;
    }

    public long getPlaceholderCacheTime() {
        return placeholderCacheTime;
    }

    public void setPlaceholderCacheTime(long placeholderCacheTime) {
        this.placeholderCacheTime = placeholderCacheTime;
    }

    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }

    public synchronized boolean tryLockNavigation() {
        if (closed || navigationLocked) return false;
        navigationLocked = true;
        return true;
    }

    public synchronized void unlockNavigation() {
        navigationLocked = false;
    }

    public synchronized boolean trackTask(BukkitTask task) {
        if (task == null || closed) return false;
        tasks.add(task);
        return true;
    }

    public synchronized void untrackTask(BukkitTask task) {
        tasks.remove(task);
    }

    /** Copie uniquement l'etat qui doit survivre a une navigation. */
    public void inheritNavigationState(PlayerGuiSession previous) {
        if (previous == null) return;
        history.addAll(previous.history);
        for (Map.Entry<String, Map<String, String>> entry : previous.runtimeArguments.entrySet()) {
            runtimeArguments.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        cooldowns.putAll(previous.cooldowns);
    }

    public synchronized boolean close(CloseReason reason) {
        if (closed) return false;
        closed = true;
        closeReason = reason == null ? CloseReason.INVALID_STATE : reason;
        for (BukkitTask task : new HashSet<>(tasks)) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
                // Le scheduler peut deja avoir termine la tache.
            }
        }
        tasks.clear();
        history.clear();
        runtimeArguments.clear();
        placeholderCache.clear();
        cooldowns.clear();
        frame = null;
        setInventory(null);
        navigationLocked = false;
        return true;
    }

    public synchronized CloseReason getCloseReason() {
        return closeReason;
    }

    public synchronized int retainedStateSize() {
        return history.size() + runtimeArguments.size() + placeholderCache.size()
            + cooldowns.size() + tasks.size() + (frame == null ? 0 : frame.size());
    }

    public Map<String, Map<String, String>> runtimeArgumentsView() {
        return Collections.unmodifiableMap(runtimeArguments);
    }
}
