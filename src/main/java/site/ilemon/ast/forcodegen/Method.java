package site.ilemon.ast.forcodegen;

import java.util.List;

public class Method {

	 public Type retType;
     public String name;
     public String className;
     public List<Declare> formals;
     public List<Declare> locals;
     public List<Stmt> stms;
     public int index; // number of index
     public int retExp;

     public Method(Type retType, String name, String className,
                         List<Declare> formals,
                         List<Declare> locals,
                         List<Stmt> stms, int retExp, int index)
     {
         this.retType = retType;
         this.name = name;
         this.className = className;
         this.formals = formals;
         this.locals = locals;
         this.stms = stms;
         this.retExp = retExp;
         this.index = index;
     }
}
