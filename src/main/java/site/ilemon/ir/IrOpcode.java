package site.ilemon.ir;

/**
 * LemonIR 操作码枚举。
 * 类似于汇编，但基于三地址结构。
 */
public enum IrOpcode {
    // ---- 赋值 ----
    MOV,        // v_dest = MOV v_src

    // ---- 算术运算 ----
    ADD,        // v_dest = ADD v_src1, v_src2
    SUB,
    MUL,
    DIV,
    MOD,
    NEG,        // v_dest = NEG v_src
    INC,        // v_dest = INC v_src (为了兼容 VM)
    DEC,
    I2F,        // v_dest = (float) v_src
    I2D,        // v_dest = (double) v_src
    F2D,        // v_dest = (double) v_src

    // ---- 逻辑与比较 ----
    NOT,        // v_dest = NOT v_src
    EQ,         // v_dest = EQ v_src1, v_src2  (返回 BOOL)
    NE,
    GT,
    LT,
    GE,
    LE,

    // ---- 跳转 ----
    JMP,        // JMP label
    BR_TRUE,    // BR_TRUE v_cond, label
    BR_FALSE,   // BR_FALSE v_cond, label

    // ---- 函数调用 ----
    CALL,       // v_dest = CALL func_name, arg1, arg2...
    RET,        // RET v_src (或空)

    // ---- 数组 ----
    NEW_ARR,    // v_dest = NEW_ARR size
    ARR_GET,    // v_dest = ARR_GET arr, index
    ARR_SET,    // ARR_SET arr, index, value
    ARR_LEN,    // v_dest = ARR_LEN arr

    // ---- I/O ----
    PRINT,      // PRINT v_src
    PRINT_NL,   // PRINT_NL
    EXIT        // EXIT
}
