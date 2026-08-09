package me.krunsh.kgui.menu.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Best-effort YAML path to source-line index used for actionable diagnostics. */
final class YamlSourceIndex {
    private final Map<String, Integer> lines = new HashMap<>();
    private final List<MenuDiagnostic> diagnostics = new ArrayList<>();

    YamlSourceIndex(String source, String content) {
        List<Node> stack = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String[] sourceLines = content.split("\\r?\\n", -1);
        for (int number = 1; number <= sourceLines.length; number++) {
            String line = sourceLines[number - 1];
            int indent = leadingSpaces(line);
            String trimmed = line.substring(Math.min(indent, line.length())).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("- ")) continue;
            if (line.substring(0, Math.min(indent, line.length())).indexOf('\t') >= 0) {
                diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, "YAML_TAB", source,
                    "", number, "Les tabulations ne sont pas autorisées pour l'indentation YAML."));
                continue;
            }
            int colon = keyColon(trimmed);
            if (colon <= 0) continue;
            String key = unquote(trimmed.substring(0, colon).trim());
            if (key.isEmpty()) continue;

            while (!stack.isEmpty() && stack.get(stack.size() - 1).indent >= indent) {
                stack.remove(stack.size() - 1);
            }
            String parent = stack.isEmpty() ? "" : stack.get(stack.size() - 1).path;
            String path = parent.isEmpty() ? key : parent + "." + key;
            if (!seen.add(path)) {
                diagnostics.add(new MenuDiagnostic(DiagnosticSeverity.ERROR, "DUPLICATE_KEY", source,
                    path, number, "Clé YAML dupliquée; la valeur précédente serait écrasée."));
            } else {
                lines.put(path, number);
            }
            stack.add(new Node(indent, path));
        }
    }

    List<MenuDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    int lineOf(String path) {
        String current = path == null ? "" : path;
        while (!current.isEmpty()) {
            Integer line = lines.get(current);
            if (line != null) return line;
            int separator = current.lastIndexOf('.');
            current = separator < 0 ? "" : current.substring(0, separator);
        }
        return 0;
    }

    private static int leadingSpaces(String line) {
        int index = 0;
        while (index < line.length()) {
            char character = line.charAt(index);
            if (character != ' ' && character != '\t') break;
            index++;
        }
        return index;
    }

    private static int keyColon(String value) {
        boolean single = false;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '\'' && !quoted) single = !single;
            if (character == '"' && !single && (i == 0 || value.charAt(i - 1) != '\\')) quoted = !quoted;
            if (character == ':' && !single && !quoted) return i;
        }
        return -1;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static final class Node {
        private final int indent;
        private final String path;

        private Node(int indent, String path) {
            this.indent = indent;
            this.path = path;
        }
    }
}
