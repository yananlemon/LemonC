package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 加法表达式节点
 * @author andy
 *
 */
public class Num extends Expr implements IElement{

	public Object num;

	public Num(Object num, int lineNum){
		this.num = num;
		this.lineNumber = lineNum;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
