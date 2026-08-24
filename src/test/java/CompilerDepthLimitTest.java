import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.compiler.LemonC;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompilerDepthLimitTest {

    @Test
    public void deeplyParenthesizedExpressionFailsCleanly() throws Exception {
        String expression = repeat("(", 512) + "1" + repeat(")", 512);
        assertCompilerRejects("DeepParentheses",
                "class DeepParentheses { void main() { int x = " + expression + "; } }",
                "嵌套过深");
    }

    @Test
    public void deeplyNestedUnaryExpressionFailsCleanly() throws Exception {
        // Keep '-' tokens separate so the lexer does not intentionally read them as '--'.
        String expression = repeat("- ", 512) + "1";
        assertCompilerRejects("DeepUnary",
                "class DeepUnary { void main() { int x = " + expression + "; } }",
                "嵌套过深");
    }

    @Test
    public void deeplyNestedBlocksFailCleanly() throws Exception {
        String source = "class DeepBlocks { void main() { "
                + repeat("{", 512) + repeat("}", 512) + " } }";
        assertCompilerRejects("DeepBlocks", source, "嵌套过深");
    }

    @Test
    public void longLeftDeepExpressionFailsDuringSemanticAnalysisWithoutOverflow()
            throws Exception {
        StringBuilder expression = new StringBuilder("1");
        for (int i = 0; i < 5000; i++) {
            expression.append(" + 1");
        }
        String source = "class DeepSemantic { void main() { int x = "
                + expression + "; } }";
        Ast.Program.Base program = new Parser(
                new Lexer(writeSource("DeepSemantic", source))).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.analyze(program);

        assertFalse(semantic.passOrNot());
        assertTrue(semantic.getErrors().toString(),
                semantic.getErrors().toString().contains("嵌套过深"));
    }

    @Test
    public void reasonableNestingStillCompiles() throws Exception {
        String expression = repeat("(", 32) + "1 + 2" + repeat(")", 32);
        File source = writeSource("ReasonableNesting",
                "class ReasonableNesting { void main() { int x = " + expression
                        + "; printf(\"%d\", x); } }");
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{source.getPath(), "--target", "vm"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(stderr));

        assertEquals(new String(stderr.toByteArray(), StandardCharsets.UTF_8), 0, exitCode);
    }

    private static void assertCompilerRejects(String className, String source,
                                              String expected) throws Exception {
        File file = writeSource(className, source);
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(stderr));

        String diagnostic = new String(stderr.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(diagnostic, 1, exitCode);
        assertTrue(diagnostic, diagnostic.contains(expected));
        assertFalse(diagnostic, diagnostic.contains("StackOverflowError"));
    }

    private static File writeSource(String className, String source) throws Exception {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, className + ".lemon");
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }

    private static String repeat(String text, int count) {
        StringBuilder result = new StringBuilder(text.length() * count);
        for (int i = 0; i < count; i++) {
            result.append(text);
        }
        return result.toString();
    }
}
