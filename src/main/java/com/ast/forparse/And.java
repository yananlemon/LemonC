package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * && 节点
 * @author andy
 *
 */
public class And extends Expr implements IElement{

	public Expr left,right;

	public And(Expr left, Expr right,int lineNumber) {
		this.left = left;
		this.right = right;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
