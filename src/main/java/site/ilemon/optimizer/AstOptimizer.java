package site.ilemon.optimizer;

import site.ilemon.source.SourceSpan;
import site.ilemon.typedast.TypedAst;

import java.util.ArrayList;
import java.util.List;

/** Local, side-effect-aware optimizations over semantically valid Typed-AST. */
public final class AstOptimizer {
    public TypedAst.Program optimize(TypedAst.Program program) {
        ArrayList<TypedAst.Method> methods = new ArrayList<TypedAst.Method>();
        for (TypedAst.Method method : program.getMethods()) {
            methods.add(optimizeMethod(method));
        }
        return new TypedAst.Program(program.getClassName(), methods, program.getSourceSpan());
    }

    private TypedAst.Method optimizeMethod(TypedAst.Method method) {
        return new TypedAst.Method(method.getSymbol(), method.getFormals(), method.getLocals(),
                optimizeStatements(method.getStatements()), method.getSourceSpan());
    }

    private List<TypedAst.Stmt> optimizeStatements(List<TypedAst.Stmt> statements) {
        ArrayList<TypedAst.Stmt> result = new ArrayList<TypedAst.Stmt>();
        for (TypedAst.Stmt statement : statements) {
            TypedAst.Stmt optimized = optimizeStatement(statement);
            if (optimized instanceof TypedAst.Block
                    && ((TypedAst.Block) optimized).getStatements().isEmpty()) {
                continue;
            }
            result.add(optimized);
        }
        return result;
    }

    private TypedAst.Stmt optimizeStatement(TypedAst.Stmt statement) {
        if (statement instanceof TypedAst.Assign) {
            TypedAst.Assign node = (TypedAst.Assign) statement;
            return new TypedAst.Assign(node.getTarget(), optimizeExpression(node.getExpression()),
                    node.getSourceSpan());
        }
        if (statement instanceof TypedAst.VarDecl) {
            TypedAst.VarDecl node = (TypedAst.VarDecl) statement;
            return new TypedAst.VarDecl(node.getDeclaration(),
                    optimizeExpression(node.getInitializer()), node.getSourceSpan());
        }
        if (statement instanceof TypedAst.ArrayAssign) {
            TypedAst.ArrayAssign node = (TypedAst.ArrayAssign) statement;
            return new TypedAst.ArrayAssign(node.getArray(), optimizeExpression(node.getIndex()),
                    optimizeExpression(node.getExpression()), node.getSourceSpan());
        }
        if (statement instanceof TypedAst.Block) {
            TypedAst.Block node = (TypedAst.Block) statement;
            return new TypedAst.Block(
                    optimizeStatements(node.getStatements()), node.getSourceSpan());
        }
        if (statement instanceof TypedAst.If) {
            TypedAst.If node = (TypedAst.If) statement;
            TypedAst.Expr condition = optimizeExpression(node.getCondition());
            TypedAst.Stmt thenStatement = optimizeStatement(node.getThenStatement());
            TypedAst.Stmt elseStatement = node.getElseStatement() == null
                    ? null : optimizeStatement(node.getElseStatement());
            Boolean value = boolValue(condition);
            if (Boolean.TRUE.equals(value)) {
                return thenStatement;
            }
            if (Boolean.FALSE.equals(value)) {
                return elseStatement == null
                        ? new TypedAst.Block(
                                new ArrayList<TypedAst.Stmt>(), node.getSourceSpan())
                        : elseStatement;
            }
            return new TypedAst.If(
                    condition, thenStatement, elseStatement, node.getSourceSpan());
        }
        if (statement instanceof TypedAst.While) {
            TypedAst.While node = (TypedAst.While) statement;
            TypedAst.Expr condition = optimizeExpression(node.getCondition());
            if (Boolean.FALSE.equals(boolValue(condition))) {
                return new TypedAst.Block(
                        new ArrayList<TypedAst.Stmt>(), node.getSourceSpan());
            }
            return new TypedAst.While(
                    condition, optimizeStatement(node.getBody()), node.getSourceSpan());
        }
        if (statement instanceof TypedAst.For) {
            TypedAst.For node = (TypedAst.For) statement;
            return new TypedAst.For(
                    node.getInitializer() == null ? null : optimizeStatement(node.getInitializer()),
                    optimizeExpression(node.getCondition()),
                    node.getUpdate() == null ? null : optimizeStatement(node.getUpdate()),
                    optimizeStatement(node.getBody()), node.getSourceSpan());
        }
        if (statement instanceof TypedAst.Return) {
            TypedAst.Return node = (TypedAst.Return) statement;
            return new TypedAst.Return(
                    optimizeExpression(node.getExpression()), node.getSourceSpan());
        }
        if (statement instanceof TypedAst.Printf) {
            TypedAst.Printf node = (TypedAst.Printf) statement;
            return new TypedAst.Printf(node.getFormat(), optimizeExpressions(node.getExpressions()),
                    node.getSourceSpan());
        }
        if (statement instanceof TypedAst.CallStmt) {
            TypedAst.CallStmt node = (TypedAst.CallStmt) statement;
            return new TypedAst.CallStmt(node.getMethod(), optimizeExpressions(node.getArguments()),
                    node.getSourceSpan());
        }
        return statement;
    }

