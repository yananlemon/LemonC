package site.ilemon.parser;

import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static site.ilemon.lexer.TokenKind.Num;


/**
 * <p>基于递归下降实现的语法分析</p>
 * @author Andy.Yan
 *
 */
public class Parser {

	private Lexer lexer; // 词法分析器

	private Token look;  // 当前token

	private boolean isValDecl;
	
	private HashMap<String,Ast.Type.T> varTable = new HashMap<String, Ast.Type.T>();

	private String currMethod;

	private  HashMap<String,HashMap<String,Ast.Type.T>> table = new  HashMap<String,HashMap<String,Ast.Type.T>>();

	public Parser(Lexer lexer) throws IOException{
		this.lexer=lexer;
		move();
	}

	/**
	 * 读取下一个token
	 */
	private void move() {
		look = lexer.next();
	}


	/**
	 * 将{@code token}与当前词法分析器读到的token进行对比,如果匹配则读取下一个token;否则抛出异常.
	 * @param lexeme
	 * @throws IOException
	 */
	private void match(String lexeme) throws IOException{
		if(lexeme.equals(look.getLexeme())) {
			move();
		} else {
			error(lexeme);
		}

	}
	
	private void match(Token token) throws IOException{
		if( token.getKind() == look.getKind() ){
			move();
		}else{
			error(token.getKind().toString());
		}
	}

	private void error(String s) { 
		throw new Error("near line : " + look.getLineNumber() + " syntax error,excepted get '" + s + "',but got " + look.getLexeme());
	}

	/**
	 * 语法分析入口
	 * @return Program
	 * @throws IOException
	 */
	public Ast.Program.T parse() throws IOException{
		Ast.MainClass.MainClassSingle mainClass = parseMainClass();
		Ast.Program.T programSingle = new Ast.Program.ProgramSingle(mainClass);
		return programSingle;
	}


	// <mainClass> -> class <name> { <methodList>}
	private Ast.MainClass.MainClassSingle parseMainClass() throws IOException {
		Ast.MainClass.MainClassSingle mainClass = null;
		match("class");
		String className = look.getLexeme();
		// 检查class名称是否一致
		if( !className.equals(lexer.getClassName()) ){
			this.error(String.format("类名%s和文件名%s不一致",className,lexer.getClassName()));
		}
		move();
		match("{");
		ArrayList<Ast.Method.T> methods = parseMethodList();
		mainClass = new Ast.MainClass.MainClassSingle(className,null,methods);
		match("}");
		match("EOF");
		//System.out.println("语法分析成功");
		return mainClass;
	}

