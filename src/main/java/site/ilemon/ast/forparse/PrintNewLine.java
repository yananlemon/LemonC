package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * PringNewLine语句节点
 * @author andy
 *
 */
public class PrintNewLine extends Stmt implements IElement{

	public PrintNewLine(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
