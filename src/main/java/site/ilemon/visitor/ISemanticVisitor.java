package site.ilemon.visitor;

import site.ilemon.ast.Ast;

/**
 * Legacy visitor for traversing the syntax-only source AST.
 *
 * <p>The historical name is retained for compatibility with the direct JVM
 * translator. Semantic analysis itself transforms source nodes into a
 * separate Typed-AST and does not implement this interface.</p>
 */
public interface ISemanticVisitor {

    // --- Base class dispatchers (default null-safe delegates) ---
    default void visit(Ast.Expr.Base obj) { if (obj != null) obj.accept(this); }
    default void visit(Ast.Stmt.Base obj) { if (obj != null) obj.accept(this); }
    default void visit(Ast.Type.Base obj) { if (obj != null) obj.accept(this); }
    default void visit(Ast.Declare.Base obj) { if (obj != null) obj.accept(this); }
    default void visit(Ast.MainClass.Base obj) { if (obj != null) obj.accept(this); }
    default void visit(Ast.Program.Base obj) { if (obj != null) obj.accept(this); }
    default void visit(Ast.Method.MethodSingle obj) { if (obj != null) obj.accept(this); }

    // --- Expr Leaves ---
    void visit(Ast.Expr.Add obj);
    void visit(Ast.Expr.And obj);
    void visit(Ast.Expr.Call obj);
    void visit(Ast.Expr.GT obj);
    void visit(Ast.Expr.LT obj);
    void visit(Ast.Expr.LTE obj);
    void visit(Ast.Expr.GTE obj);
    void visit(Ast.Expr.EQ obj);
    void visit(Ast.Expr.NEQ obj);
    void visit(Ast.Expr.Id obj);
    void visit(Ast.Expr.Div obj);
    void visit(Ast.Expr.Mod obj);
    void visit(Ast.Expr.Mul obj);
    void visit(Ast.Expr.IntLiteral obj);
    void visit(Ast.Expr.FloatLiteral obj);
    void visit(Ast.Expr.DoubleLiteral obj);
    void visit(Ast.Expr.Sub obj);
    void visit(Ast.Expr.Or obj);
    void visit(Ast.Expr.True obj);
    void visit(Ast.Expr.False obj);
    void visit(Ast.Expr.Not obj);
    void visit(Ast.Expr.UnaryMinus obj);
    void visit(Ast.Expr.Str obj);
    void visit(Ast.Expr.ArrayAccess obj);
    void visit(Ast.Expr.ArrayLength obj);

    // --- Type Leaves (default empty) ---
    default void visit(Ast.Type.Bool obj) {}
    default void visit(Ast.Type.Float obj) {}
    default void visit(Ast.Type.Double obj) {}
    default void visit(Ast.Type.Str obj) {}
    default void visit(Ast.Type.Void obj) {}
    default void visit(Ast.Type.Int obj) {}
    default void visit(Ast.Type.IntArray obj) {}
    default void visit(Ast.Type.FloatArray obj) {}
    default void visit(Ast.Type.DoubleArray obj) {}
    default void visit(Ast.Type.BoolArray obj) {}

    // --- Stmt Leaves ---
    void visit(Ast.Stmt.If obj);
    void visit(Ast.Stmt.Assign obj);
    void visit(Ast.Stmt.VarDecl obj);
    void visit(Ast.Stmt.Block obj);
    void visit(Ast.Stmt.Printf obj);
    void visit(Ast.Stmt.PrintLine obj);
    void visit(Ast.Stmt.Return obj);
    void visit(Ast.Stmt.While obj);
    void visit(Ast.Stmt.For obj);
    void visit(Ast.Stmt.Break obj);
    void visit(Ast.Stmt.Continue obj);
    void visit(Ast.Stmt.Call obj);
    void visit(Ast.Stmt.ArrayAssign obj);

}
