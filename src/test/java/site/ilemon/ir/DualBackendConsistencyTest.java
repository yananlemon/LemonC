package site.ilemon.ir;

import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DualBackendConsistencyTest {

    private String compileAndRunVm(String testFile) throws Exception {
        File file = new File("examples/" + testFile + ".lemon");
        Lexer lexer = new Lexer(file);
        Parser parser = new Parser(lexer);
        Ast.Program.Base root = parser.parse();

        SemanticVisitor semanticVisitor = new SemanticVisitor();
        root.accept(semanticVisitor);
        assertTrue("Semantic analysis should pass for " + testFile, semanticVisitor.passOrNot());

        root = new AstOptimizer().optimize(root);

        AstToIrTranslator astToIr = new AstToIrTranslator();
        root.accept(astToIr);
        IrProgram irProgram = astToIr.getProgram();
        IrVerifier.verify(irProgram);

        Script script = new IrToVmTranslator(irProgram).translate();
        return new LemonVm(script).run();
    }

    @Test
    public void testSimpleArithmetic() throws Exception {
        String output = compileAndRunVm("Cal");
        assertTrue(output, output.contains("3628800"));
    }

    @Test
    public void testWhileLoop() throws Exception {
        String output = compileAndRunVm("Iteration01");
        assertTrue(output, output.contains("5050"));
    }

    @Test
    public void testFib() throws Exception {
        String output = compileAndRunVm("Fib");
        assertTrue(output, output.contains("144"));
    }

    @Test
    public void testArrayTest01() throws Exception {
        String output = compileAndRunVm("ArrayTest01");
        assertEquals("arr[0] = 1\n"
                + "arr[1] = 2\n"
                + "arr[2] = 3\n"
                + "arr[3] = 4\n"
                + "arr[4] = 5\n", normalize(output));
    }

    @Test
    public void testArrayLengthTest() throws Exception {
        String output = compileAndRunVm("ArrayLengthTest");
        assertEquals("values=5,weights=3,total=8\n", normalize(output));
    }

    @Test
    public void testNestedLoopsBreakAndContinueOutput() throws Exception {
        String output = compileAndRunVm("NestedLoops");
        assertEquals("  inner run i=1, j=1\n"
                + "  inner break on 2\n"
                + "outer continue skip 2\n"
                + "  inner run i=3, j=1\n"
                + "  inner break on 2\n", normalize(output));
    }

    @Test
    public void testBoolTest16() throws Exception {
        String output = compileAndRunVm("BoolTest16");
        assertEquals("a=1\n"
                + "a=3\n"
                + "a=6\n"
                + "a=7\n", normalize(output));
    }

    private String normalize(String output) {
        return output.replace("\r\n", "\n").replace("\r", "\n");
    }
}
