package me.krunsh.kgui.menu;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.compiler.AtomicSnapshot;
import me.krunsh.kgui.menu.compiler.CompiledMenu;
import me.krunsh.kgui.menu.compiler.DiagnosticSeverity;
import me.krunsh.kgui.menu.compiler.MenuCompilationResult;
import me.krunsh.kgui.menu.compiler.MenuCompiler;
import me.krunsh.kgui.menu.compiler.MenuDiagnostic;

/** Compiles menus off-registry and publishes complete validated snapshots atomically. */
public class MenuManager {
    private final Kgui plugin;
    private final MenuCompiler compiler;
    private final LegacyMenuMaterializer materializer;
    private final AtomicSnapshot<State> state = new AtomicSnapshot<>(State.empty());

    public MenuManager(Kgui plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        this.compiler = new MenuCompiler(new File(dataFolder, "templates"), new File(dataFolder, "menus"));
        this.materializer = new LegacyMenuMaterializer(plugin);
        prepareDefaultSources();
        reload();
    }

    /** Compatibility alias. Prefer {@link #reload()}. */
    public void loadAll() {
        reload();
    }

    /** Validates every source without changing the live snapshot. */
    public MenuReloadResult validate() {
        return MenuReloadResult.validation(compiler.compileAll());
    }

