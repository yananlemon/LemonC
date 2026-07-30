package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.typedast.TypedAst;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/** Result of the source-AST to Typed-AST semantic boundary. */
public final class SemanticResult {
    private final TypedAst.Program program;
    private final ArrayList<String> diagnostics;
    private final ArrayList<Integer> diagnosticLines;
    private final IdentityHashMap<Ast.Expr.Base, TypedAst.Expr> expressions;
    private final IdentityHashMap<Ast.Stmt.Base, TypedAst.Stmt> statements;

    SemanticResult(TypedAst.Program program, List<String> diagnostics, List<Integer> diagnosticLines,
                   IdentityHashMap<Ast.Expr.Base, TypedAst.Expr> expressions,
                   IdentityHashMap<Ast.Stmt.Base, TypedAst.Stmt> statements) {
        this.program = Objects.requireNonNull(program, "program");
        this.diagnostics = new ArrayList<String>(
                Objects.requireNonNull(diagnostics, "diagnostics"));
        this.diagnosticLines = new ArrayList<Integer>(
                Objects.requireNonNull(diagnosticLines, "diagnosticLines"));
        this.expressions = new IdentityHashMap<Ast.Expr.Base, TypedAst.Expr>(
                Objects.requireNonNull(expressions, "expressions"));
        this.statements = new IdentityHashMap<Ast.Stmt.Base, TypedAst.Stmt>(
                Objects.requireNonNull(statements, "statements"));
    }

    public boolean isSuccess() {
        return diagnostics.isEmpty();
    }

    public TypedAst.Program getProgram() {
        return program;
    }

    public ArrayList<String> getDiagnostics() {
        return new ArrayList<String>(diagnostics);
    }

    public ArrayList<Integer> getDiagnosticLines() {
        return new ArrayList<Integer>(diagnosticLines);
    }

    public TypedAst.Expr getTypedExpression(Ast.Expr.Base source) {
        return expressions.get(source);
    }

    public TypedAst.Stmt getTypedStatement(Ast.Stmt.Base source) {
        return statements.get(source);
    }

    public TypedAst.Type getExpressionType(Ast.Expr.Base source) {
        TypedAst.Expr expression = expressions.get(source);
        return expression == null ? TypedAst.Type.ERROR : expression.getType();
    }
}
