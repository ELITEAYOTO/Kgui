package me.krunsh.kgui.menu.compiler;

import java.util.Objects;

/** A precise and immutable diagnostic emitted while compiling a menu. */
public final class MenuDiagnostic {
    private final DiagnosticSeverity severity;
    private final String code;
    private final String source;
    private final String path;
    private final int line;
    private final String message;

    public MenuDiagnostic(DiagnosticSeverity severity, String code, String source,
                          String path, int line, String message) {
        this.severity = Objects.requireNonNull(severity, "severity");
        this.code = Objects.requireNonNull(code, "code");
        this.source = source == null ? "<unknown>" : source;
        this.path = path == null ? "" : path;
        this.line = Math.max(0, line);
        this.message = Objects.requireNonNull(message, "message");
    }

    public DiagnosticSeverity getSeverity() { return severity; }
    public String getCode() { return code; }
    public String getSource() { return source; }
    public String getPath() { return path; }
    public int getLine() { return line; }
    public String getMessage() { return message; }
    public boolean isError() { return severity == DiagnosticSeverity.ERROR; }

    public String format() {
        StringBuilder output = new StringBuilder();
        output.append(severity).append(" [").append(code).append("] ").append(source);
        if (line > 0) output.append(':').append(line);
        if (!path.isEmpty()) output.append(" (").append(path).append(')');
        output.append(" - ").append(message);
        return output.toString();
    }

    @Override
    public String toString() {
        return format();
    }
}
