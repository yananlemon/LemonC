import org.junit.Before;
import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.CompilerException;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * 编译器端到端集成测试。
 * 每个 .lemon 示例文件对应独立的 @Test 方法，
 * 验证完整编译管线：Lexer → Parser → SemanticVisitor → TranslatorVisitor → ByteCodeGenerator → Jasmin。
 */
public class CompilerTest {

    @Before
    public void setUp() {
        Label.resetCounter();
    }

    // ===== 基础计算 =====
    @Test public void testCal() throws IOException { compileAndVerify("Cal"); }
    @Test public void testCal01() throws IOException { compileAndVerify("Cal01"); }
    // Example01/03/05 类名与文件名不匹配(TestMain/MulTable)，属于已知问题
    @Test public void testExample02() throws IOException { compileAndVerify("Example02"); }

    // ===== 整数运算 =====
    @Test public void testIntTest01() throws IOException { compileAndVerify("IntTest01"); }

    // ===== 浮点运算 =====
    @Test public void testFloatTest01() throws IOException { compileAndVerify("FloatTest01"); }
    @Test public void testFloatTest02() throws IOException { compileAndVerify("FloatTest02"); }

    // ===== Double 运算 =====
    @Test public void testDoubleTest01() throws IOException { compileAndVerify("DoubleTest01"); }
    @Test public void testDoubleTest02() throws IOException { compileAndVerify("DoubleTest02"); }

    // ===== Bool 表达式 =====
    @Test public void testBoolTest01() throws IOException { compileAndVerify("BoolTest01"); }
    // BoolTest02 类名写成 BoolTest01，属于已知文件名不匹配问题
    @Test public void testBoolTest03() throws IOException { compileAndVerify("BoolTest03"); }
    @Test public void testBoolTest04() throws IOException { compileAndVerify("BoolTest04"); }
    @Test public void testBoolTest05() throws IOException { compileAndVerify("BoolTest05"); }
    @Test public void testBoolTest06() throws IOException { compileAndVerify("BoolTest06"); }
    @Test public void testBoolTest07() throws IOException { compileAndVerify("BoolTest07"); }
    @Test public void testBoolTest08() throws IOException { compileAndVerify("BoolTest08"); }
    @Test public void testBoolTest10() throws IOException { compileAndVerify("BoolTest10"); }
    @Test public void testBoolTest11() throws IOException { compileAndVerify("BoolTest11"); }
    @Test public void testBoolTest12() throws IOException { compileAndVerify("BoolTest12"); }
    @Test public void testBoolTest13() throws IOException { compileAndVerify("BoolTest13"); }
    @Test public void testBoolTest14() throws IOException { compileAndVerify("BoolTest14"); }
    @Test public void testBoolTest15() throws IOException { compileAndVerify("BoolTest15"); }
    @Test public void testBoolTest16() throws IOException { compileAndVerify("BoolTest16"); }

    // ===== If 条件 =====
    @Test public void testIf01() throws IOException { compileAndVerify("If01"); }
    @Test public void testIf02() throws IOException { compileAndVerify("If02"); }
    @Test public void testIf03() throws IOException { compileAndVerify("If03"); }
    // If04 语法格式有问题（if body缺少左大括号），属于已知问题
    @Test public void testIf05() throws IOException { compileAndVerify("If05"); }
    @Test public void testIf06() throws IOException { compileAndVerify("If06"); }
    @Test public void testIf07() throws IOException { compileAndVerify("If07"); }
    @Test public void testIf08() throws IOException { compileAndVerify("If08"); }
    @Test public void testIf09() throws IOException { compileAndVerify("If09"); }
    @Test public void testIf10() throws IOException { compileAndVerify("If10"); }
    @Test public void testIf11() throws IOException { compileAndVerify("If11"); }
    @Test public void testIf12() throws IOException { compileAndVerify("If12"); }
    @Test public void testIf13() throws IOException { compileAndVerify("If13"); }

    // ===== 循环 =====
    @Test public void testIteration01() throws IOException { compileAndVerify("Iteration01"); }
    @Test public void testIteration02() throws IOException { compileAndVerify("Iteration02"); }
    @Test public void testIteration03() throws IOException { compileAndVerify("Iteration03"); }
    @Test public void testIteration04() throws IOException { compileAndVerify("Iteration04"); }
    @Test public void testIteration05() throws IOException { compileAndVerify("Iteration05"); }
    @Test public void testIteration06() throws IOException { compileAndVerify("Iteration06"); }
    @Test public void testIterationDemo() throws IOException { compileAndVerify("IterationDemo"); }
    @Test public void testGauss() throws IOException { compileAndVerify("Gauss"); }
    @Test public void testMulTable() throws IOException { compileAndVerify("MulTable"); }

