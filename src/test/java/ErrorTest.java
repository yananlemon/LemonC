import org.junit.Before;
import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.ParseException;
import site.ilemon.exception.SemanticException;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * 负面测试 — 验证编译器在非法输入时能正确报错。
 * 每个测试验证一种特定的错误场景。
 */
public class ErrorTest {

    @Before
    public void setUp() {
        Label.resetCounter();
    }

    // ===== 语义错误 =====

    @Test(expected = SemanticException.class)
    public void testUndeclaredVariable() throws IOException {
        // 使用未声明的变量应报语义错误
        compileSource("class Test { void main() { x = 1; } }");
    }

    @Test(expected = SemanticException.class)
    public void testDuplicateMethod() throws IOException {
        // 重复定义方法应报语义错误
        compileSource("class Test { void main() {} void main() {} }");
    }

    @Test(expected = SemanticException.class)
    public void testWrongArgCount() throws IOException {
        // 方法参数个数不匹配应报语义错误
        compileSource("class Test { void main() { foo(1, 2); } int foo(int a) { return a; } }");
    }

    @Test(expected = SemanticException.class)
    public void testTypeMismatchInAssign() throws IOException {
        // 将 bool 赋给 int 应报语义错误
        compileSource("class Test { void main() { int x; x = true; } }");
    }

    @Test(expected = SemanticException.class)
    public void testNonBoolCondition() throws IOException {
        // if 条件不是 bool 应报语义错误
        compileSource("class Test { void main() { int x; x = 1; if(x) { } } }");
    }

    @Test(expected = SemanticException.class)
    public void testWhileNonBoolCondition() throws IOException {
        // while 条件不是 bool 应报语义错误
        compileSource("class Test { void main() { int x; x = 1; while(x) { x = 0; } } }");
    }

    @Test(expected = SemanticException.class)
    public void testMainReturnTypeNotVoid() throws IOException {
        // main 方法返回类型非 void 应报语义错误
        compileSource("class Test { int main() { return 0; } }");
    }

    @Test(expected = SemanticException.class)
    public void testReturnTypeMismatch() throws IOException {
        // 返回值与声明类型不匹配应报语义错误
        compileSource("class Test { void main() { int x; x = foo(); } int foo() { return true; } }");
    }

    @Test(expected = SemanticException.class)
    public void testUseBeforeAssign() throws IOException {
        // 局部变量使用前未赋值应报语义错误
        compileSource("class Test { void main() { int x; int y; y = x; } }");
    }

    @Test(expected = SemanticException.class)
    public void testIfSingleBranchAssignDoesNotGuaranteeAssignment() throws IOException {
        compileSource("class Test { void main() { int x; int y; y = 1; if (y > 0) { x = 1; } printf(\"x=%d\\n\", x); } }");
    }

    @Test
    public void testIfElseAssignGuaranteesAssignment() throws IOException {
        compileSource("class Test { void main() { int x; int y; y = 1; if (y > 0) { x = 1; } else { x = 2; } printf(\"x=%d\\n\", x); } }");
    }

    @Test(expected = SemanticException.class)
    public void testWhileBodyAssignDoesNotGuaranteeAssignment() throws IOException {
        compileSource("class Test { void main() { int x; int y; y = 0; while (y < 1) { x = 1; y = y + 1; } printf(\"x=%d\\n\", x); } }");
    }

    @Test(expected = SemanticException.class)
    public void testAndOperatorNonBool() throws IOException {
        // && 运算符的操作数不是 bool 应报语义错误
        compileSource("class Test { void main() { int x; int y; x = 1; y = 2; if(x && y) {} } }");
    }

    @Test(expected = SemanticException.class)
    public void testOrOperatorNonBool() throws IOException {
        // || 运算符的操作数不是 bool 应报语义错误
        compileSource("class Test { void main() { int x; int y; x = 1; y = 2; if(x || y) {} } }");
    }

    @Test(expected = SemanticException.class)
    public void testCompareTypeMismatch() throws IOException {
        // 数值类型可提升；非数值类型参与序比较仍应报语义错误
        compileSource("class Test { void main() { int x; bool y; x = 1; y = true; if(x > y) {} } }");
    }

    @Test(expected = SemanticException.class)
    public void testModRequiresIntOperands() throws IOException {
        compileSource("class Test { void main() { float f; int x; f = 2.0; x = 5 % f; } }");
    }

    @Test(expected = SemanticException.class)
    public void testLengthRequiresArray() throws IOException {
        compileSource("class Test { void main() { int x; int y; x = 1; y = x.length; } }");
    }

    @Test(expected = ParseException.class)
    public void testOnlyLengthArrayPropertyIsSupported() throws IOException {
        compileSource("class Test { void main() { int arr[3]; int y; y = arr.size; } }");
    }

    // ===== 语法错误 =====

    @Test(expected = SemanticException.class)
    public void testNonVoidMethodMustReturnOnAllPaths() throws IOException {
        compileSource("class Test { void main() {} int f(int x) { if (x > 0) { return 1; } } }");
    }

    @Test
    public void testNonVoidIfElseBothReturnPasses() throws IOException {
        compileSource("class Test { void main() { int x; x = f(1); } int f(int x) { if (x > 0) { return 1; } else { return 2; } } }");
    }

