package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 赋值语句节点
 * @author andy
 *
 */
public class Assign extends Stmt implements IElement{

	/**标识符类型**/
	public Type type;
	
	/**标识符名称**/
	public String name;
	
	/**表达式**/
	public Expr expr;

	public Assign(Type type, String name, Expr expr,int lineNumber) {
		this.type = type;
		this.name = name;
		this.expr = expr;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
