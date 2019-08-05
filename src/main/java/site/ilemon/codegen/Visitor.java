package site.ilemon.codegen;

import site.ilemon.codegen.ast.Ast;

/**
 * Created by andy on 2019/7/31.
 */
public interface Visitor {

    void visit(Ast.Program.ProgramSingle program);
    void visit(Ast.MainClass.MainClassSingle mainClassSingle);
    void visit(Ast.Method.MethodSingle methodSingle);
    void visit(Ast.Type type);
    void visit(Ast.Declare.DeclareSingle declareSingle);
    
    // Stmt
    void visit(Ast.Stmt stmt);
    void visit(Ast.Stmt.Aload s);
    void visit(Ast.Stmt.Areturn s);
    void visit(Ast.Stmt.Astore s);
    void visit(Ast.Stmt.Goto s);
    void visit(Ast.Stmt.Iadd s);
    void visit(Ast.Stmt.Isub s);
    void visit(Ast.Stmt.Imul s);
    void visit(Ast.Stmt.Idiv s);
    void visit(Ast.Stmt.Fadd s);
    void visit(Ast.Stmt.Fsub s);
    void visit(Ast.Stmt.Fmul s);
    void visit(Ast.Stmt.Fdiv s);
    void visit(Ast.Stmt.Ificmplt s);
    void visit(Ast.Stmt.Ificmpgt s);
    void visit(Ast.Stmt.Iload s);
    void visit(Ast.Stmt.Invokevirtual s);
    void visit(Ast.Stmt.Ireturn s);
    void visit(Ast.Stmt.Istore s);
    void visit(Ast.Stmt.LabelJ s);
    void visit(Ast.Stmt.Ldc s);
    void visit(Ast.Stmt.Print s);
    void visit(Ast.Stmt.Freturn s);
    void visit(Ast.Stmt.Fstore s);
    void visit(Ast.Stmt.Fload s);

}