	// <methodList> -> <method>*
	private ArrayList<Ast.Method.T> parseMethodList() throws IOException {
		ArrayList<Ast.Method.T> methods = new ArrayList<Ast.Method.T>();
		while( look.getKind() == TokenKind.Void ||
				look.getKind() == TokenKind.Int ||
				look.getKind() == TokenKind.Float||
				look.getKind() == TokenKind.Bool) {
			methods.add(parseMethod());
		}
		return methods;
	}

	
	// <method> -> void | int | double | methodname ( <inputparams> ) {<varDeclares> <stmts> [return <expr>]}
	private Ast.Method.MethodSingle parseMethod() throws IOException {
		Ast.Type.T t = parseType();
		String methodName = look.getLexeme();
		this.currMethod= methodName;
		this.varTable.clear();
		int lineNumber = look.getLineNumber();
		move();
		match("(");
			ArrayList<Ast.Declare.T> inputParams = parseInputParams();
		match(")");
		match("{");
		ArrayList<Ast.Declare.T> localParams = parseVarDeclares();
		ArrayList<Ast.Stmt.T> stmts = parseStmts();
		match("}");
		table.put(this.currMethod,this.varTable);
		if( !methodName.equals("main")){
			Ast.Stmt.T stmt = stmts.get(stmts.size()-1);
			return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,stmt,lineNumber);
		}else{
			return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,null,lineNumber);
		}

	}

	// <varDeclares> -> <varDeclare>*
	private ArrayList<Ast.Declare.T> parseVarDeclares() throws IOException{
		ArrayList<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		while(look.getKind() == TokenKind.Int ||
				look.getKind() == TokenKind.Float||
				look.getKind() == TokenKind.Bool){
			String id = look.getLexeme();
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
				Ast.Declare.DeclareSingle d = new Ast.Declare.DeclareSingle(type,id,look.getLineNumber());
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
	private ArrayList<Ast.Declare.T> parseInputParams() throws IOException {
		ArrayList<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		if( look.getKind() == TokenKind.Int){
			Ast.Type.T t = parseType();
			String id = look.getLexeme();
			int lineNumber = look.getLineNumber();
			rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			this.varTable.put(id,new Ast.Type.Int());
			while(look.getKind() == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.getLexeme();
				lineNumber = look.getLineNumber();
				rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
				match(new Token(TokenKind.Id));
				this.varTable.put(id,new Ast.Type.Int());
			}
		}else if( look.getKind() == TokenKind.Float){
			Ast.Type.T t = parseType();
			String id = look.getLexeme();
			int lineNumber = look.getLineNumber();
			rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			this.varTable.put(id,new Ast.Type.Float());
			while(look.getKind() == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.getLexeme();
				lineNumber = look.getLineNumber();
				rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
				match(new Token(TokenKind.Id));
				this.varTable.put(id,new Ast.Type.Float());
			}
		}else if( look.getKind() == TokenKind.Bool){
			Ast.Type.T t = parseType();
			String id = look.getLexeme();
			int lineNumber = look.getLineNumber();
			rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
			match(new Token(TokenKind.Id));
			this.varTable.put(id,new Ast.Type.Bool());
			while(look.getKind() == TokenKind.Commer ){
				move();
				t = parseType();
				id = look.getLexeme();
				lineNumber = look.getLineNumber();
				rs.add(new Ast.Declare.DeclareSingle(t, id, lineNumber));
				match(new Token(TokenKind.Id));
				this.varTable.put(id,new Ast.Type.Bool());
			}
		}
		return rs;
	}

	private Ast.Type.T parseType() {
		if( look.getKind() == TokenKind.Int ){
			//varTable.put(look.getLexeme(), new Ast.Type.Int());
			move();
			return new Ast.Type.Int();
		}
		else if(look.getKind() == TokenKind.Void){
			//varTable.put(look.getLexeme(), new Ast.Type.Void());
			move();
			return new Ast.Type.Void();
		}
		else if(look.getKind() == TokenKind.Float){
			//varTable.put(look.getLexeme(), new Ast.Type.Float());
			move();
			return new Ast.Type.Float();
		}else if(look.getKind() == TokenKind.Bool){
			//varTable.put(look.getLexeme(), new Ast.Type.Bool());
			move();
			return new Ast.Type.Bool();
		}
		else 
			error(look.getLexeme());
		return null;
	}
	
	private ArrayList<Ast.Stmt.T> parseStmts() throws IOException {
		ArrayList<Ast.Stmt.T> rs = new ArrayList<Ast.Stmt.T>();
		while( look.getKind() == TokenKind.Printf || 
				look.getKind() == TokenKind.PrintLine ||
				look.getKind() == TokenKind.If ||
				look.getKind() == TokenKind.While ||
				look.getKind() == TokenKind.Lbrace ||
				look.getKind() == TokenKind.Id || 
				look.getKind() == TokenKind.Return){
			rs.add(parseStmt());
		}
		return rs;
	}

	private Ast.Stmt.T parseStmt() throws IOException {
		Ast.Stmt.T stmt = null;
		
		if( look.getKind() == TokenKind.Printf ){
			match(new Token(TokenKind.Printf));
			match(new Token(TokenKind.Lparen));
			String format = look.getLexeme();
			int lineNumber = look.getLineNumber();
			Token ahead = lexer.lookahead(1);
			ArrayList<Ast.Expr.T> exprs = null;
			if( ahead.getKind() == TokenKind.Commer ){
				exprs = new ArrayList<Ast.Expr.T>();
				while( look.getKind() != TokenKind.Rparen ){
					if( look.getKind() == TokenKind.Commer )
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
		else if( look.getKind() == TokenKind.PrintLine ){
			match(new Token(TokenKind.PrintLine));
			match(new Token(TokenKind.Lparen));
			match(new Token(TokenKind.Rparen));
			match( new Token(TokenKind.Semicolon) );
			String format = look.getLexeme();
			int lineNumber = look.getLineNumber();
			stmt = new Ast.Stmt.PrintLine();
		}
		else if( look.getKind() == TokenKind.While ){
			match(new Token(TokenKind.While));
			match(new Token(TokenKind.Lparen));
			int lineNumber = look.getLineNumber();
			Ast.Expr.T condition = parseExpr();
			match(new Token(TokenKind.Rparen));
			Ast.Stmt.T whileStmt = parseStmt();
			stmt = new Ast.Stmt.While(condition, whileStmt, lineNumber);
		}
		else if ( look.getKind() == TokenKind.Id ) {
			Token ahead = lexer.lookahead(1);
			
			// 方法调用
			if( ahead.getKind() == TokenKind.Lparen ){
				String mthName = look.getLexeme();
				int lineNumber = look.getLineNumber();
				Ast.Expr.T expr =  parseMethodCall();
				if( expr instanceof Ast.Expr.Call){
					stmt = new Ast.Stmt.Call(mthName,((Ast.Expr.Call)expr).inputParams,lineNumber);
					move();
				}

			}else{
				String id = look.getLexeme();
				int lineNum = look.getLineNumber();
				match( new Token(TokenKind.Id) );
				match( new Token(TokenKind.Assign) );
				Ast.Expr.T expr = parseExpr();
				match( new Token(TokenKind.Semicolon) );
				stmt = new Ast.Stmt.Assign(new Ast.Expr.Id(id,this.varTable.get(id),lineNum), expr, lineNum);
				
			}
		}
		else if( look.getKind() == TokenKind.Lbrace ) {
			match( "{" );
			int lineNumber = look.getLineNumber();
			stmt = new Ast.Stmt.Block(parseStmts(), lineNumber);
			match( "}" );

		}
		else if( look.getKind() == TokenKind.Return ) {
			match( "return" );
			int lineNumber = look.getLineNumber();
			Ast.Expr.T expr = parseExpr();
			stmt = new Ast.Stmt.Return(expr, lineNumber);
			match( ";" );

		}else if( look.getKind() == TokenKind.If ){
			match( "if" );
			match( "(" );
			int lineNumber = look.getLineNumber();
			Ast.Expr.T condition = parseExpr();
			match( ")" );
			Ast.Stmt.T thenStmt = parseStmt();
			Ast.Stmt.T elseStmt = null;
			if( look.getKind() == TokenKind.Else){

				match( "else" );
				elseStmt = parseStmt();
			}

			stmt = new Ast.Stmt.If(condition, thenStmt, elseStmt, lineNumber);
		}
		return stmt;
	}

	// bool_expr ->
	private Ast.Expr.T parseBoolExpr() throws IOException {
		return null;
	}


	// Exp -> AndExp || AndExp
	//  -> AndExp
	private Ast.Expr.T parseExpr() throws IOException {
		Ast.Expr.T expr = parseAndExpr();
		while( look.getKind() == TokenKind.Or ) {
			move();
			Ast.Expr.T right = parseAndExpr();
			expr = new Ast.Expr.Or(expr, right, expr.lineNum);
		}
		return expr;
	}



	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Ast.Expr.T parseAndExpr() throws IOException {
		Ast.Expr.T expr = parseRelationExpr();
		while( look.getKind() == TokenKind.And) {
			move();
			Ast.Expr.T right = parseRelationExpr();
			expr = new Ast.Expr.And(expr, right, expr.lineNum);
		}
		return expr;
	}

	// <relation_expr> -> additive_expr |<additive_expr>(>|<|>=|<=|==|!=)<additive_expr>
	private Ast.Expr.T parseRelationExpr() throws IOException {
		Ast.Expr.T expr = parseAdditiveExpr();
		while( look.getKind() == TokenKind.LT ||
				look.getKind() == TokenKind.GT ||
				look.getKind() == TokenKind.LTE ||
				look.getKind() == TokenKind.GTE ||
				look.getKind() == TokenKind.NEQ ||
				look.getKind() == TokenKind.EQ ) {
			String operator = look.getLexeme();
			move();
			Ast.Expr.T expr1 = parseAdditiveExpr();
			switch (operator) {
			case ">":
				expr = new Ast.Expr.GT(expr, expr1, look.getLineNumber());
				break;
			case "<":
				expr = new Ast.Expr.LT(expr, expr1, look.getLineNumber());
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
		while(look.getKind()==TokenKind.Add
				||look.getKind()==TokenKind.Sub) {
			Token temp=look;
			move();
			Ast.Expr.T otherExpr = parseTerm();
			if(temp.getKind()==TokenKind.Add) {
				expr = new Ast.Expr.Add(expr, otherExpr, look.getLineNumber());
			}else {
				expr = new Ast.Expr.Sub(expr, otherExpr, look.getLineNumber());
			}
		}
		return expr;
	}

	// <term> -> <factor> *|/ <factor>
	private Ast.Expr.T parseTerm() throws IOException{
		Ast.Expr.T expr = parseFactor();
		while(look.getKind()==TokenKind.Mul
				||look.getKind()==TokenKind.Div) {
			Token temp=look;
			move();
			Ast.Expr.T otherExpr = parseFactor();
			if(temp.getKind() == TokenKind.Mul) {
				expr = new Ast.Expr.Mul(expr, otherExpr, look.getLineNumber());
			}else {
				expr = new Ast.Expr.Div(expr, otherExpr, look.getLineNumber());
			}
		}
		return expr;
	}


	// <factor> -> (<expression>)
	//  		| Integer Literal
	//  		| id
	//          | not(<expression>)
	private Ast.Expr.T parseFactor() throws IOException{
		Ast.Expr.T expr = null;
		if(look.getKind()==TokenKind.Lparen){
			move();
			expr = parseExpr();
			match(new Token(TokenKind.Rparen));
			return expr;
		}else if(look.getKind()== Num){
			expr = new Ast.Expr.Number(new Ast.Type.Int(),look.getLexeme(),look.getLineNumber());
			move();
			return expr;
		}else if(look.getKind()==TokenKind.DNum){
			expr = new Ast.Expr.Number(new Ast.Type.Float(),look.getLexeme(),look.getLineNumber());
			move();
			return expr;
		}else if( look.getKind()==TokenKind.Id ){
			Token temp = look;
			Token ahead = lexer.lookahead(1);
			if( ahead.getKind() == TokenKind.Lparen){
				expr = parseMethodCall();
			}else{
				expr = new Ast.Expr.Id(look.getLexeme(),this.varTable.get(look.getLexeme()),look.getLineNumber());
				move();
			}
			return expr;
		}
		else if(look.getKind()==TokenKind.String ){
			expr = new Ast.Expr.Str(look.getLexeme(), look.getLineNumber());
			move();
			return expr;
		}
		else if(look.getKind()==TokenKind.Not ){
			move();
			match("(");
			expr = new Ast.Expr.Not(parseExpr());
			match(")");
			return expr;
		}
		else if(look.getKind()==TokenKind.True ){
			expr = new Ast.Expr.True(look.getLineNumber());
			move();
			return expr;
		}
		else if(look.getKind()==TokenKind.False ){
			expr = new Ast.Expr.False(look.getLineNumber());
			move();
			return expr;
		}
		else{
			System.out.println("near line : "+look.getLineNumber()+" syntax error: "+"excepted get identifier or expression or number or String, but got "+look.getLexeme());
			System.exit(1);
		}
		return expr;
	}

	private Ast.Expr.T parseMethodCall2() throws IOException {
		Token ahead;
		Ast.Expr.T expr;
		String methodName = look.getLexeme();
		int lineNumber = look.getLineNumber();
		move();
		match("(");
		ArrayList<Ast.Expr.T> args = null;
		args = new ArrayList<Ast.Expr.T>();
		ahead = lexer.lookahead(1);
		if( ahead.getKind() == TokenKind.Add || ahead.getKind() == TokenKind.Sub){
            args.add(parseAdditiveExpr()); //2019/8/27 测试BoolTest11时注释掉
        }else{
            while( look.getKind() == TokenKind.Id || look.getKind() == TokenKind.Num  || look.getKind() == TokenKind.DNum
            || look.getKind() == TokenKind.True || look.getKind() == TokenKind.False){
                if( look.getKind() == TokenKind.Id)
                    args.add(parseFactor());
                else if( look.getKind() == TokenKind.Num ){
					args.add(new Ast.Expr.Number(new Ast.Type.Int(),look.getLexeme(),look.getLineNumber()));
					move();
				}

                else if( look.getKind() == TokenKind.DNum ) {
					args.add(new Ast.Expr.Number(new Ast.Type.Float(), look.getLexeme(), look.getLineNumber()));
					move();
				}
                else if( look.getKind() == TokenKind.True ){
                    args.add(new Ast.Expr.True(look.getLineNumber()));
                    move();
                }

                else if( look.getKind() == TokenKind.False ){
                    args.add(new Ast.Expr.False(look.getLineNumber()));
                    move();
                }
                if( look.getKind() == TokenKind.Commer)
                    move();
            }
        }
		match(")");
		expr = new Ast.Expr.Call(methodName, args, lineNumber);
		return expr;
	}

	// methodCall->methodCall(Expr,Expr)
	private Ast.Expr.T parseMethodCall() throws IOException {
		Token ahead;
		Ast.Expr.T expr;
		String methodName = look.getLexeme();
		int lineNumber = look.getLineNumber();
		move();
		match("(");
		ArrayList<Ast.Expr.T> args = null;
		args = new ArrayList<Ast.Expr.T>();
		ahead = lexer.lookahead(1);
		if( look.getKind() == TokenKind.Rparen){

		}else{
			Ast.Expr.T e = parseExpr();
			args.add(e);
			while( look.getKind() == TokenKind.Commer){
				match(",");
				args.add(parseExpr());
			}
		}

		match(")");
		expr = new Ast.Expr.Call(methodName, args, lineNumber);
		return expr;
	}
}
