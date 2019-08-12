package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * 语义分析
 * @author andy
 *
 */
public class SemanticVisitor implements ISemanticVisitor {

    private boolean pass = true;

    private Ast.Type.T currType;

    private String currMethodName;

    private Hashtable<String,MethodVarTable> methodVarTable;

    private Hashtable<String,Ast.Type.T> methodNameRetTypeMap;

    private HashSet<String> currMethodLocalVar;

    private HashMap<String,Ast.Method.MethodSingle> methodMap;

    private Ast.Type.T typeOfMethodDeclared;

    public SemanticVisitor(){

        this.methodVarTable = new Hashtable<String,MethodVarTable>();
        this.methodMap = new HashMap<>();
        this.methodNameRetTypeMap = new Hashtable<>();
    }

    public boolean passOrNot(){
        return pass;
    }

    @Override
    public void visit(Ast.Expr.Add obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !isMatch(leftType,this.currType) )
                error(obj.lineNum,String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Expr.And obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !leftType.toString().equals("@bool") || !this.currType.toString().equals("@bool"))
            error(obj.lineNum,String.format("&& 运算符要求左右表达式必须是bool",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Type.Bool obj) {

    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {
        if(obj.expr instanceof Ast.Expr.T){
            this.visit((Ast.Expr.T)obj.expr);
            Ast.Type.T exprType = this.currType;
            if( this.currMethodLocalVar.contains(obj.id.id))
                this.currMethodLocalVar.remove(obj.id.id);
            this.visit(obj.id);
            if( !isMatch(this.currType,exprType))
                error(obj.lineNum,String.format("不能将类型%s的表达式赋值给类型%s的表达式。",
                        this.currType.toString(),exprType.toString()));
        }

    }

    @Override
    public void visit(Ast.Stmt.Block obj) {
        for( Ast.Stmt.T stmt : obj.stmts){
            this.visit(stmt);
        }
    }

    @Override
    public void visit(Ast.Expr.Call obj) {
        Ast.Method.MethodSingle currMethod = this.methodMap.get(obj.name);
       if( currMethod != null ){
           if( obj.inputParams.size() != currMethod.formals.size() )
               error(obj.lineNum,String.format("方法%s的参数个数不正确",obj.name));
           for( int i = 0; i < obj.inputParams.size(); i++){
               this.visit(obj.inputParams.get(i));
               Ast.Type.T type = this.currType;
               this.visit(currMethod.formals.get(i));
               if( !isMatch(type,this.currType))
                   error(obj.lineNum,String.format("方法%s的参数%s的类型应为%s",
                           obj.toString(),obj.inputParams.get(i).toString(),this.currType.toString()));
           }
           obj.returnType = this.methodNameRetTypeMap.get(obj.name);
           this.currType = obj.returnType;
           ArrayList<Ast.Expr.T> inputParams = obj.inputParams;
           for( int i = 0; i < obj.inputParams.size(); i++){
               this.visit(obj.inputParams.get(i));
           }

       }else{
           error(obj.lineNum,"未定义的方法："+obj.name);
       }
    }

    @Override
    public void visit(Ast.Declare.T obj) {

    }

    @Override
    public void visit(Ast.Expr.Div obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !isMatch(leftType,this.currType))
            error(obj.lineNum,String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Type.Float obj) {

    }

    @Override
    public void visit(Ast.Expr obj) {

    }

    @Override
    public void visit(Ast.Expr.GT obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.currType;
        this.visit(obj.right);
        boolean numberType = this.currType.toString().equals("@int") || this.currType.toString().equals("@float");
        if( !isMatch(t,this.currType) || !numberType){
            error(obj.lineNum, String.format("类型%s和类型%s之间不能应用比较运算符 > 。",t.toString(),this.currType.toString()));
        }
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.Id obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if( mTable.get(obj.id) == null )
            error( obj.lineNum,obj.id+"未定义");
        if( currMethodLocalVar.contains(obj.id))
            error(obj.lineNum,String.format("你应该在使用%s之前先分配一个值",obj.id));
        this.currType = obj.type;
    }

    @Override
    public void visit(Ast.Stmt.If obj) {
        this.visit(obj.condition);
        if (!this.currType.toString().equals(new Ast.Type.Bool().toString()))
            error(obj.condition.lineNum,
                    "条件表达式的类型应该是Bool。");

        this.visit(obj.thenStmt);
        this.visit(obj.elseStmt);
    }

    @Override
    public void visit(Ast.Type.Int obj) {

    }

    @Override
    public void visit(Ast.Program.T programSingle) {
        this.visit(((Ast.Program.ProgramSingle)programSingle).mainClass);
    }

    @Override
    public void visit(Ast.Expr.LT obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.currType;
        this.visit(obj.right);
        boolean numberType = this.currType.toString().equals("@int") || this.currType.toString().equals("@float");
        if( !isMatch(t,this.currType) || !numberType){
            error(obj.lineNum, String.format("类型%s和类型%s之间不能应用比较运算符 > 。",t.toString(),this.currType.toString()));
        }
        this.currType = new Ast.Type.Bool();
    }

    @Override
    public void visit(Ast.Expr.LET obj) {

    }

    @Override
    public void visit(Ast.Expr.GET obj) {

    }

    @Override
    public void visit(Ast.MainClass.T obj) {
        Ast.MainClass.MainClassSingle mainClassSingle = (Ast.MainClass.MainClassSingle) obj;
        //methodSet = new HashSet<String>();
        for(int i = 0; i < mainClassSingle.methods.size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.methods.get(i);
            if( methodMap.containsKey(method.id)){
                error(method.lineNum, "重复的方法： " + method.id);
            }else{
                methodMap.put(method.id,method);
                methodNameRetTypeMap.put(method.id,method.retType);
            }
        }
        for(int i = 0; i < mainClassSingle.methods.size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) mainClassSingle.methods.get(i);
            /*if(methodSet.add(method.id))
                this.visit(method);
            else
                error(method.lineNum, "重复的方法： " + method.id);*/
            this.visit(method);
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle obj) {
        MethodVarTable mTable = new MethodVarTable();
        this.currMethodLocalVar = new HashSet<String>();
        for( Ast.Declare.T dec : obj.locals){
            this.currMethodLocalVar.add(((Ast.Declare.DeclareSingle)dec).id);
        }

        mTable.put(obj.formals,obj.locals);
        this.methodVarTable.put(obj.id,mTable);
        this.currMethodName = obj.id;
        boolean flag = true;
        Ast.Stmt.T lastStmt  = obj.stms.get(obj.stms.size()-1);;
        if( obj.id.equals("main")){
            if( !obj.retType.toString().equals("@void"))
                error(obj.lineNum, "main方法的返回类型不能是： " + obj.retType);

            if(lastStmt instanceof Ast.Stmt.Return)
                error(obj.lineNum, "行"+ lastStmt.lineNum+"main方法的不可以有return语句 ");
        }
        else{
            flag = false;
            // 判断方法返回类型和返回的表达式类型是否一致
            //Ast.Type.T typeDeclared = obj.retType;
            this.typeOfMethodDeclared = obj.retType;
            /*if(!(lastStmt instanceof Ast.Stmt.Return))
                error(obj.lineNum, "行"+ lastStmt.lineNum+obj.id+"方法的缺少return语句 ");
            this.visit(lastStmt);*/

        }
        //int size = flag == true ? obj.stms.size() : obj.stms.size() - 1;
        for( int i = 0; i < obj.stms.size(); i++){
            Ast.Stmt.T stmt = obj.stms.get(i);
            this.visit(stmt);
        }
    }

    @Override
    public void visit(Ast.Expr.Mul obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !isMatch(leftType,this.currType))
            // 暂时不支持类型转换
            error(obj.lineNum,String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));


    }

