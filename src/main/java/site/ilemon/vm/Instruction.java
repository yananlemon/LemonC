package site.ilemon.vm;

import site.ilemon.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * LemonVM 指令。
 *
 * 每条指令由操作码 + 操作数列表组成。
 * 操作数是 Value 对象（可以是字面量、栈索引、指令索引、函数索引等）。
 */
public class Instruction {

    /** 操作码 */
    private final Opcode opcode;

    /** 操作数列表 */
    private final List<Value> operands;

    /** 该指令对应的源码位置，用于运行时错误定位；未知时为 {@link SourceSpan#UNKNOWN}。 */
    private SourceSpan sourceSpan = SourceSpan.UNKNOWN;

    public Instruction(Opcode opcode) {
        this.opcode = opcode;
        this.operands = new ArrayList<Value>();
    }

    public Instruction(Opcode opcode, List<Value> operands) {
        this.opcode = opcode;
        this.operands = operands;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public List<Value> getOperands() {
        return operands;
    }

    public int getOperandCount() {
        return operands.size();
    }

    public SourceSpan getSourceSpan() {
        return sourceSpan;
    }

    public void setSourceSpan(SourceSpan sourceSpan) {
        this.sourceSpan = sourceSpan == null ? SourceSpan.UNKNOWN : sourceSpan;
    }

    /**
     * 获取指定索引的操作数。
     * @throws VmException 如果索引越界
     */
    public Value getOperand(int index) {
        if (index < 0 || index >= operands.size()) {
            throw new VmException("操作数索引越界: " + index
                    + "（指令 " + opcode.getMnemonic() + " 有 " + operands.size() + " 个操作数）");
        }
        return operands.get(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(opcode.getMnemonic());
        for (int i = 0; i < operands.size(); i++) {
            sb.append(i == 0 ? " " : ", ");
            sb.append(operands.get(i));
        }
        return sb.toString();
    }
}
