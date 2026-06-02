package site.ilemon.lexer;

public class Token {

    public String lexeme;

    public int lineNumber;

    public int columnNumber;

    public TokenKind kind;

    public Token(String lexeme, int lineNumber, TokenKind kind) {
        this.lexeme = lexeme;
        this.lineNumber = lineNumber;
        this.columnNumber = 1;
        this.kind = kind;
    }

    public Token(TokenKind kind, String lexeme) {
        this.lexeme = lexeme;
        this.kind = kind;
        this.columnNumber = 1;
    }

    public Token(TokenKind kind, String lexeme, int lineNumber) {
        this.lexeme = lexeme;
        this.kind = kind;
        this.lineNumber = lineNumber;
        this.columnNumber = 1;
    }

    public Token(TokenKind kind, String lexeme, int lineNumber, int columnNumber) {
        this.lexeme = lexeme;
        this.kind = kind;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public Token(TokenKind kind) {
        this.kind = kind;
        this.columnNumber = 1;
    }

    @Override
    public String toString() {
        return "<lexeme=" + lexeme
                + ", lineNumber=" + lineNumber
                + ", columnNumber=" + columnNumber
                + ", kind=" + kind + ">\n";
    }
}
