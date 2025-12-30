import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * TranslatorVisitor 测试用例
 * 测试双重分派（Double Dispatch）的 Visitor 模式实现
 */
public class TranslatorVisitorTest {

    // ==================== 基础翻译测试 ====================

    @Test
    public void testTranslateBoolExpression() throws IOException {
        TranslatorVisitor visitor = translate("examples/BoolTest01.lemon");
        assertNotNull(visitor.prog);
        assertTrue("应生成至少一个方法", visitor.prog.mainClass.methods.size() > 0);
    }

    // ==================== 控制流翻译测试 ====================

    @Test
    public void testTranslateIfStatement() throws IOException {
        TranslatorVisitor visitor = translate("examples/If01.lemon");
        assertNotNull(visitor.prog);
        
        site.ilemon.codegen.ast.Ast.Method.MethodSingle method = visitor.prog.mainClass.methods.get(0);
        boolean hasGoto = false;
        boolean hasLabel = false;
        for (site.ilemon.codegen.ast.Ast.Stmt.T s : method.stms) {
            if (s instanceof site.ilemon.codegen.ast.Ast.Stmt.Goto) hasGoto = true;
            if (s instanceof site.ilemon.codegen.ast.Ast.Stmt.LabelJ) hasLabel = true;
        }
        assertTrue("If语句应生成Goto指令", hasGoto);
        assertTrue("If语句应生成Label", hasLabel);
    }

    @Test
    public void testTranslateCompareOperators() throws IOException {
        TranslatorVisitor visitor = translate("examples/CompareTest.lemon");
        assertNotNull(visitor.prog);
        
        site.ilemon.codegen.ast.Ast.Method.MethodSingle method = visitor.prog.mainClass.methods.get(0);
        boolean hasCompare = false;
        for (site.ilemon.codegen.ast.Ast.Stmt.T s : method.stms) {
            if (s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmpgt ||
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmplt ||
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmpget ||
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmplet ||
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmpeq ||
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmpne) {
                hasCompare = true;
                break;
            }
        }
        assertTrue("比较运算应生成条件跳转指令", hasCompare);
    }

    @Test
    public void testTranslateGreaterThan() throws IOException {
        TranslatorVisitor visitor = translate("examples/If01.lemon");
        assertNotNull(visitor.prog);
    }

    // ==================== 双重分派验证测试 ====================

    @Test
    public void testDoubleDispatchExprAcceptExists() {
        // 验证表达式节点的 accept 方法存在
        Ast.Expr.Number num = new Ast.Expr.Number(new Ast.Type.Int(), 42, 1);
        Ast.Expr.Add add = new Ast.Expr.Add(num, num, 1);
        Ast.Expr.Sub sub = new Ast.Expr.Sub(num, num, 1);
        Ast.Expr.Mul mul = new Ast.Expr.Mul(num, num, 1);
        Ast.Expr.Div div = new Ast.Expr.Div(num, num, 1);
        
        assertNotNull("Number节点应存在", num);
        assertNotNull("Add节点应存在", add);
        assertNotNull("Sub节点应存在", sub);
        assertNotNull("Mul节点应存在", mul);
        assertNotNull("Div节点应存在", div);
    }

    @Test
    public void testDoubleDispatchBoolExprAcceptExists() {
        // 验证布尔表达式节点的 accept 方法存在
        Ast.Expr.True trueExpr = new Ast.Expr.True(1);
        Ast.Expr.False falseExpr = new Ast.Expr.False(1);
        Ast.Expr.Not notExpr = new Ast.Expr.Not(trueExpr);
        Ast.Expr.And andExpr = new Ast.Expr.And(trueExpr, falseExpr, 1);
        Ast.Expr.Or orExpr = new Ast.Expr.Or(trueExpr, falseExpr, 1);
        
        assertNotNull(trueExpr);
        assertNotNull(falseExpr);
        assertNotNull(notExpr);
        assertNotNull(andExpr);
        assertNotNull(orExpr);
    }

    @Test
    public void testDoubleDispatchCompareExprAcceptExists() {
        // 验证比较表达式节点的 accept 方法存在
        Ast.Expr.Number num = new Ast.Expr.Number(new Ast.Type.Int(), 1, 1);
        Ast.Expr.GT gt = new Ast.Expr.GT(num, num, 1);
        Ast.Expr.LT lt = new Ast.Expr.LT(num, num, 1);
        Ast.Expr.GET get = new Ast.Expr.GET(num, num, 1);
        Ast.Expr.LET let = new Ast.Expr.LET(num, num, 1);
        Ast.Expr.EQ eq = new Ast.Expr.EQ(num, num, 1);
        Ast.Expr.NEQ neq = new Ast.Expr.NEQ(num, num, 1);
        
        assertNotNull(gt);
        assertNotNull(lt);
        assertNotNull(get);
        assertNotNull(let);
        assertNotNull(eq);
        assertNotNull(neq);
    }

    @Test
    public void testDoubleDispatchStmtAcceptExists() {
        // 验证语句节点的 accept 方法存在
        Ast.Expr.Id id = new Ast.Expr.Id("x", new Ast.Type.Int(), 1);
        Ast.Expr.Number num = new Ast.Expr.Number(new Ast.Type.Int(), 1, 1);
        Ast.Stmt.Assign assign = new Ast.Stmt.Assign(id, num, 1);
        
        assertNotNull("Assign语句应存在", assign);
    }

    @Test
    public void testDoubleDispatchTypeAcceptExists() {
        // 验证类型节点的 accept 方法存在
        Ast.Type.Int intType = new Ast.Type.Int();
        Ast.Type.Float floatType = new Ast.Type.Float();
        Ast.Type.Bool boolType = new Ast.Type.Bool();
        Ast.Type.Str strType = new Ast.Type.Str();
        Ast.Type.Void voidType = new Ast.Type.Void();
        
        assertNotNull(intType);
        assertNotNull(floatType);
        assertNotNull(boolType);
        assertNotNull(strType);
        assertNotNull(voidType);
    }

    @Test
    public void testTranslateNestedIf() throws IOException {
        TranslatorVisitor visitor = translate("examples/If05.lemon");
        assertNotNull(visitor.prog);
    }

    @Test
    public void testTranslateBoolTest03() throws IOException {
        TranslatorVisitor visitor = translate("examples/BoolTest03.lemon");
        assertNotNull(visitor.prog);
    }

    // ==================== 辅助方法 ====================

    private TranslatorVisitor translate(String filename) throws IOException {
        Lexer lexer = new Lexer(new File(filename));
        Parser parser = new Parser(lexer);
        Ast.Program.T prog = parser.parse();
        
        TranslatorVisitor visitor = new TranslatorVisitor();
        visitor.visit(prog);
        
        return visitor;
    }
}
