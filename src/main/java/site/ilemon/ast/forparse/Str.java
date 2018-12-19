package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

public class Str extends Type implements IElement{
	public Str() {}

	@Override
	public String toString(){
		return "@str";
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
