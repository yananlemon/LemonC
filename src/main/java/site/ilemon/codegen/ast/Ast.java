package site.ilemon.codegen.ast;

import java.util.List;

/**
 * Created by andy on 2019/8/5.
 */
public class Ast {

    // program
    public static class Program {

        public static class T{

        }

        public static class ProgramSingle extends T{
            public MainClass.MainClassSingle mainClass;

            public ProgramSingle(MainClass.MainClassSingle mainClass) {
                this.mainClass = mainClass;
            }
        }
    }

    // MainClass
    public static class MainClass {
        public static class MainClassSingle {
            public List<Method.MethodSingle> methods;
            public String id;

            public MainClassSingle(String id,List<Method.MethodSingle> methods) {
                this.id = id;
                this.methods = methods;
            }
        }
    }

    // Type
    public static class Type {
        public static abstract class T {}

        public static class ClassType extends T
        {
            public String id;

            public ClassType(String id)
            {
                this.id = id;
            }

            @Override
            public String toString()
            {
                return this.id;
            }
        }

        public static class Bool extends T {
            @Override
            public String toString()
            {
                return "@bool";
            }
        }

        public static class Int extends T {
            @Override
            public String toString()
            {
                return "@int";
            }
        }

        public static class Float extends T {
            @Override
            public String toString()
            {
                return "@float";
            }
        }

        public static class Double extends T {
            @Override
            public String toString()
            {
                return "@double";
            }
        }

        public static class Str extends T {
            @Override
            public String toString()
            {
                return "@string";
            }
        }

        public static class Void extends T {
            @Override
            public String toString()
            {
                return "@void";
            }
        }

        // 数组类型
        public static class IntArray extends T {
            @Override
            public String toString() { return "@int[]"; }
        }

        public static class FloatArray extends T {
            @Override
            public String toString() { return "@float[]"; }
        }

        public static class DoubleArray extends T {
            @Override
            public String toString() { return "@double[]"; }
        }

        public static class BoolArray extends T {
            @Override
            public String toString() { return "@bool[]"; }
        }
    }

    // Declare
    public static class Declare {
        public static class DeclareSingle
        {
            public Type.T type;
            public String id;

            public DeclareSingle(Type.T type, String id)
            {
                this.type = type;
                this.id = id;
            }
        }
    }

    //Stmt
    public static class Stmt {
        public static abstract class T {}

        public static class Aload extends T {
            public int index;

            public Aload(int index)
            {
                this.index = index;
            }
        }

        public static class Areturn extends T {

        }

        public static class Astore extends T {
            public int index;

            public Astore(int index) {
                this.index = index;
            }
        }

        public static class Goto extends T {
            public Label l;

            public Goto(Label l)
            {
                this.l = l;
            }
        }



        public static class Iadd extends T {

        }

        public static class Isub extends T {

        }

        public static class Imul extends T {

        }

        public static class Idiv extends T {

        }


        public static class Fadd extends T {

        }

        public static class Fsub extends T {

        }

        public static class Fmul extends T {

        }

        public static class Fdiv extends T {

        }

        public static class Dadd extends T {

        }

        public static class Dsub extends T {

        }

        public static class Dmul extends T {

        }

        public static class Ddiv extends T {

        }

        public static class Ificmplt extends T {
            public Label l;

            public Ificmplt(Label l) {
                this.l = l;
            }
        }

        public static class Ificmpgt extends T {
            public Label l;

            public Ificmpgt(Label l) {

                this.l = l;
            }
        }

        /**
         * ifgt
         *  当栈顶int型数值大于0时跳转
         */
        public static class Ifgt extends T {
            public Label l;

            public Ifgt(Label l) {

                this.l = l;
            }
        }


        /**
         * 浮点数比较指令
         */
        public static class Fcmpl extends T {
            public Fcmpl() {

            }
        }

        public static class Ificmplet extends T {
            public Label l;

            public Ificmplet(Label l) {
                this.l = l;
            }
        }

        public static class Ificmpget extends T {
            public Label l;

            public Ificmpget(Label l) {

                this.l = l;
            }
        }

        public static class Ificmpeq extends T {
            public Label l;

            public Ificmpeq(Label l) {
                this.l = l;
            }
        }

        public static class Ificmpne extends T {
            public Label l;

            public Ificmpne(Label l) {
                this.l = l;
            }
        }

        public static class Iload extends T {
            public int index;

            public Iload(int index)
            {
                this.index = index;
            }
        }

        public static class Fload extends T {
            public int index;

            public Fload(int index)
            {
                this.index = index;
            }
        }

        public static class Dload extends T {
            public int index;

            public Dload(int index)
            {
                this.index = index;
            }
        }



        public static class Invokevirtual extends T {
            public String name;
            public List<Type.T> at;
            public Type.T rt;

            public Invokevirtual(String name, List<Type.T> at, Type.T rt) {
                this.name = name;
                this.at = at;
                this.rt = rt;
            }
        }

        public static class Ireturn extends T {}

        public static class Istore extends T {
            public int index;

            public Istore(int index) {
                this.index = index;
            }
        }

        public static class Freturn extends T {}

        public static class Fstore extends T {
            public int index;

            public Fstore(int index) {
                this.index = index;
            }
        }

        public static class Dreturn extends T {}

        public static class Dstore extends T {
            public int index;

            public Dstore(int index) {
                this.index = index;
            }
        }

        public static class Dcmpl extends T {
            public Dcmpl() {
            }
        }

        public static class F2d extends T {
            public F2d() {
            }
        }


        public static class LabelJ extends T {
            public Label label;

            public LabelJ(Label label)
            {
                this.label = label;
            }
        }

        public static class Ldc extends T {
            public Object i;

            public Ldc(Object i)
            {
                this.i = i;
            }
        }



        public static class Printf extends T {
            public Type.T exprType;

            public String v;

            public Printf(Type.T t,String v) {
                this.exprType = t;
                this.v = v;
            }
        }

        public static class PrintLine extends T {

        }

        // ========== 数组相关指令 ==========

        // 创建数组: newarray int/float/double/boolean
        public static class Newarray extends T {
            public Type.T elementType;
            public Newarray(Type.T elementType) {
                this.elementType = elementType;
            }
        }

        // int数组加载: iaload
        public static class Iaload extends T {}

        // int数组存储: iastore
        public static class Iastore extends T {}

        // float数组加载: faload
        public static class Faload extends T {}

        // float数组存储: fastore
        public static class Fastore extends T {}

        // double数组加载: daload
        public static class Daload extends T {}

        // double数组存储: dastore
        public static class Dastore extends T {}

        // boolean数组加载: baload
        public static class Baload extends T {}

        // boolean数组存储: bastore
        public static class Bastore extends T {}

        // 数组长度: arraylength
        public static class Arraylength extends T {}

    }

    public static class Method
    {
        public static class MethodSingle
        {
            public Type.T retType;
            public String id;
            public String classId;
            public List<Declare.DeclareSingle> formals;
            public List<Declare.DeclareSingle> locals;
            public List<Stmt.T> stms;
            public int index; // number of index
            public int retExp;

            public MethodSingle(Type.T retType, String id, String classId,
                                List<Declare.DeclareSingle> formals,
                                List<Declare.DeclareSingle> locals,
                                List<Stmt.T> stms, int retExp, int index) {
                this.retType = retType;
                this.id = id;
                this.classId = classId;
                this.formals = formals;
                this.locals = locals;
                this.stms = stms;
                this.retExp = retExp;
                this.index = index;
            }
        }
    }

}
