package site.ilemon.compiler;

import site.ilemon.ast.Ast;

import java.util.List;

/**
 * Small teaching-oriented pretty printer for the front-end AST.
 */
public final class AstPrinter {

    private AstPrinter() {
    }

    public static String print(Ast.Program.Base program) {
        AstPrinter printer = new AstPrinter();
        printer.program(program);
        return printer.out.toString();
    }

    private final StringBuilder out = new StringBuilder();

    private void program(Ast.Program.Base program) {
        line(0, "Program");
        if (program instanceof Ast.Program.ProgramSingle) {
            mainClass(((Ast.Program.ProgramSingle) program).getMainClass(), 1);
        } else {
            line(1, nodeName(program));
        }
    }

    private void mainClass(Ast.MainClass.Base mainClass, int depth) {
        if (mainClass instanceof Ast.MainClass.MainClassSingle) {
            Ast.MainClass.MainClassSingle node = (Ast.MainClass.MainClassSingle) mainClass;
            line(depth, "Class " + node.getClassId());
            if (node.getMethods() != null) {
                for (Ast.Method.Base method : node.getMethods()) {
                    method(method, depth + 1);
                }
            }
        } else {
            line(depth, nodeName(mainClass));
        }
    }

    private void method(Ast.Method.Base method, int depth) {
        if (method instanceof Ast.Method.MethodSingle) {
            Ast.Method.MethodSingle node = (Ast.Method.MethodSingle) method;
            line(depth, "Method " + node.getId() + " : " + type(node.getRetType()));
            declarations("Params", node.getFormals(), depth + 1);
            declarations("Locals", node.getLocals(), depth + 1);
            line(depth + 1, "Body");
            for (Ast.Stmt.Base stmt : node.getStms()) {
                stmt(stmt, depth + 2);
            }
            if (node.getRetExp() != null) {
                line(depth + 1, "Exit");
                stmt(node.getRetExp(), depth + 2);
            }
        } else {
            line(depth, nodeName(method));
        }
    }

    private void declarations(String title, List<Ast.Declare.Base> declarations, int depth) {
        line(depth, title);
        if (declarations == null || declarations.isEmpty()) {
            line(depth + 1, "(none)");
            return;
        }
        for (Ast.Declare.Base declaration : declarations) {
            if (declaration instanceof Ast.Declare.DeclareSingle) {
                Ast.Declare.DeclareSingle node = (Ast.Declare.DeclareSingle) declaration;
                line(depth + 1, type(node.getType()) + " " + node.getId());
            } else {
                line(depth + 1, nodeName(declaration));
            }
        }
    }

