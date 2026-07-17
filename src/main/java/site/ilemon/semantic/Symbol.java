package site.ilemon.semantic;

import site.ilemon.ast.Ast;

public class Symbol {
    public enum Kind {
        METHOD,
        PARAMETER,
        LOCAL
    }

    private final String name;
    private final Ast.Type.Base type;
    private final Kind kind;
    private final int lineNumber;

    public Symbol(String name, Ast.Type.Base type, Kind kind, int lineNumber) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public Ast.Type.Base getType() {
        return type;
    }

    public Kind getKind() {
        return kind;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
