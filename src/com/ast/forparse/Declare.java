package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 声明节点
 * @author andy
 *
 */
public class Declare implements IElement{

	/**声明类型**/
	public Type type;
	
	/**变量名称**/
	public String name;
	
	public int lineNumber;

	public Declare(Type type, String name,int lineNumber) {
		this.type = type;
		this.name = name;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
