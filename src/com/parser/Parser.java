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

	private boolean isValDecl;

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
		Ast.MainClass.MainClassSingle mainClass = new Ast.MainClass.MainClassSingle(className, methods);
		programSingle = new ProgramSingle(mainClass);
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
	//				int|void methodname(<inputParams>){<varDeclares>* <stmt>* [return expr]}
	private Ast.Method.T parseMethodeclare() throws IOException {
		Ast.Type.T returnType = parseType();
		move();
		String methodName = look.getLexeme();
		move();
		match(new Token(TokenKind.Lparen));
		List<Ast.Declare.T> inputParams = parseInputParams();
		match(new Token(TokenKind.Rparen));
		match(new Token(TokenKind.Lbrace));
		List<Ast.Declare.T> varDeclares = parseDeclares();
		List<Ast.Stmt.T> stmts = parseStmts();
		match(new Token(TokenKind.Rbrace));
		Ast.Exp.T returnExpr = null;
		if( !methodName.equals("main") ){
			match(new Token(TokenKind.Return));
			returnExpr = parseExpr();
		}
		return new Ast.Method.MethodSingle(returnType, methodName, inputParams, varDeclares, stmts, returnExpr);
	}

	private List<com.parser.Ast.Stmt.T> parseStmts() throws IOException {
		List<com.parser.Ast.Stmt.T> rs = new ArrayList<com.parser.Ast.Stmt.T>();
		while( look.getKind() == TokenKind.Printf || 
				look.getKind() == TokenKind.PrintNewLine || 
				look.getKind() == TokenKind.If ||
				look.getKind() == TokenKind.While ||
				look.getKind() == TokenKind.Lbrace ||
				look.getKind() == TokenKind.Id){
			rs.add(parseStmt());
		}
		return rs;
	}

	private com.parser.Ast.Stmt.T parseStmt() throws IOException {
		Ast.Stmt.T stmt = null;
		if( look.getKind() == TokenKind.Printf ){
			match(new Token(TokenKind.Printf));
			match(new Token(TokenKind.Lparen));
			String format = look.getLexeme();
			int lineNumber = look.getLineNumber();
			Token ahead = lexer.lookahead(1);
			List<Ast.Exp.T> exprs = null;
			if( ahead.getKind() == TokenKind.Commer ){
				exprs = new ArrayList<Ast.Exp.T>();
				while( look.getKind() != TokenKind.Rparen ){
					if( look.getKind() == TokenKind.Commer )
						move();
					exprs.add(parseExpr());
				}
				match(new Token(TokenKind.Rparen));
				match( new Token(TokenKind.Semicolon) );
				stmt = new Ast.Stmt.Printf(format, exprs, lineNumber);
			}else{
				match(new Token(TokenKind.Lparen));
				//Ast.ExpparseExpr();
			}
		}
		else if( look.getKind() == TokenKind.PrintNewLine ){
			int lineNumber = look.getLineNumber();
			move();
			match(new Token(TokenKind.Lparen));
			match(new Token(TokenKind.Rparen));
			match( new Token(TokenKind.Semicolon) );
			stmt = new Ast.Stmt.PrintNewLine(lineNumber);
		}
		else if( look.getKind() == TokenKind.While ){
			match(new Token(TokenKind.While));
			match(new Token(TokenKind.Lparen));
			int lineNumber = look.getLineNumber();
			Ast.Exp.T condition = parseExpr();
			match(new Token(TokenKind.Rparen));
			Ast.Stmt.T whileStmt = parseStmt();
			stmt = new Ast.Stmt.While(condition, whileStmt, lineNumber);
		}
		else if ( look.getKind() == TokenKind.Id) {
			String id = look.getLexeme();
			int lineNum = look.getLineNumber();
			match( new Token(TokenKind.Id) );
			match( new Token(TokenKind.Assign) );
			Ast.Exp.T expr = parseExpr();
			match( new Token(TokenKind.Semicolon) );
			stmt = new Ast.Stmt.Assign(id, expr, lineNum);
		}
		else if( look.getKind() == TokenKind.Lbrace) {
			match( new Token(TokenKind.Lbrace) );
			int lineNumber = look.getLineNumber();
			stmt = new Ast.Stmt.Block(parseStmts(), lineNumber);
			match( new Token(TokenKind.Rbrace) );

		}
		return stmt;
	}

	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Ast.Exp.T parseExpr() throws IOException {
		Ast.Exp.T expr = parseAndExpr();
		while( look.getKind() == TokenKind.And || look.getKind() == TokenKind.Or ) {
			move();
			Ast.Exp.T right = parseAndExpr();
			expr = new Ast.Exp.And(expr, right, expr.lineNumber);
		}
		return expr;
	}

	// AndExp -> additive_expr |<additive_expr>(>|<|>=|<=|==|!=)<additive_expr>
	private Ast.Exp.T parseAndExpr() throws IOException {
		Ast.Exp.T expr = parseAdditiveExpr();
		while( look.getKind() == TokenKind.LT || 
				look.getKind() == TokenKind.GT || 
				look.getKind() == TokenKind.LTE ||
				look.getKind() == TokenKind.GTE ||
				look.getKind() == TokenKind.NEQ ||
				look.getKind() == TokenKind.EQ ) {
			String operator = look.getLexeme();
			move();
			Ast.Exp.T expr1 = parseAdditiveExpr();
			switch (operator) {
			case ">":
				expr = new Ast.Exp.GT(expr, expr1, look.getLineNumber());
				break;
			case "<":
				expr = new Ast.Exp.LT(expr, expr1, look.getLineNumber());
				break;
			/*case "<=":
				expr = new Ast.Exp.LTE(expr, expr1, look.getLineNumber());
				break;
			case ">=":
				expr = new Ast.Exp.GT(expr, expr1, look.getLineNumber());
				break;*/

			default:
				break;
			}

		}
		return expr;
	}

	//<additiveExpr>-><term>{(+|-)<term>}
	private Ast.Exp.T parseAdditiveExpr() throws IOException {
		Ast.Exp.T expr = parseTerm();
		while(look.getKind()==TokenKind.Add
				||look.getKind()==TokenKind.Sub) {
			Token temp=look;
			move();
			Ast.Exp.T otherExpr = parseTerm();
			if(temp.getKind()==TokenKind.Add) {
				expr = new Ast.Exp.Add(expr, otherExpr, look.getLineNumber());
			}else {
				expr = new Ast.Exp.Sub(expr, otherExpr, look.getLineNumber());
			}
		}
		return expr;
	}

	// <term> -> <factor> *|/ <factor>
	private Ast.Exp.T parseTerm() throws IOException{
		Ast.Exp.T expr = parseFactor();
		while(look.getKind()==TokenKind.Mul
				||look.getKind()==TokenKind.Div) {
			Token temp=look;
			move();
			Ast.Exp.T otherExpr = parseFactor();
			if(temp.getKind()==TokenKind.Mul) {
				expr = new Ast.Exp.Mul(expr, otherExpr, look.getLineNumber());
			}else {
				expr = new Ast.Exp.Div(expr, otherExpr, look.getLineNumber());
			}
		}
		return expr;
	}


	// <factor> -> (<expression>)
	//  		| Integer Literal
	//  		| id
	private Ast.Exp.T parseFactor() throws IOException{
		Ast.Exp.T expr = null;
		if(look.getKind()==TokenKind.Lparen){
			move();
			expr = parseExpr();
			match(new Token(TokenKind.Rparen));
			return expr;
		}else if(look.getKind()==TokenKind.Num){
			expr = new Ast.Exp.Num(Integer.valueOf(look.getLexeme()), look.getLineNumber());
			move();
			return expr;
		}else if( look.getKind()==TokenKind.Id ){
			Token ahead = lexer.lookahead(1);
			if( ahead.getKind() == TokenKind.Lparen){
				
				String methodName = look.getLexeme();
				int lineNumber = look.getLineNumber();
				move();
				match( new Token(TokenKind.Lparen) );
				List<Ast.Exp.T> args = null;
				if( look.getKind()==TokenKind.Id ){
					args = new ArrayList<Ast.Exp.T>();
					args.add(new Ast.Exp.Id(look.getLexeme(), look.getLineNumber()));
				}
				expr = new Ast.Exp.Call(null, methodName, args, lineNumber);
			}
			expr = new Ast.Exp.Id(look.getLexeme(), look.getLineNumber());

			move();
			return expr;
		}
		else if(look.getKind()==TokenKind.String ){

			expr = new Ast.Exp.Str(look.getLexeme(), look.getLineNumber());

			move();
			return expr;
		}
		else{
			System.out.println("near line : "+look.getLineNumber()+" syntax error: "+"excepted get identifier or expression or number or String, but got "+look.getLexeme());
			System.exit(1);
		}
		return expr;
	}

	// <varDeclares> -> <varDeclare><varDeclares>*
	private List<com.parser.Ast.Declare.T> parseDeclares() throws IOException{
		List<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		while(look.getKind() == TokenKind.Int ){
			Ast.Declare.T d = parseDeclare();
			if( d != null )
				rs.add(d);
			if ( !isValDecl ) 
				break;
		}
		return rs;

	}

	// <declare> -> type id;
	private com.parser.Ast.Declare.T parseDeclare() throws IOException {
		Token t = lexer.lookahead(2);
		if( t !=null && t.getKind() == TokenKind.Lparen){
			isValDecl = false;
			return null;
		}
		t = lexer.lookahead(1);
		if( t !=null && t.getKind() == TokenKind.Assign){
			isValDecl = false;
			return null;
		}
		Ast.Type.T type = parseType();
		if( look.getKind() == TokenKind.Assign ) {
			isValDecl = false;
			return null;
		}else if( look.getKind() == TokenKind.Id ){
			String id = look.getLexeme();
			move();
			// type id;
			if( look.getKind() == TokenKind.Semicolon) {
				isValDecl = true;
				Ast.Declare.DeclareSingle d = new Ast.Declare.DeclareSingle(type, id, look.getLineNumber());
				match(new Token(TokenKind.Semicolon));
				return d;
			}
			else if( look.getKind() == TokenKind.Lparen ) {
				isValDecl = false;
				return null;
			}else {
				error(look.getLexeme());
				return null;
			}

		}else {
			error(look.getLexeme());
			return null;
		}
	}

	// <inputParams> -> type id
	private List<com.parser.Ast.Declare.T> parseInputParams() throws IOException {
		List<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		if( look.getKind() == TokenKind.Int || look.getKind() == TokenKind.Void ){
			String id = look.getLexeme();
			int lineNumber = look.getLineNumber();
			rs.add(new Ast.Declare.DeclareSingle(parseType(), id, lineNumber));
			match(new Token(TokenKind.Id));
			while(look.getKind() == TokenKind.Commer ){
				move();
				id = look.getLexeme();
				lineNumber = look.getLineNumber();
				rs.add(new Ast.Declare.DeclareSingle(parseType(), id, lineNumber));
				match(new Token(TokenKind.Id));
			}
		}
		return rs;
	}

	private Ast.Type.T parseType() {
		if( look.getKind() == TokenKind.Int ){
			move();
			return new Ast.Type.Int();
		}
		else if(look.getKind() == TokenKind.Void){
			return new Ast.Type.Void();
		}
		else 
			error(look.getLexeme());
		return null;
	}
}
