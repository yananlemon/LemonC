import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.optimizer.AstOptimizer;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AstOptimizerTest {

    @Test
    public void foldsConstantArithmetic() {
        Ast.Expr.Base expr = new Ast.Expr.Mul(
                new Ast.Expr.Add(num(2), num(3), 1),
                num(4),
                1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.IntLiteral);
        assertEquals(Integer.valueOf(20), ((Ast.Expr.IntLiteral) optimized).getValue());
    }

    @Test
    public void simplifiesAlgebraicIdentity() {
        Ast.Expr.Id x = new Ast.Expr.Id("x", new Ast.Type.Int(), 1);
        Ast.Expr.Base expr = new Ast.Expr.Add(
                new Ast.Expr.Mul(x, num(1), 1),
                num(0),
                1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Id);
        assertEquals("x", ((Ast.Expr.Id) optimized).getId());
    }

    @Test
    public void doesNotDiscardMethodCallInArithmeticIdentity() {
        Ast.Expr.Base expr = new Ast.Expr.Mul(call("side", new Ast.Type.Int()), num(0), 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Mul);
        assertTrue(((Ast.Expr.Mul) optimized).getLeft() instanceof Ast.Expr.Call);
    }

    @Test
    public void doesNotApplyZeroMultiplicationToFloatingPoint() {
        Ast.Expr.Id f = new Ast.Expr.Id("f", new Ast.Type.Float(), 1);
        Ast.Expr.Base expr = new Ast.Expr.Mul(num(0.0f), f, 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Mul);
    }

    @Test
    public void doesNotDiscardIntegerDivisionThatMayTrap() {
        Ast.Expr.Base division = new Ast.Expr.Div(num(1), num(0), 1);
        Ast.Expr.Base expr = new Ast.Expr.Mul(num(0), division, 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Mul);
        assertTrue(((Ast.Expr.Mul) optimized).getRight() instanceof Ast.Expr.Div);
    }

    @Test
    public void doesNotSimplifyZeroDividedByVariable() {
        Ast.Expr.Id divisor = new Ast.Expr.Id("divisor", new Ast.Type.Int(), 1);
        Ast.Expr.Base expr = new Ast.Expr.Div(num(0), divisor, 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Div);
    }

    @Test
    public void zeroMultiplicationStillSimplifiesPureIntegers() {
        Ast.Expr.Id x = new Ast.Expr.Id("x", new Ast.Type.Int(), 1);
        Ast.Expr.Base expr = new Ast.Expr.Mul(num(0), x, 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.IntLiteral);
        assertEquals(Integer.valueOf(0), ((Ast.Expr.IntLiteral) optimized).getValue());
    }

    @Test
    public void identityDoesNotChangePromotedResultType() {
        Ast.Expr.Id x = new Ast.Expr.Id("x", new Ast.Type.Int(), 1);
        Ast.Expr.Base expr = new Ast.Expr.Mul(num(1.0f), x, 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Mul);
    }

    @Test
    public void doesNotDiscardMethodCallInBooleanIdentity() {
        Ast.Expr.Base expr = new Ast.Expr.Or(call("side", new Ast.Type.Bool()), new Ast.Expr.True(1), 1);

        Ast.Expr.Base optimized = optimizeAssignExpr(expr);

        assertTrue(optimized instanceof Ast.Expr.Or);
        assertTrue(((Ast.Expr.Or) optimized).getLeft() instanceof Ast.Expr.Call);
    }

    @Test
    public void foldsConstantBooleanCondition() {
        Ast.Stmt.If ifStmt = new Ast.Stmt.If(
                new Ast.Expr.LT(num(1), num(2), 1),
                new Ast.Stmt.Assign(new Ast.Expr.Id("x", 1), num(7), 1),
                new Ast.Stmt.Assign(new Ast.Expr.Id("x", 1), num(9), 1),
                1);

        Ast.Stmt.Base optimized = optimizeStmt(ifStmt);

        assertTrue(optimized instanceof Ast.Stmt.Assign);
        assertEquals(Integer.valueOf(7), ((Ast.Expr.IntLiteral) ((Ast.Stmt.Assign) optimized).getExpr()).getValue());
    }

    private Ast.Expr.Base optimizeAssignExpr(Ast.Expr.Base expr) {
        Ast.Stmt.Assign optimized = (Ast.Stmt.Assign) optimizeStmt(
                new Ast.Stmt.Assign(new Ast.Expr.Id("x", 1), expr, 1));
        return optimized.getExpr();
    }

    private Ast.Stmt.Base optimizeStmt(Ast.Stmt.Base stmt) {
        ArrayList<Ast.Stmt.Base> statements = new ArrayList<Ast.Stmt.Base>();
        statements.add(stmt);
        Ast.Method.MethodSingle method = new Ast.Method.MethodSingle(new Ast.Type.Void(), "main",
                new ArrayList<Ast.Declare.Base>(), new ArrayList<Ast.Declare.Base>(),
                statements, null, 1);
        ArrayList<Ast.Method.Base> methods = new ArrayList<Ast.Method.Base>();
        methods.add(method);
        Ast.Program.Base program = new Ast.Program.ProgramSingle(
                new Ast.MainClass.MainClassSingle("Test", methods));
        Ast.Program.ProgramSingle optimizedProgram =
                (Ast.Program.ProgramSingle) new AstOptimizer().optimize(program);
        Ast.MainClass.MainClassSingle mainClass =
                (Ast.MainClass.MainClassSingle) optimizedProgram.getMainClass();
        Ast.Method.MethodSingle optimizedMethod =
                (Ast.Method.MethodSingle) mainClass.getMethods().get(0);
        return optimizedMethod.getStms().get(0);
    }

    private Ast.Expr.IntLiteral num(int value) {
        return new Ast.Expr.IntLiteral(value, 1);
    }

    private Ast.Expr.FloatLiteral num(float value) {
        return new Ast.Expr.FloatLiteral(value, 1);
    }

    private Ast.Expr.Call call(String name, Ast.Type.Base returnType) {
        return new Ast.Expr.Call(name, new ArrayList<Ast.Expr.Base>(), 1, returnType);
    }
}
