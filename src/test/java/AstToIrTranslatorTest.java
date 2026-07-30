import org.junit.Test;
import site.ilemon.exception.CompilerException;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrBlock;
import site.ilemon.ir.IrInstruction;
import site.ilemon.ir.IrOpcode;
import site.ilemon.ir.IrProgram;
import site.ilemon.typedast.TypedAst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AstToIrTranslatorTest {
    @Test
    public void lowersUnaryMinusToNegRatherThanSubtractFromZero() {
        // 0 - x 在 IEEE-754 下丢失负零的符号，所以取负必须降为 NEG。
        TypedAst.Symbol value = new TypedAst.Symbol("value", TypedAst.Type.FLOAT,
                TypedAst.Symbol.Kind.LOCAL, 1);
        TypedAst.Symbol negated = new TypedAst.Symbol("negated", TypedAst.Type.FLOAT,
                TypedAst.Symbol.Kind.LOCAL, 1);
        TypedAst.Stmt assignment = new TypedAst.Assign(negated,
                new TypedAst.UnaryMinus(TypedAst.Type.FLOAT,
                        new TypedAst.Id(value, TypedAst.Type.FLOAT, 1), 1), 1);
        TypedAst.MethodSymbol mainSymbol = new TypedAst.MethodSymbol("main", TypedAst.Type.VOID,
                Collections.<TypedAst.Type>emptyList(), 1);
        TypedAst.Method main = new TypedAst.Method(mainSymbol,
                Collections.<TypedAst.Declaration>emptyList(),
                Arrays.asList(new TypedAst.Declaration(value, 1),
                        new TypedAst.Declaration(negated, 1)),
                Collections.singletonList(assignment), 1);
        IrProgram program = new AstToIrTranslator().translate(
                new TypedAst.Program("UnaryMinusLowering", Collections.singletonList(main)));

        List<IrOpcode> opcodes = new ArrayList<IrOpcode>();
        for (IrBlock block : program.getFunctions().get(0).getBlocks()) {
            for (IrInstruction instruction : block.getInstructions()) {
                opcodes.add(instruction.getOpcode());
            }
        }
        assertTrue("expected a NEG instruction, got " + opcodes, opcodes.contains(IrOpcode.NEG));
        assertFalse("unary minus must not lower to SUB, got " + opcodes,
                opcodes.contains(IrOpcode.SUB));
    }

    @Test
    public void rejectsSymbolMissingFromTypedMethodDeclarations() {
        TypedAst.Symbol missing = new TypedAst.Symbol("missing", TypedAst.Type.INT,
                TypedAst.Symbol.Kind.LOCAL, 1);
        TypedAst.Assign assignment = new TypedAst.Assign(missing,
                new TypedAst.IntLiteral(1, "1", 1), 1);
        TypedAst.MethodSymbol mainSymbol = new TypedAst.MethodSymbol("main", TypedAst.Type.VOID,
                Collections.<TypedAst.Type>emptyList(), 1);
        TypedAst.Method main = new TypedAst.Method(mainSymbol,
                Collections.<TypedAst.Declaration>emptyList(),
                Collections.<TypedAst.Declaration>emptyList(),
                Collections.<TypedAst.Stmt>singletonList(assignment), 1);
        TypedAst.Program program = new TypedAst.Program("MissingIrVariable",
                Collections.singletonList(main));

        try {
            new AstToIrTranslator().translate(program);
            fail("Expected IR translation to reject an unregistered symbol");
        } catch (CompilerException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("missing"));
        }
    }
}
