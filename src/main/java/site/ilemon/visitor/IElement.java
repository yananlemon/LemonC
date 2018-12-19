package site.ilemon.visitor;

public interface IElement {
	void accept(ISemanticVisitor visitor);
}
