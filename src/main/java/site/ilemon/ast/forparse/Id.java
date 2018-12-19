package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * 标识符表达式节点
 * @author andy
 *
 */
public class Id extends Expr implements IElement{

	public String name;
	
	public Type type;

	public Id(String name,Type type,int lineNumber) {
		this.name = name;
		//this.type = new Int();// 目前只支持Int类型 2018-12-18增加double
		this.type = type;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
