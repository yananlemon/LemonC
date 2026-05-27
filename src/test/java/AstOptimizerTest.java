import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.optimizer.AstOptimizer;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AstOptimizerTest {

    @Test
    public void foldsConstantArithmetic() {
        Ast.Expr.T expr = new Ast.Expr.Mul(
                new Ast.Expr.Add(num(2), num(3), 1),
                num(4),
                1);

        Ast.Expr.T optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Number);
        assertEquals(20, ((Ast.Expr.Number) optimized).getValue());
    }

    @Test
    public void simplifiesAlgebraicIdentity() {
        Ast.Expr.Id x = new Ast.Expr.Id("x", new Ast.Type.Int(), 1);
        Ast.Expr.T expr = new Ast.Expr.Add(
                new Ast.Expr.Mul(x, num(1), 1),
                num(0),
                1);

        Ast.Expr.T optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Id);
        assertEquals("x", ((Ast.Expr.Id) optimized).getId());
    }

    @Test
    public void foldsConstantBooleanCondition() {
        Ast.Stmt.If ifStmt = new Ast.Stmt.If(
                new Ast.Expr.LT(num(1), num(2), 1),
                new Ast.Stmt.Assign(new Ast.Expr.Id("x", 1), num(7), 1),
                new Ast.Stmt.Assign(new Ast.Expr.Id("x", 1), num(9), 1),
                1);

        Ast.Stmt.T optimized = optimizeStmt(ifStmt);

        assertTrue(optimized instanceof Ast.Stmt.Assign);
        assertEquals(7, ((Ast.Expr.Number) ((Ast.Stmt.Assign) optimized).getExpr()).getValue());
    }

    private Ast.Expr.T optimizeAssignExpr(Ast.Expr.T expr) {
        Ast.Stmt.Assign optimized = (Ast.Stmt.Assign) optimizeStmt(
                new Ast.Stmt.Assign(new Ast.Expr.Id("x", 1), expr, 1));
        return optimized.getExpr();
    }

    private Ast.Stmt.T optimizeStmt(Ast.Stmt.T stmt) {
        ArrayList<Ast.Stmt.T> statements = new ArrayList<Ast.Stmt.T>();
        statements.add(stmt);
        Ast.Method.MethodSingle method = new Ast.Method.MethodSingle(new Ast.Type.Void(), "main",
                new ArrayList<Ast.Declare.T>(), new ArrayList<Ast.Declare.T>(),
                statements, null, 1);
        ArrayList<Ast.Method.T> methods = new ArrayList<Ast.Method.T>();
        methods.add(method);
        Ast.Program.T program = new Ast.Program.ProgramSingle(
                new Ast.MainClass.MainClassSingle("Test", new ArrayList<Ast.Declare.T>(), methods));
        Ast.Program.ProgramSingle optimizedProgram =
                (Ast.Program.ProgramSingle) new AstOptimizer().optimize(program);
        Ast.MainClass.MainClassSingle mainClass =
                (Ast.MainClass.MainClassSingle) optimizedProgram.getMainClass();
        Ast.Method.MethodSingle optimizedMethod =
                (Ast.Method.MethodSingle) mainClass.getMethods().get(0);
        return optimizedMethod.getStms().get(0);
    }

    private Ast.Expr.Number num(int value) {
        return new Ast.Expr.Number(new Ast.Type.Int(), value, 1);
    }
}
