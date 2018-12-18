package com.ast.forcodegen;
/**
 * 存储int类型到局部变量
 * @author andy
 *
 */
public class Istore extends Stmt {
	public int index;

    public Istore(int i){
        this.index = i;
    }
}
