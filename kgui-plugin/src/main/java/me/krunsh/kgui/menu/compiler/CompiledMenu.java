package me.krunsh.kgui.menu.compiler;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.krunsh.kgui.menu.MenuType;

/** Canonical immutable representation of a fully resolved menu or template. */
public final class CompiledMenu {
    private final String id;
    private final File source;
    private final boolean template;
    private final int schemaVersion;
    private final String templateId;
    private final String title;
    private final int size;
    private final MenuType type;
    private final Map<String, CompiledItem> items;
    private final Map<String, Object> config;

    CompiledMenu(String id, File source, boolean template, int schemaVersion,
                 String templateId, String title, int size, MenuType type,
                 Map<String, CompiledItem> items, Map<String, Object> config) {
        this.id = id;
        this.source = source;
        this.template = template;
        this.schemaVersion = schemaVersion;
        this.templateId = templateId;
        this.title = title;
        this.size = size;
        this.type = type;
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
        this.config = ImmutableConfig.freezeMap(config);
    }

    public String getId() { return id; }
    public File getSource() { return source; }
    public boolean isTemplate() { return template; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getTemplateId() { return templateId; }
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public MenuType getType() { return type; }
    public Map<String, CompiledItem> getItems() { return items; }
    public Map<String, Object> getConfig() { return config; }

    /** Creates a detached mutable Bukkit configuration for the V1 runtime adapter. */
    public YamlConfiguration toYamlConfiguration() {
        YamlConfiguration yaml = new YamlConfiguration();
        copyMap(yaml, config);
        return yaml;
    }

    public String dump() {
        return toYamlConfiguration().saveToString();
    }

    @SuppressWarnings("unchecked")
    private static void copyMap(ConfigurationSection target, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                ConfigurationSection child = target.createSection(entry.getKey());
                copyMap(child, (Map<String, Object>) value);
            } else {
                target.set(entry.getKey(), ImmutableConfig.mutableCopy(value));
            }
        }
    }
}
