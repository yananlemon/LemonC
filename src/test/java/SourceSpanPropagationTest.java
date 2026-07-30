import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrBlock;
import site.ilemon.ir.IrFunction;
import site.ilemon.ir.IrInstruction;
import site.ilemon.ir.IrOpcode;
import site.ilemon.ir.IrProgram;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticResult;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.source.SourceSpan;
import site.ilemon.typedast.TypedAst;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceSpanPropagationTest {
    private static final String SOURCE =
            "class SpanSample {\n"
                    + "void main() {\n"
                    + "int value =\n"
                    + "1 +\n"
                    + "2;\n"
                    + "printf(\"%d\\n\", value);\n"
                    + "}\n"
                    + "}\n";

    @Test
    public void lexerTracksEndExclusiveTokenRanges() throws Exception {
        Lexer lexer = new Lexer(writeSource());
        lexer.lexicalAnalysis();

        Token classToken = findToken(lexer, TokenKind.Class);
        assertSpan(classToken.getSourceSpan(), 1, 1, 1, 6);

        Token stringToken = findToken(lexer, TokenKind.StringLiteral);
        assertEquals("%d\n", stringToken.getLexeme());
        assertSpan(stringToken.getSourceSpan(), 6, 8, 6, 14);
        assertEquals(6, stringToken.getEndLineNumber());
        assertEquals(14, stringToken.getEndColumnNumber());
    }

    @Test
    public void parserSemanticAndOptimizerPreserveMultilineRanges() throws Exception {
        Ast.Program.Base syntax = new Parser(new Lexer(writeSource())).parse();
        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle)
                ((Ast.Program.ProgramSingle) syntax).getMainClass();
        Ast.Method.MethodSingle sourceMain =
                (Ast.Method.MethodSingle) mainClass.getMethods().get(0);
        Ast.Stmt.VarDecl sourceDeclaration =
                (Ast.Stmt.VarDecl) sourceMain.getStms().get(0);
        Ast.Expr.Add sourceInitializer =
                (Ast.Expr.Add) sourceDeclaration.getInitializer();

        assertSpan(syntax.getSourceSpan(), 1, 1, 8, 2);
        assertSpan(sourceMain.getSourceSpan(), 2, 1, 7, 2);
        assertSpan(sourceDeclaration.getSourceSpan(), 3, 1, 5, 3);
        assertSpan(sourceInitializer.getSourceSpan(), 4, 1, 5, 2);

        SemanticResult semantic = new SemanticVisitor().analyze(syntax);
        assertTrue(semantic.isSuccess());
        TypedAst.VarDecl typedDeclaration = (TypedAst.VarDecl)
                semantic.getProgram().getMethods().get(0).getStatements().get(0);
        assertSpan(typedDeclaration.getSourceSpan(), 3, 1, 5, 3);
        assertSpan(typedDeclaration.getDeclaration().getSourceSpan(), 3, 1, 3, 10);
        assertSpan(typedDeclaration.getDeclaration().getSymbol().getSourceSpan(),
                3, 1, 3, 10);
        assertSpan(typedDeclaration.getInitializer().getSourceSpan(), 4, 1, 5, 2);

        TypedAst.Program optimized = new AstOptimizer().optimize(semantic.getProgram());
        TypedAst.VarDecl optimizedDeclaration = (TypedAst.VarDecl)
                optimized.getMethods().get(0).getStatements().get(0);
        assertTrue(optimizedDeclaration.getInitializer() instanceof TypedAst.IntLiteral);
        assertSpan(optimizedDeclaration.getInitializer().getSourceSpan(), 4, 1, 5, 2);
    }

    @Test
    public void lemonIrInstructionsCarryTypedAstRanges() throws Exception {
        Ast.Program.Base syntax = new Parser(new Lexer(writeSource())).parse();
        TypedAst.Program typed = new SemanticVisitor().analyze(syntax).getProgram();
        IrProgram ir = new AstToIrTranslator().translate(new AstOptimizer().optimize(typed));

        IrInstruction move = findInstruction(ir, IrOpcode.MOV);
        assertSpan(move.getSourceSpan(), 3, 1, 5, 3);

        IrInstruction ret = findInstruction(ir, IrOpcode.RET);
        assertSpan(ret.getSourceSpan(), 2, 1, 7, 2);

        for (IrFunction function : ir.getFunctions()) {
            for (IrBlock block : function.getBlocks()) {
                for (IrInstruction instruction : block.getInstructions()) {
                    assertTrue("generated instruction must have a source span: " + instruction,
                            instruction.getSourceSpan().isKnown());
                }
            }
        }
    }

    private static File writeSource() throws Exception {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, "SpanSample.lemon");
        Files.write(file.toPath(), SOURCE.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file;
    }

    private static Token findToken(Lexer lexer, TokenKind kind) {
        for (Token token : lexer.getTokens()) {
            if (token.getKind() == kind) {
                return token;
            }
        }
        throw new AssertionError("missing token " + kind);
    }

    private static IrInstruction findInstruction(IrProgram program, IrOpcode opcode) {
        for (IrFunction function : program.getFunctions()) {
            for (IrBlock block : function.getBlocks()) {
                for (IrInstruction instruction : block.getInstructions()) {
                    if (instruction.getOpcode() == opcode) {
                        return instruction;
                    }
                }
            }
        }
        throw new AssertionError("missing instruction " + opcode);
    }

    private static void assertSpan(SourceSpan span, int startLine, int startColumn,
                                   int endLine, int endColumn) {
        assertEquals(startLine, span.getStartLine());
        assertEquals(startColumn, span.getStartColumn());
        assertEquals(endLine, span.getEndLine());
        assertEquals(endColumn, span.getEndColumn());
    }
}
