package site.ilemon.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * LemonIR 三地址码指令。
 */
public class IrInstruction {
    private IrOpcode opcode;
    private IrType type;           // 操作的类型（如需要）
    private IrValue result;        // 目标寄存器（可为空，如跳转、返回）
    private List<IrValue> operands;// 源操作数列表
    private String labelTarget;    // 用于跳转的标签目标
    private String funcTarget;     // 用于调用的函数名

    public IrInstruction(IrOpcode opcode) {
        this.opcode = opcode;
        this.operands = new ArrayList<IrValue>();
    }

    public IrOpcode getOpcode() { return opcode; }
    public void setOpcode(IrOpcode opcode) { this.opcode = opcode; }

    public IrType getType() { return type; }
    public void setType(IrType type) { this.type = type; }

    public IrValue getResult() { return result; }
    public void setResult(IrValue result) { this.result = result; }

    public List<IrValue> getOperands() { return operands; }
    public void setOperands(List<IrValue> operands) { this.operands = operands; }

    public void addOperand(IrValue operand) {
        this.operands.add(operand);
    }

    public String getLabelTarget() { return labelTarget; }
    public void setLabelTarget(String labelTarget) { this.labelTarget = labelTarget; }

    public String getFuncTarget() { return funcTarget; }
    public void setFuncTarget(String funcTarget) { this.funcTarget = funcTarget; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (result != null) {
            sb.append(result.toString()).append(" = ");
        }
        sb.append(opcode.name());
        
        if (type != null) {
            sb.append("_").append(type.name());
        }

        if (funcTarget != null) {
            sb.append(" ").append(funcTarget);
        }

        if (!operands.isEmpty()) {
            sb.append(" ");
            for (int i = 0; i < operands.size(); i++) {
                sb.append(operands.get(i).toString());
                if (i < operands.size() - 1) sb.append(", ");
            }
        }

        if (labelTarget != null) {
            if (!operands.isEmpty() || funcTarget != null) sb.append(", ");
            else sb.append(" ");
            sb.append(labelTarget);
        }

        return sb.toString();
    }
}
