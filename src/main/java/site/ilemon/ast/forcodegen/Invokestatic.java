package site.ilemon.ast.forcodegen;

import java.util.List;

public class Invokestatic extends Stmt{

	public String name;
	
	public List<Type> at;
	
	public site.ilemon.ast.forparse.Type rt;

	public Invokestatic(String name, List<Type> at,site.ilemon.ast.forparse.Type rt) {
		this.name = name;
		this.at = at;
		this.rt = rt;
	}

}
