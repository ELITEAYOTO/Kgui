package me.krunsh.kgui.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import me.krunsh.kgui.api.MenuArguments;

/** Clé stable d'un contenu propre à un joueur/menu/arguments. */
public final class ProviderSubject {
    private final String providerId;
    private final long generation;
    private final UUID playerId;
    private final String menuId;
    private final MenuArguments arguments;
    private final String argumentFingerprint;

    public ProviderSubject(String providerId, long generation, UUID playerId, String menuId,
                           MenuArguments arguments) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.generation = generation;
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.menuId = Objects.requireNonNull(menuId, "menuId");
        this.arguments = arguments == null ? MenuArguments.empty() : arguments;
        this.argumentFingerprint = fingerprint(this.arguments.asMap());
    }

    public String getProviderId() { return providerId; }
    public long getGeneration() { return generation; }
    public UUID getPlayerId() { return playerId; }
    public String getMenuId() { return menuId; }
    public MenuArguments getArguments() { return arguments; }

    private static String fingerprint(Map<String, String> values) {
        if (values == null || values.isEmpty()) return "";
        List<String> keys = new ArrayList<>(values.keySet());
        Collections.sort(keys);
        StringBuilder result = new StringBuilder();
        for (String key : keys) {
            String value = values.get(key);
            result.append(key.length()).append(':').append(key)
                .append('=').append(value.length()).append(':').append(value).append(';');
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProviderSubject)) return false;
        ProviderSubject that = (ProviderSubject) other;
        return generation == that.generation && providerId.equals(that.providerId)
            && playerId.equals(that.playerId) && menuId.equals(that.menuId)
            && argumentFingerprint.equals(that.argumentFingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, generation, playerId, menuId, argumentFingerprint);
    }
}
