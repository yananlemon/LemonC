package com.codegen.ast;

/**
 * Created by Mengxu on 2017/1/17.
 */
public interface Visitor
{
    // Type
    default void visit(Ast.Type.T t)
    {
        if (t instanceof Ast.Type.ClassType)
            this.visit(((Ast.Type.ClassType) t));
        else if( t instanceof Ast.Type.Str )
        	this.visit((Ast.Type.Str)t);
        else this.visit(((Ast.Type.Int) t));
    }

    void visit(Ast.Type.ClassType t);

    void visit(Ast.Type.Int t);
    
    void visit(Ast.Type.Str t);

    // Dec
    void visit(Ast.Dec.DecSingle d);

    // Stm
    default void visit(Ast.Stm.T s)
    {
    	System.out.println(s.toString());
        if (s instanceof Ast.Stm.Aload)
            this.visit(((Ast.Stm.Aload) s));
        else if (s instanceof Ast.Stm.Areturn)
            this.visit(((Ast.Stm.Areturn) s));
        else if (s instanceof Ast.Stm.Astore)
            this.visit(((Ast.Stm.Astore) s));
        else if (s instanceof Ast.Stm.Goto)
            this.visit(((Ast.Stm.Goto) s));
        else if (s instanceof Ast.Stm.Getfield)
            this.visit(((Ast.Stm.Getfield) s));
        else if (s instanceof Ast.Stm.Iadd)
            this.visit(((Ast.Stm.Iadd) s));
        else if (s instanceof Ast.Stm.Ificmplt)
            this.visit(((Ast.Stm.Ificmplt) s));
        else if (s instanceof Ast.Stm.Ificmpgt)
            this.visit(((Ast.Stm.Ificmpgt) s));
        else if (s instanceof Ast.Stm.Iload)
            this.visit(((Ast.Stm.Iload) s));
        else if (s instanceof Ast.Stm.Imul)
            this.visit(((Ast.Stm.Imul) s));
        else if (s instanceof Ast.Stm.Invokevirtual)
            this.visit(((Ast.Stm.Invokevirtual) s));
        else if (s instanceof Ast.Stm.Ireturn)
            this.visit(((Ast.Stm.Ireturn) s));
        else if (s instanceof Ast.Stm.Istore)
            this.visit(((Ast.Stm.Istore) s));
        else if (s instanceof Ast.Stm.Isub)
            this.visit(((Ast.Stm.Isub) s));
        else if (s instanceof Ast.Stm.LabelJ)
            this.visit(((Ast.Stm.LabelJ) s));
        else if (s instanceof Ast.Stm.Ldc)
            this.visit(((Ast.Stm.Ldc) s));
        else if (s instanceof Ast.Stm.New)
            this.visit(((Ast.Stm.New) s));
        else if (s instanceof Ast.Stm.Printf)
            this.visit(((Ast.Stm.Printf) s));
        else if (s instanceof Ast.Stm.PrintNewLine)
            this.visit(((Ast.Stm.PrintNewLine) s));
        else  if (s instanceof Ast.Stm.Putfield)
            this.visit(((Ast.Stm.Putfield) s));
        /*else  if (s instanceof Ast.Stm.Putfield)
            this.visit(((Ast.Stm.Putfield) s));*/
    }

    void visit(Ast.Stm.Aload s);

    void visit(Ast.Stm.Areturn s);

    void visit(Ast.Stm.Astore s);

    void visit(Ast.Stm.Goto s);

    void visit(Ast.Stm.Getfield s);

    void visit(Ast.Stm.Iadd s);

    void visit(Ast.Stm.Ificmplt s);
    
    void visit(Ast.Stm.Ificmpgt s);

    void visit(Ast.Stm.Iload s);

    void visit(Ast.Stm.Imul s);

    void visit(Ast.Stm.Invokevirtual s);

    void visit(Ast.Stm.Ireturn s);

    void visit(Ast.Stm.Istore s);

    void visit(Ast.Stm.Isub s);

    void visit(Ast.Stm.LabelJ s);

    void visit(Ast.Stm.Ldc s);

    void visit(Ast.Stm.New s);

    void visit(Ast.Stm.Printf s);
    
    void visit(Ast.Stm.PrintNewLine s);

    void visit(Ast.Stm.Putfield s);

    void visit(Ast.Method.MethodSingle m);

    void visit(Ast.MainClass.MainClassSingle c);

    void visit(Ast.Class.ClassSingle c);

    void visit(Ast.Program.ProgramSingle p);
}
