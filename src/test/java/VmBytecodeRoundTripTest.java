import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrToVmTranslator;
import site.ilemon.lexer.Lexer;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.typedast.TypedAst;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;
import site.ilemon.vm.VmBytecodeParser;
import site.ilemon.vm.VmBytecodeWriter;

import java.io.File;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * `.lbc` 文本字节码的往返一致性。
 *
 * <p>这是一条独立的验证轴：{@code compile -> emit .lbc -> parse -> run} 的输出必须与
 * 直接 {@code compile -> run} 相同。它能抓到双后端差分测试抓不到的一类错误——
 * 两个后端都从同一个 Script 出发，所以 Script 的序列化/反序列化缺陷对它们是不可见的。</p>
 */
public class VmBytecodeRoundTripTest {

    @Test
    public void everyExampleSurvivesAnLbcRoundTrip() throws Exception {
        TreeSet<String> examples = listRootExamples();
        assertTrue("应能找到示例程序", examples.size() > 0);

        for (String example : examples) {
            Script direct = compileToVmScript(example);
            String expected = normalize(new LemonVm(direct).run());

            Script reparsed = VmBytecodeParser.parse(
                    VmBytecodeWriter.write(compileToVmScript(example), example));
            String actual = normalize(new LemonVm(reparsed).run());

            assertEquals(".lbc 往返后输出不一致: " + example, expected, actual);
        }
    }

    @Test
    public void emittedTextIsStableAcrossTwoEmissions() throws Exception {
        String first = VmBytecodeWriter.write(compileToVmScript("Fib"), "Fib");
        String second = VmBytecodeWriter.write(compileToVmScript("Fib"), "Fib");

        assertEquals("同一程序两次发射应逐字节相同", first, second);
    }

    @Test
    public void emittedTextDistinguishesStackSlotsFromImmediates() throws Exception {
        // .lbc 用拼写区分二者：裸整数是栈槽位，'#' 前缀是立即数。
        // 写错方向会把 "存入字面量 15" 变成 "从槽位 15 拷贝"，且往返后仍能解析成功，
        // 所以这条断言直接盯住拼写。
        String lbc = VmBytecodeWriter.write(compileToVmScript("HelloWorld"), "HelloWorld");

        assertTrue(lbc, lbc.contains("#15"));
        assertTrue("函数引用应带 _ 前缀: " + lbc, lbc.contains("_add"));
        assertTrue("返回值寄存器应写作 _RetVal: " + lbc, lbc.contains("_RetVal"));
        assertTrue("应有 .func/.end 包裹: " + lbc, lbc.contains(".func main") && lbc.contains(".end"));
    }

    @Test
    public void roundTripPreservesJumpTargetsInLoops() throws Exception {
        // 跳转目标在 Script 里是绝对 PC，在 .lbc 里是函数内标签。
        // 这一步是发射器唯一需要“反解析”的地方，所以单独用带循环的程序盯一下。
        Script direct = compileToVmScript("NestedLoops");
        String lbc = VmBytecodeWriter.write(compileToVmScript("NestedLoops"), "NestedLoops");

        assertTrue("应生成函数内标签: " + lbc, lbc.matches("(?s).*\\bL\\d+:.*"));
        assertEquals(normalize(new LemonVm(direct).run()),
                normalize(new LemonVm(VmBytecodeParser.parse(lbc)).run()));
    }

    private TreeSet<String> listRootExamples() {
        File[] files = new File("examples").listFiles((dir, name) -> name.endsWith(".lemon"));
        TreeSet<String> examples = new TreeSet<String>();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                examples.add(name.substring(0, name.lastIndexOf('.')));
            }
        }
        return examples;
    }

    private Script compileToVmScript(String name) throws Exception {
        Lexer lexer = new Lexer(new File("examples/" + name + ".lemon"));
        Parser parser = new Parser(lexer);
        Ast.Program.Base program = parser.parse();

        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(program);

        TypedAst.Program typedProgram = new AstOptimizer().optimize(semantic.getTypedProgram());
        IrProgram irProgram = new AstToIrTranslator().translate(typedProgram);
        return new IrToVmTranslator(irProgram).translate();
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
