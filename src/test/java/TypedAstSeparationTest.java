import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticResult;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.typedast.TypedAst;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TypedAstSeparationTest {
    @Test
    public void semanticAnalysisBuildsResolvedTreeWithoutAnnotatingParserNodes() throws Exception {
        Ast.Program.Base syntax = parse("TypedBoundary",
                "class TypedBoundary { void main() { int x; x = 1; printf(\"%d\", x); } }");
        SemanticResult result = new SemanticVisitor().analyze(syntax);

        assertTrue(result.isSuccess());
        assertFalse(hasMethod(Ast.Expr.Id.class, "getType"));
        assertFalse(hasMethod(Ast.Expr.Id.class, "setType"));
        assertFalse(hasMethod(Ast.Expr.Call.class, "getReturnType"));
        assertFalse(hasMethod(Ast.Expr.ArrayAccess.class, "getElementType"));

        TypedAst.Method main = result.getProgram().getMethods().get(0);
        TypedAst.VarDecl declaration = (TypedAst.VarDecl) main.getStatements().get(0);
        TypedAst.Assign assignment = (TypedAst.Assign) main.getStatements().get(1);
        TypedAst.Printf printf = (TypedAst.Printf) main.getStatements().get(2);
        TypedAst.Id read = (TypedAst.Id) printf.getExpressions().get(0);

        assertSame(TypedAst.Type.INT, declaration.getDeclaration().getType());
        assertSame(declaration.getDeclaration().getSymbol(), assignment.getTarget());
        assertSame(declaration.getDeclaration().getSymbol(), read.getSymbol());
        assertSame(TypedAst.Type.INT, read.getType());
    }

    @Test
    public void errorTypeIsConfinedToTypedSemanticResult() throws Exception {
        Ast.Program.Base syntax = parse("TypedError",
                "class TypedError { void main() { int x; x = missing + 1; } }");
        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle)
                ((Ast.Program.ProgramSingle) syntax).getMainClass();
        Ast.Method.MethodSingle main = (Ast.Method.MethodSingle) mainClass.getMethods().get(0);
        Ast.Expr.Add add = (Ast.Expr.Add) ((Ast.Stmt.Assign) main.getStms().get(1)).getExpr();
        Ast.Expr.Id missing = (Ast.Expr.Id) add.getLeft();

        SemanticResult result = SemanticVisitor.collecting().analyze(syntax);

        assertFalse(result.isSuccess());
        assertSame(TypedAst.Type.ERROR, result.getExpressionType(missing));
        for (Class<?> nested : Ast.Type.class.getDeclaredClasses()) {
            assertFalse("Parser AST must not define ErrorType", "Error".equals(nested.getSimpleName()));
        }
    }

    @Test
    public void typedAstCollectionsAreImmutableAndMiddleEndRequiresTypedProgram() throws Exception {
        SemanticResult result = new SemanticVisitor().analyze(parse("ImmutableTyped",
                "class ImmutableTyped { void main() {} }"));
        try {
            result.getProgram().getMethods().clear();
            fail("Typed-AST method list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected: all Typed-AST ownership lists are immutable snapshots.
        }

        assertEquals(TypedAst.Program.class,
                AstOptimizer.class.getMethod("optimize", TypedAst.Program.class).getParameterTypes()[0]);
        assertEquals(TypedAst.Program.class,
                AstToIrTranslator.class.getMethod("translate", TypedAst.Program.class).getParameterTypes()[0]);

        TypedAst.Symbol intSymbol = new TypedAst.Symbol("x", TypedAst.Type.INT,
                TypedAst.Symbol.Kind.LOCAL, 1);
        try {
            new TypedAst.Id(intSymbol, TypedAst.Type.FLOAT, 1);
            fail("Resolved symbol and expression types must agree");
        } catch (IllegalArgumentException expected) {
            // Expected: only the resolved type or ErrorType is permitted.
        }

        try {
            new TypedAst.Program("Invalid",
                    Collections.singletonList((TypedAst.Method) null));
            fail("Typed-AST ownership lists must reject null nodes");
        } catch (NullPointerException expected) {
            // Expected: malformed typed trees fail at their construction boundary.
        }

        TypedAst.Type unsizedArray = TypedAst.Type.array(
                TypedAst.Type.Kind.INT_ARRAY, -1);
        assertFalse(unsizedArray.hasKnownArraySize());
        assertTrue(TypedAst.Type.array(
                TypedAst.Type.Kind.INT_ARRAY, 4).hasKnownArraySize());
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (name.equals(method.getName())) return true;
        }
        return false;
    }

    private static Ast.Program.Base parse(String className, String source) throws Exception {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, className + ".lemon");
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return new Parser(new Lexer(file)).parse();
    }
}
