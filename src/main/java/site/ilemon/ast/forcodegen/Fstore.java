package site.ilemon.ast.forcodegen;
/**
 * 存储float类型到局部变量
 * @author andy
 *
 */
public class Fstore extends Stmt {
	public int index;

    public Fstore(int i){
        this.index = i;
    }
}
