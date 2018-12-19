package site.ilemon.semantic;

import java.util.HashSet;

import com.sun.xml.internal.bind.v2.model.core.ID;

import site.ilemon.ast.forparse.Float;
import site.ilemon.ast.forparse.*;
import site.ilemon.ast.forparse.Void;
import site.ilemon.visitor.ISemanticVisitor;

/**
 * 语义分析
 * @author andy
 *
 */
public class Semantic implements ISemanticVisitor {

	public boolean pass = true;
	
	private MethodVarTable methodVarTable;
	
	private HashSet<String> currMethodLocalVars;

	private Type type;

	
	
	public MethodVarTable getMethodVarTable() {
		return methodVarTable;
	}

	@Override
	public void visit(Add obj) {
		this.visit(obj.left);
		this.visit(obj.right);
	}

	@Override
	public void visit(And obj) {

	}

	@Override
	public void visit(Assign obj) {
		this.visit(this.methodVarTable.get(obj.name));
		
		this.visit(obj.expr);
	}

	@Override
	public void visit(Block obj) {
		for( Stmt stmt : obj.stmts)
			this.visit(stmt);
	}

	@Override
	public void visit(Call obj) {
		System.out.println(obj);
	}

	@Override
	public void visit(Declare obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Div obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Float obj) {
		// TODO Auto-generated method stub

	}
	
	private Type recursionType(Expr obj){
		if( obj instanceof Add){
			Type t1 = recursionType(((Add)obj).left);
			Type t2 = recursionType(((Add)obj).right);
			if( !t1.toString().equals(t2.toString())) {
				error(obj.lineNumber,t1.toString()+"和"+t2.toString()+"类型不一致" );
				System.exit(1);
			}
			if( t1 instanceof Float || t2 instanceof Float)
				return new Float();
			else
				return new Int();
		}
		if( obj instanceof Sub){
			Type t1 = recursionType(((Sub)obj).left);
			Type t2 = recursionType(((Sub)obj).right);
			if( !t1.toString().equals(t2.toString())) {
				error(obj.lineNumber,t1.toString()+"和"+t2.toString()+"类型不一致" );
				System.exit(1);
			}
			if( t1 instanceof Float || t2 instanceof Float)
				return new Float();
			else
				return new Int();
		}
		if( obj instanceof Mul){
			Type t1 = recursionType(((Mul)obj).left);
			Type t2 = recursionType(((Mul)obj).right);
			if( !t1.toString().equals(t2.toString())) {
				// 2018-12-19自动提升类型暂时未完成
				/*if( (((Mul)obj).right) instanceof Id) {
					Id id = ((Id)((Mul)obj).right);
					if( id.type instanceof Int) {
						if(this.methodVarTable.get(id.name) != null) {
							this.methodVarTable.put(id.name, new Float());
						}
					}
				}*/
				error(obj.lineNumber,t1.toString()+"和"+t2.toString()+"类型不一致" );
				System.exit(1);
			}
			if( t1 instanceof Float || t2 instanceof Float)
				return new Float();
			else
				return new Int();
		}
		if( obj instanceof Div){
			Type t1 = recursionType(((Div)obj).left);
			Type t2 = recursionType(((Div)obj).right);
			if( !t1.toString().equals(t2.toString())) {
				error(obj.lineNumber,t1.toString()+"和"+t2.toString()+"类型不一致" );
				System.exit(1);
			}
			if( t1 instanceof Float || t2 instanceof Float)
				return new Float();
			else
				return new Int();
		}
		if( obj instanceof Num){
			if( ((Num)obj).t instanceof Int)
				return new Int();
			else 
				return new Float();
		}
		return this.methodVarTable.get(((Id)obj).name);
		
	}

	@Override
	public void visit(Expr obj) {
		if( obj instanceof Add ||
				 obj instanceof Sub ||
				 obj instanceof Mul ||
				 obj instanceof Div){
			this.type = recursionType(obj);
		}
		else if( obj instanceof And || obj instanceof Or || 
				obj instanceof LT || obj instanceof GT )
			this.type = new Bool();
		else if( obj instanceof Num)
			this.type = new Int();
		else if( obj instanceof Id)
			this.visit((Id)obj);
	}

	@Override
	public void visit(GT obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Id obj) {
		if( this.methodVarTable.get(obj.name) == null) {
			error( obj.lineNumber,obj.name+"未定义");
		}
		this.type = obj.type;
	}

	@Override
	public void visit(If obj) {
		System.out.println("visiting if stmt....");
		this.visit(obj.condition);
		if( !this.type.toString().equals("@boolean") )
			 error(obj.condition.lineNumber, "the condition's type should be a boolean.");
	}

	@Override
	public void visit(Int obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(LT obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(MainClass obj) {
		HashSet<String> methodSet = new HashSet<String>();
		for(Method method : obj.methods){
			if(methodSet.add(method.name))
				this.visit(method);
			else
				error(method.lineNumber, "duplicate method " + method.name);
		}
	}

	@Override
	public void visit(Method obj) {
		this.methodVarTable = new MethodVarTable();
		this.currMethodLocalVars = new HashSet<String>();
		this.methodVarTable.put(obj.inputParams, obj.localParams);
		for (Declare dec : obj.localParams) {
			this.currMethodLocalVars.add(dec.name);
		}
		for(Stmt stmt : obj.stmts)
			this.visit(stmt);
		if( !obj.name.equals("main")){
			if( ((obj.stmts.get(obj.stmts.size()-1))) instanceof Return ){
				obj.returnExpr = ((Return)obj.stmts.get(obj.stmts.size()-1)).returnExpr;
			}else{
				//for()
				//this.visit(obj.stmts);
			}
			this.visit(obj.returnExpr);
			if (!isMatch(obj.returnType, this.type))
				error(obj.returnType.lineNumber,
						"the return expression's type is not match the method \"" +
								obj.name + "\" declared.");
		}
		
	}

	@Override
	public void visit(Mul obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Num obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Or obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Printf obj) {
		System.out.println(obj);
		for(Expr expr : obj.exprs)
			this.visit(expr);
	}

	@Override
	public void visit(PrintNewLine obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Return obj) {
		this.visit(obj.returnExpr);
	}

	@Override
	public void visit(Stmt obj) {
		if( obj instanceof While )
			this.visit( (While)obj );
		else if( obj instanceof Assign )
			this.visit( (Assign)obj );
		else if( obj instanceof Block )
			this.visit( (Block)obj );
		else if( obj instanceof Return)
			this.visit( (Return)obj );
		else if( obj instanceof If)
			this.visit( (If)obj );
		else if( obj instanceof Printf)
			this.visit( (Printf)obj );
	}

	@Override
	public void visit(Str obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Str1 obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Sub obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Type obj) {
		this.type = obj;
	}

	@Override
	public void visit(Void obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(While obj) {
		System.out.println("visiting while stmt....");
		this.visit(obj.condition);
		if( !this.type.toString().equals("@boolean") )
			 error(obj.condition.lineNumber, "the condition's type should be a boolean.");
	}
	
	private void error(int lineNum, String msg){
		this.pass = false;
		System.out.println("Error: Line " + lineNum + " " + msg);
	}
	
	private boolean isMatch(Type target,Type curr){
		if(target.toString().equals(curr.toString()))
			return true;
		else
			return false;
	}

	@Override
	public void visit(Bool obj) {
		System.out.println("aaaaaa");
	}

}
