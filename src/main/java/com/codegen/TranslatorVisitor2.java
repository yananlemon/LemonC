package com.codegen;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.ast.forcodegen.Aload;
import com.ast.forcodegen.Astore;
import com.ast.forcodegen.Fadd;
import com.ast.forcodegen.Fdiv;
import com.ast.forcodegen.Fload;
import com.ast.forcodegen.Fmul;
import com.ast.forcodegen.Freturn;
import com.ast.forcodegen.Fstore;
import com.ast.forcodegen.Fsub;
import com.ast.forcodegen.GoTo;
import com.ast.forcodegen.Iadd;
import com.ast.forcodegen.Idiv;
import com.ast.forcodegen.Ificmplt;
import com.ast.forcodegen.Iload;
import com.ast.forcodegen.Imul;
import com.ast.forcodegen.Invokestatic;
import com.ast.forcodegen.Ireturn;
import com.ast.forcodegen.Istore;
import com.ast.forcodegen.Isub;
import com.ast.forcodegen.LabelJ;
import com.ast.forcodegen.Ldc;
import com.ast.forcodegen.Type;
import com.ast.forparse.Add;
import com.ast.forparse.And;
import com.ast.forparse.Assign;
import com.ast.forparse.Block;
import com.ast.forparse.Bool;
import com.ast.forparse.Call;
import com.ast.forparse.Div;
import com.ast.forparse.Expr;
import com.ast.forparse.GT;
import com.ast.forparse.Id;
import com.ast.forparse.If;
import com.ast.forparse.Int;
import com.ast.forparse.LT;
import com.ast.forparse.Mul;
import com.ast.forparse.Num;
import com.ast.forparse.Or;
import com.ast.forparse.PrintNewLine;
import com.ast.forparse.Printf;
import com.ast.forparse.Return;
import com.ast.forparse.Stmt;
import com.ast.forparse.Str1;
import com.ast.forparse.Sub;
import com.ast.forparse.Void;
import com.ast.forparse.While;
import com.codegen.ast.Label;
import com.semantic.Semantic2;
import com.visitor.ISemanticVisitor;

public class TranslatorVisitor2 implements ISemanticVisitor {

	private String className;

	private com.ast.forparse.MainClass mainClass;

	private int index;
	private Hashtable<String, Integer> indexTable;

	private List<com.ast.forcodegen.Stmt> stmts = new ArrayList<com.ast.forcodegen.Stmt>();

	private com.ast.forcodegen.Type type;
	
	private com.ast.forparse.Type t;

	private com.ast.forcodegen.Declare declare;

	public com.ast.forcodegen.MainClass prog;

	List<com.ast.forcodegen.Method> ms = new ArrayList<com.ast.forcodegen.Method>();

	private int index1;
	
	private com.ast.forparse.Type rtTypeOfMethod;
	private Semantic2 semantic;
	public TranslatorVisitor2(Semantic2 semantic){
		this.semantic = semantic;
	}

	private void emit(com.ast.forcodegen.Stmt stmt){
		this.stmts.add(stmt);
	}

	@Override
	public void visit(Add obj) {
		this.visit(obj.left);
		this.visit(obj.right);
		
		if( rtTypeOfMethod instanceof Int)
			emit(new Iadd());
		else
			emit(new Fadd());
	}

	@Override
	public void visit(And obj) {

	}

	@Override
	public void visit(Assign obj) {
		int index = this.indexTable.get(obj.name);
		com.ast.forparse.Type t = this.semantic.getMethodVarTable().get(obj.name);
		this.visit(obj.expr);
		if( obj.type instanceof Int)
			emit(new Istore(index));
		else if( obj.type instanceof com.ast.forparse.Float)
			emit(new Fstore(index));
	}

	@Override
	public void visit(Block obj) {
		for( Stmt stmt : obj.stmts)
			this.visit(stmt);
	}

	@Override
	public void visit(Call obj) {
		System.out.println(obj);
		List<Type> at = new ArrayList<Type>();
		for( Expr expr : obj.inputParams ){
			this.visit(expr);
			at.add(this.type);
		}
		emit(new Invokestatic(obj.name,at,obj.returnType));
	}

	@Override
	public void visit(com.ast.forparse.Declare obj) {
		this.visit(obj.type);
		this.declare = new com.ast.forcodegen.Declare(this.type, obj.name);
		this.indexTable.put(obj.name, this.index++);
		index1 = this.indexTable.size();
	}

