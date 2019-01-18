package site.ilemon.codegen;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import site.ilemon.ast.forcodegen.Aload;
import site.ilemon.ast.forcodegen.Astore;
import site.ilemon.ast.forcodegen.Fadd;
import site.ilemon.ast.forcodegen.Fdiv;
import site.ilemon.ast.forcodegen.Fload;
import site.ilemon.ast.forcodegen.Fmul;
import site.ilemon.ast.forcodegen.Freturn;
import site.ilemon.ast.forcodegen.Fstore;
import site.ilemon.ast.forcodegen.Fsub;
import site.ilemon.ast.forcodegen.GoTo;
import site.ilemon.ast.forcodegen.Iadd;
import site.ilemon.ast.forcodegen.Idiv;
import site.ilemon.ast.forcodegen.Ificmpgt;
import site.ilemon.ast.forcodegen.Ificmplt;
import site.ilemon.ast.forcodegen.Iload;
import site.ilemon.ast.forcodegen.Imul;
import site.ilemon.ast.forcodegen.Invokestatic;
import site.ilemon.ast.forcodegen.Ireturn;
import site.ilemon.ast.forcodegen.Istore;
import site.ilemon.ast.forcodegen.Isub;
import site.ilemon.ast.forcodegen.LabelJ;
import site.ilemon.ast.forcodegen.Ldc;
import site.ilemon.ast.forcodegen.Type;
import site.ilemon.ast.forparse.Add;
import site.ilemon.ast.forparse.And;
import site.ilemon.ast.forparse.Assign;
import site.ilemon.ast.forparse.Block;
import site.ilemon.ast.forparse.Bool;
import site.ilemon.ast.forparse.Call;
import site.ilemon.ast.forparse.Div;
import site.ilemon.ast.forparse.Expr;
import site.ilemon.ast.forparse.GT;
import site.ilemon.ast.forparse.Id;
import site.ilemon.ast.forparse.If;
import site.ilemon.ast.forparse.Int;
import site.ilemon.ast.forparse.LT;
import site.ilemon.ast.forparse.Mul;
import site.ilemon.ast.forparse.Num;
import site.ilemon.ast.forparse.Or;
import site.ilemon.ast.forparse.PrintNewLine;
import site.ilemon.ast.forparse.Printf;
import site.ilemon.ast.forparse.Return;
import site.ilemon.ast.forparse.Stmt;
import site.ilemon.ast.forparse.Str;
import site.ilemon.ast.forparse.Str1;
import site.ilemon.ast.forparse.Sub;
import site.ilemon.ast.forparse.While;
import site.ilemon.codegen.ast.Label;
import site.ilemon.semantic.Semantic;
import site.ilemon.visitor.ISemanticVisitor;

public class TranslatorVisitor implements ISemanticVisitor {

	private String className;

	private int index;
	private Hashtable<String, Integer> indexTable;

	private List<site.ilemon.ast.forcodegen.Stmt> stmts = new ArrayList<site.ilemon.ast.forcodegen.Stmt>();

	private site.ilemon.ast.forcodegen.Type type;

	private site.ilemon.ast.forparse.Type t;

	private site.ilemon.ast.forcodegen.Declare declare;

	public site.ilemon.ast.forcodegen.MainClass prog;

	List<site.ilemon.ast.forcodegen.Method> ms = new ArrayList<site.ilemon.ast.forcodegen.Method>();

	private int index1;

	private site.ilemon.ast.forparse.Type rtTypeOfMethod;
	private Semantic semantic;
	public TranslatorVisitor(Semantic semantic){
		this.semantic = semantic;
	}

	private void emit(site.ilemon.ast.forcodegen.Stmt stmt){
		this.stmts.add(stmt);
	}

	@Override
	public void visit(Add obj) {
		this.visit(obj.left);
		site.ilemon.ast.forparse.Type leftType = this.t;
		this.visit(obj.right);
		site.ilemon.ast.forparse.Type rightType = this.t;
		if( leftType.toString().equals(rightType.toString())){
			if( leftType.toString().equals("@int")){
				emit(new Iadd());
			}else{
				emit(new Fadd());
			}
		}else{
			//er
		}
	}

	@Override
	public void visit(And obj) {

	}

