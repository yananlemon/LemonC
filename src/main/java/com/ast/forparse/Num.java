package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 整型数字节点
 * @author andy
 *
 */
public class Num extends Expr implements IElement{

	public Object num;
	
	public Type t;

	public Num(Object num, Type t, int lineNum){
		this.num = num;
		this.t = t;
		this.lineNumber = lineNum;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
