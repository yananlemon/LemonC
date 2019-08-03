package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

public class SemanticVisitor implements ISemanticVisitor {

    private boolean pass = true;

    private Ast.Type.T currType;

    private String currMethodName;

    private Hashtable<String,MethodVarTable> methodVarTable;

    public SemanticVisitor(){
        this.methodVarTable = new Hashtable<String,MethodVarTable>();
    }

    public boolean passOrNot(){
        return pass;
    }

    @Override
    public void visit(Ast.Expr.Add obj) {
        this.visit(obj.left);
        Ast.Type.T leftType = this.currType;
        this.visit(obj.right);
        if( !isMatch(leftType,this.currType))
            error(obj.lineNum,String.format("左边表达式的类型%s与右边表达式的类型%s不匹配。",
                    leftType.toString(),this.currType.toString()));
    }

    @Override
    public void visit(Ast.Expr.And obj) {

    }

    @Override
    public void visit(Ast.Type.Bool obj) {

    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {
        if(obj.expr instanceof Ast.Expr.T){
            this.visit((Ast.Expr.T)obj.expr);
            Ast.Type.T exprType = this.currType;
            this.visit(obj.id);
            if( !isMatch(this.currType,exprType))
                error(obj.lineNum,String.format("不能将类型%s的表达式赋值给类型%s的表达式。",
                        this.currType.toString(),exprType.toString()));
        }

    }

    @Override
    public void visit(Ast.Stmt.Block obj) {

    }

    @Override
    public void visit(Ast.Expr.Call obj) {

    }

    @Override
    public void visit(Ast.Declare obj) {

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

    }

    @Override
    public void visit(Ast.Expr.Id obj) {
        MethodVarTable mTable = this.methodVarTable.get(currMethodName);
        if( mTable.get(obj.id) == null )
            error( obj.lineNum,obj.id+"未定义");
        this.currType = obj.type;
    }

    @Override
    public void visit(Ast.Stmt.If obj) {
        this.visit(obj.condition);
        if (!this.currType.toString().equals(new Ast.Type.Bool().toString()))
            error(obj.lineNum,
                    "条件表达式的类型应该是Bool。");

        this.visit(obj.thenStmt);
        this.visit(obj.elseStmt);
    }

    @Override
    public void visit(Ast.Type.Int obj) {

    }

    @Override
    public void visit(Ast.Expr.LT obj) {

    }

    @Override
    public void visit(Ast.MainClass.MainClassSingle obj) {
        HashSet<String> methodSet = new HashSet<String>();
        for(int i = 0; i < obj.methods.size(); i++){
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) obj.methods.get(i);
            if(methodSet.add(method.id))
                this.visit(method);
            else
                error(method.lineNum, "重复的方法： " + method.id);
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle obj) {
        MethodVarTable mTable = new MethodVarTable();
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
            Ast.Type.T typeDeclared = obj.retType;
            if(!(lastStmt instanceof Ast.Stmt.Return))
                error(obj.lineNum, "行"+ lastStmt.lineNum+obj.id+"方法的缺少return语句 ");
            this.visit(lastStmt);
            if( !isMatch(typeDeclared,this.currType))
                error(obj.lineNum, "行"+ lastStmt.lineNum+"返回值与声明的不一致： "+typeDeclared+"!="+this.currType);
        }
        int size = flag == true ? obj.stms.size() : obj.stms.size() - 1;
        for( int i = 0; i < size; i++){
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
        if(obj instanceof Ast.Stmt.Return){
            this.visit((Ast.Stmt.Return)obj);
        }else if(obj instanceof Ast.Stmt.Assign){
            this.visit((Ast.Stmt.Assign)obj);
        }else if(obj instanceof Ast.Stmt.If){
            this.visit((Ast.Stmt.If)obj);
        }

    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {

    }

    @Override
    public void visit(Ast.Expr.T obj) {
        if(obj instanceof Ast.Expr.Id){
            this.visit((Ast.Expr.Id)obj);
        }
        else if(obj instanceof Ast.Expr.Add){
            this.visit((Ast.Expr.Add)obj);
        }
        else if(obj instanceof Ast.Expr.And){
            this.visit((Ast.Expr.And)obj);
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
    public void visit(Ast.Stmt.Return obj) {
        this.visit(obj.expr);
    }

    @Override
    public void visit(Ast.Stmt obj) {

    }

    @Override
    public void visit(Ast.Stmt.While obj) {

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
