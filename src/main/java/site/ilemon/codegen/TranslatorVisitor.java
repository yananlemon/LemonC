package site.ilemon.codegen;


import site.ilemon.codegen.ast.Ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TranslatorVisitor implements Visitor{

    private String classId;
    private int index;
    private HashMap<String, Integer> indexTable;
    private Ast.Type.T type;
    private Ast.Declare.DeclareSingle dec;
    private Ast.Method.MethodSingle method;
    private Ast.MainClass.MainClassSingle mainClass;
    public Ast.Program.ProgramSingle prog;
    private List<Ast.Stmt.T> stmts;

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
    public void visit(Ast.Program.ProgramSingle program) {
        this.visit(program.mainClass);
        this.prog = new Ast.Program.ProgramSingle(this.mainClass);
    }

    @Override
    public void visit(Ast.MainClass.MainClassSingle mainClassSingle) {
        for(Ast.Method.MethodSingle methodSingle : mainClassSingle.methods)
            this.visit(methodSingle);
    }

    @Override
    public void visit(Ast.Method.MethodSingle methodSingle) {
        this.index = 1;
        this.indexTable = new HashMap<>();
        //this.visit(methodSingle.retType);

    }

    @Override
    public void visit(Ast.Type type) {

    }

    @Override
    public void visit(Ast.Declare.DeclareSingle declareSingle) {

    }

    @Override
    public void visit(Ast.Stmt stmt) {

    }

    @Override
    public void visit(Ast.Stmt.Aload s) {

    }

    @Override
    public void visit(Ast.Stmt.Areturn s) {

    }

    @Override
    public void visit(Ast.Stmt.Astore s) {

    }

    @Override
    public void visit(Ast.Stmt.Goto s) {

    }

    @Override
    public void visit(Ast.Stmt.Iadd s) {

    }

    @Override
    public void visit(Ast.Stmt.Isub s) {

    }

    @Override
    public void visit(Ast.Stmt.Imul s) {

    }

    @Override
    public void visit(Ast.Stmt.Idiv s) {

    }

    @Override
    public void visit(Ast.Stmt.Fadd s) {

    }

    @Override
    public void visit(Ast.Stmt.Fsub s) {

    }

    @Override
    public void visit(Ast.Stmt.Fmul s) {

    }

    @Override
    public void visit(Ast.Stmt.Fdiv s) {

    }

    @Override
    public void visit(Ast.Stmt.Ificmplt s) {

    }

    @Override
    public void visit(Ast.Stmt.Ificmpgt s) {

    }

    @Override
    public void visit(Ast.Stmt.Iload s) {

    }

    @Override
    public void visit(Ast.Stmt.Invokevirtual s) {

    }

    @Override
    public void visit(Ast.Stmt.Ireturn s) {

    }

    @Override
    public void visit(Ast.Stmt.Istore s) {

    }

    @Override
    public void visit(Ast.Stmt.LabelJ s) {

    }

    @Override
    public void visit(Ast.Stmt.Ldc s) {

    }

    @Override
    public void visit(Ast.Stmt.Print s) {

    }

    @Override
    public void visit(Ast.Stmt.Freturn s) {

    }

    @Override
    public void visit(Ast.Stmt.Fstore s) {

    }

    @Override
    public void visit(Ast.Stmt.Fload s) {

    }
}
