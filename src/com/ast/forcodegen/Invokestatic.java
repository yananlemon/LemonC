package com.ast.forcodegen;

import java.util.List;

public class Invokestatic extends Stmt{

	public String name;
	
	public List<Type> at;

	public Invokestatic(String name, List<Type> at) {
		this.name = name;
		this.at = at;
	}

}
