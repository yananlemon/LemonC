package site.ilemon.lexer;

public class Token {

    private final TokenKind kind;
    private final String lexeme;
    private final int lineNumber;
    private final int columnNumber;

    public Token(TokenKind kind, String lexeme, int lineNumber, int columnNumber) {
        this.kind = kind;
        this.lexeme = lexeme;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public TokenKind getKind() {
        return kind;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public String toString() {
        return "<lexeme=" + lexeme
                + ", lineNumber=" + lineNumber
                + ", columnNumber=" + columnNumber
                + ", kind=" + kind + ">\n";
    }
}
