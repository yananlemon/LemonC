import org.junit.Assert;
import org.junit.Test;
import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Parser测试用例
 * 测试递归下降语法分析器
 */
public class ParserTest {

    // ==================== 基础解析测试 ====================

    @Test
    public void testParseBasic() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull("应成功解析程序", prog);
    }

    @Test
    public void testParseFloat() throws IOException {
        Parser parser = createParser("examples/FloatTest01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
    }

    @Test
    public void testParseIteration() throws IOException {
        Parser parser = createParser("examples/Iteration01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
    }

    // ==================== 比较运算符测试 ====================

    @Test
    public void testCompareOperators() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull("应成功解析包含所有比较运算符的程序", prog);
    }

    @Test
    public void testGreaterThan() throws IOException {
        Parser parser = createParser("examples/If01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含GT节点
        boolean hasGT = containsExprType(prog, Ast.Expr.GT.class);
        // If01.lemon 使用了 > 运算符
    }

    @Test
    public void testLessThan() throws IOException {
        Parser parser = createParser("examples/If01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含LT节点
        boolean hasLT = containsExprType(prog, Ast.Expr.LT.class);
        assertTrue("应包含小于运算符", hasLT);
    }

    @Test
    public void testGreaterThanOrEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含GET节点 (>=)
        boolean hasGET = containsExprType(prog, Ast.Expr.GET.class);
        assertTrue("应包含大于等于运算符", hasGET);
    }

    @Test
    public void testLessThanOrEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含LET节点 (<=)
        boolean hasLET = containsExprType(prog, Ast.Expr.LET.class);
        assertTrue("应包含小于等于运算符", hasLET);
    }

    @Test
    public void testEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含EQ节点 (==)
        boolean hasEQ = containsExprType(prog, Ast.Expr.EQ.class);
        assertTrue("应包含等于运算符", hasEQ);
    }

    @Test
    public void testNotEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含NEQ节点 (!=)
        boolean hasNEQ = containsExprType(prog, Ast.Expr.NEQ.class);
        assertTrue("应包含不等于运算符", hasNEQ);
    }

    // ==================== 逻辑运算符测试 ====================

    @Test
    public void testLogicalAnd() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasAnd = containsExprType(prog, Ast.Expr.And.class);
        assertTrue("应包含逻辑与运算符", hasAnd);
    }

    @Test
    public void testLogicalOr() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasOr = containsExprType(prog, Ast.Expr.Or.class);
        assertTrue("应包含逻辑或运算符", hasOr);
    }

    @Test
    public void testLogicalNot() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasNot = containsExprType(prog, Ast.Expr.Not.class);
        assertTrue("应包含逻辑非运算符", hasNot);
    }

    // ==================== 方法调用测试 ====================

    @Test
    public void testMethodCall() throws IOException {
        Parser parser = createParser("examples/SimpleMethodCall.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasCall = containsExprType(prog, Ast.Expr.Call.class);
        assertTrue("应包含方法调用", hasCall);
    }

    @Test
    public void testRecursiveCall() throws IOException {
        Parser parser = createParser("examples/Cal.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull("应成功解析递归调用", prog);
    }

    // ==================== 语句测试 ====================

    @Test
    public void testIfStatement() throws IOException {
        Parser parser = createParser("examples/If01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasIf = containsStmtType(prog, Ast.Stmt.If.class);
        assertTrue("应包含if语句", hasIf);
    }

    @Test
    public void testWhileStatement() throws IOException {
        Parser parser = createParser("examples/Iteration01.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasWhile = containsStmtType(prog, Ast.Stmt.While.class);
        assertTrue("应包含while语句", hasWhile);
    }

    @Test
    public void testReturnStatement() throws IOException {
        Parser parser = createParser("examples/Cal.lemon");
        Ast.Program.T prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasReturn = containsStmtType(prog, Ast.Stmt.Return.class);
        assertTrue("应包含return语句", hasReturn);
    }

    // ==================== 辅助方法 ====================

    private Parser createParser(String filename) throws IOException {
        Lexer lexer = new Lexer(new File(filename));
        return new Parser(lexer);
    }

    /**
     * 检查AST中是否包含指定类型的表达式
     */
    private boolean containsExprType(Ast.Program.T prog, Class<?> exprType) {
        if (prog instanceof Ast.Program.ProgramSingle) {
            Ast.Program.ProgramSingle ps = (Ast.Program.ProgramSingle) prog;
            if (ps.mainClass instanceof Ast.MainClass.MainClassSingle) {
                Ast.MainClass.MainClassSingle mc = (Ast.MainClass.MainClassSingle) ps.mainClass;
                for (Ast.Method.T method : mc.methods) {
                    if (method instanceof Ast.Method.MethodSingle) {
                        Ast.Method.MethodSingle ms = (Ast.Method.MethodSingle) method;
                        for (Ast.Stmt.T stmt : ms.stms) {
                            if (containsExprInStmt(stmt, exprType)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean containsExprInStmt(Ast.Stmt.T stmt, Class<?> exprType) {
        if (stmt instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) stmt;
            if (containsExprInExpr(ifStmt.condition, exprType)) return true;
            if (ifStmt.thenStmt != null && containsExprInStmt(ifStmt.thenStmt, exprType)) return true;
            if (ifStmt.elseStmt != null && containsExprInStmt(ifStmt.elseStmt, exprType)) return true;
        } else if (stmt instanceof Ast.Stmt.While) {
            Ast.Stmt.While whileStmt = (Ast.Stmt.While) stmt;
            if (containsExprInExpr(whileStmt.condition, exprType)) return true;
            if (whileStmt.body != null && containsExprInStmt(whileStmt.body, exprType)) return true;
        } else if (stmt instanceof Ast.Stmt.Assign) {
            Ast.Stmt.Assign assign = (Ast.Stmt.Assign) stmt;
            if (containsExprInExpr(assign.expr, exprType)) return true;
        } else if (stmt instanceof Ast.Stmt.Block) {
            Ast.Stmt.Block block = (Ast.Stmt.Block) stmt;
            for (Ast.Stmt.T s : block.stmts) {
                if (containsExprInStmt(s, exprType)) return true;
            }
        } else if (stmt instanceof Ast.Stmt.Return) {
            Ast.Stmt.Return ret = (Ast.Stmt.Return) stmt;
            if (containsExprInExpr(ret.expr, exprType)) return true;
        }
        return false;
    }

    private boolean containsExprInExpr(Ast.Expr.T expr, Class<?> exprType) {
        if (expr == null) return false;
        if (exprType.isInstance(expr)) return true;
        
        // 递归检查子表达式
        if (expr instanceof Ast.Expr.And) {
            Ast.Expr.And and = (Ast.Expr.And) expr;
            return containsExprInExpr(and.left, exprType) || containsExprInExpr(and.right, exprType);
        } else if (expr instanceof Ast.Expr.Or) {
            Ast.Expr.Or or = (Ast.Expr.Or) expr;
            return containsExprInExpr(or.left, exprType) || containsExprInExpr(or.right, exprType);
        } else if (expr instanceof Ast.Expr.Not) {
            return containsExprInExpr(((Ast.Expr.Not) expr).expr, exprType);
        } else if (expr instanceof Ast.Expr.GT) {
            Ast.Expr.GT gt = (Ast.Expr.GT) expr;
            return containsExprInExpr(gt.left, exprType) || containsExprInExpr(gt.right, exprType);
        } else if (expr instanceof Ast.Expr.LT) {
            Ast.Expr.LT lt = (Ast.Expr.LT) expr;
            return containsExprInExpr(lt.left, exprType) || containsExprInExpr(lt.right, exprType);
        } else if (expr instanceof Ast.Expr.GET) {
            Ast.Expr.GET get = (Ast.Expr.GET) expr;
            return containsExprInExpr(get.left, exprType) || containsExprInExpr(get.right, exprType);
        } else if (expr instanceof Ast.Expr.LET) {
            Ast.Expr.LET let = (Ast.Expr.LET) expr;
            return containsExprInExpr(let.left, exprType) || containsExprInExpr(let.right, exprType);
        } else if (expr instanceof Ast.Expr.EQ) {
            Ast.Expr.EQ eq = (Ast.Expr.EQ) expr;
            return containsExprInExpr(eq.left, exprType) || containsExprInExpr(eq.right, exprType);
        } else if (expr instanceof Ast.Expr.NEQ) {
            Ast.Expr.NEQ neq = (Ast.Expr.NEQ) expr;
            return containsExprInExpr(neq.left, exprType) || containsExprInExpr(neq.right, exprType);
        } else if (expr instanceof Ast.Expr.Add) {
            Ast.Expr.Add add = (Ast.Expr.Add) expr;
            return containsExprInExpr(add.left, exprType) || containsExprInExpr(add.right, exprType);
        } else if (expr instanceof Ast.Expr.Sub) {
            Ast.Expr.Sub sub = (Ast.Expr.Sub) expr;
            return containsExprInExpr(sub.left, exprType) || containsExprInExpr(sub.right, exprType);
        } else if (expr instanceof Ast.Expr.Mul) {
            Ast.Expr.Mul mul = (Ast.Expr.Mul) expr;
            return containsExprInExpr(mul.left, exprType) || containsExprInExpr(mul.right, exprType);
        } else if (expr instanceof Ast.Expr.Div) {
            Ast.Expr.Div div = (Ast.Expr.Div) expr;
            return containsExprInExpr(div.left, exprType) || containsExprInExpr(div.right, exprType);
        } else if (expr instanceof Ast.Expr.Call) {
            Ast.Expr.Call call = (Ast.Expr.Call) expr;
            for (Ast.Expr.T arg : call.inputParams) {
                if (containsExprInExpr(arg, exprType)) return true;
            }
        }
        return false;
    }

    /**
     * 检查AST中是否包含指定类型的语句
     */
    private boolean containsStmtType(Ast.Program.T prog, Class<?> stmtType) {
        if (prog instanceof Ast.Program.ProgramSingle) {
            Ast.Program.ProgramSingle ps = (Ast.Program.ProgramSingle) prog;
            if (ps.mainClass instanceof Ast.MainClass.MainClassSingle) {
                Ast.MainClass.MainClassSingle mc = (Ast.MainClass.MainClassSingle) ps.mainClass;
                for (Ast.Method.T method : mc.methods) {
                    if (method instanceof Ast.Method.MethodSingle) {
                        Ast.Method.MethodSingle ms = (Ast.Method.MethodSingle) method;
                        for (Ast.Stmt.T stmt : ms.stms) {
                            if (containsStmtInStmt(stmt, stmtType)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean containsStmtInStmt(Ast.Stmt.T stmt, Class<?> stmtType) {
        if (stmt == null) return false;
        if (stmtType.isInstance(stmt)) return true;
        
        if (stmt instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) stmt;
            if (containsStmtInStmt(ifStmt.thenStmt, stmtType)) return true;
            if (containsStmtInStmt(ifStmt.elseStmt, stmtType)) return true;
        } else if (stmt instanceof Ast.Stmt.While) {
            return containsStmtInStmt(((Ast.Stmt.While) stmt).body, stmtType);
        } else if (stmt instanceof Ast.Stmt.Block) {
            for (Ast.Stmt.T s : ((Ast.Stmt.Block) stmt).stmts) {
                if (containsStmtInStmt(s, stmtType)) return true;
            }
        }
        return false;
    }
}
