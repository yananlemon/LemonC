package site.ilemon.vm;

/**
 * LemonVM 指令操作码枚举。
 *
 * 设计参考 XVM (Game Scripting Mastery)：
 * - 类型无关指令（一条 Add 通吃 int/float/double）
 * - 二地址格式（Dest = Dest op Source）
 * - 比较+跳转合一（Je/Jg/Jl 等将比较和跳转合为一条指令）
 *
 * 共 28 条指令。
 */
public enum Opcode {

    // ---- 数据移动 (1) ----
    /** Mov Dest, Source — 将 Source 的值复制到 Dest */
    MOV(0, "Mov", 2),

    // ---- 算术运算 (7) ----
    /** Add Dest, Source — Dest = Dest + Source */
    ADD(1, "Add", 2),
    /** Sub Dest, Source — Dest = Dest - Source */
    SUB(2, "Sub", 2),
    /** Mul Dest, Source — Dest = Dest * Source */
    MUL(3, "Mul", 2),
    /** Div Dest, Source — Dest = Dest / Source */
    DIV(4, "Div", 2),
    /** Mod Dest, Source — Dest = Dest % Source (仅整数) */
    MOD(5, "Mod", 2),
    /** Neg Dest — Dest = -Dest */
    NEG(6, "Neg", 1),
    /** Inc Dest — Dest = Dest + 1 */
    INC(7, "Inc", 1),
    /** Dec Dest — Dest = Dest - 1 */
    DEC(8, "Dec", 1),

    // ---- 逻辑运算 (1) ----
    /** Not Dest — Dest = !Dest (布尔取反) 或 ~Dest (整数按位取反) */
    NOT(9, "Not", 1),

    // ---- 条件跳转 (7) ----
    /** Jmp Target — 无条件跳转 */
    JMP(10, "Jmp", 1),
    /** Je Op0, Op1, Target — if Op0 == Op1 then jump */
    JE(11, "Je", 3),
    /** Jne Op0, Op1, Target — if Op0 != Op1 then jump */
    JNE(12, "Jne", 3),
    /** Jg Op0, Op1, Target — if Op0 > Op1 then jump */
    JG(13, "Jg", 3),
    /** Jl Op0, Op1, Target — if Op0 < Op1 then jump */
    JL(14, "Jl", 3),
    /** Jge Op0, Op1, Target — if Op0 >= Op1 then jump */
    JGE(15, "Jge", 3),
    /** Jle Op0, Op1, Target — if Op0 <= Op1 then jump */
    JLE(16, "Jle", 3),

    // ---- 栈操作与函数调用 (4) ----
    /** Push Source — 将 Source 值压入栈顶 */
    PUSH(17, "Push", 1),
    /** Pop Dest — 弹出栈顶值到 Dest */
    POP(18, "Pop", 1),
    /** Call FuncIndex — 调用函数 */
    CALL(19, "Call", 1),
    /** Ret — 从函数返回 */
    RET(20, "Ret", 0),

    // ---- 数组操作 (4) ----
    /** NewArr Dest, Size — 在堆上分配数组，引用存入 Dest */
    NEW_ARR(21, "NewArr", 2),
    /** ArrGet Dest, ArrRef, Index — Dest = Array[Index] */
    ARR_GET(22, "ArrGet", 3),
    /** ArrSet ArrRef, Index, Source — Array[Index] = Source */
    ARR_SET(23, "ArrSet", 3),
    /** ArrLen Dest, ArrRef — Dest = Array.length */
    ARR_LEN(24, "ArrLen", 2),

    // ---- I/O 与控制 (3) ----
    /** Print Source — 输出值（根据 Value 类型自动格式化） */
    PRINT(25, "Print", 1),
    /** PrintNL — 输出换行符 */
    PRINT_NL(26, "PrintNL", 0),
    /** Exit — 终止 VM 执行 */
    EXIT(27, "Exit", 0),
    I2F(28, "I2f", 2),
    I2D(29, "I2d", 2),
    F2D(30, "F2d", 2);

    /** 操作码编号 */
    private final int code;
    /** 助记符（用于 .lbc 文本格式） */
    private final String mnemonic;
    /** 操作数个数 */
    private final int operandCount;

    Opcode(int code, String mnemonic, int operandCount) {
        this.code = code;
        this.mnemonic = mnemonic;
        this.operandCount = operandCount;
    }

    public int getCode() { return code; }
    public String getMnemonic() { return mnemonic; }
    public int getOperandCount() { return operandCount; }

    /** 查找表：通过编号快速查找 */
    private static final Opcode[] BY_CODE = new Opcode[values().length];
    static {
        for (Opcode op : values()) {
            BY_CODE[op.code] = op;
        }
    }

    /**
     * 根据操作码编号查找枚举值。
     * @throws VmException 如果编号无效
     */
    public static Opcode fromCode(int code) {
        if (code < 0 || code >= BY_CODE.length || BY_CODE[code] == null) {
            throw new VmException("无效的操作码: " + code);
        }
        return BY_CODE[code];
    }

    /**
     * 根据助记符查找枚举值（不区分大小写）。
     * @throws VmException 如果助记符无效
     */
    public static Opcode fromMnemonic(String mnemonic) {
        for (Opcode op : values()) {
            if (op.mnemonic.equalsIgnoreCase(mnemonic)) {
                return op;
            }
        }
        throw new VmException("无效的指令助记符: " + mnemonic);
    }
}
