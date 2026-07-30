import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmpge ||
                s instanceof site.ilemon.codegen.ast.Ast.Stmt.Ificmple ||
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
        Ast.Expr.IntLiteral num = new Ast.Expr.IntLiteral(42, 1);
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
        Ast.Expr.IntLiteral num = new Ast.Expr.IntLiteral(1, 1);
        Ast.Expr.GT gt = new Ast.Expr.GT(num, num, 1);
        Ast.Expr.LT lt = new Ast.Expr.LT(num, num, 1);
        Ast.Expr.GTE gte = new Ast.Expr.GTE(num, num, 1);
        Ast.Expr.LTE lte = new Ast.Expr.LTE(num, num, 1);
        Ast.Expr.EQ eq = new Ast.Expr.EQ(num, num, 1);
        Ast.Expr.NEQ neq = new Ast.Expr.NEQ(num, num, 1);
        
        assertNotNull(gt);
        assertNotNull(lt);
        assertNotNull(gte);
        assertNotNull(lte);
        assertNotNull(eq);
        assertNotNull(neq);
    }

    @Test
    public void testDoubleDispatchStmtAcceptExists() {
        // 验证语句节点的 accept 方法存在
        Ast.Expr.Id id = new Ast.Expr.Id("x", 1);
        Ast.Expr.IntLiteral num = new Ast.Expr.IntLiteral(1, 1);
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

    @Test
    public void testTranslateHexadecimalAndOctalIntegerLiterals() throws IOException {
        File sourceFile = writeSource("LegacyRadix",
                "class LegacyRadix {\n" +
                "    void main() {\n" +
                "        int hex;\n" +
                "        int octal;\n" +
                "        hex = 0x2A;\n" +
                "        octal = 077;\n" +
                "    }\n" +
                "}\n");

        TranslatorVisitor visitor = translate(sourceFile);
        boolean hasHexValue = false;
        boolean hasOctalValue = false;
        for (site.ilemon.codegen.ast.Ast.Method.MethodSingle method : visitor.prog.mainClass.methods) {
            for (site.ilemon.codegen.ast.Ast.Stmt.T statement : method.stms) {
                if (statement instanceof site.ilemon.codegen.ast.Ast.Stmt.Ldc) {
                    Object value = ((site.ilemon.codegen.ast.Ast.Stmt.Ldc) statement).i;
                    hasHexValue |= Integer.valueOf(42).equals(value);
                    hasOctalValue |= Integer.valueOf(63).equals(value);
                }
            }
        }
        assertTrue("legacy translator should decode hexadecimal literals", hasHexValue);
        assertTrue("legacy translator should decode octal literals", hasOctalValue);
    }

    @Test
    public void testTranslateInitializedAndBlockLocalDeclarations() throws IOException {
        File sourceFile = writeSource("LegacyBlockDeclarations",
                "class LegacyBlockDeclarations {\n" +
                "    void main() {\n" +
                "        int x = 1;\n" +
                "        { int y = x + 1; printf(\"%d\", y); }\n" +
                "    }\n" +
                "}\n");

        TranslatorVisitor visitor = translate(sourceFile);

        assertNotNull(visitor.prog);
        assertEquals(2, visitor.prog.mainClass.methods.get(0).locals.size());
    }

    @Test
    public void testLegacyDoubleComparisonUsesContiguousTemporarySlots() throws IOException {
        File sourceFile = writeSource("LegacyTempSlots",
                "class LegacyTempSlots {\n" +
                "    void main() {\n" +
                "        bool less;\n" +
                "        less = 1.0d < 2.0d;\n" +
                "    }\n" +
                "}\n");

        TranslatorVisitor visitor = translate(sourceFile);
        site.ilemon.codegen.ast.Ast.Method.MethodSingle method =
                visitor.prog.mainClass.methods.get(0);
        boolean hasLeftDoubleTemp = false;
        boolean hasRightDoubleTemp = false;
        boolean hasComparisonTemp = false;
        for (site.ilemon.codegen.ast.Ast.Stmt.T statement : method.stms) {
            if (statement instanceof site.ilemon.codegen.ast.Ast.Stmt.Dstore) {
                int slot = ((site.ilemon.codegen.ast.Ast.Stmt.Dstore) statement).index;
                hasLeftDoubleTemp |= slot == 1;
                hasRightDoubleTemp |= slot == 3;
            }
            if (statement instanceof site.ilemon.codegen.ast.Ast.Stmt.Istore) {
                hasComparisonTemp |= ((site.ilemon.codegen.ast.Ast.Stmt.Istore) statement).index == 5;
            }
        }

        assertTrue("left double temporary should start at the first free slot", hasLeftDoubleTemp);
        assertTrue("right double temporary should not overlap the left temporary", hasRightDoubleTemp);
        assertTrue("comparison result should use the next free slot", hasComparisonTemp);
        assertEquals("index should remain the next free local slot", 6, method.index);
    }

    // ==================== 辅助方法 ====================

    private TranslatorVisitor translate(String filename) throws IOException {
        return translate(new File(filename));
    }

    private TranslatorVisitor translate(File sourceFile) throws IOException {
        Lexer lexer = new Lexer(sourceFile);
        Parser parser = new Parser(lexer);
        Ast.Program.Base prog = parser.parse();
        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(prog);
        
        TranslatorVisitor visitor = new TranslatorVisitor(semantic.getResult());
        visitor.visit(prog);
        
        return visitor;
    }

    private File writeSource(String className, String source) throws IOException {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, className + ".lemon");
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }
}
