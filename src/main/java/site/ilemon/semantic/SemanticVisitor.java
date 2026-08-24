package site.ilemon.semantic;

import site.ilemon.ast.Ast;
import site.ilemon.exception.SemanticException;
import site.ilemon.source.SourceSpan;
import site.ilemon.typedast.TypedAst;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Semantic boundary from the parser AST to the immutable Typed-AST.
 *
 * <p>The historical class name is retained for source compatibility. This is
 * no longer an AST visitor that stores inferred types back into parser nodes;
 * every analysis operation returns a new typed node.</p>
 */
public final class SemanticVisitor {
    private static final int MAX_ANALYSIS_DEPTH = 256;

    private final boolean collectErrors;
    private final ArrayList<String> errors = new ArrayList<String>();
    private final ArrayList<Integer> errorLineNumbers = new ArrayList<Integer>();
    private final IdentityHashMap<Ast.Expr.Base, TypedAst.Expr> typedExpressions =
            new IdentityHashMap<Ast.Expr.Base, TypedAst.Expr>();
    private final IdentityHashMap<Ast.Stmt.Base, TypedAst.Stmt> typedStatements =
            new IdentityHashMap<Ast.Stmt.Base, TypedAst.Stmt>();
    private final IdentityHashMap<Ast.Method.MethodSingle, TypedAst.MethodSymbol> sourceMethodSymbols =
            new IdentityHashMap<Ast.Method.MethodSingle, TypedAst.MethodSymbol>();

    private final Map<String, TypedAst.MethodSymbol> methods =
            new HashMap<String, TypedAst.MethodSymbol>();

    /** 方法内所有局部声明，按分析顺序排列；被遮蔽的名字会得到各自独立的符号。 */
    private List<TypedAst.Declaration> methodLocals;
    private Deque<Map<String, TypedAst.Symbol>> variableScopes;
    private Set<TypedAst.Symbol> unassigned;
    private TypedAst.Type currentReturnType;
    private int loopDepth;
    private int analysisDepth;
    private SemanticResult result;

    public SemanticVisitor() {
        this(false);
    }

    private SemanticVisitor(boolean collectErrors) {
        this.collectErrors = collectErrors;
    }

    public static SemanticVisitor collecting() {
        return new SemanticVisitor(true);
    }

    public SemanticResult analyze(Ast.Program.Base source) {
        reset();
        TypedAst.Program program = analyzeProgram(source);
        this.result = new SemanticResult(program, errors, errorLineNumbers,
                typedExpressions, typedStatements);
        return result;
    }

    public void visit(Ast.Program.Base source) {
        analyze(source);
    }

    public boolean passOrNot() {
        return errors.isEmpty();
    }

    public ArrayList<String> getErrors() {
        return new ArrayList<String>(errors);
    }

    public ArrayList<Integer> getErrorLineNumbers() {
        return new ArrayList<Integer>(errorLineNumbers);
    }

    public SemanticResult getResult() {
        if (result == null) {
            throw new IllegalStateException("semantic analysis has not run");
        }
        return result;
    }

    public TypedAst.Program getTypedProgram() {
        return getResult().getProgram();
    }

    private void reset() {
        errors.clear();
        errorLineNumbers.clear();
        typedExpressions.clear();
        typedStatements.clear();
        sourceMethodSymbols.clear();
        methods.clear();
        methodLocals = null;
        variableScopes = null;
        unassigned = null;
        currentReturnType = null;
        loopDepth = 0;
        analysisDepth = 0;
        result = null;
    }

    private TypedAst.Program analyzeProgram(Ast.Program.Base source) {
        if (!(source instanceof Ast.Program.ProgramSingle)) {
            error(1, "内部错误: 不支持的程序 AST 节点");
            return new TypedAst.Program("<error>", Collections.<TypedAst.Method>emptyList(),
                    source == null ? SourceSpan.UNKNOWN : source.getSourceSpan());
        }
        Ast.MainClass.Base mainBase = ((Ast.Program.ProgramSingle) source).getMainClass();
        if (!(mainBase instanceof Ast.MainClass.MainClassSingle)) {
            error(1, "内部错误: 不支持的主类 AST 节点");
            return new TypedAst.Program("<error>", Collections.<TypedAst.Method>emptyList(),
                    source.getSourceSpan());
        }

        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle) mainBase;
        declareMethods(mainClass.getMethods());
        validateMainMethod();

