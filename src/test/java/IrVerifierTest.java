import org.junit.Test;
import site.ilemon.exception.CompilerException;
import site.ilemon.ir.IrBlock;
import site.ilemon.ir.IrFunction;
import site.ilemon.ir.IrInstruction;
import site.ilemon.ir.IrOpcode;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrType;
import site.ilemon.ir.IrValue;
import site.ilemon.ir.IrVerifier;

public class IrVerifierTest {

    @Test
    public void validMinimalProgramPasses() {
        IrProgram program = minimalProgram();

        IrVerifier.verify(program);
    }

    @Test(expected = CompilerException.class)
    public void rejectsUnresolvedBranchLabel() {
        IrProgram program = minimalProgram();
        IrInstruction jump = new IrInstruction(IrOpcode.JMP);
        jump.setLabelTarget("missing");
        mainBlock(program).getInstructions().add(0, jump);

        IrVerifier.verify(program);
    }

    @Test(expected = CompilerException.class)
    public void rejectsCallArgumentCountMismatch() {
        IrProgram program = minimalProgram();
        IrFunction add = new IrFunction("id");
        add.setReturnType(IrType.INT);
        add.addParameter(IrValue.vreg(0, IrType.INT));
        IrBlock addBlock = new IrBlock("id_entry");
        IrInstruction addRet = new IrInstruction(IrOpcode.RET);
        addRet.addOperand(IrValue.vreg(0, IrType.INT));
        addBlock.addInstruction(addRet);
        add.addBlock(addBlock);
        program.addFunction(add);

        IrInstruction call = new IrInstruction(IrOpcode.CALL);
        call.setType(IrType.INT);
        call.setFuncTarget("id");
        call.setResult(IrValue.vreg(0, IrType.INT));
        mainBlock(program).getInstructions().add(0, call);

        IrVerifier.verify(program);
    }

    @Test(expected = CompilerException.class)
    public void rejectsArrayAccessWithNonIntIndex() {
        IrProgram program = minimalProgram();
        IrInstruction arrGet = new IrInstruction(IrOpcode.ARR_GET);
        arrGet.setType(IrType.INT);
        arrGet.setResult(IrValue.vreg(2, IrType.INT));
        arrGet.addOperand(IrValue.vreg(0, IrType.INT_ARRAY));
        arrGet.addOperand(IrValue.vreg(1, IrType.FLOAT));
        mainBlock(program).getInstructions().add(0, arrGet);

        IrVerifier.verify(program);
    }

    @Test(expected = CompilerException.class)
    public void rejectsMainWithLemonParameter() {
        IrProgram program = minimalProgram();
        program.getFunctions().get(0).addParameter(IrValue.vreg(0, IrType.INT));

        IrVerifier.verify(program);
    }

    private IrProgram minimalProgram() {
        IrProgram program = new IrProgram();
        program.setClassName("VerifierProgram");
        IrFunction main = new IrFunction("main");
        main.setReturnType(IrType.VOID);
        IrBlock block = new IrBlock("main_entry");
        block.addInstruction(new IrInstruction(IrOpcode.RET));
        main.addBlock(block);
        program.addFunction(main);
        return program;
    }

    private IrBlock mainBlock(IrProgram program) {
        return program.getFunctions().get(0).getBlocks().get(0);
    }
}
