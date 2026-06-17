package site.ilemon.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * LemonIR 函数。
 */
public class IrFunction {
    private String name;
    private IrType returnType = IrType.VOID;
    private List<IrValue> parameters;
    private List<IrType> parameterTypes;
    private List<IrBlock> blocks;
    
    // 该函数中使用的最大虚拟寄存器编号（用于分配 localDataSize）
    private int maxVRegId = -1;

    public IrFunction(String name) {
        this.name = name;
        this.parameters = new ArrayList<IrValue>();
        this.parameterTypes = new ArrayList<IrType>();
        this.blocks = new ArrayList<IrBlock>();
    }

    public String getName() { return name; }
    public IrType getReturnType() { return returnType; }
    public void setReturnType(IrType returnType) { this.returnType = returnType; }
    
    public List<IrValue> getParameters() { return parameters; }
    public List<IrType> getParameterTypes() { return parameterTypes; }
    public void addParameter(IrValue param) {
        this.parameters.add(param);
        this.parameterTypes.add(param.getType());
        updateMaxVReg(param);
    }

    public List<IrBlock> getBlocks() { return blocks; }
    public void addBlock(IrBlock block) {
        this.blocks.add(block);
    }

    public void updateMaxVReg(IrValue vreg) {
        if (vreg.getKind() == IrValue.Kind.VREG) {
            if (vreg.getId() > maxVRegId) {
                maxVRegId = vreg.getId();
            }
        }
    }

    /**
     * 获取此函数使用的局部变量总数。
     * 由于我们采用 1对1 的虚拟寄存器分配，所需空间即为 maxVRegId + 1。
     */
    public int getUsedVRegCount() {
        return maxVRegId + 1;
    }
}
