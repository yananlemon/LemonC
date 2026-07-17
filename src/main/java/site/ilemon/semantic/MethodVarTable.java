package site.ilemon.semantic;

import site.ilemon.ast.Ast;

import java.util.HashMap;
import java.util.List;
import site.ilemon.exception.SemanticException;

/**
 * Method-level variable table.
 */
public class MethodVarTable {
    private HashMap<String, Symbol> table;

    public MethodVarTable() {
        this.table = new HashMap<String, Symbol>();
    }

    public void put(List<Ast.Declare.Base> formals, List<Ast.Declare.Base> locals) {
        for (Ast.Declare.Base dec : formals) {
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null) {
                throw new SemanticException("重复的参数 " + declareSingle.getId()
                        + " 在行 " + dec.getLineNum());
            }
            this.table.put(declareSingle.getId(), new Symbol(
                    declareSingle.getId(), declareSingle.getType(), Symbol.Kind.PARAMETER, dec.getLineNum()));
        }

        for (Ast.Declare.Base dec : locals) {
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null) {
                throw new SemanticException("重复的变量 " + declareSingle.getId()
                        + " 在行 " + dec.getLineNum());
            }
            this.table.put(declareSingle.getId(), new Symbol(
                    declareSingle.getId(), declareSingle.getType(), Symbol.Kind.LOCAL, dec.getLineNum()));
        }
    }

    public Ast.Type.Base get(String id) {
        Symbol symbol = this.table.get(id);
        return symbol == null ? null : symbol.getType();
    }

    public Symbol resolve(String id) {
        return this.table.get(id);
    }

    public Ast.Type.Base put(String key, Ast.Type.Base value) {
        Symbol previous = this.table.put(key, new Symbol(key, value, Symbol.Kind.LOCAL, -1));
        return previous == null ? null : previous.getType();
    }
}
