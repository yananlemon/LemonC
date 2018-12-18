package com.lexer;

public enum TokenKind {
	
	/***文件末尾标识符****/
	EndOfFile,
	
	/***关键字****/
	Class,
	Main,
	Void,
	String,
	Int,
	Float,
	While,
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
	DoubleQuotation,// "
	Lbrace,			// {
	Rbrace,			// }
	Lparen,			// (
	Rparen,			// )
	Semicolon,		// ;
	Commer,			// ,
	
	Id,
	Num,
	DNum,
	Assign,
	Unknown, PrintNewLine,
	
}
