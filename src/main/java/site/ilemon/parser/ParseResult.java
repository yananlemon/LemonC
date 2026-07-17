package site.ilemon.parser;

import site.ilemon.ast.Ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParseResult {
    private final Ast.Program.Base program;
    private final List<ParseDiagnostic> diagnostics;

    public ParseResult(Ast.Program.Base program, List<ParseDiagnostic> diagnostics) {
        this.program = program;
        this.diagnostics = Collections.unmodifiableList(
                new ArrayList<ParseDiagnostic>(diagnostics));
    }

    public Ast.Program.Base getProgram() {
        return program;
    }

    public List<ParseDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public boolean hasErrors() {
        return !diagnostics.isEmpty();
    }
}
