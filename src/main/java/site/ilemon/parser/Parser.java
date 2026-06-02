package site.ilemon.parser;

import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;
import site.ilemon.exception.ParseException;

import java.io.IOException;
import java.util.ArrayList;

import static site.ilemon.lexer.TokenKind.Num;


/**
 * 递归下降语法分析器。
 *
 * <p>将 {@link Lexer} 产生的 Token 流按照 Lemon 语言的 BNF 文法进行分析，
 * 构建出抽象语法树（AST）。采用自顶向下的 LL(2) 分析策略，通过
 * {@code lookahead(1)} / {@code lookahead(2)} 处理文法歧义。</p>
 *
 * <h3>核心文法规则</h3>
 * <pre>
 * program    ::= "class" id "{" method* "}"
 * method     ::= type id "(" params? ")" "{" varDecl* stmt* "}"
 * stmt       ::= assign | if | while | block | return | printf | call
 * expr       ::= andExpr ("||" andExpr)*    -- 运算符优先级从低到高
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * Lexer lexer = new Lexer(new File("Hello.lemon"));
 * Parser parser = new Parser(lexer);
 * Ast.Program.T ast = parser.parse();
 * </pre>
 *
 * @author andy
 * @see Lexer
 * @see Ast.Program
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
	 * @param lexeme
	 * @throws IOException
	 */
	private void match(String lexeme) throws IOException{
		if( lexeme.equals(look.lexeme))
			move();
		else
			expected(lexeme);
	}
	
	private void match(Token token) throws IOException{
		if( token.kind == look.kind ){
			move();
		}else{
			expected(token.kind.toString());
		}
	}

	private void expected(String s) {
		throw new ParseException(formatError("语法错误，期望 '" + s + "'，实际得到 '" + look.lexeme + "'"));
	}

	private void error(String message) {
		throw new ParseException(formatError(message + "，当前 token 为 '" + look.lexeme + "'"));
	}

	/**
	 * 语法分析入口
	 * @return Program
	 * @throws IOException
	 */
	private String formatError(String message) {
		String sourceLine = lexer.getSourceLine(look.lineNumber);
		StringBuilder result = new StringBuilder();
		result.append(String.format("[语法分析] 行 %d, 列 %d: %s",
				look.lineNumber, look.columnNumber, message));
		if (sourceLine != null && !sourceLine.isEmpty()) {
			result.append(System.lineSeparator());
			result.append("    ").append(sourceLine).append(System.lineSeparator());
			result.append("    ");
			for (int i = 1; i < look.columnNumber; i++) {
				result.append(' ');
			}
			result.append('^');
		}
		return result.toString();
	}

	public Ast.Program.T parse() throws IOException{
		Ast.MainClass.MainClassSingle mainClass = parseMainClass();
		Ast.Program.T programSingle = new Ast.Program.ProgramSingle(mainClass);
		return programSingle;
	}


	// <mainClass> -> class <name> { <methodList>}
	private Ast.MainClass.MainClassSingle parseMainClass() throws IOException {
		Ast.MainClass.MainClassSingle mainClass = null;
		match("class");
		String className = look.lexeme;
		// 检查class名称是否一致
		if( !className.equals(lexer.getClassName()) ){
			this.error(String.format("类名 '%s' 与文件名 '%s' 不一致", className, lexer.getClassName()));
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
		while( look.kind == TokenKind.Void ||
				look.kind == TokenKind.Int ||
				look.kind == TokenKind.Float||
				look.kind == TokenKind.Double||
				look.kind == TokenKind.Bool) {
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
		if( !methodName.equals("main")){
			Ast.Stmt.T stmt = stmts.isEmpty() ? null : stmts.get(stmts.size()-1);
			return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,stmt,lineNumber);
		}else{
			return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,null,lineNumber);
		}

	}

	// <varDeclares> -> <varDeclare>*
	private ArrayList<Ast.Declare.T> parseVarDeclares() throws IOException{
		ArrayList<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		while(isVarDeclarationStart()){
			rs.add(parseDeclare());
		}
		return rs;

	}

	private boolean isVarDeclarationStart() {
		if (!isTypeToken(look.kind)) {
			return false;
		}
		Token id = lexer.lookahead(1);
		Token afterId = lexer.lookahead(2);
		return id != null && id.kind == TokenKind.Id
				&& afterId != null
				&& (afterId.kind == TokenKind.Semicolon || afterId.kind == TokenKind.Lbracket);
	}

	// // <declare> -> type id; | type id[size];
	private Ast.Declare.T parseDeclare() throws IOException {
		Ast.Type.T type = parseType();
		if( look.kind == TokenKind.Id ){
			String id = look.lexeme;
			int lineNumber = look.lineNumber;
			move();
			// type id[size]; - 数组声明
			if( look.kind == TokenKind.Lbracket) {
				match("[");
				int size;
				try {
					size = Integer.parseInt(look.lexeme);
				} catch (NumberFormatException e) {
					error(String.format("数组大小必须是整数，但得到: '%s'", look.lexeme));
					return null;
				}
				if( size <= 0 ){
					error(String.format("数组大小必须为正整数，但得到: %d", size));
					return null;
				}
				match(new Token(TokenKind.Num));
				match("]");
				match(";");
				// 根据基础类型创建数组类型
				Ast.Type.T arrayType = createArrayType(type, size);
				if( arrayType == null ){
					error(String.format("不支持的数组基础类型: %s", type));
					return null;
				}
				return new Ast.Declare.DeclareSingle(arrayType, id, lineNumber);
			}
			// type id;
			else if( look.kind == TokenKind.Semicolon) {
				Ast.Declare.DeclareSingle d = new Ast.Declare.DeclareSingle(type,id,lineNumber);
				match(";");
				return d;
			}
			else {
				error(String.format("声明语句格式错误，变量 '%s' 后期望 ';' 或数组下标声明", id));
				return null;
			}

		}else {
			error("声明语句格式错误，类型后必须跟变量名");
			return null;
		}
	}

	// 根据基础类型创建对应的数组类型
	private Ast.Type.T createArrayType(Ast.Type.T baseType, int size) {
		if (baseType instanceof Ast.Type.Int) {
			return new Ast.Type.IntArray(size);
		} else if (baseType instanceof Ast.Type.Float) {
			return new Ast.Type.FloatArray(size);
		} else if (baseType instanceof Ast.Type.Double) {
			return new Ast.Type.DoubleArray(size);
		} else if (baseType instanceof Ast.Type.Bool) {
			return new Ast.Type.BoolArray(size);
		}
		return null;
	}

	// <inputparams> -> <formalParam> ("," <formalParam>)*
	private ArrayList<Ast.Declare.T> parseInputParams() throws IOException {
		ArrayList<Ast.Declare.T> rs = new ArrayList<Ast.Declare.T>();
		if( isTypeToken(look.kind) ){
			rs.add(parseFormalParam());
			while(look.kind == TokenKind.Comma ){
				move();
				rs.add(parseFormalParam());
			}
		}
		return rs;
	}

	// <formalParam> -> type id | type id "[" "]"
	private Ast.Declare.T parseFormalParam() throws IOException {
		Ast.Type.T type = parseType();
		String id = look.lexeme;
		int lineNumber = look.lineNumber;
		match(new Token(TokenKind.Id));
		if (look.kind == TokenKind.Lbracket) {
			match("[");
			match("]");
			Ast.Type.T arrayType = createArrayType(type, -1);
			if (arrayType == null) {
				error(String.format("不支持的数组参数基础类型: %s", type));
				return null;
			}
			type = arrayType;
		}
		return new Ast.Declare.DeclareSingle(type, id, lineNumber);
	}

	/**
	 * 判断当前 token 是否为类型关键字
	 */
	private boolean isTypeToken(TokenKind kind) {
		return kind == TokenKind.Int || kind == TokenKind.Float
				|| kind == TokenKind.Double || kind == TokenKind.Bool;
	}


	private Ast.Type.T parseType() {
		if( look.kind == TokenKind.Int ){
			move();
			return new Ast.Type.Int();
		}
		else if(look.kind == TokenKind.Void){
			move();
			return new Ast.Type.Void();
		}
		else if(look.kind == TokenKind.Float){
			move();
			return new Ast.Type.Float();
		}
		else if(look.kind == TokenKind.Double){
			move();
			return new Ast.Type.Double();
		}
		else if(look.kind == TokenKind.Bool){
			move();
			return new Ast.Type.Bool();
		}
		else 
			error("期望类型关键字 int、float、double、bool 或 void");
		return null;
	}
	
	private ArrayList<Ast.Stmt.T> parseStmts() throws IOException {
		ArrayList<Ast.Stmt.T> rs = new ArrayList<Ast.Stmt.T>();
		while( look.kind == TokenKind.Printf || 
				look.kind == TokenKind.PrintLine ||
				look.kind == TokenKind.If ||
				look.kind == TokenKind.While ||
				look.kind == TokenKind.For ||
				look.kind == TokenKind.Lbrace ||
				look.kind == TokenKind.Id ||
				look.kind == TokenKind.Break ||
				look.kind == TokenKind.Continue ||
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
			if (look.kind != TokenKind.String) {
				error("printf 的第一个参数必须是字符串格式");
			}
			String format = look.lexeme;
			int lineNumber = look.lineNumber;
			match(new Token(TokenKind.String));
			ArrayList<Ast.Expr.T> exprs = new ArrayList<Ast.Expr.T>();
			while( look.kind == TokenKind.Comma ){
				match(new Token(TokenKind.Comma));
				exprs.add(parseExpr());
			}
			match( new Token(TokenKind.Rparen) );
			match( new Token(TokenKind.Semicolon) );
			stmt = new Ast.Stmt.Printf(format,exprs,lineNumber);
		}
		else if( look.kind == TokenKind.PrintLine ){
			int lineNumber = look.lineNumber;
			match(new Token(TokenKind.PrintLine));
			match(new Token(TokenKind.Lparen));
			match(new Token(TokenKind.Rparen));
			match( new Token(TokenKind.Semicolon) );
			stmt = new Ast.Stmt.PrintLine();
		}
		else if( look.kind == TokenKind.Break ){
			int lineNumber = look.lineNumber;
			match(new Token(TokenKind.Break));
			match(new Token(TokenKind.Semicolon));
			stmt = new Ast.Stmt.Break(lineNumber);
		}
		else if( look.kind == TokenKind.Continue ){
			int lineNumber = look.lineNumber;
			match(new Token(TokenKind.Continue));
			match(new Token(TokenKind.Semicolon));
			stmt = new Ast.Stmt.Continue(lineNumber);
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
		else if( look.kind == TokenKind.For ){
			int lineNumber = look.lineNumber;
			match(new Token(TokenKind.For));
			match(new Token(TokenKind.Lparen));
			Ast.Stmt.T init = null;
			if (look.kind != TokenKind.Semicolon) {
				init = parseSimpleStmtWithoutTerminator();
			}
			match(new Token(TokenKind.Semicolon));
			Ast.Expr.T condition = look.kind == TokenKind.Semicolon
					? new Ast.Expr.True(lineNumber)
					: parseExpr();
			match(new Token(TokenKind.Semicolon));
			Ast.Stmt.T update = null;
			if (look.kind != TokenKind.Rparen) {
				update = parseSimpleStmtWithoutTerminator();
			}
			match(new Token(TokenKind.Rparen));
			Ast.Stmt.T body = parseStmt();
			stmt = new Ast.Stmt.For(init, condition, update, body, lineNumber);
		}
		else if ( look.kind == TokenKind.Id ) {
			Token ahead = lexer.lookahead(1);
			
			// 方法调用
			if( ahead.kind == TokenKind.Lparen ){
				String mthName = look.lexeme;
				int lineNumber = look.lineNumber;
				Ast.Expr.T expr =  parseMethodCall();
				if( expr instanceof Ast.Expr.Call){
					stmt = new Ast.Stmt.Call(mthName,((Ast.Expr.Call)expr).getInputParams(),lineNumber);
					match(new Token(TokenKind.Semicolon));
				}

			}
			// 数组赋值: arr[i] = expr;
			else if( ahead.kind == TokenKind.Lbracket ){
				String arrayName = look.lexeme;
				int lineNum = look.lineNumber;
				match( new Token(TokenKind.Id) );
				match( "[" );
				Ast.Expr.T index = parseExpr();
				match( "]" );
				match( new Token(TokenKind.Assign) );
				Ast.Expr.T expr = parseExpr();
				match( new Token(TokenKind.Semicolon) );
				stmt = new Ast.Stmt.ArrayAssign(arrayName, index, expr, lineNum);
			}
			else{
				String id = look.lexeme;
				int lineNum = look.lineNumber;
				match( new Token(TokenKind.Id) );
				match( new Token(TokenKind.Assign) );
				Ast.Expr.T expr = parseExpr();
				match( new Token(TokenKind.Semicolon) );
				stmt = new Ast.Stmt.Assign(new Ast.Expr.Id(id,lineNum), expr, lineNum);
				
			}
		}
		else if( look.kind == TokenKind.Lbrace ) {
			match( "{" );
			int lineNumber = look.lineNumber;
			stmt = new Ast.Stmt.Block(parseStmts(), lineNumber);
			match( "}" );

		}
		else if( look.kind == TokenKind.Return ) {
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
			Ast.Stmt.T elseStmt = null;
			if( look.kind == TokenKind.Else){

				match( "else" );
				elseStmt = parseStmt();
			}

			stmt = new Ast.Stmt.If(condition, thenStmt, elseStmt, lineNumber);
		}
		return stmt;
	}

	private Ast.Stmt.T parseSimpleStmtWithoutTerminator() throws IOException {
		if (look.kind != TokenKind.Id) {
			error("期望赋值语句或方法调用");
		}
		Token ahead = lexer.lookahead(1);
		if( ahead.kind == TokenKind.Lparen ){
			String mthName = look.lexeme;
			int lineNumber = look.lineNumber;
			Ast.Expr.T expr = parseMethodCall();
			if( expr instanceof Ast.Expr.Call){
				return new Ast.Stmt.Call(mthName,((Ast.Expr.Call)expr).getInputParams(),lineNumber);
			}
		}
		else if( ahead.kind == TokenKind.Lbracket ){
			String arrayName = look.lexeme;
			int lineNum = look.lineNumber;
			match( new Token(TokenKind.Id) );
			match( "[" );
			Ast.Expr.T index = parseExpr();
			match( "]" );
			match( new Token(TokenKind.Assign) );
			Ast.Expr.T expr = parseExpr();
			return new Ast.Stmt.ArrayAssign(arrayName, index, expr, lineNum);
		}
		else{
			String id = look.lexeme;
			int lineNum = look.lineNumber;
			match( new Token(TokenKind.Id) );
			match( new Token(TokenKind.Assign) );
			Ast.Expr.T expr = parseExpr();
			return new Ast.Stmt.Assign(new Ast.Expr.Id(id,lineNum), expr, lineNum);
		}
		error("无法解析简单语句");
		return null;
	}




	// Exp -> AndExp || AndExp
	//  -> AndExp
	private Ast.Expr.T parseExpr() throws IOException {
		Ast.Expr.T expr = parseAndExpr();
		while( look.kind == TokenKind.Or ) {
			move();
			Ast.Expr.T right = parseAndExpr();
			expr = new Ast.Expr.Or(expr, right, expr.getLineNum());
		}
		return expr;
	}



	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Ast.Expr.T parseAndExpr() throws IOException {
		Ast.Expr.T expr = parseRelationExpr();
		while( look.kind == TokenKind.And) {
			move();
			Ast.Expr.T right = parseRelationExpr();
			expr = new Ast.Expr.And(expr, right, expr.getLineNum());
		}
		return expr;
	}

	// <relation_expr> -> additive_expr |<additive_expr>(>|<|>=|<=|==|!=)<additive_expr>
	private Ast.Expr.T parseRelationExpr() throws IOException {
		Ast.Expr.T expr = parseAdditiveExpr();
		while( look.kind == TokenKind.LT ||
				look.kind == TokenKind.GT ||
				look.kind == TokenKind.LTE ||
				look.kind == TokenKind.GTE ||
				look.kind == TokenKind.NEQ ||
				look.kind == TokenKind.EQ ) {
			String operator = look.lexeme;
			int lineNumber = look.lineNumber;
			move();
			Ast.Expr.T right = parseAdditiveExpr();
			switch (operator) {
			case ">":
				expr = new Ast.Expr.GT(expr, right, lineNumber);
				break;
			case "<":
				expr = new Ast.Expr.LT(expr, right, lineNumber);
				break;
			case ">=":
				expr = new Ast.Expr.GTE(expr, right, lineNumber);
				break;
			case "<=":
				expr = new Ast.Expr.LTE(expr, right, lineNumber);
				break;
			case "==":
				expr = new Ast.Expr.EQ(expr, right, lineNumber);
				break;
			case "!=":
				expr = new Ast.Expr.NEQ(expr, right, lineNumber);
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
				||look.kind==TokenKind.Div
				||look.kind==TokenKind.Mod) {
			Token temp=look;
			move();
			Ast.Expr.T otherExpr = parseFactor();
			if(temp.kind == TokenKind.Mul) {
				expr = new Ast.Expr.Mul(expr, otherExpr, look.lineNumber);
			}else if(temp.kind == TokenKind.Div) {
				expr = new Ast.Expr.Div(expr, otherExpr, look.lineNumber);
			}else {
				expr = new Ast.Expr.Mod(expr, otherExpr, look.lineNumber);
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
		if(look.kind==TokenKind.Lparen){
			move();
			expr = parseExpr();
			match(new Token(TokenKind.Rparen));
			return expr;
		}else if(look.kind==TokenKind.Sub){
			int lineNumber = look.lineNumber;
			move();
			Ast.Expr.T operand = parseFactor();
			return new Ast.Expr.Sub(new Ast.Expr.Number(new Ast.Type.Int(), 0, lineNumber),
					operand, lineNumber);
		}else if(look.kind== Num){
			expr = new Ast.Expr.Number(new Ast.Type.Int(),look.lexeme,look.lineNumber);
			move();
			return expr;
		}else if(look.kind==TokenKind.FloatLiteral){
			// 浮点数字面量默认为float类型（保持向后兼容）
			expr = new Ast.Expr.Number(new Ast.Type.Float(),look.lexeme,look.lineNumber);
			move();
			return expr;
		}else if( look.kind==TokenKind.Id ){
			Token temp = look;
			Token ahead = lexer.lookahead(1);
			if( ahead.kind == TokenKind.Lparen){
				expr = parseMethodCall();
			}
			// 数组访问: arr[i]
			else if( ahead.kind == TokenKind.Lbracket){
				String arrayName = look.lexeme;
				int lineNum = look.lineNumber;
				move(); // consume id
				match("[");
				Ast.Expr.T index = parseExpr();
				match("]");
				expr = new Ast.Expr.ArrayAccess(arrayName, index, lineNum);
			}
			else if( ahead.kind == TokenKind.Dot){
				String arrayName = look.lexeme;
				int lineNum = look.lineNumber;
				move(); // consume id
				match(".");
				if (!"length".equals(look.lexeme)) {
					error("数组属性只支持 length");
				}
				match(new Token(TokenKind.Id));
				expr = new Ast.Expr.ArrayLength(arrayName, lineNum);
			}
			else{
				expr = new Ast.Expr.Id(look.lexeme,look.lineNumber);
				move();
			}
			return expr;
		}
		else if(look.kind==TokenKind.String ){
			expr = new Ast.Expr.Str(look.lexeme, look.lineNumber);
			move();
			return expr;
		}
		else if(look.kind==TokenKind.Not ){
			move();
			match("(");
			expr = new Ast.Expr.Not(parseExpr());
			match(")");
			return expr;
		}
		else if(look.kind==TokenKind.True ){
			expr = new Ast.Expr.True(look.lineNumber);
			move();
			return expr;
		}
		else if(look.kind==TokenKind.False ){
			expr = new Ast.Expr.False(look.lineNumber);
			move();
			return expr;
		}
		else{
			throw new ParseException(String.format(
				"[语法分析] 行 %d: 语法错误，期望标识符、表达式、数字或字符串，实际得到 '%s'",
				look.lineNumber, look.lexeme));
		}
	}



	// methodCall->methodCall(Expr,Expr)
	private Ast.Expr.T parseMethodCall() throws IOException {
		Token ahead;
		Ast.Expr.T expr;
		String methodName = look.lexeme;
		int lineNumber = look.lineNumber;
		move();
		match("(");
		ArrayList<Ast.Expr.T> args = null;
		args = new ArrayList<Ast.Expr.T>();
		ahead = lexer.lookahead(1);
		if( look.kind == TokenKind.Rparen){

		}else{
			Ast.Expr.T e = parseExpr();
			args.add(e);
			while( look.kind == TokenKind.Comma){
				match(",");
				args.add(parseExpr());
			}
		}

		match(")");
		expr = new Ast.Expr.Call(methodName, args, lineNumber);
		return expr;
	}
}
