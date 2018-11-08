package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

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