	@Override
	public void visit(Div obj) {
		this.visit(obj.left);
		this.visit(obj.right);
		if( rtTypeOfMethod instanceof Int)
			emit(new Idiv());
		else
			emit(new Fdiv());
	}

	@Override
	public void visit(com.ast.forparse.Float obj) {

	}

	@Override
	public void visit(Expr obj) {
		
		if( obj instanceof Add)
			this.visit((Add)obj);
		else if( obj instanceof Sub)
			this.visit((Sub)obj);
		else if( obj instanceof Sub)
			this.visit((Sub)obj);
		else if( obj instanceof Mul)
			this.visit((Mul)obj);
		else if( obj instanceof Div)
			this.visit((Div)obj);
		else if( obj instanceof Num)
			this.visit((Num)obj);
		else if( obj instanceof Call)
			this.visit((Call)obj);
		else if( obj instanceof Id)
			this.visit((Id)obj);
		else if( obj instanceof com.ast.forparse.Str1)
			this.visit((Str1)obj);
		else if( obj instanceof com.ast.forparse.And )
			this.visit((And)obj);
		else if( obj instanceof com.ast.forparse.Or )
			this.visit((Or)obj);
		else if( obj instanceof com.ast.forparse.LT )
			this.visit((LT)obj);
		else if( obj instanceof com.ast.forparse.GT )
			this.visit((GT)obj);
	}

	@Override
	public void visit(GT obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Id obj) {
		int index = this.indexTable.get(obj.name);
		com.ast.forparse.Type t = this.semantic.getMethodVarTable().get(obj.name);
		if( t instanceof com.ast.forparse.Float || obj.type instanceof com.ast.forparse.Float){
			emit(new Fload(index));
			//rtTypeOfMethod = new Double();
		}
		else{
			emit(new Iload(index));
			//rtTypeOfMethod = new Int();
		}
		this.t = obj.type;
	}

	@Override
	public void visit(If obj) {
		Label l = new Label();
		Label r = new Label();
		this.visit(obj.condition);
		emit(new Ldc(1));
		emit(new Ificmplt(l));
		this.visit(obj.thenStmt);
		emit(new GoTo(r));
		emit(new LabelJ(l));
		this.visit(obj.elseStmt);
		emit(new LabelJ(r));
	}

