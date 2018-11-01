package com.semantic;

import java.util.HashSet;

import com.parser.Ast;
import com.parser.Ast.Declare.DeclareSingle;
import com.parser.Ast.Exp.Add;
import com.parser.Ast.Exp.And;
import com.parser.Ast.Exp.Call;
import com.parser.Ast.Exp.GT;
import com.parser.Ast.Exp.Id;
import com.parser.Ast.Exp.LT;
import com.parser.Ast.Exp.Mul;
import com.parser.Ast.Exp.Not;
import com.parser.Ast.Exp.Num;
import com.parser.Ast.Exp.Str;
import com.parser.Ast.Exp.Sub;
import com.parser.Ast.MainClass;
import com.parser.Ast.MainClass.MainClassSingle;
import com.parser.Ast.Method.MethodSingle;
import com.parser.Ast.Program.ProgramSingle;
import com.parser.Ast.Stmt.Assign;
import com.parser.Ast.Stmt.Block;
import com.parser.Ast.Stmt.If;
import com.parser.Ast.Stmt.PrintNewLine;
import com.parser.Ast.Stmt.Printf;
import com.parser.Ast.Stmt.While;
import com.parser.Ast.Type.Int;
import com.parser.Ast.Type.Void;

/**
 * 语义分析
 * @author andy
 * @version 1.0
 */
public class Semantic implements Visitor{

	private MethodVariableTable methodVariableTable;

	private HashSet<String> curMthLocals;

	private Ast.Type.T type; // 临时保存类型

	private boolean isOk;

	public Semantic(){
		this.methodVariableTable = new MethodVariableTable();
		this.curMthLocals = new HashSet<String>();
	}

	@Override
	public void visit(Int t) {

	}
	

	@Override
	public void visit(DeclareSingle d) {

	}

	@Override
	public void visit(Add e) {

	}

	@Override
	public void visit(And e) {

	}

	@Override
	public void visit(Call e) {

	}

	@Override
	public void visit(Id e) {
		Ast.Type.T idType = this.methodVariableTable.get(e.name);
		if (idType == null){
			error(e.lineNumber, "you should declare \"" + e.name + "\" before use it.");
			e.type = new Ast.Type.T()
			{
				@Override
				public String toString()
				{
					return "unknown";
				}
			};
			this.type = e.type;
		} else
		{
			e.type = idType;
			this.type = idType;
		}
	}

	@Override
	public void visit(LT e) {
		this.visit(e.left);
        Ast.Type.T lefty = this.type;
        this.visit(e.right);
        if (!this.type.toString().equals(lefty.toString()))
        {
            error(e.lineNumber, "compare expression" +
                    " the type of left is " + lefty.toString() +
                    ", but the type of right is " + this.type.toString());
        } else if (!new Ast.Type.Int().toString().equals(this.type.toString()))
            error(e.lineNumber, "only integer numbers can be compared.");

        this.type = new Ast.Type.Boolean();
	}

	@Override
	public void visit(GT e) {

	}

	@Override
	public void visit(Not e) {

	}

	@Override
	public void visit(Num e) {
		this.type = new Ast.Type.Int();
	}

	@Override
	public void visit(Sub e) {

	}

	@Override
	public void visit(Mul e) {

	}

	@Override
	public void visit(Str e) {

	}

	@Override
	public void visit(Assign s) {
		System.out.println(s);
		this.visit(s.expr);
		s.type = this.type;
		Ast.Exp.Id id = new Id(s.name, s.lineNumber);
		this.visit(id);
		Ast.Type.T idType = this.type;
		if( !isMatch(s.type, idType) )
			error(s.lineNumber, "the type of \"" + s.name + "\" is " + idType.toString() +
					", but the type of expression is " + s.type.toString() +
					". Assign failed.");
	}

	@Override
	public void visit(Block s) {

	}

	@Override
	public void visit(If s) {

	}

	@Override
	public void visit(Printf s) {
		if( s.exprs != null && s.exprs.size() > 0 ){
			for( int i = 1; i < s.exprs.size(); i++ ){
				if( s.exprs.get(i) instanceof Ast.Exp.Id){
					if( !this.curMthLocals.contains(((Ast.Exp.Id)(s.exprs.get(i))).name))
						error(s.lineNumber, "you should declare \"" + ((Ast.Exp.Id)(s.exprs.get(i))).name + "\" before use it.");
				}
			}
		}
	}

	@Override
	public void visit(While s) {
		this.visit(s.condition);
		if (!this.type.toString().equals(new Ast.Type.Boolean().toString()))
			error(s.condition.lineNumber, "the condition's type should be a boolean.");
		this.visit(s.body);
	}

	@Override
	public void visit(MethodSingle m) {

		this.methodVariableTable.put(m.formals, m.locals);
		m.locals.forEach(local ->this.curMthLocals.add(((Ast.Declare.DeclareSingle)local).id ));
		m.stms.forEach(this::visit);
		this.visit(m.retExp);
		/*if (!isMatch(m.retType, this.type))
			error(m.retExp.lineNumber,
					"the return expression's type is not match the method \"" +
							m.name + "\" declared.");*/
	}

	private boolean isMatch(Ast.Type.T target, Ast.Type.T cur){
		if (target.toString().equals(cur.toString()))
			return true;
		return false;
	}

	private void error(int lineNum, String msg){
		this.isOk = false;
		System.out.println("Error: Line " + lineNum + " " + msg);
	}

	@Override
	public void visit(MainClass c) {

	}

	@Override
	public void visit(MainClassSingle c) {

	}

	@Override
	public void visit(ProgramSingle p) {
		if( p.mainClass instanceof Ast.MainClass.MainClassSingle){
			((Ast.MainClass.MainClassSingle)(p.mainClass)).methods.forEach(this::visit);
		}
	}

	@Override
	public void visit(Void t) {
		this.type = new Ast.Type.Void();
	}

	@Override
	public void visit(PrintNewLine s) {
		
	}


}
