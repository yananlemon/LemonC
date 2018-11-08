package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

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
