package site.ilemon.optimizer;

import site.ilemon.ast.Ast;
import site.ilemon.ast.Ast.Type.TypeKind;

import java.util.ArrayList;

public class AstOptimizer {

    public Ast.Program.T optimize(Ast.Program.T program) {
        if (!(program instanceof Ast.Program.ProgramSingle)) {
            return program;
        }
        Ast.Program.ProgramSingle single = (Ast.Program.ProgramSingle) program;
        return new Ast.Program.ProgramSingle(optimizeMainClass(single.getMainClass()));
    }

    private Ast.MainClass.T optimizeMainClass(Ast.MainClass.T mainClass) {
        Ast.MainClass.MainClassSingle single = (Ast.MainClass.MainClassSingle) mainClass;
        ArrayList<Ast.Method.T> methods = new ArrayList<Ast.Method.T>();
        for (Ast.Method.T method : single.getMethods()) {
            methods.add(optimizeMethod(method));
        }
        return new Ast.MainClass.MainClassSingle(single.getClassId(), single.getFields(), methods);
    }

    private Ast.Method.T optimizeMethod(Ast.Method.T method) {
        Ast.Method.MethodSingle single = (Ast.Method.MethodSingle) method;
        ArrayList<Ast.Stmt.T> statements = optimizeStatements(single.getStms());
        Ast.Stmt.T retExp = single.getRetExp() == null ? null : optimizeStmt(single.getRetExp());
        return new Ast.Method.MethodSingle(single.getRetType(), single.getId(),
                single.getFormals(), single.getLocals(), statements, retExp, single.getLineNum());
    }

    private ArrayList<Ast.Stmt.T> optimizeStatements(ArrayList<Ast.Stmt.T> statements) {
        ArrayList<Ast.Stmt.T> result = new ArrayList<Ast.Stmt.T>();
        if (statements == null) {
            return result;
        }
        for (Ast.Stmt.T statement : statements) {
            Ast.Stmt.T optimized = optimizeStmt(statement);
            if (optimized == null) {
                continue;
            }
            if (optimized instanceof Ast.Stmt.Block) {
                Ast.Stmt.Block block = (Ast.Stmt.Block) optimized;
                if (block.getStmts().isEmpty()) {
                    continue;
                }
            }
            result.add(optimized);
        }
        return result;
    }

