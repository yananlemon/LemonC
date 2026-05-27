import org.junit.Before;
import org.junit.Test;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.CompilerException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class ByteCodeGeneratorTest {

    @Before
    public void setUp() {
        Label.resetCounter();
    }

    @Test
    public void testSimpleArithmeticMaxStackIsExact() {
        assertEquals(2, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(10),
                new Ast.Stmt.Ldc(9),
                new Ast.Stmt.Iadd(),
                new Ast.Stmt.Istore(0))));
    }

    @Test
    public void testIntegerRemainderMaxStackIsExact() {
        assertEquals(2, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(10),
                new Ast.Stmt.Ldc(3),
                new Ast.Stmt.Irem(),
                new Ast.Stmt.Istore(0))));
    }

    @Test
    public void testDoublePrintfAccountsForDupX2() {
        assertEquals(4, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(1.5d),
                new Ast.Stmt.Printf(new Ast.Type.Double(), null))));
    }

    @Test
    public void testDoubleCompareMaxStackIsExact() {
        assertEquals(4, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(1.5d),
                new Ast.Stmt.Ldc(2.0d),
                new Ast.Stmt.Dcmpl(),
                new Ast.Stmt.Istore(0))));
    }

    @Test
    public void testInvokestaticUsesDescriptorSlots() {
        assertEquals(3, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(1),
                new Ast.Stmt.Ldc(2.0d),
                new Ast.Stmt.Invokestatic("mix",
                        Arrays.<Ast.Type.T>asList(new Ast.Type.Int(), new Ast.Type.Double()),
                        new Ast.Type.Float()),
                new Ast.Stmt.Fstore(0))));
    }

    @Test
    public void testVoidInvokestaticHasNoReturnSlot() {
        assertEquals(1, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(1),
                new Ast.Stmt.Invokestatic("noop",
                        Arrays.<Ast.Type.T>asList(new Ast.Type.Int()),
                        new Ast.Type.Void()))));
    }

    @Test
    public void testPopAndPop2ConsumeStatementCallResults() {
        assertEquals(2, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Invokestatic("intValue",
                        Collections.<Ast.Type.T>emptyList(),
                        new Ast.Type.Int()),
                new Ast.Stmt.Pop(),
                new Ast.Stmt.Invokestatic("doubleValue",
                        Collections.<Ast.Type.T>emptyList(),
                        new Ast.Type.Double()),
                new Ast.Stmt.Pop2())));
    }

    @Test
    public void testBranchMergeWithSameHeight() {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        assertEquals(1, ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(1),
                new Ast.Stmt.Ifgt(trueLabel),
                new Ast.Stmt.Ldc(2),
                new Ast.Stmt.Goto(endLabel),
                new Ast.Stmt.LabelJ(trueLabel),
                new Ast.Stmt.Ldc(3),
                new Ast.Stmt.LabelJ(endLabel),
                new Ast.Stmt.Istore(0))));
    }

    @Test(expected = CompilerException.class)
    public void testBranchMergeRejectsDifferentHeights() {
        Label target = new Label();

        ByteCodeGenerator.calculateMaxStack(stmts(
                new Ast.Stmt.Ldc(1),
                new Ast.Stmt.Ifgt(target),
                new Ast.Stmt.Ldc(2),
                new Ast.Stmt.Goto(target),
                new Ast.Stmt.LabelJ(target)));
    }

    @Test
    public void testMaxLocalsUsesActualLoadStoreIndexes() {
        assertEquals(8, ByteCodeGenerator.calculateMaxLocals(method("helper",
                Collections.<Ast.Declare.DeclareSingle>emptyList(),
                stmts(
                        new Ast.Stmt.Istore(0),
                        new Ast.Stmt.Dstore(3),
                        new Ast.Stmt.Aload(7)))));
    }

    @Test
    public void testMaxLocalsAccountsForMainArgs() {
        assertEquals(1, ByteCodeGenerator.calculateMaxLocals(method("main",
                Collections.<Ast.Declare.DeclareSingle>emptyList(),
                Collections.<Ast.Stmt.T>emptyList())));
    }

    @Test
    public void testMaxLocalsKeepsUnusedFormalSlots() {
        assertEquals(4, ByteCodeGenerator.calculateMaxLocals(method("unusedArgs",
                Arrays.asList(
                        formal(new Ast.Type.Int(), "x"),
                        formal(new Ast.Type.Double(), "y"),
                        formal(new Ast.Type.Float(), "z")),
                Collections.<Ast.Stmt.T>emptyList())));
    }

    @Test
    public void testVoidMethodDescriptorAndReturnAreEmitted() throws Exception {
        File outputDir = new File("target/test-bytecode-generator");
        ByteCodeGenerator generator = new ByteCodeGenerator(outputDir);
        Ast.Method.MethodSingle noop = new Ast.Method.MethodSingle(new Ast.Type.Void(), "noop", "DescriptorCheck",
                Collections.<Ast.Declare.DeclareSingle>emptyList(),
                Collections.<Ast.Declare.DeclareSingle>emptyList(),
                Collections.<Ast.Stmt.T>emptyList(), 0, 0);
        Ast.Method.MethodSingle main = new Ast.Method.MethodSingle(new Ast.Type.Void(), "main", "DescriptorCheck",
                Collections.<Ast.Declare.DeclareSingle>emptyList(),
                Collections.<Ast.Declare.DeclareSingle>emptyList(),
                stmts(new Ast.Stmt.Invokestatic("noop",
                        Collections.<Ast.Type.T>emptyList(),
                        new Ast.Type.Void())), 0, 0);
        Ast.MainClass.MainClassSingle mainClass = new Ast.MainClass.MainClassSingle("DescriptorCheck",
                Arrays.asList(noop, main));
        generator.visit(new Ast.Program.ProgramSingle(mainClass));

        String il = new String(Files.readAllBytes(generator.getOutputFile().toPath()), StandardCharsets.UTF_8);
        org.junit.Assert.assertTrue(il.contains(".method static noop()V"));
        org.junit.Assert.assertTrue(il.contains("invokestatic DescriptorCheck/noop()V"));
        org.junit.Assert.assertTrue(il.contains("return"));
    }

    private List<Ast.Stmt.T> stmts(Ast.Stmt.T... stmts) {
        return Arrays.asList(stmts);
    }

    private Ast.Declare.DeclareSingle formal(Ast.Type.T type, String id) {
        return new Ast.Declare.DeclareSingle(type, id);
    }

    private Ast.Method.MethodSingle method(String id, List<Ast.Declare.DeclareSingle> formals,
                                           List<Ast.Stmt.T> stmts) {
        return new Ast.Method.MethodSingle(new Ast.Type.Int(), id, "Test",
                formals, Collections.<Ast.Declare.DeclareSingle>emptyList(), stmts, 0, 0);
    }
}
