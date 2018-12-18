package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;


/**
 * 格式化打印语句节点
 * @author andy
 *
 */
public class Return extends Stmt implements IElement{

	/**格式化字符串**/
	public Expr returnExpr;
	

	public Return(Expr returnExpr,int lineNumber) {
		this.returnExpr = returnExpr;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
