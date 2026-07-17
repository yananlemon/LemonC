package site.ilemon.optimizer;

import site.ilemon.ast.Ast;
import site.ilemon.ast.Ast.Type.TypeKind;
import site.ilemon.lexer.IntegerLiterals;

import java.util.ArrayList;

public class AstOptimizer {

    public Ast.Program.Base optimize(Ast.Program.Base program) {
        if (!(program instanceof Ast.Program.ProgramSingle)) {
            return program;
        }
        Ast.Program.ProgramSingle single = (Ast.Program.ProgramSingle) program;
        return new Ast.Program.ProgramSingle(optimizeMainClass(single.getMainClass()));
    }

    private Ast.MainClass.Base optimizeMainClass(Ast.MainClass.Base mainClass) {
        Ast.MainClass.MainClassSingle single = (Ast.MainClass.MainClassSingle) mainClass;
        ArrayList<Ast.Method.Base> methods = new ArrayList<Ast.Method.Base>();
        for (Ast.Method.Base method : single.getMethods()) {
            methods.add(optimizeMethod(method));
        }
        return new Ast.MainClass.MainClassSingle(single.getClassId(), methods);
    }

    private Ast.Method.Base optimizeMethod(Ast.Method.Base method) {
        Ast.Method.MethodSingle single = (Ast.Method.MethodSingle) method;
        ArrayList<Ast.Stmt.Base> statements = optimizeStatements(single.getStms());
        Ast.Stmt.Base retExp = single.getRetExp() == null ? null : optimizeStmt(single.getRetExp());
        return new Ast.Method.MethodSingle(single.getRetType(), single.getId(),
                single.getFormals(), single.getLocals(), statements, retExp, single.getLineNum());
    }

