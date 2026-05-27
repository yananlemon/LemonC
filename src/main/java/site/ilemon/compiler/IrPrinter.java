package site.ilemon.compiler;

import site.ilemon.codegen.ast.Ast;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Small teaching-oriented pretty printer for the lowered JVM IR.
 */
public final class IrPrinter {

    private IrPrinter() {
    }

    public static String print(Ast.Program.T program) {
        IrPrinter printer = new IrPrinter();
        printer.program(program);
        return printer.out.toString();
    }

    private final StringBuilder out = new StringBuilder();

    private void program(Ast.Program.T program) {
        line(0, "IR Program");
        if (program instanceof Ast.Program.ProgramSingle) {
            Ast.Program.ProgramSingle node = (Ast.Program.ProgramSingle) program;
            mainClass(node.mainClass, 1);
        } else {
            line(1, nodeName(program));
        }
    }

    private void mainClass(Ast.MainClass.MainClassSingle mainClass, int depth) {
        line(depth, "Class " + mainClass.id);
        if (mainClass.methods != null) {
            for (Ast.Method.MethodSingle method : mainClass.methods) {
                method(method, depth + 1);
            }
        }
    }

    private void method(Ast.Method.MethodSingle method, int depth) {
        line(depth, "Method " + method.id + " : " + method.retType);
        declarations("Params", method.formals, depth + 1);
        declarations("Locals", method.locals, depth + 1);
        line(depth + 1, "Instructions");
        if (method.stms != null) {
            int index = 0;
            for (Ast.Stmt.T stmt : method.stms) {
                line(depth + 2, String.format("%04d  %s", index++, instruction(stmt)));
            }
        }
    }

    private void declarations(String title, List<Ast.Declare.DeclareSingle> declarations, int depth) {
        line(depth, title);
        if (declarations == null || declarations.isEmpty()) {
            line(depth + 1, "(none)");
            return;
        }
        for (Ast.Declare.DeclareSingle declaration : declarations) {
            line(depth + 1, declaration.type + " " + declaration.id);
        }
    }

    private String instruction(Ast.Stmt.T stmt) {
        StringBuilder builder = new StringBuilder(nodeName(stmt));
        Field[] fields = stmt.getClass().getFields();
        if (fields.length == 0) {
            return builder.toString();
        }
        builder.append("(");
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(field.getName()).append("=");
            try {
                builder.append(formatValue(field.get(stmt)));
            } catch (IllegalAccessException e) {
                builder.append("?");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + ((String) value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return value.toString();
    }

    private String nodeName(Object node) {
        return node.getClass().getSimpleName();
    }

    private void line(int depth, String text) {
        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
        out.append(text).append(System.lineSeparator());
    }
}
