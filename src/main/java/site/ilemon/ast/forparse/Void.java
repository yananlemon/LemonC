package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

public class Void extends Type implements IElement{
	public Void() {}

	@Override
	public String toString(){
		return "@void";
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