    @Override
    public void visit(Ast.Expr.Number obj) {
        if(obj.type instanceof Ast.Type.Int){
            this.currType = new Ast.Type.Int();
        }else if(obj.type instanceof Ast.Type.Float){
            this.currType = new Ast.Type.Float();
        }else{
            // 不支持的数字类型
            error(obj.lineNum,"不支持的数字类型："+obj.type.toString());
        }
    }

    @Override
    public void visit(Ast.Expr.Or obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !leftType.toString().equals("@bool") && !isMatch(leftType,this.currType))
            error(obj.lineNum,String.format("|| 运算符要求左右表达式必须是bool",
                    leftType.toString(),this.currType.toString()));
    }



    @Override
    public void visit(Ast.Type.Str obj) {

    }

    @Override
    public void visit(Ast.Expr.Sub obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !isMatch(leftType,this.currType))
            error(obj.lineNum,String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
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
        if(obj instanceof Ast.Stmt.Block)
            this.visit((Ast.Stmt.Block)obj);
        else if(obj instanceof Ast.Stmt.Return)
            this.visit((Ast.Stmt.Return)obj);
        else if(obj instanceof Ast.Stmt.Assign)
            this.visit((Ast.Stmt.Assign)obj);
        else if(obj instanceof Ast.Stmt.If)
            this.visit((Ast.Stmt.If)obj);
        else if(obj instanceof Ast.Stmt.Block)
            this.visit((Ast.Stmt.Block)obj);
        else if(obj instanceof Ast.Stmt.While)
            this.visit((Ast.Stmt.While)obj);
        else if(obj instanceof Ast.Stmt.Call)
            this.visit((Ast.Stmt.Call)obj);
        else if(obj instanceof Ast.Stmt.Printf)
            this.visit((Ast.Stmt.Printf)obj);
        else if(obj instanceof Ast.Stmt.PrintLine)
            this.visit((Ast.Stmt.PrintLine)obj);

    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {
        if( obj.exprs == null || obj.exprs.size() <= 0)
            error(obj.lineNum,"printf 需要有表达式");
        String format = obj.format;
        String[] array = format.split("%d");
        if( array.length == 0 )
            error(obj.lineNum,"printf 语句第1个参数必须包含%d");
        for( int i = 1; i < array.length; i++ ){
            Ast.Expr.T expr = obj.exprs.get(i);
            this.visit(expr);
            if(!isMatch(new Ast.Type.Int(),this.currType) && !isMatch(new Ast.Type.Float(),this.currType))
                error(expr.lineNum,String.format("表达式%s的类型需要是int或float",expr.toString()));
        }

    }

    @Override
    public void visit(Ast.Stmt.PrintLine obj) {

    }

    @Override
    public void visit(Ast.Expr.T obj) {
        if(obj instanceof Ast.Expr.Call){
            this.visit((Ast.Expr.Call)obj);
        }
        else if(obj instanceof Ast.Expr.Id){
            this.visit((Ast.Expr.Id)obj);
        }
        else if(obj instanceof Ast.Expr.Add){
            this.visit((Ast.Expr.Add)obj);
        }
        else if(obj instanceof Ast.Expr.And){
            this.visit((Ast.Expr.And)obj);
        }
        else if(obj instanceof Ast.Expr.Or){
            this.visit((Ast.Expr.Or)obj);
        }
        else if(obj instanceof Ast.Expr.Sub){
            this.visit((Ast.Expr.Sub)obj);
        }
        else if(obj instanceof Ast.Expr.Mul){
            this.visit((Ast.Expr.Mul)obj);
        }
        else if(obj instanceof Ast.Expr.Div){
            this.visit((Ast.Expr.Div)obj);
        }
        else if(obj instanceof Ast.Expr.True){
            this.visit((Ast.Expr.True)obj);
        }
        else if(obj instanceof Ast.Expr.Number){
            this.visit((Ast.Expr.Number)obj);
        }
        else if(obj instanceof Ast.Expr.False){
            this.visit((Ast.Expr.False)obj);
        }
        else if(obj instanceof Ast.Expr.Not){
            this.visit((Ast.Expr.Not)obj);
        }
        else if(obj instanceof Ast.Expr.LT){
            this.visit((Ast.Expr.LT)obj);
        }
        else if(obj instanceof Ast.Expr.GT){
            this.visit((Ast.Expr.GT)obj);
        }


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
        this.visit(obj.expr);
        if( !this.currType.toString().equals("@bool"))
            error(obj.lineNum,"表达式的类型似乎不是Bool。");
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
        this.visit(obj.expr);
        if( !isMatch(typeOfMethodDeclared,this.currType))
            error(obj.lineNum,String.format("返回值%s与声明的%s不一致。",typeOfMethodDeclared.toString(),this.currType.toString()));
    }


    @Override
    public void visit(Ast.Stmt.While obj) {
        this.visit(obj.condition);
        if( !this.currType.toString().equals("@bool") )
            error(obj.condition.lineNum, "while语句的条件表达式的类型应该是bool。");
        this.visit(obj.body);

    }

    @Override
    public void visit(Ast.Stmt.Call obj) {
        Ast.Method.MethodSingle currMethod = this.methodMap.get(obj.name);
        if( currMethod != null ){
            if( obj.inputParams.size() != currMethod.formals.size() )
                error(obj.lineNum,String.format("方法%s的参数个数不正确",obj.name));
            for( int i = 0; i < obj.inputParams.size(); i++){
                this.visit(obj.inputParams.get(i));
                Ast.Type.T type = this.currType;
                this.visit(currMethod.formals.get(i));
                if( !isMatch(type,this.currType))
                    error(obj.lineNum,String.format("方法%s的参数%s的类型应为%s",
                            obj.toString(),obj.inputParams.get(i).toString(),this.currType.toString()));
            }
            obj.returnType = this.methodNameRetTypeMap.get(obj.name);
            this.currType = obj.returnType;
            ArrayList<Ast.Expr.T> inputParams = obj.inputParams;
            for( int i = 0; i < obj.inputParams.size(); i++){
                this.visit(obj.inputParams.get(i));
            }

        }else{
            error(obj.lineNum,"未定义的方法："+obj.name);
        }
    }

    private void error(int lineNum, String msg){
        this.pass = false;
        System.out.println("错误: 行 " + lineNum + " " + msg);
        System.exit(1);
    }

    private boolean isMatch(Ast.Type.T target,Ast.Type.T curr){
        if(target.toString().equals(curr.toString()))
            return true;
        else
            return false;
    }
}
