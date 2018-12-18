package com.ast.forparse;

import java.util.List;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 方法调用表达式节点
 * @author andy
 *
 */
public class Call extends Expr implements IElement{

	/**方法返回类型**/
	public Type returnType;
	
	/**方法名称**/
	public String name;
	
	/**方法参数**/
	public List<Expr> inputParams;

	public Call(String name, List<Expr> inputParams,int lineNumber) {
		this.name = name;
		this.inputParams = inputParams;
		this.lineNumber = lineNumber;
	}
	
	public Call(String name, List<Expr> inputParams,int lineNumber,Type rt) {
		this.name = name;
		this.inputParams = inputParams;
		this.lineNumber = lineNumber;
		this.returnType = rt;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
