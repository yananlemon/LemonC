package site.ilemon.parser;

import site.ilemon.ast.Ast;
import site.ilemon.lexer.IntegerLiterals;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;
import site.ilemon.exception.ParseException;
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
 * method     ::= type id "(" params? ")" "{" blockItem* "}"
 * blockItem  ::= localDecl | stmt
 * stmt       ::= assign | if | while | block | return | printf | call
 * expr       ::= andExpr ("||" andExpr)*    -- 运算符优先级从低到高
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * Lexer lexer = new Lexer(new File("Hello.lemon"));
 * Parser parser = new Parser(lexer);
 * Ast.Program.Base ast = parser.parse();
 * </pre>
 *
 * @author andy
 * @see Lexer
 * @see Ast.Program
 */
public class Parser {
	private static final int MAX_DIAGNOSTICS = 100;

	private Lexer lexer; // 词法分析器

	private Token look;  // 当前token

	private ArrayList<Ast.Declare.Base> currentMethodLocals;

	private final ArrayList<ParseDiagnostic> diagnostics = new ArrayList<ParseDiagnostic>();

	private boolean collectingErrors;

	public Parser(Lexer lexer) {
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


	private void match(TokenKind kind) {
		if( kind == look.getKind() ){
			move();
		}else{
			expected(kind.toString());
		}
	}

	private void expected(String s) {
		throw syntaxError("语法错误，期望 '" + s + "'，实际得到 '" + look.getLexeme() + "'");
	}

	private void error(String message) {
		throw syntaxError(message + "，当前 token 为 '" + look.getLexeme() + "'");
	}

	private ParseException syntaxError(String message) {
		return new ParseException(formatError(message));
	}

	private Token consumeName(String description, boolean allowMain) {
		if (look.getKind() != TokenKind.Id && !(allowMain && look.getKind() == TokenKind.Main)) {
			throw syntaxError("期望" + description + "，实际得到 '" + look.getLexeme() + "'");
		}
		Token name = look;
		move();
		return name;
	}

	private String formatError(String message) {
		String sourceLine = lexer.getSourceLine(look.getLineNumber());
		StringBuilder result = new StringBuilder();
		result.append(String.format("[语法分析] 行 %d, 列 %d: %s",
				look.getLineNumber(), look.getColumnNumber(), message));
		if (sourceLine != null && !sourceLine.isEmpty()) {
			result.append(System.lineSeparator());
			result.append("    ").append(sourceLine).append(System.lineSeparator());
			result.append("    ");
			for (int i = 1; i < look.getColumnNumber(); i++) {
				result.append(' ');
			}
			result.append('^');
		}
		return result.toString();
	}

	/**
	 * 语法分析入口
	 * @return Program
	 */
	public Ast.Program.Base parse() {
		Ast.MainClass.MainClassSingle mainClass = parseMainClass();
		Ast.Program.Base programSingle = new Ast.Program.ProgramSingle(mainClass);
		return programSingle;
	}

	public ParseResult parseCollecting() {
		this.diagnostics.clear();
		this.collectingErrors = true;
		Ast.Program.Base program = null;
		try {
			program = parse();
		} catch (ParseException e) {
			recordDiagnostic(e);
		} finally {
			this.collectingErrors = false;
		}
		return new ParseResult(program, this.diagnostics);
	}


	// <mainClass> -> class <name> { <methodList>}
	private Ast.MainClass.MainClassSingle parseMainClass() {
		Ast.MainClass.MainClassSingle mainClass = null;
		match(TokenKind.Class);
		String className = consumeName("类名", false).getLexeme();
		// 检查class名称是否一致
		if( !className.equals(lexer.getClassName()) ){
			this.error(String.format("类名 '%s' 与文件名 '%s' 不一致", className, lexer.getClassName()));
		}
		match(TokenKind.Lbrace);
		ArrayList<Ast.Method.Base> methods = parseMethodList();
		mainClass = new Ast.MainClass.MainClassSingle(className, methods);
		match(TokenKind.Rbrace);
		match(TokenKind.EOF);
		//System.out.println("语法分析成功");
		return mainClass;
	}

	// <methodList> -> <method>*
	private ArrayList<Ast.Method.Base> parseMethodList() {
		ArrayList<Ast.Method.Base> methods = new ArrayList<Ast.Method.Base>();
		while (look.getKind() != TokenKind.Rbrace && look.getKind() != TokenKind.EOF) {
			if (!isMethodStart(look.getKind())) {
				ParseException error = syntaxError("期望方法声明，实际得到 '" + look.getLexeme() + "'");
				if (!this.collectingErrors) {
					throw error;
				}
				recordDiagnostic(error);
				synchronizeMethod();
				continue;
			}
			try {
				methods.add(parseMethod());
			} catch (ParseException e) {
				if (!this.collectingErrors) {
					throw e;
				}
				recordDiagnostic(e);
				synchronizeMethod();
			}
		}
		return methods;
	}

	
	// <method> -> void | int | double | methodname ( <inputparams> ) {<varDeclares> <stmts> [return <expr>]}
	private Ast.Method.MethodSingle parseMethod() {
		Ast.Type.Base t = parseType();
		Token methodNameToken = consumeName("方法名", true);
		String methodName = methodNameToken.getLexeme();
		int lineNumber = methodNameToken.getLineNumber();
		match(TokenKind.Lparen);
			ArrayList<Ast.Declare.Base> inputParams = parseInputParams();
		match(TokenKind.Rparen);
		match(TokenKind.Lbrace);
		ArrayList<Ast.Declare.Base> localParams = new ArrayList<Ast.Declare.Base>();
		this.currentMethodLocals = localParams;
		ArrayList<Ast.Stmt.Base> stmts = parseBlockItems();
		this.currentMethodLocals = null;
		match(TokenKind.Rbrace);
		if( !methodName.equals("main")){
			Ast.Stmt.Base stmt = stmts.isEmpty() ? null : stmts.get(stmts.size()-1);
			return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,stmt,lineNumber);
		}else{
			return new Ast.Method.MethodSingle(t,methodName,inputParams,localParams,stmts,null,lineNumber);
		}

	}

