package com.ast.forparse;

import java.util.List;

import com.visitor.IElement;
import com.visitor.ISemanticVisitor;

/**
 * 抽象语法树根节点
 * @author andy
 *
 */
public class MainClass implements IElement{
	
	public String className;
	
	public List<Method> methods = null;

	public MainClass(String name,List<Method> methods) {
		this.className = name;
		this.methods = methods;
	}
	
	@Override
	public void accept(ISemanticVisitor visitor) {
		visitor.visit(this);
	}
}
