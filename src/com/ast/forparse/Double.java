package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

public class Double extends Type implements IElement{
	public Double() {}

	@Override
	public String toString(){
		return "@double";
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
