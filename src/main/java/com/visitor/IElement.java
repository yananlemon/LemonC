package com.visitor;

public interface IElement {
	void accept(ISemanticVisitor visitor);
}
