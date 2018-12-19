package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

public class Int extends Type implements IElement{
	public Int() {}

	@Override
	public String toString(){
		return "@int";
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
