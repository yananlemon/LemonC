package com.ast.forcodegen;

import java.util.List;

public class MainClass {

	public String name;
	
	public List<Method> methods;

	public MainClass(String name, List<Method> methods) {
		this.name = name;
		this.methods = methods;
	}
	
}
