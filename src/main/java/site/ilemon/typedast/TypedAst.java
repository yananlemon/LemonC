package site.ilemon.typedast;

import site.ilemon.source.SourceSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, name-resolved and type-checked AST consumed by the middle end.
 * Every node and resolved symbol retains an end-exclusive source span.
 *
 * <p>The parser AST deliberately does not implement or extend these nodes. A
 * {@code TypedAst.Program} can only be produced by semantic analysis, which
 * makes the semantic boundary explicit in the Java type system.</p>
 */
public final class TypedAst {
    private TypedAst() {
    }

    private static <T> List<T> immutableCopy(List<? extends T> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<T> copy = new ArrayList<T>(values.size());
        for (T value : values) {
            copy.add(Objects.requireNonNull(value, "list element"));
        }
        return Collections.unmodifiableList(copy);
    }

    private static Type compatibleType(Type actual, Type expected, String owner) {
        Objects.requireNonNull(actual, "type");
        Objects.requireNonNull(expected, "expectedType");
        if (actual != Type.ERROR && !actual.equals(expected)) {
            throw new IllegalArgumentException(owner + " type " + actual
                    + " does not match resolved type " + expected);
        }
        return actual;
    }

    public static final class Type {
        public enum Kind {
            INT, FLOAT, DOUBLE, BOOL, STRING, VOID,
            INT_ARRAY, FLOAT_ARRAY, DOUBLE_ARRAY, BOOL_ARRAY,
            ERROR
        }

        public static final Type INT = new Type(Kind.INT, -1);
        public static final Type FLOAT = new Type(Kind.FLOAT, -1);
        public static final Type DOUBLE = new Type(Kind.DOUBLE, -1);
        public static final Type BOOL = new Type(Kind.BOOL, -1);
        public static final Type STRING = new Type(Kind.STRING, -1);
        public static final Type VOID = new Type(Kind.VOID, -1);
        public static final Type ERROR = new Type(Kind.ERROR, -1);

        private final Kind kind;
        private final int arraySize;

        private Type(Kind kind, int arraySize) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.arraySize = arraySize;
        }

        public static Type array(Kind kind, int size) {
            if (kind != Kind.INT_ARRAY && kind != Kind.FLOAT_ARRAY
                    && kind != Kind.DOUBLE_ARRAY && kind != Kind.BOOL_ARRAY) {
                throw new IllegalArgumentException("not an array type: " + kind);
            }
            if (size < -1) {
                throw new IllegalArgumentException("array size must be -1 or non-negative: " + size);
            }
            return new Type(kind, size);
        }

        public Kind getKind() {
            return kind;
        }

        public int getArraySize() {
            return arraySize;
        }

        public boolean hasKnownArraySize() {
            return isArray() && arraySize >= 0;
        }

        public boolean isError() {
            return kind == Kind.ERROR;
        }

        public boolean isNumeric() {
            return kind == Kind.INT || kind == Kind.FLOAT || kind == Kind.DOUBLE;
        }

        public boolean isArray() {
            return kind == Kind.INT_ARRAY || kind == Kind.FLOAT_ARRAY
                    || kind == Kind.DOUBLE_ARRAY || kind == Kind.BOOL_ARRAY;
        }

