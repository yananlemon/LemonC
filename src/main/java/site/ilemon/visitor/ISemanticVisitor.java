package site.ilemon.visitor;

import site.ilemon.ast.forparse.*;
import site.ilemon.ast.forparse.Float;

/**
 * 语义分析访问者接口
 * @author andy
 *
 */
public interface ISemanticVisitor {
	
	void visit(Add obj);
	void visit(site.ilemon.ast.forparse.Bool obj);
	void visit(And obj);
	void visit(Assign obj);
	void visit(Block obj);
	void visit(Call obj);
	void visit(Declare obj);
	void visit(Div obj);
	void visit(Float obj);
	void visit(Expr obj);
	void visit(GT obj);
	void visit(Id obj);
	void visit(If obj);
	void visit(Int obj);
	void visit(LT obj);
	void visit(MainClass obj);
	void visit(Method obj);
	void visit(Mul obj);
	void visit(Num obj);
	void visit(Or obj);
	void visit(Printf obj);
	void visit(PrintNewLine obj);
	void visit(Return obj);
	void visit(Stmt obj);
	void visit(Str obj);
	void visit(Str1 obj);
	void visit(Sub obj);
	void visit(Type obj);
	void visit(site.ilemon.ast.forparse.Void obj);
	void visit(While obj);
}
