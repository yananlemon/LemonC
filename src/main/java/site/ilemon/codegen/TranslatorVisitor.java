package site.ilemon.codegen;


import site.ilemon.codegen.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.visitor.ISemanticVisitor;
import site.ilemon.ast.Ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TranslatorVisitor implements ISemanticVisitor {

    private String classId;
    private int index;
    private HashMap<String, Integer> indexTable;
    private Ast.Type.T type;
    private Ast.Declare.DeclareSingle dec;
    private Ast.Method.MethodSingle method;
    private Ast.MainClass.MainClassSingle mainClass;
    public Ast.Program.ProgramSingle prog;
    private List<Ast.Stmt.T> stmts;

    private int strIndex;
    private Label tempLabel;

    public TranslatorVisitor() {
        this.stmts = new ArrayList<Ast.Stmt.T>();
        this.classId = null;
        this.indexTable = null;
        this.type = null;
        this.dec = null;
        this.method = null;
        this.classId = null;
        this.mainClass = null;
        this.prog = null;
    }

    private void emit(Ast.Stmt.T stmt){
        this.stmts.add(stmt);
    }


    @Override
    public void visit(Expr.T obj) {
        if( obj instanceof Expr.Add)
            this.visit((Expr.Add) obj);
        else if( obj instanceof Expr.Sub)
            this.visit((Expr.Sub) obj);
        else if( obj instanceof Expr.Mul)
            this.visit((Expr.Mul) obj);
        else if( obj instanceof Expr.Div)
            this.visit((Expr.Div) obj);
        else if( obj instanceof Expr.Number)
            this.visit((Expr.Number) obj);
        else if( obj instanceof Expr.Str)
            this.visit((Expr.Str) obj);
        else if( obj instanceof Expr.Id)
            this.visit((Expr.Id) obj);
        else if( obj instanceof Expr.Call)
            this.visit((Expr.Call) obj);
        else if( obj instanceof Expr.LT)
            this.visit((Expr.LT) obj);
        else if( obj instanceof Expr.GT)
            this.visit((Expr.GT) obj);

    }

    @Override
    public void visit(Expr.Add obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if( t.toString().equals("@int") ){
            emit(new Ast.Stmt.Iadd());
        }else if( t.toString().equals("@float") ){
            emit(new Ast.Stmt.Fadd());
        }else{
            // error
        }
    }

    @Override
    public void visit(Expr.And obj) {

    }

    @Override
    public void visit(Expr.Call obj) {
        this.visit(obj.returnType);
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for( int i = 0; i < obj.inputParams.size(); i++ ){
            this.visit(obj.inputParams.get(i));
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokevirtual(obj.name,at,returnType));
    }

    @Override
    public void visit(Expr obj) {

    }

    @Override
    public void visit(Expr.GT obj) {
        Label t = null;
        if( tempLabel == null ){
            t = new Label();
            tempLabel = t;
        }
        this.visit(obj.left);
        this.visit(obj.right);
    }

    @Override
    public void visit(Expr.Id obj) {
        int index = this.indexTable.get(obj.id);
        if( obj.type instanceof Type.Int || obj.type instanceof Type.Bool){
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Iload(index));
        }
        else if( obj.type instanceof Type.Float ){
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Fload(index));
        }
        else if( obj.type instanceof Type.Str ){
            this.type = new Ast.Type.Str();
            emit(new Ast.Stmt.Aload(index));
        }
    }

    @Override
    public void visit(Expr.Div obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if( t.toString().equals("@int") ){
            emit(new Ast.Stmt.Idiv());
        }else if( t.toString().equals("@float") ){
            emit(new Ast.Stmt.Fdiv());
        }else{
            // error
        }
    }

    @Override
    public void visit(Expr.Mul obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if( t.toString().equals("@int") ){
            emit(new Ast.Stmt.Imul());
        }else if( t.toString().equals("@float") ){
            emit(new Ast.Stmt.Fmul());
        }else{
            // error
        }
    }

    @Override
    public void visit(Expr.Number obj) {
        if (obj.type instanceof Type.Int)
            emit(new Ast.Stmt.Ldc(Integer.parseInt(obj.value.toString())));
        else if( obj.type instanceof Type.Float )
            emit(new Ast.Stmt.Ldc(Float.parseFloat(obj.value.toString())));
        //else
    }

    @Override
    public void visit(Expr.LT obj) {
        Label t = null;
        if( tempLabel == null ){
            t = new Label();
            tempLabel = t;
        }
        this.visit(obj.left);
        this.visit(obj.right);
    }

    @Override
    public void visit(Expr.Sub obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if( t.toString().equals("@int") ){
            emit(new Ast.Stmt.Isub());
        }else if( t.toString().equals("@float") ){
            emit(new Ast.Stmt.Fsub());
        }else{
            // error
        }
    }

    @Override
    public void visit(Expr.Or obj) {

    }

    @Override
    public void visit(Expr.True obj) {

    }

    @Override
    public void visit(Expr.False obj) {

    }

    @Override
    public void visit(Expr.Not obj) {

    }

    @Override
    public void visit(Expr.Str obj) {
        this.type = new Ast.Type.Str();
        emit(new Ast.Stmt.Ldc("\""+obj.value+"\""));
        //emit(new Ast.Stmt.Astore(strIndex++));
        emit(new Ast.Stmt.Astore(index++));
    }

    @Override
    public void visit(Type.T obj) {
        if( obj instanceof Type.Int)
            this.visit((Type.Int)obj);
        else if(obj instanceof Type.Float)
            this.visit((Type.Float)obj);
        else if(obj instanceof Type.Bool)
            this.visit((Type.Bool)obj);
        else if(obj instanceof Type.Str)
            this.visit((Type.Str)obj);
        else if(obj instanceof Type.Void)
            this.visit((Type.Void)obj);
    }

    @Override
    public void visit(Type.Bool obj) {
        this.type = new Ast.Type.Bool();
    }

    @Override
    public void visit(Type.Float obj) {
        this.type = new Ast.Type.Float();
    }

    @Override
    public void visit(Type.Str obj) {
        this.type = new Ast.Type.Str();
    }

    @Override
    public void visit(Type obj) {

    }

    @Override
    public void visit(Type.Void obj) {
        this.type = new Ast.Type.Void();
    }

    @Override
    public void visit(Type.Int obj) {
        this.type = new Ast.Type.Int();
    }

    @Override
    public void visit(Program.T programSingle) {
        this.visit(((Program.ProgramSingle)programSingle).mainClass);
        this.prog = new Ast.Program.ProgramSingle(this.mainClass);
    }

    @Override
    public void visit(Declare.T obj) {
        Declare.DeclareSingle declareSingle = ((Declare.DeclareSingle)obj);
        this.visit(declareSingle.type);
        this.dec = new Ast.Declare.DeclareSingle(this.type,declareSingle.id);
        if (this.indexTable != null) // if it is field
            this.indexTable.put(declareSingle.id, index++);
        strIndex = this.indexTable.size();
    }


    @Override
    public void visit(MainClass.T obj) {

        MainClass.MainClassSingle mainClassSingle = (MainClass.MainClassSingle) obj;
        this.classId = mainClassSingle.classId;
        List<Ast.Method.MethodSingle> methods = new ArrayList<>();
        for( int i = 0; i < mainClassSingle.methods.size(); i++ ){
            Method.MethodSingle methodSingle = (Method.MethodSingle) mainClassSingle.methods.get(i);
            this.visit(methodSingle);
            methods.add(this.method);
        }
        this.mainClass = new Ast.MainClass.MainClassSingle(this.classId,methods);
    }

    @Override
    public void visit(Method.MethodSingle obj) {
        this.index = 0;
        this.indexTable = new HashMap<>();
        this.visit(obj.retType);
        Ast.Type.T returnType = this.type;

        // 遍历入参
        List<Ast.Declare.DeclareSingle> formals = new ArrayList<>();
        for(int i = 0; i < obj.formals.size(); i++){
            this.visit(obj.formals.get(i));
            formals.add(this.dec);
        }

        // 遍历局部变量
        List<Ast.Declare.DeclareSingle> locals = new ArrayList<>();
        for(int i = 0; i < obj.locals.size(); i++){
            this.visit(obj.locals.get(i));
            locals.add(this.dec);
        }

        // 遍历stmts
        this.stmts = new ArrayList<>();
        for(int i = 0; i < obj.stms.size(); i++){
            this.visit(obj.stms.get(i));
        }
        //this.visit(obj.retExp);

        this.method = new Ast.Method.MethodSingle(returnType,obj.id,this.classId,
                formals,locals,this.stmts,0,this.index);
    }

    @Override
    public void visit(Stmt.If obj) {
        Label l = new Label();
        Label r = new Label();
        this.visit(obj.condition);
        if( obj.condition instanceof Expr.LT){
            emit(new Ast.Stmt.Ificmpgt(l));
        }
        else if( obj.condition instanceof  Expr.GT ){
            emit(new Ast.Stmt.Ificmplt(l));
        }
        this.visit(obj.thenStmt);
        emit(new Ast.Stmt.Goto(r));
        emit(new Ast.Stmt.LabelJ(l));
        this.visit(obj.elseStmt);
        emit(new Ast.Stmt.LabelJ(r));
    }

    @Override
    public void visit(Stmt.T obj) {
        if( obj instanceof Stmt.Assign)
            this.visit((Stmt.Assign)obj);
        else if( obj instanceof Stmt.Block)
            this.visit((Stmt.Block)obj);
        else if( obj instanceof Stmt.If)
            this.visit((Stmt.If)obj);
        else if( obj instanceof Stmt.Printf)
            this.visit((Stmt.Printf)obj);
        else if( obj instanceof Stmt.While)
            this.visit((Stmt.While)obj);
        else if( obj instanceof Stmt.Return)
            this.visit((Stmt.Return)obj);
        // else error
    }

    @Override
    public void visit(Stmt.Assign obj) {
        int index = this.indexTable.get(obj.id.id);
        this.visit(obj.expr);
        if( obj.id.type instanceof Type.Int)
            emit(new Ast.Stmt.Istore(index));
        else if( obj.id.type instanceof Type.Float)
            emit(new Ast.Stmt.Fstore(index));
    }

    @Override
    public void visit(Stmt.Block obj) {
        for( int i = 0; i < obj.stmts.size(); i++){
            this.visit(obj.stmts.get(i));
        }

    }

    @Override
    public void visit(Stmt.Printf obj) {
        String f = obj.format;
        String[] array = f.split("%d|%f");
        if( array.length == 0){
            array = new String[1];
            array[0] = f;
        }
        for (int i = 0; i < array.length; i++) {
            this.visit(new Expr.Str(array[i], obj.lineNum));
            //emit(new Ast.Stmt.Aload(strIndex -1 ) );
            emit(new Ast.Stmt.Aload(index -1 ) );
            emit(new Ast.Stmt.Printf(new Ast.Type.Str(), array[i]));
            if( i+1 < obj.exprs.size() ){
                this.visit(obj.exprs.get(i+1));
                if( this.type instanceof Ast.Type.Int)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Int(), null));
                else if(this.type instanceof Ast.Type.Float)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Float(), null));
            }

        }
    }

    @Override
    public void visit(Stmt.Return obj) {
        if( obj.expr instanceof Expr.Add)
            this.visit((Expr.Add)obj.expr);
        else if( obj.expr instanceof Expr.Sub)
            this.visit((Expr.Sub)obj.expr);
        else if( obj.expr instanceof Expr.Mul)
            this.visit((Expr.Mul)obj.expr);
        else if( obj.expr instanceof Expr.Div)
            this.visit((Expr.Div)obj.expr);
        else if( obj.expr instanceof Expr.Number)
            this.visit((Expr.Number)obj.expr);
        else if( obj.expr instanceof Expr.Id)
            this.visit((Expr.Id)obj.expr);

        if( this.type.toString().equals("@int"))
            emit(new Ast.Stmt.Ireturn());
        else if( this.type.toString().equals("@float"))
            emit(new Ast.Stmt.Freturn());


    }

    @Override
    public void visit(Stmt.While obj) {

    }
}