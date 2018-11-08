package com.ast.forcodegen;


public class Printf extends Stmt {
	public Type exprType;
	
	public String v;
	
	public Printf(Type t,String v) {
		this.exprType = t;
		this.v = v;
	}
}
