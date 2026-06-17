import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrToVmTranslator;
import site.ilemon.lexer.Lexer;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllExamplesVmTest {

    @Test
    public void allRootExamplesMatchVmOutputManifest() throws Exception {
        Map<String, String> expectedOutputs = loadManifest();
        TreeSet<String> examples = listRootExamples();

        assertEquals("Every root example must have one manifest entry",
                examples, new TreeSet<String>(expectedOutputs.keySet()));

        for (String example : examples) {
            String output = compileAndRunVm(example);
            assertEquals("LemonVM output mismatch for " + example,
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

    private String compileAndRunVm(String name) throws Exception {
        File sourceFile = new File("examples/" + name + ".lemon");
        Lexer lexer = new Lexer(sourceFile);
        Parser parser = new Parser(lexer);
        Ast.Program.T program = parser.parse();

        SemanticVisitor semantic = new SemanticVisitor();
        semantic.visit(program);

        program = new AstOptimizer().optimize(program);

        AstToIrTranslator astToIr = new AstToIrTranslator();
        program.accept(astToIr);
        IrProgram irProgram = astToIr.getProgram();

        IrToVmTranslator irToVm = new IrToVmTranslator(irProgram);
        Script script = irToVm.translate();
        return new LemonVm(script).run();
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
