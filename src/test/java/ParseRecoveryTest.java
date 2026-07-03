import org.junit.Test;
import site.ilemon.compiler.LemonC;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.ParseDiagnostic;
import site.ilemon.parser.ParseResult;
import site.ilemon.parser.Parser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ParseRecoveryTest {

    @Test
    public void collectingParserReportsMultipleStatementErrors() throws Exception {
        ParseResult result = parseCollecting("MultipleSyntaxErrors",
                "class MultipleSyntaxErrors {\n" +
                "    void main() {\n" +
                "        int a = ;\n" +
                "        int b = 2\n" +
                "        int c = ;\n" +
                "        if (true) else {}\n" +
                "        return;\n" +
                "    }\n" +
                "}\n");

        assertNotNull(result.getProgram());
        assertTrue(result.getDiagnostics().toString(), result.getDiagnostics().size() >= 4);
        assertDiagnosticsHaveLocations(result.getDiagnostics());
    }

    @Test
    public void collectingParserRecoversAtMethodBoundary() throws Exception {
        ParseResult result = parseCollecting("MethodRecovery",
                "class MethodRecovery {\n" +
                "    void main() {}\n" +
                "    int 123() { return 1; }\n" +
                "    void helper() { int x = ; return; }\n" +
                "}\n");

        assertNotNull(result.getProgram());
        assertTrue(result.getDiagnostics().toString(), result.getDiagnostics().size() >= 2);
        assertContains(result.getDiagnostics(), "期望方法名");
    }

    @Test
    public void methodRecoverySkipsNestedInvalidMethodBodyCompletely() throws Exception {
        ParseResult result = parseCollecting("NestedMethodRecovery",
                "class NestedMethodRecovery {\n" +
                "    void main() {}\n" +
                "    int 123() { if (true) { while (true) {} } return 1; }\n" +
                "    void helper() { int x = ; return; }\n" +
                "}\n");

        assertNotNull(result.getProgram());
        assertContains(result.getDiagnostics(), "期望方法名");
        assertTrue(result.getDiagnostics().toString(), result.getDiagnostics().size() >= 2);
    }

    @Test
    public void cliPrintsAllSyntaxErrorsAndSkipsLaterPhases() throws Exception {
        File file = writeSource("CliParseRecovery",
                "class CliParseRecovery {\n" +
                "    void main() {\n" +
                "        int a = ;\n" +
                "        int b = ;\n" +
                "        if (true) else {}\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));
        String errorOutput = err.toString("UTF-8");

        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("syntax analysis has errors"));
        assertTrue(errorOutput, occurrences(errorOutput, "[语法分析]") >= 3);
        assertTrue(errorOutput, !errorOutput.contains("NullPointerException"));
        assertTrue(errorOutput, !errorOutput.contains("semantic analysis"));
    }

    private static ParseResult parseCollecting(String className, String source) throws Exception {
        return new Parser(new Lexer(writeSource(className, source))).parseCollecting();
    }

    private static void assertDiagnosticsHaveLocations(List<ParseDiagnostic> diagnostics) {
        for (ParseDiagnostic diagnostic : diagnostics) {
            assertTrue(diagnostic.toString(), diagnostic.getLine() > 0);
            assertTrue(diagnostic.toString(), diagnostic.getColumn() > 0);
            assertTrue(diagnostic.toString(), diagnostic.getMessage().contains("^"));
        }
    }

    private static void assertContains(List<ParseDiagnostic> diagnostics, String expected) {
        for (ParseDiagnostic diagnostic : diagnostics) {
            if (diagnostic.getMessage().contains(expected)) {
                return;
            }
        }
        throw new AssertionError("Expected '" + expected + "' in " + diagnostics);
    }

    private static int occurrences(String value, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }

    private static File writeSource(String className, String source) throws Exception {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, className + ".lemon");
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }
}
