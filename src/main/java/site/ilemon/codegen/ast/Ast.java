package site.ilemon.codegen.ast;

import java.util.LinkedList;

public class Ast
{
    public static class Type
    {
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
        
        public static class Void extends T
        {

            public Void()
            {
            }

            @Override
            public String toString()
            {
                return "@void";
            }
        }

        public static class Int extends T
        {
            @Override
            public String toString()
            {
                return "@int";
            }
        }
        
        public static class Str extends T
        {
            @Override
            public String toString()
            {
                return "@string";
            }
        }
    }

    public static class Dec
    {
        public static class DecSingle
        {
            public Type.T type;
            public String id;

            public DecSingle(Type.T type, String id)
            {
                this.type = type;
                this.id = id;
            }
        }
    }

    public static class Stm
    {
        public static abstract class T {}

        public static class Aload extends T
        {
            public int index;

            public Aload(int index)
            {
                this.index = index;
            }
        }

        public static class Areturn extends T {}

        public static class Astore extends T
        {
            public int index;

            public Astore(int index)
            {
                this.index = index;
            }
        }

        public static class Goto extends T
        {
            public Label l;

            public Goto(Label l)
            {
                this.l = l;
            }
        }

        public static class Getfield extends T
        {
            public String fieldSpec, descriptor;

            public Getfield(String fieldSpec, String descriptor)
            {
                this.fieldSpec = fieldSpec;
                this.descriptor = descriptor;
            }
        }

        public static class Iadd extends T {}

        public static class Ificmplt extends T
        {
            public Label l;

            public Ificmplt(Label l)
            {
                this.l = l;
            }
        }
        
        public static class Ificmpgt extends T
        {
            public Label l;

            public Ificmpgt(Label l)
            {
                this.l = l;
            }
        }

        public static class Iload extends T
        {
            public int index;

            public Iload(int index)
            {
                this.index = index;
            }
        }

        public static class Imul extends T {}

        public static class Invokevirtual extends T
        {
            public String f;
            public String c;
            public LinkedList<Type.T> at;
            public Type.T rt;

            public Invokevirtual(String f, String c, LinkedList<Type.T> at, Type.T rt)
            {
                this.f = f;
                this.c = c;
                this.at = at;
                this.rt = rt;
            }
        }

        public static class Ireturn extends T {}

        // 用于存储数字
        public static class Istore extends T
        {
            public int index;

            public Istore(int index)
            {
                this.index = index;
            }
        }
        
        
       /* // 用于存储字符串
        public static class Astore extends T
        {
            public int index;

            public Istore(int index)
            {
                this.index = index;
            }
        }*/

        
        
        public static class Isub extends T {}

        public static class LabelJ extends T
        {
            public Label label;

            public LabelJ(Label label)
            {
                this.label = label;
            }
        }

        public static class Ldc extends T
        {
            public Object i;

            public Ldc(Object i)
            {
                this.i = i;
            }
        }

        public static class New extends T
        {
            public String c;

            public New(String c)
            {
                this.c = c;
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
        
        public static class PrintNewLine extends T {
        	
        }

        public static class Putfield extends T
        {
            public String fieldSpec, descriptor;

            public Putfield(String fieldSpec, String descriptor)
            {
                this.fieldSpec = fieldSpec;
                this.descriptor = descriptor;
            }
        }
    }

    public static class Method
    {
        public static class MethodSingle
        {
            public Type.T retType;
            public String id;
            public String classId;
            public LinkedList<Dec.DecSingle> formals;
            public LinkedList<Dec.DecSingle> locals;
            public LinkedList<Stm.T> stms;
            public int index; // number of index
            public int retExp;

            public MethodSingle(Type.T retType, String id, String classId,
                                LinkedList<Dec.DecSingle> formals,
                                LinkedList<Dec.DecSingle> locals,
                                LinkedList<Stm.T> stms, int retExp, int index)
            {
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

    public static class Class
    {
        public static class ClassSingle
        {
            public String id;
            public String base;
            public LinkedList<Dec.DecSingle> fields;
            public LinkedList<Method.MethodSingle> methods;

            public ClassSingle(String id, String base,
                               LinkedList<Dec.DecSingle> fields,
                               LinkedList<Method.MethodSingle> methods)
            {
                this.id = id;
                this.base = base;
                this.fields = fields;
                this.methods = methods;
            }
        }
    }

    public static class MainClass
    {
        public static class MainClassSingle
        {
            public String id;
            public LinkedList<Stm.T> stms;
            public int varLocalCount;

            public MainClassSingle(String id,
                                   LinkedList<Stm.T> stms,int varLocalCount)
            {
                this.id = id;
                this.stms = stms;
                this.varLocalCount = varLocalCount;
            }
        }
    }

    public static class Program
    {
        public static class ProgramSingle
        {
            public MainClass.MainClassSingle mainClass;
            public LinkedList<Class.ClassSingle> classes;

            public ProgramSingle(MainClass.MainClassSingle mainClass,
                                 LinkedList<Class.ClassSingle> classes)
            {
                this.mainClass = mainClass;
                this.classes = classes;
            }
        }
    }
}
