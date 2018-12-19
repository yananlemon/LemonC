package site.ilemon.ast.forparse;

import java.util.List;

import site.ilemon.visitor.*;

/**
 * 方法节点
 * @author andy
 *
 */
public class Method implements IElement{

	public int lineNumber;
	
	/**方法返回类型**/
	public Type returnType;
	
	/**方法名称**/
	public String name;
	
	/**方法入参集合**/
	public List<Declare> inputParams;
	
	/**方法局部变量集合**/
	public List<Declare> localParams;
	
	/**方法局部变量集合**/
	public List<Stmt> stmts;
	
	public Expr returnExpr;

	public Method(Type returnType, String name, List<Declare> inputParams,
			List<Declare> localParams, List<Stmt> stmts,Expr returnExpr,int lineNumber) {
		super();
		this.returnType = returnType;
		this.name = name;
		this.inputParams = inputParams;
		this.localParams = localParams;
		this.stmts = stmts;
		this.returnExpr = returnExpr;
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
