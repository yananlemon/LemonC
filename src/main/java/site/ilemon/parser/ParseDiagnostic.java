package site.ilemon.parser;

public final class ParseDiagnostic {
    private final int line;
    private final int column;
    private final String message;

    public ParseDiagnostic(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
