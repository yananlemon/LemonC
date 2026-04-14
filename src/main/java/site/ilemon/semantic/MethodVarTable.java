package site.ilemon.semantic;

import site.ilemon.ast.Ast;

import java.util.HashMap;
import java.util.List;
import site.ilemon.exception.SemanticException;

/**
 * 方法局部变量表
 * @author andy
 */
public class MethodVarTable{
    private HashMap<String, Ast.Type.T> table;

    public MethodVarTable()
    {
        this.table = new HashMap<>();
    }

    public void put(List<Ast.Declare.T> formals, List<Ast.Declare.T> locals){

        for (Ast.Declare.T dec : formals){
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null){
                throw new SemanticException("重复的参数: " + declareSingle.getId() +
                        " 在行 " + dec.getLineNum());
            } else this.table.put(declareSingle.getId(), declareSingle.getType());
        }

        for (Ast.Declare.T dec : locals){
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null){
                throw new SemanticException("重复的变量: " + declareSingle.getId() +
                        " 在行 " + dec.getLineNum());
            } else
                this.table.put(declareSingle.getId(), declareSingle.getType());
        }
    }

    public Ast.Type.T get(String id){
        return this.table.get(id);
    }
    
    public Ast.Type.T put(String key,Ast.Type.T value){
        return this.table.put(key, value);
    }
}