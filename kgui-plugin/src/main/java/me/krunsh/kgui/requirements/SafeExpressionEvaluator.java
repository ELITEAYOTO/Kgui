package me.krunsh.kgui.requirements;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluateur sans moteur de script. Il accepte un booleen ou une comparaison
 * unique et bornee. Toute expression ambigue ou mal formee vaut false.
 */
public final class SafeExpressionEvaluator {
    private static final int MAX_LENGTH = 512;
    private static final Pattern WORD_OPERATOR = Pattern.compile(
        "^(.{1,240}?)\\s+(equals|not_equals|contains|starts_with|ends_with)\\s+(.{1,240})$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_OPERATOR = Pattern.compile(
        "^\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*(>=|<=|==|!=|>|<)\\s*" +
        "([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*$");

    public boolean evaluate(String expression) {
        return Boolean.TRUE.equals(evaluateResult(expression));
    }

    public Boolean evaluateResult(String expression) {
        if (expression == null) return null;
        String value = expression.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH || containsUnsafeToken(value)) return null;
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) return true;
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value)) return false;

        Matcher numeric = NUMERIC_OPERATOR.matcher(value);
        if (numeric.matches()) {
            double left;
            double right;
            try {
                left = Double.parseDouble(numeric.group(1));
                right = Double.parseDouble(numeric.group(3));
            } catch (NumberFormatException ignored) {
                return null;
            }
            if (!Double.isFinite(left) || !Double.isFinite(right)) return null;
            switch (numeric.group(2)) {
                case ">=": return left >= right;
                case "<=": return left <= right;
                case ">": return left > right;
                case "<": return left < right;
                case "==": return Double.compare(left, right) == 0;
                case "!=": return Double.compare(left, right) != 0;
                default: return null;
            }
        }

        Matcher word = WORD_OPERATOR.matcher(value);
        if (!word.matches()) return null;
        String left = unquote(word.group(1).trim());
        String right = unquote(word.group(3).trim());
        if (left == null || right == null) return null;
        String operator = word.group(2).toLowerCase(Locale.ROOT);
        switch (operator) {
            case "equals": return left.equalsIgnoreCase(right);
            case "not_equals": return !left.equalsIgnoreCase(right);
            case "contains": return left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
            case "starts_with": return left.toLowerCase(Locale.ROOT).startsWith(right.toLowerCase(Locale.ROOT));
            case "ends_with": return left.toLowerCase(Locale.ROOT).endsWith(right.toLowerCase(Locale.ROOT));
            default: return null;
        }
    }

    private static boolean containsUnsafeToken(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("&&") || lower.contains("||") || lower.contains(";")
            || lower.contains("javascript:") || lower.contains("class.") || lower.contains("runtime")
            || lower.contains("reflect") || lower.indexOf('\n') >= 0 || lower.indexOf('\r') >= 0;
    }

    private static String unquote(String value) {
        if (value.isEmpty()) return null;
        boolean starts = value.startsWith("\"") || value.startsWith("'");
        boolean ends = value.endsWith("\"") || value.endsWith("'");
        if (starts != ends) return null;
        if (!starts) return value;
        if (value.length() < 2 || value.charAt(0) != value.charAt(value.length() - 1)) return null;
        return value.substring(1, value.length() - 1);
    }
}
