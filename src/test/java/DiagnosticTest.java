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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DiagnosticTest {

    @Test
    public void lexerRecordsTokenColumns() throws Exception {
        File file = writeSource("Test", "class Test {\n    void main() {}\n}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        Token classToken = lexer.getTokens().get(0);
        assertEquals(TokenKind.Class, classToken.getKind());
        assertEquals(1, classToken.getLineNumber());
        assertEquals(1, classToken.getColumnNumber());

        Token voidToken = findToken(lexer.getTokens(), TokenKind.Void);
        assertEquals(2, voidToken.getLineNumber());
        assertEquals(5, voidToken.getColumnNumber());
    }

    @Test
    public void lexerSkipsUtf8BomAtStartOfFile() throws Exception {
        File file = writeSource("BomTest", "\uFEFFclass BomTest {\n    void main() {}\n}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        Token classToken = lexer.getTokens().get(0);
        assertEquals(TokenKind.Class, classToken.getKind());
        assertEquals(1, classToken.getLineNumber());
        assertEquals(1, classToken.getColumnNumber());
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
    public void parserPreservesLogicalNotSourceLine() throws Exception {
        File file = writeSource("NotLine",
                "class NotLine {\n" +
                "    void main() {\n" +
                "        if (!false) {}\n" +
                "    }\n" +
                "}\n");
        Ast.Program.ProgramSingle program = (Ast.Program.ProgramSingle)
                new Parser(new Lexer(file)).parse();
        Ast.MainClass.MainClassSingle mainClass =
                (Ast.MainClass.MainClassSingle) program.getMainClass();
        Ast.Method.MethodSingle main = (Ast.Method.MethodSingle) mainClass.getMethods().get(0);
        Ast.Stmt.If ifStatement = (Ast.Stmt.If) main.getStms().get(0);

        assertEquals(3, ifStatement.getCondition().getLineNum());
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
        Ast.Program.Base program = parser.parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertTrue("collecting visitor should fail", !semantic.passOrNot());
        assertTrue("should collect at least two errors: " + semantic.getErrors(),
                semantic.getErrors().size() >= 2);
        assertTrue(contains(semantic.getErrors(), "y"));
        assertTrue(contains(semantic.getErrors(), "bool"));
    }

    @Test
    public void semanticErrorTypePropagatesWithoutCascadingDiagnostics() throws Exception {
        File file = writeSource("ErrorPropagation",
                "class ErrorPropagation {\n" +
                "    void main() {\n" +
                "        int x;\n" +
                "        x = missingValue + true;\n" +
                "        x = missingArray[0];\n" +
                "        if (missingCondition) {}\n" +
                "        printf(\"%d\", missingPrint);\n" +
                "    }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 4, semantic.getErrors().size());
        assertTrue(contains(semantic.getErrors(), "missingValue"));
        assertTrue(contains(semantic.getErrors(), "missingArray"));
        assertTrue(contains(semantic.getErrors(), "missingCondition"));
        assertTrue(contains(semantic.getErrors(), "missingPrint"));

        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle)
                ((Ast.Program.ProgramSingle) program).getMainClass();
        Ast.Method.MethodSingle main = (Ast.Method.MethodSingle) mainClass.getMethods().get(0);
        Ast.Stmt.Assign assignment = (Ast.Stmt.Assign) main.getStms().get(1);
        Ast.Expr.Add addition = (Ast.Expr.Add) assignment.getExpr();
        Ast.Expr.Id missing = (Ast.Expr.Id) addition.getLeft();
        assertSame(Ast.Type.Error.INSTANCE, missing.getType());
        assertEquals(Ast.Type.TypeKind.ERROR, missing.getType().getKind());

        Ast.Stmt.Assign arrayAssignment = (Ast.Stmt.Assign) main.getStms().get(2);
        Ast.Expr.ArrayAccess arrayAccess = (Ast.Expr.ArrayAccess) arrayAssignment.getExpr();
        assertSame(Ast.Type.Error.INSTANCE, arrayAccess.getElementType());
    }

    @Test
    public void semanticCollectingModeAnalyzesArgumentsOfUndefinedMethod() throws Exception {
        File file = writeSource("UndefinedCallArguments",
                "class UndefinedCallArguments {\n" +
                "    void main() { missing(unknownArgument, 1); }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 2, semantic.getErrors().size());
        assertTrue(contains(semantic.getErrors(), "unknownArgument"));
        assertTrue(contains(semantic.getErrors(), "missing"));
    }

    @Test
    public void semanticRootErrorsDoNotProduceDerivedTypeMismatches() throws Exception {
        File file = writeSource("RootErrorRecovery",
                "class RootErrorRecovery {\n" +
                "    void main() {\n" +
                "        int uninitialized;\n" +
                "        if (uninitialized) {}\n" +
                "        bool result;\n" +
                "        result = takesInt(true);\n" +
                "    }\n" +
                "    int takesInt(int value) { return value; }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 2, semantic.getErrors().size());
        assertTrue(contains(semantic.getErrors(), "uninitialized"));
        assertTrue(contains(semantic.getErrors(), "takesInt"));
    }

    @Test
    public void failedAssignmentDoesNotInitializeItsTarget() throws Exception {
        File file = writeSource("FailedAssignmentInitialization",
                "class FailedAssignmentInitialization {\n" +
                "    void main() {\n" +
                "        int value;\n" +
                "        value = missing;\n" +
                "        printf(\"%d\", value);\n" +
                "    }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 2, semantic.getErrors().size());
        assertTrue(contains(semantic.getErrors(), "missing"));
        assertTrue(contains(semantic.getErrors(), "value"));
    }

    @Test
    public void semanticCollectingModeKeepsArrayAssignmentStackBalanced() throws Exception {
        File file = writeSource("ArrayAssignmentRecovery",
                "class ArrayAssignmentRecovery {\n" +
                "    void main() {\n" +
                "        int scalar;\n" +
                "        scalar[missingIndex] = missingValue;\n" +
                "    }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 3, semantic.getErrors().size());
        assertTrue(contains(semantic.getErrors(), "missingIndex"));
        assertTrue(contains(semantic.getErrors(), "missingValue"));
        assertTrue(contains(semantic.getErrors(), "scalar"));
    }

    @Test
    public void semanticCollectingModeHandlesDanglingPrintfPercent() throws Exception {
        File file = writeSource("DanglingPrintfPercent",
                "class DanglingPrintfPercent {\n" +
                "    void main() { printf(\"x=%\", 1); }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 2, semantic.getErrors().size());
    }

    @Test
    public void semanticCollectingModeHandlesExtraPrintfArguments() throws Exception {
        File file = writeSource("PrintfDiagnostics",
                "class PrintfDiagnostics {\n" +
                "    void main() { printf(\"plain text\", 1); }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertTrue("collecting visitor should report the count mismatch", !semantic.passOrNot());
        assertEquals(1, semantic.getErrors().size());
    }

    @Test
    public void semanticCollectingModeReportsNonVoidMainOnce() throws Exception {
        File file = writeSource("NonVoidMain",
                "class NonVoidMain {\n" +
                "    int main() { return 1; }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);

        assertEquals(semantic.getErrors().toString(), 1, semantic.getErrors().size());
        assertTrue(contains(semantic.getErrors(), "main"));
        assertTrue(contains(semantic.getErrors(), "void"));
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
    public void cliCollectingModeReportsTooManyArgumentsWithoutIndexError() throws Exception {
        File file = writeSource("TooManyArgs",
                "class TooManyArgs {\n" +
                "    void main() {\n" +
                "        foo(1, 2);\n" +
                "    }\n" +
                "    int foo(int a) {\n" +
                "        return a;\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath()},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        String errorOutput = err.toString("UTF-8");
        assertEquals(1, exitCode);
        assertTrue(errorOutput, errorOutput.contains("foo"));
        assertTrue(errorOutput, !errorOutput.contains("IndexOutOfBoundsException"));
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
        for (Token token : lexer.getTokens()) {
            if (token.getKind() == TokenKind.Id && "sum_count".equals(token.getLexeme())) {
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

        Token voidToken = findToken(lexer.getTokens(), TokenKind.Void);
        assertEquals(4, voidToken.getLineNumber());
        assertEquals(5, voidToken.getColumnNumber());
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

    @Test
    public void lexerSeparatesStringKeywordAndStringLiteral() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() { printf(\"String\"); }\n" +
                "}\n" +
                "String\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        Token literal = findToken(lexer.getTokens(), TokenKind.StringLiteral);
        Token keyword = findToken(lexer.getTokens(), TokenKind.StringType);
        assertEquals("String", literal.getLexeme());
        assertEquals("String", keyword.getLexeme());
    }

    @Test
    public void lexerDecodesStringEscapes() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() {\n" +
                "        printf(\"a\\n\\t\\\"\\\\\");\n" +
                "    }\n" +
                "}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        Token literal = findToken(lexer.getTokens(), TokenKind.StringLiteral);
        assertEquals("a\n\t\"\\", literal.getLexeme());
    }

    @Test
    public void lexerReportsUnknownStringEscape() throws Exception {
        assertLexErrorContains(
                "class Test { void main() { printf(\"bad\\q\"); } }",
                "unknown escape sequence");
    }

    @Test
    public void lexerRecognizesDoubleAndLeadingDotFloatLiterals() throws Exception {
        File file = writeSource("Test",
                "class Test {\n" +
                "    void main() {\n" +
                "        float f;\n" +
                "        double d;\n" +
                "        f = .5f;\n" +
                "        d = 1e2;\n" +
                "    }\n" +
                "}\n");
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();

        boolean hasFloat = false;
        boolean hasDouble = false;
        for (Token token : lexer.getTokens()) {
            if (token.getKind() == TokenKind.FloatLiteral && ".5f".equals(token.getLexeme())) {
                hasFloat = true;
            }
            if (token.getKind() == TokenKind.DoubleLiteral && "1e2".equals(token.getLexeme())) {
                hasDouble = true;
            }
        }
        assertTrue("leading-dot float literal should be recognized", hasFloat);
        assertTrue("scientific double literal should be recognized", hasDouble);
    }

    @Test
    public void cliCompilesEscapedStringsAndDoubleLiterals() throws Exception {
        File file = writeSource("EscapedAndDouble",
                "class EscapedAndDouble {\n" +
                "    void main() {\n" +
                "        double d;\n" +
                "        d = 1e2;\n" +
                "        printf(\"quote=\\\" slash=\\\\ value=%f\\n\", d);\n" +
                "    }\n" +
                "}\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = LemonC.run(new String[]{file.getPath(), "--target=vm"},
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8"));

        assertEquals(err.toString("UTF-8"), 0, exitCode);
        assertEquals("quote=\" slash=\\ value=100.0\n",
                out.toString("UTF-8").replace("\r\n", "\n").replace("\r", "\n"));
    }

    private static Token findToken(List<Token> tokens, TokenKind kind) {
        for (Token token : tokens) {
            if (token.getKind() == kind) {
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
