package site.ilemon.codegen.ast;

import site.ilemon.ast.Ast.Type.TypeKind;
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
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }

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
        public static abstract class T {
            public abstract void accept(site.ilemon.codegen.Visitor v);
            public abstract TypeKind getKind();
        }

        public static class ClassType extends T
        {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public String id;

            public ClassType(String id)
            {
                this.id = id;
            }

            @Override
            public TypeKind getKind() { return null; } // ClassType has no standard kind
            @Override
            public String toString()
            {
                return this.id;
            }
        }

        public static class Bool extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.BOOL; }
            @Override
            public String toString()
            {
                return "@bool";
            }
        }

        public static class Int extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.INT; }
            @Override
            public String toString()
            {
                return "@int";
            }
        }

        public static class Float extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.FLOAT; }
            @Override
            public String toString()
            {
                return "@float";
            }
        }

        public static class Double extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.DOUBLE; }
            @Override
            public String toString()
            {
                return "@double";
            }
        }

        public static class Str extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.STRING; }
            @Override
            public String toString()
            {
                return "@string";
            }
        }

        public static class Void extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.VOID; }
            @Override
            public String toString()
            {
                return "@void";
            }
        }

        // 数组类型
        public static class IntArray extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.INT_ARRAY; }
            @Override
            public String toString() { return "@int[]"; }
        }

        public static class FloatArray extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.FLOAT_ARRAY; }
            @Override
            public String toString() { return "@float[]"; }
        }

        public static class DoubleArray extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.DOUBLE_ARRAY; }
            @Override
            public String toString() { return "@double[]"; }
        }

        public static class BoolArray extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
@Override
            public TypeKind getKind() { return TypeKind.BOOL_ARRAY; }
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
        public static abstract class T {
            public abstract void accept(site.ilemon.codegen.Visitor v);
        }

        public static class Aload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Aload(int index)
            {
                this.index = index;
            }
        }

        public static class Areturn extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Astore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Astore(int index) {
                this.index = index;
            }
        }

        public static class Goto extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Goto(Label l)
            {
                this.l = l;
            }
        }



        public static class Iadd extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Isub extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Imul extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Idiv extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Irem extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}


        public static class Fadd extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Fsub extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Fmul extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Fdiv extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Dadd extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Dsub extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Dmul extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Ddiv extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Ificmplt extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Ificmplt(Label l) {
                this.l = l;
            }
        }

        public static class Ificmpgt extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
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
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Ifgt(Label l) {

                this.l = l;
            }
        }


        /**
         * 浮点数比较指令
         */
        public static class Fcmpl extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Fcmpl() {

            }
        }

        public static class Fcmpg extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Fcmpg() {

            }
        }

        public static class Ificmple extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Ificmple(Label l) {
                this.l = l;
            }
        }

        public static class Ificmpge extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Ificmpge(Label l) {

                this.l = l;
            }
        }

        public static class Ificmpeq extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Ificmpeq(Label l) {
                this.l = l;
            }
        }

        public static class Ificmpne extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label l;

            public Ificmpne(Label l) {
                this.l = l;
            }
        }

        public static class Iload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Iload(int index)
            {
                this.index = index;
            }
        }

        public static class Fload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Fload(int index)
            {
                this.index = index;
            }
        }

        public static class Dload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Dload(int index)
            {
                this.index = index;
            }
        }



        public static class Invokestatic extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public String name;
            public List<Type.T> at;
            public Type.T rt;

            public Invokestatic(String name, List<Type.T> at, Type.T rt) {
                this.name = name;
                this.at = at;
                this.rt = rt;
            }
        }

        public static class Ireturn extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Istore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Istore(int index) {
                this.index = index;
            }
        }

        public static class Freturn extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Fstore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Fstore(int index) {
                this.index = index;
            }
        }

        public static class Dreturn extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Dstore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public int index;

            public Dstore(int index) {
                this.index = index;
            }
        }

        public static class Dcmpl extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Dcmpl() {
            }
        }

        public static class Dcmpg extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Dcmpg() {
            }
        }

        public static class F2d extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public F2d() {
            }
        }

        public static class I2f extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public I2f() {
            }
        }

        public static class I2d extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public I2d() {
            }
        }


        public static class LabelJ extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Label label;

            public LabelJ(Label label)
            {
                this.label = label;
            }
        }

        public static class Ldc extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Object i;

            public Ldc(Object i)
            {
                this.i = i;
            }
        }



        public static class Printf extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Type.T exprType;

            public String v;

            public Printf(Type.T t,String v) {
                this.exprType = t;
                this.v = v;
            }
        }

        public static class PrintLine extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Pop extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        public static class Pop2 extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // ========== 数组相关指令 ==========

        // 创建数组: newarray int/float/double/boolean
        public static class Newarray extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
public Type.T elementType;
            public Newarray(Type.T elementType) {
                this.elementType = elementType;
            }
        }

        // int数组加载: iaload
        public static class Iaload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // int数组存储: iastore
        public static class Iastore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // float数组加载: faload
        public static class Faload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // float数组存储: fastore
        public static class Fastore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // double数组加载: daload
        public static class Daload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // double数组存储: dastore
        public static class Dastore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // boolean数组加载: baload
        public static class Baload extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // boolean数组存储: bastore
        public static class Bastore extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

        // 数组长度: arraylength
        public static class Arraylength extends T {
            public void accept(site.ilemon.codegen.Visitor v) { v.visit(this); }
}

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
