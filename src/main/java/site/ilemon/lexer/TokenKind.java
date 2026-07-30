package site.ilemon.lexer;

public enum TokenKind {

    /***文件末尾标识符****/
    EOF,

    /***关键字****/
    Class,
    Main,
    Void,
    StringType,
    Int,
    Float,
    Double,
    Bool,
    While,
    For,
    True,
    False,
    If,
    Else,
    Printf,
    Return,
    Break,
    Continue,

    /***算术运算符****/
    Add,
    Sub,
    Mul,
    Div,
    Mod,

    /***自增自减****/
    Increment,      // ++
    Decrement,      // --

    /***复合赋值****/
    AddAssign,      // +=
    SubAssign,      // -=
    MulAssign,      // *=
    DivAssign,      // /=
    ModAssign,      // %=

    /***比较运算符****/
    LT,         // <
    GT,         // >
    LTE,        // <=
    GTE,        // >=
    EQ,         // ==
    NEQ,        // !=

    /***关系运算符****/
    And,        // &&
    Or,         // ||
    Not,        // !

    /***界符****/
    Lbrace,         // {
    Rbrace,         // }
    Lparen,         // (
    Rparen,         // )
    Lbracket,       // [
    Rbracket,       // ]
    Semicolon,      // ;
    Comma,          // ,
    Dot,            // .

    Id,
    Num,
    FloatLiteral,
    DoubleLiteral,
    StringLiteral,
    Assign,
    PrintLine,
}
