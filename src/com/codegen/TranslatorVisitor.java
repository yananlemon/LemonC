package com.codegen;

import java.util.Hashtable;
import java.util.LinkedList;

import com.codegen.ast.Ast.Dec;
import com.codegen.ast.Ast.MainClass;
import com.codegen.ast.Ast.Method;
import com.codegen.ast.Ast.Program;
import com.codegen.ast.Ast.Stm;
import com.codegen.ast.Ast.Type;
import com.codegen.ast.Label;
import com.parser.Ast;
import com.parser.Ast.Stmt.PrintNewLine;
import com.parser.Ast.Type.Void;


public class TranslatorVisitor implements com.semantic.Visitor
{
    private String classId;
    private int index;
    private Hashtable<String, Integer> indexTable;
    private Type.T type;
    private Dec.DecSingle dec;
    private LinkedList<Stm.T> stms;
    private Method.MethodSingle method;
    private com.codegen.ast.Ast.Class.ClassSingle classs;
    private MainClass.MainClassSingle mainClass;
    public Program.ProgramSingle prog;

    public TranslatorVisitor()
    {
        this.classId = null;
        this.indexTable = null;
        this.type = null;
        this.dec = null;
        this.stms = new LinkedList<>();
        this.method = null;
        this.classId = null;
        this.mainClass = null;
        this.classs = null;
        this.prog = null;
    }

    private void emit(Stm.T s)
    {
        this.stms.add(s);
    }

    public void visit(Ast.Type.Boolean t)
    {
        this.type = new Type.Int();
    }


    @Override
    public void visit(Ast.Type.Int t)
    {
        this.type = new Type.Int();
    }
    
	int index1;


    @Override
    public void visit(Ast.Declare.DeclareSingle d)
    {
        this.visit(d.type);
        this.dec = new Dec.DecSingle(this.type, d.id);
        if (this.indexTable != null) // if it is field
        this.indexTable.put(d.id, index++);
        index1 = this.indexTable.size() + 1;
    }

    @Override
    public void visit(Ast.Exp.Add e)
    {
    	this.type = new Type.Int();
        this.visit(e.left);
        this.visit(e.right);
        emit(new Stm.Iadd());
    }

    @Override
    public void visit(Ast.Exp.And e)
    {
        Label f = new Label();
        Label r = new Label();
        this.visit(e.left);
        emit(new Stm.Ldc(1));
        emit(new Stm.Ificmplt(f));
        this.visit(e.right);
        emit(new Stm.Ldc(1));
        emit(new Stm.Ificmplt(f));
        emit(new Stm.Ldc(1));
        emit(new Stm.Goto(r));
        emit(new Stm.LabelJ(f));
        emit(new Stm.Ldc(0));
        emit(new Stm.LabelJ(r));
    }

    @Override
    public void visit(Ast.Exp.Call e)
    {
        this.visit(e.exp);
        e.args.forEach(this::visit);
        this.visit(e.rt);
        Type.T rt = this.type;
        LinkedList<Type.T> at = new LinkedList<>();
        e.at.forEach(a ->
        {
            this.visit(a);
            at.add(this.type);
        });
        emit(new Stm.Invokevirtual(e.id, e.type, at, rt));
    }

    @Override
    public void visit(Ast.Exp.Id e)
    {
        if (e.isField)
        {
            emit(new Stm.Aload(0));
            Ast.Type.T type = e.type;
           
        } else
        {
            int index = this.indexTable.get(e.name);
            if ( e.type instanceof Ast.Type.Str){
            	this.type = new Type.Str();
            	emit(new Stm.Aload(index));
            }
            else{
            	this.type = new Type.Int();
            	emit(new Stm.Iload(index));
            }
        }
    }

    @Override
    public void visit(Ast.Exp.LT e)
    {
        Label t = new Label();
        Label r = new Label();
        this.visit(e.left);
        this.visit(e.right);
        emit(new Stm.Ificmplt(t));
        emit(new Stm.Ldc(0));
        emit(new Stm.Goto(r));
        emit(new Stm.LabelJ(t));
        emit(new Stm.Ldc(1));
        emit(new Stm.LabelJ(r));
    }

    @Override
    public void visit(Ast.Exp.Not e)
    {
        Label f = new Label();
        Label r = new Label();
        this.visit(e.exp);
        emit(new Stm.Ldc(1));
        emit(new Stm.Ificmplt(f));
        emit(new Stm.Ldc(1));
        emit(new Stm.Goto(r));
        emit(new Stm.LabelJ(f));
        emit(new Stm.Ldc(0));
        emit(new Stm.LabelJ(r));
    }

    @Override
    public void visit(Ast.Exp.Num e)
    {
    	this.type = new Type.Int();
    	emit(new Stm.Ldc(e.num));
    		 
    }

    @Override
    public void visit(Ast.Exp.Sub e)
    {
    	this.type = new Type.Int();
        this.visit(e.left);
        this.visit(e.right);
        emit(new Stm.Isub());
    }

