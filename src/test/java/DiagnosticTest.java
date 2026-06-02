import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.compiler.LemonC;
import site.ilemon.exception.LexException;
import site.ilemon.exception.ParseException;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DiagnosticTest {

    @Test
    public void lexerRecordsTokenColumns() throws Exception {
        File file = writeSource("Test", "class Test {\n    void main() {}\n}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        Token classToken = lexer.tokens.get(0);
        assertEquals(TokenKind.Class, classToken.kind);
        assertEquals(1, classToken.lineNumber);
        assertEquals(1, classToken.columnNumber);

        Token voidToken = findToken(lexer.tokens, TokenKind.Void);
        assertEquals(2, voidToken.lineNumber);
        assertEquals(5, voidToken.columnNumber);
    }

    @Test
    public void parserErrorIncludesColumnAndSourcePointer() throws Exception {
        File file = writeSource("Test", "class Test {\n    void main() { int x }\n}\n");
        try {
            new Parser(new Lexer(file)).parse();
            fail("Expected ParseException");
        } catch (ParseException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("int x }"));
            assertTrue(message, message.contains("^"));
        }
    }

    @Test
    public void semanticCollectingModeReportsMultipleErrors() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        y = 1;\n" +
                "        x = true;\n" +
                "    }\n" +
                "}\n");
        Parser parser = new Parser(new Lexer(file));
        Ast.Program.T program = parser.parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertTrue("collecting visitor should fail", !semantic.passOrNot());
        assertTrue("should collect at least two errors: " + semantic.getErrors(),
                semantic.getErrors().size() >= 2);
        assertTrue(contains(semantic.getErrors(), "y"));
        assertTrue(contains(semantic.getErrors(), "bool"));
    }

    @Test
    public void cliReportsMultipleSemanticErrors() throws Exception {
        File file = writeSource("CliDiagnostics",
                "class CliDiagnostics {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        y = 1;\n" +
                "        x = true;\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String errorOutput = err.toString("UTF-8");
        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("compile failed"));
        assertTrue(errorOutput, errorOutput.contains("y"));
        assertTrue(errorOutput, errorOutput.contains("bool"));
        assertTrue(errorOutput, errorOutput.contains("        y = 1;"));
        assertTrue(errorOutput, errorOutput.contains("        x = true;"));
        assertTrue(errorOutput, errorOutput.contains("^"));
    }

    @Test
    public void cliCollectingModeReportsMissingMainWithoutNpe() throws Exception {
        File file = writeSource("NoMain",
                "class NoMain {\n" +
                "    int f() { return 1; }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String errorOutput = err.toString("UTF-8");
        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("compile failed"));
        assertTrue(errorOutput, errorOutput.toLowerCase().contains("main"));
        assertTrue(errorOutput, !errorOutput.contains("NullPointerException"));
    }

    @Test
    public void cliCollectingModeReportsUndefinedMethodWithoutNpe() throws Exception {
        File file = writeSource("UndefinedMethod",
                "class UndefinedMethod {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        x = missing();\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String errorOutput = err.toString("UTF-8");
        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("missing"));
        assertTrue(errorOutput, !errorOutput.contains("NullPointerException"));
    }

    @Test
    public void cliCollectingModeReportsUndefinedArrayWithoutNpe() throws Exception {
        File file = writeSource("UndefinedArray",
                "class UndefinedArray {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        x = values.length;\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String errorOutput = err.toString("UTF-8");
        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("values"));
        assertTrue(errorOutput, !errorOutput.contains("NullPointerException"));
    }

    @Test
    public void lexerReportsIllegalCharacterWithSourcePointer() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        x = 1 @ 2;\n" +
                "    }\n" +
                "}\n");
        try {
            new Lexer(file).lexicalAnalysis();
            fail("Expected LexException");
        } catch (LexException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("lexical analysis"));
            assertTrue(message, message.contains("illegal character '@'"));
            assertTrue(message, message.contains("x = 1 @ 2;"));
            assertTrue(message, message.contains("^"));
        }
    }

    @Test
    public void lexerReportsUnclosedString() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() {\n" +
                "        printf(\"hello);\n" +
                "    }\n" +
                "}\n");
        try {
            new Lexer(file).lexicalAnalysis();
            fail("Expected LexException");
        } catch (LexException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("unclosed string literal"));
            assertTrue(message, message.contains("printf(\"hello);"));
            assertTrue(message, message.contains("^"));
        }
    }

    @Test
    public void lexerReportsSingleAmpersandAndPipe() throws Exception {
        assertLexErrorContains(
                "class Test { void main() { bool a; bool b; a = true; b = false; if (a & b) {} } }",
                "did you mean '&&'");
        assertLexErrorContains(
                "class Test { void main() { bool a; bool b; a = true; b = false; if (a | b) {} } }",
                "did you mean '||'");
    }

    @Test
    public void cliReportsLexicalErrors() throws Exception {
        File file = writeSource("CliLexDiagnostics",
                "class CliLexDiagnostics {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        x = 1 @ 2;\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String errorOutput = err.toString("UTF-8");
        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("compile failed"));
        assertTrue(errorOutput, errorOutput.contains("illegal character '@'"));
        assertTrue(errorOutput, errorOutput.contains("^"));
    }

    @Test
    public void lexerSupportsUnderscoreIdentifiers() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() {\n" +
                "        int sum_count;\n" +
                "        sum_count = 3;\n" +
                "        printf(\"x=%d\\n\", sum_count);\n" +
                "    }\n" +
                "}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        boolean found = false;
        for (Token token : lexer.tokens) {
            if (token.kind == TokenKind.Id && "sum_count".equals(token.lexeme)) {
                found = true;
                break;
            }
        }
        assertTrue("underscore identifier should be a single Id token", found);
    }

    @Test
    public void cliCompilesUnderscoreIdentifiers() throws Exception {
        File file = writeSource("Under_score",
                "class Under_score {\n" +
                "    void main() {\n" +
                "        int sum_count;\n" +
                "        sum_count = 3;\n" +
                "        printf(\"x=%d\\n\", sum_count);\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(err.toString("UTF-8"), 0, exitCode);
    }

    @Test
    public void cliSuccessfulCompileIsQuietByDefault() throws Exception {
        File file = writeSource("QuietCli",
                "class QuietCli {\n" +
                "    void main() {\n" +
                "        printf(\"ok\\n\");\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertEquals("", out.toString("UTF-8"));
        assertEquals("", err.toString("UTF-8"));
    }

    @Test
    public void cliVerboseShowsGenerationOutput() throws Exception {
        File file = writeSource("VerboseCli",
                "class VerboseCli {\n" +
                "    void main() {\n" +
                "        printf(\"ok\\n\");\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath(), "--verbose"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String output = out.toString("UTF-8");
        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertTrue(output, output.contains("Generated:"));
        assertEquals("", err.toString("UTF-8"));
    }

    @Test
    public void lexerSkipsMultilineCommentsAndKeepsLineNumbers() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    /* comment line 1\n" +
                "       comment line 2 */\n" +
                "    void main() {}\n" +
                "}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        Token voidToken = findToken(lexer.tokens, TokenKind.Void);
        assertEquals(4, voidToken.lineNumber);
        assertEquals(5, voidToken.columnNumber);
    }

    @Test
    public void lexerReportsUnclosedMultilineComment() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    /* comment starts\n" +
                "    void main() {}\n" +
                "}\n");
        try {
            new Lexer(file).lexicalAnalysis();
            fail("Expected LexException");
        } catch (LexException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("unclosed multiline comment"));
            assertTrue(message, message.contains("/* comment starts"));
            assertTrue(message, message.contains("^"));
        }
    }

    private static Token findToken(List<Token> tokens, TokenKind kind) {
        for (Token token : tokens) {
            if (token.kind == kind) {
                return token;
            }
        }
        throw new AssertionError("Token not found: " + kind);
    }

    private static boolean contains(List<String> values, String part) {
        for (String value : values) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static void assertLexErrorContains(String source, String expected) throws Exception {
        File file = writeSource("Test", source);
        try {
            new Lexer(file).lexicalAnalysis();
            fail("Expected LexException");
        } catch (LexException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }

    private static File writeSource(String className, String source) throws Exception {
        File dir = new File("test_tmp");
        dir.mkdirs();
        File file = new File(dir, className + ".lemon");
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }
}
