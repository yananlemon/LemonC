package site.ilemon.vm;

/**
 * LemonVM 运行时值 — 带类型标签的联合体。
 *
 * 设计参考 XVM 的 Value 结构体：类型信息存在 Value 上，不在指令名上。
 * 一条 Add 指令在运行时检查 Value 的类型标签来决定做整数加法还是浮点加法。
 *
 * Java 不支持 union，所以用多个字段模拟。同一时刻只有一个字段有效，
 * 由 {@link #type} 决定哪个字段有意义。
 */
public class Value {

    /**
     * Value 的类型标签。
     */
    public enum Type {
        /** 未初始化 / 空值 */
        NULL,
        /** 32 位整数 */
        INT,
        /** 32 位浮点数 */
        FLOAT,
        /** 64 位浮点数 */
        DOUBLE,
        /** 布尔值 */
        BOOL,
        /** 字符串（用于 printf 格式串片段） */
        STRING,
        /** 数组引用（堆中的索引） */
        ARRAY_REF,
        /** 指令索引（用于跳转目标和返回地址） */
        INSTR_INDEX,
        /** 函数索引（用于 Call 指令） */
        FUNC_INDEX,
        /** 栈帧标记（用于标识栈帧边界，携带函数索引和旧 FrameIndex） */
        STACK_FRAME_MARKER
    }

    /** 类型标签 */
    public Type type;

    // ---- 值字段（同一时刻只有一个有效） ----
    public int intValue;
    public float floatValue;
    public double doubleValue;
    public boolean boolValue;
    public String stringValue;
    public int arrayRef;       // 堆中的数组 ID
    public int instrIndex;     // 指令流中的位置
    public int funcIndex;      // 函数表中的索引
    public int oldFrameIndex;  // 旧的栈帧索引（用于 Ret 时恢复）

    // ---- 操作数寻址标记 ----
    // 类似 XVM 的 OP_TYPE_ABS_STACK_INDEX / OP_TYPE_REL_STACK_INDEX / OP_TYPE_REG
    // 用于在运行时区分"整数字面量 42"和"栈索引 -2"
    /** 标记此操作数为栈引用（intValue 作为栈索引） */
    public boolean isStackRef;
    /** 标记此操作数为 _RetVal 寄存器引用 */
    public boolean isRetValRef;

    /** 创建 NULL 值 */
    public Value() {
        this.type = Type.NULL;
    }

    // ---- 工厂方法 ----

    public static Value ofInt(int value) {
        Value v = new Value();
        v.type = Type.INT;
        v.intValue = value;
        return v;
    }

    public static Value ofFloat(float value) {
        Value v = new Value();
        v.type = Type.FLOAT;
        v.floatValue = value;
        return v;
    }

    public static Value ofDouble(double value) {
        Value v = new Value();
        v.type = Type.DOUBLE;
        v.doubleValue = value;
        return v;
    }

    public static Value ofBool(boolean value) {
        Value v = new Value();
        v.type = Type.BOOL;
        v.boolValue = value;
        return v;
    }

    public static Value ofString(String value) {
        Value v = new Value();
        v.type = Type.STRING;
        v.stringValue = value;
        return v;
    }

    public static Value ofArrayRef(int ref) {
        Value v = new Value();
        v.type = Type.ARRAY_REF;
        v.arrayRef = ref;
        return v;
    }

    public static Value ofInstrIndex(int index) {
        Value v = new Value();
        v.type = Type.INSTR_INDEX;
        v.instrIndex = index;
        return v;
    }

    public static Value ofFuncIndex(int index) {
        Value v = new Value();
        v.type = Type.FUNC_INDEX;
        v.funcIndex = index;
        return v;
    }

    /**
     * 创建栈帧标记，保存函数索引和旧的 FrameIndex。
     * 参考 XVM: CallFunc() 中将 FuncIndex 和 OldFrameIndex 写入栈帧顶部。
     */
    public static Value ofStackFrameMarker(int funcIndex, int oldFrameIndex) {
        Value v = new Value();
        v.type = Type.STACK_FRAME_MARKER;
        v.funcIndex = funcIndex;
        v.oldFrameIndex = oldFrameIndex;
        return v;
    }

    /**
     * 创建栈引用操作数（intValue 作为栈索引）。
     * 类似 XVM 的 OP_TYPE_ABS_STACK_INDEX / OP_TYPE_REL_STACK_INDEX。
     */
    public static Value ofStackRef(int stackIndex) {
        Value v = new Value();
        v.type = Type.INT;
        v.intValue = stackIndex;
        v.isStackRef = true;
        return v;
    }

    /**
     * 创建 _RetVal 寄存器引用操作数。
     */
    public static Value ofRetValRef() {
        Value v = new Value();
        v.isRetValRef = true;
        return v;
    }

    /**
     * 将当前值强制转换为 int。
     * - INT → 原样返回
     * - FLOAT → 截断为 int
     * - DOUBLE → 截断为 int
     * - BOOL → true=1, false=0
     * - 其他 → 0
     */
    public int coerceToInt() {
        switch (type) {
            case INT:    return intValue;
            case FLOAT:  return (int) floatValue;
            case DOUBLE: return (int) doubleValue;
            case BOOL:   return boolValue ? 1 : 0;
            default:     return 0;
        }
    }

    /**
     * 将当前值强制转换为 float。
     */
    public float coerceToFloat() {
        switch (type) {
            case INT:    return (float) intValue;
            case FLOAT:  return floatValue;
            case DOUBLE: return (float) doubleValue;
            case BOOL:   return boolValue ? 1.0f : 0.0f;
            default:     return 0.0f;
        }
    }

    /**
     * 将当前值强制转换为 double。
     */
    public double coerceToDouble() {
        switch (type) {
            case INT:    return (double) intValue;
            case FLOAT:  return (double) floatValue;
            case DOUBLE: return doubleValue;
            case BOOL:   return boolValue ? 1.0 : 0.0;
            default:     return 0.0;
        }
    }

    /**
     * 深拷贝：将 source 的所有字段复制到当前 Value。
     * 参考 XVM 的 CopyValue()。字符串做值拷贝（Java 中 String 不可变，直接引用即可）。
     */
    public void copyFrom(Value source) {
        this.type = source.type;
        this.intValue = source.intValue;
        this.floatValue = source.floatValue;
        this.doubleValue = source.doubleValue;
        this.boolValue = source.boolValue;
        this.stringValue = source.stringValue;
        this.arrayRef = source.arrayRef;
        this.instrIndex = source.instrIndex;
        this.funcIndex = source.funcIndex;
        this.oldFrameIndex = source.oldFrameIndex;
        this.isStackRef = source.isStackRef;
        this.isRetValRef = source.isRetValRef;
    }

    /**
     * 创建当前值的深拷贝。
     */
    public Value copy() {
        Value v = new Value();
        v.copyFrom(this);
        return v;
    }

    @Override
    public String toString() {
        switch (type) {
            case NULL:               return "null";
            case INT:                return String.valueOf(intValue);
            case FLOAT:              return String.valueOf(floatValue);
            case DOUBLE:             return String.valueOf(doubleValue);
            case BOOL:               return String.valueOf(boolValue);
            case STRING:             return stringValue;
            case ARRAY_REF:          return "ArrayRef(" + arrayRef + ")";
            case INSTR_INDEX:        return "InstrIndex(" + instrIndex + ")";
            case FUNC_INDEX:         return "FuncIndex(" + funcIndex + ")";
            case STACK_FRAME_MARKER: return "FrameMarker(func=" + funcIndex + ",oldFrame=" + oldFrameIndex + ")";
            default:                 return "?";
        }
    }
}
