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

    public void put(List<Ast.Declare.T> formals, List<Ast.Declare.T> locals) {
        for (Ast.Declare.T dec : formals) {
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null) {
                throw new SemanticException("重复的参数 " + declareSingle.getId()
                        + " 在行 " + dec.getLineNum());
            }
            this.table.put(declareSingle.getId(), new Symbol(
                    declareSingle.getId(), declareSingle.getType(), Symbol.Kind.PARAMETER, dec.getLineNum()));
        }

        for (Ast.Declare.T dec : locals) {
            Ast.Declare.DeclareSingle declareSingle = (Ast.Declare.DeclareSingle) dec;
            if (this.table.get(declareSingle.getId()) != null) {
                throw new SemanticException("重复的变量 " + declareSingle.getId()
                        + " 在行 " + dec.getLineNum());
            }
            this.table.put(declareSingle.getId(), new Symbol(
                    declareSingle.getId(), declareSingle.getType(), Symbol.Kind.LOCAL, dec.getLineNum()));
        }
    }

    public Ast.Type.T get(String id) {
        Symbol symbol = this.table.get(id);
        return symbol == null ? null : symbol.getType();
    }

    public Symbol resolve(String id) {
        return this.table.get(id);
    }

    public Ast.Type.T put(String key, Ast.Type.T value) {
        Symbol previous = this.table.put(key, new Symbol(key, value, Symbol.Kind.LOCAL, -1));
        return previous == null ? null : previous.getType();
    }
}
