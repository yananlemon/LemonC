package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

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