    private List<TypedAst.Expr> optimizeExpressions(List<TypedAst.Expr> expressions) {
        ArrayList<TypedAst.Expr> result = new ArrayList<TypedAst.Expr>();
        for (TypedAst.Expr expression : expressions) {
            result.add(optimizeExpression(expression));
        }
        return result;
    }

    private TypedAst.Expr optimizeExpression(TypedAst.Expr expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof TypedAst.UnaryMinus) {
            TypedAst.UnaryMinus node = (TypedAst.UnaryMinus) expression;
            TypedAst.Expr inner = optimizeExpression(node.getExpression());
            if (inner instanceof TypedAst.IntLiteral) {
                return new TypedAst.IntLiteral(-((TypedAst.IntLiteral) inner).getValue(), null,
                        node.getSourceSpan());
            }
            if (inner instanceof TypedAst.FloatLiteral) {
                return new TypedAst.FloatLiteral(-((TypedAst.FloatLiteral) inner).getValue(), null,
                        node.getSourceSpan());
            }
            if (inner instanceof TypedAst.DoubleLiteral) {
                return new TypedAst.DoubleLiteral(-((TypedAst.DoubleLiteral) inner).getValue(), null,
                        node.getSourceSpan());
            }
            return new TypedAst.UnaryMinus(node.getType(), inner, node.getSourceSpan());
        }
        if (isArithmetic(expression)) {
            TypedAst.BinaryExpr node = (TypedAst.BinaryExpr) expression;
            return foldArithmetic(arithmeticOperator(expression), optimizeExpression(node.getLeft()),
                    optimizeExpression(node.getRight()), node.getType(), node.getSourceSpan());
        }
        if (expression instanceof TypedAst.And || expression instanceof TypedAst.Or) {
            TypedAst.BinaryExpr node = (TypedAst.BinaryExpr) expression;
            TypedAst.Expr left = optimizeExpression(node.getLeft());
            TypedAst.Expr right = optimizeExpression(node.getRight());
            Boolean leftValue = boolValue(left);
            Boolean rightValue = boolValue(right);
            if (expression instanceof TypedAst.And) {
                if (Boolean.FALSE.equals(leftValue)) return bool(false, node.getSourceSpan());
                if (Boolean.TRUE.equals(leftValue)) return right;
                if (Boolean.TRUE.equals(rightValue)) return left;
                if (Boolean.FALSE.equals(rightValue) && canDiscardEvaluation(left)) {
                    return bool(false, node.getSourceSpan());
                }
                return new TypedAst.And(left, right, node.getSourceSpan());
            }
            if (Boolean.TRUE.equals(leftValue)) return bool(true, node.getSourceSpan());
            if (Boolean.FALSE.equals(leftValue)) return right;
            if (Boolean.FALSE.equals(rightValue)) return left;
            if (Boolean.TRUE.equals(rightValue) && canDiscardEvaluation(left)) {
                return bool(true, node.getSourceSpan());
            }
            return new TypedAst.Or(left, right, node.getSourceSpan());
        }
        if (expression instanceof TypedAst.Not) {
            TypedAst.Not node = (TypedAst.Not) expression;
            TypedAst.Expr inner = optimizeExpression(node.getExpression());
            Boolean value = boolValue(inner);
            return value == null ? new TypedAst.Not(inner, node.getSourceSpan())
                    : bool(!value.booleanValue(), node.getSourceSpan());
        }
        if (isComparison(expression)) {
            TypedAst.BinaryExpr node = (TypedAst.BinaryExpr) expression;
            return foldComparison(comparisonOperator(expression), optimizeExpression(node.getLeft()),
                    optimizeExpression(node.getRight()), node.getSourceSpan());
        }
        if (expression instanceof TypedAst.Call) {
            TypedAst.Call node = (TypedAst.Call) expression;
            return new TypedAst.Call(node.getMethod(), optimizeExpressions(node.getArguments()),
                    node.getType(), node.getSourceSpan());
        }
        if (expression instanceof TypedAst.ArrayAccess) {
            TypedAst.ArrayAccess node = (TypedAst.ArrayAccess) expression;
            return new TypedAst.ArrayAccess(node.getArray(), optimizeExpression(node.getIndex()),
                    node.getType(), node.getSourceSpan());
        }
        return expression;
    }

    private TypedAst.Expr foldArithmetic(String operator, TypedAst.Expr left, TypedAst.Expr right,
                                         TypedAst.Type resultType, SourceSpan sourceSpan) {
        if (isNumberLiteral(left) && isNumberLiteral(right)) {
            if (("/".equals(operator) || "%".equals(operator)) && isZero(right)) {
                return rebuildArithmetic(operator, left, right, resultType, sourceSpan);
            }
            return number(resultType, calculate(operator, left, right, resultType), sourceSpan);
        }
        TypedAst.Expr simplified =
                simplifyArithmetic(operator, left, right, resultType, sourceSpan);
        return simplified == null
                ? rebuildArithmetic(operator, left, right, resultType, sourceSpan)
                : simplified;
    }

    private TypedAst.Expr simplifyArithmetic(String operator, TypedAst.Expr left, TypedAst.Expr right,
                                             TypedAst.Type resultType, SourceSpan sourceSpan) {
        boolean integerOperation = resultType == TypedAst.Type.INT;
        if ("+".equals(operator)) {
            if (integerOperation && isZero(left)) return right;
            if (integerOperation && isZero(right)) return left;
        } else if ("-".equals(operator)) {
            if (integerOperation && isZero(right)) return left;
        } else if ("*".equals(operator)) {
            if (integerOperation && isZero(left) && canDiscardEvaluation(right)) {
                return number(resultType, 0, sourceSpan);
            }
            if (integerOperation && isZero(right) && canDiscardEvaluation(left)) {
                return number(resultType, 0, sourceSpan);
            }
            if (isOne(left) && right.getType() == resultType) return right;
            if (isOne(right) && left.getType() == resultType) return left;
        } else if ("/".equals(operator) && isOne(right) && left.getType() == resultType) {
            return left;
        }
        return null;
    }

    private TypedAst.Expr foldComparison(String operator, TypedAst.Expr left,
                                         TypedAst.Expr right, SourceSpan sourceSpan) {
        if (isNumberLiteral(left) && isNumberLiteral(right)) {
            return bool(compare(operator, left, right), sourceSpan);
        }
        Boolean leftBool = boolValue(left);
        Boolean rightBool = boolValue(right);
        if (leftBool != null && rightBool != null
                && ("==".equals(operator) || "!=".equals(operator))) {
            boolean equal = leftBool.booleanValue() == rightBool.booleanValue();
            return bool("==".equals(operator) == equal, sourceSpan);
        }
        return rebuildComparison(operator, left, right, sourceSpan);
    }

    private EvaluationEffect evaluationEffect(TypedAst.Expr expression) {
        if (expression == null || isNumberLiteral(expression)
                || expression instanceof TypedAst.BoolLiteral
                || expression instanceof TypedAst.Id
                || expression instanceof TypedAst.StringLiteral) {
            return EvaluationEffect.PURE;
        }
        if (expression instanceof TypedAst.Call) {
            return EvaluationEffect.HAS_SIDE_EFFECT;
        }
        if (expression instanceof TypedAst.ArrayAccess || expression instanceof TypedAst.ArrayLength) {
            return EvaluationEffect.MAY_TRAP;
        }
        if (expression instanceof TypedAst.Not) {
            return evaluationEffect(((TypedAst.Not) expression).getExpression());
        }
        if (expression instanceof TypedAst.UnaryMinus) {
            return evaluationEffect(((TypedAst.UnaryMinus) expression).getExpression());
        }
        if (expression instanceof TypedAst.BinaryExpr) {
            TypedAst.BinaryExpr binary = (TypedAst.BinaryExpr) expression;
            EvaluationEffect children = EvaluationEffect.combine(
                    evaluationEffect(binary.getLeft()), evaluationEffect(binary.getRight()));
            if (expression instanceof TypedAst.Mod
                    || (expression instanceof TypedAst.Div && expression.getType() == TypedAst.Type.INT)) {
                return EvaluationEffect.combine(children, EvaluationEffect.MAY_TRAP);
            }
            return children;
        }
        return EvaluationEffect.HAS_SIDE_EFFECT;
    }

    private boolean canDiscardEvaluation(TypedAst.Expr expression) {
        return evaluationEffect(expression) == EvaluationEffect.PURE;
    }

    private TypedAst.Expr rebuildArithmetic(String operator, TypedAst.Expr left, TypedAst.Expr right,
                                            TypedAst.Type type, SourceSpan sourceSpan) {
        if ("+".equals(operator)) return new TypedAst.Add(type, left, right, sourceSpan);
        if ("-".equals(operator)) return new TypedAst.Sub(type, left, right, sourceSpan);
        if ("*".equals(operator)) return new TypedAst.Mul(type, left, right, sourceSpan);
        if ("/".equals(operator)) return new TypedAst.Div(type, left, right, sourceSpan);
        return new TypedAst.Mod(left, right, sourceSpan);
    }

    private TypedAst.Expr rebuildComparison(String operator, TypedAst.Expr left,
                                            TypedAst.Expr right, SourceSpan sourceSpan) {
        if (">".equals(operator)) return new TypedAst.GT(left, right, sourceSpan);
        if ("<".equals(operator)) return new TypedAst.LT(left, right, sourceSpan);
        if (">=".equals(operator)) return new TypedAst.GTE(left, right, sourceSpan);
        if ("<=".equals(operator)) return new TypedAst.LTE(left, right, sourceSpan);
        if ("==".equals(operator)) return new TypedAst.EQ(left, right, sourceSpan);
        return new TypedAst.NEQ(left, right, sourceSpan);
    }

    private TypedAst.Expr number(TypedAst.Type type, Number value, SourceSpan sourceSpan) {
        if (type == TypedAst.Type.DOUBLE) {
            return new TypedAst.DoubleLiteral(value.doubleValue(), null, sourceSpan);
        }
        if (type == TypedAst.Type.FLOAT) {
            return new TypedAst.FloatLiteral(value.floatValue(), null, sourceSpan);
        }
        return new TypedAst.IntLiteral(value.intValue(), null, sourceSpan);
    }

    private Number calculate(String operator, TypedAst.Expr left, TypedAst.Expr right,
                             TypedAst.Type type) {
        if (type == TypedAst.Type.INT) {
            int l = numericValue(left).intValue();
            int r = numericValue(right).intValue();
            if ("+".equals(operator)) return l + r;
            if ("-".equals(operator)) return l - r;
            if ("*".equals(operator)) return l * r;
            if ("/".equals(operator)) return l / r;
            return l % r;
        }
        if (type == TypedAst.Type.FLOAT) {
            float l = numericValue(left).floatValue();
            float r = numericValue(right).floatValue();
            if ("+".equals(operator)) return l + r;
            if ("-".equals(operator)) return l - r;
            if ("*".equals(operator)) return l * r;
            return l / r;
        }
        double l = doubleValueOf(left);
        double r = doubleValueOf(right);
        if ("+".equals(operator)) return l + r;
        if ("-".equals(operator)) return l - r;
        if ("*".equals(operator)) return l * r;
        return l / r;
    }

    /**
     * 取字面量在 double 上下文中的值。
     *
     * <p>小数字面量的类型是 float，但它携带的是一个十进制常量。用在 double 位置时必须按
     * 十进制原文以 double 精度取值，而不是先舍入成 float 再展宽——这一点必须与
     * {@code AstToIrTranslator.numericLiteralForTarget} 保持一致，否则折叠出来的常量
     * 会和没被折叠时算出的结果不同。</p>
     */
    private double doubleValueOf(TypedAst.Expr expression) {
        if (expression instanceof TypedAst.FloatLiteral) {
            return Double.parseDouble(((TypedAst.FloatLiteral) expression).getRawValue());
        }
        return numericValue(expression).doubleValue();
    }

    /**
     * 折叠数值比较。操作数必须先按运行时的提升类型舍入，否则折叠结果会和后端不一致：
     * {@code 16777217 > 16777216.0} 在 float 精度下为 false，若一律按 double 比较则为 true。
     */
    private boolean compare(String operator, TypedAst.Expr left, TypedAst.Expr right) {
        TypedAst.Type type = promotedLiteralType(left, right);
        double l = valueAs(left, type);
        double r = valueAs(right, type);
        if (">".equals(operator)) return l > r;
        if ("<".equals(operator)) return l < r;
        if (">=".equals(operator)) return l >= r;
        if ("<=".equals(operator)) return l <= r;
        if ("==".equals(operator)) return l == r;
        return l != r;
    }

    /** 数值字面量比较的操作数提升类型，与 SemanticVisitor 的提升规则保持一致。 */
    private TypedAst.Type promotedLiteralType(TypedAst.Expr left, TypedAst.Expr right) {
        if (left instanceof TypedAst.DoubleLiteral || right instanceof TypedAst.DoubleLiteral) {
            return TypedAst.Type.DOUBLE;
        }
        if (left instanceof TypedAst.FloatLiteral || right instanceof TypedAst.FloatLiteral) {
            return TypedAst.Type.FLOAT;
        }
        return TypedAst.Type.INT;
    }

    /**
     * 按提升类型取值后再展宽为 double。int 和 float 到 double 都是无损的，
     * 所以"按哪个类型取值"这一步（而不是比较宽度）才是与运行时对齐的关键。
     */
    private double valueAs(TypedAst.Expr expression, TypedAst.Type type) {
        if (type == TypedAst.Type.INT) {
            return numericValue(expression).intValue();
        }
        if (type == TypedAst.Type.FLOAT) {
            return numericValue(expression).floatValue();
        }
        return doubleValueOf(expression);
    }

    private Number numericValue(TypedAst.Expr expression) {
        if (expression instanceof TypedAst.IntLiteral) return ((TypedAst.IntLiteral) expression).getValue();
        if (expression instanceof TypedAst.FloatLiteral) return ((TypedAst.FloatLiteral) expression).getValue();
        return ((TypedAst.DoubleLiteral) expression).getValue();
    }

    private TypedAst.BoolLiteral bool(boolean value, SourceSpan sourceSpan) {
        return new TypedAst.BoolLiteral(value, sourceSpan);
    }

    private Boolean boolValue(TypedAst.Expr expression) {
        return expression instanceof TypedAst.BoolLiteral
                ? Boolean.valueOf(((TypedAst.BoolLiteral) expression).getValue()) : null;
    }

    private boolean isZero(TypedAst.Expr expression) {
        return isNumberLiteral(expression) && numericValue(expression).doubleValue() == 0.0d;
    }

    private boolean isOne(TypedAst.Expr expression) {
        return isNumberLiteral(expression) && numericValue(expression).doubleValue() == 1.0d;
    }

    private boolean isNumberLiteral(TypedAst.Expr expression) {
        return expression instanceof TypedAst.IntLiteral
                || expression instanceof TypedAst.FloatLiteral
                || expression instanceof TypedAst.DoubleLiteral;
    }

    private boolean isArithmetic(TypedAst.Expr expression) {
        return expression instanceof TypedAst.Add || expression instanceof TypedAst.Sub
                || expression instanceof TypedAst.Mul || expression instanceof TypedAst.Div
                || expression instanceof TypedAst.Mod;
    }

    private String arithmeticOperator(TypedAst.Expr expression) {
        if (expression instanceof TypedAst.Add) return "+";
        if (expression instanceof TypedAst.Sub) return "-";
        if (expression instanceof TypedAst.Mul) return "*";
        if (expression instanceof TypedAst.Div) return "/";
        return "%";
    }

    private boolean isComparison(TypedAst.Expr expression) {
        return expression instanceof TypedAst.EQ || expression instanceof TypedAst.NEQ
                || expression instanceof TypedAst.GT || expression instanceof TypedAst.LT
                || expression instanceof TypedAst.GTE || expression instanceof TypedAst.LTE;
    }

    private String comparisonOperator(TypedAst.Expr expression) {
        if (expression instanceof TypedAst.EQ) return "==";
        if (expression instanceof TypedAst.NEQ) return "!=";
        if (expression instanceof TypedAst.GT) return ">";
        if (expression instanceof TypedAst.LT) return "<";
        if (expression instanceof TypedAst.GTE) return ">=";
        return "<=";
    }

    private enum EvaluationEffect {
        PURE,
        MAY_TRAP,
        HAS_SIDE_EFFECT;

        static EvaluationEffect combine(EvaluationEffect left, EvaluationEffect right) {
            return left.ordinal() >= right.ordinal() ? left : right;
        }
    }
}
