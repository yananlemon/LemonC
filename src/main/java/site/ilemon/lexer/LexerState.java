package site.ilemon.lexer;

/**
 * 词法分析器状态枚举
 * 定义DFA的所有状态
 */
public enum LexerState {
    START,          // 初始状态
    IN_ID,          // 正在读取标识符
    IN_NUM,         // 正在读取整数
    IN_FLOAT,       // 正在读取浮点数（小数点后）
    IN_STRING,      // 正在读取字符串
    IN_COMMENT,     // 正在读取单行注释
    IN_ASSIGN,      // 读到 =，可能是 = 或 ==
    IN_LT,          // 读到 <，可能是 < 或 <=
    IN_GT,          // 读到 >，可能是 > 或 >=
    IN_NOT,         // 读到 !，可能是 ! 或 !=
    IN_AND,         // 读到 &，期待 &&
    IN_OR,          // 读到 |，期待 ||
    IN_DIV,         // 读到 /，可能是 / 或 //
    DONE,           // 完成一个token
    ERROR           // 错误状态
}
