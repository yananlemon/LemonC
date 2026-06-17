package site.ilemon.vm;

/**
 * LemonVM 函数元数据。
 *
 * 参考 XVM 的 Func 结构体。每个函数记录入口点、参数数量和局部变量数量，
 * VM 在 Call 时根据这些信息分配栈帧。
 */
public class VmFunction {

    /** 函数名称 */
    private final String name;

    /** 入口点（指令流中的索引） */
    private final int entryPoint;

    /** 参数数量 */
    private final int paramCount;

    /** 局部变量数量（不含参数） */
    private final int localDataSize;

    /**
     * 栈帧大小 = 参数数量 + 1 (返回地址) + 局部变量数量 + 1 (栈帧标记)
     * 参考 XVM: iStackFrameSize = iParamCount + 1 + iLocalDataSize
     */
    private final int stackFrameSize;

    public VmFunction(String name, int entryPoint, int paramCount, int localDataSize) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.paramCount = paramCount;
        this.localDataSize = localDataSize;
        // 与 XVM 一致: stackFrameSize = paramCount + 1(返回地址) + localDataSize
        this.stackFrameSize = paramCount + 1 + localDataSize;
    }

    public String getName() { return name; }
    public int getEntryPoint() { return entryPoint; }
    public int getParamCount() { return paramCount; }
    public int getLocalDataSize() { return localDataSize; }
    public int getStackFrameSize() { return stackFrameSize; }

    @Override
    public String toString() {
        return "VmFunction{name='" + name + "', entry=" + entryPoint
                + ", params=" + paramCount + ", locals=" + localDataSize + "}";
    }
}
