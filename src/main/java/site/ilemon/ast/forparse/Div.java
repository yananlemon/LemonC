package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * 除法表达式节点
 * @author andy
 *
 */
public class Div extends Expr implements IElement{

	public Expr left,right;

	public Div(Expr left, Expr right,int lineNumber) {
		this.left = left;
		this.right = right;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
