package site.ilemon.ast;

import java.util.ArrayList;

/**
 * Created by andy on 2019/7/31.
 */
public class Ast {

    /**
     * Program
     */
    public static class Program{
        public static abstract class T{

        }
        public static class ProgramSingle extends T{
            public MainClass.T mainClass;

            public ProgramSingle(MainClass.T mainClass) {
                this.mainClass = mainClass;
            }
        }
    }

    /**
     * MainClass
     */
    public static class MainClass{
        public static abstract class T{

        }
        public static class MainClassSingle extends T {
            public String classId;
            public ArrayList<Ast.Declare.T> fields;
            public ArrayList<Ast.Method.T> methods;

            public MainClassSingle(String classId, ArrayList<Declare.T> fields, ArrayList<Method.T> methods) {
                this.classId = classId;
                this.fields = null;
                this.methods = methods;
            }
        }
    }

    /**
     * Stmt
     */
    public static class Stmt{
        public static abstract class T{
            public int lineNum;
        }
        public static class Assign extends T {
            public Ast.Expr.Id id;
            private Expr.T expr;
            //private Type.T type;
            public Assign(Ast.Expr.Id id, Expr.T exp, int lineNum) {
                this.id = id;
                this.expr = exp;
                //this.type = type;
                this.lineNum = lineNum;
            }
        }

        public static class Block extends T{
            public ArrayList<T> stmts;

            public Block(ArrayList<T> stmts,int lineNum) {
                this.stmts = stmts;
                this.lineNum = lineNum;
            }
        }

        public static class If extends T{
            public Expr.T condition;
            public T thenStmt,elseStmt;

            public If(Expr.T condition,T thenStmt,T elseStmt,int lineNum) {
                this.condition = condition;
                this.thenStmt = thenStmt;
                this.elseStmt = elseStmt;
                this.lineNum = lineNum;
            }
        }

        public static class Printf extends T {
            public String format;
            public ArrayList<Ast.Expr.T> exprs;

            public Printf(String format,ArrayList<Ast.Expr.T> exprs, int lineNum) {
                this.format = format;
                this.exprs = exprs;
                this.lineNum = lineNum;
            }
        }

        public static class Return extends T {
            public Expr.T expr;

            public Return(Expr.T expr, int lineNum) {
                this.expr = expr;
                this.lineNum = lineNum;
            }
        }

        public static class While extends T {
            public Ast.Expr.T condition;
            public Ast.Stmt.T body;

            public While(Ast.Expr.T condition, Ast.Stmt.T body, int lineNum)
            {
                this.condition = condition;
                this.body = body;
                this.lineNum = lineNum;
            }
        }
    }

    /**
     * Declare
     */
    public static class Declare{
        public static abstract class T{
            public int lineNum;
        }
        public static class DeclareSingle extends T {
            public Type.T type;
            public String id;

            public DeclareSingle(Type.T type, String id,int lineNum) {
                this.type = type;
                this.id = id;
                this.lineNum = lineNum;
            }
        }
    }

    /**
     * Type
     */
    public static class Type{
        public static abstract class T{

        }
        public static class Void extends T {
            @Override
            public String toString() {
                return "@void";
            }
        }
        public static class Int extends T {
            @Override
            public String toString() {
                return "@int";
            }
        }

        public static class Float extends T {
            @Override
            public String toString() {
                return "@float";
            }
        }

        public static class Str extends T {
            @Override
            public String toString() {
                return "@str";
            }
        }

        public static class Bool extends T {
            @Override
            public String toString() {
                return "@bool";
            }
        }
    }

    /**
     * Expression
     */
    public static class Expr {
        public static abstract class T {
            public static int lineNum;
        }

        /** 四则运算表达式 **/
        public static class Add extends T{
            public Ast.Expr.T left,right;

            public Add(Ast.Expr.T left, Ast.Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class Sub extends T{
            public Ast.Expr.T left,right;

            public Sub(Ast.Expr.T left, Ast.Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class Mul extends T{
            public Expr.T left,right;

            public Mul(Expr.T left, Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class Div extends T{
            public Expr.T left,right;

            public Div(Expr.T left, Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        /** 逻辑运算表达式 **/
        public static class And extends T{
            public Expr.T left,right;

            public And(Expr.T left, Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class Or extends T{
            public Expr.T left,right;

            public Or(Expr.T left, Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class Not extends T{
            public Expr.T expr;

            public Not(Expr.T expr) {
                this.expr = expr;
            }
        }

        public static class Call extends T{
            /**方法返回类型**/
            public Ast.Type.T returnType;

            /**方法名称**/
            public String name;

            /**方法参数**/
            public ArrayList<Expr.T> inputParams;

            public Call(String name, ArrayList<Expr.T> inputParams,int lineNumber) {
                this.name = name;
                this.inputParams = inputParams;
                this.lineNum = lineNumber;
            }

            public Call(String name, ArrayList<Expr.T> inputParams,int lineNumber,Ast.Type.T rt) {
                this.name = name;
                this.inputParams = inputParams;
                this.lineNum = lineNumber;
                this.returnType = rt;
            }
        }

        /** 比较运算表达式 **/
        public static class GT extends T{
            public Ast.Expr.T left,right;

            public GT(Ast.Expr.T left, Ast.Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class LT extends T{
            public Ast.Expr.T left,right;

            public LT(Ast.Expr.T left, Ast.Expr.T right,int lineNum) {
                this.left = left;
                this.right = right;
                this.lineNum = lineNum;
            }
        }

        public static class Number extends T{
            public Ast.Type.T type;
            public Object value;

            public Number(Ast.Type.T t, Object o,int lineNumber) {
                this.type = t;
                this.value = o;
                this.lineNum = lineNumber;
            }
        }

        public static class True extends T{
           public True(int lineNum){
                this.lineNum = lineNum;
           }
        }

        public static class False extends T{
            public False(int lineNum){
                this.lineNum = lineNum;
            }
        }

        public static class Str extends T{
            public String value;
            public Str(String value,int lineNum){
                this.value = value;
                this.lineNum = lineNum;
            }
        }

        public static class Id extends T {
            public String id; // name of the id
            public Type.T type; // type of the id

            public Id(String id, int lineNum)
            {
                this.id = id;
                this.lineNum = lineNum;
            }

            public Id(String id, Type.T type, int lineNum) {
                this.id = id;
                this.type = type;
                this.lineNum = lineNum;
            }
        }

    }


    public static class Method {
        public static abstract class T {

        }

        public static class MethodSingle extends T {
            public Ast.Type.T retType; // 方法返回类型
            public String id;
            public ArrayList<Declare.T> formals;
            public ArrayList<Declare.T> locals;
            public ArrayList<Stmt.T> stms;
            public Expr.T retExp;

            public MethodSingle(Ast.Type.T  retType, String id,
                                ArrayList<Declare.T> formals,
                                ArrayList<Declare.T> locals,
                                ArrayList<Stmt.T> stms,
                                Expr.T retExp) {
                this.retType = retType;
                this.id = id;
                this.formals = formals;
                this.locals = locals;
                this.stms = stms;
                this.retExp = retExp;
            }
        }
    }
}
