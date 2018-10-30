package com.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.lexer.Lexer;
import com.lexer.Token;
import com.lexer.TokenKind;
import com.parser.Ast.Method;
import com.parser.Ast.Program.ProgramSingle;

/**
 * 语法分析
 * @author andy
 * @version 1.0
 */
public class Parser {

	private Lexer lexer; // 词法分析器

	private Token look;  // 当前token
	
	public Parser(Lexer lexer) throws IOException{
		this.lexer=lexer;
		lexer.lexicalAnalysis();
		move();
	}
	
	/**
	 * 读取下一个token
	 */
	private void move() {
		look = lexer.next();
	}
	
	/**
	 * 将{@code token}与当前词法分析器读到的token进行对比
	 * @param token
	 * @throws IOException
	 */
	private void match(Token token) throws IOException{
		if( token.getKind() == look.getKind() ){
			move();
		}else{
			error(token.getLexeme());
		}
	}

	private void error(String s) { 
		throw new Error("near line : "+look.getLineNumber()+" syntax error,excepted get '"+s+"',but got "+look.getLexeme()); 
	}
	
	/**
	 * 语法分析入口
	 * @return Program
	 * @throws IOException
	 */
	public Ast.Program.ProgramSingle parse() throws IOException{
		return parseProgram();
	}
	// <program> -> class classname { <methodDeclareList> }
	private ProgramSingle parseProgram() throws IOException {
		Ast.Program.ProgramSingle programSingle = null;
		match(new Token(TokenKind.Class,"class"));
		String className = look.getLexeme();
		match(new Token(TokenKind.Id));
		match(new Token(TokenKind.Lbrace));
		List<Method.T> methods = parseMethodDeclareList();
		match(new Token(TokenKind.Rbrace));
		match(new Token(TokenKind.EndOfFile, "EOF"));
		System.out.println("语法分析成功");
		return programSingle;
	}

	// <methodDeclareList> -> <MethodDecl>*
	private List<Ast.Method.T> parseMethodDeclareList() throws IOException {
		List<Ast.Method.T> rs = new ArrayList<Ast.Method.T>();
		while( look.getKind() == TokenKind.Void || 
				 look.getKind() == TokenKind.Int) {
			rs.add(parseMethodeclare());
		}
		return rs;
	}
	
	// <MethodDecl> -> 
	//				int|void methodname(<inputParams>){<varDeclare>* <stmt>* [return expr]}
	private Ast.Method.T parseMethodeclare() throws IOException {
		
		Ast.Type.T returnType = parseType();
		move();
		String methodName = look.getLexeme();
		List<Ast.Declare.T> inputParams = parseInputParams();
		match(new Token(TokenKind.Lbrace));
		
		match(new Token(TokenKind.Rbrace));
		return null;
	}

	private List<com.parser.Ast.Declare.T> parseInputParams() {
		return null;
	}

	private Ast.Type.T parseType() {
		if( look.getKind() == TokenKind.Int )
			return new Ast.Type.Int();
		else if(look.getKind() == TokenKind.Void)
			return new Ast.Type.Void();
		else 
			error(look.getLexeme());
		return null;
	}
}
