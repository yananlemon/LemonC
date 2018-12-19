package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * 乘法表达式节点
 * @author andy
 *
 */
public class Mul extends Expr implements IElement{

	public Expr left,right;

	public Mul(Expr left, Expr right,int lineNumber) {
		this.left = left;
		this.right = right;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
