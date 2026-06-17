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
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackendEquivalenceTest {

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
        Label.resetCounter();
        File sourceFile = new File("examples/" + name + ".lemon");
        Lexer lexer = new Lexer(sourceFile);
        Parser parser = new Parser(lexer);
        Ast.Program.T program = parser.parse();

        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(program);
        assertTrue("Semantic analysis should pass for " + name, semantic.passOrNot());

        program = new AstOptimizer().optimize(program);
        AstToIrTranslator astToIr = new AstToIrTranslator();
        program.accept(astToIr);
        return astToIr.getProgram();
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
