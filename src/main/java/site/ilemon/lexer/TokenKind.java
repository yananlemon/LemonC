package site.ilemon.lexer;

public enum TokenKind {

	/***文件末尾标识符****/
	EOF,

	/***关键字****/
	Class,
	Main,
	Void,
	String,
	Int,
	Float,
	Bool,
	While,
	True,
	False,
	If,
	Else,
	Printf,
	Return,

	/***算术运算符****/
	Add,
	Sub,
	Mul,
	Div,
	Mod,

	/***比较运算符****/
	LT,			// <
	GT,			// >
	LTE,		// <=
	GTE,		// >=
	EQ,			// ==
	NEQ,		// !=

	/***关系运算符****/
	And,		// &&
	Or,			// ||
	Not,		// !

	/***界符****/
	DoubleQuotation,	// "
	Lbrace,				// {
	Rbrace,				// }
	Lparen,				// (
	Rparen,				// )
	Semicolon,			// ;
	Commer,				// ,

	Id,
	Num,
	DNum,
	Assign,
	Unknown, PrintLine,

}