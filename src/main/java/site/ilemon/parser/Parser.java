package site.ilemon.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import site.ilemon.ast.forparse.*;
import site.ilemon.ast.forparse.Float;
import site.ilemon.ast.forparse.Void;
import site.ilemon.lexer.*;


/**
 * 语法分析
 * @author andy
 *
 */
public class Parser {

	private Lexer lexer; // 词法分析器

	private Token look;  // 当前token

	private boolean isValDecl;
	
	private Hashtable<String,Type> varTable = new Hashtable<String, Type>();

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
	private void match(String lexeme) throws IOException{
		if( lexeme.equals(look.getLexeme()))
			move();
		else
			error(lexeme);
	}
	
	private void match(Token token) throws IOException{
		if( token.getKind() == look.getKind() ){
			move();
		}else{
			error(token.getKind().toString());
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
	public MainClass parse() throws IOException{
		return parseMainClass();
	}

	// <mainClass> -> class <name> { <methodList>}
	private MainClass parseMainClass() throws IOException {
		MainClass mainClass = null;
		match("class");
		String className = look.getLexeme();
		move();
		match("{");
		List<Method> methods = parseMethodList();
		mainClass = new MainClass(className,methods);
		match("}");
		match("EOF");
		System.out.println("语法分析成功");
		return mainClass;
	}

	// <methodList> -> <method>*
	private List<Method> parseMethodList() throws IOException {
		List<Method> methods = new ArrayList<Method>();
		while( look.getKind() == TokenKind.Void || 
				look.getKind() == TokenKind.Int ||
				look.getKind() == TokenKind.Float) {
			methods.add(parseMethod());
		}
		return methods;
	}

	private boolean isInMethod = false;
	private boolean returnFound = false;
	
	// <method> -> void | int | double | methodname ( <inputparams> ) {<varDeclares> <stmts> [return <expr>]}
	private Method parseMethod() throws IOException {
		isInMethod = true;
		Type t = parseType();
		String methodName = look.getLexeme();
		int lineNumber = look.getLineNumber();
		move();
		match("(");
		List<Declare> inputParams = parseInputParams();
		match(")");
		match("{");
		List<Declare> localParams = parseVarDeclares();
		List<Stmt> stmts = parseStmts();
		match("}");
		isInMethod = false;
		return new Method(t, methodName, inputParams, localParams, stmts, null,lineNumber);
	}

	// <varDeclares> -> <varDeclare>*
	private List<Declare> parseVarDeclares() throws IOException{
		List<Declare> rs = new ArrayList<Declare>();
		while(look.getKind() == TokenKind.Int || 
				look.getKind() == TokenKind.Float){
			Declare d = parseDeclare();
			if( d != null ){
				varTable.put(d.name, d.type);
				rs.add(d);
			}
			if ( !isValDecl ) 
				break;
		}
		return rs;

	}

	// // <declare> -> type id;
	private Declare parseDeclare() throws IOException {
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
		Type type = parseType();
		if( look.getKind() == TokenKind.Assign ) {
			isValDecl = false;
			return null;
		}else if( look.getKind() == TokenKind.Id ){
			String id = look.getLexeme();
			move();
			// type id;
			if( look.getKind() == TokenKind.Semicolon) {
				isValDecl = true;
				Declare d = new Declare(type, id, look.getLineNumber());
				match(";");
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

	// <inputparams> -> type id,
	private List<Declare> parseInputParams() throws IOException {
		List<Declare> rs = new ArrayList<Declare>();
		if( look.getKind() == TokenKind.Int){
			Type t = parseType();
			String id = look.getLexeme();
			int lineNumber = look.getLineNumber();
			rs.add(new Declare(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			while(look.getKind() == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.getLexeme();
				lineNumber = look.getLineNumber();
				rs.add(new Declare(t, id, lineNumber));
				match(new Token(TokenKind.Id));
			}
		}else if( look.getKind() == TokenKind.Float){
			Type t = parseType();
			String id = look.getLexeme();
			int lineNumber = look.getLineNumber();
			rs.add(new Declare(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			while(look.getKind() == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.getLexeme();
				lineNumber = look.getLineNumber();
				rs.add(new Declare(t, id, lineNumber));
				match(new Token(TokenKind.Id));
			}
		}
		return rs;
	}

	private Type parseType() {
		if( look.getKind() == TokenKind.Int ){
			move();
			return new Int();
		}
		else if(look.getKind() == TokenKind.Void){
			move();
			return new Void();
		}
		else if(look.getKind() == TokenKind.Float){
			move();
			return new Float();
		}
		else 
			error(look.getLexeme());
		return null;
	}
	
	private List<Stmt> parseStmts() throws IOException {
		List<Stmt> rs = new ArrayList<Stmt>();
		while( look.getKind() == TokenKind.Printf || 
				look.getKind() == TokenKind.PrintNewLine || 
				look.getKind() == TokenKind.If ||
				look.getKind() == TokenKind.While ||
				look.getKind() == TokenKind.Lbrace ||
				look.getKind() == TokenKind.Id || 
				look.getKind() == TokenKind.Return){
			rs.add(parseStmt());
		}
		return rs;
	}

	private Stmt parseStmt() throws IOException {
		Stmt stmt = null;
		
		if( look.getKind() == TokenKind.Printf ){
			match(new Token(TokenKind.Printf));
			match(new Token(TokenKind.Lparen));
			String format = look.getLexeme();
			int lineNumber = look.getLineNumber();
			Token ahead = lexer.lookahead(1);
			List<Expr> exprs = null;
			if( ahead.getKind() == TokenKind.Commer ){
				exprs = new ArrayList<Expr>();
				while( look.getKind() != TokenKind.Rparen ){
					if( look.getKind() == TokenKind.Commer )
						move();
					exprs.add(parseExpr());
				}
				match(new Token(TokenKind.Rparen));
				match( new Token(TokenKind.Semicolon) );
				stmt = new Printf(format, exprs, lineNumber);
			}else{
				match(new Token(TokenKind.Lparen));
			}
		}
		else if( look.getKind() == TokenKind.PrintNewLine ){
			int lineNumber = look.getLineNumber();
			move();
			match(new Token(TokenKind.Lparen));
			match(new Token(TokenKind.Rparen));
			match( new Token(TokenKind.Semicolon) );
			stmt = new PrintNewLine(lineNumber);
		}
		else if( look.getKind() == TokenKind.While ){
			match(new Token(TokenKind.While));
			match(new Token(TokenKind.Lparen));
			int lineNumber = look.getLineNumber();
			Expr condition = parseExpr();
			match(new Token(TokenKind.Rparen));
			Stmt whileStmt = parseStmt();
			stmt = new While(condition, whileStmt, lineNumber);
		}
		else if ( look.getKind() == TokenKind.Id ) {
			Token ahead = lexer.lookahead(1);
			
			// 方法调用
			if( ahead.getKind() == TokenKind.Lparen ){
				
			}else{
				String id = look.getLexeme();
				int lineNum = look.getLineNumber();
				match( new Token(TokenKind.Id) );
				match( new Token(TokenKind.Assign) );
				Expr expr = parseExpr();
				match( new Token(TokenKind.Semicolon) );
				Type t = varTable.get(id);
				if( expr instanceof Num){
					if(t instanceof Float){
						stmt = new Assign(new Float(),id, expr, lineNum);
					}else{
						stmt = new Assign(new Int(),id, expr, lineNum);
					}
				}else{
					if( t == null){
						error("未定义的变量"+id);
					}else{
						if( expr instanceof Call){
							((Call)expr).returnType = t;
						}
						stmt = new Assign(t,id, expr, lineNum);
					}
				}
				
			}
		}
		else if( look.getKind() == TokenKind.Lbrace ) {
			match( "{" );
			int lineNumber = look.getLineNumber();
			stmt = new Block(parseStmts(), lineNumber);
			match( "}" );

		}
		else if( look.getKind() == TokenKind.Return ) {
			// 确保在同一个方法内并且return语句尚未解析
			if( isInMethod && returnFound){
				throw new Error("near line : "+look.getLineNumber()+ ",syntx error:multiple return statements found!");
			}
			match( "return" );
			int lineNumber = look.getLineNumber();
			Expr expr = parseExpr();
			stmt = new Return(expr, lineNumber);
			match( ";" );
			returnFound = true;

		}else if( look.getKind() == TokenKind.If ){
			match( "if" );
			match( "(" );
			int lineNumber = look.getLineNumber();
			Expr condition = parseExpr();
			match( ")" );
			Stmt thenStmt = parseStmt();
			match( "else" );
			Stmt elseStmt = parseStmt();
			stmt = new If(condition, thenStmt, elseStmt, lineNumber);
		}
		return stmt;
	}

	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Expr parseExpr() throws IOException {
		Expr expr = parseAndExpr();
		while( look.getKind() == TokenKind.And || look.getKind() == TokenKind.Or ) {
			move();
			Expr right = parseAndExpr();
			expr = new And(expr, right, expr.lineNumber);
		}
		return expr;
	}

	// AndExp -> additive_expr |<additive_expr>(>|<|>=|<=|==|!=)<additive_expr>
	private Expr parseAndExpr() throws IOException {
		Expr expr = parseAdditiveExpr();
		while( look.getKind() == TokenKind.LT || 
				look.getKind() == TokenKind.GT || 
				look.getKind() == TokenKind.LTE ||
				look.getKind() == TokenKind.GTE ||
				look.getKind() == TokenKind.NEQ ||
				look.getKind() == TokenKind.EQ ) {
			String operator = look.getLexeme();
			move();
			Expr expr1 = parseAdditiveExpr();
			switch (operator) {
			case ">":
				expr = new GT(expr, expr1, look.getLineNumber());
				break;
			case "<":
				expr = new LT(expr, expr1, look.getLineNumber());
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
	private Expr parseAdditiveExpr() throws IOException {
		Expr expr = parseTerm();
		while(look.getKind()==TokenKind.Add
				||look.getKind()==TokenKind.Sub) {
			Token temp=look;
			move();
			Expr otherExpr = parseTerm();
			if(temp.getKind()==TokenKind.Add) {
				expr = new Add(expr, otherExpr, look.getLineNumber());
			}else {
				expr = new Sub(expr, otherExpr, look.getLineNumber());
			}
		}
		return expr;
	}

	// <term> -> <factor> *|/ <factor>
	private Expr parseTerm() throws IOException{
		Expr expr = parseFactor();
		while(look.getKind()==TokenKind.Mul
				||look.getKind()==TokenKind.Div) {
			Token temp=look;
			move();
			Expr otherExpr = parseFactor();
			if(temp.getKind()==TokenKind.Mul) {
				expr = new Mul(expr, otherExpr, look.getLineNumber());
			}else {
				expr = new Div(expr, otherExpr, look.getLineNumber());
			}
		}
		return expr;
	}


	// <factor> -> (<expression>)
	//  		| Integer Literal
	//  		| id
	private Expr parseFactor() throws IOException{
		Expr expr = null;
		if(look.getKind()==TokenKind.Lparen){
			move();
			expr = parseExpr();
			match(new Token(TokenKind.Rparen));
			return expr;
		}else if(look.getKind()==TokenKind.Num){
			expr = new Num(Integer.valueOf(look.getLexeme()),new Int(), look.getLineNumber());
			move();
			return expr;
		}else if(look.getKind()==TokenKind.DNum){
			expr = new Num(java.lang.Double.valueOf(look.getLexeme()),new Float(), look.getLineNumber());
			move();
			return expr;
		}else if( look.getKind()==TokenKind.Id ){
			Token temp = look;
			Token ahead = lexer.lookahead(1);
			if( ahead.getKind() == TokenKind.Lparen){
				String methodName = look.getLexeme();
				int lineNumber = look.getLineNumber();
				move();
				match("(");
				List<Expr> args = null;
				args = new ArrayList<Expr>();
				ahead = lexer.lookahead(1);
				if( ahead.getKind() == TokenKind.Add || ahead.getKind() == TokenKind.Sub){
					args.add(parseAdditiveExpr());
				}else{
					while( look.getKind() == TokenKind.Id ){
						Type t = this.varTable.get(look.getLexeme());
						if( t == null){
							error("未知的变量"+look.getLexeme());
						}
						if( t instanceof Float)
							args.add(new Id(look.getLexeme(),new Float(), look.getLineNumber()));
						else
							args.add(new Id(look.getLexeme(),new Int(), look.getLineNumber()));
						move();
						if( look.getKind() == TokenKind.Commer)
							move();
					}
				}
				match(")");
				expr = new Call(methodName, args, lineNumber,this.varTable.get(temp.getLexeme()));
			}else{
				expr = new Id(look.getLexeme(), this.varTable.get(look.getLexeme()), look.getLineNumber());
				move();
			}
			return expr;
		}
		else if(look.getKind()==TokenKind.String ){
			expr = new Str1(look.getLexeme(), look.getLineNumber());
			move();
			return expr;
		}
		else{
			System.out.println("near line : "+look.getLineNumber()+" syntax error: "+"excepted get identifier or expression or number or String, but got "+look.getLexeme());
			System.exit(1);
		}
		return expr;
	}
}
