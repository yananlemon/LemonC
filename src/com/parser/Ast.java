package com.parser;

import java.util.List;

public class Ast {

	// program 节点
	public static class Program{
		public static abstract class T{	}

		public static class ProgramSingle extends T{
			public MainClass.T mainClass;
			public ProgramSingle(MainClass.T mainClass){
				this.mainClass = mainClass;
			}
		}
	}

	// declare 节点
	public static class Declare{
		public static abstract class T{
			public int lineNumber;
		}

		public static class DeclareSingle extends T{

			public Type.T type;

			public String id;

			public DeclareSingle(Type.T type, String id,int lineNumber) {
				this.type = type;
				this.id = id;
				this.lineNumber = lineNumber;
			}


		}
	}


	// main class 节点
	public static class MainClass{
		public static abstract class T{	}

		public static class MainClassSingle extends T{
			public String className;
			public List<Method.T> methods;	// 方法集合
			public MainClassSingle(String name, List<Method.T> methods){
				this.className = name;
				this.methods = methods;
			}
		}
	}

	// statement 节点
	public static class Stmt{
		public static abstract class T{
			public int lineNumber;
		}

		// 赋值语句
		public static class Assign extends T{

			public Type.T type;		// 标识符类型

			public String name;		// 标识符名称

			public Exp.T expr; 		// 表达式

			public Assign(String name, Exp.T expr, int lineNumber) {
				this.name = name;
				this.expr = expr;
				this.type = null;
				this.lineNumber = lineNumber;
			}
		}

		// 块语句
		public static class Block extends T{

			public List<Stmt.T> stmts;

			public Block(List<T> stms, int lineNum){
				this.stmts = stms;
				this.lineNumber = lineNum;
			}
		}

		// if语句
		public static class If extends T{

			public Exp.T condition; // 条件

			public T ifStmt;

			public T elseStmt;

			public If(com.parser.Ast.Exp.T condition, T ifStmt, T elseStmt,int lineNumber) {
				super();
				this.condition = condition;
				this.ifStmt = ifStmt;
				this.elseStmt = elseStmt;
				this.lineNumber = lineNumber;
			}


		}

		// 格式化打印语句
		public static class Printf extends T{
			public String format; 		// 格式化的字符串
			public List<Exp.T> exprs;	// 表达式列表

			public Printf(String format, List<Exp.T> exprs,int lineNum){
				this.format = format;
				this.exprs = exprs;
				this.lineNumber = lineNum;
			}
		}
		
		public static class PrintNewLine extends T{

			public PrintNewLine(int lineNum){
				this.lineNumber = lineNum;
			}
		}

		// 循环语句
		public static class While extends T{
			public Exp.T condition; // 条件

			public T body;			// 循环体

			public While(Exp.T condition, T body,int lineNumber) {
				this.condition = condition;
				this.body = body;
				this.lineNumber = lineNumber;
			}
		}
	}

	// expression 节点
	public static class Exp{

		public static abstract class T{
			public int lineNumber;
		}

		public static class Add extends T{

			public T left,right;

			public Add(T left, T right,int lineNumber) {
				this.left = left;
				this.right = right;
				this.lineNumber = lineNumber;
			}

		}

		// &&
		public static class And extends T{

			public T left,right;

			public And(T left, T right,int lineNumber) {
				this.left = left;
				this.right = right;
				this.lineNumber = lineNumber;
			}

		}

		// ||
		public static class Or extends T{

			public T left,right;

			public Or(T left, T right,int lineNumber) {
				this.left = left;
				this.right = right;
				this.lineNumber = lineNumber;
			}

		}

		public static class Call extends T{

			public T exp;
			public String id;
			public List<T> args;// 方法参数集合
			public String type; // type of first field "exp"
			public List<Type.T> at; // 方法参数类型
			public Type.T rt;

			public Call(T exp, String id, List<T> args, int lineNumber)
			{
				this.exp = exp;
				this.id = id;
				this.args = args;
				this.type = null;
				this.lineNumber = lineNumber;
			}

		}

		// 标识符
		public static class Id extends T{
			public String name; // 标识符名称
			public Type.T type; // 标识符类型
			public boolean isField; // 是否是一个字段

			public Id(String name, int lineNumber){
				this.name = name;
				this.lineNumber = lineNumber;
			}

			public Id(String name, Type.T type, boolean isField, int lineNumber){
				this.name = name;
				this.type = type;
				this.isField = isField;
				this.lineNumber = lineNumber;
			}
		}

		// <
		public static class LT extends T{

			public T left,right;

			public LT(T left, T right,int lineNumber) {
				this.left = left;
				this.right = right;
				this.lineNumber = lineNumber;
			}

		}

		// >
		public static class GT extends T{

			public T left,right;

			public GT(T left, T right,int lineNumber) {
				this.left = left;
				this.right = right;
				this.lineNumber = lineNumber;
			}

		}

		// !
		public static class Not extends T{
			public T exp;

			public Not(T exp, int lineNum)
			{
				this.exp = exp;
				this.lineNumber = lineNum;
			}
		}

		// 数字节点
		public static class Num extends T{

			public Object num;

			public Num(Object num, int lineNum){
				this.num = num;
				this.lineNumber = lineNum;
			}
		}

		public static class Sub extends T
		{
			public T left, right;

			public Sub(T left, T right, int lineNum)
			{
				this.left = left;
				this.right = right;
				this.lineNumber = lineNum;
			}
		}
		
		// 标识符
		public static class Str extends T{
			public String value; // 标识符名称

			public Str(String value, int lineNumber){
				this.value = value;
				this.lineNumber = lineNumber;
			}
		}

		// *
		public static class Mul extends T{
			public T left, right;

			public Mul(T left, T right, int lineNum){
				this.left = left;
				this.right = right;
				this.lineNumber = lineNum;
			}
		}

		// /
		public static class Div extends T{
			public T left, right;

			public Div(T left, T right, int lineNum){
				this.left = left;
				this.right = right;
				this.lineNumber = lineNum;
			}
		}

	}


	public static class Method{
		public static abstract class T{}

		public static class MethodSingle extends T
		{
			public Type.T retType;				// 方法声明的返回值
			public String name;					// 方面名称		
			public List<Declare.T> formals;		// 方法入参集合
			public List<Declare.T> locals;		// 方法局部变量集合
			public List<Stmt.T> stms;			// 方法语句集合
			public Exp.T retExp;

			public MethodSingle(Type.T retType, String name,
					List<Declare.T> formals,
					List<Declare.T> locals,
					List<Stmt.T> stms,
					Exp.T retExp){
				this.retType = retType;
				this.name = name;
				this.formals = formals;
				this.locals = locals;
				this.stms = stms;
				this.retExp = retExp;
			}
		}
	}

	public static class Type{
		public static abstract class T{	}

		public static class Void extends T{
			public Void() {}

			@Override
			public String toString(){
				return "@void";
			}
		}
		
		public static class Int extends T{
			public Int() {}

			@Override
			public String toString(){
				return "@int";
			}
		}

		public static class Double extends T{
			public Double() {}

			@Override
			public String toString(){
				return "@double";
			}
		}
		
		public static class Boolean extends T{
			public Boolean() {}

			@Override
			public String toString(){
				return "@boolean";
			}
		}
		
		public static class Str extends T{
			public Str() {}

			@Override
			public String toString(){
				return "@void";
			}
		}
	}
}