	@Override
	public void visit(Assign obj) {
		int index = this.indexTable.get(obj.name);
		this.visit(obj.expr);
		if( obj.type instanceof Int)
			emit(new Istore(index));
		else if( obj.type instanceof site.ilemon.ast.forparse.Float)
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
	public void visit(site.ilemon.ast.forparse.Declare obj) {
		this.visit(obj.type);
		this.declare = new site.ilemon.ast.forcodegen.Declare(this.type, obj.name);
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
	public void visit(site.ilemon.ast.forparse.Float obj) {

	}

	@Override
	public void visit(Expr obj) {

		if( obj instanceof Add)
			this.visit((Add)obj);
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
		else if( obj instanceof Str1)
			this.visit((Str1)obj);
		else if( obj instanceof And )
			this.visit((And)obj);
		else if( obj instanceof Or )
			this.visit((Or)obj);
		else if( obj instanceof LT )
			this.visit((LT)obj);
		else if( obj instanceof GT )
			this.visit((GT)obj);
	}

	

	@Override
	public void visit(Id obj) {
		int index = this.indexTable.get(obj.name);
		site.ilemon.ast.forparse.Type t = this.semantic.getMethodVarTable().get(obj.name);
		if( t instanceof site.ilemon.ast.forparse.Float || obj.type instanceof site.ilemon.ast.forparse.Float){
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
	public void visit(site.ilemon.ast.forparse.Int obj) {

	}


	@Override
	public void visit(site.ilemon.ast.forparse.MainClass obj) {
		this.className = obj.className;

		for(site.ilemon.ast.forparse.Method method : obj.methods){
			this.visit(method);
		}
		this.prog = new site.ilemon.ast.forcodegen.MainClass(this.className,this.ms);
	}

	@Override
	public void visit(site.ilemon.ast.forparse.Method obj) {
		rtTypeOfMethod = obj.returnType;
		this.index = 0;
		this.indexTable = new Hashtable<>();
		this.visit(obj.returnType);
		Type t = this.type;
		List<site.ilemon.ast.forcodegen.Declare> formals = new ArrayList<site.ilemon.ast.forcodegen.Declare>();
		for (site.ilemon.ast.forparse.Declare dec : obj.inputParams){
			this.visit(dec);
			formals.add(this.declare);
		}
		List<site.ilemon.ast.forcodegen.Declare> locals = new ArrayList<site.ilemon.ast.forcodegen.Declare>();
		for (site.ilemon.ast.forparse.Declare dec : obj.localParams){
			this.visit(dec);
			locals.add(this.declare);
		}
		this.stmts = new ArrayList<site.ilemon.ast.forcodegen.Stmt>();
		for(site.ilemon.ast.forparse.Stmt stmt:obj.stmts){
			this.visit(stmt);
		}
		if( obj.returnType.toString().equals("@int"))
			emit(new Ireturn());
		else if( obj.returnType.toString().equals("@float"))
			emit(new Freturn());
		site.ilemon.ast.forcodegen.Method method = new site.ilemon.ast.forcodegen.Method(t, obj.name, 
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
		else if(  obj.t instanceof site.ilemon.ast.forparse.Float){
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
		if( array.length == 0){
			array = new String[1];
			array[0] = f;
		}
		for (int i = 0; i < array.length; i++) {
			this.visit(new site.ilemon.ast.forparse.Str1(array[i], obj.lineNumber));
			emit(new Aload(index1 -1 ) );
			emit(new site.ilemon.ast.forcodegen.Printf(new site.ilemon.ast.forcodegen.Str(), array[i]));
			if( i+1 < obj.exprs.size() ){
				this.visit(obj.exprs.get(i+1));
				if( t instanceof Int)
					emit(new site.ilemon.ast.forcodegen.Printf(new site.ilemon.ast.forcodegen.Int(), null));
				else
					emit(new site.ilemon.ast.forcodegen.Printf(new site.ilemon.ast.forcodegen.Float(), null));
			}

		}
	}

	@Override
	public void visit(PrintNewLine obj) {
		emit(new site.ilemon.ast.forcodegen.PrintNewLine());
	}

	@Override
	public void visit(Return obj) {
		if(obj.returnExpr instanceof Add)
			this.visit((Add)obj.returnExpr);
		else if(obj.returnExpr instanceof Sub)
			this.visit((Sub)obj.returnExpr);
		else if(obj.returnExpr instanceof Mul)
			this.visit((Mul)obj.returnExpr);
		else if(obj.returnExpr instanceof Div)
			this.visit((Div)obj.returnExpr);
		else if(obj.returnExpr instanceof Num)
			this.visit((Num)obj.returnExpr);
		else if(obj.returnExpr instanceof Id)
			this.visit((Id)obj.returnExpr);
	}

	@Override
	public void visit(Stmt obj) {
		if( obj instanceof Return)
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
	public void visit(Str obj) {

	}

	@Override
	public void visit(Str1 obj) {
		this.type = new site.ilemon.ast.forcodegen.Str();
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
	public void visit(site.ilemon.ast.forparse.Type obj) {
		if( obj.toString().equals("@int"))
			this.type = new site.ilemon.ast.forcodegen.Int();
		else if( obj.toString().equals("@float"))
			this.type = new site.ilemon.ast.forcodegen.Float();
		//else
		//this.type = new com.ast.forcodegen.Void();
	}

	@Override
	public void visit(site.ilemon.ast.forparse.Void obj) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void visit(If obj) {
		Label l = new Label();
		Label r = new Label();
		this.visit(obj.condition);
		if( obj.condition instanceof LT ){
			emit(new Ificmpgt(l));
			
		}
		else if( obj.condition instanceof GT ){
			emit(new Ificmplt(l));
		}
		this.visit(obj.thenStmt);
		emit(new GoTo(r));
		emit(new LabelJ(l));
		this.visit(obj.elseStmt);
		emit(new LabelJ(r));
	}
	
	@Override
	public void visit(GT obj) {
		Label t = null;
		if( tempLabel == null ){
			t = new Label();
			tempLabel = t;
		}
		this.visit(obj.left);
		this.visit(obj.right);
		//emit(new Ificmplt(t));
		tempLabel = null;
	}
	

	@Override
	public void visit(LT obj) {
		Label t = null;
		if( tempLabel == null ){
			t = new Label();
			tempLabel = t;
		}
		this.visit(obj.left);
		this.visit(obj.right);
		//emit(new Ificmpgt(t));
	}
	
	Label tempLabel;

	@Override
	public void visit(While obj) {
		Label con = new Label();
		Label end = new Label();
		emit(new LabelJ(con));
		this.visit(obj.condition);
		if( obj.condition instanceof LT ){
			emit(new Ificmpgt(end));
			
		}
		else if( obj.condition instanceof GT ){
			emit(new Ificmplt(end));
		}
		this.visit(obj.body);
		emit(new GoTo(con));
		emit(new LabelJ(end));
	}

	@Override
	public void visit(Bool obj) {

	}

}
