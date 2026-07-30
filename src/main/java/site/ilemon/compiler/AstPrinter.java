package site.ilemon.compiler;

import site.ilemon.typedast.TypedAst;

import java.util.List;

/** Teaching-oriented pretty printer for the semantically resolved Typed-AST. */
public final class AstPrinter {
    private final StringBuilder out = new StringBuilder();

    private AstPrinter() {
    }

    public static String print(TypedAst.Program program) {
        AstPrinter printer = new AstPrinter();
        printer.program(program);
        return printer.out.toString();
    }

    private void program(TypedAst.Program program) {
        line(0, "Program");
        line(1, "Class " + program.getClassName());
        for (TypedAst.Method method : program.getMethods()) {
            method(method, 2);
        }
    }

    private void method(TypedAst.Method method, int depth) {
        line(depth, "Method " + method.getName() + " : " + method.getReturnType());
        declarations("Params", method.getFormals(), depth + 1);
        declarations("Locals", method.getLocals(), depth + 1);
        line(depth + 1, "Body");
        for (TypedAst.Stmt statement : method.getStatements()) {
            statement(statement, depth + 2);
        }
    }

    private void declarations(String title, List<TypedAst.Declaration> declarations, int depth) {
        line(depth, title);
        if (declarations.isEmpty()) {
            line(depth + 1, "(none)");
            return;
        }
        for (TypedAst.Declaration declaration : declarations) {
            line(depth + 1, declaration.getType() + " " + declaration.getName());
        }
    }

    private void statement(TypedAst.Stmt statement, int depth) {
        if (statement instanceof TypedAst.Assign) {
            TypedAst.Assign node = (TypedAst.Assign) statement;
            line(depth, "Assign " + node.getTarget().getName());
            expression(node.getExpression(), depth + 1);
        } else if (statement instanceof TypedAst.VarDecl) {
            TypedAst.VarDecl node = (TypedAst.VarDecl) statement;
            line(depth, "VarDecl " + node.getDeclaration().getType() + " "
                    + node.getDeclaration().getName());
            if (node.getInitializer() != null) expression(node.getInitializer(), depth + 1);
        } else if (statement instanceof TypedAst.ArrayAssign) {
            TypedAst.ArrayAssign node = (TypedAst.ArrayAssign) statement;
            line(depth, "ArrayAssign " + node.getArray().getName());
            line(depth + 1, "Index");
            expression(node.getIndex(), depth + 2);
            line(depth + 1, "Value");
            expression(node.getExpression(), depth + 2);
        } else if (statement instanceof TypedAst.Block) {
            line(depth, "Block");
            for (TypedAst.Stmt child : ((TypedAst.Block) statement).getStatements()) {
                statement(child, depth + 1);
            }
        } else if (statement instanceof TypedAst.If) {
            TypedAst.If node = (TypedAst.If) statement;
            line(depth, "If");
            line(depth + 1, "Condition");
            expression(node.getCondition(), depth + 2);
            line(depth + 1, "Then");
            statement(node.getThenStatement(), depth + 2);
            if (node.getElseStatement() != null) {
                line(depth + 1, "Else");
                statement(node.getElseStatement(), depth + 2);
            }
        } else if (statement instanceof TypedAst.While) {
            TypedAst.While node = (TypedAst.While) statement;
            line(depth, "While");
            expression(node.getCondition(), depth + 1);
            statement(node.getBody(), depth + 1);
        } else if (statement instanceof TypedAst.For) {
            TypedAst.For node = (TypedAst.For) statement;
            line(depth, "For");
            if (node.getInitializer() != null) statement(node.getInitializer(), depth + 1);
            expression(node.getCondition(), depth + 1);
            if (node.getUpdate() != null) statement(node.getUpdate(), depth + 1);
            statement(node.getBody(), depth + 1);
        } else if (statement instanceof TypedAst.Return) {
            line(depth, "Return");
            expression(((TypedAst.Return) statement).getExpression(), depth + 1);
        } else if (statement instanceof TypedAst.Printf) {
            TypedAst.Printf node = (TypedAst.Printf) statement;
            line(depth, "Printf \"" + escape(node.getFormat()) + "\"");
            for (TypedAst.Expr expression : node.getExpressions()) expression(expression, depth + 1);
        } else if (statement instanceof TypedAst.CallStmt) {
            TypedAst.CallStmt node = (TypedAst.CallStmt) statement;
            line(depth, "Call " + node.getMethod().getName());
            for (TypedAst.Expr argument : node.getArguments()) expression(argument, depth + 1);
        } else {
            line(depth, statement.getClass().getSimpleName());
        }
    }

    private void expression(TypedAst.Expr expression, int depth) {
        if (expression == null) {
            line(depth, "(null)");
        } else if (expression instanceof TypedAst.BinaryExpr) {
            TypedAst.BinaryExpr binary = (TypedAst.BinaryExpr) expression;
            line(depth, expression.getClass().getSimpleName() + " : " + expression.getType());
            expression(binary.getLeft(), depth + 1);
            expression(binary.getRight(), depth + 1);
        } else if (expression instanceof TypedAst.Not) {
            line(depth, "Not : bool");
            expression(((TypedAst.Not) expression).getExpression(), depth + 1);
        } else if (expression instanceof TypedAst.UnaryMinus) {
            line(depth, "UnaryMinus : " + expression.getType());
            expression(((TypedAst.UnaryMinus) expression).getExpression(), depth + 1);
        } else if (expression instanceof TypedAst.Call) {
            TypedAst.Call node = (TypedAst.Call) expression;
            line(depth, "Call " + node.getMethod().getName() + " : " + node.getType());
            for (TypedAst.Expr argument : node.getArguments()) expression(argument, depth + 1);
        } else if (expression instanceof TypedAst.Id) {
            TypedAst.Id node = (TypedAst.Id) expression;
            line(depth, "Id " + node.getName() + " : " + node.getType());
        } else if (expression instanceof TypedAst.IntLiteral) {
            line(depth, "IntLiteral " + ((TypedAst.IntLiteral) expression).getValue() + " : int");
        } else if (expression instanceof TypedAst.FloatLiteral) {
            line(depth, "FloatLiteral " + ((TypedAst.FloatLiteral) expression).getValue() + " : float");
        } else if (expression instanceof TypedAst.DoubleLiteral) {
            line(depth, "DoubleLiteral " + ((TypedAst.DoubleLiteral) expression).getValue() + " : double");
        } else if (expression instanceof TypedAst.BoolLiteral) {
            line(depth, "BoolLiteral " + ((TypedAst.BoolLiteral) expression).getValue() + " : bool");
        } else if (expression instanceof TypedAst.StringLiteral) {
            line(depth, "String \"" + escape(((TypedAst.StringLiteral) expression).getValue()) + "\"");
        } else if (expression instanceof TypedAst.ArrayAccess) {
            TypedAst.ArrayAccess node = (TypedAst.ArrayAccess) expression;
            line(depth, "ArrayAccess " + node.getArray().getName() + " : " + node.getType());
            expression(node.getIndex(), depth + 1);
        } else if (expression instanceof TypedAst.ArrayLength) {
            line(depth, "ArrayLength " + ((TypedAst.ArrayLength) expression).getArray().getName()
                    + " : int");
        } else {
            line(depth, expression.getClass().getSimpleName() + " : " + expression.getType());
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
    }

    private void line(int depth, String text) {
        for (int i = 0; i < depth; i++) out.append("  ");
        out.append(text).append(System.lineSeparator());
    }
}
