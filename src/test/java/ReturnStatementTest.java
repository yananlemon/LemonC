import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReturnStatementTest {

    @Test
    public void parserRepresentsEmptyReturnWithNullExpression() throws Exception {
        Ast.Program.T program = parse("EmptyReturnAst",
                "class EmptyReturnAst { void main() { return; } }");
        Ast.Method.MethodSingle main = method(program, "main");
        Ast.Stmt.Return returnStmt = (Ast.Stmt.Return) main.getStms().get(0);

        assertNull(returnStmt.getExpr());
    }

    @Test
    public void semanticAcceptsEmptyReturnInVoidAndMainMethods() throws Exception {
        SemanticVisitor semantic = analyze("ValidEmptyReturns",
                "class ValidEmptyReturns {\n" +
                "    void main() { helper(); return; }\n" +
                "    void helper() { return; }\n" +
                "}\n");

        assertTrue(semantic.getErrors().toString(), semantic.passOrNot());
    }

    @Test
    public void semanticRejectsReturnShapeThatDoesNotMatchMethodType() throws Exception {
        SemanticVisitor missingValue = analyze("MissingReturnValue",
                "class MissingReturnValue {\n" +
                "    void main() {}\n" +
                "    int value() { return; }\n" +
                "}\n");
        SemanticVisitor unexpectedValue = analyze("UnexpectedReturnValue",
                "class UnexpectedReturnValue {\n" +
                "    void main() {}\n" +
                "    void helper() { return 1; }\n" +
                "}\n");

        assertContains(missingValue.getErrors(), "必须返回");
        assertContains(unexpectedValue.getErrors(), "不能返回值");
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
