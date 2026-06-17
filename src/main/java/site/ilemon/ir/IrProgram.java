package site.ilemon.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * LemonIR 顶层程序表示。
 */
public class IrProgram {
    private String className;
    private List<IrFunction> functions;

    public IrProgram() {
        this.functions = new ArrayList<IrFunction>();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public List<IrFunction> getFunctions() { return functions; }
    
    public void addFunction(IrFunction function) {
        this.functions.add(function);
    }
}
