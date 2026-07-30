package site.ilemon.lexer;

import site.ilemon.source.SourceSpan;

import java.util.Objects;

public class Token {

    private final TokenKind kind;
    private final String lexeme;
    private final SourceSpan sourceSpan;

    public Token(TokenKind kind, String lexeme, int lineNumber, int columnNumber) {
        this(kind, lexeme, inferredSpan(lexeme, lineNumber, columnNumber));
    }

    public Token(TokenKind kind, String lexeme, SourceSpan sourceSpan) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.lexeme = Objects.requireNonNull(lexeme, "lexeme");
        this.sourceSpan = Objects.requireNonNull(sourceSpan, "sourceSpan");
    }

    public TokenKind getKind() {
        return kind;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLineNumber() {
        return sourceSpan.getStartLine();
    }

    public int getColumnNumber() {
        return sourceSpan.getStartColumn();
    }

    public int getEndLineNumber() {
        return sourceSpan.getEndLine();
    }

    public int getEndColumnNumber() {
        return sourceSpan.getEndColumn();
    }

    public SourceSpan getSourceSpan() {
        return sourceSpan;
    }

    @Override
    public String toString() {
        return "<lexeme=" + lexeme
                + ", lineNumber=" + getLineNumber()
                + ", columnNumber=" + getColumnNumber()
                + ", endLineNumber=" + getEndLineNumber()
                + ", endColumnNumber=" + getEndColumnNumber()
                + ", kind=" + kind + ">\n";
    }

    private static SourceSpan inferredSpan(String lexeme, int lineNumber, int columnNumber) {
        int endLine = lineNumber;
        int endColumn = columnNumber;
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c == '\n') {
                endLine++;
                endColumn = 1;
            } else {
                endColumn++;
            }
        }
        return SourceSpan.of(lineNumber, columnNumber, endLine, endColumn);
    }
}