    // ===== 方法调用 =====
    @Test public void testMethodCallTest01() throws IOException { compileAndVerify("MethodCallTest01"); }
    @Test public void testMethodCallTest02() throws IOException { compileAndVerify("MethodCallTest02"); }
    @Test public void testMethodCallTest03() throws IOException { compileAndVerify("MethodCallTest03"); }
    @Test public void testMethodCallTest04() throws IOException { compileAndVerify("MethodCallTest04"); }
    @Test public void testSimpleMethodCall() throws IOException { compileAndVerify("SimpleMethodCall"); }
    @Test public void testSimpleMethodCallTwo() throws IOException { compileAndVerify("SimpleMethodCallTwo"); }
    @Test public void testSimpleMethodCallThree() throws IOException { compileAndVerify("SimpleMethodCallThree"); }
    @Test public void testSimpleMethodCallFour() throws IOException { compileAndVerify("SimpleMethodCallFour"); }

    // ===== 综合 =====
    @Test public void testFib() throws IOException { compileAndVerify("Fib"); }
    @Test public void testCalHeightOfChild() throws IOException { compileAndVerify("CalHeightOfChild"); }
    @Test public void testCompareTest() throws IOException { compileAndVerify("CompareTest"); }
    // Return.lemon 类名是 TestMain 与文件名不匹配，属于已知问题
    @Test public void testHelloWorld() throws IOException { compileAndVerify("Test"); }
    @Test public void testTestTwo() throws IOException { compileAndVerify("TestTwo"); }

    // ===== 数组 =====
    @Test public void testArrayTest01() throws IOException { compileAndVerify("ArrayTest01"); }
    @Test public void testArrayTest02() throws IOException { compileAndVerify("ArrayTest02"); }
    @Test public void testBubbleSort() throws IOException { compileAndVerify("BubbleSort"); }

    // ===== 多次编译（验证 Label.resetCounter 是否生效） =====
    @Test
    public void testMultipleCompilationsLabelReset() throws IOException {
        // 连续编译两个不同文件，验证 label 不冲突
        compileAndVerify("Cal");
        Label.resetCounter();
        compileAndVerify("Fib");
    }

    // ======================== 基础设施 ========================

    /**
     * 完整编译一个 .lemon 文件并验证每个阶段的输出。
     */
    private void compileAndVerify(String name) throws IOException {
        File sourceFile = new File("examples/" + name + ".lemon");
        assertTrue("源文件应存在: " + sourceFile.getPath(), sourceFile.exists());

        // 1. 词法分析
        Lexer lexer = new Lexer(sourceFile);
        assertNotNull("Lexer 不应为 null", lexer);

        // 2. 语法分析
        Parser parser = new Parser(lexer);
        Ast.Program.T program = parser.parse();
        assertNotNull("AST 不应为 null", program);

        // 3. 语义分析
        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(program);
        assertTrue("语义分析应通过: " + name, semantic.passOrNot());

        // 4. IR 翻译
        TranslatorVisitor translator = new TranslatorVisitor();
        translator.visit(program);
        assertNotNull("IR 程序不应为 null", translator.prog);
        assertNotNull("IR 主类不应为 null", translator.prog.mainClass);

        // 5. 字节码生成
        ByteCodeGenerator generator = new ByteCodeGenerator();
        generator.visit(translator.prog);

        // 6. 验证 .il 文件已生成
        String ilFileName = translator.prog.mainClass.id + ".il";
        File ilFile = new File(ilFileName);
        assertTrue(".il 文件应存在: " + ilFileName, ilFile.exists());
        assertTrue(".il 文件不应为空: " + ilFileName, ilFile.length() > 0);

        // 7. Jasmin 汇编 → .class
        jasmin.Main.main(new String[]{ilFileName});
        File classFile = new File(translator.prog.mainClass.id + ".class");
        assertTrue(".class 文件应存在: " + name, classFile.exists());
        assertTrue(".class 文件不应为空: " + name, classFile.length() > 0);
    }
}
