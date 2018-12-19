package site.ilemon.visitor;

import site.ilemon.ast.forcodegen.*;


public interface IGeneratorCodeVistor {
	
	void visit(Aload obj);
	
	void visit(Astore obj);
	
	void visit(Declare obj);
	
	
	void visit(Fadd obj);
	void visit(Fsub obj);
	void visit(Fmul obj);
	void visit(Fdiv obj);
	void visit(Fload obj);
	void visit(Fstore obj);
	
	void visit(Iadd obj);
	
	void visit(Idiv obj);
	
	void visit(Iload obj);
	
	void visit(Ificmplt obj);
	
	void visit(GoTo obj);
	
	void visit(LabelJ obj);
	
	void visit(Imul obj);
	
	void visit(Int obj);
	
	void visit(Invokestatic obj);
	
	void visit(Ireturn obj);
	
	void visit(Freturn obj);
	
	void visit(Istore obj);
	
	void visit(Isub obj);
	
	void visit(Ldc obj);

	void visit(MainClass obj);
	
	void visit(Method obj);
	
	void visit(Stmt obj);
	
	void visit(Type obj);
	
	void visit(Printf obj);
	
	void visit(PrintNewLine obj);
	
}
