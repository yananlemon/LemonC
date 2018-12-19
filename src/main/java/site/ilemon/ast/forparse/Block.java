package site.ilemon.ast.forparse;

import java.util.List;

import site.ilemon.visitor.*;

/**
 * 赋值语句节点
 * @author andy
 *
 */
public class Block extends Stmt implements IElement{

	/**标识符类型**/
	public List<Stmt> stmts;

	public Block(List<Stmt> stmts,int lineNumber) {
		this.stmts = stmts;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