    @Override
    public void visit(Ast.Exp.Mul e)
    {
    	this.type = new Type.Int();
        this.visit(e.left);
        this.visit(e.right);
        emit(new Stm.Imul());
    }

    @Override
    public void visit(Ast.Stmt.Assign s)
    {
        try
        {
            int index = this.indexTable.get(s.name);
            this.visit(s.expr);
            if(s.type instanceof Ast.Type.Str)
            	emit(new Stm.Astore(index));
            else emit(new Stm.Istore(index));
        } catch (NullPointerException e)
        {
            emit(new Stm.Aload(0));
            this.visit(s.expr);
        }
    }

    @Override
    public void visit(Ast.Stmt.Block s)
    {
        s.stmts.forEach(this::visit);
    }

    @Override
    public void visit(Ast.Stmt.If s)
    {
        Label l = new Label();
        Label r = new Label();
        this.visit(s.condition);
        emit(new Stm.Ldc(1));
        emit(new Stm.Ificmplt(l));
        this.visit(s.ifStmt);
        emit(new Stm.Goto(r));
        emit(new Stm.LabelJ(l));
        this.visit(s.elseStmt);
        emit(new Stm.LabelJ(r));
    }

    @Override
    public void visit(Ast.Stmt.Printf s)
    {
    	String f = s.format;
    	String[] array = f.split("%d");
    	for (int i = 0; i < array.length; i++) {
    		this.visit(new Ast.Exp.Str(array[i], s.lineNumber));
    		emit(new Stm.Aload(index1 -1 ) );
    		emit(new Stm.Printf(new Type.Str(), array[i]));
    		if( i+1 < s.exprs.size() ){
    			this.visit(s.exprs.get(i+1));
    			//emit( new Stm.Iload(index) );
    			emit(new Stm.Printf(new Type.Int(), null));
    		}
    		
		}
    }

    @Override
    public void visit(Ast.Stmt.While s)
    {
        Label con = new Label();
        Label end = new Label();
        emit(new Stm.LabelJ(con));
        this.visit(s.condition);
        emit(new Stm.Ldc(1));
        emit(new Stm.Ificmplt(end));
        this.visit(s.body);
        emit(new Stm.Goto(con));
        emit(new Stm.LabelJ(end));
    }
    boolean returnFalg = false;
    @Override
    public void visit(Ast.Method.MethodSingle m){
        this.index = 1;
        this.indexTable = new Hashtable<>();
        this.visit(m.retType);
        Type.T _retType = this.type;

        LinkedList<Dec.DecSingle> _formals = new LinkedList<>();
        m.formals.forEach(f ->
        {
            this.visit(f);
            _formals.add(this.dec);
        });

        LinkedList<Dec.DecSingle> _locals = new LinkedList<>();
        m.locals.forEach(l ->
        {
            this.visit(l);
            _locals.add(this.dec);
        });
        this.stms = new LinkedList<>();
        m.stms.forEach(this::visit);

        returnFalg = true;
        this.visit(m.retExp);
        /*if (m.retType instanceof Ast.Type.ClassType)
            emit(new Stm.Areturn());
        else*/ 
        	emit(new Stm.Ireturn());

        this.method = new Method.MethodSingle(_retType, m.name, this.classId,
                _formals, _locals, this.stms, 0, this.index);
    }
    
    @Override
    public void visit(Ast.MainClass.MainClassSingle c)
    {
    	this.classId = c.className;
    	
    	for (int i = 0; i < c.methods.size(); i++) {
			this.visit(c.methods.get(i));
		}
    }

    @Override
    public void visit(Ast.Program.ProgramSingle p)
    {
        this.visit(p.mainClass);
        this.mainClass = new MainClass.MainClassSingle(((Ast.MainClass.MainClassSingle)p.mainClass).className, this.stms,this.indexTable.size());
        this.prog = new Program.ProgramSingle(this.mainClass, null);
    }

	@Override
	public void visit(Ast.Exp.GT e) {
		Label t = new Label();
        Label r = new Label();
        this.visit(e.left);
        this.visit(e.right);
        emit(new Stm.Ificmpgt(t));
        emit(new Stm.Ldc(0));
        emit(new Stm.Goto(r));
        emit(new Stm.LabelJ(t));
        emit(new Stm.Ldc(1));
        emit(new Stm.LabelJ(r));
	}

	@Override
	public void visit(Ast.Exp.Str e) {
		this.type = new Type.Str();
		emit(new Stm.Ldc("\""+e.value+"\""));
		emit(new Stm.Astore(index1++));
	}

	@Override
	public void visit(com.parser.Ast.MainClass c) {
		//if( c instanceof com.parser.Ast.MainClass )
		//this.visit();
		System.out.println(c);
	}
	
	@Override
	public void visit(Void t) {
		this.type = new Type.Void();
	}

	@Override
	public void visit(PrintNewLine s) {
		emit(new Stm.PrintNewLine());
	}
}
