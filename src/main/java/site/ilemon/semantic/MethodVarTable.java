package site.ilemon.semantic;

import java.util.Hashtable;
import java.util.List;

import site.ilemon.ast.forparse.Declare;
import site.ilemon.ast.forparse.Type;




/**
 * 方法局部变量表
 * @author andy
 */
public class MethodVarTable{
    private Hashtable<String, Type> table;

    public MethodVarTable()
    {
        this.table = new Hashtable<>();
    }

    public void put(List<Declare> formals, List<Declare> locals){
        for (Declare dec : formals){
            if (this.table.get(dec.name) != null){
                System.out.println("duplicated parameter: " + dec.name +
                        " at line " + dec.lineNumber);
                System.exit(1);
            } else this.table.put(dec.name, dec.type);
        }

        for (Declare dec : locals){
            if (this.table.get(dec.name) != null){
                System.out.println("duplicated variable: " + dec.name +
                        " at line " + dec.lineNumber);
                System.exit(1);
            } else this.table.put(dec.name, dec.type);
        }
    }

    public Type get(String id){
        return this.table.get(id);
    }
}