    private void stmt(Ast.Stmt.Base stmt, int depth) {
        if (stmt instanceof Ast.Stmt.Assign) {
            Ast.Stmt.Assign node = (Ast.Stmt.Assign) stmt;
            line(depth, "Assign " + node.getId().getId());
            expr(node.getExpr(), depth + 1);
        } else if (stmt instanceof Ast.Stmt.VarDecl) {
            Ast.Stmt.VarDecl node = (Ast.Stmt.VarDecl) stmt;
            Ast.Declare.DeclareSingle declaration = node.getDeclaration();
            line(depth, "VarDecl " + type(declaration.getType()) + " " + declaration.getId());
            if (node.getInitializer() != null) {
                expr(node.getInitializer(), depth + 1);
            }
        } else if (stmt instanceof Ast.Stmt.ArrayAssign) {
            Ast.Stmt.ArrayAssign node = (Ast.Stmt.ArrayAssign) stmt;
            line(depth, "ArrayAssign " + node.getArrayName());
            line(depth + 1, "Index");
            expr(node.getIndex(), depth + 2);
            line(depth + 1, "Value");
            expr(node.getExpr(), depth + 2);
        } else if (stmt instanceof Ast.Stmt.Block) {
            line(depth, "Block");
            for (Ast.Stmt.Base child : ((Ast.Stmt.Block) stmt).getStmts()) {
                stmt(child, depth + 1);
            }
        } else if (stmt instanceof Ast.Stmt.If) {
            Ast.Stmt.If node = (Ast.Stmt.If) stmt;
            line(depth, "If");
            line(depth + 1, "Condition");
            expr(node.getCondition(), depth + 2);
            line(depth + 1, "Then");
            stmt(node.getThenStmt(), depth + 2);
            if (node.getElseStmt() != null) {
                line(depth + 1, "Else");
                stmt(node.getElseStmt(), depth + 2);
            }
        } else if (stmt instanceof Ast.Stmt.While) {
            Ast.Stmt.While node = (Ast.Stmt.While) stmt;
            line(depth, "While");
            line(depth + 1, "Condition");
            expr(node.getCondition(), depth + 2);
            line(depth + 1, "Body");
            stmt(node.getBody(), depth + 2);
        } else if (stmt instanceof Ast.Stmt.For) {
            Ast.Stmt.For node = (Ast.Stmt.For) stmt;
            line(depth, "For");
            line(depth + 1, "Init");
            stmt(node.getInit(), depth + 2);
            line(depth + 1, "Condition");
            expr(node.getCondition(), depth + 2);
            line(depth + 1, "Update");
            stmt(node.getUpdate(), depth + 2);
            line(depth + 1, "Body");
            stmt(node.getBody(), depth + 2);
        } else if (stmt instanceof Ast.Stmt.Return) {
            line(depth, "Return");
            expr(((Ast.Stmt.Return) stmt).getExpr(), depth + 1);
        } else if (stmt instanceof Ast.Stmt.Printf) {
            Ast.Stmt.Printf node = (Ast.Stmt.Printf) stmt;
            line(depth, "Printf \"" + escape(node.getFormat()) + "\"");
            for (Ast.Expr.Base expr : node.getExprs()) {
                expr(expr, depth + 1);
            }
        } else if (stmt instanceof Ast.Stmt.Call) {
            Ast.Stmt.Call node = (Ast.Stmt.Call) stmt;
            line(depth, "Call " + node.getName());
            for (Ast.Expr.Base arg : node.getInputParams()) {
                expr(arg, depth + 1);
            }
        } else if (stmt instanceof Ast.Stmt.PrintLine) {
            line(depth, "PrintLine");
        } else if (stmt instanceof Ast.Stmt.Break) {
            line(depth, "Break");
        } else if (stmt instanceof Ast.Stmt.Continue) {
            line(depth, "Continue");
        } else {
            line(depth, nodeName(stmt));
        }
    }

    private void expr(Ast.Expr.Base expr, int depth) {
        if (expr == null) {
            line(depth, "(null)");
        } else if (binary(expr, depth)) {
            return;
        } else if (expr instanceof Ast.Expr.Not) {
            line(depth, "Not");
            expr(((Ast.Expr.Not) expr).getExpr(), depth + 1);
        } else if (expr instanceof Ast.Expr.Call) {
            Ast.Expr.Call node = (Ast.Expr.Call) expr;
            line(depth, "Call " + node.getName() + " : " + type(node.getReturnType()));
            for (Ast.Expr.Base arg : node.getInputParams()) {
                expr(arg, depth + 1);
            }
        } else if (expr instanceof Ast.Expr.Id) {
            Ast.Expr.Id node = (Ast.Expr.Id) expr;
            line(depth, "Id " + node.getId() + " : " + type(node.getType()));
        } else if (expr instanceof Ast.Expr.IntLiteral) {
            Ast.Expr.IntLiteral node = (Ast.Expr.IntLiteral) expr;
            line(depth, "IntLiteral " + node.getValue() + " : Int");
        } else if (expr instanceof Ast.Expr.FloatLiteral) {
            Ast.Expr.FloatLiteral node = (Ast.Expr.FloatLiteral) expr;
            line(depth, "FloatLiteral " + node.getValue() + " : Float");
        } else if (expr instanceof Ast.Expr.DoubleLiteral) {
            Ast.Expr.DoubleLiteral node = (Ast.Expr.DoubleLiteral) expr;
            line(depth, "DoubleLiteral " + node.getValue() + " : Double");
        } else if (expr instanceof Ast.Expr.Str) {
            line(depth, "String \"" + escape(((Ast.Expr.Str) expr).getValue()) + "\"");
        } else if (expr instanceof Ast.Expr.True) {
            line(depth, "True");
        } else if (expr instanceof Ast.Expr.False) {
            line(depth, "False");
        } else if (expr instanceof Ast.Expr.ArrayAccess) {
            Ast.Expr.ArrayAccess node = (Ast.Expr.ArrayAccess) expr;
            line(depth, "ArrayAccess " + node.getArrayName() + " : " + type(node.getElementType()));
            expr(node.getIndex(), depth + 1);
        } else if (expr instanceof Ast.Expr.ArrayLength) {
            line(depth, "ArrayLength " + ((Ast.Expr.ArrayLength) expr).getArrayName());
        } else {
            line(depth, nodeName(expr));
        }
    }

