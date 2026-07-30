import org.junit.Test;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.typedast.TypedAst;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AstOptimizerTest {
    @Test
    public void foldsConstantArithmetic() {
        TypedAst.Expr expression = new TypedAst.Mul(TypedAst.Type.INT,
                new TypedAst.Add(TypedAst.Type.INT, num(2), num(3), 1), num(4), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue(optimized instanceof TypedAst.IntLiteral);
        assertEquals(20, ((TypedAst.IntLiteral) optimized).getValue());
    }

    @Test
    public void simplifiesAlgebraicIdentity() {
        TypedAst.Id x = id("x", TypedAst.Type.INT);
        TypedAst.Expr expression = new TypedAst.Add(TypedAst.Type.INT,
                new TypedAst.Mul(TypedAst.Type.INT, x, num(1), 1), num(0), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue(optimized instanceof TypedAst.Id);
        assertEquals("x", ((TypedAst.Id) optimized).getName());
    }

    @Test
    public void doesNotDiscardMethodCallInArithmeticIdentity() {
        TypedAst.Expr expression = new TypedAst.Mul(TypedAst.Type.INT,
                call("side", TypedAst.Type.INT), num(0), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue(optimized instanceof TypedAst.Mul);
        assertTrue(((TypedAst.Mul) optimized).getLeft() instanceof TypedAst.Call);
    }

    @Test
    public void doesNotApplyZeroMultiplicationToFloatingPoint() {
        TypedAst.Expr expression = new TypedAst.Mul(TypedAst.Type.FLOAT,
                num(0.0f), id("f", TypedAst.Type.FLOAT), 1);
        assertTrue(optimizeAssignExpression(expression) instanceof TypedAst.Mul);
    }

    @Test
    public void doesNotDiscardIntegerDivisionThatMayTrap() {
        TypedAst.Expr division = new TypedAst.Div(TypedAst.Type.INT, num(1), num(0), 1);
        TypedAst.Expr expression = new TypedAst.Mul(TypedAst.Type.INT, num(0), division, 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue(optimized instanceof TypedAst.Mul);
        assertTrue(((TypedAst.Mul) optimized).getRight() instanceof TypedAst.Div);
    }

    @Test
    public void doesNotSimplifyZeroDividedByVariable() {
        TypedAst.Expr expression = new TypedAst.Div(TypedAst.Type.INT,
                num(0), id("divisor", TypedAst.Type.INT), 1);
        assertTrue(optimizeAssignExpression(expression) instanceof TypedAst.Div);
    }

    @Test
    public void zeroMultiplicationStillSimplifiesPureIntegers() {
        TypedAst.Expr expression = new TypedAst.Mul(TypedAst.Type.INT,
                num(0), id("x", TypedAst.Type.INT), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue(optimized instanceof TypedAst.IntLiteral);
        assertEquals(0, ((TypedAst.IntLiteral) optimized).getValue());
    }

    @Test
    public void identityDoesNotChangePromotedResultType() {
        TypedAst.Expr expression = new TypedAst.Mul(TypedAst.Type.FLOAT,
                num(1.0f), id("x", TypedAst.Type.INT), 1);
        assertTrue(optimizeAssignExpression(expression) instanceof TypedAst.Mul);
    }

    @Test
    public void doesNotDiscardMethodCallInBooleanIdentity() {
        TypedAst.Expr expression = new TypedAst.Or(call("side", TypedAst.Type.BOOL),
                new TypedAst.BoolLiteral(true, 1), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue(optimized instanceof TypedAst.Or);
        assertTrue(((TypedAst.Or) optimized).getLeft() instanceof TypedAst.Call);
    }

    @Test
    public void foldsFloatLiteralInDoubleContextAtDoublePrecision() {
        // 小数字面量类型是 float，但携带的是十进制常量。在 double 运算里必须按十进制原文
        // 取 double 精度值，否则折叠出的常量与"没被折叠时"算出的结果不同。
        TypedAst.Expr expression = new TypedAst.Add(TypedAst.Type.DOUBLE,
                new TypedAst.DoubleLiteral(0.0d, "0.0d", 1),
                new TypedAst.FloatLiteral(0.1f, "0.1", 1), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);

        assertTrue(optimized instanceof TypedAst.DoubleLiteral);
        assertEquals(0.1d, ((TypedAst.DoubleLiteral) optimized).getValue(), 0.0d);
    }

    @Test
    public void foldingKeepsAllDigitsOfAWideDecimalLiteral() {
        TypedAst.Expr expression = new TypedAst.Add(TypedAst.Type.DOUBLE,
                new TypedAst.DoubleLiteral(0.0d, "0.0d", 1),
                new TypedAst.FloatLiteral((float) 3.14159265358979d, "3.14159265358979", 1), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);

        assertEquals(3.14159265358979d,
                ((TypedAst.DoubleLiteral) optimized).getValue(), 0.0d);
    }

    @Test
    public void foldsFloatLiteralInFloatContextAtFloatPrecision() {
        // 反向守卫：float 运算里仍必须按 float 取值。
        TypedAst.Expr expression = new TypedAst.Add(TypedAst.Type.FLOAT,
                new TypedAst.FloatLiteral(0.1f, "0.1", 1),
                new TypedAst.FloatLiteral(0.2f, "0.2", 1), 1);
        TypedAst.Expr optimized = optimizeAssignExpression(expression);

        assertTrue(optimized instanceof TypedAst.FloatLiteral);
        assertEquals(0.1f + 0.2f, ((TypedAst.FloatLiteral) optimized).getValue(), 0.0f);
    }

    @Test
    public void foldsMixedIntFloatComparisonAtFloatPrecision() {
        // 提升类型是 float，16777217 舍入到 16777216f，所以结果必须是 false。
        TypedAst.Expr expression = new TypedAst.GT(num(16777217), num(16777216.0f), 1);
        assertFoldsToBool(false, expression);
    }

    @Test
    public void foldsMixedIntFloatEqualityAtFloatPrecision() {
        TypedAst.Expr expression = new TypedAst.EQ(num(16777217), num(16777216.0f), 1);
        assertFoldsToBool(true, expression);
    }

    @Test
    public void foldsMixedIntDoubleComparisonAtDoublePrecision() {
        // 提升类型是 double，两个值互不相等，所以结果必须是 true。
        TypedAst.Expr expression = new TypedAst.GT(num(16777217), num(16777216.0d), 1);
        assertFoldsToBool(true, expression);
    }

    @Test
    public void foldsIntegerComparisonWithoutPromotion() {
        assertFoldsToBool(true, new TypedAst.LT(num(1), num(2), 1));
    }

    @Test
    public void foldsConstantBooleanCondition() {
        TypedAst.Symbol x = symbol("x", TypedAst.Type.INT);
        TypedAst.Stmt statement = new TypedAst.If(new TypedAst.LT(num(1), num(2), 1),
                new TypedAst.Assign(x, num(7), 1), new TypedAst.Assign(x, num(9), 1), 1);
        TypedAst.Stmt optimized = optimizeStatement(statement);
        assertTrue(optimized instanceof TypedAst.Assign);
        assertEquals(7, ((TypedAst.IntLiteral)
                ((TypedAst.Assign) optimized).getExpression()).getValue());
    }

    private void assertFoldsToBool(boolean expected, TypedAst.Expr expression) {
        TypedAst.Expr optimized = optimizeAssignExpression(expression);
        assertTrue("expected a folded bool literal, got " + optimized.getClass().getSimpleName(),
                optimized instanceof TypedAst.BoolLiteral);
        assertEquals(expected, ((TypedAst.BoolLiteral) optimized).getValue());
    }

    private TypedAst.Expr optimizeAssignExpression(TypedAst.Expr expression) {
        TypedAst.Symbol x = symbol("target", expression.getType());
        return ((TypedAst.Assign) optimizeStatement(new TypedAst.Assign(x, expression, 1)))
                .getExpression();
    }

    private TypedAst.Stmt optimizeStatement(TypedAst.Stmt statement) {
        TypedAst.MethodSymbol main = new TypedAst.MethodSymbol("main", TypedAst.Type.VOID,
                Collections.<TypedAst.Type>emptyList(), 1);
        TypedAst.Method method = new TypedAst.Method(main,
                Collections.<TypedAst.Declaration>emptyList(),
                Collections.<TypedAst.Declaration>emptyList(),
                Collections.singletonList(statement), 1);
        TypedAst.Program program = new TypedAst.Program("Test", Collections.singletonList(method));
        return new AstOptimizer().optimize(program).getMethods().get(0).getStatements().get(0);
    }

    private TypedAst.IntLiteral num(int value) {
        return new TypedAst.IntLiteral(value, null, 1);
    }

    private TypedAst.FloatLiteral num(float value) {
        return new TypedAst.FloatLiteral(value, null, 1);
    }

    private TypedAst.DoubleLiteral num(double value) {
        return new TypedAst.DoubleLiteral(value, null, 1);
    }

    private TypedAst.Id id(String name, TypedAst.Type type) {
        TypedAst.Symbol symbol = symbol(name, type);
        return new TypedAst.Id(symbol, type, 1);
    }

    private TypedAst.Symbol symbol(String name, TypedAst.Type type) {
        return new TypedAst.Symbol(name, type, TypedAst.Symbol.Kind.LOCAL, 1);
    }

    private TypedAst.Call call(String name, TypedAst.Type returnType) {
        TypedAst.MethodSymbol method = new TypedAst.MethodSymbol(name, returnType,
                new ArrayList<TypedAst.Type>(), 1);
        return new TypedAst.Call(method, new ArrayList<TypedAst.Expr>(), returnType, 1);
    }
}
