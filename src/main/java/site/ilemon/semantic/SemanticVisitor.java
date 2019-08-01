package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.HashSet;

public class SemanticVisitor implements ISemanticVisitor {

    private boolean pass = true;

    private Ast.Type.T currType;

    public boolean passOrNot(){
        return pass;
    }

    @Override
    public void visit(Ast.Expr.Add obj) {

    }

    @Override
    public void visit(Ast.Expr.And obj) {

    }

    @Override
    public void visit(Ast.Type.Bool obj) {

    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {

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
        this.currType = obj.type;
    }

    @Override
    public void visit(Ast.Stmt.If obj) {

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
        Ast.Stmt.T lastStmt  = obj.stms.get(obj.stms.size()-1);;
        if( obj.id.equals("main")){
            if( !obj.retType.toString().equals("@void"))
                error(obj.lineNum, "main方法的返回类型不能是： " + obj.retType);

            if(lastStmt instanceof Ast.Stmt.Return)
                error(obj.lineNum, "行"+ lastStmt.lineNum+"main方法的不可以有return语句 ");
        }
        else{
            // 判断方法返回类型和返回的表达式类型是否一致
            Ast.Type.T typeDeclared = obj.retType;
            if(!(lastStmt instanceof Ast.Stmt.Return))
                error(obj.lineNum, "行"+ lastStmt.lineNum+obj.id+"方法的缺少return语句 ");
            this.visit(lastStmt);
            if( !isMatch(typeDeclared,this.currType))
                error(obj.lineNum, "行"+ lastStmt.lineNum+"返回值与声明的不一致： "+typeDeclared+"!="+this.currType);
        }


    }

    @Override
    public void visit(Ast.Expr.Mul obj) {

    }

    @Override
    public void visit(Ast.Expr.Number obj) {

    }

    @Override
    public void visit(Ast.Expr.Or obj) {

    }



    @Override
    public void visit(Ast.Type.Str obj) {

    }

    @Override
    public void visit(Ast.Expr.Sub obj) {

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
        System.out.println("Error: Line " + lineNum + " " + msg);
    }

    private boolean isMatch(Ast.Type.T target,Ast.Type.T curr){
        if(target.toString().equals(curr.toString()))
            return true;
        else
            return false;
    }
}
