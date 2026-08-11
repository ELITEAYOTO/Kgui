package me.krunsh.kgui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.krunsh.kgui.menu.compiler.MenuCompilationResult;
import me.krunsh.kgui.menu.compiler.MenuDiagnostic;

/** Outcome of a validation or atomic publication attempt. */
public final class MenuReloadResult {
    private final boolean success;
    private final boolean published;
    private final String target;
    private final int menuCount;
    private final List<MenuDiagnostic> diagnostics;

    private MenuReloadResult(boolean success, boolean published, String target, int menuCount,
                             List<MenuDiagnostic> diagnostics) {
        this.success = success;
        this.published = published;
        this.target = target;
        this.menuCount = menuCount;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public static MenuReloadResult validation(MenuCompilationResult result) {
        return new MenuReloadResult(result.isSuccess(), false, null, result.getMenus().size(), result.getDiagnostics());
    }

    public static MenuReloadResult publication(MenuCompilationResult result, boolean published, String target) {
        return new MenuReloadResult(result.isSuccess() && published, published, target,
            result.getMenus().size(), result.getDiagnostics());
    }

    public static MenuReloadResult failure(String target, int menuCount, List<MenuDiagnostic> diagnostics) {
        return new MenuReloadResult(false, false, target, menuCount, diagnostics);
    }

    public boolean isSuccess() { return success; }
    public boolean isPublished() { return published; }
    public String getTarget() { return target; }
    public int getMenuCount() { return menuCount; }
    public List<MenuDiagnostic> getDiagnostics() { return diagnostics; }

    public int getErrorCount() {
        int count = 0;
        for (MenuDiagnostic diagnostic : diagnostics) if (diagnostic.isError()) count++;
        return count;
    }

    public int getWarningCount() {
        return diagnostics.size() - getErrorCount();
    }
}
