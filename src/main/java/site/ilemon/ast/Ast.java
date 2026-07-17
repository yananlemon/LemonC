package site.ilemon.ast;

import site.ilemon.codegen.ast.Label;
import site.ilemon.list.DoublyLinkedList;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;

/**
 * Created by andy on 2019/7/31.
 */
public class Ast {

    /**
     * Program
     */
    public static class Program{
        public static abstract class Base{
            public abstract void accept(ISemanticVisitor v);
        }
        public static class ProgramSingle extends Base{
            private final MainClass.Base mainClass;
            public MainClass.Base getMainClass() { return this.mainClass; }

            public ProgramSingle(MainClass.Base mainClass) {
                this.mainClass = mainClass;
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }
    }

    /**
     * MainClass
     */
    public static class MainClass{
        public static abstract class Base{
            public abstract void accept(ISemanticVisitor v);
        }
        public static class MainClassSingle extends Base {
            private final String classId;
            public String getClassId() { return this.classId; }
            private final ArrayList<Ast.Method.Base> methods;
            public ArrayList<Ast.Method.Base> getMethods() { return this.methods; }

            public MainClassSingle(String classId, ArrayList<Ast.Method.Base> methods) {
                this.classId = classId;
                this.methods = methods;
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }
    }

    /**
     * Stmt
     */
    public static class Stmt{
        public static abstract class Base{
            // breakList and continueList removed
            private int lineNum;
            public int getLineNum() { return this.lineNum; }
            public void setLineNum(int lineNum) { this.lineNum = lineNum; }
            public abstract void accept(ISemanticVisitor v);
        }

