import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.ast.Label;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrToJvmTranslator;
import site.ilemon.lexer.Lexer;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllExamplesJvmTest {

    @Test
    public void allRootExamplesMatchJvmOutputManifest() throws Exception {
        Map<String, String> expectedOutputs = loadManifest();
        TreeSet<String> examples = listRootExamples();

        assertEquals("Every root example must have one manifest entry",
                examples, new TreeSet<String>(expectedOutputs.keySet()));

        for (String example : examples) {
            String output = compileAndRun(example);
            assertEquals("JVM output mismatch for " + example,
                    normalize(expectedOutputs.get(example)), normalize(output));
        }
    }

    private Map<String, String> loadManifest() throws Exception {
        File manifest = new File("examples/example-output-manifest.tsv");
        assertTrue("Example output manifest should exist", manifest.exists());
        Map<String, String> outputs = new LinkedHashMap<String, String>();
        for (String line : Files.readAllLines(manifest.toPath(), StandardCharsets.US_ASCII)) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            assertEquals("Manifest line must contain name and base64 output: " + line, 2, parts.length);
            assertFalse("Duplicate manifest entry: " + parts[0], outputs.containsKey(parts[0]));
            outputs.put(parts[0], new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        }
        return outputs;
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

    private String compileAndRun(String name) throws Exception {
        Label.resetCounter();
        File sourceFile = new File("examples/" + name + ".lemon");
        Lexer lexer = new Lexer(sourceFile);
        Parser parser = new Parser(lexer);
        Ast.Program.Base program = parser.parse();

        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(program);

        program = new AstOptimizer().optimize(program);
        AstToIrTranslator astToIr = new AstToIrTranslator();
        program.accept(astToIr);
        IrProgram irProgram = astToIr.getProgram();
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
        assertTrue("JVM execution timed out for " + name, completed);
        assertEquals("JVM exit code should be 0 for " + name + ", output:\n" + output,
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
