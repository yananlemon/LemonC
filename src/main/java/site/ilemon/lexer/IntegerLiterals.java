package site.ilemon.lexer;

/**
 * Parses the integer literal spellings accepted by LemonC.
 */
public final class IntegerLiterals {

    private IntegerLiterals() {
    }

    public static int parse(String text) {
        if (text == null || text.isEmpty()) {
            throw new NumberFormatException("empty integer literal");
        }
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Integer.parseInt(text.substring(2), 16);
        }
        if (text.length() > 1 && text.charAt(0) == '0') {
            return Integer.parseInt(text.substring(1), 8);
        }
        return Integer.parseInt(text, 10);
    }
}