        ArrayList<TypedAst.Method> typedMethods = new ArrayList<TypedAst.Method>();
        for (Ast.Method.Base methodBase : mainClass.getMethods()) {
            if (methodBase instanceof Ast.Method.MethodSingle) {
                typedMethods.add(analyzeMethod((Ast.Method.MethodSingle) methodBase));
            }
        }
        return new TypedAst.Program(
                mainClass.getClassId(), typedMethods, source.getSourceSpan());
    }

    private void declareMethods(List<Ast.Method.Base> methodNodes) {
        for (Ast.Method.Base methodBase : methodNodes) {
            Ast.Method.MethodSingle method = (Ast.Method.MethodSingle) methodBase;
            ArrayList<TypedAst.Type> parameterTypes = new ArrayList<TypedAst.Type>();
            for (Ast.Declare.Base formalBase : method.getFormals()) {
                parameterTypes.add(typeOf(((Ast.Declare.DeclareSingle) formalBase).getType()));
            }
            TypedAst.MethodSymbol symbol = new TypedAst.MethodSymbol(method.getId(),
                    typeOf(method.getRetType()), parameterTypes, method.getSourceSpan());
            sourceMethodSymbols.put(method, symbol);
            if (methods.containsKey(method.getId())) {
                error(method.getLineNum(), "重复定义的方法: " + method.getId());
            } else {
                methods.put(method.getId(), symbol);
            }
        }
    }

    private void validateMainMethod() {
        TypedAst.MethodSymbol main = methods.get("main");
        if (main == null) {
            error(1, "程序必须定义 void main()");
            return;
        }
        if (main.getReturnType() != TypedAst.Type.VOID) {
            error(main.getLineNumber(), "main 方法的返回类型必须是 void，实际声明为: "
                    + main.getReturnType());
        }
        if (!main.getParameterTypes().isEmpty()) {
            error(main.getLineNumber(), "main 方法不能声明参数，必须是 void main()");
        }
    }

    private TypedAst.Method analyzeMethod(Ast.Method.MethodSingle source) {
        variableScopes = new ArrayDeque<Map<String, TypedAst.Symbol>>();
        methodLocals = new ArrayList<TypedAst.Declaration>();
        unassigned = new HashSet<TypedAst.Symbol>();
        currentReturnType = typeOf(source.getRetType());
        loopDepth = 0;

        // 形参与方法体最外层的局部变量同处一个作用域：void f(int x) { int x; } 是重复声明。
        enterScope();
        ArrayList<TypedAst.Declaration> formals = new ArrayList<TypedAst.Declaration>();
        for (Ast.Declare.Base formalBase : source.getFormals()) {
            Ast.Declare.DeclareSingle formal = (Ast.Declare.DeclareSingle) formalBase;
            formals.add(declare(formal, TypedAst.Symbol.Kind.PARAMETER));
        }

        ArrayList<TypedAst.Stmt> statements = new ArrayList<TypedAst.Stmt>();
        for (Ast.Stmt.Base statement : source.getStms()) {
            statements.add(analyzeStatement(statement));
        }

        if (!"main".equals(source.getId()) && currentReturnType != TypedAst.Type.VOID
                && !statementsMustReturn(statements)) {
            error(source.getLineNum(), "非 void 方法 '" + source.getId() + "' 不是所有路径都有 return");
        }

        exitScope();
        TypedAst.Method typed = new TypedAst.Method(sourceMethodSymbols.get(source),
                formals, methodLocals, statements, source.getSourceSpan());
        variableScopes = null;
        methodLocals = null;
        unassigned = null;
        currentReturnType = null;
        return typed;
    }

    /**
     * 在当前作用域声明一个形参或局部变量。
     *
     * <p>只要名字在任一尚未关闭的作用域中可见就报重复声明——即禁止遮蔽（同 Java）。
     * 作用域关闭后名字随之消失，所以并列的兄弟块可以复用同一个名字。</p>
     */
    private TypedAst.Declaration declare(Ast.Declare.DeclareSingle source,
                                         TypedAst.Symbol.Kind kind) {
        TypedAst.Symbol symbol = new TypedAst.Symbol(source.getId(), typeOf(source.getType()),
                kind, source.getSourceSpan());
        if (lookup(source.getId()) != null) {
            String noun = kind == TypedAst.Symbol.Kind.PARAMETER ? "参数 " : "变量 ";
            error(source.getLineNum(), "重复的" + noun + source.getId());
        }
        // 即使重复也要写入当前作用域：否则后续对这个名字的每次引用都会级联报"未定义的变量"。
        variableScopes.peek().put(source.getId(), symbol);
        TypedAst.Declaration declaration =
                new TypedAst.Declaration(symbol, source.getSourceSpan());
        if (kind == TypedAst.Symbol.Kind.LOCAL) {
            methodLocals.add(declaration);
        }
        return declaration;
    }

    private TypedAst.Stmt analyzeStatement(Ast.Stmt.Base source) {
        if (source == null) {
            return new TypedAst.Block(
                    Collections.<TypedAst.Stmt>emptyList(), SourceSpan.UNKNOWN);
        }
        if (!enterAnalysis(source, "语句")) {
            TypedAst.Stmt fallback = new TypedAst.Block(
                    Collections.<TypedAst.Stmt>emptyList(), source.getSourceSpan());
            typedStatements.put(source, fallback);
            return fallback;
        }
        try {
            return analyzeStatementCore(source);
        } finally {
            exitAnalysis();
        }
    }

    private TypedAst.Stmt analyzeStatementCore(Ast.Stmt.Base source) {
        TypedAst.Stmt typed;
        if (source instanceof Ast.Stmt.Assign) {
            Ast.Stmt.Assign node = (Ast.Stmt.Assign) source;
            TypedAst.Expr expression = analyzeExpression(node.getExpr());
            TypedAst.Symbol target = resolveVisible(
                    node.getId().getId(), node.getId().getSourceSpan(), "变量");
            typedExpressions.put(node.getId(),
                    new TypedAst.Id(target, target.getType(), node.getId().getSourceSpan()));
            boolean valid = !target.getType().isError() && !expression.getType().isError();
            if (valid && (target.getType().isArray() || expression.getType().isArray())) {
                error(node.getLineNum(), String.format("数组不支持整体赋值：不能将 %s 赋值给 %s",
                        expression.getType(), target.getType()));
                valid = false;
            }
            if (valid && !isAssignable(target.getType(), expression.getType())) {
                error(node.getLineNum(), String.format("不能将 %s 类型的表达式赋值给 %s 类型的变量 '%s'",
                        expression.getType(), target.getType(), target.getName()));
                valid = false;
            }
            if (valid) {
                unassigned.remove(target);
            }
            typed = new TypedAst.Assign(target, expression, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.VarDecl) {
            Ast.Stmt.VarDecl node = (Ast.Stmt.VarDecl) source;
            TypedAst.Declaration declaration =
                    declare(node.getDeclaration(), TypedAst.Symbol.Kind.LOCAL);
            if (!declaration.getType().isArray()) {
                unassigned.add(declaration.getSymbol());
            }
            TypedAst.Expr initializer = node.getInitializer() == null
                    ? null : analyzeExpression(node.getInitializer());
            if (initializer != null && !initializer.getType().isError()) {
                if (!isAssignable(declaration.getType(), initializer.getType())) {
                    error(node.getLineNum(), String.format(
                            "不能用 %s 类型初始化 %s 类型的变量 '%s'",
                            initializer.getType(), declaration.getType(), declaration.getName()));
                } else {
                    unassigned.remove(declaration.getSymbol());
                }
            }
            typed = new TypedAst.VarDecl(declaration, initializer, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.Block) {
            Ast.Stmt.Block node = (Ast.Stmt.Block) source;
            enterScope();
            ArrayList<TypedAst.Stmt> statements = new ArrayList<TypedAst.Stmt>();
            for (Ast.Stmt.Base statement : node.getStmts()) {
                statements.add(analyzeStatement(statement));
            }
            exitScope();
            typed = new TypedAst.Block(statements, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.If) {
            Ast.Stmt.If node = (Ast.Stmt.If) source;
            TypedAst.Expr condition = analyzeExpression(node.getCondition());
            requireBooleanCondition(condition, "if");
            Set<TypedAst.Symbol> before = copyUnassigned();
            unassigned = new HashSet<TypedAst.Symbol>(before);
            TypedAst.Stmt thenStatement = analyzeStatement(node.getThenStmt());
            Set<TypedAst.Symbol> thenUnassigned = copyUnassigned();
            TypedAst.Stmt elseStatement = null;
            if (node.getElseStmt() != null) {
                unassigned = new HashSet<TypedAst.Symbol>(before);
                elseStatement = analyzeStatement(node.getElseStmt());
                thenUnassigned.addAll(unassigned);
                unassigned = thenUnassigned;
            } else {
                unassigned = before;
            }
            typed = new TypedAst.If(
                    condition, thenStatement, elseStatement, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.While) {
            Ast.Stmt.While node = (Ast.Stmt.While) source;
            TypedAst.Expr condition = analyzeExpression(node.getCondition());
            requireBooleanCondition(condition, "while");
            Set<TypedAst.Symbol> before = copyUnassigned();
            loopDepth++;
            unassigned = new HashSet<TypedAst.Symbol>(before);
            TypedAst.Stmt body = analyzeStatement(node.getBody());
            loopDepth--;
            unassigned = before;
            typed = new TypedAst.While(condition, body, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.For) {
            Ast.Stmt.For node = (Ast.Stmt.For) source;
            TypedAst.Stmt initializer = node.getInit() == null ? null : analyzeStatement(node.getInit());
            TypedAst.Expr condition = analyzeExpression(node.getCondition());
            requireBooleanCondition(condition, "for");
            Set<TypedAst.Symbol> before = copyUnassigned();
            loopDepth++;
            unassigned = new HashSet<TypedAst.Symbol>(before);
            TypedAst.Stmt body = analyzeStatement(node.getBody());
            TypedAst.Stmt update = node.getUpdate() == null ? null : analyzeStatement(node.getUpdate());
            loopDepth--;
            unassigned = before;
            typed = new TypedAst.For(
                    initializer, condition, update, body, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.Return) {
            Ast.Stmt.Return node = (Ast.Stmt.Return) source;
            TypedAst.Expr expression = node.getExpr() == null ? null : analyzeExpression(node.getExpr());
            if (expression == null && currentReturnType != TypedAst.Type.VOID) {
                error(node.getLineNum(), "非 void 方法必须返回一个值");
            } else if (expression != null && currentReturnType == TypedAst.Type.VOID) {
                error(node.getLineNum(), "void 方法不能返回值");
            } else if (expression != null && !expression.getType().isError()
                    && !isAssignable(currentReturnType, expression.getType())) {
                error(node.getLineNum(), String.format("返回值类型不匹配：期望 %s，实际 %s",
                        currentReturnType, expression.getType()));
            }
            typed = new TypedAst.Return(expression, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.Printf) {
            typed = analyzePrintf((Ast.Stmt.Printf) source);
        } else if (source instanceof Ast.Stmt.PrintLine) {
            typed = new TypedAst.PrintLine(source.getSourceSpan());
        } else if (source instanceof Ast.Stmt.Break) {
            if (loopDepth == 0) {
                error(source.getLineNum(), "break语句必须包含在循环体中。");
            }
            typed = new TypedAst.Break(source.getSourceSpan());
        } else if (source instanceof Ast.Stmt.Continue) {
            if (loopDepth == 0) {
                error(source.getLineNum(), "continue语句必须包含在循环体中。");
            }
            typed = new TypedAst.Continue(source.getSourceSpan());
        } else if (source instanceof Ast.Stmt.Call) {
            Ast.Stmt.Call node = (Ast.Stmt.Call) source;
            CallResolution call = analyzeCall(
                    node.getName(), node.getInputParams(), node.getSourceSpan(), false);
            typed = new TypedAst.CallStmt(
                    call.method, call.arguments, node.getSourceSpan());
        } else if (source instanceof Ast.Stmt.ArrayAssign) {
            typed = analyzeArrayAssign((Ast.Stmt.ArrayAssign) source);
        } else {
            error(source.getLineNum(), "内部错误: 不支持的语句 AST 节点 " + source.getClass().getSimpleName());
            typed = new TypedAst.Block(
                    Collections.<TypedAst.Stmt>emptyList(), source.getSourceSpan());
        }
        typedStatements.put(source, typed);
        return typed;
    }

    private TypedAst.Stmt analyzePrintf(Ast.Stmt.Printf source) {
        ArrayList<Character> placeholders = printfPlaceholders(source.getFormat(), source.getLineNum());
        ArrayList<TypedAst.Expr> expressions = new ArrayList<TypedAst.Expr>();
        for (Ast.Expr.Base expression : source.getExprs()) {
            expressions.add(analyzeExpression(expression));
        }
        if (placeholders.size() != expressions.size()) {
            error(source.getLineNum(), String.format(
                    "printf 参数个数不匹配：格式串需要 %d 个，实际 %d 个",
                    placeholders.size(), expressions.size()));
        }
        int checked = Math.min(placeholders.size(), expressions.size());
        for (int i = 0; i < checked; i++) {
            TypedAst.Expr expression = expressions.get(i);
            if (expression.getType().isError()) {
                continue;
            }
            char placeholder = placeholders.get(i);
            if (placeholder == 'd' && expression.getType() != TypedAst.Type.INT) {
                error(expression.getLineNum(), String.format(
                        "printf 占位符 %%d 需要 int，实际为 %s", expression.getType()));
            }
            if (placeholder == 'f' && expression.getType() != TypedAst.Type.FLOAT
                    && expression.getType() != TypedAst.Type.DOUBLE) {
                error(expression.getLineNum(), String.format(
                        "printf 占位符 %%f 需要 float 或 double，实际为 %s", expression.getType()));
            }
        }
        return new TypedAst.Printf(
                source.getFormat(), expressions, source.getSourceSpan());
    }

    private TypedAst.Stmt analyzeArrayAssign(Ast.Stmt.ArrayAssign source) {
        TypedAst.Expr index = analyzeExpression(source.getIndex());
        TypedAst.Expr expression = analyzeExpression(source.getExpr());
        TypedAst.Symbol array = resolveVisible(
                source.getArrayName(), source.getSourceSpan(), "数组");
        TypedAst.Type elementType = array.getType().elementType();
        if (!array.getType().isError() && !array.getType().isArray()) {
            error(source.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    array.getName(), array.getType()));
            elementType = TypedAst.Type.ERROR;
        }
        if (!index.getType().isError() && index.getType() != TypedAst.Type.INT) {
            error(index.getLineNum(), "数组下标必须是 int，实际为 " + index.getType());
        }
        if (!elementType.isError() && !expression.getType().isError()
                && !isAssignable(elementType, expression.getType())) {
            error(source.getLineNum(), String.format("不能将 %s 类型的表达式赋值给 %s 数组元素",
                    expression.getType(), elementType));
        }
        return new TypedAst.ArrayAssign(
                array, index, expression, source.getSourceSpan());
    }

    private TypedAst.Expr analyzeExpression(Ast.Expr.Base source) {
        if (source == null) {
            return new TypedAst.ErrorExpr(SourceSpan.UNKNOWN);
        }
        if (!enterAnalysis(source, "表达式")) {
            TypedAst.Expr fallback = new TypedAst.ErrorExpr(source.getSourceSpan());
            typedExpressions.put(source, fallback);
            return fallback;
        }
        try {
            return analyzeExpressionCore(source);
        } finally {
            exitAnalysis();
        }
    }

    private TypedAst.Expr analyzeExpressionCore(Ast.Expr.Base source) {
        TypedAst.Expr typed;
        if (source instanceof Ast.Expr.IntLiteral) {
            Ast.Expr.IntLiteral node = (Ast.Expr.IntLiteral) source;
            typed = new TypedAst.IntLiteral(
                    node.getValue(), node.getRawValue(), node.getSourceSpan());
        } else if (source instanceof Ast.Expr.FloatLiteral) {
            Ast.Expr.FloatLiteral node = (Ast.Expr.FloatLiteral) source;
            typed = new TypedAst.FloatLiteral(
                    node.getValue(), node.getRawValue(), node.getSourceSpan());
        } else if (source instanceof Ast.Expr.DoubleLiteral) {
            Ast.Expr.DoubleLiteral node = (Ast.Expr.DoubleLiteral) source;
            typed = new TypedAst.DoubleLiteral(
                    node.getValue(), node.getRawValue(), node.getSourceSpan());
        } else if (source instanceof Ast.Expr.True) {
            typed = new TypedAst.BoolLiteral(true, source.getSourceSpan());
        } else if (source instanceof Ast.Expr.False) {
            typed = new TypedAst.BoolLiteral(false, source.getSourceSpan());
        } else if (source instanceof Ast.Expr.Str) {
            typed = new TypedAst.StringLiteral(
                    ((Ast.Expr.Str) source).getValue(), source.getSourceSpan());
        } else if (source instanceof Ast.Expr.Id) {
            Ast.Expr.Id node = (Ast.Expr.Id) source;
            TypedAst.Symbol symbol = resolveVisible(
                    node.getId(), node.getSourceSpan(), "变量");
            TypedAst.Type type = symbol.getType();
            if (!type.isError() && unassigned.contains(symbol)) {
                error(node.getLineNum(), String.format("变量 '%s' 在使用前未赋值", node.getId()));
                type = TypedAst.Type.ERROR;
            }
            typed = new TypedAst.Id(symbol, type, node.getSourceSpan());
        } else if (source instanceof Ast.Expr.Call) {
            Ast.Expr.Call node = (Ast.Expr.Call) source;
            CallResolution call = analyzeCall(
                    node.getName(), node.getInputParams(), node.getSourceSpan(), true);
            typed = new TypedAst.Call(
                    call.method, call.arguments, call.type, node.getSourceSpan());
        } else if (source instanceof Ast.Expr.ArrayAccess) {
            typed = analyzeArrayAccess((Ast.Expr.ArrayAccess) source);
        } else if (source instanceof Ast.Expr.ArrayLength) {
            Ast.Expr.ArrayLength node = (Ast.Expr.ArrayLength) source;
            TypedAst.Symbol array = resolveVisible(
                    node.getArrayName(), node.getSourceSpan(), "数组");
            TypedAst.Type type = TypedAst.Type.INT;
            if (!array.getType().isError() && !array.getType().isArray()) {
                error(node.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                        array.getName(), array.getType()));
                type = TypedAst.Type.ERROR;
            } else if (array.getType().isError()) {
                type = TypedAst.Type.ERROR;
            }
            typed = new TypedAst.ArrayLength(array, type, node.getSourceSpan());
        } else if (source instanceof Ast.Expr.UnaryMinus) {
            Ast.Expr.UnaryMinus node = (Ast.Expr.UnaryMinus) source;
            TypedAst.Expr expression = analyzeExpression(node.getExpr());
            TypedAst.Type type = expression.getType();
            if (!type.isError() && !type.isNumeric()) {
                error(node.getLineNum(), "一元负号不能用于类型 " + type);
                type = TypedAst.Type.ERROR;
            }
            typed = new TypedAst.UnaryMinus(type, expression, node.getSourceSpan());
        } else if (source instanceof Ast.Expr.Not) {
            Ast.Expr.Not node = (Ast.Expr.Not) source;
            TypedAst.Expr expression = analyzeExpression(node.getExpr());
            TypedAst.Type type = expression.getType();
            if (!type.isError() && type != TypedAst.Type.BOOL) {
                error(node.getLineNum(), "! 运算符要求操作数是 bool，实际为 " + type);
                type = TypedAst.Type.ERROR;
            }
            typed = type.isError() ? new TypedAst.ErrorExpr(node.getSourceSpan())
                    : new TypedAst.Not(expression, node.getSourceSpan());
        } else if (source instanceof Ast.Expr.And || source instanceof Ast.Expr.Or) {
            typed = analyzeBooleanBinary((Ast.Expr.BinaryExpr) source);
        } else if (source instanceof Ast.Expr.Mod) {
            Ast.Expr.BinaryExpr node = (Ast.Expr.BinaryExpr) source;
            TypedAst.Expr left = analyzeExpression(node.getLeft());
            TypedAst.Expr right = analyzeExpression(node.getRight());
            if (hasError(left, right)) {
                typed = new TypedAst.ErrorExpr(node.getSourceSpan());
            } else if (left.getType() != TypedAst.Type.INT || right.getType() != TypedAst.Type.INT) {
                error(node.getLineNum(), String.format(
                        "运算符 '%%' 只支持 int 操作数：左侧为 %s，右侧为 %s",
                        left.getType(), right.getType()));
                typed = new TypedAst.ErrorExpr(node.getSourceSpan());
            } else {
                typed = new TypedAst.Mod(left, right, node.getSourceSpan());
            }
        } else if (isArithmetic(source)) {
            typed = analyzeArithmetic((Ast.Expr.BinaryExpr) source);
        } else if (isComparison(source)) {
            typed = analyzeComparison((Ast.Expr.BinaryExpr) source);
        } else {
            error(source.getLineNum(), "内部错误: 不支持的表达式 AST 节点 "
                    + source.getClass().getSimpleName());
            typed = new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        typedExpressions.put(source, typed);
        return typed;
    }

    private boolean enterAnalysis(Ast.Node source, String construct) {
        if (analysisDepth >= MAX_ANALYSIS_DEPTH) {
            error(source.getLineNum(), construct + "嵌套过深，最大允许 "
                    + MAX_ANALYSIS_DEPTH + " 层");
            return false;
        }
        analysisDepth++;
        return true;
    }

    private void exitAnalysis() {
        analysisDepth--;
    }

    private TypedAst.Expr analyzeArithmetic(Ast.Expr.BinaryExpr source) {
        TypedAst.Expr left = analyzeExpression(source.getLeft());
        TypedAst.Expr right = analyzeExpression(source.getRight());
        if (hasError(left, right)) {
            return new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        TypedAst.Type type = promoteNumeric(left.getType(), right.getType());
        if (type == null) {
            error(source.getLineNum(), String.format(
                    "算术运算要求数值操作数：左侧为 %s，右侧为 %s",
                    left.getType(), right.getType()));
            return new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.Add) {
            return new TypedAst.Add(type, left, right, source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.Sub) {
            return new TypedAst.Sub(type, left, right, source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.Mul) {
            return new TypedAst.Mul(type, left, right, source.getSourceSpan());
        }
        return new TypedAst.Div(type, left, right, source.getSourceSpan());
    }

    private TypedAst.Expr analyzeBooleanBinary(Ast.Expr.BinaryExpr source) {
        TypedAst.Expr left = analyzeExpression(source.getLeft());
        TypedAst.Expr right = analyzeExpression(source.getRight());
        if (hasError(left, right)) {
            return new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        if (left.getType() != TypedAst.Type.BOOL || right.getType() != TypedAst.Type.BOOL) {
            String operator = source instanceof Ast.Expr.And ? "&&" : "||";
            error(source.getLineNum(), String.format(
                    "运算符 '%s' 要求左右操作数都是 bool：左侧为 %s，右侧为 %s",
                    operator, left.getType(), right.getType()));
            return new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        return source instanceof Ast.Expr.And
                ? new TypedAst.And(left, right, source.getSourceSpan())
                : new TypedAst.Or(left, right, source.getSourceSpan());
    }

    private TypedAst.Expr analyzeComparison(Ast.Expr.BinaryExpr source) {
        TypedAst.Expr left = analyzeExpression(source.getLeft());
        TypedAst.Expr right = analyzeExpression(source.getRight());
        if (hasError(left, right)) {
            return new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        boolean equality = source instanceof Ast.Expr.EQ || source instanceof Ast.Expr.NEQ;
        boolean valid = promoteNumeric(left.getType(), right.getType()) != null
                || (equality && left.getType() == TypedAst.Type.BOOL
                && right.getType() == TypedAst.Type.BOOL);
        if (!valid) {
            String op = comparisonOperator(source);
            error(source.getLineNum(), String.format(
                    "比较运算符 '%s' %s：左侧为 %s，右侧为 %s", op,
                    equality ? "要求两侧均为数值或均为 bool" : "只支持数值操作数",
                    left.getType(), right.getType()));
            return new TypedAst.ErrorExpr(source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.EQ) {
            return new TypedAst.EQ(left, right, source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.NEQ) {
            return new TypedAst.NEQ(left, right, source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.GT) {
            return new TypedAst.GT(left, right, source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.LT) {
            return new TypedAst.LT(left, right, source.getSourceSpan());
        }
        if (source instanceof Ast.Expr.GTE) {
            return new TypedAst.GTE(left, right, source.getSourceSpan());
        }
        return new TypedAst.LTE(left, right, source.getSourceSpan());
    }

    private TypedAst.Expr analyzeArrayAccess(Ast.Expr.ArrayAccess source) {
        TypedAst.Expr index = analyzeExpression(source.getIndex());
        TypedAst.Symbol array = resolveVisible(
                source.getArrayName(), source.getSourceSpan(), "数组");
        TypedAst.Type type = array.getType().elementType();
        if (!array.getType().isError() && !array.getType().isArray()) {
            error(source.getLineNum(), String.format("变量 '%s' 不是数组，实际类型为 %s",
                    array.getName(), array.getType()));
            type = TypedAst.Type.ERROR;
        }
        if (!index.getType().isError() && index.getType() != TypedAst.Type.INT) {
            error(index.getLineNum(), "数组下标必须是 int，实际为 " + index.getType());
            type = TypedAst.Type.ERROR;
        }
        if (index.getType().isError() || array.getType().isError()) {
            type = TypedAst.Type.ERROR;
        }
        return new TypedAst.ArrayAccess(array, index, type, source.getSourceSpan());
    }

    private CallResolution analyzeCall(String name, List<Ast.Expr.Base> sourceArguments,
                                       SourceSpan sourceSpan, boolean expressionContext) {
        int lineNumber = sourceSpan.getStartLine();
        ArrayList<TypedAst.Expr> arguments = new ArrayList<TypedAst.Expr>();
        for (Ast.Expr.Base sourceArgument : sourceArguments) {
            arguments.add(analyzeExpression(sourceArgument));
        }
        TypedAst.MethodSymbol method = methods.get(name);
        if (method == null) {
            error(lineNumber, "未定义的方法: " + name);
            method = new TypedAst.MethodSymbol(name, TypedAst.Type.ERROR,
                    Collections.<TypedAst.Type>emptyList(), sourceSpan);
            return new CallResolution(method, arguments, TypedAst.Type.ERROR);
        }
        boolean valid = true;
        if (arguments.size() != method.getParameterTypes().size()) {
            valid = false;
            error(lineNumber, String.format("方法 '%s' 的参数个数不正确：期望 %d 个，实际 %d 个",
                    name, method.getParameterTypes().size(), arguments.size()));
        }
        int checked = Math.min(arguments.size(), method.getParameterTypes().size());
        for (int i = 0; i < checked; i++) {
            TypedAst.Type actual = arguments.get(i).getType();
            TypedAst.Type expected = method.getParameterTypes().get(i);
            if (actual.isError()) {
                valid = false;
            } else if (!isAssignable(expected, actual)) {
                valid = false;
                error(lineNumber, String.format(
                        "方法 '%s' 的第 %d 个参数类型不匹配：期望 %s，实际 %s",
                        name, i + 1, expected, actual));
            }
        }
        if (expressionContext && method.getReturnType() == TypedAst.Type.VOID) {
            valid = false;
            error(lineNumber, "void 方法 '" + name + "' 不能作为表达式使用");
        }
        return new CallResolution(method, arguments,
                valid ? method.getReturnType() : TypedAst.Type.ERROR);
    }

    private void enterScope() {
        variableScopes.push(new HashMap<String, TypedAst.Symbol>());
    }

    private void exitScope() {
        Map<String, TypedAst.Symbol> declarations = variableScopes.pop();
        unassigned.removeAll(declarations.values());
    }

    /** 由内向外查找名字；未找到返回 null。 */
    private TypedAst.Symbol lookup(String name) {
        if (variableScopes == null) {
            return null;
        }
        for (Map<String, TypedAst.Symbol> scope : variableScopes) {
            TypedAst.Symbol symbol = scope.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    private TypedAst.Symbol resolveVisible(String name, SourceSpan sourceSpan, String noun) {
        TypedAst.Symbol symbol = lookup(name);
        if (symbol != null) {
            return symbol;
        }
        error(sourceSpan.getStartLine(), "未定义的" + noun + ": " + name);
        return new TypedAst.Symbol(name, TypedAst.Type.ERROR,
                TypedAst.Symbol.Kind.LOCAL, sourceSpan);
    }

    private Set<TypedAst.Symbol> copyUnassigned() {
        return new HashSet<TypedAst.Symbol>(unassigned);
    }

    private void requireBooleanCondition(TypedAst.Expr condition, String statement) {
        if (!condition.getType().isError() && condition.getType() != TypedAst.Type.BOOL) {
            error(condition.getLineNum(), statement + " 条件必须是 bool，实际为 " + condition.getType());
        }
    }

    private TypedAst.Type typeOf(Ast.Type.Base source) {
        if (source == null) {
            return TypedAst.Type.ERROR;
        }
        switch (source.getKind()) {
            case INT:
                return TypedAst.Type.INT;
            case FLOAT:
                return TypedAst.Type.FLOAT;
            case DOUBLE:
                return TypedAst.Type.DOUBLE;
            case BOOL:
                return TypedAst.Type.BOOL;
            case STRING:
                return TypedAst.Type.STRING;
            case VOID:
                return TypedAst.Type.VOID;
            case INT_ARRAY:
                return TypedAst.Type.array(TypedAst.Type.Kind.INT_ARRAY,
                        ((Ast.Type.IntArray) source).getSize());
            case FLOAT_ARRAY:
                return TypedAst.Type.array(TypedAst.Type.Kind.FLOAT_ARRAY,
                        ((Ast.Type.FloatArray) source).getSize());
            case DOUBLE_ARRAY:
                return TypedAst.Type.array(TypedAst.Type.Kind.DOUBLE_ARRAY,
                        ((Ast.Type.DoubleArray) source).getSize());
            case BOOL_ARRAY:
                return TypedAst.Type.array(TypedAst.Type.Kind.BOOL_ARRAY,
                        ((Ast.Type.BoolArray) source).getSize());
            default:
                return TypedAst.Type.ERROR;
        }
    }

    private boolean isAssignable(TypedAst.Type target, TypedAst.Type source) {
        if (target.isError() || source.isError()) {
            return true;
        }
        if (target.getKind() == source.getKind()) {
            return true;
        }
        return (target == TypedAst.Type.FLOAT && source == TypedAst.Type.INT)
                || (target == TypedAst.Type.DOUBLE
                && (source == TypedAst.Type.INT || source == TypedAst.Type.FLOAT));
    }

    private TypedAst.Type promoteNumeric(TypedAst.Type left, TypedAst.Type right) {
        if (!left.isNumeric() || !right.isNumeric()) {
            return null;
        }
        if (left == TypedAst.Type.DOUBLE || right == TypedAst.Type.DOUBLE) {
            return TypedAst.Type.DOUBLE;
        }
        if (left == TypedAst.Type.FLOAT || right == TypedAst.Type.FLOAT) {
            return TypedAst.Type.FLOAT;
        }
        return TypedAst.Type.INT;
    }

    private boolean hasError(TypedAst.Expr left, TypedAst.Expr right) {
        return left.getType().isError() || right.getType().isError();
    }

    private boolean isArithmetic(Ast.Expr.Base source) {
        return source instanceof Ast.Expr.Add || source instanceof Ast.Expr.Sub
                || source instanceof Ast.Expr.Mul || source instanceof Ast.Expr.Div;
    }

    private boolean isComparison(Ast.Expr.Base source) {
        return source instanceof Ast.Expr.EQ || source instanceof Ast.Expr.NEQ
                || source instanceof Ast.Expr.GT || source instanceof Ast.Expr.LT
                || source instanceof Ast.Expr.GTE || source instanceof Ast.Expr.LTE;
    }

    private String comparisonOperator(Ast.Expr.BinaryExpr source) {
        if (source instanceof Ast.Expr.EQ) return "==";
        if (source instanceof Ast.Expr.NEQ) return "!=";
        if (source instanceof Ast.Expr.GT) return ">";
        if (source instanceof Ast.Expr.LT) return "<";
        if (source instanceof Ast.Expr.GTE) return ">=";
        return "<=";
    }

    private boolean statementsMustReturn(List<TypedAst.Stmt> statements) {
        return flowOfStatements(statements).mustReturn;
    }

    private FlowResult flowOfStatements(List<TypedAst.Stmt> statements) {
        boolean canCompleteNormally = true;
        for (TypedAst.Stmt statement : statements) {
            if (!canCompleteNormally) {
                break;
            }
            FlowResult flow = flowOfStatement(statement);
            canCompleteNormally = flow.canCompleteNormally;
            if (flow.mustReturn) {
                return new FlowResult(false, true);
            }
        }
        return new FlowResult(canCompleteNormally, false);
    }

    private FlowResult flowOfStatement(TypedAst.Stmt statement) {
        if (statement instanceof TypedAst.Return) {
            return new FlowResult(false, true);
        }
        if (statement instanceof TypedAst.Block) {
            return flowOfStatements(((TypedAst.Block) statement).getStatements());
        }
        if (statement instanceof TypedAst.If) {
            TypedAst.If ifStatement = (TypedAst.If) statement;
            if (ifStatement.getElseStatement() == null) {
                return new FlowResult(true, false);
            }
            FlowResult thenFlow = flowOfStatement(ifStatement.getThenStatement());
            FlowResult elseFlow = flowOfStatement(ifStatement.getElseStatement());
            return new FlowResult(thenFlow.canCompleteNormally || elseFlow.canCompleteNormally,
                    thenFlow.mustReturn && elseFlow.mustReturn);
        }
        return new FlowResult(true, false);
    }

    private ArrayList<Character> printfPlaceholders(String format, int lineNumber) {
        ArrayList<Character> placeholders = new ArrayList<Character>();
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) != '%') {
                continue;
            }
            if (i + 1 >= format.length()) {
                error(lineNumber, "printf 格式串中的 % 缺少占位符");
                break;
            }
            char placeholder = format.charAt(++i);
            if (placeholder == 'd' || placeholder == 'f') {
                placeholders.add(placeholder);
            } else {
                error(lineNumber, "printf 不支持占位符 %" + placeholder);
            }
        }
        return placeholders;
    }

    private void error(int lineNumber, String message) {
        String diagnostic = "[语义分析] 行 " + lineNumber + ": " + message;
        if (!collectErrors) {
            throw new SemanticException(diagnostic);
        }
        errors.add(diagnostic);
        errorLineNumbers.add(lineNumber);
    }

    private static final class CallResolution {
        final TypedAst.MethodSymbol method;
        final List<TypedAst.Expr> arguments;
        final TypedAst.Type type;

        CallResolution(TypedAst.MethodSymbol method, List<TypedAst.Expr> arguments, TypedAst.Type type) {
            this.method = method;
            this.arguments = arguments;
            this.type = type;
        }
    }

    private static final class FlowResult {
        final boolean canCompleteNormally;
        final boolean mustReturn;

        FlowResult(boolean canCompleteNormally, boolean mustReturn) {
            this.canCompleteNormally = canCompleteNormally;
            this.mustReturn = mustReturn;
        }
    }
}
