package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

public class Float extends Type implements IElement{
	public Float() {}

	@Override
	public String toString(){
		return "@float";
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