    /** Compiles and atomically replaces the complete live snapshot on success. */
    public MenuReloadResult reload() {
        MenuCompilationResult compilation = compiler.compileAll();
        if (!compilation.isSuccess()) {
            logFailure(compilation, "Reload rejected; previous menu snapshot retained");
            return MenuReloadResult.publication(compilation, false, null);
        }
        try {
            Map<String, MenuData> runtimeMenus = materialize(compilation.getMenus());
            State candidate = new State(runtimeMenus, compilation.getMenus(), compilation.getTemplates());
            state.publishIfValid(true, candidate);
            plugin.getLogger().info("Published " + runtimeMenus.size() + " compiled menus atomically"
                + warningSuffix(compilation));
            return MenuReloadResult.publication(compilation, true, null);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Menu materialization failed; previous snapshot retained", exception);
            MenuDiagnostic diagnostic = new MenuDiagnostic(DiagnosticSeverity.ERROR, "MATERIALIZATION_ERROR",
                "<runtime>", "", 0, exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            return MenuReloadResult.failure(null, getMenuCount(), Collections.singletonList(diagnostic));
        }
    }

    /** Compiles the whole dependency graph, then publishes only one selected menu. */
    public MenuReloadResult reload(String menuId) {
        if (menuId == null || menuId.trim().isEmpty()) {
            MenuDiagnostic diagnostic = new MenuDiagnostic(DiagnosticSeverity.ERROR, "UNKNOWN_MENU",
                "<command>", "id", 0, "L'identifiant du menu ne peut pas être vide.");
            return MenuReloadResult.failure(null, getMenuCount(), Collections.singletonList(diagnostic));
        }
        String normalized = normalize(menuId);
        MenuCompilationResult compilation = compiler.compileAll();
        if (!compilation.isSuccess()) {
            logFailure(compilation, "Targeted reload rejected; previous menu snapshot retained");
            return MenuReloadResult.publication(compilation, false, normalized);
        }
        CompiledMenu compiled = compilation.getMenus().get(normalized);
        if (compiled == null) {
            MenuDiagnostic diagnostic = new MenuDiagnostic(DiagnosticSeverity.ERROR, "UNKNOWN_MENU",
                normalized + ".yml", "id", 0, "Menu inconnu: '" + normalized + "'.");
            return MenuReloadResult.failure(normalized, getMenuCount(), Collections.singletonList(diagnostic));
        }
        try {
            MenuData runtime = materializer.materialize(compiled);
            State current = state.get();
            Map<String, MenuData> runtimeMenus = new LinkedHashMap<>(current.runtimeMenus);
            Map<String, CompiledMenu> compiledMenus = new LinkedHashMap<>(current.compiledMenus);
            runtimeMenus.put(normalized, runtime);
            compiledMenus.put(normalized, compiled);
            State candidate = new State(runtimeMenus, compiledMenus, compilation.getTemplates());
            state.publishIfValid(true, candidate);
            plugin.getLogger().info("Published compiled menu '" + normalized + "' atomically" + warningSuffix(compilation));
            return MenuReloadResult.publication(compilation, true, normalized);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Menu materialization failed; previous snapshot retained", exception);
            MenuDiagnostic diagnostic = new MenuDiagnostic(DiagnosticSeverity.ERROR, "MATERIALIZATION_ERROR",
                compiled.getSource().getName(), "", 0,
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            return MenuReloadResult.failure(normalized, getMenuCount(), Collections.singletonList(diagnostic));
        }
    }

    public boolean hasMenu(String menuId) {
        return menuId != null && state.get().runtimeMenus.containsKey(normalize(menuId));
    }

    public MenuData getMenu(String menuId) {
        return menuId == null ? null : state.get().runtimeMenus.get(normalize(menuId));
    }

    public CompiledMenu getCompiledMenu(String menuId) {
        return menuId == null ? null : state.get().compiledMenus.get(normalize(menuId));
    }

    public int getMenuCount() {
        return state.get().runtimeMenus.size();
    }

    public Map<String, MenuData> getMenus() {
        return state.get().runtimeMenus;
    }

    public Set<String> getMenuIds() {
        return state.get().runtimeMenus.keySet();
    }

    public MenuData findByOpenCommand(String command) {
        if (command == null) return null;
        for (MenuData menu : state.get().runtimeMenus.values()) {
            if (command.equalsIgnoreCase(menu.getOpenCommand())) return menu;
            for (String candidate : menu.getOpenCommands()) {
                if (command.equalsIgnoreCase(candidate)) return menu;
            }
        }
        return null;
    }

    public List<MenuData> findByRegionEnter(String regionId) {
        List<MenuData> result = new ArrayList<>();
        for (MenuData menu : state.get().runtimeMenus.values()) {
            if (menu.getOpenOnRegionEnter().contains(regionId)) result.add(menu);
        }
        return result;
    }

    private Map<String, MenuData> materialize(Map<String, CompiledMenu> compiledMenus) {
        Map<String, MenuData> result = new LinkedHashMap<>();
        for (Map.Entry<String, CompiledMenu> entry : compiledMenus.entrySet()) {
            result.put(entry.getKey(), materializer.materialize(entry.getValue()));
        }
        return result;
    }

    private void prepareDefaultSources() {
        File templates = new File(plugin.getDataFolder(), "templates");
        File menus = new File(plugin.getDataFolder(), "menus");
        if (!templates.exists()) templates.mkdirs();
        if (!menus.exists()) menus.mkdirs();
        extractResourceFolder("templates");
        extractResourceFolder("menus");
    }

    private void extractResourceFolder(String folderName) {
        try {
            File jarPath = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarPath.isFile()) return;
            int extracted = 0;
            JarFile jar = new JarFile(jarPath);
            try {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(folderName + "/") && name.endsWith(".yml") && !entry.isDirectory()) {
                        File target = new File(plugin.getDataFolder(), name);
                        if (!target.exists()) {
                            plugin.saveResource(name, false);
                            extracted++;
                        }
                    }
                }
            } finally {
                jar.close();
            }
            if (extracted > 0) plugin.getLogger().info("Extracted " + extracted + " default " + folderName + " files");
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not extract " + folderName + " from JAR: " + exception.getMessage());
        }
    }

    private void logFailure(MenuCompilationResult result, String header) {
        plugin.getLogger().severe(header + " (" + result.getErrorCount() + " error(s), "
            + result.getWarningCount() + " warning(s))");
        int emitted = 0;
        for (MenuDiagnostic diagnostic : result.getDiagnostics()) {
            if (!diagnostic.isError()) continue;
            plugin.getLogger().severe(diagnostic.format());
            if (++emitted == 20) break;
        }
        if (result.getErrorCount() > emitted) {
            plugin.getLogger().severe("... and " + (result.getErrorCount() - emitted) + " more error(s)");
        }
    }

    private static String warningSuffix(MenuCompilationResult result) {
        return result.getWarningCount() == 0 ? "" : " (" + result.getWarningCount() + " migration warning(s))";
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static final class State {
        private final Map<String, MenuData> runtimeMenus;
        private final Map<String, CompiledMenu> compiledMenus;
        private final Map<String, CompiledMenu> compiledTemplates;

        private State(Map<String, MenuData> runtimeMenus, Map<String, CompiledMenu> compiledMenus,
                      Map<String, CompiledMenu> compiledTemplates) {
            this.runtimeMenus = immutable(runtimeMenus);
            this.compiledMenus = immutable(compiledMenus);
            this.compiledTemplates = immutable(compiledTemplates);
        }

        private static State empty() {
            return new State(Collections.<String, MenuData>emptyMap(),
                Collections.<String, CompiledMenu>emptyMap(), Collections.<String, CompiledMenu>emptyMap());
        }

        private static <T> Map<String, T> immutable(Map<String, T> source) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }
    }
}
