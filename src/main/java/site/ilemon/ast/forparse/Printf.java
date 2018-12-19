package site.ilemon.ast.forparse;

import java.util.List;

import site.ilemon.visitor.*;

/**
 * 格式化打印语句节点
 * @author andy
 *
 */
public class Printf extends Stmt implements IElement{

	/**格式化字符串**/
	public String format;
	
	/**表达式集合**/
	public List<Expr> exprs;
	

	public Printf(String format, List<Expr> exprs,int lineNumber) {
		super();
		this.format = format;
		this.exprs = exprs;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
	
}
