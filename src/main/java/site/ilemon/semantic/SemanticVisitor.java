package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.ast.Ast.Type.TypeKind;
import site.ilemon.exception.SemanticException;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;


/**
 * 语义分析 Visitor。
 *
 * <p>基于 Visitor 模式遍历 AST，执行以下静态语义检查：</p>
 * <ul>
 *   <li><b>类型检查</b>：赋值、运算、方法调用的类型一致性</li>
 *   <li><b>变量检查</b>：未声明变量、使用前未赋值</li>
 *   <li><b>方法检查</b>：未定义方法、重复定义、参数个数/类型匹配</li>
 *   <li><b>控制流检查</b>：if/while 条件必须是 bool 类型</li>
 *   <li><b>返回值检查</b>：返回类型与方法声明一致、main 方法必须是 void</li>
 * </ul>
 *
 * <p>发现错误时抛出 {@link SemanticException}，错误信息格式为
 * {@code [语义分析] 行 N: 错误描述}。</p>
 *
 * @author andy
 * @see site.ilemon.visitor.ISemanticVisitor
 * @see SemanticException
 */
public class SemanticVisitor implements ISemanticVisitor {

    private boolean pass = true;

    private final boolean collectErrors;

    private final ArrayList<String> errors = new ArrayList<String>();

    private final ArrayList<Integer> errorLineNumbers = new ArrayList<Integer>();

    private java.util.Stack<Ast.Type.Base> typeStack = new java.util.Stack<>();

    private String currMethodName;

    private HashMap<String,MethodVarTable> methodVarTable;

    private HashMap<String,Ast.Type.Base> methodNameRetTypeMap;

    private HashSet<String> currMethodLocalVar;

    private Deque<HashSet<String>> variableScopes;

    private int loopDepth = 0;

    private HashMap<String,Ast.Method.MethodSingle> methodMap;

    private Ast.Type.Base typeOfMethodDeclared;

    public SemanticVisitor(){
        this(false);
    }

    private SemanticVisitor(boolean collectErrors){

        this.collectErrors = collectErrors;
        this.methodVarTable = new HashMap<String,MethodVarTable>();
        this.methodMap = new HashMap<String, Ast.Method.MethodSingle>();
        this.methodNameRetTypeMap = new HashMap<String, Ast.Type.Base>();
    }

    public static SemanticVisitor collecting() {
        return new SemanticVisitor(true);
    }

    public ArrayList<String> getErrors() {
        return new ArrayList<String>(this.errors);
    }

    public ArrayList<Integer> getErrorLineNumbers() {
        return new ArrayList<Integer>(this.errorLineNumbers);
    }

    public boolean passOrNot(){
        return pass;
    }

    private Ast.Type.Base errorType() {
        return Ast.Type.Error.INSTANCE;
    }

    private boolean isErrorType(Ast.Type.Base type) {
        return type != null && type.getKind() == TypeKind.ERROR;
    }

    private Ast.Type.Base analyzeExpression(Ast.Expr.Base expression) {
        if (expression == null) {
            return errorType();
        }
        int initialDepth = this.typeStack.size();
        expression.accept(this);
        if (this.typeStack.size() <= initialDepth) {
            error(expression.getLineNum(), "internal error: expression analysis produced no type");
            return errorType();
        }
        Ast.Type.Base result = this.typeStack.pop();
        while (this.typeStack.size() > initialDepth) {
            this.typeStack.pop();
        }
        return result == null ? errorType() : result;
    }

    private void pushType(Ast.Type.Base type) {
        this.typeStack.push(type == null ? errorType() : type);
    }

    private String typeName(Ast.Type.Base type) {
        if (type == null) {
            return "未知";
        }
        String name = type.toString();
        return name.startsWith("@") ? name.substring(1) : name;
    }

