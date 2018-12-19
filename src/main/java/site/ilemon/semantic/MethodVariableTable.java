package site.ilemon.semantic;

import java.util.Hashtable;
import java.util.List;

import site.ilemon.parser.Ast;


/**
 * 方法局部变量表
 * @author andy
 * @version 1.0
 */
public class MethodVariableTable{
    private Hashtable<String, Ast.Type.T> table;

    public MethodVariableTable()
    {
        this.table = new Hashtable<>();
    }

    public void put(List<Ast.Declare.T> formals, List<Ast.Declare.T> locals)
    {
        for (Ast.Declare.T dec : formals)
        {
            Ast.Declare.DeclareSingle decc = ((Ast.Declare.DeclareSingle) dec);
            if (this.table.get(decc.id) != null)
            {
                System.out.println("duplicated parameter: " + decc.id +
                        " at line " + decc.lineNumber);
                System.exit(1);
            } else this.table.put(decc.id, decc.type);
        }

        for (Ast.Declare.T dec : locals)
        {
            Ast.Declare.DeclareSingle decc = ((Ast.Declare.DeclareSingle) dec);
            if (this.table.get(decc.id) != null)
            {
                System.out.println("duplicated variable: " + decc.id +
                        " at line " + decc.lineNumber);
                System.exit(1);
            } else this.table.put(decc.id, decc.type);
        }
    }

    public Ast.Type.T get(String id)
    {
        return this.table.get(id);
    }
}
