package com.semantic;

import com.parser.Ast;
import com.parser.Ast.*;

/**
 * 
 * @author andy
 * @version 1.0
 */
public interface Visitor
{
	 // Type
    default void visit(Type.T t)
    {
        if (t instanceof Ast.Type.Int)
            this.visit(((Ast.Type.Int) t));
        else if(t instanceof Ast.Type.Void)
        	this.visit(((Ast.Type.Void) t));
    }

    void visit(Type.Int t);
    void visit(Type.Void t);

    // Dec
    default void visit(Declare.T d)
    {
        this.visit(((Declare.DeclareSingle) d));
    }

    void visit(Declare.DeclareSingle d);

    // Exp
    default void visit(Exp.T e)
    {
        if (e instanceof Ast.Exp.Add)
            this.visit(((Ast.Exp.Add) e));
        else if (e instanceof Ast.Exp.And)
            this.visit(((Ast.Exp.And) e));
       
       
        else if (e instanceof Ast.Exp.Id)
            this.visit(((Ast.Exp.Id) e));
        else if (e instanceof Ast.Exp.LT)
            this.visit(((Ast.Exp.LT) e));
        else if (e instanceof Ast.Exp.GT)
            this.visit(((Ast.Exp.GT) e));
       
        else if (e instanceof Ast.Exp.Not)
            this.visit(((Ast.Exp.Not) e));
        else if (e instanceof Ast.Exp.Num)
            this.visit(((Ast.Exp.Num) e));
        else if (e instanceof Ast.Exp.Sub)
            this.visit(((Ast.Exp.Sub) e));
       
        else if (e instanceof Ast.Exp.Mul)
            this.visit(((Ast.Exp.Mul) e));
        else if (e instanceof Ast.Exp.Str)
            this.visit(((Ast.Exp.Str) e));
    }

    
    void visit(Exp.Add e);

    void visit(Exp.And e);

    void visit(Exp.Call e);


    void visit(Exp.Id e);

    void visit(Exp.LT e);
    
    void visit(Exp.GT e);

    void visit(Exp.Not e);

    void visit(Exp.Num e);

    void visit(Exp.Sub e);

    void visit(Exp.Mul e);
    
    void visit(Exp.Str e);

    // Stm
    default void visit(Stmt.T s)
    {
        if (s instanceof Ast.Stmt.Assign)
            this.visit(((Ast.Stmt.Assign) s));
        else if (s instanceof Ast.Stmt.Block)
            this.visit(((Ast.Stmt.Block) s));
        else if (s instanceof Ast.Stmt.If)
            this.visit(((Ast.Stmt.If) s));
        else if (s instanceof Ast.Stmt.Printf)
            this.visit(((Ast.Stmt.Printf) s));
        else if (s instanceof Ast.Stmt.PrintNewLine)
            this.visit(((Ast.Stmt.PrintNewLine) s));
        else if (s instanceof Ast.Stmt.While)
            this.visit(((Ast.Stmt.While) s));
    }

    void visit(Stmt.Assign s);

    void visit(Stmt.Block s);

    void visit(Stmt.If s);

    void visit(Stmt.Printf s);
    
    void visit(Stmt.PrintNewLine s);

    void visit(Stmt.While s);

    // Method
    default void visit(Method.T m)
    {
        this.visit(((Method.MethodSingle) m));
    }

    void visit(Method.MethodSingle m);

    void visit(Ast.MainClass c);

    default void visit(Ast.MainClass.T c)
    {
        this.visit(((MainClass.MainClassSingle) c));
    }

    void visit(MainClass.MainClassSingle c);

    // Program
    default void visit(Program.T p)
    {
        this.visit(((Program.ProgramSingle) p));
    }

    void visit(Program.ProgramSingle p);
}
