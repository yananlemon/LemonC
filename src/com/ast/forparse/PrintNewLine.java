package com.ast.forparse;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

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