    private boolean binary(Ast.Expr.Base expr, int depth) {
        Ast.Expr.Base left;
        Ast.Expr.Base right;
        if (expr instanceof Ast.Expr.Add) {
            left = ((Ast.Expr.Add) expr).getLeft();
            right = ((Ast.Expr.Add) expr).getRight();
        } else if (expr instanceof Ast.Expr.Sub) {
            left = ((Ast.Expr.Sub) expr).getLeft();
            right = ((Ast.Expr.Sub) expr).getRight();
        } else if (expr instanceof Ast.Expr.Mul) {
            left = ((Ast.Expr.Mul) expr).getLeft();
            right = ((Ast.Expr.Mul) expr).getRight();
        } else if (expr instanceof Ast.Expr.Div) {
            left = ((Ast.Expr.Div) expr).getLeft();
            right = ((Ast.Expr.Div) expr).getRight();
        } else if (expr instanceof Ast.Expr.Mod) {
            left = ((Ast.Expr.Mod) expr).getLeft();
            right = ((Ast.Expr.Mod) expr).getRight();
        } else if (expr instanceof Ast.Expr.And) {
            left = ((Ast.Expr.And) expr).getLeft();
            right = ((Ast.Expr.And) expr).getRight();
        } else if (expr instanceof Ast.Expr.Or) {
            left = ((Ast.Expr.Or) expr).getLeft();
            right = ((Ast.Expr.Or) expr).getRight();
        } else if (expr instanceof Ast.Expr.GT) {
            left = ((Ast.Expr.GT) expr).getLeft();
            right = ((Ast.Expr.GT) expr).getRight();
        } else if (expr instanceof Ast.Expr.LT) {
            left = ((Ast.Expr.LT) expr).getLeft();
            right = ((Ast.Expr.LT) expr).getRight();
        } else if (expr instanceof Ast.Expr.GTE) {
            left = ((Ast.Expr.GTE) expr).getLeft();
            right = ((Ast.Expr.GTE) expr).getRight();
        } else if (expr instanceof Ast.Expr.LTE) {
            left = ((Ast.Expr.LTE) expr).getLeft();
            right = ((Ast.Expr.LTE) expr).getRight();
        } else if (expr instanceof Ast.Expr.EQ) {
            left = ((Ast.Expr.EQ) expr).getLeft();
            right = ((Ast.Expr.EQ) expr).getRight();
        } else if (expr instanceof Ast.Expr.NEQ) {
            left = ((Ast.Expr.NEQ) expr).getLeft();
            right = ((Ast.Expr.NEQ) expr).getRight();
        } else {
            return false;
        }

        line(depth, nodeName(expr));
        expr(left, depth + 1);
        expr(right, depth + 1);
        return true;
    }

    private String type(Ast.Type.Base type) {
        return type == null ? "?" : type.toString();
    }

    private String nodeName(Object node) {
        return node.getClass().getSimpleName();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
    }

    private void line(int depth, String text) {
        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
        out.append(text).append(System.lineSeparator());
    }
}
