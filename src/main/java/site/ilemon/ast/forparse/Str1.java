package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * 加法表达式节点
 * @author andy
 *
 */
public class Str1 extends Expr implements IElement{

	public String value;

	public Str1(String value,int lineNumber) {
		this.value = value;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
