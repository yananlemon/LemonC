package site.ilemon.ast.forcodegen;

import site.ilemon.codegen.ast.Label;


public class GoTo extends Stmt{

	public Label l;

	public GoTo(Label l){
		this.l = l;
	}
}
