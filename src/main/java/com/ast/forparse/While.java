package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * if语句节点
 * @author andy
 *
 */
public class While extends Stmt implements IElement{

	/**条件表达式**/
	public Expr condition;
	
	public Stmt body;

	public While(Expr condition, Stmt body,int lineNumber) {
		super();
		this.condition = condition;
		this.body = body;
		this.lineNumber = lineNumber;
	}

	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