    private ArrayList<Ast.Stmt.Base> optimizeStatements(ArrayList<Ast.Stmt.Base> statements) {
        ArrayList<Ast.Stmt.Base> result = new ArrayList<Ast.Stmt.Base>();
        if (statements == null) {
            return result;
        }
        for (Ast.Stmt.Base statement : statements) {
            Ast.Stmt.Base optimized = optimizeStmt(statement);
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

    private Ast.Stmt.Base optimizeStmt(Ast.Stmt.Base stmt) {
        if (stmt == null) {
            return null;
        }
        if (stmt instanceof Ast.Stmt.Assign) {
            Ast.Stmt.Assign assign = (Ast.Stmt.Assign) stmt;
            return new Ast.Stmt.Assign(assign.getId(), optimizeExpr(assign.getExpr()), assign.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.VarDecl) {
            Ast.Stmt.VarDecl varDecl = (Ast.Stmt.VarDecl) stmt;
            return new Ast.Stmt.VarDecl(varDecl.getDeclaration(),
                    optimizeExpr(varDecl.getInitializer()));
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
            Ast.Expr.Base condition = optimizeExpr(ifStmt.getCondition());
            Ast.Stmt.Base thenStmt = optimizeStmt(ifStmt.getThenStmt());
            Ast.Stmt.Base elseStmt = optimizeStmt(ifStmt.getElseStmt());
            Boolean constant = boolValue(condition);
            if (Boolean.TRUE.equals(constant)) {
                return thenStmt;
            }
            if (Boolean.FALSE.equals(constant)) {
                return elseStmt == null ? new Ast.Stmt.Block(new ArrayList<Ast.Stmt.Base>(), ifStmt.getLineNum()) : elseStmt;
            }
            return new Ast.Stmt.If(condition, thenStmt, elseStmt, ifStmt.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.While) {
            Ast.Stmt.While whileStmt = (Ast.Stmt.While) stmt;
            Ast.Expr.Base condition = optimizeExpr(whileStmt.getCondition());
            if (Boolean.FALSE.equals(boolValue(condition))) {
                return new Ast.Stmt.Block(new ArrayList<Ast.Stmt.Base>(), whileStmt.getLineNum());
            }
            return new Ast.Stmt.While(condition, optimizeStmt(whileStmt.getBody()), whileStmt.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.For) {
            Ast.Stmt.For forStmt = (Ast.Stmt.For) stmt;
            Ast.Stmt.Base init = optimizeStmt(forStmt.getInit());
            Ast.Expr.Base condition = optimizeExpr(forStmt.getCondition());
            Ast.Stmt.Base update = optimizeStmt(forStmt.getUpdate());
            Ast.Stmt.Base body = optimizeStmt(forStmt.getBody());
            return new Ast.Stmt.For(init, condition, update, body, forStmt.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.Return) {
            Ast.Stmt.Return ret = (Ast.Stmt.Return) stmt;
            return new Ast.Stmt.Return(optimizeExpr(ret.getExpr()), ret.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.Printf) {
            Ast.Stmt.Printf printf = (Ast.Stmt.Printf) stmt;
            ArrayList<Ast.Expr.Base> exprs = new ArrayList<Ast.Expr.Base>();
            for (Ast.Expr.Base expr : printf.getExprs()) {
                exprs.add(optimizeExpr(expr));
            }
            return new Ast.Stmt.Printf(printf.getFormat(), exprs, printf.getLineNum());
        }
        if (stmt instanceof Ast.Stmt.Call) {
            Ast.Stmt.Call call = (Ast.Stmt.Call) stmt;
            ArrayList<Ast.Expr.Base> args = optimizeExprList(call.getInputParams());
            Ast.Stmt.Call optimized = new Ast.Stmt.Call(call.getName(), args, call.getLineNum());
            optimized.setReturnType(call.getReturnType());
            return optimized;
        }
        return stmt;
    }

    private ArrayList<Ast.Expr.Base> optimizeExprList(ArrayList<Ast.Expr.Base> exprs) {
        ArrayList<Ast.Expr.Base> result = new ArrayList<Ast.Expr.Base>();
        if (exprs != null) {
            for (Ast.Expr.Base expr : exprs) {
                result.add(optimizeExpr(expr));
            }
        }
        return result;
    }

    private Ast.Expr.Base optimizeExpr(Ast.Expr.Base expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof Ast.Expr.UnaryMinus) {
            Ast.Expr.UnaryMinus node = (Ast.Expr.UnaryMinus) expr;
            Ast.Expr.Base inner = optimizeExpr(node.getExpr());
            if (isLiteral(inner)) {
                if (inner instanceof Ast.Expr.IntLiteral) {
                    return new Ast.Expr.IntLiteral(-((Ast.Expr.IntLiteral) inner).getValue(), node.getLineNum());
                } else if (inner instanceof Ast.Expr.FloatLiteral) {
                    return new Ast.Expr.FloatLiteral(-((Ast.Expr.FloatLiteral) inner).getValue(), node.getLineNum());
                } else if (inner instanceof Ast.Expr.DoubleLiteral) {
                    return new Ast.Expr.DoubleLiteral(-((Ast.Expr.DoubleLiteral) inner).getValue(), node.getLineNum());
                }
            }
            return new Ast.Expr.UnaryMinus(inner, node.getLineNum());
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
            Ast.Expr.Base left = optimizeExpr(node.getLeft());
            Ast.Expr.Base right = optimizeExpr(node.getRight());
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
            Ast.Expr.Base left = optimizeExpr(node.getLeft());
            Ast.Expr.Base right = optimizeExpr(node.getRight());
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
            Ast.Expr.Base inner = optimizeExpr(node.getExpr());
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

    private Ast.Expr.Base foldArithmetic(String op, Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
        if (isLiteral(left) && isLiteral(right)) {
            if (("/".equals(op) || "%".equals(op)) && isZero(right)) {
                return rebuildArithmetic(op, left, right, lineNum);
            }
            return number(resultType(left, right), calculate(op, left, right), lineNum);
        }
        Ast.Expr.Base simplified = simplifyArithmeticIdentity(op, left, right, lineNum);
        if (simplified != null) {
            return simplified;
        }
        return rebuildArithmetic(op, left, right, lineNum);
    }

    private Ast.Expr.Base simplifyArithmeticIdentity(String op, Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
        boolean integerOperation = isIntegerOperation(left, right);
        if ("+".equals(op)) {
            if (integerOperation && isZero(left)) return right;
            if (integerOperation && isZero(right)) return left;
        } else if ("-".equals(op)) {
            if (integerOperation && isZero(right)) return left;
        } else if ("*".equals(op)) {
            if (integerOperation && isZero(left) && canDiscardEvaluation(right)) {
                return zeroFor(left, right, lineNum);
            }
            if (integerOperation && isZero(right) && canDiscardEvaluation(left)) {
                return zeroFor(left, right, lineNum);
            }
            if (isOne(left) && preservesResultType(right, left, right)) return right;
            if (isOne(right) && preservesResultType(left, left, right)) return left;
        } else if ("/".equals(op)) {
            if (isOne(right) && preservesResultType(left, left, right)) return left;
        }
        return null;
    }

    private boolean canDiscardEvaluation(Ast.Expr.Base expr) {
        return evaluationEffect(expr) == EvaluationEffect.PURE;
    }

    private EvaluationEffect evaluationEffect(Ast.Expr.Base expr) {
        if (expr == null) {
            return EvaluationEffect.PURE;
        }
        if (isLiteral(expr)
                || expr instanceof Ast.Expr.True
                || expr instanceof Ast.Expr.False
                || expr instanceof Ast.Expr.Id
                || expr instanceof Ast.Expr.Str) {
            return EvaluationEffect.PURE;
        }
        if (expr instanceof Ast.Expr.Call) {
            return EvaluationEffect.HAS_SIDE_EFFECT;
        }
        if (expr instanceof Ast.Expr.ArrayAccess || expr instanceof Ast.Expr.ArrayLength) {
            return EvaluationEffect.MAY_TRAP;
        }
        if (expr instanceof Ast.Expr.Not) {
            return evaluationEffect(((Ast.Expr.Not) expr).getExpr());
        }
        if (expr instanceof Ast.Expr.Add) {
            Ast.Expr.Add node = (Ast.Expr.Add) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.Sub) {
            Ast.Expr.Sub node = (Ast.Expr.Sub) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.Mul) {
            Ast.Expr.Mul node = (Ast.Expr.Mul) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.Div) {
            Ast.Expr.Div node = (Ast.Expr.Div) expr;
            EvaluationEffect children = combineEffects(node.getLeft(), node.getRight());
            return isIntegerOperation(node.getLeft(), node.getRight())
                    ? EvaluationEffect.combine(children, EvaluationEffect.MAY_TRAP)
                    : children;
        }
        if (expr instanceof Ast.Expr.Mod) {
            Ast.Expr.Mod node = (Ast.Expr.Mod) expr;
            return EvaluationEffect.combine(
                    combineEffects(node.getLeft(), node.getRight()), EvaluationEffect.MAY_TRAP);
        }
        if (expr instanceof Ast.Expr.And) {
            Ast.Expr.And node = (Ast.Expr.And) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.Or) {
            Ast.Expr.Or node = (Ast.Expr.Or) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.GT) {
            Ast.Expr.GT node = (Ast.Expr.GT) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.LT) {
            Ast.Expr.LT node = (Ast.Expr.LT) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.GTE) {
            Ast.Expr.GTE node = (Ast.Expr.GTE) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.LTE) {
            Ast.Expr.LTE node = (Ast.Expr.LTE) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.EQ) {
            Ast.Expr.EQ node = (Ast.Expr.EQ) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        if (expr instanceof Ast.Expr.NEQ) {
            Ast.Expr.NEQ node = (Ast.Expr.NEQ) expr;
            return combineEffects(node.getLeft(), node.getRight());
        }
        return EvaluationEffect.HAS_SIDE_EFFECT;
    }

    private EvaluationEffect combineEffects(Ast.Expr.Base left, Ast.Expr.Base right) {
        return EvaluationEffect.combine(evaluationEffect(left), evaluationEffect(right));
    }

    private boolean isIntegerOperation(Ast.Expr.Base left, Ast.Expr.Base right) {
        Ast.Type.Base type = promotedNumericType(numericTypeOf(left), numericTypeOf(right));
        return type != null && type.getKind() == TypeKind.INT;
    }

    private boolean preservesResultType(Ast.Expr.Base candidate, Ast.Expr.Base left, Ast.Expr.Base right) {
        Ast.Type.Base candidateType = numericTypeOf(candidate);
        Ast.Type.Base result = promotedNumericType(numericTypeOf(left), numericTypeOf(right));
        return candidateType != null && result != null && candidateType.getKind() == result.getKind();
    }

    private Ast.Expr.Base foldComparison(String op, Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
        if (isLiteral(left) && isLiteral(right)) {
            boolean value = compare(op, left, right);
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

    private Ast.Expr.Base rebuildArithmetic(String op, Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
        if ("+".equals(op)) return new Ast.Expr.Add(left, right, lineNum);
        if ("-".equals(op)) return new Ast.Expr.Sub(left, right, lineNum);
        if ("*".equals(op)) return new Ast.Expr.Mul(left, right, lineNum);
        if ("/".equals(op)) return new Ast.Expr.Div(left, right, lineNum);
        return new Ast.Expr.Mod(left, right, lineNum);
    }

    private Ast.Expr.Base rebuildComparison(String op, Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
        if (">".equals(op)) return new Ast.Expr.GT(left, right, lineNum);
        if ("<".equals(op)) return new Ast.Expr.LT(left, right, lineNum);
        if (">=".equals(op)) return new Ast.Expr.GTE(left, right, lineNum);
        if ("<=".equals(op)) return new Ast.Expr.LTE(left, right, lineNum);
        if ("==".equals(op)) return new Ast.Expr.EQ(left, right, lineNum);
        return new Ast.Expr.NEQ(left, right, lineNum);
    }

    private Boolean boolValue(Ast.Expr.Base expr) {
        if (expr instanceof Ast.Expr.True) return Boolean.TRUE;
        if (expr instanceof Ast.Expr.False) return Boolean.FALSE;
        return null;
    }

    private boolean isZero(Ast.Expr.Base expr) {
        return isLiteral(expr) && numericValue(expr).doubleValue() == 0.0d;
    }

    private boolean isOne(Ast.Expr.Base expr) {
        return isLiteral(expr) && numericValue(expr).doubleValue() == 1.0d;
    }

    private Ast.Expr.Base zeroFor(Ast.Expr.Base left, Ast.Expr.Base right, int lineNum) {
        Ast.Type.Base type = promotedNumericType(numericTypeOf(left), numericTypeOf(right));
        if (type == null) {
            type = new Ast.Type.Int();
        }
        return number(type, 0, lineNum);
    }

    private Ast.Type.Base numericTypeOf(Ast.Expr.Base expr) {
        if (isLiteral(expr)) {
            return typeOfLiteral(expr);
        }
        if (expr instanceof Ast.Expr.Id) {
            return numericOrNull(((Ast.Expr.Id) expr).getType());
        }
        if (expr instanceof Ast.Expr.Call) {
            return numericOrNull(((Ast.Expr.Call) expr).getReturnType());
        }
        if (expr instanceof Ast.Expr.ArrayAccess) {
            return numericOrNull(((Ast.Expr.ArrayAccess) expr).getElementType());
        }
        if (expr instanceof Ast.Expr.Add) {
            Ast.Expr.Add node = (Ast.Expr.Add) expr;
            return promotedNumericType(numericTypeOf(node.getLeft()), numericTypeOf(node.getRight()));
        }
        if (expr instanceof Ast.Expr.Sub) {
            Ast.Expr.Sub node = (Ast.Expr.Sub) expr;
            return promotedNumericType(numericTypeOf(node.getLeft()), numericTypeOf(node.getRight()));
        }
        if (expr instanceof Ast.Expr.Mul) {
            Ast.Expr.Mul node = (Ast.Expr.Mul) expr;
            return promotedNumericType(numericTypeOf(node.getLeft()), numericTypeOf(node.getRight()));
        }
        if (expr instanceof Ast.Expr.Div) {
            Ast.Expr.Div node = (Ast.Expr.Div) expr;
            return promotedNumericType(numericTypeOf(node.getLeft()), numericTypeOf(node.getRight()));
        }
        if (expr instanceof Ast.Expr.Mod) {
            return new Ast.Type.Int();
        }
        return null;
    }

    private Ast.Type.Base numericOrNull(Ast.Type.Base type) {
        if (type == null) {
            return null;
        }
        TypeKind kind = type.getKind();
        if (kind == TypeKind.INT || kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            return type;
        }
        return null;
    }

    private Ast.Type.Base promotedNumericType(Ast.Type.Base left, Ast.Type.Base right) {
        if (left == null || right == null) {
            return left != null ? left : right;
        }
        if (left.getKind() == TypeKind.DOUBLE || right.getKind() == TypeKind.DOUBLE) {
            return new Ast.Type.Double();
        }
        if (left.getKind() == TypeKind.FLOAT || right.getKind() == TypeKind.FLOAT) {
            return new Ast.Type.Float();
        }
        return new Ast.Type.Int();
    }

    private Ast.Type.Base typeOfLiteral(Ast.Expr.Base expr) {
        if (expr instanceof Ast.Expr.IntLiteral) return new Ast.Type.Int();
        if (expr instanceof Ast.Expr.FloatLiteral) return new Ast.Type.Float();
        return new Ast.Type.Double();
    }

    private Ast.Type.Base resultType(Ast.Expr.Base left, Ast.Expr.Base right) {
        if (left instanceof Ast.Expr.DoubleLiteral || right instanceof Ast.Expr.DoubleLiteral) return new Ast.Type.Double();
        if (left instanceof Ast.Expr.FloatLiteral || right instanceof Ast.Expr.FloatLiteral) return new Ast.Type.Float();
        return new Ast.Type.Int();
    }


    private Ast.Expr.Base number(Ast.Type.Base type, Object value, int lineNum) {
        if (type.getKind() == TypeKind.DOUBLE) return new Ast.Expr.DoubleLiteral(Double.valueOf(asDouble(value)), lineNum);
        if (type.getKind() == TypeKind.FLOAT) return new Ast.Expr.FloatLiteral(Float.valueOf((float) asDouble(value)), lineNum);
        return new Ast.Expr.IntLiteral(Integer.valueOf((int) asDouble(value)), lineNum);
    }


    private Object calculate(String op, Ast.Expr.Base left, Ast.Expr.Base right) {
        Ast.Type.Base type = resultType(left, right);
        if (type.getKind() == TypeKind.INT) {
            int l = numericValue(left).intValue();
            int r = numericValue(right).intValue();
            if ("+".equals(op)) return l + r;
            if ("-".equals(op)) return l - r;
            if ("*".equals(op)) return l * r;
            if ("/".equals(op)) return l / r;
            return l % r;
        }
        if (type.getKind() == TypeKind.FLOAT) {
            float l = numericValue(left).floatValue();
            float r = numericValue(right).floatValue();
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

    private boolean compare(String op, Ast.Expr.Base left, Ast.Expr.Base right) {
        double l = numericValue(left).doubleValue();
        double r = numericValue(right).doubleValue();
        if (">".equals(op)) return l > r;
        if ("<".equals(op)) return l < r;
        if (">=".equals(op)) return l >= r;
        if ("<=".equals(op)) return l <= r;
        if ("==".equals(op)) return l == r;
        return l != r;
    }

    private Number numericValue(Ast.Expr.Base number) {
        if (number instanceof Ast.Expr.IntLiteral) return ((Ast.Expr.IntLiteral)number).getValue();
        if (number instanceof Ast.Expr.FloatLiteral) return ((Ast.Expr.FloatLiteral)number).getValue();
        return ((Ast.Expr.DoubleLiteral)number).getValue();
    }


    private boolean isLiteral(Ast.Expr.Base expr) {
        return expr instanceof Ast.Expr.IntLiteral || expr instanceof Ast.Expr.FloatLiteral || expr instanceof Ast.Expr.DoubleLiteral;
    }

    private double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
    }

    private enum EvaluationEffect {
        PURE,
        MAY_TRAP,
        HAS_SIDE_EFFECT;

        private static EvaluationEffect combine(EvaluationEffect left, EvaluationEffect right) {
            return left.ordinal() >= right.ordinal() ? left : right;
        }
    }
}
