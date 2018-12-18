package com.ast.forcodegen;

import java.util.List;

public class Invokestatic extends Stmt{

	public String name;
	
	public List<Type> at;
	
	public com.ast.forparse.Type rt;

	public Invokestatic(String name, List<Type> at,com.ast.forparse.Type rt) {
		this.name = name;
		this.at = at;
		this.rt = rt;
	}

}
