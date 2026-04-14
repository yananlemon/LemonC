package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.ast.Ast.Type.TypeKind;
import site.ilemon.exception.SemanticException;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
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

    private Ast.Type.T currType;

    private String currMethodName;

    private HashMap<String,MethodVarTable> methodVarTable;

    private HashMap<String,Ast.Type.T> methodNameRetTypeMap;

    private HashSet<String> currMethodLocalVar;

    private HashMap<String,Ast.Method.MethodSingle> methodMap;

    private Ast.Type.T typeOfMethodDeclared;

    public SemanticVisitor(){

        this.methodVarTable = new HashMap<String,MethodVarTable>();
        this.methodMap = new HashMap<>();
        this.methodNameRetTypeMap = new HashMap<>();
    }

    public boolean passOrNot(){
        return pass;
    }

    @Override
    public void visit(Ast.Expr.Add obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        if( !isMatch(leftType,this.currType) )
                error(obj.getLineNum(),String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Expr.And obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        if( leftType.getKind() != TypeKind.BOOL || this.currType.getKind() != TypeKind.BOOL)
            error(obj.getLineNum(),String.format("&& 运算符要求左右表达式必须是bool",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Type.Bool obj) {

    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {
        if(obj.getExpr() instanceof Ast.Expr.T){
            this.visit((Ast.Expr.T)obj.getExpr());
            Ast.Type.T exprType = null;
            if( obj.getExpr() instanceof Ast.Expr.Call)
                exprType = ((Ast.Expr.Call) obj.getExpr()).getReturnType();
            else
                exprType = this.currType;
            if( this.currMethodLocalVar.contains(obj.getId().getId()))
                this.currMethodLocalVar.remove(obj.getId().getId());
            this.visit(obj.getId());
            if( !isMatch(this.currType,exprType))
                error(obj.getLineNum(),String.format("不能将类型%s的表达式赋值给类型%s的表达式。",
                        this.currType.toString(),exprType.toString()));
        }

    }

    @Override
    public void visit(Ast.Stmt.Block obj) {
        for( Ast.Stmt.T stmt : obj.getStmts()){
            this.visit(stmt);
        }
    }

    @Override
    public void visit(Ast.Expr.Call obj) {
        Ast.Type.T returnType = validateMethodCall(obj.getName(), obj.getInputParams(), obj.getLineNum());
        obj.setReturnType(returnType);
        this.currType = returnType;
    }

    @Override
    public void visit(Ast.Declare.T obj) {

    }

    @Override
    public void visit(Ast.Expr.Div obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        if( !isMatch(leftType,this.currType))
            error(obj.getLineNum(),String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Type.Float obj) {

    }

    @Override
    public void visit(Ast.Type.Double obj) {

    }

    @Override
    public void visit(Ast.Expr obj) {

    }

    @Override
    public void visit(Ast.Expr.GT obj) {
        checkOrderComparison(obj.getLeft(), obj.getRight(), ">", obj.getLineNum());
    }

    @Override
    public void visit(Ast.Expr.Id obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if( mTable == null )
            error( obj.getLineNum(), "内部错误: 方法 '" + currMethodName + "' 的变量表未找到");
        if( mTable.get(obj.getId()) == null )
            error( obj.getLineNum(), "未定义的变量: " + obj.getId());
        if( currMethodLocalVar.contains(obj.getId()))
            error(obj.getLineNum(),String.format("变量 '%s' 在使用前未赋值",obj.getId()));
        if( obj.getType() == null ) {
            // 类型可能在 Parser 阶段未成功解析（变量未在 varTable 中注册）
            obj.setType(mTable.get(obj.getId()));
        }
        this.currType = obj.getType();
    }

    @Override
    public void visit(Ast.Stmt.If obj) {
        this.visit(obj.getCondition());
        if (this.currType.getKind() != TypeKind.BOOL)
            error(obj.getCondition().getLineNum(),
                    "条件表达式的类型应该是Bool。");

        this.visit(obj.getThenStmt());
        if (obj.getElseStmt() != null)
            this.visit(obj.getElseStmt());
    }

    @Override
    public void visit(Ast.Type.Int obj) {

    }

    @Override
    public void visit(Ast.Program.T programSingle) {
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
    public void visit(Ast.MainClass.T obj) {
        Ast.MainClass.MainClassSingle mainClassSingle = (Ast.MainClass.MainClassSingle) obj;
        for(int i = 0; i < mainClassSingle.getMethods().size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.getMethods().get(i);
            if( methodMap.containsKey(method.getId())){
                error(method.getLineNum(), "重复的方法： " + method.getId());
            }else{
                methodMap.put(method.getId(),method);
                methodNameRetTypeMap.put(method.getId(),method.getRetType());
            }
        }
        for(int i = 0; i < mainClassSingle.getMethods().size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.getMethods().get(i);
            this.visit(method);
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle obj) {
        MethodVarTable mTable = new MethodVarTable();
        this.currMethodLocalVar = new HashSet<String>();
        for( Ast.Declare.T dec : obj.getLocals()){
            this.currMethodLocalVar.add(((Ast.Declare.DeclareSingle)dec).getId());
        }

        mTable.put(obj.getFormals(),obj.getLocals());
        this.methodVarTable.put(obj.getId(),mTable);
        this.currMethodName = obj.getId();

        if( obj.getStms().isEmpty() ){
            if( obj.getRetType().getKind() != TypeKind.VOID ){
                error(obj.getLineNum(), "非void方法 '" + obj.getId() + "' 体内没有语句");
            }
            return;
        }

        Ast.Stmt.T lastStmt = obj.getStms().get(obj.getStms().size()-1);
        if( obj.getId().equals("main")){
            if( obj.getRetType().getKind() != TypeKind.VOID)
                error(obj.getLineNum(), "main方法的返回类型必须是void，但声明为: " + obj.getRetType());

            if(lastStmt instanceof Ast.Stmt.Return)
                error(obj.getLineNum(), "行 "+ lastStmt.getLineNum() + ": main方法不可以有return语句");
        }
        else{
            this.typeOfMethodDeclared = obj.getRetType();
        }
        for( int i = 0; i < obj.getStms().size(); i++){
            Ast.Stmt.T stmt = obj.getStms().get(i);
            this.visit(stmt);
        }
    }

    @Override
    public void visit(Ast.Expr.Mul obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        if( !isMatch(leftType,this.currType))
            // 暂时不支持类型转换
            error(obj.getLineNum(),String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));


    }

    @Override
    public void visit(Ast.Expr.Number obj) {
        if(obj.getType() instanceof Ast.Type.Int){
            this.currType = new Ast.Type.Int();
        }else if(obj.getType() instanceof Ast.Type.Float){
            this.currType = new Ast.Type.Float();
        }else if(obj.getType() instanceof Ast.Type.Double){
            this.currType = new Ast.Type.Double();
        }else{
            // 不支持的数字类型
            error(obj.getLineNum(),"不支持的数字类型："+obj.getType().toString());
        }
    }

    @Override
    public void visit(Ast.Expr.Or obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        if( leftType.getKind() != TypeKind.BOOL || this.currType.getKind() != TypeKind.BOOL)
            error(obj.getLineNum(),String.format("|| 运算符要求左右表达式必须是bool",
                    leftType.toString(),this.currType.toString()));
    }



    @Override
    public void visit(Ast.Type.Str obj) {

    }

    @Override
    public void visit(Ast.Expr.Sub obj) {
        this.visit(obj.getLeft());
        Ast.Type.T leftType = this.currType;
        this.visit(obj.getRight());
        if( !isMatch(leftType,this.currType))
            error(obj.getLineNum(),String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Type obj) {

    }

    @Override
    public void visit(Ast.Type.Void obj) {

    }

    @Override
    public void visit(Ast.Stmt.T obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {
        if( obj.getExprs() == null || obj.getExprs().size() <= 0)
            error(obj.getLineNum(),"printf 需要有表达式");
        String format = obj.getFormat();
        String[] array = format.split("%d|%f");
        if( array.length == 0 )
            error(obj.getLineNum(),"printf 语句第1个参数必须包含%d");
        for( int i = 1; i < obj.getExprs().size(); i++ ){
            Ast.Expr.T expr = obj.getExprs().get(i);
            this.visit(expr);
            if(!isMatch(new Ast.Type.Str(),this.currType) &&!isMatch(new Ast.Type.Int(),this.currType) && !isMatch(new Ast.Type.Float(),this.currType) && !isMatch(new Ast.Type.Double(),this.currType))
                error(expr.getLineNum(),String.format("表达式%s的类型需要是int、float或double",expr.toString()));
        }

    }

    @Override
    public void visit(Ast.Stmt.PrintLine obj) {

    }

    @Override
    public void visit(Ast.Expr.T obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Ast.Expr.True obj) {
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.False obj) {
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.Not obj) {
        this.visit(obj.getExpr());
        if( this.currType.getKind() != TypeKind.BOOL)
            error(obj.getLineNum(),"表达式的类型似乎不是Bool。");
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.Str obj) {

    }

    @Override
    public void visit(Ast.Type.T obj) {

    }

    @Override
    public void visit(Ast.Stmt.Return obj) {
        this.visit(obj.getExpr());
        if( !isMatch(typeOfMethodDeclared,this.currType))
            error(obj.getLineNum(),String.format("返回值%s与声明的%s不一致。",typeOfMethodDeclared.toString(),this.currType.toString()));
    }


    @Override
    public void visit(Ast.Stmt.While obj) {
        this.visit(obj.getCondition());
        if( this.currType.getKind() != TypeKind.BOOL )
            error(obj.getCondition().getLineNum(), "while语句的条件表达式的类型应该是bool。");
        this.visit(obj.getBody());

    }

    @Override
    public void visit(Ast.Stmt.Call obj) {
        Ast.Type.T returnType = validateMethodCall(obj.getName(), obj.getInputParams(), obj.getLineNum());
        obj.setReturnType(returnType);
        this.currType = returnType;
    }

    private void error(int lineNum, String msg){
        this.pass = false;
        throw new SemanticException("[语义分析] 行 " + lineNum + ": " + msg);
    }

    private boolean isMatch(Ast.Type.T target,Ast.Type.T curr){
        if( target == null || curr == null )
            return false;
        if(target.getKind() == curr.getKind())
            return true;
        // 允许float隐式转换为double
        if(target.getKind() == TypeKind.DOUBLE && curr.getKind() == TypeKind.FLOAT)
            return true;
        return false;
    }

    private boolean isNumberType(Ast.Type.T type) {
        TypeKind kind = type.getKind();
        return kind == TypeKind.INT || kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE;
    }

    /**
     * 校验比较运算符（== / !=）：只要左右类型匹配即可
     */
    private void checkComparison(Ast.Expr.T left, Ast.Expr.T right, String op, int lineNum) {
        this.visit(left);
        Ast.Type.T leftType = this.currType;
        this.visit(right);
        if (!isMatch(leftType, this.currType)) {
            error(lineNum, String.format("类型%s和类型%s之间不能应用比较运算符 %s",
                    leftType, this.currType, op));
        }
        this.currType = new Ast.Type.Bool();
    }

    /**
     * 校验序比较运算符（> / < / >= / <=）：除了类型匹配，还要求是数值类型
     */
    private void checkOrderComparison(Ast.Expr.T left, Ast.Expr.T right, String op, int lineNum) {
        this.visit(left);
        Ast.Type.T leftType = this.currType;
        this.visit(right);
        if (!isMatch(leftType, this.currType) || !isNumberType(this.currType)) {
            error(lineNum, String.format("类型%s和类型%s之间不能应用比较运算符 %s",
                    leftType, this.currType, op));
        }
        this.currType = new Ast.Type.Bool();
    }

    /**
     * 公共方法调用校验逻辑，被 Expr.Call 和 Stmt.Call 共用。
     * 校验方法是否存在、参数个数是否匹配、参数类型是否匹配。
     * @return 方法的返回类型
     */
    private Ast.Type.T validateMethodCall(String methodName, ArrayList<Ast.Expr.T> inputParams, int lineNum) {
        Ast.Method.MethodSingle method = this.methodMap.get(methodName);
        if (method == null) {
            error(lineNum, "未定义的方法：" + methodName);
            return null; // unreachable, error() throws
        }
        if (inputParams.size() != method.getFormals().size()) {
            error(lineNum, String.format("方法%s的参数个数不正确，期望 %d 个，实际 %d 个",
                    methodName, method.getFormals().size(), inputParams.size()));
        }
        for (int i = 0; i < inputParams.size(); i++) {
            this.visit(inputParams.get(i));
            Ast.Type.T actualType = this.currType;
            this.visit(method.getFormals().get(i));
            Ast.Type.T expectedType = this.currType;
            if (!isMatch(actualType, expectedType)) {
                error(lineNum, String.format("方法%s的第%d个参数类型不匹配，期望%s，实际%s",
                        methodName, i + 1, expectedType, actualType));
            }
        }
        return this.methodNameRetTypeMap.get(methodName);
    }

    // ========== 数组相关的 visit 方法 ==========

    @Override
    public void visit(Ast.Type.IntArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.FloatArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.DoubleArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Type.BoolArray obj) {
        this.currType = obj;
    }

    @Override
    public void visit(Ast.Expr.ArrayAccess obj) {
        // 检查数组是否已声明
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        Ast.Type.T arrayType = mTable.get(obj.getArrayName());
        if (arrayType == null) {
            error(obj.getLineNum(), "未定义的数组: " + obj.getArrayName());
        }
        // 检查下标类型必须是int
        this.visit(obj.getIndex());
        if (this.currType.getKind() != TypeKind.INT) {
            error(obj.getLineNum(), "数组下标必须是int类型");
        }
        // 设置元素类型
        obj.setElementType(getElementType(arrayType));
        this.currType = obj.getElementType();
    }

    @Override
    public void visit(Ast.Expr.ArrayLength obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        Ast.Type.T arrayType = mTable.get(obj.getArrayName());
        if (arrayType == null) {
            error(obj.getLineNum(), "未定义的数组: " + obj.getArrayName());
        }
        this.currType = new Ast.Type.Int();
    }

    @Override
    public void visit(Ast.Stmt.ArrayAssign obj) {
        // 检查数组是否已声明
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        Ast.Type.T arrayType = mTable.get(obj.getArrayName());
        if (arrayType == null) {
            error(obj.getLineNum(), "未定义的数组: " + obj.getArrayName());
        }
        // 设置元素类型
        Ast.Type.T elementType = getElementType(arrayType);
        obj.setElementType(elementType);
        // 检查下标类型
        this.visit(obj.getIndex());
        if (this.currType.getKind() != TypeKind.INT) {
            error(obj.getLineNum(), "数组下标必须是int类型");
        }
        // 检查赋值类型
        this.visit(obj.getExpr());
        if (!isMatch(elementType, this.currType)) {
            error(obj.getLineNum(), String.format("不能将类型%s赋值给%s数组元素",
                    this.currType.toString(), elementType.toString()));
        }
    }

    // 获取数组元素类型
    private Ast.Type.T getElementType(Ast.Type.T arrayType) {
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
