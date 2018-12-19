package site.ilemon.ast.forcodegen;

public class Astore extends Stmt {
	public int index;

    public Astore(int index)
    {
        this.index = index;
    }
}
