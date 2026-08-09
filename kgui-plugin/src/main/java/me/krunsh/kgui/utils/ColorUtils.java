package me.krunsh.kgui.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilitaires pour la gestion des couleurs
 */
public final class ColorUtils {

    // Pattern HEX pour support futur (1.16+)
    @SuppressWarnings("unused")
    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    
    private ColorUtils() {
        // Utility class
    }

    /**
     * Colorise une chaîne avec les codes couleur Minecraft
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Colorise une liste de chaînes
     */
    public static List<String> colorize(List<String> texts) {
        if (texts == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>();
        for (String text : texts) {
            colored.add(colorize(text));
        }
        return colored;
    }

    /**
     * Supprime les codes couleur d'une chaîne
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }

    /**
     * Centre un texte pour le chat Minecraft (approximatif)
     */
    public static String center(String text) {
        if (text == null || text.isEmpty()) return "";
        
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        
        for (char c : text.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }
        
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        
        return sb + text;
    }

    /**
     * Enum pour la taille des caractères (pour le centrage)
     */
    public enum DefaultFontInfo {
        A('A', 5), B('B', 5), C('C', 5), D('D', 5), E('E', 5), F('F', 5), G('G', 5), 
        H('H', 5), I('I', 3), J('J', 5), K('K', 5), L('L', 5), M('M', 5), N('N', 5), 
        O('O', 5), P('P', 5), Q('Q', 5), R('R', 5), S('S', 5), T('T', 5), U('U', 5), 
        V('V', 5), W('W', 5), X('X', 5), Y('Y', 5), Z('Z', 5),
        a('a', 5), b('b', 5), c('c', 5), d('d', 5), e('e', 5), f('f', 4), g('g', 5), 
        h('h', 5), i('i', 1), j('j', 5), k('k', 4), l('l', 1), m('m', 5), n('n', 5), 
        o('o', 5), p('p', 5), q('q', 5), r('r', 5), s('s', 5), t('t', 4), u('u', 5), 
        v('v', 5), w('w', 5), x('x', 5), y('y', 5), z('z', 5),
        NUM_1('1', 5), NUM_2('2', 5), NUM_3('3', 5), NUM_4('4', 5), NUM_5('5', 5), 
        NUM_6('6', 5), NUM_7('7', 5), NUM_8('8', 5), NUM_9('9', 5), NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1), AT_SYMBOL('@', 6), POUND('#', 5), DOLLAR('$', 5), 
        PERCENT('%', 5), UP_ARROW('^', 5), AMPERSAND('&', 5), ASTERISK('*', 5), 
        LEFT_PAREN('(', 4), RIGHT_PAREN(')', 4), MINUS('-', 5), UNDERSCORE('_', 5), 
        PLUS('+', 5), EQUALS('=', 5), LEFT_CURL('{', 4), RIGHT_CURL('}', 4), 
        LEFT_BRACKET('[', 3), RIGHT_BRACKET(']', 3), COLON(':', 1), SEMI_COLON(';', 1), 
        DOUBLE_QUOTE('"', 3), SINGLE_QUOTE('\'', 1), LEFT_ANGLE('<', 4), RIGHT_ANGLE('>', 4), 
        QUESTION('?', 5), SLASH('/', 5), BACK_SLASH('\\', 5), PIPE('|', 1), 
        TILDE('~', 5), TICK('`', 2), PERIOD('.', 1), COMMA(',', 1), SPACE(' ', 3),
        DEFAULT('a', 4);

        private final char character;
        private final int length;

        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        public char getCharacter() {
            return character;
        }

        public int getLength() {
            return length;
        }

        public int getBoldLength() {
            if (this == SPACE) return length;
            return length + 1;
        }

        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo info : values()) {
                if (info.getCharacter() == c) return info;
            }
            return DEFAULT;
        }
    }
}
