import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.ast.Label;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrToJvmTranslator;
import site.ilemon.ir.IrToVmTranslator;
import site.ilemon.lexer.Lexer;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.typedast.TypedAst;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackendEquivalenceTest {

    @Test
    public void initializedAndBlockLocalDeclarationsMatchAcrossBackends() throws Exception {
        File sourceFile = writeSource("BlockDeclarationIntegration",
                "class BlockDeclarationIntegration {\n" +
                "    void main() {\n" +
                "        int x = 0x10;\n" +
                "        x = x + 1;\n" +
                "        {\n" +
                "            int y = 010;\n" +
                "            double widened = x;\n" +
                "            int values[2];\n" +
                "            values[0] = y;\n" +
                "            printf(\"%d %d %f %d\\n\", x, y, widened, values[0]);\n" +
                "        }\n" +
                "        int z;\n" +
                "        z = 3;\n" +
                "        printf(\"%d\\n\", z);\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("17 8 17.0 8\n3\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void emptyReturnStopsVoidMethodsAcrossBackends() throws Exception {
        File sourceFile = writeSource("EmptyReturnIntegration",
                "class EmptyReturnIntegration {\n" +
                "    void main() {\n" +
                "        helper();\n" +
                "        return;\n" +
                "        printf(\"unreachable-main\\n\");\n" +
                "    }\n" +
                "    void helper() {\n" +
                "        printf(\"before\\n\");\n" +
                "        return;\n" +
                "        printf(\"unreachable-helper\\n\");\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("before\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void allPathReturnFunctionCompilesAcrossBackends() throws Exception {
        File sourceFile = writeSource("AllPathReturnIntegration",
                "class AllPathReturnIntegration {\n" +
                "    void main() { printf(\"%d %d\\n\", choose(true), choose(false)); }\n" +
                "    int choose(bool value) {\n" +
                "        if (value) { return 1; } else { return 2; }\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("1 2\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void signedZeroComparisonsMatchAcrossBackends() throws Exception {
        File sourceFile = writeSource("SignedZeroIntegration",
                "class SignedZeroIntegration {\n" +
                "    void main() {\n" +
                "        float f;\n" +
                "        double d;\n" +
                "        f = -0.0f;\n" +
                "        d = -0.0;\n" +
                "        if (f == 0.0f) { printf(\"float-equal\\n\"); }\n" +
                "        if (d < 0.0) { printf(\"double-less\\n\"); } else { printf(\"double-not-less\\n\"); }\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("float-equal\ndouble-not-less\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void workloadsPastTheOldVmLimitsMatchAcrossBackends() throws Exception {
        // 两个规模都刻意越过旧的 VM 硬上限：
        //   循环 200000 次 ≈ 120 万条指令 > 旧的 1_000_000 指令上限
        //   递归深度 2000 > 旧的 4096 槽位栈能承受的约 600 层
        // 在修复之前，JVM 后端能跑完而 VM 后端直接失败——"双后端等价"名不副实。
        File sourceFile = writeSource("VmLimitsIntegration",
                "class VmLimitsIntegration {\n" +
                "    int rec(int n) {\n" +
                "        if (n <= 0) { return 0; }\n" +
                "        return rec(n - 1) + 1;\n" +
                "    }\n" +
                "    void main() {\n" +
                "        int i;\n" +
                "        int sum;\n" +
                "        sum = 0;\n" +
                "        for (i = 0; i < 200000; i++) {\n" +
                "            sum += 1;\n" +
                "        }\n" +
                "        printf(\"%d %d\\n\", sum, rec(2000));\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("200000 2000\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void incrementAndCompoundAssignmentMatchAcrossBackends() throws Exception {
        // ++/--/op= 在解析阶段按 a = a op b 脱糖，因此后端无需任何改动；
        // 这里验证脱糖出来的语义在两个后端上都正确。
        File sourceFile = writeSource("CompoundAssignIntegration",
                "class CompoundAssignIntegration {\n" +
                "    void main() {\n" +
                "        int i; int s; int k; int a[4];\n" +
                "        s = 0;\n" +
                "        for (i = 0; i < 4; i++) { a[i] = i; }\n" +
                "        for (i = 0; i < 4; i++) { a[i] *= 10; }\n" +
                "        for (i = 0; i < 4; i++) { s += a[i]; }\n" +
                "        k = 3;\n" +
                "        k -= 1;\n" +
                "        k *= 5;\n" +
                "        k /= 2;\n" +
                "        k %= 4;\n" +
                "        a[2]++;\n" +
                "        i--;\n" +
                "        printf(\"%d %d %d %d\\n\", s, k, a[2], i);\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("60 1 21 3\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void compoundAssignmentOnFloatingTargetMatchesAcrossBackends() throws Exception {
        File sourceFile = writeSource("CompoundWideningIntegration",
                "class CompoundWideningIntegration {\n" +
                "    void main() {\n" +
                "        double d;\n" +
                "        float f;\n" +
                "        d = 1.0d;\n" +
                "        f = 2.0;\n" +
                "        d += 2;\n" +
                "        f *= 3;\n" +
                "        printf(\"%f %f\\n\", d, f);\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("3.0 6.0\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void siblingBlockScopesGetDistinctSlotsAcrossBackends() throws Exception {
        // 三个兄弟块复用同一个名字且类型不同（含占两个 JVM 槽位的 double），
        // 每个声明必须拿到独立的 vreg 与槽位。
        File sourceFile = writeSource("SiblingScopeIntegration",
                "class SiblingScopeIntegration {\n" +
                "    void main() {\n" +
                "        { int v; v = 10; printf(\"%d\\n\", v); }\n" +
                "        { double v; v = 2.5d; printf(\"%f\\n\", v); }\n" +
                "        { int v; v = 30; printf(\"%d\\n\", v); }\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("10\n2.5\n30\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void runtimeNegationPreservesSignedZeroAcrossBackends() throws Exception {
        // 取负的操作数是变量而非字面量，所以常量折叠不会介入，走的是 LemonIR 的 NEG 路径。
        File sourceFile = writeSource("RuntimeNegationIntegration",
                "class RuntimeNegationIntegration {\n" +
                "    void main() {\n" +
                "        float fz;\n" +
                "        double dz;\n" +
                "        int i;\n" +
                "        fz = 0.0;\n" +
                "        dz = 0.0d;\n" +
                "        i = 7;\n" +
                "        printf(\"%f %f %d\\n\", -fz, -dz, -i);\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("-0.0 -0.0 -7\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void aDecimalLiteralHasTheSameValueInEverySyntacticPosition() throws Exception {
        // 同一个字面量 0.1 出现在赋值、二元表达式、数组元素、实参、返回值、比较六个位置，
        // 都必须取到同一个 double 值。修复前赋值路径按十进制原文materialize（0.1），
        // 而其余路径先舍入成 float 再 F2D（0.10000000149011612）。
        File sourceFile = writeSource("LiteralPositionIntegration",
                "class LiteralPositionIntegration {\n" +
                "    double identity(double v) { return v; }\n" +
                "    double literal() { return 0.1; }\n" +
                "    void main() {\n" +
                "        double assigned;\n" +
                "        double viaExpression;\n" +
                "        double element[1];\n" +
                "        assigned = 0.1;\n" +
                "        viaExpression = 0.0d + 0.1;\n" +
                "        element[0] = 0.1;\n" +
                "        printf(\"%f %f %f %f %f\\n\",\n" +
                "               assigned, viaExpression, element[0], identity(0.1), literal());\n" +
                "        if (assigned == 0.1) { printf(\"cmp=yes\\n\"); } else { printf(\"cmp=no\\n\"); }\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("0.1 0.1 0.1 0.1 0.1\ncmp=yes\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void widePrecisionLiteralsKeepAllTheirDigitsInADoubleContext() throws Exception {
        // 反向守卫：不能为了"一致"而把赋值路径也改成先过 float——那会让
        // double a = 3.14159265358979 静默变成 3.1415927410125732。
        File sourceFile = writeSource("WideLiteralIntegration",
                "class WideLiteralIntegration {\n" +
                "    void main() {\n" +
                "        double pi;\n" +
                "        double viaExpression;\n" +
                "        pi = 3.14159265358979;\n" +
                "        viaExpression = 0.0d + 3.14159265358979;\n" +
                "        printf(\"%f %f\\n\", pi, viaExpression);\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("3.14159265358979 3.14159265358979\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void mixedPrecisionConstantComparisonsMatchRuntimeAcrossBackends() throws Exception {
        // 折叠后的常量比较必须和未折叠的变量比较得到同样的结果。
        File sourceFile = writeSource("MixedPrecisionCompareIntegration",
                "class MixedPrecisionCompareIntegration {\n" +
                "    void main() {\n" +
                "        float f;\n" +
                "        int i;\n" +
                "        f = 16777216.0;\n" +
                "        i = 16777217;\n" +
                "        report(16777217 > 16777216.0);\n" +
                "        report(i > f);\n" +
                "        report(16777217 > 16777216.0d);\n" +
                "    }\n" +
                "    void report(bool value) {\n" +
                "        if (value) { printf(\"true\\n\"); } else { printf(\"false\\n\"); }\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("false\nfalse\ntrue\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void hexadecimalAndOctalLiteralsMatchAcrossBackends() throws Exception {
        File sourceFile = writeSource("RadixIntegration",
                "class RadixIntegration {\n" +
                "    void main() {\n" +
                "        int values[010];\n" +
                "        int folded;\n" +
                "        double widened;\n" +
                "        folded = 0x20 + 010 + 2;\n" +
                "        widened = 0x2A;\n" +
                "        values[07] = 077;\n" +
                "        printf(\"%d %d %f\\n\", folded, values[07], widened);\n" +
                "    }\n" +
                "}\n");

        IrProgram irProgram = compileToLemonIr(sourceFile);
        String vmOutput = normalize(runVm(irProgram));
        String jvmOutput = normalize(runJvm(irProgram));

        assertEquals("42 63 42.0\n", vmOutput);
        assertEquals(vmOutput, jvmOutput);
    }

    @Test
    public void allRootExamplesHaveEquivalentJvmAndVmOutputFromSameLemonIr() throws Exception {
        for (String example : listRootExamples()) {
            IrProgram irProgram = compileToLemonIr(example);
            String vmOutput = runVm(irProgram);
            String jvmOutput = runJvm(irProgram);
            assertEquals("Backend output mismatch for " + example,
                    normalize(vmOutput), normalize(jvmOutput));
        }
    }

    private IrProgram compileToLemonIr(String name) throws Exception {
        return compileToLemonIr(new File("examples/" + name + ".lemon"));
    }

    private IrProgram compileToLemonIr(File sourceFile) throws Exception {
        Label.resetCounter();
        Lexer lexer = new Lexer(sourceFile);
        Parser parser = new Parser(lexer);
        Ast.Program.Base program = parser.parse();

        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(program);
        assertTrue("Semantic analysis should pass for " + sourceFile.getName(), semantic.passOrNot());

        TypedAst.Program typedProgram = new AstOptimizer().optimize(semantic.getTypedProgram());
        AstToIrTranslator astToIr = new AstToIrTranslator();
        return astToIr.translate(typedProgram);
    }

    private File writeSource(String className, String source) throws Exception {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, className + ".lemon");
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }

    private String runVm(IrProgram irProgram) {
        Script script = new IrToVmTranslator(irProgram).translate();
        return new LemonVm(script).run();
    }

    private String runJvm(IrProgram irProgram) throws Exception {
        site.ilemon.codegen.ast.Ast.Program.ProgramSingle jvmProgram =
                new IrToJvmTranslator(irProgram).translate();
        ByteCodeGenerator generator = new ByteCodeGenerator();
        generator.visit(jvmProgram);
        assembleWithJasmin(generator.getOutputDir(), generator.getOutputFile());

        Process process = new ProcessBuilder(javaExecutable(),
                "-Dfile.encoding=UTF-8",
                "-Dsun.stdout.encoding=UTF-8",
                "-Dsun.stderr.encoding=UTF-8",
                "-cp", generator.getOutputDir().getPath(), jvmProgram.mainClass.id)
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(10, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
        }
        String output = readAll(process.getInputStream());
        assertTrue("JVM execution timed out for " + jvmProgram.mainClass.id, completed);
        assertEquals("JVM exit code should be 0 for " + jvmProgram.mainClass.id + ", output:\n" + output,
                0, process.exitValue());
        return output;
    }

    private void assembleWithJasmin(File outputDir, File ilFile) throws Exception {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        PrintStream quiet = new PrintStream(sink, true, "UTF-8");
        try {
            System.setOut(quiet);
            System.setErr(quiet);
            jasmin.Main.main(new String[]{"-d", outputDir.getPath(), ilFile.getPath()});
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            quiet.close();
        }
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

    private String javaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getPath();
    }

    private String readAll(InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int read;
        while ((read = stream.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
