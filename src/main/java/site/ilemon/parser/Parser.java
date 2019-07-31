package site.ilemon.parser;

import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import static site.ilemon.lexer.TokenKind.Num;


/**
 * 语法分析
 * @author andy
 *
 */
public class Parser {

	private Lexer lexer; // 词法分析器

	private Token look;  // 当前token

	private boolean isValDecl;
	
	private Hashtable<String,Ast.Type.T> varTable = new Hashtable<String, Ast.Type.T>();

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
	 * @param lexeme
	 * @throws IOException
	 */
	private void match(String lexeme) throws IOException{
		if( lexeme.equals(look.lexeme))
			move();
		else
			error(lexeme);
	}
	
	private void match(Token token) throws IOException{
		if( token.kind == look.kind ){
			move();
		}else{
			error(token.kind.toString());
		}
	}

	private void error(String s) { 
		throw new Error("near line : "+look.lineNumber+" syntax error,excepted get '"+s+"',but got "+look.lexeme);
	}

	/**
	 * 语法分析入口
	 * @return Program
	 * @throws IOException
	 */
	public Ast.MainClass.MainClassSingle parse() throws IOException{
		return parseMainClass();
	}

	// <mainClass> -> class <name> { <methodList>}
	private Ast.MainClass.MainClassSingle parseMainClass() throws IOException {
		Ast.MainClass.MainClassSingle mainClass = null;
		match("class");
		String className = look.lexeme;
		move();
		match("{");
		ArrayList<Ast.Method.T> methods = parseMethodList();
		mainClass = new Ast.MainClass.MainClassSingle(className,null,methods);
		match("}");
		match("EOF");
		System.out.println("语法分析成功");
		return mainClass;
	}

