package site.ilemon.ast.forparse;

import site.ilemon.visitor.IElement;
import site.ilemon.visitor.ISemanticVisitor;

public class Bool extends Type implements IElement{
	public Bool() {}

	@Override
	public String toString(){
		return "@boolean";
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}