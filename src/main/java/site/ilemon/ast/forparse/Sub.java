package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * 减法表达式节点
 * @author andy
 *
 */
public class Sub extends Expr implements IElement{

	public Expr left,right;

	public Sub(Expr left, Expr right,int lineNumber) {
		this.left = left;
		this.right = right;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
