package site.ilemon.ast.forcodegen;

import site.ilemon.codegen.ast.Label;

public class Ificmpgt extends Stmt{

	public Label l;

	public Ificmpgt(Label l){
		this.l = l;
	}
}
