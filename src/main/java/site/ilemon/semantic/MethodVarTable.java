package site.ilemon.semantic;

import site.ilemon.ast.Ast;

import java.util.Hashtable;
import java.util.List;

/**
 * 方法局部变量表
 * @author andy
 */
public class MethodVarTable{
    private Hashtable<String, Ast.Type.T> table;

    public MethodVarTable()
    {
        this.table = new Hashtable<>();
    }

    public void put(List<Ast.Declare.T> formals, List<Ast.Declare.T> locals){

        for (Ast.Declare.T dec : formals){
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.id) != null){
                System.out.println("重复的参数: " + declareSingle.id +
                        " 在行 " + dec.lineNum);
                System.exit(1);
            } else this.table.put(declareSingle.id, declareSingle.type);
        }

        for (Ast.Declare.T dec : locals){
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.id) != null){
                System.out.println("重复的变量: " + declareSingle.id +
                        " 在行 " + dec.lineNum);
                System.exit(1);
            } else
                this.table.put(declareSingle.id, declareSingle.type);
        }
    }

    public Ast.Type.T get(String id){
        return this.table.get(id);
    }
    
    public Ast.Type.T put(String key,Ast.Type.T value){
        return this.table.put(key, value);
    }
}