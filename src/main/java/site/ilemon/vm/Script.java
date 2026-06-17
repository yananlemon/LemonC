package site.ilemon.vm;

import java.util.HashMap;
import java.util.Map;

/**
 * LemonVM 脚本 — 封装一个完整的可执行程序。
 *
 * 参考 XVM 的 Script 结构体：包含指令流、运行时栈、函数表、堆和 _RetVal 寄存器。
 */
public class Script {

    /** 指令流 */
    private Instruction[] instrStream;

    /** 运行时栈 */
    private RuntimeStack stack;

    /** 函数表（按名称查找） */
    private Map<String, VmFunction> funcTableByName;

    /** 函数表（按索引查找） */
    private VmFunction[] funcTableByIndex;

    /** 堆 */
    private VmHeap heap;

    /** _RetVal 寄存器：函数返回值 */
    private Value retVal;

    /** 全局变量数量 */
    private int globalDataSize;

    /** 主函数名 */
    private String mainFuncName;

    /** 指令指针 (Program Counter) */
    private int pc;

    /** 是否正在运行 */
    private boolean running;

    public Script() {
        this.stack = new RuntimeStack();
        this.heap = new VmHeap();
        this.retVal = new Value();
        this.funcTableByName = new HashMap<String, VmFunction>();
        this.pc = 0;
        this.running = false;
        this.globalDataSize = 0;
        this.mainFuncName = "main";
    }

    public void resetRuntimeState() {
        this.stack = new RuntimeStack();
        this.heap = new VmHeap();
        this.retVal = new Value();
        this.pc = 0;
        this.running = false;
    }

    // ---- 指令流 ----

    public Instruction[] getInstrStream() { return instrStream; }
    public void setInstrStream(Instruction[] instrStream) { this.instrStream = instrStream; }

    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }
    public void incPc() { this.pc++; }

    public Instruction getCurrentInstr() {
        if (pc < 0 || pc >= instrStream.length) {
            throw new VmException("指令指针越界: pc=" + pc + ", instrCount=" + instrStream.length);
        }
        return instrStream[pc];
    }

    // ---- 栈 ----

    public RuntimeStack getStack() { return stack; }
    public void setStack(RuntimeStack stack) { this.stack = stack; }

    // ---- 函数表 ----

    public void setFuncTable(VmFunction[] funcs) {
        this.funcTableByIndex = funcs;
        this.funcTableByName = new HashMap<String, VmFunction>();
        for (int i = 0; i < funcs.length; i++) {
            funcTableByName.put(funcs[i].getName(), funcs[i]);
        }
    }

    public VmFunction getFunc(int index) {
        if (index < 0 || index >= funcTableByIndex.length) {
            throw new VmException("函数索引越界: " + index);
        }
        return funcTableByIndex[index];
    }

    public VmFunction getFunc(String name) {
        VmFunction func = funcTableByName.get(name);
        if (func == null) {
            throw new VmException("未找到函数: " + name);
        }
        return func;
    }

    public int getFuncIndex(String name) {
        for (int i = 0; i < funcTableByIndex.length; i++) {
            if (funcTableByIndex[i].getName().equals(name)) {
                return i;
            }
        }
        throw new VmException("未找到函数: " + name);
    }

    public int getFuncCount() {
        return funcTableByIndex == null ? 0 : funcTableByIndex.length;
    }

    // ---- 堆 ----

    public VmHeap getHeap() { return heap; }

    // ---- _RetVal ----

    public Value getRetVal() { return retVal; }
    public void setRetVal(Value retVal) { this.retVal.copyFrom(retVal); }

    // ---- 全局数据 ----

    public int getGlobalDataSize() { return globalDataSize; }
    public void setGlobalDataSize(int globalDataSize) { this.globalDataSize = globalDataSize; }

    // ---- 主函数 ----

    public String getMainFuncName() { return mainFuncName; }
    public void setMainFuncName(String mainFuncName) { this.mainFuncName = mainFuncName; }

    // ---- 运行状态 ----

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
}
