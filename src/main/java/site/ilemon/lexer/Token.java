package site.ilemon.lexer;

public class Token {

	private TokenKind kind;
	private String lexeme;
	private int lineNumber;

	public Token(TokenKind kind,String lexeme,int lineNumber){
		this.kind = kind;
		this.lexeme = lexeme;
		this.lineNumber = lineNumber;
	}
	public Token(TokenKind kind,int lineNumber){
		this.kind = kind;
		this.lineNumber = lineNumber;
	}
	public Token(TokenKind kind,String lexeme){
		this.kind = kind;
		this.lexeme = lexeme;
	}

	public Token(TokenKind tokenKind) {
		this.kind = tokenKind;
	}

	public TokenKind getKind() {
		return kind;
	}

	public void setKind(TokenKind kind) {
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

	@Override
	public String toString() {
		return "Token{" +
				"kind=" + kind +
				", lexeme='" + lexeme + '\'' +
				", lineNumber='" + lineNumber + '\'' +
				'}';
	}
}
