import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.exception.CompilerException;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AstToIrTranslatorTest {

    @Test
    public void rejectsVariableMissingFromTypedSymbolState() throws Exception {
        File file = writeSource("MissingIrVariable",
                "class MissingIrVariable {\n" +
                "    void main() { missing = 1; }\n" +
                "}\n");
        Ast.Program.Base program = new Parser(new Lexer(file)).parse();

        try {
            program.accept(new AstToIrTranslator());
            fail("Expected IR translation to reject an unregistered variable");
        } catch (CompilerException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("missing"));
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