	// <localDecl> -> type id ("[" num "]")? ("=" expr)? ";"
	private Ast.Stmt.VarDecl parseLocalDeclaration() {
		Ast.Type.Base type = parseType();
		Token idToken = consumeName("变量名", false);
		String id = idToken.getLexeme();
		int lineNumber = idToken.getLineNumber();
		boolean array = false;
		if (look.getKind() == TokenKind.Lbracket) {
			array = true;
			match(TokenKind.Lbracket);
			if (look.getKind() != TokenKind.Num) {
				error("数组大小必须是整数");
			}
			int size;
			try {
				size = IntegerLiterals.parse(look.getLexeme());
			} catch (NumberFormatException e) {
				throw syntaxError(String.format(
						"数组大小必须是整数，但得到: '%s'", look.getLexeme()));
			}
			if (size <= 0) {
				error(String.format("数组大小必须为正整数，但得到: %d", size));
			}
			match(TokenKind.Num);
			match(TokenKind.Rbracket);
			type = createArrayType(type, size);
			if (type == null) {
				error("不支持的数组基础类型");
			}
		}

		Ast.Expr.Base initializer = null;
		if (look.getKind() == TokenKind.Assign) {
			if (array) {
				error("数组声明暂不支持初始化表达式");
			}
			match(TokenKind.Assign);
			initializer = parseExpr();
		}
		match(TokenKind.Semicolon);

		Ast.Declare.DeclareSingle declaration =
				new Ast.Declare.DeclareSingle(type, id, lineNumber);
		if (this.currentMethodLocals == null) {
			throw syntaxError("局部变量声明只能出现在方法体中");
		}
		this.currentMethodLocals.add(declaration);
		return new Ast.Stmt.VarDecl(declaration, initializer);
	}

