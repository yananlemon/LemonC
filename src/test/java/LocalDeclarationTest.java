import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.exception.SemanticException;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LocalDeclarationTest {

    @Test
    public void parserKeepsDeclarationOrderAndCollectsAllLocals() throws Exception {
        Ast.Program.T program = parse("DeclarationAst",
                "class DeclarationAst {\n" +
                "    void main() {\n" +
                "        printLine();\n" +
                "        int x = 1;\n" +
                "        { float y; }\n" +
                "    }\n" +
                "}\n");
        Ast.Method.MethodSingle main = method(program, "main");

        assertEquals(2, main.getLocals().size());
        assertTrue(main.getStms().get(1) instanceof Ast.Stmt.VarDecl);
        Ast.Stmt.Block block = (Ast.Stmt.Block) main.getStms().get(2);
        assertTrue(block.getStmts().get(0) instanceof Ast.Stmt.VarDecl);
    }

    @Test
    public void semanticAcceptsInitializedAndMixedDeclarations() throws Exception {
        SemanticVisitor semantic = analyze("ValidDeclarations",
                "class ValidDeclarations {\n" +
                "    void main() {\n" +
                "        int x = 1;\n" +
                "        x = x + 1;\n" +
                "        { int y = x + 2; printf(\"%d\", y); }\n" +
                "        int z;\n" +
                "        z = x;\n" +
                "    }\n" +
                "}\n");

        assertTrue(semantic.getErrors().toString(), semantic.passOrNot());
    }

    @Test
    public void semanticRejectsUseBeforeDeclarationAndAfterBlock() throws Exception {
        SemanticVisitor beforeDeclaration = analyze("UseBeforeDeclaration",
                "class UseBeforeDeclaration { void main() { x = 1; int x; } }");
        SemanticVisitor afterBlock = analyze("UseAfterBlock",
                "class UseAfterBlock { void main() { { int x = 1; } printf(\"%d\", x); } }");

        assertContains(beforeDeclaration.getErrors(), "未定义的变量: x");
        assertContains(afterBlock.getErrors(), "未定义的变量: x");
    }

    @Test
    public void semanticRejectsInitializerTypeMismatch() throws Exception {
        SemanticVisitor semantic = analyze("BadInitializer",
                "class BadInitializer { void main() { int x = 1.5; } }");

        assertContains(semantic.getErrors(), "不能用 float 类型初始化 int 类型");
    }

    @Test
    public void semanticRejectsDuplicateLocalsAcrossNestedBlocks() throws Exception {
        Ast.Program.T program = parse("DuplicateBlockLocal",
                "class DuplicateBlockLocal { void main() { int x; { int x; } } }");
        try {
            new SemanticVisitor().visit(program);
            fail("Expected SemanticException");
        } catch (SemanticException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("重复的变量 x"));
        }
    }

    private static SemanticVisitor analyze(String className, String source) throws Exception {
        Ast.Program.T program = parse(className, source);
        SemanticVisitor semantic = SemanticVisitor.collecting();
        semantic.visit(program);
        return semantic;
    }

    private static Ast.Program.T parse(String className, String source) throws Exception {
        return new Parser(new Lexer(writeSource(className, source))).parse();
    }

    private static Ast.Method.MethodSingle method(Ast.Program.T program, String name) {
        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle)
                ((Ast.Program.ProgramSingle) program).getMainClass();
        for (Ast.Method.T candidate : mainClass.getMethods()) {
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) candidate;
            if (name.equals(method.getId())) {
                return method;
            }
        }
        throw new AssertionError("Method not found: " + name);
    }

    private static void assertContains(List<String> errors, String expected) {
        for (String error : errors) {
            if (error.contains(expected)) {
                return;
            }
        }
        throw new AssertionError("Expected '" + expected + "' in " + errors);
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
