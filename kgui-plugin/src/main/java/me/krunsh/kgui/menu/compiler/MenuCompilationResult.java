package me.krunsh.kgui.menu.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete result of a side-effect-free compilation pass. */
public final class MenuCompilationResult {
    private final Map<String, CompiledMenu> menus;
    private final Map<String, CompiledMenu> templates;
    private final List<MenuDiagnostic> diagnostics;

    MenuCompilationResult(Map<String, CompiledMenu> menus,
                          Map<String, CompiledMenu> templates,
                          List<MenuDiagnostic> diagnostics) {
        this.menus = Collections.unmodifiableMap(new LinkedHashMap<>(menus));
        this.templates = Collections.unmodifiableMap(new LinkedHashMap<>(templates));
        Map<String, MenuDiagnostic> uniqueDiagnostics = new LinkedHashMap<>();
        for (MenuDiagnostic diagnostic : diagnostics) {
            uniqueDiagnostics.put(diagnostic.format(), diagnostic);
        }
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(uniqueDiagnostics.values()));
    }

    public Map<String, CompiledMenu> getMenus() { return menus; }
    public Map<String, CompiledMenu> getTemplates() { return templates; }
    public List<MenuDiagnostic> getDiagnostics() { return diagnostics; }

    public boolean isSuccess() {
        for (MenuDiagnostic diagnostic : diagnostics) {
            if (diagnostic.isError()) return false;
        }
        return true;
    }

    public int getErrorCount() {
        int count = 0;
        for (MenuDiagnostic diagnostic : diagnostics) if (diagnostic.isError()) count++;
        return count;
    }

    public int getWarningCount() {
        return diagnostics.size() - getErrorCount();
    }
}
