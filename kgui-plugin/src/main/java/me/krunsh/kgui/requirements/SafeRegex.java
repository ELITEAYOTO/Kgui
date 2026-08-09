package me.krunsh.kgui.requirements;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Sous-ensemble de regex borne pour eviter les expressions pathologiques. */
public final class SafeRegex {
    private static final int MAX_PATTERN = 256;
    private static final int MAX_INPUT = 2048;

    private SafeRegex() {
    }

    public static boolean matches(String input, String expression) {
        if (input == null || expression == null || input.length() > MAX_INPUT
                || !isValidPattern(expression)) return false;
        try {
            return Pattern.compile(expression).matcher(input).matches();
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    public static boolean isValidPattern(String expression) {
        if (expression == null || expression.isEmpty() || expression.length() > MAX_PATTERN
                || hasRiskyConstruct(expression)) return false;
        try {
            Pattern.compile(expression);
            return true;
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    static boolean hasRiskyConstruct(String expression) {
        if (expression.contains("(?<") || expression.contains("(?=") || expression.contains("(?!")
                || expression.matches(".*\\\\[1-9].*")) return true;
        // Rejette les groupes quantifies contenant eux-memes un quantificateur.
        return expression.matches(".*\\([^)]*[+*][^)]*\\)[+*?{].*")
            || expression.contains("++") || expression.contains("**") || expression.contains("+*")
            || expression.contains("*+") || expression.contains("}{");
    }
}
