import org.junit.Test;
import site.ilemon.compiler.LemonC;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LemonVmCliTest {

    @Test
    public void cliRunsVmBackend() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--target", "vm"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertEquals("values=5,weights=3,total=8\n", normalize(out.toString("UTF-8")));
    }

    @Test
    public void cliDumpsVmBytecodeAndRunsProgram() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--target=vm", "--dump-vm-bytecode"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String output = normalize(out.toString("UTF-8"));
        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertTrue(output, output.contains("== LemonVM Bytecode =="));
        assertTrue(output, output.contains("NewArr"));
        assertTrue(output, output.endsWith("values=5,weights=3,total=8\n"));
    }

    @Test
    public void cliReportsRuntimeErrorCleanlyAndKeepsOutputProducedBeforeIt() throws Exception {
        java.io.File source = writeSource("CliRuntimeFault",
                "class CliRuntimeFault {\n" +
                "    void main() {\n" +
                "        int a; int b;\n" +
                "        a = 10;\n" +
                "        b = 0;\n" +
                "        printf(\"before\\n\");\n" +
                "        printf(\"%d\\n\", a / b);\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{source.getPath(), "--run-vm"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));
        String stdout = normalize(out.toString("UTF-8"));
        String stderr = normalize(err.toString("UTF-8"));

        assertEquals(1, exitCode);
        assertEquals("出错前的输出必须保留（JVM 后端天然如此）", "before\n", stdout);
        assertTrue(stderr, stderr.contains("runtime error"));
        assertTrue(stderr, stderr.contains("除零"));
        assertTrue("诊断应带源码位置: " + stderr, stderr.contains("行 7"));
        assertTrue("不应向用户暴露 Java 栈回溯: " + stderr,
                !stderr.contains("site.ilemon.vm.LemonVm"));
        assertTrue("不应向用户暴露 Java 栈回溯: " + stderr, !stderr.contains("\tat "));
    }

    @Test
    public void cliRejectsResourceLimitFlagsWithoutVmTarget() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(
                new String[]{"examples/ArrayLengthTest.lemon", "--vm-instruction-limit", "1000"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(1, exitCode);
        assertTrue(err.toString("UTF-8"), err.toString("UTF-8").contains("--vm-instruction-limit"));
    }

    @Test
    public void cliInstructionLimitFlagBoundsRunawayLoops() throws Exception {
        java.io.File source = writeSource("CliRunawayLoop",
                "class CliRunawayLoop {\n" +
                "    void main() {\n" +
                "        int i;\n" +
                "        i = 0;\n" +
                "        while (true) { i = i + 1; }\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(
                new String[]{source.getPath(), "--run-vm", "--vm-instruction-limit", "5000"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));
        String stderr = normalize(err.toString("UTF-8"));

        assertEquals(1, exitCode);
        assertTrue(stderr, stderr.contains("超出指令执行上限 5000"));
        assertTrue("应指出停在哪一行: " + stderr, stderr.contains("行 5"));
    }

    @Test
    public void cliRunsWorkloadThatExceededTheOldInstructionCap() throws Exception {
        // 旧上限 1_000_000 会误杀这个合法程序。
        java.io.File source = writeSource("CliLongLoop",
                "class CliLongLoop {\n" +
                "    void main() {\n" +
                "        int i; int sum;\n" +
                "        sum = 0;\n" +
                "        for (i = 0; i < 300000; i++) { sum += 1; }\n" +
                "        printf(\"%d\\n\", sum);\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{source.getPath(), "--run-vm"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertEquals("300000\n", normalize(out.toString("UTF-8")));
    }

    private static java.io.File writeSource(String className, String source) throws Exception {
        java.io.File directory = new java.io.File("test_tmp");
        directory.mkdirs();
        java.io.File file = new java.io.File(directory, className + ".lemon");
        java.nio.file.Files.write(file.toPath(),
                source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }

    @Test
    public void cliRejectsVmBytecodeDumpWithoutVmTarget() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--dump-vm-bytecode"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(1, exitCode);
        assertEquals("", out.toString("UTF-8"));
        assertTrue(err.toString("UTF-8").contains("--dump-vm-bytecode requires --target vm or --run-vm"));
    }

    @Test
    public void cliShowsFullVmPipeline() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{"examples/ArrayLengthTest.lemon", "--pipeline"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String output = normalize(out.toString("UTF-8"));
        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertTrue(output, output.contains("== TOKENS =="));
        assertTrue(output, output.contains("== AST =="));
        assertTrue(output, output.contains("== LemonIR =="));
        assertTrue(output, output.contains("== LemonVM Bytecode =="));
        assertTrue(output, output.contains("== LemonVM Output =="));
        assertTrue(output, output.endsWith("values=5,weights=3,total=8\n"));
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
