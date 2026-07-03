import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.exception.ParseException;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParserRobustnessTest {

    @Test
    public void rejectsMissingIfBranchAsParseError() throws Exception {
        assertParseError("MissingIfBranch",
                "class MissingIfBranch { void main() { if (true) else {} } }",
                "期望合法语句");
    }

    @Test
    public void rejectsMissingLoopBodiesAsParseErrors() throws Exception {
        assertParseError("MissingWhileBody",
                "class MissingWhileBody { void main() { while (true) } }",
                "期望合法语句");
        assertParseError("MissingForBody",
                "class MissingForBody { void main() { for (;;) } }",
                "期望合法语句");
    }

    @Test
    public void rejectsNumericAndKeywordMethodNames() throws Exception {
        assertParseError("NumericMethod",
                "class NumericMethod { void main() {} int 123() { return 1; } }",
                "期望方法名");
        assertParseError("KeywordMethod",
                "class KeywordMethod { void main() {} int return() { return 1; } }",
                "期望方法名");
    }

    @Test
    public void rejectsKeywordClassName() throws Exception {
        assertParseError("return", "class return { void main() {} }", "期望类名");
    }

    @Test
    public void stillAcceptsMainKeywordAsEntryMethodName() throws Exception {
        Ast.Program.T program = parse("ValidMain", "class ValidMain { void main() {} }");
        Ast.MainClass.MainClassSingle mainClass =
                (Ast.MainClass.MainClassSingle) ((Ast.Program.ProgramSingle) program).getMainClass();

        assertEquals(1, mainClass.getMethods().size());
        assertEquals("main", ((Ast.Method.MethodSingle) mainClass.getMethods().get(0)).getId());
    }

    private static Ast.Program.T parse(String className, String source) throws Exception {
        return new Parser(new Lexer(writeSource(className, source))).parse();
    }

    private static void assertParseError(String className, String source, String expected)
            throws Exception {
        try {
            parse(className, source);
            fail("Expected ParseException");
        } catch (ParseException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
            assertTrue(e.getMessage(), !e.getMessage().contains("NullPointerException"));
        }
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