	// <methodList> -> <method>*
	private ArrayList<Ast.Method.T> parseMethodList() throws IOException {
		ArrayList<Ast.Method.T> methods = new ArrayList<Ast.Method.T>();
		while( look.kind == TokenKind.Void ||
				look.kind == TokenKind.Int ||
				look.kind == TokenKind.Float) {
			methods.add(parseMethod());
		}
		return methods;
	}

	
	// <method> -> void | int | double | methodname ( <inputparams> ) {<varDeclares> <stmts> [return <expr>]}
	private Ast.Method.MethodSingle parseMethod() throws IOException {
		Ast.Type.T t = parseType();
		String methodName = look.lexeme;
		int lineNumber = look.lineNumber;
		move();
		match("(");
			ArrayList<Ast.Declare.T> inputParams = parseInputParams();
		match(")");
		match("{");
		ArrayList<Ast.Declare.T> localParams = parseVarDeclares();
		ArrayList<Ast.Stmt.T> stmts = parseStmts();
		match("}");
		return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,null);
	}

	// <varDeclares> -> <varDeclare>*
	private ArrayList<Ast.Declare.T> parseVarDeclares() throws IOException{
		ArrayList<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		while(look.kind == TokenKind.Int || 
				look.kind == TokenKind.Float){
			String id = look.lexeme;
			Ast.Declare.T d = parseDeclare();
			if( d != null ){
				if( d instanceof  Ast.Declare.DeclareSingle){
					Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle)d;
					varTable.put(declareSingle.id,declareSingle.type);
				}
				rs.add(d);
			}
			if ( !isValDecl ) 
				break;
		}
		return rs;

	}

	// // <declare> -> type id;
	private Ast.Declare.T parseDeclare() throws IOException {
		Token t = lexer.lookahead(2);
		if( t !=null && t.kind == TokenKind.Lparen){
			isValDecl = false;
			return null;
		}
		t = lexer.lookahead(1);
		if( t !=null && t.kind == TokenKind.Assign){
			isValDecl = false;
			return null;
		}
		Ast.Type.T type = parseType();
		if( look.kind == TokenKind.Assign ) {
			isValDecl = false;
			return null;
		}else if( look.kind == TokenKind.Id ){
			String id = look.lexeme;
			move();
			// type id;
			if( look.kind == TokenKind.Semicolon) {
				isValDecl = true;
				Ast.Declare.DeclareSingle d = new Ast.Declare.DeclareSingle(type,id,look.lineNumber);
				match(";");
				return d;
			}
			else if( look.kind == TokenKind.Lparen ) {
				isValDecl = false;
				return null;
			}else {
				error(look.lexeme);
				return null;
			}

		}else {
			error(look.lexeme);
			return null;
		}
	}

	// <inputparams> -> type id,
	private ArrayList<Ast.Declare.T> parseInputParams() throws IOException {
		ArrayList<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		if( look.kind == TokenKind.Int){
			Ast.Type.T t = parseType();
			String id = look.lexeme;
			int lineNumber = look.lineNumber;
			rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			while(look.kind == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.lexeme;
				lineNumber = look.lineNumber;
				rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
				match(new Token(TokenKind.Id));
			}
		}else if( look.kind == TokenKind.Float){
			Ast.Type.T t = parseType();
			String id = look.lexeme;
			int lineNumber = look.lineNumber;
			rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			while(look.kind == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.lexeme;
				lineNumber = look.lineNumber;
				rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
				match(new Token(TokenKind.Id));
			}
		}
		return rs;
	}

	private Ast.Type.T parseType() {
		if( look.kind == TokenKind.Int ){
			varTable.put(look.lexeme, new Ast.Type.Int());
			move();
			return new Ast.Type.Int();
		}
		else if(look.kind == TokenKind.Void){
			varTable.put(look.lexeme, new Ast.Type.Void());
			move();
			return new Ast.Type.Void();
		}
		else if(look.kind == TokenKind.Float){
			varTable.put(look.lexeme, new Ast.Type.Float());
			move();
			return new Ast.Type.Float();
		}
		else 
			error(look.lexeme);
		return null;
	}
	
	private HashMap<String,Boolean> methodMap = new HashMap<String,Boolean>();
	
	private ArrayList<Ast.Stmt.T> parseStmts() throws IOException {
		ArrayList<Ast.Stmt.T> rs = new ArrayList<Ast.Stmt.T>();
		while( look.kind == TokenKind.Printf || 
				look.kind == TokenKind.PrintNewLine || 
				look.kind == TokenKind.If ||
				look.kind == TokenKind.While ||
				look.kind == TokenKind.Lbrace ||
				look.kind == TokenKind.Id || 
				look.kind == TokenKind.Return){
			rs.add(parseStmt());
		}
		return rs;
	}

	private Ast.Stmt.T parseStmt() throws IOException {
		Ast.Stmt.T stmt = null;
		
		if( look.kind == TokenKind.Printf ){
			match(new Token(TokenKind.Printf));
			match(new Token(TokenKind.Lparen));
			String format = look.lexeme;
			int lineNumber = look.lineNumber;
			Token ahead = lexer.lookahead(1);
			ArrayList<Ast.Expr.T> exprs = null;
			if( ahead.kind == TokenKind.Commer ){
				exprs = new ArrayList<Ast.Expr.T>();
				while( look.kind != TokenKind.Rparen ){
					if( look.kind == TokenKind.Commer )
						move();
					exprs.add(parseExpr());
				}
				match( new Token(TokenKind.Rparen) );
				match( new Token(TokenKind.Semicolon) );
				stmt = new Ast.Stmt.Printf(format,exprs,lineNumber);
			}else{
				match(new Token(TokenKind.Lparen));
			}
		}
		else if( look.kind == TokenKind.While ){
			match(new Token(TokenKind.While));
			match(new Token(TokenKind.Lparen));
			int lineNumber = look.lineNumber;
			Ast.Expr.T condition = parseExpr();
			match(new Token(TokenKind.Rparen));
			Ast.Stmt.T whileStmt = parseStmt();
			stmt = new Ast.Stmt.While(condition, whileStmt, lineNumber);
		}
		else if ( look.kind == TokenKind.Id ) {
			Token ahead = lexer.lookahead(1);
			
			// 方法调用
			if( ahead.kind == TokenKind.Lparen ){
				
			}else{
				String id = look.lexeme;
				int lineNum = look.lineNumber;
				match( new Token(TokenKind.Id) );
				match( new Token(TokenKind.Assign) );
				Ast.Expr.T expr = parseExpr();
				match( new Token(TokenKind.Semicolon) );
				//stmt = new Ast.Stmt.Assign(id, expr, lineNum);
				stmt = new Ast.Stmt.Assign(new Ast.Expr.Id(id,this.varTable.get(id),lineNum), expr, lineNum);
				
			}
		}
		else if( look.kind == TokenKind.Lbrace ) {
			match( "{" );
			int lineNumber = look.lineNumber;
			stmt = new Ast.Stmt.Block(parseStmts(), lineNumber);
			match( "}" );

		}
		else if( look.kind == TokenKind.Return ) {
			/*// 确保在同一个方法内并且return语句尚未解析
			if( returnFound){
				throw new Error("near line : "+look.lineNumber+ ",syntx error:multiple return statements found!");
			}*/
			match( "return" );
			int lineNumber = look.lineNumber;
			Ast.Expr.T expr = parseExpr();
			stmt = new Ast.Stmt.Return(expr, lineNumber);
			match( ";" );

		}else if( look.kind == TokenKind.If ){
			match( "if" );
			match( "(" );
			int lineNumber = look.lineNumber;
			Ast.Expr.T condition = parseExpr();
			match( ")" );
			Ast.Stmt.T thenStmt = parseStmt();
			match( "else" );
			Ast.Stmt.T elseStmt = parseStmt();
			stmt = new Ast.Stmt.If(condition, thenStmt, elseStmt, lineNumber);
		}
		return stmt;
	}

	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Ast.Expr.T parseExpr() throws IOException {
		Ast.Expr.T expr = parseAndExpr();
		while( look.kind == TokenKind.And || look.kind == TokenKind.Or ) {
			move();
			Ast.Expr.T right = parseAndExpr();
			expr = new Ast.Expr.And(expr, right, expr.lineNum);
		}
		return expr;
	}

	// AndExp -> additive_expr |<additive_expr>(>|<|>=|<=|==|!=)<additive_expr>
	private Ast.Expr.T parseAndExpr() throws IOException {
		Ast.Expr.T expr = parseAdditiveExpr();
		while( look.kind == TokenKind.LT ||
				look.kind == TokenKind.GT ||
				look.kind == TokenKind.LTE ||
				look.kind == TokenKind.GTE ||
				look.kind == TokenKind.NEQ ||
				look.kind == TokenKind.EQ ) {
			String operator = look.lexeme;
			move();
			Ast.Expr.T expr1 = parseAdditiveExpr();
			switch (operator) {
			case ">":
				expr = new Ast.Expr.GT(expr, expr1, look.lineNumber);
				break;
			case "<":
				expr = new Ast.Expr.LT(expr, expr1, look.lineNumber);
				break;
			default:
				break;
			}

		}
		return expr;
	}

	//<additiveExpr>-><term>{(+|-)<term>}
	private Ast.Expr.T parseAdditiveExpr() throws IOException {
		Ast.Expr.T expr = parseTerm();
		while(look.kind==TokenKind.Add
				||look.kind==TokenKind.Sub) {
			Token temp=look;
			move();
			Ast.Expr.T otherExpr = parseTerm();
			if(temp.kind==TokenKind.Add) {
				expr = new Ast.Expr.Add(expr, otherExpr, look.lineNumber);
			}else {
				expr = new Ast.Expr.Sub(expr, otherExpr, look.lineNumber);
			}
		}
		return expr;
	}

	// <term> -> <factor> *|/ <factor>
	private Ast.Expr.T parseTerm() throws IOException{
		Ast.Expr.T expr = parseFactor();
		while(look.kind==TokenKind.Mul
				||look.kind==TokenKind.Div) {
			Token temp=look;
			move();
			Ast.Expr.T otherExpr = parseFactor();
			if(temp.kind == TokenKind.Mul) {
				expr = new Ast.Expr.Mul(expr, otherExpr, look.lineNumber);
			}else {
				expr = new Ast.Expr.Div(expr, otherExpr, look.lineNumber);
			}
		}
		return expr;
	}


	// <factor> -> (<expression>)
	//  		| Integer Literal
	//  		| id
	private Ast.Expr.T parseFactor() throws IOException{
		Ast.Expr.T expr = null;
		if(look.kind==TokenKind.Lparen){
			move();
			expr = parseExpr();
			match(new Token(TokenKind.Rparen));
			return expr;
		}else if(look.kind== Num){
			expr = new Ast.Expr.Number(new Ast.Type.Int(),look.lexeme,look.lineNumber);
			move();
			return expr;
		}else if(look.kind==TokenKind.DNum){
			expr = new Ast.Expr.Number(new Ast.Type.Float(),look.lexeme,look.lineNumber);
			move();
			return expr;
		}else if( look.kind==TokenKind.Id ){
			Token temp = look;
			Token ahead = lexer.lookahead(1);
			if( ahead.kind == TokenKind.Lparen){
				String methodName = look.lexeme;
				int lineNumber = look.lineNumber;
				move();
				match("(");
				ArrayList<Ast.Expr.T> args = null;
				args = new ArrayList<Ast.Expr.T>();
				ahead = lexer.lookahead(1);
				if( ahead.kind == TokenKind.Add || ahead.kind == TokenKind.Sub){
					args.add(parseAdditiveExpr());
				}else{
					while( look.kind == TokenKind.Id || look.kind == TokenKind.Num  || look.kind == TokenKind.DNum ){
						if( look.kind == TokenKind.Id)
							args.add(parseFactor());
						else if( look.kind == TokenKind.Num )
							args.add(new Ast.Expr.Number(new Ast.Type.Int(),look.lexeme,look.lineNumber));
						else if( look.kind == TokenKind.DNum )
							args.add(new Ast.Expr.Number(new Ast.Type.Float(),look.lexeme,look.lineNumber));
						//move();
						if( look.kind == TokenKind.Commer)
							move();
					}
				}
				match(")");
				expr = new Ast.Expr.Call(methodName, args, lineNumber);
			}else{
				expr = new Ast.Expr.Id(look.lexeme,this.varTable.get(look.lexeme),look.lineNumber);
				move();
			}
			return expr;
		}
		else if(look.kind==TokenKind.String ){
			expr = new Ast.Expr.Str(look.lexeme, look.lineNumber);
			move();
			return expr;
		}
		else{
			System.out.println("near line : "+look.lineNumber+" syntax error: "+"excepted get identifier or expression or number or String, but got "+look.lexeme);
			System.exit(1);
		}
		return expr;
	}
}
