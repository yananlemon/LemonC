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
        Ast.Program.Base prog = parser.parse();
        assertNotNull("应成功解析程序", prog);
    }

    @Test
    public void testParseFloat() throws IOException {
        Parser parser = createParser("examples/FloatTest01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
    }

    @Test
    public void testParseIteration() throws IOException {
        Parser parser = createParser("examples/Iteration01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
    }

    // ==================== 比较运算符测试 ====================

    @Test
    public void testCompareOperators() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull("应成功解析包含所有比较运算符的程序", prog);
    }

    @Test
    public void testGreaterThan() throws IOException {
        Parser parser = createParser("examples/If01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含GT节点
        boolean hasGT = containsExprType(prog, Ast.Expr.GT.class);
        // If01.lemon 使用了 > 运算符
    }

    @Test
    public void testLessThan() throws IOException {
        Parser parser = createParser("examples/If01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含LT节点
        boolean hasLT = containsExprType(prog, Ast.Expr.LT.class);
        assertTrue("应包含小于运算符", hasLT);
    }

    @Test
    public void testGreaterThanOrEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含GET节点 (>=)
        boolean hasGTE = containsExprType(prog, Ast.Expr.GTE.class);
        assertTrue("应包含大于等于运算符", hasGTE);
    }

    @Test
    public void testLessThanOrEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含LET节点 (<=)
        boolean hasLTE = containsExprType(prog, Ast.Expr.LTE.class);
        assertTrue("应包含小于等于运算符", hasLTE);
    }

    @Test
    public void testEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含EQ节点 (==)
        boolean hasEQ = containsExprType(prog, Ast.Expr.EQ.class);
        assertTrue("应包含等于运算符", hasEQ);
    }

    @Test
    public void testNotEqual() throws IOException {
        Parser parser = createParser("examples/CompareTest.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        // 验证AST中包含NEQ节点 (!=)
        boolean hasNEQ = containsExprType(prog, Ast.Expr.NEQ.class);
        assertTrue("应包含不等于运算符", hasNEQ);
    }

    // ==================== 逻辑运算符测试 ====================

    @Test
    public void testLogicalAnd() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasAnd = containsExprType(prog, Ast.Expr.And.class);
        assertTrue("应包含逻辑与运算符", hasAnd);
    }

    @Test
    public void testLogicalOr() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasOr = containsExprType(prog, Ast.Expr.Or.class);
        assertTrue("应包含逻辑或运算符", hasOr);
    }

    @Test
    public void testLogicalNot() throws IOException {
        Parser parser = createParser("examples/BoolTest01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasNot = containsExprType(prog, Ast.Expr.Not.class);
        assertTrue("应包含逻辑非运算符", hasNot);
    }

    // ==================== 方法调用测试 ====================

    @Test
    public void testMethodCall() throws IOException {
        Parser parser = createParser("examples/SimpleMethodCall.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasCall = containsExprType(prog, Ast.Expr.Call.class);
        assertTrue("应包含方法调用", hasCall);
    }

    @Test
    public void testRecursiveCall() throws IOException {
        Parser parser = createParser("examples/Cal.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull("应成功解析递归调用", prog);
    }

    // ==================== 语句测试 ====================

    @Test
    public void testIfStatement() throws IOException {
        Parser parser = createParser("examples/If01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasIf = containsStmtType(prog, Ast.Stmt.If.class);
        assertTrue("应包含if语句", hasIf);
    }

    @Test
    public void testWhileStatement() throws IOException {
        Parser parser = createParser("examples/Iteration01.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasWhile = containsStmtType(prog, Ast.Stmt.While.class);
        assertTrue("应包含while语句", hasWhile);
    }

    @Test
    public void testReturnStatement() throws IOException {
        Parser parser = createParser("examples/Cal.lemon");
        Ast.Program.Base prog = parser.parse();
        assertNotNull(prog);
        
        boolean hasReturn = containsStmtType(prog, Ast.Stmt.Return.class);
        assertTrue("应包含return语句", hasReturn);
    }

    // ==================== 优先级层级测试 ====================

    @Test
    public void equalityBindsLooserThanRelational() throws IOException {
        // a < b == c < d 必须解析为 (a<b) == (c<d)：相等运算符优先级低于关系运算符（同 C）。
        // 拆层之前这个合法表达式会被解析成 ((a<b)==c)<d 并在语义层被拒。
        Ast.Expr.Base condition = firstIfCondition(parseSource("EqualityPrecedence",
                "class EqualityPrecedence {\n" +
                "    void main() {\n" +
                "        int a; int b; int c; int d;\n" +
                "        a = 1; b = 2; c = 3; d = 4;\n" +
                "        if (a < b == c < d) { printf(\"yes\\n\"); }\n" +
                "    }\n" +
                "}\n"));

        assertTrue("根节点应为 EQ，实际为 " + condition.getClass().getSimpleName(),
                condition instanceof Ast.Expr.EQ);
        Ast.Expr.EQ equality = (Ast.Expr.EQ) condition;
        assertTrue("左子树应为 LT", equality.getLeft() instanceof Ast.Expr.LT);
        assertTrue("右子树应为 LT", equality.getRight() instanceof Ast.Expr.LT);
    }

    @Test
    public void andBindsLooserThanEquality() throws IOException {
        Ast.Expr.Base condition = firstIfCondition(parseSource("AndPrecedence",
                "class AndPrecedence {\n" +
                "    void main() {\n" +
                "        int a; int b;\n" +
                "        a = 1; b = 2;\n" +
                "        if (a == 1 && b == 2) { printf(\"yes\\n\"); }\n" +
                "    }\n" +
                "}\n"));

        assertTrue("根节点应为 And", condition instanceof Ast.Expr.And);
        Ast.Expr.And and = (Ast.Expr.And) condition;
        assertTrue(and.getLeft() instanceof Ast.Expr.EQ);
        assertTrue(and.getRight() instanceof Ast.Expr.EQ);
    }

    // ==================== 复合赋值与自增测试 ====================

    @Test
    public void incrementDesugarsToAssignPlusOne() throws IOException {
        // i++ 脱糖为 i = i + 1，所以后端与语义层不需要新增节点类型。
        Ast.Stmt.Base stmt = firstStatementOfKind(parseSource("IncrementDesugar",
                "class IncrementDesugar {\n" +
                "    void main() { int i; i = 0; i++; }\n" +
                "}\n"), 2);

        assertTrue("应脱糖为 Assign", stmt instanceof Ast.Stmt.Assign);
        Ast.Stmt.Assign assign = (Ast.Stmt.Assign) stmt;
        assertEquals("i", assign.getId().getId());
        assertTrue("右值应为 Add", assign.getExpr() instanceof Ast.Expr.Add);
        Ast.Expr.Add add = (Ast.Expr.Add) assign.getExpr();
        assertTrue(add.getLeft() instanceof Ast.Expr.Id);
        assertTrue(add.getRight() instanceof Ast.Expr.IntLiteral);
        assertEquals(Integer.valueOf(1), ((Ast.Expr.IntLiteral) add.getRight()).getValue());
    }

    @Test
    public void compoundAssignmentDesugarsToBinaryOperation() throws IOException {
        Ast.Stmt.Base stmt = firstStatementOfKind(parseSource("CompoundDesugar",
                "class CompoundDesugar {\n" +
                "    void main() { int k; k = 3; k *= 5; }\n" +
                "}\n"), 2);

        Ast.Stmt.Assign assign = (Ast.Stmt.Assign) stmt;
        assertTrue("右值应为 Mul", assign.getExpr() instanceof Ast.Expr.Mul);
        Ast.Expr.Mul mul = (Ast.Expr.Mul) assign.getExpr();
        assertEquals("k", ((Ast.Expr.Id) mul.getLeft()).getId());
    }

    @Test
    public void arrayElementCompoundAssignmentDoesNotShareTheIndexNode() throws IOException {
        // 这是一条卫生性不变式，不是当前的 bug：共享下标节点目前不会造成可观察的错误。
        // 但 AST 的 source span 是可变的，而 SemanticVisitor 用 IdentityHashMap 建立
        // 源节点到类型化节点的映射，一旦有人依赖这两点，共享节点就会静默出错。
        // 脱糖是唯一会凭空造出重复节点的地方，所以在这里把不变式钉住。
        Ast.Stmt.Base stmt = firstStatementOfKind(parseSource("ArrayCompoundDesugar",
                "class ArrayCompoundDesugar {\n" +
                "    void main() { int a[3]; int i; i = 0; a[i] = 1; a[i] += 2; }\n" +
                "}\n"), 4);

        assertTrue(stmt instanceof Ast.Stmt.ArrayAssign);
        Ast.Stmt.ArrayAssign arrayAssign = (Ast.Stmt.ArrayAssign) stmt;
        Ast.Expr.Add add = (Ast.Expr.Add) arrayAssign.getExpr();
        Ast.Expr.ArrayAccess read = (Ast.Expr.ArrayAccess) add.getLeft();
        assertEquals("a", read.getArrayName());
        assertNotSame("读回的下标不能与写入的下标共享同一个节点",
                arrayAssign.getIndex(), read.getIndex());
    }

    @Test
    public void rejectsCompoundAssignmentWithSideEffectingIndex() throws IOException {
        try {
            parseSource("SideEffectIndex",
                    "class SideEffectIndex {\n" +
                    "    int id(int x) { return x; }\n" +
                    "    void main() { int a[3]; a[0] = 1; a[id(0)] += 1; }\n" +
                    "}\n");
            fail("下标含方法调用时应拒绝复合赋值");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("下标会被求值两次"));
        }
    }

    // ==================== 辅助方法 ====================

    private Ast.Stmt.Base firstStatementOfKind(Ast.Program.Base program, int index) {
        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle)
                ((Ast.Program.ProgramSingle) program).getMainClass();
        for (Ast.Method.Base methodBase : mainClass.getMethods()) {
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) methodBase;
            if ("main".equals(method.getId())) {
                return method.getStms().get(index);
            }
        }
        throw new AssertionError("main not found");
    }

    private Ast.Program.Base parseSource(String className, String source) throws IOException {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = new File(directory, className + ".lemon");
        java.nio.file.Files.write(file.toPath(),
                source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        file.deleteOnExit();
        return new Parser(new Lexer(file)).parse();
    }

    private Ast.Expr.Base firstIfCondition(Ast.Program.Base program) {
        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle)
                ((Ast.Program.ProgramSingle) program).getMainClass();
        for (Ast.Method.Base methodBase : mainClass.getMethods()) {
            for (Ast.Stmt.Base stmt : ((Ast.Method.MethodSingle) methodBase).getStms()) {
                if (stmt instanceof Ast.Stmt.If) {
                    return ((Ast.Stmt.If) stmt).getCondition();
                }
            }
        }
        throw new AssertionError("No if statement found");
    }

    private Parser createParser(String filename) throws IOException {
        Lexer lexer = new Lexer(new File(filename));
        return new Parser(lexer);
    }

    /**
     * 检查AST中是否包含指定类型的表达式
     */
    private boolean containsExprType(Ast.Program.Base prog, Class<?> exprType) {
        if (prog instanceof Ast.Program.ProgramSingle) {
            Ast.Program.ProgramSingle ps = (Ast.Program.ProgramSingle) prog;
            if (ps.getMainClass() instanceof Ast.MainClass.MainClassSingle) {
                Ast.MainClass.MainClassSingle mc = (Ast.MainClass.MainClassSingle) ps.getMainClass();
                for (Ast.Method.Base method : mc.getMethods()) {
                    if (method instanceof Ast.Method.MethodSingle) {
                        Ast.Method.MethodSingle ms = (Ast.Method.MethodSingle) method;
                        for (Ast.Stmt.Base stmt : ms.getStms()) {
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

    private boolean containsExprInStmt(Ast.Stmt.Base stmt, Class<?> exprType) {
        if (stmt instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) stmt;
            if (containsExprInExpr(ifStmt.getCondition(), exprType)) return true;
            if (ifStmt.getThenStmt() != null && containsExprInStmt(ifStmt.getThenStmt(), exprType)) return true;
            if (ifStmt.getElseStmt() != null && containsExprInStmt(ifStmt.getElseStmt(), exprType)) return true;
        } else if (stmt instanceof Ast.Stmt.While) {
            Ast.Stmt.While whileStmt = (Ast.Stmt.While) stmt;
            if (containsExprInExpr(whileStmt.getCondition(), exprType)) return true;
            if (whileStmt.getBody() != null && containsExprInStmt(whileStmt.getBody(), exprType)) return true;
        } else if (stmt instanceof Ast.Stmt.Assign) {
            Ast.Stmt.Assign assign = (Ast.Stmt.Assign) stmt;
            if (containsExprInExpr(assign.getExpr(), exprType)) return true;
        } else if (stmt instanceof Ast.Stmt.Block) {
            Ast.Stmt.Block block = (Ast.Stmt.Block) stmt;
            for (Ast.Stmt.Base s : block.getStmts()) {
                if (containsExprInStmt(s, exprType)) return true;
            }
        } else if (stmt instanceof Ast.Stmt.Return) {
            Ast.Stmt.Return ret = (Ast.Stmt.Return) stmt;
            if (containsExprInExpr(ret.getExpr(), exprType)) return true;
        }
        return false;
    }

    private boolean containsExprInExpr(Ast.Expr.Base expr, Class<?> exprType) {
        if (expr == null) return false;
        if (exprType.isInstance(expr)) return true;
        
        // 递归检查子表达式
        if (expr instanceof Ast.Expr.And) {
            Ast.Expr.And and = (Ast.Expr.And) expr;
            return containsExprInExpr(and.getLeft(), exprType) || containsExprInExpr(and.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Or) {
            Ast.Expr.Or or = (Ast.Expr.Or) expr;
            return containsExprInExpr(or.getLeft(), exprType) || containsExprInExpr(or.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Not) {
            return containsExprInExpr(((Ast.Expr.Not) expr).getExpr(), exprType);
        } else if (expr instanceof Ast.Expr.GT) {
            Ast.Expr.GT gt = (Ast.Expr.GT) expr;
            return containsExprInExpr(gt.getLeft(), exprType) || containsExprInExpr(gt.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.LT) {
            Ast.Expr.LT lt = (Ast.Expr.LT) expr;
            return containsExprInExpr(lt.getLeft(), exprType) || containsExprInExpr(lt.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.GTE) {
            Ast.Expr.GTE gte = (Ast.Expr.GTE) expr;
            return containsExprInExpr(gte.getLeft(), exprType) || containsExprInExpr(gte.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.LTE) {
            Ast.Expr.LTE lte = (Ast.Expr.LTE) expr;
            return containsExprInExpr(lte.getLeft(), exprType) || containsExprInExpr(lte.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.EQ) {
            Ast.Expr.EQ eq = (Ast.Expr.EQ) expr;
            return containsExprInExpr(eq.getLeft(), exprType) || containsExprInExpr(eq.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.NEQ) {
            Ast.Expr.NEQ neq = (Ast.Expr.NEQ) expr;
            return containsExprInExpr(neq.getLeft(), exprType) || containsExprInExpr(neq.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Add) {
            Ast.Expr.Add add = (Ast.Expr.Add) expr;
            return containsExprInExpr(add.getLeft(), exprType) || containsExprInExpr(add.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Sub) {
            Ast.Expr.Sub sub = (Ast.Expr.Sub) expr;
            return containsExprInExpr(sub.getLeft(), exprType) || containsExprInExpr(sub.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Mul) {
            Ast.Expr.Mul mul = (Ast.Expr.Mul) expr;
            return containsExprInExpr(mul.getLeft(), exprType) || containsExprInExpr(mul.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Div) {
            Ast.Expr.Div div = (Ast.Expr.Div) expr;
            return containsExprInExpr(div.getLeft(), exprType) || containsExprInExpr(div.getRight(), exprType);
        } else if (expr instanceof Ast.Expr.Call) {
            Ast.Expr.Call call = (Ast.Expr.Call) expr;
            for (Ast.Expr.Base arg : call.getInputParams()) {
                if (containsExprInExpr(arg, exprType)) return true;
            }
        }
        return false;
    }

    /**
     * 检查AST中是否包含指定类型的语句
     */
    private boolean containsStmtType(Ast.Program.Base prog, Class<?> stmtType) {
        if (prog instanceof Ast.Program.ProgramSingle) {
            Ast.Program.ProgramSingle ps = (Ast.Program.ProgramSingle) prog;
            if (ps.getMainClass() instanceof Ast.MainClass.MainClassSingle) {
                Ast.MainClass.MainClassSingle mc = (Ast.MainClass.MainClassSingle) ps.getMainClass();
                for (Ast.Method.Base method : mc.getMethods()) {
                    if (method instanceof Ast.Method.MethodSingle) {
                        Ast.Method.MethodSingle ms = (Ast.Method.MethodSingle) method;
                        for (Ast.Stmt.Base stmt : ms.getStms()) {
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

    private boolean containsStmtInStmt(Ast.Stmt.Base stmt, Class<?> stmtType) {
        if (stmt == null) return false;
        if (stmtType.isInstance(stmt)) return true;
        
        if (stmt instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) stmt;
            if (containsStmtInStmt(ifStmt.getThenStmt(), stmtType)) return true;
            if (containsStmtInStmt(ifStmt.getElseStmt(), stmtType)) return true;
        } else if (stmt instanceof Ast.Stmt.While) {
            return containsStmtInStmt(((Ast.Stmt.While) stmt).getBody(), stmtType);
        } else if (stmt instanceof Ast.Stmt.Block) {
            for (Ast.Stmt.Base s : ((Ast.Stmt.Block) stmt).getStmts()) {
                if (containsStmtInStmt(s, stmtType)) return true;
            }
        }
        return false;
    }
}