        public static class Break extends Base {
            public Break(int lineNum) { this.setLineNum(lineNum); }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class Continue extends Base {
            public Continue(int lineNum) { this.setLineNum(lineNum); }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }
        public static class Assign extends Base {
            private final Ast.Expr.Id id;
            public Ast.Expr.Id getId() { return this.id; }
            private Expr.Base expr;
            public Expr.Base getExpr() { return this.expr; }
            public void setExpr(Expr.Base expr) { this.expr = expr; }
            //private Type.Base type;
            public Assign(Ast.Expr.Id id, Expr.Base exp, int lineNum) {
                this.id = id;
                this.expr = exp;
                //this.type = type;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class VarDecl extends Base {
            private final Ast.Declare.DeclareSingle declaration;
            private final Ast.Expr.Base initializer;

            public Ast.Declare.DeclareSingle getDeclaration() { return this.declaration; }
            public Ast.Expr.Base getInitializer() { return this.initializer; }

            public VarDecl(Ast.Declare.DeclareSingle declaration, Ast.Expr.Base initializer) {
                this.declaration = declaration;
                this.initializer = initializer;
                this.setLineNum(declaration.getLineNum());
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Block extends Base{
            private final ArrayList<Base> stmts;
            public ArrayList<Base> getStmts() { return this.stmts; }

            public Block(ArrayList<Base> stmts,int lineNum) {
                this.stmts = stmts;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Call extends Base {
            /**方法返回类型**/
            private Ast.Type.Base returnType;
            public Ast.Type.Base getReturnType() { return this.returnType; }
            public void setReturnType(Ast.Type.Base returnType) { this.returnType = returnType; }

            /**方法名称**/
            private final String name;
            public String getName() { return this.name; }

            /**方法参数**/
            private final ArrayList<Expr.Base> inputParams;
            public ArrayList<Expr.Base> getInputParams() { return this.inputParams; }

            public Call(String name, ArrayList<Expr.Base> inputParams,int lineNumber) {
                this.name = name;
                this.inputParams = inputParams;
                this.setLineNum(lineNumber);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class If extends Base{
            private final Expr.Base condition;
            public Expr.Base getCondition() { return this.condition; }
            private final Base thenStmt;
            private final Base elseStmt;
            public Base getThenStmt() { return this.thenStmt; }
            public Base getElseStmt() { return this.elseStmt; }

            public If(Expr.Base condition,Base thenStmt,Base elseStmt,int lineNum) {
                this.condition = condition;
                this.thenStmt = thenStmt;
                this.elseStmt = elseStmt;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Printf extends Base {
            private final String format;
            public String getFormat() { return this.format; }
            private final ArrayList<Ast.Expr.Base> exprs;
            public ArrayList<Ast.Expr.Base> getExprs() { return this.exprs; }

            public Printf(String format,ArrayList<Ast.Expr.Base> exprs, int lineNum) {
                this.format = format;
                this.exprs = exprs;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Return extends Base {
            private final Ast.Expr.Base expr;
            public Ast.Expr.Base getExpr() { return this.expr; }

            public Return(Expr.Base expr, int lineNum) {
                this.expr = expr;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class While extends Base {
            private final Ast.Expr.Base condition;
            public Ast.Expr.Base getCondition() { return this.condition; }
            private final Ast.Stmt.Base body;
            public Ast.Stmt.Base getBody() { return this.body; }

            public While(Ast.Expr.Base condition, Ast.Stmt.Base body, int lineNum)
            {
                this.condition = condition;
                this.body = body;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class For extends Base {
            private Ast.Stmt.Base init;
            public Ast.Stmt.Base getInit() { return this.init; }
            private Ast.Expr.Base condition;
            public Ast.Expr.Base getCondition() { return this.condition; }
            private Ast.Stmt.Base update;
            public Ast.Stmt.Base getUpdate() { return this.update; }
            private Ast.Stmt.Base body;
            public Ast.Stmt.Base getBody() { return this.body; }

            public For(Ast.Stmt.Base init, Ast.Expr.Base condition, Ast.Stmt.Base update,
                       Ast.Stmt.Base body, int lineNum) {
                this.init = init;
                this.condition = condition;
                this.update = update;
                this.body = body;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class PrintLine extends Base {
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // 数组赋值语句: arr[index] = expr;
        public static class ArrayAssign extends Base {
            private final String arrayName;
            public String getArrayName() { return this.arrayName; }
            private final Expr.Base index;
            public Expr.Base getIndex() { return this.index; }
            private final Expr.Base expr;
            public Expr.Base getExpr() { return this.expr; }
            private Type.Base elementType;
            public Type.Base getElementType() { return this.elementType; }
            public void setElementType(Type.Base elementType) { this.elementType = elementType; }

            public ArrayAssign(String arrayName, Expr.Base index, Expr.Base expr, int lineNum) {
                this.arrayName = arrayName;
                this.index = index;
                this.expr = expr;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }
    }

    /**
     * Declare
     */
    public static class Declare{
        public static abstract class Base{
            private int lineNum;
            public int getLineNum() { return this.lineNum; }
            public void setLineNum(int lineNum) { this.lineNum = lineNum; }
            public abstract void accept(ISemanticVisitor v);
        }
        public static class DeclareSingle extends Base {
            private final Type.Base type;
            public Type.Base getType() { return this.type; }
            private final String id;
            public String getId() { return this.id; }

            public DeclareSingle(Type.Base type, String id,int lineNum) {
                this.type = type;
                this.id = id;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }
    }

    /**
     * Type
     */
    public static class Type{
        /**
         * 类型枚举，用于类型安全的比较（替代 toString().equals() 方式）
         */
        public enum TypeKind {
            INT, FLOAT, DOUBLE, BOOL, STRING, VOID,
            INT_ARRAY, FLOAT_ARRAY, DOUBLE_ARRAY, BOOL_ARRAY,
            ERROR
        }

        public static abstract class Base{
            public abstract void accept(ISemanticVisitor v);
            public abstract TypeKind getKind();
        }
        public static class Void extends Base {
            @Override
            public TypeKind getKind() { return TypeKind.VOID; }
            @Override
            public String toString() {
                return "@void";
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }
        public static final class Error extends Base {
            public static final Error INSTANCE = new Error();

            private Error() {
            }

            @Override
            public TypeKind getKind() { return TypeKind.ERROR; }

            @Override
            public String toString() { return "@error"; }

            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }
        public static class Int extends Base {
            @Override
            public TypeKind getKind() { return TypeKind.INT; }
            @Override
            public String toString() {
                return "@int";
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Float extends Base {
            @Override
            public TypeKind getKind() { return TypeKind.FLOAT; }
            @Override
            public String toString() {
                return "@float";
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Double extends Base {
            @Override
            public TypeKind getKind() { return TypeKind.DOUBLE; }
            @Override
            public String toString() {
                return "@double";
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Str extends Base {
            @Override
            public TypeKind getKind() { return TypeKind.STRING; }
            @Override
            public String toString() {
                return "@str";
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Bool extends Base {
            @Override
            public TypeKind getKind() { return TypeKind.BOOL; }
            @Override
            public String toString() {
                return "@bool";
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // 数组类型
        public static class IntArray extends Base {
            private int size;
            public int getSize() { return this.size; }
            public void setSize(int size) { this.size = size; }
            public IntArray() { this.size = -1; }
            public IntArray(int size) { this.size = size; }
            @Override
            public TypeKind getKind() { return TypeKind.INT_ARRAY; }
            @Override
            public String toString() { return "@int[]"; }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class FloatArray extends Base {
            private int size;
            public int getSize() { return this.size; }
            public void setSize(int size) { this.size = size; }
            public FloatArray() { this.size = -1; }
            public FloatArray(int size) { this.size = size; }
            @Override
            public TypeKind getKind() { return TypeKind.FLOAT_ARRAY; }
            @Override
            public String toString() { return "@float[]"; }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class DoubleArray extends Base {
            private int size;
            public int getSize() { return this.size; }
            public void setSize(int size) { this.size = size; }
            public DoubleArray() { this.size = -1; }
            public DoubleArray(int size) { this.size = size; }
            @Override
            public TypeKind getKind() { return TypeKind.DOUBLE_ARRAY; }
            @Override
            public String toString() { return "@double[]"; }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class BoolArray extends Base {
            private int size;
            public int getSize() { return this.size; }
            public void setSize(int size) { this.size = size; }
            public BoolArray() { this.size = -1; }
            public BoolArray(int size) { this.size = size; }
            @Override
            public TypeKind getKind() { return TypeKind.BOOL_ARRAY; }
            @Override
            public String toString() { return "@bool[]"; }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }
    }


    /**
     * Expression
     */
    public static class Expr {
        public static abstract class Base {
            private int lineNum;
            public int getLineNum() { return this.lineNum; }
            public void setLineNum(int lineNum) { this.lineNum = lineNum; }

            public abstract void accept(ISemanticVisitor v);
        }

        public static abstract class BinaryExpr extends Base {
            private Ast.Expr.Base left;
            private Ast.Expr.Base right;
            public Ast.Expr.Base getLeft() { return this.left; }
            public void setLeft(Ast.Expr.Base left) { this.left = left; }
            public Ast.Expr.Base getRight() { return this.right; }
            public void setRight(Ast.Expr.Base right) { this.right = right; }
            public BinaryExpr(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                this.left = left;
                this.right = right;
                this.setLineNum(lineNum);
            }
        }

        /** 四则运算表达式 **/
        public static class Add extends BinaryExpr {
            public Add(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Sub extends BinaryExpr {
            public Sub(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Mul extends BinaryExpr {
            public Mul(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Div extends BinaryExpr {
            public Div(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Mod extends BinaryExpr {
            public Mod(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        /** 逻辑运算表达式 **/
        public static class And extends BinaryExpr {
            public And(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Or extends BinaryExpr {
            public Or(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Not extends Base{
            private final Expr.Base expr;
            public Expr.Base getExpr() { return this.expr; }

            public Not(Expr.Base expr) {
                this(expr, expr == null ? 0 : expr.getLineNum());
            }

            public Not(Expr.Base expr, int lineNum) {
                this.expr = expr;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class UnaryMinus extends Base{
            private final Expr.Base expr;
            public Expr.Base getExpr() { return this.expr; }
            public UnaryMinus(Expr.Base expr, int lineNumber) {
                this.expr = expr;
                this.setLineNum(lineNumber);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Call extends Base{
            /**方法返回类型**/
            private Ast.Type.Base returnType;
            public Ast.Type.Base getReturnType() { return this.returnType; }
            public void setReturnType(Ast.Type.Base returnType) { this.returnType = returnType; }

            /**方法名称**/
            private final String name;
            public String getName() { return this.name; }

            /**方法参数**/
            private final ArrayList<Expr.Base> inputParams;
            public ArrayList<Expr.Base> getInputParams() { return this.inputParams; }

            public Call(String name, ArrayList<Expr.Base> inputParams,int lineNumber) {
                this.name = name;
                this.inputParams = inputParams;
                this.setLineNum(lineNumber);
            }

            public Call(String name, ArrayList<Expr.Base> inputParams,int lineNumber,Ast.Type.Base rt) {
                this.name = name;
                this.inputParams = inputParams;
                this.setLineNum(lineNumber);
                this.returnType = rt;
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        /** 比较运算表达式 **/
        // >
        public static class GT extends BinaryExpr {
            public GT(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // <
        public static class LT extends BinaryExpr {
            public LT(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // >=
        public static class GTE extends BinaryExpr {
            public GTE(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // <=
        public static class LTE extends BinaryExpr {
            public LTE(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // ==
        public static class EQ extends BinaryExpr {
            public EQ(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // !=
        public static class NEQ extends BinaryExpr {
            public NEQ(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
                super(left, right, lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class IntLiteral extends Base {
            private final Integer value;
            private String rawValue;
            public Integer getValue() { return this.value; }
            public String getRawValue() { return this.rawValue != null ? this.rawValue : String.valueOf(value); }

            public IntLiteral(Integer value, int lineNum) {
                this.value = value;
                this.rawValue = String.valueOf(value);
                this.setLineNum(lineNum);
            }

            public IntLiteral(String rawValue, int lineNum) {
                this.rawValue = rawValue;
                this.value = site.ilemon.lexer.IntegerLiterals.parse(rawValue);
                this.setLineNum(lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class FloatLiteral extends Base {
            private final Float value;
            private String rawValue;
            public Float getValue() { return this.value; }
            public String getRawValue() { return this.rawValue != null ? this.rawValue : String.valueOf(value); }

            public FloatLiteral(Float value, int lineNum) {
                this.value = value;
                this.rawValue = String.valueOf(value);
                this.setLineNum(lineNum);
            }

            public FloatLiteral(String rawValue, int lineNum) {
                this.rawValue = rawValue;
                this.value = Float.parseFloat(rawValue);
                this.setLineNum(lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class DoubleLiteral extends Base {
            private final Double value;
            private String rawValue;
            public Double getValue() { return this.value; }
            public String getRawValue() { return this.rawValue != null ? this.rawValue : String.valueOf(value); }

            public DoubleLiteral(Double value, int lineNum) {
                this.value = value;
                this.rawValue = String.valueOf(value);
                this.setLineNum(lineNum);
            }

            public DoubleLiteral(String rawValue, int lineNum) {
                this.rawValue = rawValue;
                this.value = Double.parseDouble(rawValue);
                this.setLineNum(lineNum);
            }
            @Override
            public void accept(ISemanticVisitor v) { v.visit(this); }
        }

        public static class True extends Base{
           public True(int lineNum){
                this.setLineNum(lineNum);
           }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class False extends Base{
            public False(int lineNum){
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Str extends Base{
            private final String value;
            public String getValue() { return this.value; }
            public Str(String value,int lineNum){
                this.value = value;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        public static class Id extends Base {
            private final String id;
            public String getId() { return this.id; }
            private Type.Base type;
            public Type.Base getType() { return this.type; }
            public void setType(Type.Base type) { this.type = type; }

            public Id(String id, int lineNum)
            {
                this.id = id;
                this.setLineNum(lineNum);
            }

            public Id(String id, Type.Base type, int lineNum) {
                this.id = id;
                this.type = type;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // 数组访问表达式: arr[index]
        public static class ArrayAccess extends Base {
            private final String arrayName;
            public String getArrayName() { return this.arrayName; }
            private final Expr.Base index;
            public Expr.Base getIndex() { return this.index; }
            private Type.Base elementType;
            public Type.Base getElementType() { return this.elementType; }
            public void setElementType(Type.Base elementType) { this.elementType = elementType; }

            public ArrayAccess(String arrayName, Expr.Base index, int lineNum) {
                this.arrayName = arrayName;
                this.index = index;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

        // 数组长度表达式: arr.length
        public static class ArrayLength extends Base {
            private final String arrayName;
            public String getArrayName() { return this.arrayName; }

            public ArrayLength(String arrayName, int lineNum) {
                this.arrayName = arrayName;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }

    }


    public static class Method {
        public static abstract class Base {
            private int lineNum;
            public int getLineNum() { return this.lineNum; }
            public void setLineNum(int lineNum) { this.lineNum = lineNum; }
            public abstract void accept(ISemanticVisitor v);
        }

        public static class MethodSingle extends Base {
            private Ast.Type.Base retType;
            public Ast.Type.Base getRetType() { return this.retType; }
            public void setRetType(Ast.Type.Base retType) { this.retType = retType; }
            private String id;
            public String getId() { return this.id; }
            public void setId(String id) { this.id = id; }
            private ArrayList<Declare.Base> formals;
            public ArrayList<Declare.Base> getFormals() { return this.formals; }
            public void setFormals(ArrayList<Declare.Base> formals) { this.formals = formals; }
            private ArrayList<Declare.Base> locals;
            public ArrayList<Declare.Base> getLocals() { return this.locals; }
            public void setLocals(ArrayList<Declare.Base> locals) { this.locals = locals; }
            private ArrayList<Stmt.Base> stms;
            public ArrayList<Stmt.Base> getStms() { return this.stms; }
            public void setStms(ArrayList<Stmt.Base> stms) { this.stms = stms; }
            private Ast.Stmt.Base retExp;
            public Ast.Stmt.Base getRetExp() { return this.retExp; }
            public void setRetExp(Ast.Stmt.Base retExp) { this.retExp = retExp; }

            public MethodSingle(Ast.Type.Base  retType, String id,
                                ArrayList<Declare.Base> formals,
                                ArrayList<Declare.Base> locals,
                                ArrayList<Stmt.Base> stms,
                                Ast.Stmt.Base retExp,int lineNum) {
                this.retType = retType;
                this.id = id;
                this.formals = formals;
                this.locals = locals;
                this.stms = stms;
                this.retExp = retExp;
                this.setLineNum(lineNum);
            }

            @Override
            public void accept(ISemanticVisitor v) {
                v.visit(this);
            }
        }
    }
}
