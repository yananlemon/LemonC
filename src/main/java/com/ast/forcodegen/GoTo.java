package com.ast.forcodegen;

import com.codegen.ast.Label;

public class GoTo extends Stmt{

	public Label l;

	public GoTo(Label l){
		this.l = l;
	}
}