	// 根据基础类型创建对应的数组类型
	private Ast.Type.Base createArrayType(Ast.Type.Base baseType, int size) {
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
	private ArrayList<Ast.Declare.Base> parseInputParams() {
		ArrayList<Ast.Declare.Base> rs = new ArrayList<Ast.Declare.Base>();
		if( isTypeToken(look.getKind()) ){
			rs.add(parseFormalParam());
			while(look.getKind() == TokenKind.Comma ){
				move();
				rs.add(parseFormalParam());
			}
		}
		return rs;
	}

	// <formalParam> -> type id | type id "[" "]"
	private Ast.Declare.Base parseFormalParam() {
		Ast.Type.Base type = parseType();
		String id = look.getLexeme();
		int lineNumber = look.getLineNumber();
		match(TokenKind.Id);
		if (look.getKind() == TokenKind.Lbracket) {
			match(TokenKind.Lbracket);
			match(TokenKind.Rbracket);
			Ast.Type.Base arrayType = createArrayType(type, -1);
			if (arrayType == null) {
				throw syntaxError(String.format("不支持的数组参数基础类型: %s", type));
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


	private Ast.Type.Base parseType() {
		if( look.getKind() == TokenKind.Int ){
			move();
			return new Ast.Type.Int();
		}
		else if(look.getKind() == TokenKind.Void){
			move();
			return new Ast.Type.Void();
		}
		else if(look.getKind() == TokenKind.Float){
			move();
			return new Ast.Type.Float();
		}
		else if(look.getKind() == TokenKind.Double){
			move();
			return new Ast.Type.Double();
		}
		else if(look.getKind() == TokenKind.Bool){
			move();
			return new Ast.Type.Bool();
		}
		throw syntaxError("期望类型关键字 int、float、double、bool 或 void");
	}

	private boolean isMethodStart(TokenKind kind) {
		return kind == TokenKind.Void || isTypeToken(kind);
	}
	
	private ArrayList<Ast.Stmt.Base> parseBlockItems() {
		ArrayList<Ast.Stmt.Base> rs = new ArrayList<Ast.Stmt.Base>();
		while (look.getKind() != TokenKind.Rbrace && look.getKind() != TokenKind.EOF) {
			Token itemStart = look;
			try {
				if (isTypeToken(look.getKind())) {
					rs.add(parseLocalDeclaration());
				} else {
					rs.add(parseStmt());
				}
			} catch (ParseException e) {
				if (!this.collectingErrors) {
					throw e;
				}
				recordDiagnostic(e);
				synchronizeStatement(itemStart);
			}
		}
		return rs;
	}

	private boolean isStatementStart(TokenKind kind) {
		return kind == TokenKind.Printf || kind == TokenKind.PrintLine
				|| kind == TokenKind.If || kind == TokenKind.While
				|| kind == TokenKind.For || kind == TokenKind.Lbrace
				|| kind == TokenKind.Id || kind == TokenKind.Break
				|| kind == TokenKind.Continue || kind == TokenKind.Return;
	}

	private boolean isBlockItemStart(TokenKind kind) {
		return isTypeToken(kind) || isStatementStart(kind);
	}

	private void recordDiagnostic(ParseException exception) {
		if (this.diagnostics.size() >= MAX_DIAGNOSTICS) {
			return;
		}
		this.diagnostics.add(new ParseDiagnostic(
				look.getLineNumber(), look.getColumnNumber(), exception.getMessage()));
	}

	private void synchronizeStatement(Token itemStart) {
		if (look == itemStart && look.getKind() != TokenKind.Rbrace && look.getKind() != TokenKind.EOF) {
			move();
		}
		while (look.getKind() != TokenKind.EOF) {
			if (look.getKind() == TokenKind.Semicolon) {
				move();
				return;
			}
			if (look.getKind() == TokenKind.Rbrace || isBlockItemStart(look.getKind())) {
				return;
			}
			move();
		}
	}

	private void synchronizeMethod() {
		int braceDepth = 0;
		boolean consumed = false;
		while (look.getKind() != TokenKind.EOF) {
			if (braceDepth == 0 && consumed && isMethodStart(look.getKind())) {
				return;
			}
			if (look.getKind() == TokenKind.Lbrace) {
				braceDepth++;
				move();
				consumed = true;
				continue;
			}
			if (look.getKind() == TokenKind.Rbrace) {
				if (braceDepth == 0) {
					return;
				}
				braceDepth--;
				move();
				consumed = true;
				if (braceDepth == 0) {
					return;
				}
				continue;
			}
			move();
			consumed = true;
		}
	}

	private Ast.Stmt.Base parseStmt() {
		Ast.Stmt.Base stmt = null;
		
		if( look.getKind() == TokenKind.Printf ){
			match(TokenKind.Printf);
			match(TokenKind.Lparen);
			if (look.getKind() != TokenKind.StringLiteral) {
				error("printf 的第一个参数必须是字符串格式");
			}
			String format = look.getLexeme();
			int lineNumber = look.getLineNumber();
			match(TokenKind.StringLiteral);
			ArrayList<Ast.Expr.Base> exprs = new ArrayList<Ast.Expr.Base>();
			while( look.getKind() == TokenKind.Comma ){
				match(TokenKind.Comma);
				exprs.add(parseExpr());
			}
			match(TokenKind.Rparen);
			match(TokenKind.Semicolon);
			stmt = new Ast.Stmt.Printf(format,exprs,lineNumber);
		}
		else if( look.getKind() == TokenKind.PrintLine ){
			int lineNumber = look.getLineNumber();
			match(TokenKind.PrintLine);
			match(TokenKind.Lparen);
			match(TokenKind.Rparen);
			match(TokenKind.Semicolon);
			stmt = new Ast.Stmt.PrintLine();
		}
		else if( look.getKind() == TokenKind.Break ){
			int lineNumber = look.getLineNumber();
			match(TokenKind.Break);
			match(TokenKind.Semicolon);
			stmt = new Ast.Stmt.Break(lineNumber);
		}
		else if( look.getKind() == TokenKind.Continue ){
			int lineNumber = look.getLineNumber();
			match(TokenKind.Continue);
			match(TokenKind.Semicolon);
			stmt = new Ast.Stmt.Continue(lineNumber);
		}
		else if( look.getKind() == TokenKind.While ){
			match(TokenKind.While);
			match(TokenKind.Lparen);
			int lineNumber = look.getLineNumber();
			Ast.Expr.Base condition = parseExpr();
			match(TokenKind.Rparen);
			Ast.Stmt.Base whileStmt = parseStmt();
			stmt = new Ast.Stmt.While(condition, whileStmt, lineNumber);
		}
		else if( look.getKind() == TokenKind.For ){
			int lineNumber = look.getLineNumber();
			match(TokenKind.For);
			match(TokenKind.Lparen);
			Ast.Stmt.Base init = null;
			if (look.getKind() != TokenKind.Semicolon) {
				init = parseSimpleStmtWithoutTerminator();
			}
			match(TokenKind.Semicolon);
			Ast.Expr.Base condition = look.getKind() == TokenKind.Semicolon
					? new Ast.Expr.True(lineNumber)
					: parseExpr();
			match(TokenKind.Semicolon);
			Ast.Stmt.Base update = null;
			if (look.getKind() != TokenKind.Rparen) {
				update = parseSimpleStmtWithoutTerminator();
			}
			match(TokenKind.Rparen);
			Ast.Stmt.Base body = parseStmt();
			stmt = new Ast.Stmt.For(init, condition, update, body, lineNumber);
		}
		else if ( look.getKind() == TokenKind.Id ) {
			stmt = parseSimpleStmtWithoutTerminator();
			match(TokenKind.Semicolon);
		}
		else if( look.getKind() == TokenKind.Lbrace ) {
			match(TokenKind.Lbrace);
			int lineNumber = look.getLineNumber();
			stmt = new Ast.Stmt.Block(parseBlockItems(), lineNumber);
			match(TokenKind.Rbrace);

		}
		else if( look.getKind() == TokenKind.Return ) {
			int lineNumber = look.getLineNumber();
			match(TokenKind.Return);
			Ast.Expr.Base expr = look.getKind() == TokenKind.Semicolon ? null : parseExpr();
			stmt = new Ast.Stmt.Return(expr, lineNumber);
			match(TokenKind.Semicolon);

		}else if( look.getKind() == TokenKind.If ){
			match(TokenKind.If);
			match(TokenKind.Lparen);
			int lineNumber = look.getLineNumber();
			Ast.Expr.Base condition = parseExpr();
			match(TokenKind.Rparen);
			Ast.Stmt.Base thenStmt = parseStmt();
			Ast.Stmt.Base elseStmt = null;
			if( look.getKind() == TokenKind.Else){

				match(TokenKind.Else);
				elseStmt = parseStmt();
			}

			stmt = new Ast.Stmt.If(condition, thenStmt, elseStmt, lineNumber);
		}
		else {
			throw syntaxError("期望合法语句，实际得到 '" + look.getLexeme() + "'");
		}
		return stmt;
	}

	private Ast.Stmt.Base parseSimpleStmtWithoutTerminator() {
		if (look.getKind() != TokenKind.Id) {
			error("期望赋值语句或方法调用");
		}
		Token ahead = lexer.lookahead(1);
		if( ahead.getKind() == TokenKind.Lparen ){
			String mthName = look.getLexeme();
			int lineNumber = look.getLineNumber();
			Ast.Expr.Base expr = parseMethodCall();
			if( expr instanceof Ast.Expr.Call){
				return new Ast.Stmt.Call(mthName,((Ast.Expr.Call)expr).getInputParams(),lineNumber);
			}
		}
		else if( ahead.getKind() == TokenKind.Lbracket ){
			String arrayName = look.getLexeme();
			int lineNum = look.getLineNumber();
			match(TokenKind.Id);
			match(TokenKind.Lbracket);
			Ast.Expr.Base index = parseExpr();
			match(TokenKind.Rbracket);
			match(TokenKind.Assign);
			Ast.Expr.Base expr = parseExpr();
			return new Ast.Stmt.ArrayAssign(arrayName, index, expr, lineNum);
		}
		else{
			String id = look.getLexeme();
			int lineNum = look.getLineNumber();
			match(TokenKind.Id);
			match(TokenKind.Assign);
			Ast.Expr.Base expr = parseExpr();
			return new Ast.Stmt.Assign(new Ast.Expr.Id(id,lineNum), expr, lineNum);
		}
		throw syntaxError("无法解析简单语句");
	}




	// Exp -> AndExp || AndExp
	//  -> AndExp
	private Ast.Expr.Base parseExpr() {
		Ast.Expr.Base expr = parseAndExpr();
		while( look.getKind() == TokenKind.Or ) {
			move();
			Ast.Expr.Base right = parseAndExpr();
			expr = new Ast.Expr.Or(expr, right, expr.getLineNum());
		}
		return expr;
	}



	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Ast.Expr.Base parseAndExpr() {
		Ast.Expr.Base expr = parseRelationExpr();
		while( look.getKind() == TokenKind.And) {
			move();
			Ast.Expr.Base right = parseRelationExpr();
			expr = new Ast.Expr.And(expr, right, expr.getLineNum());
		}
		return expr;
	}

	// <relation_expr> -> additive_expr |<additive_expr>(>|<|>=|<=|==|!=)<additive_expr>
	private Ast.Expr.Base parseRelationExpr() {
		Ast.Expr.Base expr = parseAdditiveExpr();
		while( look.getKind() == TokenKind.LT ||
				look.getKind() == TokenKind.GT ||
				look.getKind() == TokenKind.LTE ||
				look.getKind() == TokenKind.GTE ||
				look.getKind() == TokenKind.NEQ ||
				look.getKind() == TokenKind.EQ ) {
			String operator = look.getLexeme();
			int lineNumber = look.getLineNumber();
			move();
			Ast.Expr.Base right = parseAdditiveExpr();
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
	private Ast.Expr.Base parseAdditiveExpr() {
		Ast.Expr.Base expr = parseTerm();
		while(look.getKind()==TokenKind.Add
				||look.getKind()==TokenKind.Sub) {
			Token temp=look;
			move();
			Ast.Expr.Base otherExpr = parseTerm();
			if(temp.getKind()==TokenKind.Add) {
				expr = new Ast.Expr.Add(expr, otherExpr, temp.getLineNumber());
			}else {
				expr = new Ast.Expr.Sub(expr, otherExpr, temp.getLineNumber());
			}
		}
		return expr;
	}

	// <term> -> <factor> *|/ <factor>
	private Ast.Expr.Base parseTerm() {
		Ast.Expr.Base expr = parseFactor();
		while(look.getKind()==TokenKind.Mul
				||look.getKind()==TokenKind.Div
				||look.getKind()==TokenKind.Mod) {
			Token temp=look;
			move();
			Ast.Expr.Base otherExpr = parseFactor();
			if(temp.getKind() == TokenKind.Mul) {
				expr = new Ast.Expr.Mul(expr, otherExpr, temp.getLineNumber());
			}else if(temp.getKind() == TokenKind.Div) {
				expr = new Ast.Expr.Div(expr, otherExpr, temp.getLineNumber());
			}else {
				expr = new Ast.Expr.Mod(expr, otherExpr, temp.getLineNumber());
			}
		}
		return expr;
	}


	// <factor> -> (<expression>)
	//  		| Integer Literal
	//  		| id
	//          | not(<expression>)
	private Ast.Expr.Base parseFactor() {
		Ast.Expr.Base expr = null;
		if(look.getKind()==TokenKind.Lparen){
			move();
			expr = parseExpr();
			match(TokenKind.Rparen);
			return expr;
		}else if(look.getKind()==TokenKind.Sub){
			int lineNumber = look.getLineNumber();
			move();
			Ast.Expr.Base operand = parseFactor();
			return new Ast.Expr.UnaryMinus(operand, lineNumber);
		}else if(look.getKind()== Num){
			expr = new Ast.Expr.IntLiteral(look.getLexeme(), look.getLineNumber());
			move();
			return expr;
		}else if(look.getKind()==TokenKind.FloatLiteral){
			// 浮点数字面量默认为float类型（保持向后兼容）
			expr = new Ast.Expr.FloatLiteral(look.getLexeme(), look.getLineNumber());
			move();
			return expr;
		}else if(look.getKind()==TokenKind.DoubleLiteral){
			expr = new Ast.Expr.DoubleLiteral(look.getLexeme(), look.getLineNumber());
			move();
			return expr;
		}else if( look.getKind()==TokenKind.Id ){
			Token ahead = lexer.lookahead(1);
			if( ahead.getKind() == TokenKind.Lparen){
				expr = parseMethodCall();
			}
			// 数组访问: arr[i]
			else if( ahead.getKind() == TokenKind.Lbracket){
				String arrayName = look.getLexeme();
				int lineNum = look.getLineNumber();
				move(); // consume id
				match(TokenKind.Lbracket);
				Ast.Expr.Base index = parseExpr();
				match(TokenKind.Rbracket);
				expr = new Ast.Expr.ArrayAccess(arrayName, index, lineNum);
			}
			else if( ahead.getKind() == TokenKind.Dot){
				String arrayName = look.getLexeme();
				int lineNum = look.getLineNumber();
				move(); // consume id
				match(TokenKind.Dot);
				if (!"length".equals(look.getLexeme())) {
					error("数组属性只支持 length");
				}
				match(TokenKind.Id);
				expr = new Ast.Expr.ArrayLength(arrayName, lineNum);
			}
			else{
				expr = new Ast.Expr.Id(look.getLexeme(),look.getLineNumber());
				move();
			}
			return expr;
		}
		else if(look.getKind()==TokenKind.StringLiteral ){
			expr = new Ast.Expr.Str(look.getLexeme(), look.getLineNumber());
			move();
			return expr;
		}
		else if(look.getKind()==TokenKind.Not ){
			int lineNumber = look.getLineNumber();
			move();
			expr = new Ast.Expr.Not(parseFactor(), lineNumber);
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
			throw syntaxError(String.format(
				"语法错误，期望标识符、表达式、数字或字符串，实际得到 '%s'",
				look.getLexeme()));
		}
	}



	// methodCall->methodCall(Expr,Expr)
	private Ast.Expr.Base parseMethodCall() {
		Ast.Expr.Base expr;
		String methodName = look.getLexeme();
		int lineNumber = look.getLineNumber();
		move();
		match(TokenKind.Lparen);
		ArrayList<Ast.Expr.Base> args = new ArrayList<Ast.Expr.Base>();
		if( look.getKind() == TokenKind.Rparen){

		}else{
			Ast.Expr.Base e = parseExpr();
			args.add(e);
			while( look.getKind() == TokenKind.Comma){
				match(TokenKind.Comma);
				args.add(parseExpr());
			}
		}

		match(TokenKind.Rparen);
		expr = new Ast.Expr.Call(methodName, args, lineNumber);
		return expr;
	}
}