    private Ast.Stmt.T optimizeStmt(Ast.Stmt.T stmt) {
        if (stmt == null) {
            return null;
        }
        if (stmt instanceof Ast.Stmt.Assign) {
            Ast.Stmt.Assign assign = (Ast.Stmt.Assign) stmt;
            return new Ast.Stmt.Assign(assign.getId(), optimizeExpr(assign.getExpr()), assign.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.ArrayAssign) {
            Ast.Stmt.ArrayAssign arrayAssign = (Ast.Stmt.ArrayAssign) stmt;
            Ast.Stmt.ArrayAssign optimized = new Ast.Stmt.ArrayAssign(arrayAssign.getArrayName(),
                    optimizeExpr(arrayAssign.getIndex()), optimizeExpr(arrayAssign.getExpr()),
                    arrayAssign.getLineNum());
            optimized.setElementType(arrayAssign.getElementType());
            return optimized;
        }
        if (stmt instanceof Ast.Stmt.Block) {
            Ast.Stmt.Block block = (Ast.Stmt.Block) stmt;
            return new Ast.Stmt.Block(optimizeStatements(block.getStmts()), block.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.If) {
            Ast.Stmt.If ifStmt = (Ast.Stmt.If) stmt;
            Ast.Expr.T condition = optimizeExpr(ifStmt.getCondition());
            Ast.Stmt.T thenStmt = optimizeStmt(ifStmt.getThenStmt());
            Ast.Stmt.T elseStmt = optimizeStmt(ifStmt.getElseStmt());
            Boolean constant = boolValue(condition);
            if (Boolean.TRUE.equals(constant)) {
                return thenStmt;
            }
            if (Boolean.FALSE.equals(constant)) {
                return elseStmt == null ? new Ast.Stmt.Block(new ArrayList<Ast.Stmt.T>(), ifStmt.getLineNum()) : elseStmt;
            }
            return new Ast.Stmt.If(condition, thenStmt, elseStmt, ifStmt.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.While) {
            Ast.Stmt.While whileStmt = (Ast.Stmt.While) stmt;
            Ast.Expr.T condition = optimizeExpr(whileStmt.getCondition());
            if (Boolean.FALSE.equals(boolValue(condition))) {
                return new Ast.Stmt.Block(new ArrayList<Ast.Stmt.T>(), whileStmt.getLineNum());
            }
            return new Ast.Stmt.While(condition, optimizeStmt(whileStmt.getBody()), whileStmt.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.For) {
            Ast.Stmt.For forStmt = (Ast.Stmt.For) stmt;
            Ast.Stmt.T init = optimizeStmt(forStmt.getInit());
            Ast.Expr.T condition = optimizeExpr(forStmt.getCondition());
            Ast.Stmt.T update = optimizeStmt(forStmt.getUpdate());
            Ast.Stmt.T body = optimizeStmt(forStmt.getBody());
            return new Ast.Stmt.For(init, condition, update, body, forStmt.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.Return) {
            Ast.Stmt.Return ret = (Ast.Stmt.Return) stmt;
            return new Ast.Stmt.Return(optimizeExpr(ret.getExpr()), ret.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.Printf) {
            Ast.Stmt.Printf printf = (Ast.Stmt.Printf) stmt;
            ArrayList<Ast.Expr.T> exprs = new ArrayList<Ast.Expr.T>();
            for (Ast.Expr.T expr : printf.getExprs()) {
                exprs.add(optimizeExpr(expr));
            }
            return new Ast.Stmt.Printf(printf.getFormat(), exprs, printf.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.Call) {
            Ast.Stmt.Call call = (Ast.Stmt.Call) stmt;
            ArrayList<Ast.Expr.T> args = optimizeExprList(call.getInputParams());
            Ast.Stmt.Call optimized = new Ast.Stmt.Call(call.getName(), args, call.getLineNum());
            optimized.setReturnType(call.getReturnType());
            return optimized;
        }
        return stmt;
    }

    private ArrayList<Ast.Expr.T> optimizeExprList(ArrayList<Ast.Expr.T> exprs) {
        ArrayList<Ast.Expr.T> result = new ArrayList<Ast.Expr.T>();
        if (exprs != null) {
            for (Ast.Expr.T expr : exprs) {
                result.add(optimizeExpr(expr));
            }
        }
        return result;
    }

    private Ast.Expr.T optimizeExpr(Ast.Expr.T expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof Ast.Expr.Add) {
            Ast.Expr.Add node = (Ast.Expr.Add) expr;
            return foldArithmetic("+", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Sub) {
            Ast.Expr.Sub node = (Ast.Expr.Sub) expr;
            return foldArithmetic("-", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Mul) {
            Ast.Expr.Mul node = (Ast.Expr.Mul) expr;
            return foldArithmetic("*", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Div) {
            Ast.Expr.Div node = (Ast.Expr.Div) expr;
            return foldArithmetic("/", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Mod) {
            Ast.Expr.Mod node = (Ast.Expr.Mod) expr;
            return foldArithmetic("%", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.And) {
            Ast.Expr.And node = (Ast.Expr.And) expr;
            Ast.Expr.T left = optimizeExpr(node.getLeft());
            Ast.Expr.T right = optimizeExpr(node.getRight());
            Boolean leftValue = boolValue(left);
            Boolean rightValue = boolValue(right);
            if (Boolean.FALSE.equals(leftValue)) return new Ast.Expr.False(node.getLineNum());
            if (Boolean.TRUE.equals(leftValue)) return right;
            if (rightValue != null) {
                if (rightValue.booleanValue()) {
                    return left;
                }
                if (canDiscardEvaluation(left)) {
                    return new Ast.Expr.False(node.getLineNum());
                }
            }
            return new Ast.Expr.And(left, right, node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Or) {
            Ast.Expr.Or node = (Ast.Expr.Or) expr;
            Ast.Expr.T left = optimizeExpr(node.getLeft());
            Ast.Expr.T right = optimizeExpr(node.getRight());
            Boolean leftValue = boolValue(left);
            Boolean rightValue = boolValue(right);
            if (Boolean.TRUE.equals(leftValue)) return new Ast.Expr.True(node.getLineNum());
            if (Boolean.FALSE.equals(leftValue)) return right;
            if (rightValue != null) {
                if (!rightValue.booleanValue()) {
                    return left;
                }
                if (canDiscardEvaluation(left)) {
                    return new Ast.Expr.True(node.getLineNum());
                }
            }
            return new Ast.Expr.Or(left, right, node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Not) {
            Ast.Expr.Not node = (Ast.Expr.Not) expr;
            Ast.Expr.T inner = optimizeExpr(node.getExpr());
            Boolean value = boolValue(inner);
            if (value != null) {
                return value ? new Ast.Expr.False(node.getLineNum()) : new Ast.Expr.True(node.getLineNum());
            }
            Ast.Expr.Not optimized = new Ast.Expr.Not(inner);
            optimized.setLineNum(node.getLineNum());
            return optimized;
        }
        if (expr instanceof Ast.Expr.GT) {
            Ast.Expr.GT node = (Ast.Expr.GT) expr;
            return foldComparison(">", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.LT) {
            Ast.Expr.LT node = (Ast.Expr.LT) expr;
            return foldComparison("<", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.GTE) {
            Ast.Expr.GTE node = (Ast.Expr.GTE) expr;
            return foldComparison(">=", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.LTE) {
            Ast.Expr.LTE node = (Ast.Expr.LTE) expr;
            return foldComparison("<=", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.EQ) {
            Ast.Expr.EQ node = (Ast.Expr.EQ) expr;
            return foldComparison("==", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.NEQ) {
            Ast.Expr.NEQ node = (Ast.Expr.NEQ) expr;
            return foldComparison("!=", optimizeExpr(node.getLeft()), optimizeExpr(node.getRight()), node.getLineNum());
        }
        if (expr instanceof Ast.Expr.Call) {
            Ast.Expr.Call call = (Ast.Expr.Call) expr;
            Ast.Expr.Call optimized = new Ast.Expr.Call(call.getName(), optimizeExprList(call.getInputParams()),
                    call.getLineNum(), call.getReturnType());
            return optimized;
        }
        if (expr instanceof Ast.Expr.ArrayAccess) {
            Ast.Expr.ArrayAccess access = (Ast.Expr.ArrayAccess) expr;
            Ast.Expr.ArrayAccess optimized = new Ast.Expr.ArrayAccess(access.getArrayName(),
                    optimizeExpr(access.getIndex()), access.getLineNum());
            optimized.setElementType(access.getElementType());
            return optimized;
        }
        return expr;
    }

    private Ast.Expr.T foldArithmetic(String op, Ast.Expr.T left, Ast.Expr.T right, int lineNum) {
        Ast.Expr.T simplified = simplifyArithmeticIdentity(op, left, right, lineNum);
        if (simplified != null) {
            return simplified;
        }
        if (left instanceof Ast.Expr.Number && right instanceof Ast.Expr.Number) {
            Ast.Expr.Number l = (Ast.Expr.Number) left;
            Ast.Expr.Number r = (Ast.Expr.Number) right;
            if (("/".equals(op) || "%".equals(op)) && isZero(r)) {
                return rebuildArithmetic(op, left, right, lineNum);
            }
            return number(resultType(l, r), calculate(op, l, r), lineNum);
        }
        return rebuildArithmetic(op, left, right, lineNum);
    }

    private Ast.Expr.T simplifyArithmeticIdentity(String op, Ast.Expr.T left, Ast.Expr.T right, int lineNum) {
        if ("+".equals(op)) {
            if (isZero(left)) return right;
            if (isZero(right)) return left;
        } else if ("-".equals(op)) {
            if (isZero(right)) return left;
        } else if ("*".equals(op)) {
            if (isZero(left) && canDiscardEvaluation(right)) return zeroFor(left, right, lineNum);
            if (isZero(right) && canDiscardEvaluation(left)) return zeroFor(left, right, lineNum);
            if (isOne(left)) return right;
            if (isOne(right)) return left;
        } else if ("/".equals(op)) {
            if (isOne(right)) return left;
            if (isZero(left) && !isZero(right) && canDiscardEvaluation(right)) return zeroFor(left, right, lineNum);
        } else if ("%".equals(op)) {
            if (isZero(left) && !isZero(right) && canDiscardEvaluation(right)) return zeroFor(left, right, lineNum);
        }
        return null;
    }

    private boolean canDiscardEvaluation(Ast.Expr.T expr) {
        if (expr == null) {
            return true;
        }
        if (expr instanceof Ast.Expr.Number
                || expr instanceof Ast.Expr.True
                || expr instanceof Ast.Expr.False
                || expr instanceof Ast.Expr.Id
                || expr instanceof Ast.Expr.Str
                || expr instanceof Ast.Expr.ArrayLength) {
            return true;
        }
        if (expr instanceof Ast.Expr.Call || expr instanceof Ast.Expr.ArrayAccess) {
            return false;
        }
        if (expr instanceof Ast.Expr.Not) {
            return canDiscardEvaluation(((Ast.Expr.Not) expr).getExpr());
        }
        if (expr instanceof Ast.Expr.Add) {
            Ast.Expr.Add node = (Ast.Expr.Add) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.Sub) {
            Ast.Expr.Sub node = (Ast.Expr.Sub) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.Mul) {
            Ast.Expr.Mul node = (Ast.Expr.Mul) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.Div) {
            Ast.Expr.Div node = (Ast.Expr.Div) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.Mod) {
            Ast.Expr.Mod node = (Ast.Expr.Mod) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.And) {
            Ast.Expr.And node = (Ast.Expr.And) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.Or) {
            Ast.Expr.Or node = (Ast.Expr.Or) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.GT) {
            Ast.Expr.GT node = (Ast.Expr.GT) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.LT) {
            Ast.Expr.LT node = (Ast.Expr.LT) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.GTE) {
            Ast.Expr.GTE node = (Ast.Expr.GTE) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.LTE) {
            Ast.Expr.LTE node = (Ast.Expr.LTE) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.EQ) {
            Ast.Expr.EQ node = (Ast.Expr.EQ) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        if (expr instanceof Ast.Expr.NEQ) {
            Ast.Expr.NEQ node = (Ast.Expr.NEQ) expr;
            return canDiscardEvaluation(node.getLeft()) && canDiscardEvaluation(node.getRight());
        }
        return false;
    }

    private Ast.Expr.T foldComparison(String op, Ast.Expr.T left, Ast.Expr.T right, int lineNum) {
        if (left instanceof Ast.Expr.Number && right instanceof Ast.Expr.Number) {
            boolean value = compare(op, (Ast.Expr.Number) left, (Ast.Expr.Number) right);
            return value ? new Ast.Expr.True(lineNum) : new Ast.Expr.False(lineNum);
        }
        if (left instanceof Ast.Expr.True || left instanceof Ast.Expr.False) {
            Boolean l = boolValue(left);
            Boolean r = boolValue(right);
            if (r != null && ("==".equals(op) || "!=".equals(op))) {
                boolean equal = l.booleanValue() == r.booleanValue();
                return ("==".equals(op) == equal) ? new Ast.Expr.True(lineNum) : new Ast.Expr.False(lineNum);
            }
        }
        return rebuildComparison(op, left, right, lineNum);
    }

    private Ast.Expr.T rebuildArithmetic(String op, Ast.Expr.T left, Ast.Expr.T right, int lineNum) {
        if ("+".equals(op)) return new Ast.Expr.Add(left, right, lineNum);
        if ("-".equals(op)) return new Ast.Expr.Sub(left, right, lineNum);
        if ("*".equals(op)) return new Ast.Expr.Mul(left, right, lineNum);
        if ("/".equals(op)) return new Ast.Expr.Div(left, right, lineNum);
        return new Ast.Expr.Mod(left, right, lineNum);
    }

    private Ast.Expr.T rebuildComparison(String op, Ast.Expr.T left, Ast.Expr.T right, int lineNum) {
        if (">".equals(op)) return new Ast.Expr.GT(left, right, lineNum);
        if ("<".equals(op)) return new Ast.Expr.LT(left, right, lineNum);
        if (">=".equals(op)) return new Ast.Expr.GTE(left, right, lineNum);
        if ("<=".equals(op)) return new Ast.Expr.LTE(left, right, lineNum);
        if ("==".equals(op)) return new Ast.Expr.EQ(left, right, lineNum);
        return new Ast.Expr.NEQ(left, right, lineNum);
    }

    private Boolean boolValue(Ast.Expr.T expr) {
        if (expr instanceof Ast.Expr.True) return Boolean.TRUE;
        if (expr instanceof Ast.Expr.False) return Boolean.FALSE;
        return null;
    }

    private boolean isZero(Ast.Expr.T expr) {
        return expr instanceof Ast.Expr.Number && numericValue((Ast.Expr.Number) expr).doubleValue() == 0.0d;
    }

    private boolean isOne(Ast.Expr.T expr) {
        return expr instanceof Ast.Expr.Number && numericValue((Ast.Expr.Number) expr).doubleValue() == 1.0d;
    }

    private Ast.Expr.T zeroFor(Ast.Expr.T left, Ast.Expr.T right, int lineNum) {
        Ast.Type.T type = left instanceof Ast.Expr.Number && right instanceof Ast.Expr.Number
                ? resultType((Ast.Expr.Number) left, (Ast.Expr.Number) right)
                : new Ast.Type.Int();
        return number(type, 0, lineNum);
    }

    private Ast.Type.T resultType(Ast.Expr.Number left, Ast.Expr.Number right) {
        if (left.getType().getKind() == TypeKind.DOUBLE || right.getType().getKind() == TypeKind.DOUBLE) {
            return new Ast.Type.Double();
        }
        if (left.getType().getKind() == TypeKind.FLOAT || right.getType().getKind() == TypeKind.FLOAT) {
            return new Ast.Type.Float();
        }
        return new Ast.Type.Int();
    }

    private Ast.Expr.Number number(Ast.Type.T type, Object value, int lineNum) {
        if (type.getKind() == TypeKind.DOUBLE) {
            return new Ast.Expr.Number(type, Double.valueOf(asDouble(value)), lineNum);
        }
        if (type.getKind() == TypeKind.FLOAT) {
            return new Ast.Expr.Number(type, Float.valueOf((float) asDouble(value)), lineNum);
        }
        return new Ast.Expr.Number(type, Integer.valueOf((int) asDouble(value)), lineNum);
    }

    private Object calculate(String op, Ast.Expr.Number left, Ast.Expr.Number right) {
        Ast.Type.T type = resultType(left, right);
        if (type.getKind() == TypeKind.INT) {
            int l = numericValue(left).intValue();
            int r = numericValue(right).intValue();
            if ("+".equals(op)) return l + r;
            if ("-".equals(op)) return l - r;
            if ("*".equals(op)) return l * r;
            if ("/".equals(op)) return l / r;
            return l % r;
        }
        double l = numericValue(left).doubleValue();
        double r = numericValue(right).doubleValue();
        if ("+".equals(op)) return l + r;
        if ("-".equals(op)) return l - r;
        if ("*".equals(op)) return l * r;
        if ("/".equals(op)) return l / r;
        return l % r;
    }

    private boolean compare(String op, Ast.Expr.Number left, Ast.Expr.Number right) {
        double l = numericValue(left).doubleValue();
        double r = numericValue(right).doubleValue();
        if (">".equals(op)) return l > r;
        if ("<".equals(op)) return l < r;
        if (">=".equals(op)) return l >= r;
        if ("<=".equals(op)) return l <= r;
        if ("==".equals(op)) return l == r;
        return l != r;
    }

    private Number numericValue(Ast.Expr.Number number) {
        Object value = number.getValue();
        if (value instanceof Number) {
            return (Number) value;
        }
        if (number.getType().getKind() == TypeKind.INT) {
            return Integer.valueOf(value.toString());
        }
        if (number.getType().getKind() == TypeKind.FLOAT) {
            return Float.valueOf(value.toString());
        }
        return Double.valueOf(value.toString());
    }

    private double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
    }
}
