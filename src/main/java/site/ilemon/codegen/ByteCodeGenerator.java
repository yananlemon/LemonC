package site.ilemon.codegen;

import site.ilemon.codegen.ast.Ast;

import java.io.IOException;

public class ByteCodeGenerator implements Visitor {

    private String currClassId;

    private java.io.BufferedWriter writer;

    private void writeln(String s)
    {
        write(s + "\n");
    }

    private void write(String s)
    {
        try
        {
            this.writer.write(s);
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void iwriteln(String s)
    {
        write("    " + s + "\n");
    }


    @Override
    public void visit(Ast.Program.ProgramSingle program) {
        this.visit(program.mainClass);
    }

    @Override
    public void visit(Ast.MainClass.MainClassSingle mainClassSingle) {
        this.currClassId = mainClassSingle.id;
        try {
            this.writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(mainClassSingle.id + ".il")));
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        this.writeln("; This file is automatically generated by the compiler");
        this.writeln("; Do Not Modify!\n");

        this.writeln(".class public " + mainClassSingle.id);
        this.writeln(".super java/lang/Object");
        for( int i = 0; i < mainClassSingle.methods.size(); i++ ){
            this.visit(mainClassSingle.methods.get(i));
        }
        //this.iwriteln("return");
        //this.writeln(".end method");

        try
        {
            this.writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle methodSingle) {
        if( methodSingle.id.equals("main")){
            this.writeln(".method public static main([Ljava/lang/String;)V");
            this.writeln(".limit stack 4096");
            this.writeln(".limit locals 2000");
            for( int i = 0; i < methodSingle.stms.size(); i++ ){
                this.visit(methodSingle.stms.get(i));
            }
            this.writeln("return");
        }else{
            if( methodSingle.formals !=null && methodSingle.formals.size() > 0 ){
                this.write(".method static " + methodSingle.id + "(");
                for( int i = 0; i< methodSingle.formals.size(); i++){
                    if( methodSingle.formals.get(i).type instanceof Ast.Type.Int || methodSingle.retType instanceof Ast.Type.Bool)
                        this.write("I");
                    else
                        this.write("F");
                }
                if(methodSingle.retType instanceof Ast.Type.Int || methodSingle.retType instanceof Ast.Type.Bool)
                    this.write(")I");
                else
                    this.write(")F");
            }else{
                if(methodSingle.retType instanceof Ast.Type.Int || methodSingle.retType instanceof Ast.Type.Bool)
                    this.write(".method static " + methodSingle.id + "()I");
                else
                    this.write(".method static " + methodSingle.id + "()F");

            }
            this.writeln("");
            this.writeln(".limit stack 4096");
            this.writeln(".limit locals 1000");// 暂时写成1000
            for( int i = 0; i < methodSingle.stms.size(); i++ ){
                this.visit(methodSingle.stms.get(i));
            }
        }
        this.writeln(".end method");
    }

    @Override
    public void visit(Ast.Type type) {

    }

    @Override
    public void visit(Ast.Declare.DeclareSingle declareSingle) {

    }

    @Override
    public void visit(Ast.Stmt.T stmt) {
        if( stmt instanceof Ast.Stmt.Aload )
            this.visit((Ast.Stmt.Aload)stmt);
        else if( stmt instanceof Ast.Stmt.Areturn )
            this.visit((Ast.Stmt.Areturn)stmt);
        else if( stmt instanceof Ast.Stmt.Astore )
            this.visit((Ast.Stmt.Astore)stmt);
        else if( stmt instanceof Ast.Stmt.Goto )
            this.visit((Ast.Stmt.Goto)stmt);

        else if( stmt instanceof Ast.Stmt.Iadd )
            this.visit((Ast.Stmt.Iadd)stmt);
        else if( stmt instanceof Ast.Stmt.Isub )
            this.visit((Ast.Stmt.Isub)stmt);
        else if( stmt instanceof Ast.Stmt.Imul )
            this.visit((Ast.Stmt.Imul)stmt);
        else if( stmt instanceof Ast.Stmt.Idiv )
            this.visit((Ast.Stmt.Idiv)stmt);


        else if( stmt instanceof Ast.Stmt.Fadd )
            this.visit((Ast.Stmt.Fadd)stmt);
        else if( stmt instanceof Ast.Stmt.Fsub )
            this.visit((Ast.Stmt.Fsub)stmt);
        else if( stmt instanceof Ast.Stmt.Fmul )
            this.visit((Ast.Stmt.Fmul)stmt);
        else if( stmt instanceof Ast.Stmt.Fdiv )
            this.visit((Ast.Stmt.Fdiv)stmt);
        else if( stmt instanceof Ast.Stmt.Fload )
            this.visit((Ast.Stmt.Fload)stmt);
        else if( stmt instanceof Ast.Stmt.Fdiv)
            this.visit((Ast.Stmt.Fdiv)stmt);
        else if( stmt instanceof Ast.Stmt.Fadd )
            this.visit((Ast.Stmt.Fadd)stmt);
        else if( stmt instanceof Ast.Stmt.Freturn )
            this.visit((Ast.Stmt.Freturn)stmt);
        else if( stmt instanceof Ast.Stmt.Fstore )
            this.visit((Ast.Stmt.Fstore)stmt);

        else if( stmt instanceof Ast.Stmt.Ificmpgt )
            this.visit((Ast.Stmt.Ificmpgt)stmt);
        else if( stmt instanceof Ast.Stmt.Ificmplt )
            this.visit((Ast.Stmt.Ificmplt)stmt);

        else if( stmt instanceof Ast.Stmt.Iload )
            this.visit((Ast.Stmt.Iload)stmt);
        else if( stmt instanceof Ast.Stmt.Ireturn )
            this.visit((Ast.Stmt.Ireturn)stmt);
        else if( stmt instanceof Ast.Stmt.Istore )
            this.visit((Ast.Stmt.Istore)stmt);

        else if( stmt instanceof Ast.Stmt.LabelJ )
            this.visit((Ast.Stmt.LabelJ)stmt);
        else if( stmt instanceof Ast.Stmt.Ldc )
            this.visit((Ast.Stmt.Ldc)stmt);

        else if( stmt instanceof Ast.Stmt.Printf )
            this.visit((Ast.Stmt.Printf)stmt);
        else if( stmt instanceof Ast.Stmt.PrintLine )
            this.visit((Ast.Stmt.PrintLine)stmt);

        else if( stmt instanceof Ast.Stmt.Invokevirtual )
            this.visit((Ast.Stmt.Invokevirtual)stmt);


    }

    @Override
    public void visit(Ast.Stmt.Aload s) {
        this.iwriteln("aload " + s.index);
    }

    @Override
    public void visit(Ast.Stmt.Areturn s) {
        this.iwriteln("areturn " );
    }

    @Override
    public void visit(Ast.Stmt.Astore s) {
        this.iwriteln("astore " + s.index);
    }

    @Override
    public void visit(Ast.Stmt.Goto s) {
        this.iwriteln("goto " + s.l.toString());
    }

    @Override
    public void visit(Ast.Stmt.Iadd s) {
        this.iwriteln("iadd");
    }

    @Override
    public void visit(Ast.Stmt.Isub s) {
        this.iwriteln("isub");
    }

    @Override
    public void visit(Ast.Stmt.Imul s) {
        this.iwriteln("imul");
    }

    @Override
    public void visit(Ast.Stmt.Idiv s) {
        this.iwriteln("idiv");
    }

    @Override
    public void visit(Ast.Stmt.Fadd s) {
        this.iwriteln("fadd");
    }

    @Override
    public void visit(Ast.Stmt.Fsub s) {
        this.iwriteln("fsub");
    }

    @Override
    public void visit(Ast.Stmt.Fmul s) {
        this.iwriteln("fmul");
    }

    @Override
    public void visit(Ast.Stmt.Fdiv s) {
        this.iwriteln("fdiv");
    }

    @Override
    public void visit(Ast.Stmt.Ificmplt s) {
        this.iwriteln("if_icmple " + s.l.toString());
    }

    @Override
    public void visit(Ast.Stmt.Ificmpgt s) {
        this.iwriteln("if_icmpge " + s.l.toString());
    }

    @Override
    public void visit(Ast.Stmt.Iload s) {
        this.iwriteln("iload " + s.index);
    }

    @Override
    public void visit(Ast.Stmt.Invokevirtual s) {
        this.write("    invokestatic " + currClassId + "/" + s.name + "(");
        for( int i = 0; i< s.at.size(); i++){
            if( s.at.get(i) instanceof Ast.Type.Int){
                this.write("I");
            }else{
                this.write("F");
            }
        }
        if( s.rt != null && s.rt.toString().equals("@float"))
            this.write(")F");
        else
            this.write(")I");

        this.writeln("");
    }

    @Override
    public void visit(Ast.Stmt.Ireturn s) {
        this.iwriteln("ireturn");
    }

    @Override
    public void visit(Ast.Stmt.Istore s) {
        this.iwriteln("istore " + s.index);
    }

    @Override
    public void visit(Ast.Stmt.LabelJ s) {
        this.writeln(s.label.toString() + ":");
    }

    @Override
    public void visit(Ast.Stmt.Ldc s) {
        this.iwriteln("ldc " + s.i);
    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {
        if( obj.exprType.toString().equals("@int")){
            this.iwriteln("getstatic java/lang/System/out Ljava/io/PrintStream;");
            this.iwriteln("swap");
            this.iwriteln("invokevirtual java/io/PrintStream/print(I)V");
        }
        else if( obj.exprType.toString().equals("@float")){
            this.iwriteln("getstatic java/lang/System/out Ljava/io/PrintStream;");
            this.iwriteln("swap");
            this.iwriteln("invokevirtual java/io/PrintStream/print(F)V");
        }
        else if( obj.exprType.toString().equals("@string")){
            this.iwriteln("getstatic java/lang/System/out Ljava/io/PrintStream;");
            this.iwriteln("swap");
            this.iwriteln("invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V");
        }
    }

    @Override
    public void visit(Ast.Stmt.PrintLine obj) {
        /*this.iwriteln("ldc \"\\n\"");
        this.iwriteln("astore 100 ");
        this.iwriteln("aload 100 ");*/
        this.iwriteln("getstatic java/lang/System/out Ljava/io/PrintStream;");
        this.iwriteln("swap");
        this.iwriteln("invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V");
    }

    @Override
    public void visit(Ast.Stmt.Freturn s) {
        this.iwriteln("freturn");
    }

    @Override
    public void visit(Ast.Stmt.Fstore s) {
        this.iwriteln("fstore " + s.index);
    }

    @Override
    public void visit(Ast.Stmt.Fload s) {
        this.iwriteln("fload " + s.index);
    }

    @Override
    public void visit(Ast.Type.T obj) {
        if( obj instanceof Ast.Type.Int)
            this.visit((Ast.Type.Int)obj);
        else if( obj instanceof Ast.Type.Float)
            this.visit((Ast.Type.Int)obj);
        else if( obj instanceof Ast.Type.Bool)
            this.visit((Ast.Type.Bool)obj);
    }

    @Override
    public void visit(Ast.Type.Int obj) {
        this.write("I");
    }

    @Override
    public void visit(Ast.Type.Float obj) {
        this.write("F");
    }

    @Override
    public void visit(Ast.Type.Str obj) {

    }

    @Override
    public void visit(Ast.Type.Bool obj) {

    }
}