	@Override
	public void visit(com.ast.forparse.Int obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(LT obj) {
		Label t = new Label();
		Label r = new Label();
		this.visit(obj.left);
		this.visit(obj.right);
		emit(new Ificmplt(t));
		emit(new Ldc(0));
		emit(new GoTo(r));
		emit(new LabelJ(t));
		emit(new Ldc(1));
		emit(new LabelJ(r));
	}

	@Override
	public void visit(com.ast.forparse.MainClass obj) {
		this.className = obj.className;

		for(com.ast.forparse.Method method : obj.methods){
			this.visit(method);
		}
		this.prog = new com.ast.forcodegen.MainClass(this.className,this.ms);
	}

	@Override
	public void visit(com.ast.forparse.Method obj) {
		rtTypeOfMethod = obj.returnType;
		this.index = 0;
		this.indexTable = new Hashtable<>();
		this.visit(obj.returnType);
		Type t = this.type;
		List<com.ast.forcodegen.Declare> formals = new ArrayList<com.ast.forcodegen.Declare>();
		for (com.ast.forparse.Declare dec : obj.inputParams){
			this.visit(dec);
			formals.add(this.declare);
		}
		List<com.ast.forcodegen.Declare> locals = new ArrayList<com.ast.forcodegen.Declare>();
		for (com.ast.forparse.Declare dec : obj.localParams){
			this.visit(dec);
			locals.add(this.declare);
		}
		this.stmts = new ArrayList<com.ast.forcodegen.Stmt>();
		for(com.ast.forparse.Stmt stmt:obj.stmts){
			this.visit(stmt);
		}
		if( obj.returnType.toString().equals("@int"))
			emit(new Ireturn());
		else if( obj.returnType.toString().equals("@float"))
			emit(new Freturn());
		com.ast.forcodegen.Method method = new com.ast.forcodegen.Method(t, obj.name, 
				className, formals, locals, this.stmts, 0, this.index);
		this.ms.add(method);

	}

	@Override
	public void visit(Mul obj) {
		this.visit(obj.left);
		this.visit(obj.right);
		
		if( rtTypeOfMethod instanceof Int)
			emit(new Imul());
		else
			emit(new Fmul());
	}

	@Override
	public void visit(Num obj) {
		if( obj.t instanceof Int){
			emit(new Ldc(Integer.parseInt(obj.num.toString())));
			//this.t = new Int();
		}
		else if(  obj.t instanceof com.ast.forparse.Float){
			emit(new Ldc(java.lang.Double.parseDouble(obj.num.toString())));
			//this.t = new Double();
		}
	}

	@Override
	public void visit(Or obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Printf obj) {
		String f = obj.format;
		String[] array = f.split("%d|%f");
		for (int i = 0; i < array.length; i++) {
			this.visit(new com.ast.forparse.Str1(array[i], obj.lineNumber));
			emit(new Aload(index1 -1 ) );
			emit(new com.ast.forcodegen.Printf(new com.ast.forcodegen.Str(), array[i]));
			if( i+1 < obj.exprs.size() ){
				this.visit(obj.exprs.get(i+1));
				if( t instanceof Int)
					emit(new com.ast.forcodegen.Printf(new com.ast.forcodegen.Int(), null));
				else
					emit(new com.ast.forcodegen.Printf(new com.ast.forcodegen.Float(), null));
			}

		}
	}

	@Override
	public void visit(PrintNewLine obj) {
		emit(new com.ast.forcodegen.PrintNewLine());
	}

	@Override
	public void visit(Return obj) {
		if(obj.returnExpr instanceof com.ast.forparse.Add)
			this.visit((com.ast.forparse.Add)obj.returnExpr);
		else if(obj.returnExpr instanceof com.ast.forparse.Sub)
			this.visit((com.ast.forparse.Sub)obj.returnExpr);
		else if(obj.returnExpr instanceof com.ast.forparse.Mul)
			this.visit((com.ast.forparse.Mul)obj.returnExpr);
		else if(obj.returnExpr instanceof com.ast.forparse.Div)
			this.visit((com.ast.forparse.Div)obj.returnExpr);
		else if(obj.returnExpr instanceof com.ast.forparse.Num)
			this.visit((com.ast.forparse.Num)obj.returnExpr);
		else if(obj.returnExpr instanceof com.ast.forparse.Id)
			this.visit((com.ast.forparse.Id)obj.returnExpr);
	}

	@Override
	public void visit(com.ast.forparse.Stmt obj) {
		if( obj instanceof com.ast.forparse.Return)
			this.visit((Return)(obj));
		else if( obj instanceof Assign)
			this.visit((Assign)obj);
		else if( obj instanceof Printf)
			this.visit((Printf)obj);
		else if( obj instanceof While)
			this.visit((While)obj);
		else if( obj instanceof Block)
			this.visit((Block)obj);
		else if( obj instanceof PrintNewLine)
			this.visit((PrintNewLine)obj);
		else if( obj instanceof If)
			this.visit((If)obj);
	}

	@Override
	public void visit(com.ast.forparse.Str obj) {

	}

	@Override
	public void visit(Str1 obj) {
		this.type = new com.ast.forcodegen.Str();
		emit(new Ldc("\""+obj.value+"\""));
		emit(new Astore(index1++));
	}

	@Override
	public void visit(Sub obj) {
		this.visit(obj.left);
		this.visit(obj.right);
		
		if( rtTypeOfMethod instanceof Int)
			emit(new Isub());
		else
			emit(new Fsub());
	}

	@Override
	public void visit(com.ast.forparse.Type obj) {
		if( obj.toString().equals("@int"))
			this.type = new com.ast.forcodegen.Int();
		else if( obj.toString().equals("@float"))
			this.type = new com.ast.forcodegen.Float();
		//else
		//this.type = new com.ast.forcodegen.Void();
	}

	@Override
	public void visit(Void obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(While obj) {
		Label con = new Label();
		Label end = new Label();
		emit(new LabelJ(con));
		this.visit(obj.condition);
		emit(new Ldc(1));
		emit(new Ificmplt(end));
		this.visit(obj.body);
		emit(new GoTo(con));
		emit(new LabelJ(end));
	}

	@Override
	public void visit(Bool obj) {

	}

}
