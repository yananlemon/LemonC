package site.ilemon.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * LemonIR 基本块。
 * 一段按顺序执行的指令，只有一个入口和一个出口。
 */
public class IrBlock {
    private String label;
    private List<IrInstruction> instructions;

    public IrBlock(String label) {
        this.label = label;
        this.instructions = new ArrayList<IrInstruction>();
    }

    public String getLabel() { return label; }
    
    public List<IrInstruction> getInstructions() { return instructions; }
    
    public void addInstruction(IrInstruction instr) {
        this.instructions.add(instr);
    }
}