        public Type elementType() {
            switch (kind) {
                case INT_ARRAY:
                    return INT;
                case FLOAT_ARRAY:
                    return FLOAT;
                case DOUBLE_ARRAY:
                    return DOUBLE;
                case BOOL_ARRAY:
                    return BOOL;
                default:
                    return ERROR;
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Type)) {
                return false;
            }
            Type that = (Type) other;
            return kind == that.kind && arraySize == that.arraySize;
        }

        @Override
        public int hashCode() {
            return 31 * kind.hashCode() + arraySize;
        }

        @Override
        public String toString() {
            switch (kind) {
                case INT:
                    return "int";
                case FLOAT:
                    return "float";
                case DOUBLE:
                    return "double";
                case BOOL:
                    return "bool";
                case STRING:
                    return "string";
                case VOID:
                    return "void";
                case INT_ARRAY:
                    return "int[]";
                case FLOAT_ARRAY:
                    return "float[]";
                case DOUBLE_ARRAY:
                    return "double[]";
                case BOOL_ARRAY:
                    return "bool[]";
                default:
                    return "<error>";
            }
        }
    }

    public static final class Symbol {
        public enum Kind {
            PARAMETER, LOCAL
        }

        private final String name;
        private final Type type;
        private final Kind kind;
        private final SourceSpan sourceSpan;

        public Symbol(String name, Type type, Kind kind, int lineNumber) {
            this(name, type, kind, SourceSpan.line(lineNumber));
        }

        public Symbol(String name, Type type, Kind kind, SourceSpan sourceSpan) {
            this.name = Objects.requireNonNull(name, "name");
            this.type = Objects.requireNonNull(type, "type");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.sourceSpan = Objects.requireNonNull(sourceSpan, "sourceSpan");
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Kind getKind() {
            return kind;
        }

        public int getLineNumber() {
            return sourceSpan.getStartLine();
        }

        public SourceSpan getSourceSpan() {
            return sourceSpan;
        }
    }

    public static final class MethodSymbol {
        private final String name;
        private final Type returnType;
        private final List<Type> parameterTypes;
        private final SourceSpan sourceSpan;

        public MethodSymbol(String name, Type returnType, List<Type> parameterTypes, int lineNumber) {
            this(name, returnType, parameterTypes, SourceSpan.line(lineNumber));
        }

        public MethodSymbol(String name, Type returnType, List<Type> parameterTypes,
                            SourceSpan sourceSpan) {
            this.name = Objects.requireNonNull(name, "name");
            this.returnType = Objects.requireNonNull(returnType, "returnType");
            this.parameterTypes = immutableCopy(parameterTypes);
            this.sourceSpan = Objects.requireNonNull(sourceSpan, "sourceSpan");
        }

        public String getName() {
            return name;
        }

        public Type getReturnType() {
            return returnType;
        }

        public List<Type> getParameterTypes() {
            return parameterTypes;
        }

        public int getLineNumber() {
            return sourceSpan.getStartLine();
        }

        public SourceSpan getSourceSpan() {
            return sourceSpan;
        }
    }

    public abstract static class Node {
        private final SourceSpan sourceSpan;

        protected Node(int lineNumber) {
            this(SourceSpan.line(lineNumber));
        }

        protected Node(SourceSpan sourceSpan) {
            this.sourceSpan = Objects.requireNonNull(sourceSpan, "sourceSpan");
        }

        public final int getLineNumber() {
            return sourceSpan.getStartLine();
        }

        public final int getLineNum() {
            return sourceSpan.getStartLine();
        }

        public final SourceSpan getSourceSpan() {
            return sourceSpan;
        }
    }

    public static final class Program extends Node {
        private final String className;
        private final List<Method> methods;

        public Program(String className, List<Method> methods) {
            this(className, methods, SourceSpan.UNKNOWN);
        }

        public Program(String className, List<Method> methods, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.className = Objects.requireNonNull(className, "className");
            this.methods = immutableCopy(methods);
        }

        public String getClassName() {
            return className;
        }

        public List<Method> getMethods() {
            return methods;
        }
    }

    public static final class Method extends Node {
        private final MethodSymbol symbol;
        private final List<Declaration> formals;
        private final List<Declaration> locals;
        private final List<Stmt> statements;

        public Method(MethodSymbol symbol, List<Declaration> formals,
                      List<Declaration> locals, List<Stmt> statements, int lineNumber) {
            this(symbol, formals, locals, statements, SourceSpan.line(lineNumber));
        }

        public Method(MethodSymbol symbol, List<Declaration> formals,
                      List<Declaration> locals, List<Stmt> statements, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.symbol = Objects.requireNonNull(symbol, "symbol");
            this.formals = immutableCopy(formals);
            this.locals = immutableCopy(locals);
            this.statements = immutableCopy(statements);
        }

        public MethodSymbol getSymbol() {
            return symbol;
        }

        public String getName() {
            return symbol.getName();
        }

        public Type getReturnType() {
            return symbol.getReturnType();
        }

        public List<Declaration> getFormals() {
            return formals;
        }

        public List<Declaration> getLocals() {
            return locals;
        }

        public List<Stmt> getStatements() {
            return statements;
        }
    }

    public static final class Declaration extends Node {
        private final Symbol symbol;

        public Declaration(Symbol symbol, int lineNumber) {
            this(symbol, SourceSpan.line(lineNumber));
        }

        public Declaration(Symbol symbol, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.symbol = Objects.requireNonNull(symbol, "symbol");
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public String getName() {
            return symbol.getName();
        }

        public Type getType() {
            return symbol.getType();
        }
    }

    public abstract static class Stmt extends Node {
        protected Stmt(int lineNumber) {
            super(lineNumber);
        }

        protected Stmt(SourceSpan sourceSpan) {
            super(sourceSpan);
        }
    }

    public static final class Assign extends Stmt {
        private final Symbol target;
        private final Expr expression;

        public Assign(Symbol target, Expr expression, int lineNumber) {
            this(target, expression, SourceSpan.line(lineNumber));
        }

        public Assign(Symbol target, Expr expression, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.target = Objects.requireNonNull(target, "target");
            this.expression = Objects.requireNonNull(expression, "expression");
        }

        public Symbol getTarget() {
            return target;
        }

        public Expr getExpression() {
            return expression;
        }
    }

    public static final class VarDecl extends Stmt {
        private final Declaration declaration;
        private final Expr initializer;

        public VarDecl(Declaration declaration, Expr initializer, int lineNumber) {
            this(declaration, initializer, SourceSpan.line(lineNumber));
        }

        public VarDecl(Declaration declaration, Expr initializer, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.declaration = Objects.requireNonNull(declaration, "declaration");
            this.initializer = initializer;
        }

        public Declaration getDeclaration() {
            return declaration;
        }

        public Expr getInitializer() {
            return initializer;
        }
    }

    public static final class Block extends Stmt {
        private final List<Stmt> statements;

        public Block(List<Stmt> statements, int lineNumber) {
            this(statements, SourceSpan.line(lineNumber));
        }

        public Block(List<Stmt> statements, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.statements = immutableCopy(statements);
        }

        public List<Stmt> getStatements() {
            return statements;
        }
    }

    public static final class CallStmt extends Stmt {
        private final MethodSymbol method;
        private final List<Expr> arguments;

        public CallStmt(MethodSymbol method, List<Expr> arguments, int lineNumber) {
            this(method, arguments, SourceSpan.line(lineNumber));
        }

        public CallStmt(MethodSymbol method, List<Expr> arguments, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.method = Objects.requireNonNull(method, "method");
            this.arguments = immutableCopy(arguments);
        }

        public MethodSymbol getMethod() {
            return method;
        }

        public List<Expr> getArguments() {
            return arguments;
        }
    }

    public static final class If extends Stmt {
        private final Expr condition;
        private final Stmt thenStatement;
        private final Stmt elseStatement;

        public If(Expr condition, Stmt thenStatement, Stmt elseStatement, int lineNumber) {
            this(condition, thenStatement, elseStatement, SourceSpan.line(lineNumber));
        }

        public If(Expr condition, Stmt thenStatement, Stmt elseStatement,
                  SourceSpan sourceSpan) {
            super(sourceSpan);
            this.condition = Objects.requireNonNull(condition, "condition");
            this.thenStatement = Objects.requireNonNull(thenStatement, "thenStatement");
            this.elseStatement = elseStatement;
        }

        public Expr getCondition() {
            return condition;
        }

        public Stmt getThenStatement() {
            return thenStatement;
        }

        public Stmt getElseStatement() {
            return elseStatement;
        }
    }

    public static final class Printf extends Stmt {
        private final String format;
        private final List<Expr> expressions;

        public Printf(String format, List<Expr> expressions, int lineNumber) {
            this(format, expressions, SourceSpan.line(lineNumber));
        }

        public Printf(String format, List<Expr> expressions, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.format = Objects.requireNonNull(format, "format");
            this.expressions = immutableCopy(expressions);
        }

        public String getFormat() {
            return format;
        }

        public List<Expr> getExpressions() {
            return expressions;
        }
    }

    public static final class PrintLine extends Stmt {
        public PrintLine(int lineNumber) {
            super(lineNumber);
        }

        public PrintLine(SourceSpan sourceSpan) {
            super(sourceSpan);
        }
    }

    public static final class Return extends Stmt {
        private final Expr expression;

        public Return(Expr expression, int lineNumber) {
            this(expression, SourceSpan.line(lineNumber));
        }

        public Return(Expr expression, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.expression = expression;
        }

        public Expr getExpression() {
            return expression;
        }
    }

    public static final class While extends Stmt {
        private final Expr condition;
        private final Stmt body;

        public While(Expr condition, Stmt body, int lineNumber) {
            this(condition, body, SourceSpan.line(lineNumber));
        }

        public While(Expr condition, Stmt body, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.condition = Objects.requireNonNull(condition, "condition");
            this.body = Objects.requireNonNull(body, "body");
        }

        public Expr getCondition() {
            return condition;
        }

        public Stmt getBody() {
            return body;
        }
    }

    public static final class For extends Stmt {
        private final Stmt initializer;
        private final Expr condition;
        private final Stmt update;
        private final Stmt body;

        public For(Stmt initializer, Expr condition, Stmt update, Stmt body, int lineNumber) {
            this(initializer, condition, update, body, SourceSpan.line(lineNumber));
        }

        public For(Stmt initializer, Expr condition, Stmt update, Stmt body,
                   SourceSpan sourceSpan) {
            super(sourceSpan);
            this.initializer = initializer;
            this.condition = Objects.requireNonNull(condition, "condition");
            this.update = update;
            this.body = Objects.requireNonNull(body, "body");
        }

        public Stmt getInitializer() {
            return initializer;
        }

        public Expr getCondition() {
            return condition;
        }

        public Stmt getUpdate() {
            return update;
        }

        public Stmt getBody() {
            return body;
        }
    }

    public static final class Break extends Stmt {
        public Break(int lineNumber) {
            super(lineNumber);
        }

        public Break(SourceSpan sourceSpan) {
            super(sourceSpan);
        }
    }

    public static final class Continue extends Stmt {
        public Continue(int lineNumber) {
            super(lineNumber);
        }

        public Continue(SourceSpan sourceSpan) {
            super(sourceSpan);
        }
    }

    public static final class ArrayAssign extends Stmt {
        private final Symbol array;
        private final Expr index;
        private final Expr expression;

        public ArrayAssign(Symbol array, Expr index, Expr expression, int lineNumber) {
            this(array, index, expression, SourceSpan.line(lineNumber));
        }

        public ArrayAssign(Symbol array, Expr index, Expr expression, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.array = Objects.requireNonNull(array, "array");
            this.index = Objects.requireNonNull(index, "index");
            this.expression = Objects.requireNonNull(expression, "expression");
        }

        public Symbol getArray() {
            return array;
        }

        public Expr getIndex() {
            return index;
        }

        public Expr getExpression() {
            return expression;
        }
    }

    public abstract static class Expr extends Node {
        private final Type type;

        protected Expr(Type type, int lineNumber) {
            super(lineNumber);
            this.type = Objects.requireNonNull(type, "type");
        }

        protected Expr(Type type, SourceSpan sourceSpan) {
            super(sourceSpan);
            this.type = Objects.requireNonNull(type, "type");
        }

        public final Type getType() {
            return type;
        }
    }

    public abstract static class BinaryExpr extends Expr {
        private final Expr left;
        private final Expr right;

        protected BinaryExpr(Type type, Expr left, Expr right, int lineNumber) {
            this(type, left, right, SourceSpan.line(lineNumber));
        }

        protected BinaryExpr(Type type, Expr left, Expr right, SourceSpan sourceSpan) {
            super(type, sourceSpan);
            this.left = Objects.requireNonNull(left, "left");
            this.right = Objects.requireNonNull(right, "right");
        }

        public Expr getLeft() {
            return left;
        }

        public Expr getRight() {
            return right;
        }
    }

    public static final class Add extends BinaryExpr {
        public Add(Type type, Expr left, Expr right, int lineNumber) { super(type, left, right, lineNumber); }
        public Add(Type type, Expr left, Expr right, SourceSpan sourceSpan) { super(type, left, right, sourceSpan); }
    }
    public static final class Sub extends BinaryExpr {
        public Sub(Type type, Expr left, Expr right, int lineNumber) { super(type, left, right, lineNumber); }
        public Sub(Type type, Expr left, Expr right, SourceSpan sourceSpan) { super(type, left, right, sourceSpan); }
    }
    public static final class Mul extends BinaryExpr {
        public Mul(Type type, Expr left, Expr right, int lineNumber) { super(type, left, right, lineNumber); }
        public Mul(Type type, Expr left, Expr right, SourceSpan sourceSpan) { super(type, left, right, sourceSpan); }
    }
    public static final class Div extends BinaryExpr {
        public Div(Type type, Expr left, Expr right, int lineNumber) { super(type, left, right, lineNumber); }
        public Div(Type type, Expr left, Expr right, SourceSpan sourceSpan) { super(type, left, right, sourceSpan); }
    }
    public static final class Mod extends BinaryExpr {
        public Mod(Expr left, Expr right, int lineNumber) { super(Type.INT, left, right, lineNumber); }
        public Mod(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.INT, left, right, sourceSpan); }
    }
    public static final class And extends BinaryExpr {
        public And(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public And(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class Or extends BinaryExpr {
        public Or(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public Or(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class GT extends BinaryExpr {
        public GT(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public GT(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class LT extends BinaryExpr {
        public LT(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public LT(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class GTE extends BinaryExpr {
        public GTE(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public GTE(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class LTE extends BinaryExpr {
        public LTE(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public LTE(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class EQ extends BinaryExpr {
        public EQ(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public EQ(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }
    public static final class NEQ extends BinaryExpr {
        public NEQ(Expr left, Expr right, int lineNumber) { super(Type.BOOL, left, right, lineNumber); }
        public NEQ(Expr left, Expr right, SourceSpan sourceSpan) { super(Type.BOOL, left, right, sourceSpan); }
    }

    public static final class Not extends Expr {
        private final Expr expression;
        public Not(Expr expression, int lineNumber) {
            this(expression, SourceSpan.line(lineNumber));
        }
        public Not(Expr expression, SourceSpan sourceSpan) {
            super(Type.BOOL, sourceSpan);
            this.expression = Objects.requireNonNull(expression, "expression");
        }
        public Expr getExpression() { return expression; }
    }

    public static final class UnaryMinus extends Expr {
        private final Expr expression;
        public UnaryMinus(Type type, Expr expression, int lineNumber) {
            this(type, expression, SourceSpan.line(lineNumber));
        }
        public UnaryMinus(Type type, Expr expression, SourceSpan sourceSpan) {
            super(type, sourceSpan);
            this.expression = Objects.requireNonNull(expression, "expression");
        }
        public Expr getExpression() { return expression; }
    }

    public static final class Call extends Expr {
        private final MethodSymbol method;
        private final List<Expr> arguments;
        public Call(MethodSymbol method, List<Expr> arguments, Type type, int lineNumber) {
            this(method, arguments, type, SourceSpan.line(lineNumber));
        }
        public Call(MethodSymbol method, List<Expr> arguments, Type type,
                    SourceSpan sourceSpan) {
            super(compatibleType(type, Objects.requireNonNull(method, "method").getReturnType(),
                    "call"), sourceSpan);
            this.method = Objects.requireNonNull(method, "method");
            this.arguments = immutableCopy(arguments);
        }
        public MethodSymbol getMethod() { return method; }
        public List<Expr> getArguments() { return arguments; }
    }

    public static final class IntLiteral extends Expr {
        private final int value;
        private final String rawValue;
        public IntLiteral(int value, String rawValue, int lineNumber) {
            this(value, rawValue, SourceSpan.line(lineNumber));
        }
        public IntLiteral(int value, String rawValue, SourceSpan sourceSpan) {
            super(Type.INT, sourceSpan);
            this.value = value;
            this.rawValue = rawValue == null ? String.valueOf(value) : rawValue;
        }
        public int getValue() { return value; }
        public String getRawValue() { return rawValue; }
    }

    public static final class FloatLiteral extends Expr {
        private final float value;
        private final String rawValue;
        public FloatLiteral(float value, String rawValue, int lineNumber) {
            this(value, rawValue, SourceSpan.line(lineNumber));
        }
        public FloatLiteral(float value, String rawValue, SourceSpan sourceSpan) {
            super(Type.FLOAT, sourceSpan);
            this.value = value;
            this.rawValue = rawValue == null ? String.valueOf(value) : rawValue;
        }
        public float getValue() { return value; }
        public String getRawValue() { return rawValue; }
    }

    public static final class DoubleLiteral extends Expr {
        private final double value;
        private final String rawValue;
        public DoubleLiteral(double value, String rawValue, int lineNumber) {
            this(value, rawValue, SourceSpan.line(lineNumber));
        }
        public DoubleLiteral(double value, String rawValue, SourceSpan sourceSpan) {
            super(Type.DOUBLE, sourceSpan);
            this.value = value;
            this.rawValue = rawValue == null ? String.valueOf(value) : rawValue;
        }
        public double getValue() { return value; }
        public String getRawValue() { return rawValue; }
    }

    public static final class BoolLiteral extends Expr {
        private final boolean value;
        public BoolLiteral(boolean value, int lineNumber) {
            this(value, SourceSpan.line(lineNumber));
        }
        public BoolLiteral(boolean value, SourceSpan sourceSpan) {
            super(Type.BOOL, sourceSpan);
            this.value = value;
        }
        public boolean getValue() { return value; }
    }

    public static final class StringLiteral extends Expr {
        private final String value;
        public StringLiteral(String value, int lineNumber) {
            this(value, SourceSpan.line(lineNumber));
        }
        public StringLiteral(String value, SourceSpan sourceSpan) {
            super(Type.STRING, sourceSpan);
            this.value = Objects.requireNonNull(value, "value");
        }
        public String getValue() { return value; }
    }

    public static final class Id extends Expr {
        private final Symbol symbol;
        public Id(Symbol symbol, Type type, int lineNumber) {
            this(symbol, type, SourceSpan.line(lineNumber));
        }
        public Id(Symbol symbol, Type type, SourceSpan sourceSpan) {
            super(compatibleType(type, Objects.requireNonNull(symbol, "symbol").getType(),
                    "identifier"), sourceSpan);
            this.symbol = Objects.requireNonNull(symbol, "symbol");
        }
        public Symbol getSymbol() { return symbol; }
        public String getName() { return symbol.getName(); }
    }

    public static final class ArrayAccess extends Expr {
        private final Symbol array;
        private final Expr index;
        public ArrayAccess(Symbol array, Expr index, Type type, int lineNumber) {
            this(array, index, type, SourceSpan.line(lineNumber));
        }
        public ArrayAccess(Symbol array, Expr index, Type type, SourceSpan sourceSpan) {
            super(compatibleType(type,
                    Objects.requireNonNull(array, "array").getType().elementType(),
                    "array access"), sourceSpan);
            this.array = Objects.requireNonNull(array, "array");
            this.index = Objects.requireNonNull(index, "index");
        }
        public Symbol getArray() { return array; }
        public Expr getIndex() { return index; }
    }

    public static final class ArrayLength extends Expr {
        private final Symbol array;
        public ArrayLength(Symbol array, Type type, int lineNumber) {
            this(array, type, SourceSpan.line(lineNumber));
        }
        public ArrayLength(Symbol array, Type type, SourceSpan sourceSpan) {
            super(compatibleType(type,
                    Objects.requireNonNull(array, "array").getType().isArray()
                            ? Type.INT : Type.ERROR,
                    "array length"), sourceSpan);
            this.array = Objects.requireNonNull(array, "array");
        }
        public Symbol getArray() { return array; }
    }

    public static final class ErrorExpr extends Expr {
        public ErrorExpr(int lineNumber) {
            super(Type.ERROR, lineNumber);
        }

        public ErrorExpr(SourceSpan sourceSpan) {
            super(Type.ERROR, sourceSpan);
        }
    }
}