    @Test
    public void testBlockReturnSatisfiesNonVoidMethod() throws IOException {
        compileSource("class Test { void main() { int x; x = f(); } int f() { { return 1; } } }");
    }

    @Test(expected = SemanticException.class)
    public void testWhileReturnDoesNotGuaranteeNonVoidMethod() throws IOException {
        compileSource("class Test { void main() {} int f() { while (true) { return 1; } } }");
    }

    @Test(expected = SemanticException.class)
    public void testVoidCallCannotBeUsedAsExpression() throws IOException {
        compileSource("class Test { void main() { int x; x = foo(); } void foo() { printf(\"x\\n\"); } }");
    }

    @Test(expected = SemanticException.class)
    public void testPrintfArgCountMismatch() throws IOException {
        compileSource("class Test { void main() { printf(\"x=%d\\n\"); } }");
    }

    @Test(expected = SemanticException.class)
    public void testPrintfTypeMismatch() throws IOException {
        compileSource("class Test { void main() { float f; f = 1.0; printf(\"x=%d\\n\", f); } }");
    }

    @Test(expected = SemanticException.class)
    public void testPrintfUnsupportedPlaceholder() throws IOException {
        compileSource("class Test { void main() { printf(\"x=%s\\n\", 1); } }");
    }

    @Test(expected = SemanticException.class)
    public void testPrintfDanglingPercent() throws IOException {
        compileSource("class Test { void main() { printf(\"x=%\", 1); } }");
    }

    @Test(expected = SemanticException.class)
    public void testMainCannotReturnFromMiddle() throws IOException {
        compileSource("class Test { void main() { return 1; printf(\"unreachable\\n\"); } }");
    }

    @Test(expected = ParseException.class)
    public void testMissingSemicolon() throws IOException {
        // 缺少分号应报语法错误
        compileSource("class Test { void main() { int x } }");
    }

    @Test(expected = ParseException.class)
    public void testMissingClosingBrace() throws IOException {
        // 缺少右大括号应报语法错误
        compileSource("class Test { void main() { int x; ");
    }

    @Test(expected = ParseException.class)
    public void testMissingClosingParen() throws IOException {
        // 缺少右括号应报语法错误
        compileSource("class Test { void main( { } }");
    }

    @Test(expected = ParseException.class)
    public void testClassNameMismatch() throws IOException {
        File dir = new File("test_tmp");
        dir.mkdirs();
        File f = new File(dir, "Mismatch.lemon");
        Files.write(f.toPath(), "class Other { void main() {} }".getBytes("UTF-8"));
        try {
            Lexer lexer = new Lexer(f);
            Parser parser = new Parser(lexer);
            parser.parse();
        } finally {
            f.delete();
        }
    }

    // ===== 错误信息格式验证 =====

    @Test
    public void testSemanticErrorMessageFormat() throws IOException {
        try {
            compileSource("class Test { void main() { x = 1; } }");
            fail("应该抛出 SemanticException");
        } catch (SemanticException e) {
            String msg = e.getMessage();
            // 验证错误信息以 [语义分析] 开头，包含行号
            assertTrue("错误信息应包含阶段标识: " + msg, msg.contains("[语义分析]"));
            assertTrue("错误信息应包含行号: " + msg, msg.contains("行"));
        }
    }

    @Test
    public void testParseErrorMessageFormat() throws IOException {
        try {
            compileSource("class Test { void main() { int x } }");
            fail("应该抛出 ParseException");
        } catch (ParseException e) {
            String msg = e.getMessage();
            // 验证错误信息以 [语法分析] 开头，包含行号
            assertTrue("错误信息应包含阶段标识: " + msg, msg.contains("[语法分析]"));
            assertTrue("错误信息应包含行号: " + msg, msg.contains("行"));
        }
    }

    // ======================== 基础设施 ========================

    /**
     * 将源码字符串写入临时文件并执行完整编译管线。
     * 用于验证编译器在非法输入时能正确抛出异常。
     */
    private void compileSource(String source) throws IOException {
        // 从源码中提取类名作为文件名
        String className = "Test";
        int classIdx = source.indexOf("class ");
        if (classIdx >= 0) {
            String rest = source.substring(classIdx + 6).trim();
            int spaceIdx = rest.indexOf(' ');
            int braceIdx = rest.indexOf('{');
            int endIdx = Math.min(
                    spaceIdx >= 0 ? spaceIdx : Integer.MAX_VALUE,
                    braceIdx >= 0 ? braceIdx : Integer.MAX_VALUE
            );
            if (endIdx < Integer.MAX_VALUE) {
                className = rest.substring(0, endIdx).trim();
            }
        }

        File tempDir = new File("test_tmp");
        tempDir.mkdirs();
        File tempFile = new File(tempDir, className + ".lemon");
        try {
            Files.write(tempFile.toPath(), source.getBytes("UTF-8"));

            Lexer lexer = new Lexer(tempFile);
            Parser parser = new Parser(lexer);
            Ast.Program.T program = parser.parse();

            SemanticVisitor semantic = new SemanticVisitor();
            semantic.visit(program);
        } finally {
            tempFile.delete();
        }
    }
}