    private void checkSameOperandTypes(int lineNum, String operator, Ast.Type.Base leftType, Ast.Type.Base rightType) {
        if (isErrorType(leftType) || isErrorType(rightType)) {
            pushType(errorType());
            return;
        }
        if (isArrayType(leftType) || isArrayType(rightType)) {
            error(lineNum, String.format(
                    "运算符 '%s' 不支持数组操作数：左侧为 %s，右侧为 %s",
                    operator, typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        Ast.Type.Base promoted = promoteNumeric(leftType, rightType);
        if (promoted != null) {
            pushType(promoted);
            return;
        }
        if (!isMatch(leftType, rightType)) {
            error(lineNum, String.format(
                    "运算符 '%s' 的左右操作数类型不匹配：左侧为 %s，右侧为 %s",
                    operator, typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        pushType(leftType);
    }

    private void checkBooleanOperandTypes(int lineNum, String operator, Ast.Type.Base leftType, Ast.Type.Base rightType) {
        if (isErrorType(leftType) || isErrorType(rightType)) {
            pushType(errorType());
            return;
        }
        if (leftType.getKind() != TypeKind.BOOL || rightType.getKind() != TypeKind.BOOL) {
            error(lineNum, String.format(
                    "运算符 '%s' 要求左右操作数都是 bool：左侧为 %s，右侧为 %s",
                    operator, typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        pushType(new Ast.Type.Bool());
    }

    @Override
    public void visit(Ast.Expr.Add obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "+", leftType, rightType);
    }

    @Override
    public void visit(Ast.Expr.And obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        checkBooleanOperandTypes(obj.getLineNum(), "&&", leftType, rightType);
    }

    @Override
    public void visit(Ast.Type.Bool obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {
        if(obj.getExpr() instanceof Ast.Expr.Base){
            Ast.Type.Base exprType = analyzeExpression(obj.getExpr());
            boolean wasUnassigned = this.currMethodLocalVar.contains(obj.getId().getId());
            if (wasUnassigned)
                this.currMethodLocalVar.remove(obj.getId().getId());
            Ast.Type.Base destType = analyzeExpression(obj.getId());
            if (isErrorType(destType) || isErrorType(exprType)) {
                if (wasUnassigned) {
                    this.currMethodLocalVar.add(obj.getId().getId());
                }
                return;
            }
            if (isArrayType(destType) || isArrayType(exprType)) {
                error(obj.getLineNum(), String.format("数组不支持整体赋值：不能将 %s 赋值给 %s",
                        typeName(exprType), typeName(destType)));
                if (wasUnassigned) {
                    this.currMethodLocalVar.add(obj.getId().getId());
                }
                return;
            }
            if (!isMatch(destType, exprType) && wasUnassigned) {
                this.currMethodLocalVar.add(obj.getId().getId());
            }
            if( !isMatch(destType,exprType))
                error(obj.getLineNum(),String.format("不能将 %s 类型的表达式赋值给 %s 类型的变量 '%s'",
                        typeName(exprType), typeName(destType), obj.getId().getId()));
        }

    }

    @Override
    public void visit(Ast.Stmt.VarDecl obj) {
        Ast.Declare.DeclareSingle declaration = obj.getDeclaration();
        if (this.variableScopes == null || this.variableScopes.isEmpty()) {
            error(obj.getLineNum(), "内部错误: 局部变量作用域未初始化");
            return;
        }
        this.variableScopes.peek().add(declaration.getId());
        if (!isArrayType(declaration.getType())) {
            this.currMethodLocalVar.add(declaration.getId());
        }
        if (obj.getInitializer() == null) {
            return;
        }

        Ast.Type.Base initializerType = analyzeExpression(obj.getInitializer());
        if (isErrorType(initializerType)) {
            return;
        }
        if (!isMatch(declaration.getType(), initializerType)) {
            error(obj.getLineNum(), String.format(
                    "不能用 %s 类型初始化 %s 类型的变量 '%s'",
                    typeName(initializerType), typeName(declaration.getType()), declaration.getId()));
            return;
        }
        this.currMethodLocalVar.remove(declaration.getId());
    }

    @Override
    public void visit(Ast.Stmt.Block obj) {
        enterVariableScope();
        for( Ast.Stmt.Base stmt : obj.getStmts()){
            this.visit(stmt);
        }
        exitVariableScope();
    }

    @Override
    public void visit(Ast.Expr.Call obj) {
        Ast.Type.Base returnType = validateMethodCall(obj.getName(), obj.getInputParams(), obj.getLineNum());
        if (!isErrorType(returnType) && returnType.getKind() == TypeKind.VOID) {
            error(obj.getLineNum(), "void 方法 '" + obj.getName() + "' 不能作为表达式使用");
            returnType = errorType();
        }
        obj.setReturnType(returnType);
        pushType(returnType);
    }

    @Override
    public void visit(Ast.Declare.Base obj) {
        if (obj instanceof Ast.Declare.DeclareSingle) {
            pushType(((Ast.Declare.DeclareSingle) obj).getType());
        }
    }

    @Override
    public void visit(Ast.Expr.Div obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "/", leftType, rightType);
    }

    @Override
    public void visit(Ast.Expr.Mod obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        if (isErrorType(leftType) || isErrorType(rightType)) {
            pushType(errorType());
            return;
        }
        if (leftType.getKind() != TypeKind.INT
                || rightType.getKind() != TypeKind.INT) {
            error(obj.getLineNum(), String.format(
                    "运算符 '%%' 只支持 int 操作数：左侧为 %s，右侧为 %s",
                    typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        pushType(new Ast.Type.Int());
    }

    @Override
    public void visit(Ast.Type.Float obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Type.Double obj) {
        pushType(obj);
    }@Override
    public void visit(Ast.Expr.GT obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), ">", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.Id obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            error(obj.getLineNum(), "内部错误: 方法 '" + currMethodName + "' 的变量表未找到");
            obj.setType(errorType());
            pushType(errorType());
            return;
        }
        boolean visible = isVariableVisible(obj.getId());
        Ast.Type.Base declaredType = visible ? mTable.get(obj.getId()) : null;
        if (declaredType == null) {
            error(obj.getLineNum(), "未定义的变量: " + obj.getId());
            obj.setType(errorType());
            pushType(errorType());
            return;
        }
        if (currMethodLocalVar.contains(obj.getId())) {
            error(obj.getLineNum(), String.format("变量 '%s' 在使用前未赋值", obj.getId()));
            obj.setType(errorType());
            pushType(errorType());
            return;
        }
        obj.setType(declaredType);
        pushType(obj.getType());
    }

    @Override
    public void visit(Ast.Stmt.If obj) {
        Ast.Type.Base condType = analyzeExpression(obj.getCondition());
        if (!isErrorType(condType) && condType.getKind() != TypeKind.BOOL)
            error(obj.getCondition().getLineNum(),
                    "if 条件必须是 bool，实际为 " + typeName(condType));

        HashSet<String> before = new HashSet<String>(this.currMethodLocalVar);
        this.currMethodLocalVar = new HashSet<String>(before);
        this.visit(obj.getThenStmt());
        HashSet<String> thenUnassigned = new HashSet<String>(this.currMethodLocalVar);

        if (obj.getElseStmt() != null) {
            this.currMethodLocalVar = new HashSet<String>(before);
            this.visit(obj.getElseStmt());
            HashSet<String> elseUnassigned = new HashSet<String>(this.currMethodLocalVar);
            thenUnassigned.addAll(elseUnassigned);
            this.currMethodLocalVar = thenUnassigned;
        } else {
            this.currMethodLocalVar = before;
        }
    }

    @Override
    public void visit(Ast.Type.Int obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Program.Base programSingle) {
        this.visit(((Ast.Program.ProgramSingle)programSingle).getMainClass());
    }

    @Override
    public void visit(Ast.Expr.LT obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), "<", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.LTE obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), "<=", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.GTE obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), ">=", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.EQ obj) {
        checkComparison(obj.getLeft(), obj.getRight(), "==", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.NEQ obj) {
        checkComparison(obj.getLeft(), obj.getRight(), "!=", obj.getLineNum());
    }

    @Override
    public void visit(Ast.MainClass.Base obj) {
        Ast.MainClass.MainClassSingle mainClassSingle = (Ast.MainClass.MainClassSingle) obj;
        for(int i = 0; i < mainClassSingle.getMethods().size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.getMethods().get(i);
            if( methodMap.containsKey(method.getId())){
                error(method.getLineNum(), "重复定义的方法: " + method.getId());
            }else{
                methodMap.put(method.getId(),method);
                methodNameRetTypeMap.put(method.getId(),method.getRetType());
            }
        }
        validateMainMethod();
        for(int i = 0; i < mainClassSingle.getMethods().size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.getMethods().get(i);
            this.visit(method);
        }
    }

    private void validateMainMethod() {
        Ast.Method.MethodSingle main = this.methodMap.get("main");
        if (main == null) {
            error(1, "程序必须定义 void main()");
        }
        if (main == null) {
            return;
        }
        if (main.getRetType().getKind() != TypeKind.VOID) {
            error(main.getLineNum(), "main 方法的返回类型必须是 void，实际声明为: "
                    + typeName(main.getRetType()));
        }
        if (main.getFormals() != null && !main.getFormals().isEmpty()) {
            error(main.getLineNum(), "main 方法不能声明参数，必须是 void main()");
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle obj) {
        this.typeStack.clear();
        MethodVarTable mTable = new MethodVarTable();
        this.currMethodLocalVar = new HashSet<String>();
        mTable.put(obj.getFormals(),obj.getLocals());
        this.methodVarTable.put(obj.getId(),mTable);
        this.currMethodName = obj.getId();
        this.typeOfMethodDeclared = obj.getRetType();
		this.variableScopes = new ArrayDeque<HashSet<String>>();
		enterVariableScope();
		for (Ast.Declare.Base formal : obj.getFormals()) {
			this.variableScopes.peek().add(((Ast.Declare.DeclareSingle) formal).getId());
		}

        for( int i = 0; i < obj.getStms().size(); i++){
            Ast.Stmt.Base stmt = obj.getStms().get(i);
            this.visit(stmt);
        }
        if( !obj.getId().equals("main")
                && obj.getRetType().getKind() != TypeKind.VOID
                && !statementsMustReturn(obj.getStms()) ){
            error(obj.getLineNum(), "非 void 方法 '" + obj.getId() + "' 不是所有路径都有 return");
        }
		if (!this.typeStack.isEmpty()) {
			error(obj.getLineNum(), "internal error: semantic type stack is not balanced");
			this.typeStack.clear();
		}
		exitVariableScope();
		this.variableScopes = null;
    }

    @Override
    public void visit(Ast.Expr.Mul obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "*", leftType, rightType);


    }

    @Override
    public void visit(Ast.Expr.IntLiteral obj) {
        pushType(new Ast.Type.Int());
    }

    @Override
    public void visit(Ast.Expr.FloatLiteral obj) {
        pushType(new Ast.Type.Float());
    }

    @Override
    public void visit(Ast.Expr.DoubleLiteral obj) {
        pushType(new Ast.Type.Double());
    }

    @Override
    public void visit(Ast.Expr.Or obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        checkBooleanOperandTypes(obj.getLineNum(), "||", leftType, rightType);
    }



    @Override
    public void visit(Ast.Type.Str obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Expr.Sub obj) {
        Ast.Type.Base leftType = analyzeExpression(obj.getLeft());
        Ast.Type.Base rightType = analyzeExpression(obj.getRight());
        checkSameOperandTypes(obj.getLineNum(), "-", leftType, rightType);
    }@Override
    public void visit(Ast.Type.Void obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Type.Error obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Stmt.Base obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {
        ArrayList<Character> placeholders = printfPlaceholders(obj.getFormat(), obj.getLineNum());
        int argCount = obj.getExprs() == null ? 0 : obj.getExprs().size();
        if (placeholders.size() != argCount) {
            error(obj.getLineNum(), String.format(
                    "printf 参数个数不匹配：格式串需要 %d 个，实际 %d 个",
                    placeholders.size(), argCount));
        }
        for (int i = 0; i < argCount; i++) {
            Ast.Expr.Base expr = obj.getExprs().get(i);
            Ast.Type.Base argType = analyzeExpression(expr);
            if (isErrorType(argType) || i >= placeholders.size()) {
                continue;
            }
            char placeholder = placeholders.get(i);
            if (placeholder == 'd' && argType.getKind() != TypeKind.INT) {
                error(expr.getLineNum(), String.format(
                        "printf 占位符 %%d 需要 int，实际为 %s", typeName(argType)));
            }
            if (placeholder == 'f'
                    && argType.getKind() != TypeKind.FLOAT
                    && argType.getKind() != TypeKind.DOUBLE) {
                error(expr.getLineNum(), String.format(
                        "printf 占位符 %%f 需要 float 或 double，实际为 %s", typeName(argType)));
            }
        }
    }

    @Override
    public void visit(Ast.Stmt.PrintLine obj) {

    }

    @Override
    public void visit(Ast.Expr.Base obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Ast.Expr.True obj) {
        pushType(new Ast.Type.Bool());
    }

    @Override
    public void visit(Ast.Expr.False obj) {
        pushType(new Ast.Type.Bool());
    }

    @Override
    public void visit(Ast.Expr.UnaryMinus obj) {
        Ast.Type.Base type = analyzeExpression(obj.getExpr());
        if (isErrorType(type)) {
            pushType(errorType());
            return;
        }
        if (type.getKind() != TypeKind.INT && type.getKind() != TypeKind.FLOAT && type.getKind() != TypeKind.DOUBLE) {
            error(obj.getLineNum(), "一元负号不能用于类型 " + typeName(type));
        }
        pushType(isNumberType(type) ? type : errorType());
    }

    @Override
    public void visit(Ast.Expr.Not obj) {
        Ast.Type.Base condType = analyzeExpression(obj.getExpr());
        if (isErrorType(condType)) {
            pushType(errorType());
            return;
        }
        if( condType.getKind() != TypeKind.BOOL)
            error(obj.getLineNum(),"! 运算符要求操作数是 bool，实际为 " + typeName(condType));
        pushType(condType.getKind() == TypeKind.BOOL ? new Ast.Type.Bool() : errorType());
    }

    @Override
    public void visit(Ast.Expr.Str obj) {
        pushType(new Ast.Type.Str());
    }

    @Override
    public void visit(Ast.Type.Base obj) {

    }

    @Override
    public void visit(Ast.Stmt.Return obj) {
        boolean voidMethod = this.typeOfMethodDeclared != null
                && this.typeOfMethodDeclared.getKind() == TypeKind.VOID;
        if (obj.getExpr() == null) {
            if (!voidMethod) {
                error(obj.getLineNum(), "非 void 方法必须返回一个值");
            }
            return;
        }
        Ast.Type.Base retType = analyzeExpression(obj.getExpr());
        if (voidMethod) {
            error(obj.getLineNum(), "void 方法不能返回值");
            return;
        }
        if(!isErrorType(retType) && !isMatch(typeOfMethodDeclared,retType))
            error(obj.getLineNum(),String.format("返回值类型不匹配：期望 %s，实际 %s",
                    typeName(typeOfMethodDeclared), typeName(retType)));
    }


    @Override
    public void visit(Ast.Stmt.While obj) {
        Ast.Type.Base condType = analyzeExpression(obj.getCondition());
        if(!isErrorType(condType) && condType.getKind() != TypeKind.BOOL )
            error(obj.getCondition().getLineNum(), "while 条件必须是 bool，实际为 " + typeName(condType));
        HashSet<String> before = new HashSet<String>(this.currMethodLocalVar);
        loopDepth++;
        this.currMethodLocalVar = new HashSet<String>(before);
        this.visit(obj.getBody());
        loopDepth--;
        this.currMethodLocalVar = before;
    }

    @Override
    public void visit(Ast.Stmt.For obj) {
        if (obj.getInit() != null) {
            this.visit(obj.getInit());
        }
        Ast.Type.Base condType = analyzeExpression(obj.getCondition());
        if(!isErrorType(condType) && condType.getKind() != TypeKind.BOOL )
            error(obj.getCondition().getLineNum(), "for 条件必须是 bool，实际为 " + typeName(condType));
        HashSet<String> before = new HashSet<String>(this.currMethodLocalVar);
        loopDepth++;
        this.currMethodLocalVar = new HashSet<String>(before);
        this.visit(obj.getBody());
        if (obj.getUpdate() != null) {
            this.visit(obj.getUpdate());
        }
        loopDepth--;
        this.currMethodLocalVar = before;
    }

    @Override
    public void visit(Ast.Stmt.Break obj) {
        if (loopDepth <= 0)
            error(obj.getLineNum(), "break语句必须包含在循环体中。");
    }

    @Override
    public void visit(Ast.Stmt.Continue obj) {
        if (loopDepth <= 0)
            error(obj.getLineNum(), "continue语句必须包含在循环体中。");
    }

    @Override
    public void visit(Ast.Stmt.Call obj) {
        Ast.Type.Base returnType = validateMethodCall(obj.getName(), obj.getInputParams(), obj.getLineNum());
        obj.setReturnType(returnType);
    }

    private static class FlowResult {
        final boolean canCompleteNormally;
        final boolean mustReturn;

        FlowResult(boolean canCompleteNormally, boolean mustReturn) {
            this.canCompleteNormally = canCompleteNormally;
            this.mustReturn = mustReturn;
        }
    }

    private boolean statementsMustReturn(ArrayList<Ast.Stmt.Base> statements) {
        return flowOfStatements(statements).mustReturn;
    }

    private FlowResult flowOfStatements(ArrayList<Ast.Stmt.Base> statements) {
        if (statements == null || statements.isEmpty()) {
            return new FlowResult(true, false);
        }
        boolean canCompleteNormally = true;
        for (Ast.Stmt.Base statement : statements) {
            if (!canCompleteNormally) {
                break;
            }
            FlowResult result = flowOfStatement(statement);
            canCompleteNormally = result.canCompleteNormally;
            if (result.mustReturn) {
                return new FlowResult(false, true);
            }
        }
        return new FlowResult(canCompleteNormally, false);
    }

    private FlowResult flowOfStatement(Ast.Stmt.Base statement) {
        if (statement instanceof Ast.Stmt.Return) {
            return new FlowResult(false, true);
        }
        if (statement instanceof Ast.Stmt.Block) {
            return flowOfStatements(((Ast.Stmt.Block) statement).getStmts());
        }
        if (statement instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) statement;
            if (ifStmt.getElseStmt() == null) {
                return new FlowResult(true, false);
            }
            FlowResult thenFlow = flowOfStatement(ifStmt.getThenStmt());
            FlowResult elseFlow = flowOfStatement(ifStmt.getElseStmt());
            return new FlowResult(
                    thenFlow.canCompleteNormally || elseFlow.canCompleteNormally,
                    thenFlow.mustReturn && elseFlow.mustReturn);
        }
        return new FlowResult(true, false);
    }

    private ArrayList<Character> printfPlaceholders(String format, int lineNum) {
        ArrayList<Character> placeholders = new ArrayList<Character>();
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) != '%') {
                continue;
            }
            if (i + 1 >= format.length()) {
                error(lineNum, "printf 格式串中的 % 缺少占位符");
                break;
            }
            char placeholder = format.charAt(++i);
            if (placeholder == 'd' || placeholder == 'f') {
                placeholders.add(placeholder);
            } else {
                error(lineNum, "printf 不支持占位符 %" + placeholder);
            }
        }
        return placeholders;
    }

    private void error(int lineNum, String msg){
        this.pass = false;
        if (this.collectErrors) {
            this.errors.add("[语义分析] 行 " + lineNum + ": " + msg);
            this.errorLineNumbers.add(lineNum);
            return;
        }
        throw new SemanticException("[语义分析] 行 " + lineNum + ": " + msg);
    }

    private void enterVariableScope() {
        this.variableScopes.push(new HashSet<String>());
    }

    private void exitVariableScope() {
        HashSet<String> declarations = this.variableScopes.pop();
        this.currMethodLocalVar.removeAll(declarations);
    }

    private boolean isVariableVisible(String name) {
        if (this.variableScopes == null) {
            return false;
        }
        for (HashSet<String> scope : this.variableScopes) {
            if (scope.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMatch(Ast.Type.Base target,Ast.Type.Base curr){
        if (isErrorType(target) || isErrorType(curr))
            return true;
        if( target == null || curr == null )
            return false;
        if(target.getKind() == curr.getKind())
            return true;
        // 允许float隐式转换为double
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.FLOAT)
            return true;
        if(target.getKind() == TypeKind.FLOAT && curr.getKind() == TypeKind.INT)
            return true;
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.INT)
            return true;
        return false;
    }

    private Ast.Type.Base promoteNumeric(Ast.Type.Base left, Ast.Type.Base right) {
        if (!isNumberType(left) || !isNumberType(right)) {
            return null;
        }
        if (left.getKind() == TypeKind.DOUBLE || right.getKind() == TypeKind.DOUBLE) {
            return new Ast.Type.Double();
        }
        if (left.getKind() == TypeKind.FLOAT || right.getKind() == TypeKind.FLOAT) {
            return new Ast.Type.Float();
        }
        return new Ast.Type.Int();
    }

    private boolean isNumberType(Ast.Type.Base type) {
        if (type == null) {
            return false;
        }
        TypeKind kind = type.getKind();
        return kind == TypeKind.INT || kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE;
    }

    private boolean isArrayType(Ast.Type.Base type) {
        if (type == null) {
            return false;
        }
        TypeKind kind = type.getKind();
        return kind == TypeKind.INT_ARRAY || kind == TypeKind.FLOAT_ARRAY
                || kind == TypeKind.DOUBLE_ARRAY || kind == TypeKind.BOOL_ARRAY;
    }

    /**
     * 校验比较运算符（== / !=）：只要左右类型匹配即可
     */
    private void checkComparison(Ast.Expr.Base left, Ast.Expr.Base right, String op, int lineNum) {
        Ast.Type.Base leftType = analyzeExpression(left);
        Ast.Type.Base rightType = analyzeExpression(right);
        if (isErrorType(leftType) || isErrorType(rightType)) {
            pushType(errorType());
            return;
        }
        if (isArrayType(leftType) || isArrayType(rightType)) {
            error(lineNum, String.format("比较运算符 '%s' 不支持数组操作数：左侧为 %s，右侧为 %s",
                    op, typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        if (promoteNumeric(leftType, rightType) == null && !isMatch(leftType, rightType)) {
            error(lineNum, String.format("比较运算符 '%s' 的左右操作数类型不匹配：左侧为 %s，右侧为 %s",
                    op, typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        pushType(new Ast.Type.Bool());
    }

    /**
     * 校验序比较运算符（> / < / >= / <=）：除了类型匹配，还要求是数值类型
     */
    private void checkOrderComparison(Ast.Expr.Base left, Ast.Expr.Base right, String op, int lineNum) {
        Ast.Type.Base leftType = analyzeExpression(left);
        Ast.Type.Base rightType = analyzeExpression(right);
        if (isErrorType(leftType) || isErrorType(rightType)) {
            pushType(errorType());
            return;
        }
        if (promoteNumeric(leftType, rightType) == null) {
            error(lineNum, String.format("比较运算符 '%s' 只支持同类型数值操作数：左侧为 %s，右侧为 %s",
                    op, typeName(leftType), typeName(rightType)));
            pushType(errorType());
            return;
        }
        pushType(new Ast.Type.Bool());
    }

    /**
     * 公共方法调用校验逻辑，被 Expr.Call 和 Stmt.Call 共用。
     * 校验方法是否存在、参数个数是否匹配、参数类型是否匹配。
     * @return 方法的返回类型
     */
    private Ast.Type.Base validateMethodCall(String methodName, ArrayList<Ast.Expr.Base> inputParams, int lineNum) {
        ArrayList<Ast.Type.Base> actualTypes = new ArrayList<Ast.Type.Base>();
        if (inputParams != null) {
            for (Ast.Expr.Base inputParam : inputParams) {
                actualTypes.add(analyzeExpression(inputParam));
            }
        }
        Ast.Method.MethodSingle method = this.methodMap.get(methodName);
        if (method == null) {
            error(lineNum, "未定义的方法: " + methodName);
            return errorType();
        }
        boolean callHasError = false;
        if (actualTypes.size() != method.getFormals().size()) {
            callHasError = true;
            error(lineNum, String.format("方法 '%s' 的参数个数不正确：期望 %d 个，实际 %d 个",
                    methodName, method.getFormals().size(), actualTypes.size()));
        }
        int checkedArgCount = Math.min(actualTypes.size(), method.getFormals().size());
        for (int i = 0; i < checkedArgCount; i++) {
            Ast.Type.Base actualType = actualTypes.get(i);
            Ast.Type.Base expectedType = ((Ast.Declare.DeclareSingle) method.getFormals().get(i)).getType();
            if (isErrorType(actualType)) {
                callHasError = true;
                continue;
            }
            if (!isMatch(expectedType, actualType)) {
                callHasError = true;
                error(lineNum, String.format("方法 '%s' 的第 %d 个参数类型不匹配：期望 %s，实际 %s",
                        methodName, i + 1, typeName(expectedType), typeName(actualType)));
            }
        }
        if (callHasError) {
            return errorType();
        }
        Ast.Type.Base returnType = this.methodNameRetTypeMap.get(methodName);
        return returnType == null ? errorType() : returnType;
    }

    // ========== 数组相关的 visit 方法 ==========

    @Override
    public void visit(Ast.Type.IntArray obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Type.FloatArray obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Type.DoubleArray obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Type.BoolArray obj) {
        pushType(obj);
    }

    @Override
    public void visit(Ast.Expr.ArrayAccess obj) {
        Ast.Type.Base idxType = analyzeExpression(obj.getIndex());
        // 检查数组是否已声明
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            error(obj.getLineNum(), "内部错误: 方法 '" + currMethodName + "' 的变量表未找到");
        }
        if (mTable == null) {
            obj.setElementType(errorType());
            pushType(errorType());
            return;
        }
        Ast.Type.Base arrayType = isVariableVisible(obj.getArrayName())
                ? mTable.get(obj.getArrayName()) : null;
        if (arrayType == null) {
            error(obj.getLineNum(), "未定义的数组: " + obj.getArrayName());
        }
        if (arrayType == null) {
            obj.setElementType(errorType());
            pushType(errorType());
            return;
        }
        Ast.Type.Base elementType = getElementType(arrayType);
        if (elementType == null) {
            error(obj.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    obj.getArrayName(), typeName(arrayType)));
        }
        // 检查下标类型必须是int
        if (elementType == null) {
            obj.setElementType(errorType());
            pushType(errorType());
            return;
        }
        if (isErrorType(idxType)) {
            obj.setElementType(errorType());
            pushType(errorType());
            return;
        }
        if (idxType.getKind() != TypeKind.INT) {
            error(obj.getIndex().getLineNum(), "数组下标必须是 int，实际为 " + typeName(idxType));
        }
        // 设置元素类型
        if (idxType.getKind() != TypeKind.INT) {
            obj.setElementType(errorType());
            pushType(errorType());
            return;
        }
        obj.setElementType(elementType);
        pushType(obj.getElementType());
    }

    @Override
    public void visit(Ast.Expr.ArrayLength obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            error(obj.getLineNum(), "内部错误: 方法 '" + currMethodName + "' 的变量表未找到");
            pushType(errorType());
            return;
        }
        Ast.Type.Base arrayType = isVariableVisible(obj.getArrayName())
                ? mTable.get(obj.getArrayName()) : null;
        if (arrayType == null) {
            error(obj.getLineNum(), "未定义的数组: " + obj.getArrayName());
        }
        if (arrayType == null) {
            pushType(errorType());
            return;
        }
        if (getElementType(arrayType) == null) {
            error(obj.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    obj.getArrayName(), typeName(arrayType)));
            pushType(errorType());
            return;
        }
        pushType(new Ast.Type.Int());
    }

    @Override
    public void visit(Ast.Stmt.ArrayAssign obj) {
        Ast.Type.Base idxType = analyzeExpression(obj.getIndex());
        Ast.Type.Base elemType = analyzeExpression(obj.getExpr());
        // 检查数组是否已声明
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if (mTable == null) {
            error(obj.getLineNum(), "内部错误: 方法 '" + currMethodName + "' 的变量表未找到");
            return;
        }
        Ast.Type.Base arrayType = isVariableVisible(obj.getArrayName())
                ? mTable.get(obj.getArrayName()) : null;
        if (arrayType == null) {
            error(obj.getLineNum(), "未定义的数组: " + obj.getArrayName());
            return;
        }
        // 设置元素类型
        Ast.Type.Base elementType = getElementType(arrayType);
        if (elementType == null) {
            error(obj.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    obj.getArrayName(), typeName(arrayType)));
            return;
        }
        obj.setElementType(elementType);
        // 检查下标类型
        if (!isErrorType(idxType) && idxType.getKind() != TypeKind.INT) {
            error(obj.getIndex().getLineNum(), "数组下标必须是 int，实际为 " + typeName(idxType));
        }
        // 检查赋值类型
        if (!isErrorType(elemType) && !isMatch(elementType, elemType)) {
            error(obj.getLineNum(), String.format("不能将 %s 类型的表达式赋值给 %s 数组元素",
                    typeName(elemType), typeName(elementType)));
        }
    }

    // 获取数组元素类型
    private Ast.Type.Base getElementType(Ast.Type.Base arrayType) {
        if (arrayType instanceof Ast.Type.IntArray) {
            return new Ast.Type.Int();
        } else if (arrayType instanceof Ast.Type.FloatArray) {
            return new Ast.Type.Float();
        } else if (arrayType instanceof Ast.Type.DoubleArray) {
            return new Ast.Type.Double();
        } else if (arrayType instanceof Ast.Type.BoolArray) {
            return new Ast.Type.Bool();
        }
        return null;
    }
}
