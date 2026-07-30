package site.ilemon.parser;

import site.ilemon.ast.Ast;
import site.ilemon.lexer.IntegerLiterals;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;
import site.ilemon.exception.ParseException;
import site.ilemon.source.SourceSpan;
import java.util.ArrayList;

import static site.ilemon.lexer.TokenKind.Num;


/**
 * 递归下降语法分析器。
 *
 * <p>将 {@link Lexer} 产生的 Token 流按照 Lemon 语言的 BNF 文法进行分析，
 * 构建出抽象语法树（AST）。采用自顶向下的 LL(2) 分析策略，通过
 * {@code lookahead(1)} / {@code lookahead(2)} 处理文法歧义，并为节点保留
 * end-exclusive 起止源码位置。</p>
 *
 * <h3>核心文法规则</h3>
 * <pre>
 * program        ::= "class" id "{" method* "}"
 * method         ::= type id "(" params? ")" "{" blockItem* "}"
 * blockItem      ::= localDecl | stmt
 * stmt           ::= assign | if | while | block | return | printf | call
 * expr           ::= orExpr                                  -- 优先级从低到高
 * orExpr         ::= andExpr ("||" andExpr)*
 * andExpr        ::= equalityExpr ("&amp;&amp;" equalityExpr)*
 * equalityExpr   ::= relationalExpr (("=="|"!=") relationalExpr)*
 * relationalExpr ::= additiveExpr (("&lt;"|"&gt;"|"&lt;="|"&gt;=") additiveExpr)?  -- 非结合
 * </pre>
 *
 * <p>相等运算符的优先级低于关系运算符（同 C），因此 {@code a < b == c < d} 解析为
 * {@code (a<b) == (c<d)}。关系运算符本身是非结合的，{@code a < b < c} 在语法阶段即被拒绝；
 * 详见 {@code document/LemonC文法规则.md}。</p>
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
	private Token previous;

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
		previous = look;
		look = lexer.next();
	}


	private Token match(TokenKind kind) {
		if( kind == look.getKind() ){
			Token matched = look;
			move();
			return matched;
		}else{
			expected(kind.toString());
			return null;
		}
	}

	private <T extends Ast.Node> T located(T node, Token start) {
		node.setSourceSpan(SourceSpan.covering(
				start.getSourceSpan(), previous.getSourceSpan()));
		return node;
	}

	private <T extends Ast.Node> T located(T node, SourceSpan span) {
		node.setSourceSpan(span);
		return node;
	}

	private SourceSpan covering(Ast.Node first, Ast.Node second) {
		return SourceSpan.covering(first.getSourceSpan(), second.getSourceSpan());
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
		Ast.Program.Base programSingle = located(
				new Ast.Program.ProgramSingle(mainClass), mainClass.getSourceSpan());
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
		Token start = match(TokenKind.Class);
		String className = consumeName("类名", false).getLexeme();
		// 检查class名称是否一致
		if( !className.equals(lexer.getClassName()) ){
			this.error(String.format("类名 '%s' 与文件名 '%s' 不一致", className, lexer.getClassName()));
		}
		match(TokenKind.Lbrace);
		ArrayList<Ast.Method.Base> methods = parseMethodList();
		match(TokenKind.Rbrace);
		mainClass = located(new Ast.MainClass.MainClassSingle(className, methods), start);
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
		Token start = look;
		Ast.Type.Base t = parseType();
		Token methodNameToken = consumeName("方法名", true);
		String methodName = methodNameToken.getLexeme();
		int lineNumber = methodNameToken.getLineNumber();
		match(TokenKind.Lparen);
			ArrayList<Ast.Declare.Base> inputParams = parseInputParams();
		match(TokenKind.Rparen);
		match(TokenKind.Lbrace);
		ArrayList<Ast.Stmt.Base> stmts = parseBlockItems();
		match(TokenKind.Rbrace);
		Ast.Method.MethodSingle method;
		if( !methodName.equals("main")){
			Ast.Stmt.Base stmt = stmts.isEmpty() ? null : stmts.get(stmts.size()-1);
			method = new Ast.Method.MethodSingle(
					t, methodName, inputParams, stmts, stmt, lineNumber);
		}else{
			method = new Ast.Method.MethodSingle(
					t, methodName, inputParams, stmts, null, lineNumber);
		}
		return located(method, start);

	}

	// <localDecl> -> type id ("[" num "]")? ("=" expr)? ";"
	private Ast.Stmt.VarDecl parseLocalDeclaration() {
		Token start = look;
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
			Ast.Type.Base arrayType = createArrayType(type, size);
			if (arrayType == null) {
				error("不支持的数组基础类型");
			}
			type = located(arrayType, SourceSpan.covering(
					type.getSourceSpan(), previous.getSourceSpan()));
		}
		SourceSpan declarationSpan = SourceSpan.covering(
				start.getSourceSpan(), previous.getSourceSpan());

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
				located(new Ast.Declare.DeclareSingle(type, id, lineNumber), declarationSpan);
		return located(new Ast.Stmt.VarDecl(declaration, initializer), start);
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
		Token start = look;
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
			type = located(arrayType, SourceSpan.covering(
					type.getSourceSpan(), previous.getSourceSpan()));
		}
		return located(new Ast.Declare.DeclareSingle(type, id, lineNumber), start);
	}

	/**
	 * 判断当前 token 是否为类型关键字
	 */
	private boolean isTypeToken(TokenKind kind) {
		return kind == TokenKind.Int || kind == TokenKind.Float
				|| kind == TokenKind.Double || kind == TokenKind.Bool;
	}


	private Ast.Type.Base parseType() {
		Token token = look;
		Ast.Type.Base type;
		if( look.getKind() == TokenKind.Int ){
			move();
			type = new Ast.Type.Int();
		}
		else if(look.getKind() == TokenKind.Void){
			move();
			type = new Ast.Type.Void();
		}
		else if(look.getKind() == TokenKind.Float){
			move();
			type = new Ast.Type.Float();
		}
		else if(look.getKind() == TokenKind.Double){
			move();
			type = new Ast.Type.Double();
		}
		else if(look.getKind() == TokenKind.Bool){
			move();
			type = new Ast.Type.Bool();
		} else {
			throw syntaxError("期望类型关键字 int、float、double、bool 或 void");
		}
		return located(type, token.getSourceSpan());
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
		Token start = look;
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
			int lineNumber = look.getLineNumber();
			match(TokenKind.While);
			match(TokenKind.Lparen);
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
			Ast.Expr.Base condition;
			if (look.getKind() == TokenKind.Semicolon) {
				condition = located(new Ast.Expr.True(lineNumber),
						SourceSpan.point(look.getLineNumber(), look.getColumnNumber()));
			} else {
				condition = parseExpr();
			}
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
			stmt = new Ast.Stmt.Block(parseBlockItems(), start.getLineNumber());
			match(TokenKind.Rbrace);

		}
		else if( look.getKind() == TokenKind.Return ) {
			int lineNumber = look.getLineNumber();
			match(TokenKind.Return);
			Ast.Expr.Base expr = look.getKind() == TokenKind.Semicolon ? null : parseExpr();
			stmt = new Ast.Stmt.Return(expr, lineNumber);
			match(TokenKind.Semicolon);

		}else if( look.getKind() == TokenKind.If ){
			int lineNumber = look.getLineNumber();
			match(TokenKind.If);
			match(TokenKind.Lparen);
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
		return located(stmt, start);
	}

	// <exprStmt> -> <call>
	//            | <assignTarget> ("=" | "+=" | "-=" | "*=" | "/=" | "%=") <expr>
	//            | <assignTarget> ("++" | "--")
	// <assignTarget> -> id | id "[" <expr> "]"
	private Ast.Stmt.Base parseSimpleStmtWithoutTerminator() {
		Token start = look;
		if (look.getKind() != TokenKind.Id) {
			error("期望赋值语句或方法调用");
		}
		if (lexer.lookahead(1).getKind() == TokenKind.Lparen) {
			String methodName = look.getLexeme();
			int lineNumber = look.getLineNumber();
			Ast.Expr.Base expr = parseMethodCall();
			return located(new Ast.Stmt.Call(
					methodName, ((Ast.Expr.Call) expr).getInputParams(), lineNumber),
					expr.getSourceSpan());
		}

		AssignTarget target = parseAssignTarget();

		if (look.getKind() == TokenKind.Increment || look.getKind() == TokenKind.Decrement) {
			Token operator = look;
			boolean increment = operator.getKind() == TokenKind.Increment;
			move();
			requireReevaluableIndex(target, operator.getLexeme());
			int lineNumber = operator.getLineNumber();
			Ast.Expr.Base read = target.read();
			Ast.Expr.Base one = located(
					new Ast.Expr.IntLiteral("1", lineNumber), operator.getSourceSpan());
			Ast.Expr.Base value = increment
					? new Ast.Expr.Add(read, one, lineNumber)
					: (Ast.Expr.Base) new Ast.Expr.Sub(read, one, lineNumber);
			located(value, SourceSpan.covering(
					read.getSourceSpan(), operator.getSourceSpan()));
			return located(target.assign(value), start);
		}

		if (isCompoundAssignOperator(look.getKind())) {
			Token operator = look;
			move();
			requireReevaluableIndex(target, operator.getLexeme());
			Ast.Expr.Base rhs = parseExpr();
			Ast.Expr.Base read = target.read();
			Ast.Expr.Base value = located(
					compoundOperation(operator.getKind(), read, rhs, operator.getLineNumber()),
					SourceSpan.covering(read.getSourceSpan(), rhs.getSourceSpan()));
			return located(target.assign(value), start);
		}

		match(TokenKind.Assign);
		Ast.Expr.Base value = parseExpr();
		return located(target.assign(value), start);
	}

	private AssignTarget parseAssignTarget() {
		Token idToken = look;
		String name = idToken.getLexeme();
		int lineNumber = idToken.getLineNumber();
		match(TokenKind.Id);
		if (look.getKind() != TokenKind.Lbracket) {
			return new AssignTarget(name, lineNumber, idToken.getSourceSpan(), null);
		}
		match(TokenKind.Lbracket);
		Ast.Expr.Base index = parseExpr();
		match(TokenKind.Rbracket);
		return new AssignTarget(name, lineNumber,
				SourceSpan.covering(idToken.getSourceSpan(), previous.getSourceSpan()), index);
	}

	private boolean isCompoundAssignOperator(TokenKind kind) {
		return kind == TokenKind.AddAssign || kind == TokenKind.SubAssign
				|| kind == TokenKind.MulAssign || kind == TokenKind.DivAssign
				|| kind == TokenKind.ModAssign;
	}

	private Ast.Expr.Base compoundOperation(TokenKind operator, Ast.Expr.Base left,
											Ast.Expr.Base right, int lineNumber) {
		switch (operator) {
			case AddAssign:
				return new Ast.Expr.Add(left, right, lineNumber);
			case SubAssign:
				return new Ast.Expr.Sub(left, right, lineNumber);
			case MulAssign:
				return new Ast.Expr.Mul(left, right, lineNumber);
			case DivAssign:
				return new Ast.Expr.Div(left, right, lineNumber);
			default:
				return new Ast.Expr.Mod(left, right, lineNumber);
		}
	}

	/**
	 * 复合赋值和自增按 {@code a = a op b} 脱糖，所以数组下标会被求值两次。
	 * 只有能安全重复求值的下标才被接受；AST 层没有临时变量可用来只求值一次。
	 */
	private void requireReevaluableIndex(AssignTarget target, String operatorLexeme) {
		if (!target.isArrayElement()) {
			return;
		}
		Ast.Expr.Base index = target.index();
		if (index instanceof Ast.Expr.Id || index instanceof Ast.Expr.IntLiteral) {
			return;
		}
		throw syntaxError(String.format(
				"'%s' 作用于数组元素时，下标必须是变量或整数字面量（下标会被求值两次）；"
				+ "请改写为 a[i] = a[i] ... 的形式", operatorLexeme));
	}

	/** 赋值目标：普通变量或数组元素。 */
	private final class AssignTarget {
		private final String name;
		private final int lineNumber;
		private final SourceSpan span;
		private final Ast.Expr.Base index;

		AssignTarget(String name, int lineNumber, SourceSpan span, Ast.Expr.Base index) {
			this.name = name;
			this.lineNumber = lineNumber;
			this.span = span;
			this.index = index;
		}

		boolean isArrayElement() {
			return index != null;
		}

		Ast.Expr.Base index() {
			return index;
		}

		/**
		 * 构造一次对该目标的读取，供复合赋值与自增使用。
		 * 必须是新节点：AST 节点携带可变的 source span，而语义分析按节点身份建立
		 * 源节点到类型化节点的映射，共享同一个节点会让两者互相覆盖。
		 */
		Ast.Expr.Base read() {
			if (index == null) {
				return located(new Ast.Expr.Id(name, lineNumber), span);
			}
			return located(new Ast.Expr.ArrayAccess(name, copyIndex(), lineNumber), span);
		}

		Ast.Stmt.Base assign(Ast.Expr.Base value) {
			if (index == null) {
				Ast.Expr.Id id = located(new Ast.Expr.Id(name, lineNumber), span);
				return new Ast.Stmt.Assign(id, value, lineNumber);
			}
			return new Ast.Stmt.ArrayAssign(name, index, value, lineNumber);
		}

		/** 下标已被 {@link #requireReevaluableIndex} 限制为这两种形式。 */
		private Ast.Expr.Base copyIndex() {
			if (index instanceof Ast.Expr.Id) {
				return located(new Ast.Expr.Id(((Ast.Expr.Id) index).getId(), lineNumber),
						index.getSourceSpan());
			}
			return located(new Ast.Expr.IntLiteral(
					((Ast.Expr.IntLiteral) index).getRawValue(), lineNumber),
					index.getSourceSpan());
		}
	}




	// Exp -> AndExp || AndExp
	//  -> AndExp
	private Ast.Expr.Base parseExpr() {
		Ast.Expr.Base expr = parseAndExpr();
		while( look.getKind() == TokenKind.Or ) {
			move();
			Ast.Expr.Base right = parseAndExpr();
			expr = located(new Ast.Expr.Or(expr, right, expr.getLineNum()),
					covering(expr, right));
		}
		return expr;
	}



	// Exp -> AndExp && AndExp
	//  -> AndExp
	private Ast.Expr.Base parseAndExpr() {
		Ast.Expr.Base expr = parseEqualityExpr();
		while( look.getKind() == TokenKind.And) {
			move();
			Ast.Expr.Base right = parseEqualityExpr();
			expr = located(new Ast.Expr.And(expr, right, expr.getLineNum()),
					covering(expr, right));
		}
		return expr;
	}

	// <equalityExpr> -> <relationalExpr> (("=="|"!=") <relationalExpr>)*
	// 相等运算符优先级低于关系运算符（同 C），因此 a < b == c < d 解析为 (a<b) == (c<d)。
	private Ast.Expr.Base parseEqualityExpr() {
		Ast.Expr.Base expr = parseRelationalExpr();
		while( look.getKind() == TokenKind.EQ || look.getKind() == TokenKind.NEQ ) {
			boolean equal = look.getKind() == TokenKind.EQ;
			int lineNumber = look.getLineNumber();
			move();
			Ast.Expr.Base right = parseRelationalExpr();
			expr = located(equal
					? new Ast.Expr.EQ(expr, right, lineNumber)
					: (Ast.Expr.Base) new Ast.Expr.NEQ(expr, right, lineNumber),
					covering(expr, right));
		}
		return expr;
	}

	// <relationalExpr> -> <additiveExpr> [("<"|">"|"<="|">=") <additiveExpr>]
	// 关系运算符是非结合的。Lemon 的 bool 是独立类型且没有 int/bool 隐式转换，
	// 所以 a < b < c 永远无法通过类型检查；在这里直接拒绝比漂到语义层
	// 报"左侧为 bool"更准确。这是对 C 的有意偏离。
	private Ast.Expr.Base parseRelationalExpr() {
		Ast.Expr.Base expr = parseAdditiveExpr();
		if( !isRelationalOperator(look.getKind()) ) {
			return expr;
		}
		Token operator = look;
		move();
		Ast.Expr.Base right = parseAdditiveExpr();
		Ast.Expr.Base relational = located(
				newRelational(operator, expr, right), covering(expr, right));
		if( !isRelationalOperator(look.getKind()) ) {
			return relational;
		}

		ParseException error = syntaxError(String.format(
				"关系运算符不可连用；请改用括号或 '&&'，例如 (a %s b) && (b %s c)",
				operator.getLexeme(), look.getLexeme()));
		if (!this.collectingErrors) {
			throw error;
		}
		recordDiagnostic(error);
		// 这里已经准确知道用户写了什么，所以按左结合把链的剩余部分消费掉，
		// 让外层结构保持完整。交给恐慌模式同步只会在链尾产生连带错误。
		while (isRelationalOperator(look.getKind())) {
			Token next = look;
			move();
			Ast.Expr.Base operand = parseAdditiveExpr();
			relational = located(
					newRelational(next, relational, operand), covering(relational, operand));
		}
		return relational;
	}

	private boolean isRelationalOperator(TokenKind kind) {
		return kind == TokenKind.LT || kind == TokenKind.GT
				|| kind == TokenKind.LTE || kind == TokenKind.GTE;
	}

	private Ast.Expr.Base newRelational(Token operator, Ast.Expr.Base left, Ast.Expr.Base right) {
		int lineNumber = operator.getLineNumber();
		switch (operator.getKind()) {
			case LT:
				return new Ast.Expr.LT(left, right, lineNumber);
			case GT:
				return new Ast.Expr.GT(left, right, lineNumber);
			case LTE:
				return new Ast.Expr.LTE(left, right, lineNumber);
			default:
				return new Ast.Expr.GTE(left, right, lineNumber);
		}
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
				expr = located(new Ast.Expr.Add(expr, otherExpr, temp.getLineNumber()),
						covering(expr, otherExpr));
			}else {
				expr = located(new Ast.Expr.Sub(expr, otherExpr, temp.getLineNumber()),
						covering(expr, otherExpr));
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
				expr = located(new Ast.Expr.Mul(expr, otherExpr, temp.getLineNumber()),
						covering(expr, otherExpr));
			}else if(temp.getKind() == TokenKind.Div) {
				expr = located(new Ast.Expr.Div(expr, otherExpr, temp.getLineNumber()),
						covering(expr, otherExpr));
			}else {
				expr = located(new Ast.Expr.Mod(expr, otherExpr, temp.getLineNumber()),
						covering(expr, otherExpr));
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
			Token start = match(TokenKind.Lparen);
			expr = parseExpr();
			match(TokenKind.Rparen);
			return located(expr, start);
		}else if(look.getKind()==TokenKind.Sub){
			Token start = look;
			int lineNumber = look.getLineNumber();
			move();
			Ast.Expr.Base operand = parseFactor();
			return located(new Ast.Expr.UnaryMinus(operand, lineNumber),
					SourceSpan.covering(start.getSourceSpan(), operand.getSourceSpan()));
		}else if(look.getKind()== Num){
			Token token = look;
			expr = located(new Ast.Expr.IntLiteral(
					token.getLexeme(), token.getLineNumber()), token.getSourceSpan());
			move();
			return expr;
		}else if(look.getKind()==TokenKind.FloatLiteral){
			// 浮点数字面量默认为float类型（保持向后兼容）
			Token token = look;
			expr = located(new Ast.Expr.FloatLiteral(
					token.getLexeme(), token.getLineNumber()), token.getSourceSpan());
			move();
			return expr;
		}else if(look.getKind()==TokenKind.DoubleLiteral){
			Token token = look;
			expr = located(new Ast.Expr.DoubleLiteral(
					token.getLexeme(), token.getLineNumber()), token.getSourceSpan());
			move();
			return expr;
		}else if( look.getKind()==TokenKind.Id ){
			Token idToken = look;
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
				expr = located(new Ast.Expr.ArrayAccess(arrayName, index, lineNum), idToken);
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
				expr = located(new Ast.Expr.ArrayLength(arrayName, lineNum), idToken);
			}
			else{
				expr = located(new Ast.Expr.Id(
						look.getLexeme(), look.getLineNumber()), look.getSourceSpan());
				move();
			}
			return expr;
		}
		else if(look.getKind()==TokenKind.StringLiteral ){
			Token token = look;
			expr = located(new Ast.Expr.Str(
					token.getLexeme(), token.getLineNumber()), token.getSourceSpan());
			move();
			return expr;
		}
		else if(look.getKind()==TokenKind.Not ){
			Token start = look;
			int lineNumber = look.getLineNumber();
			move();
			Ast.Expr.Base operand = parseFactor();
			expr = located(new Ast.Expr.Not(operand, lineNumber),
					SourceSpan.covering(start.getSourceSpan(), operand.getSourceSpan()));
			return expr;
		}
		else if(look.getKind()==TokenKind.True ){
			Token token = look;
			expr = located(new Ast.Expr.True(token.getLineNumber()), token.getSourceSpan());
			move();
			return expr;
		}
		else if(look.getKind()==TokenKind.False ){
			Token token = look;
			expr = located(new Ast.Expr.False(token.getLineNumber()), token.getSourceSpan());
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
		Token start = look;
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
		expr = located(new Ast.Expr.Call(methodName, args, lineNumber), start);
		return expr;
	}
}
