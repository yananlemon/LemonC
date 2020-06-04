package site.ilemon.lexer;

/**
 * <p>有穷状态自动机状态枚举</p>
 * @author Andy.Yan
 */
public enum DFAState {
    INITIAL,                // 初始状态
    IDENTIFIER,             // 标识符状态
    DIGITAL,                // 数字状态
    ASSIGN,                 // 赋值状态
    GREATER_THAN,           // 大于状态
    LESS_THAN,              // 小于状态
    NOT,                    // 非状态
    OR,                     // 或者状态
    AND,                    // 并且状态
    ARITH_OPERATION,        // 算术运算状态
    LEFT_PAREN,             // 左括号状态
    RIGHT_PAREN,            // 右括号状态
    LEFT_BRACE,             // 左大括号状态
    RIGHT_BRACE,            // 右大括号状态
    SEMICOLON,              // 分号状态
    COMMA,                  // 分号状态
    DOUBLE_QUOTATION,       // 字符串状态

}