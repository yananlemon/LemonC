package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

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
