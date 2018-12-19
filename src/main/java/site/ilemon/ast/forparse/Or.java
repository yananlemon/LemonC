package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * || 节点
 * @author andy
 *
 */
public class Or extends Expr implements IElement{

	public Expr left,right;

	public Or(Expr left, Expr right,int lineNumber) {
		this.left = left;
		this.right = right;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
