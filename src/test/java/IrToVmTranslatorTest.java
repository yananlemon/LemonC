import org.junit.Test;
import site.ilemon.ir.IrBlock;
import site.ilemon.ir.IrFunction;
import site.ilemon.ir.IrInstruction;
import site.ilemon.ir.IrOpcode;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrType;
import site.ilemon.ir.IrToVmTranslator;
import site.ilemon.ir.IrValue;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;

import static org.junit.Assert.assertEquals;

public class IrToVmTranslatorTest {

    @Test
    public void duplicateLabelsAreScopedToTheirFunction() {
        Script script = new IrToVmTranslator(duplicateLabelProgram()).translate();

        assertEquals("main", new LemonVm(script).run());
    }

    @Test
    public void translateCanBeCalledTwiceOnSameInstance() {
        IrToVmTranslator translator = new IrToVmTranslator(duplicateLabelProgram());

        Script first = translator.translate();
        Script second = translator.translate();

        assertEquals(first.getInstrStream().length, second.getInstrStream().length);
        assertEquals("main", new LemonVm(first).run());
        assertEquals("main", new LemonVm(second).run());
    }

    private IrProgram duplicateLabelProgram() {
        IrProgram program = new IrProgram();
        program.setClassName("DuplicateLabelProgram");
        program.addFunction(function("main", "main"));
        program.addFunction(function("helper", "helper"));
        return program;
    }

    private IrFunction function(String name, String message) {
        IrFunction function = new IrFunction(name);
        function.setReturnType(IrType.VOID);

        IrBlock entry = new IrBlock("entry");
        entry.addInstruction(jump("target"));
        function.addBlock(entry);

        IrBlock target = new IrBlock("target");
        target.addInstruction(print(message));
        target.addInstruction(ret());
        function.addBlock(target);

        return function;
    }

    private IrInstruction jump(String label) {
        IrInstruction instruction = new IrInstruction(IrOpcode.JMP);
        instruction.setLabelTarget(label);
        return instruction;
    }

    private IrInstruction print(String value) {
        IrInstruction instruction = new IrInstruction(IrOpcode.PRINT);
        instruction.addOperand(IrValue.constString(value));
        return instruction;
    }

    private IrInstruction ret() {
        return new IrInstruction(IrOpcode.RET);
    }
}
