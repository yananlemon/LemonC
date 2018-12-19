package site.ilemon.ast.forparse;

import site.ilemon.visitor.*;

/**
 * if语句节点
 * @author andy
 *
 */
public class If extends Stmt implements IElement{

	/**条件表达式**/
	public Expr condition;
	
	public Stmt thenStmt,elseStmt;

	public If(Expr condition, Stmt thenStmt, Stmt elseStmt,int lineNumber) {
		super();
		this.condition = condition;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
