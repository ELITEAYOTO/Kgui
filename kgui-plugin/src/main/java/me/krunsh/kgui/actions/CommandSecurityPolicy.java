package me.krunsh.kgui.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validation stricte des commandes configurees avant et apres placeholders. */
public final class CommandSecurityPolicy {
    private static final int MAX_COMMAND_LENGTH = 512;
    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%]+)%");
    private static final Set<String> SAFE_CONSOLE_PLACEHOLDERS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("player", "player_name")));
    private static final Set<String> DENIED_CONSOLE_ROOTS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "op", "deop", "stop", "restart", "reload", "rl", "bukkit:reload",
            "minecraft:op", "minecraft:deop", "minecraft:stop", "minecraft:reload",
            "plugman", "plugmanx", "permissions", "pex", "lp", "luckperms")));

    private CommandSecurityPolicy() {
    }

    public static boolean isSafePlayerTemplate(String template) {
        return structurallySafe(template);
    }

    public static boolean isSafeConsoleTemplate(String template, ActionOrigin origin) {
        if (origin == null || !origin.allowsPrivilegedCommands() || !structurallySafe(template)) return false;
        String lower = template.toLowerCase(Locale.ROOT);
        if (lower.contains("{kgui_data_") || lower.contains("{input") || lower.contains("%player_displayname%")) {
            return false;
        }
        Matcher matcher = PLACEHOLDER.matcher(lower);
        while (matcher.find()) {
            if (!SAFE_CONSOLE_PLACEHOLDERS.contains(matcher.group(1))) return false;
        }
        return !DENIED_CONSOLE_ROOTS.contains(commandRoot(template));
    }

    public static boolean isSafeResolvedCommand(String command, boolean privileged) {
        if (!structurallySafe(command)) return false;
        if (command.indexOf('%') >= 0 || command.indexOf('{') >= 0 || command.indexOf('}') >= 0) return false;
        return !privileged || !DENIED_CONSOLE_ROOTS.contains(commandRoot(command));
    }

    static String commandRoot(String command) {
        if (command == null) return "";
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) normalized = normalized.substring(1).trim();
        int space = normalized.indexOf(' ');
        return space < 0 ? normalized : normalized.substring(0, space);
    }

    private static boolean structurallySafe(String command) {
        if (command == null) return false;
        String value = command.trim();
        if (value.isEmpty() || value.length() > MAX_COMMAND_LENGTH) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) || c == ';' || c == '`') return false;
        }
        return !value.contains("&&") && !value.contains("||");
    }
}
