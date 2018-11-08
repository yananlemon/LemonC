package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 加法表达式节点
 * @author andy
 *
 */
public class Str1 extends Expr implements IElement{

	public String value;

	public Str1(String value,int lineNumber) {
		this.value = value;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
