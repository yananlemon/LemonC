package com.semantic;

import java.util.HashSet;
import java.util.Iterator;

import java_cup.runtime.int_token;

import com.ast.forparse.Add;
import com.ast.forparse.And;
import com.ast.forparse.Assign;
import com.ast.forparse.Block;
import com.ast.forparse.Bool;
import com.ast.forparse.Call;
import com.ast.forparse.Declare;
import com.ast.forparse.Div;
import com.ast.forparse.Double;
import com.ast.forparse.Expr;
import com.ast.forparse.GT;
import com.ast.forparse.Id;
import com.ast.forparse.If;
import com.ast.forparse.Int;
import com.ast.forparse.LT;
import com.ast.forparse.MainClass;
import com.ast.forparse.Method;
import com.ast.forparse.Mul;
import com.ast.forparse.Num;
import com.ast.forparse.Or;
import com.ast.forparse.PrintNewLine;
import com.ast.forparse.Printf;
import com.ast.forparse.Return;
import com.ast.forparse.Stmt;
import com.ast.forparse.Str;
import com.ast.forparse.Str1;
import com.ast.forparse.Sub;
import com.ast.forparse.Type;
import com.ast.forparse.Void;
import com.ast.forparse.While;
import com.visitor.ISemanticVisitor;

/**
 * 语义分析
 * @author andy
 *
 */
public class Semantic2 implements ISemanticVisitor {

	public boolean pass;
	
	private MethodVarTable methodVarTable;
	
	private HashSet<String> currMethodLocalVars;

	private Type type;

	@Override
	public void visit(Add obj) {

	}

	@Override
	public void visit(And obj) {

	}

	@Override
	public void visit(Assign obj) {

	}

	@Override
	public void visit(Block obj) {
		for( Stmt stmt : obj.stmts)
			this.visit(stmt);
	}

	@Override
	public void visit(Call obj) {
		// TODO Auto-generated method stub

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
	public void visit(Double obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Expr obj) {
		if( obj instanceof Add ||
				 obj instanceof Sub ||
				 obj instanceof Mul ||
				 obj instanceof Div){
			this.type = new Int();
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
		// TODO Auto-generated method stub

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
