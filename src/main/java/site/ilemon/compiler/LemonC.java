package site.ilemon.compiler;

import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.CompilerException;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * LemonC command line entry point.
 */
public class LemonC {

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CompilerOptions options = CompilerOptions.parse(args, err);
            if (options == null) {
                usage(err);
                return 1;
            }
            if (!options.sourcePath.endsWith(".lemon")) {
                err.println("error: source file must end with .lemon, got: " + options.sourcePath);
                usage(err);
                return 1;
            }

            File sourceFile = new File(options.sourcePath);
            if (!sourceFile.exists()) {
                err.println("error: file does not exist - " + options.sourcePath);
                return 1;
            }
            if (!sourceFile.canRead()) {
                err.println("error: file is not readable - " + options.sourcePath);
                return 1;
            }

            Label.resetCounter();

            Lexer lexer = new Lexer(sourceFile);
            Parser parser = new Parser(lexer);
            Ast.Program.T program = parser.parse();
            if (options.dumpTokens) {
                dumpTokens(lexer, out);
            }

            SemanticVisitor semantic = SemanticVisitor.collecting();
            semantic.visit(program);
            if (!semantic.passOrNot()) {
                err.println("compile failed: semantic analysis has errors");
                printSemanticDiagnostics(semantic, lexer, err);
                return 1;
            }
            if (options.dumpAst) {
                out.println("== AST ==");
                out.print(AstPrinter.print(program));
            }

            Ast.Program.T optimizedProgram = new AstOptimizer().optimize(program);

            TranslatorVisitor translator = new TranslatorVisitor();
            translator.visit(optimizedProgram);
            if (options.dumpIr) {
                out.println("== IR ==");
                out.print(IrPrinter.print(translator.prog));
            }

            ByteCodeGenerator generator = new ByteCodeGenerator();
            generator.visit(translator.prog);
            File ilFile = generator.getOutputFile();
            assembleWithJasmin(generator.getOutputDir(), ilFile, out, err, options.verbose);
            File classFile = generator.getClassFile(translator.prog.mainClass.id);
            if (!classFile.isFile() || classFile.length() == 0) {
                throw new CompilerException("Jasmin did not generate class file: " + classFile.getPath());
            }
            return 0;
        } catch (CompilerException e) {
            err.println("compile failed: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            err.println("io error: " + e.getMessage());
            return 1;
        }
    }

    private static void printSemanticDiagnostics(SemanticVisitor semantic, Lexer lexer, PrintStream err) {
        ArrayList<String> diagnostics = semantic.getErrors();
        ArrayList<Integer> lineNumbers = semantic.getErrorLineNumbers();
        for (int i = 0; i < diagnostics.size(); i++) {
            err.println(diagnostics.get(i));
            if (i < lineNumbers.size()) {
                printSourcePointer(lexer.getSourceLine(lineNumbers.get(i)), err);
            }
        }
    }

    private static void printSourcePointer(String sourceLine, PrintStream err) {
        if (sourceLine == null || sourceLine.isEmpty()) {
            return;
        }
        int column = firstNonWhitespaceColumn(sourceLine);
        err.println("    " + sourceLine);
        err.print("    ");
        for (int i = 1; i < column; i++) {
            err.print(' ');
        }
        err.println('^');
    }

    private static int firstNonWhitespaceColumn(String sourceLine) {
        for (int i = 0; i < sourceLine.length(); i++) {
            if (!Character.isWhitespace(sourceLine.charAt(i))) {
                return i + 1;
            }
        }
        return 1;
    }

    private static void assembleWithJasmin(File outputDir, File ilFile, PrintStream out, PrintStream err, boolean verbose) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        PrintStream quiet = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
        synchronized (LemonC.class) {
            try {
                System.setOut(verbose ? out : quiet);
                System.setErr(verbose ? err : quiet);
                jasmin.Main.main(new String[]{"-d", outputDir.getPath(), ilFile.getPath()});
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
                quiet.close();
            }
        }
    }

    private static void dumpTokens(Lexer lexer, PrintStream out) {
        out.println("== TOKENS ==");
        for (Token token : lexer.tokens) {
            out.printf("%4d:%-3d %-14s  %s%n",
                    token.lineNumber, token.columnNumber, token.kind, token.lexeme);
        }
    }

    private static void usage(PrintStream err) {
        err.println("usage: java -jar LemonC.jar <source.lemon> [--dump-tokens] [--dump-ast] [--dump-ir] [--verbose]");
    }

    private static final class CompilerOptions {
        private final String sourcePath;
        private boolean dumpTokens;
        private boolean dumpAst;
        private boolean dumpIr;
        private boolean verbose;

        private CompilerOptions(String sourcePath) {
            this.sourcePath = sourcePath;
        }

        private static CompilerOptions parse(String[] args, PrintStream err) {
            if (args == null || args.length < 1) {
                return null;
            }
            CompilerOptions options = new CompilerOptions(args[0]);
            for (int i = 1; i < args.length; i++) {
                if ("--dump-tokens".equals(args[i])) {
                    options.dumpTokens = true;
                } else if ("--dump-ast".equals(args[i])) {
                    options.dumpAst = true;
                } else if ("--dump-ir".equals(args[i])) {
                    options.dumpIr = true;
                } else if ("--verbose".equals(args[i])) {
                    options.verbose = true;
                } else {
                    err.println("error: unknown option - " + args[i]);
                    return null;
                }
            }
            return options;
        }
    }
}
