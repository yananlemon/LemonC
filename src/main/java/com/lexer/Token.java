package com.lexer;

public class Token {
	
	private String lexeme;	// 单词内容
	
	private int lineNumber; // 单词所在源文件行数
	
	private TokenKind kind; // 单词种类

	public Token(String lexeme, int lineNumber, TokenKind kind) {
		super();
		this.lexeme = lexeme;
		this.lineNumber = lineNumber;
		this.kind = kind;
	}
	
	public Token(TokenKind kind,String lexeme) {
		super();
		this.lexeme = lexeme;
		this.kind = kind;
	}
	
	public Token(TokenKind kind,String lexeme,int lineNumber) {
		super();
		this.lexeme = lexeme;
		this.kind = kind;
		this.lineNumber = lineNumber;
	}


	

	public Token(TokenKind kind) {
		this.kind = kind;
	}

	public String getLexeme() {
		return lexeme;
	}

	public void setLexeme(String lexeme) {
		this.lexeme = lexeme;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public TokenKind getKind() {
		return kind;
	}

	public void setKind(TokenKind kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "<lexeme=" + lexeme  + ", lineNumber=" + lineNumber
				+ ", kind=" + kind + ">\n";
	}
}
