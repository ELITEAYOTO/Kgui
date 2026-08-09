package me.krunsh.kgui.extension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Petit registre deterministe pour les extensions du coeur.
 *
 * <p>Chaque entree a une identite canonique namespaced. Les anciens noms courts
 * ne sont que des alias de compatibilite et ne creent jamais une seconde entree.</p>
 */
public final class NamespacedRegistry<T> {
    private static final String ID_PATTERN = "[a-z0-9_.-]+:[a-z0-9_./-]+";

    private final String defaultNamespace;
    private final Map<String, T> values = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public NamespacedRegistry(String defaultNamespace) {
        String normalized = normalizePart(defaultNamespace, "namespace");
        if (!normalized.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid namespace: " + defaultNamespace);
        }
        this.defaultNamespace = normalized;
    }

    /** Compatibilite avec l'ancien usage Map.put(nomCourt, valeur). */
    public synchronized void put(String id, T value) {
        register(id, value);
    }

    public synchronized void register(String requestedId, T value, String... legacyAliases) {
        if (value == null) throw new IllegalArgumentException("value must not be null");
        String canonical = canonicalize(requestedId);
        if (values.containsKey(canonical)) {
            throw new IllegalStateException("Extension already registered: " + canonical);
        }
        values.put(canonical, value);

        String shortId = canonical.substring(canonical.indexOf(':') + 1);
        if (canonical.startsWith(defaultNamespace + ":")) addAlias(shortId, canonical);
        if (legacyAliases != null) {
            for (String alias : legacyAliases) addAlias(alias, canonical);
        }
    }

    public synchronized T get(String requestedId) {
        if (requestedId == null || requestedId.trim().isEmpty()) return null;
        String normalized = requestedId.trim().toLowerCase(Locale.ROOT);
        String canonical = normalized.indexOf(':') >= 0 ? normalized : aliases.get(normalized);
        if (canonical == null) canonical = defaultNamespace + ":" + normalized;
        return values.get(canonical);
    }

    public synchronized String canonicalId(String requestedId) {
        if (requestedId == null || requestedId.trim().isEmpty()) return null;
        String normalized = requestedId.trim().toLowerCase(Locale.ROOT);
        if (normalized.indexOf(':') >= 0) return values.containsKey(normalized) ? normalized : null;
        String canonical = aliases.get(normalized);
        if (canonical == null) canonical = defaultNamespace + ":" + normalized;
        return values.containsKey(canonical) ? canonical : null;
    }

    public synchronized Set<String> ids() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(values.keySet()));
    }

    public synchronized int size() {
        return values.size();
    }

    private void addAlias(String requestedAlias, String canonical) {
        String alias = normalizePart(requestedAlias, "alias");
        if (alias.indexOf(':') >= 0 || !alias.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid legacy alias: " + requestedAlias);
        }
        String previous = aliases.putIfAbsent(alias, canonical);
        if (previous != null && !previous.equals(canonical)) {
            throw new IllegalStateException("Alias already registered: " + alias);
        }
    }

    private String canonicalize(String requestedId) {
        String id = normalizePart(requestedId, "id");
        if (id.indexOf(':') < 0) id = defaultNamespace + ":" + id;
        if (!id.matches(ID_PATTERN)) throw new IllegalArgumentException("Invalid namespaced id: " + id);
        return id;
    }

    private static String normalizePart(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
