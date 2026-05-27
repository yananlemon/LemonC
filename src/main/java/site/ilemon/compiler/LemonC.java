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

/**
 * LemonC command line entry point.
 */
public class LemonC {

    public static void main(String[] args) {
        try {
            CompilerOptions options = CompilerOptions.parse(args);
            if (options == null) {
                usage();
                System.exit(1);
            }
            if (!options.sourcePath.endsWith(".lemon")) {
                System.err.println("error: source file must end with .lemon, got: " + options.sourcePath);
                usage();
                System.exit(1);
            }

            File sourceFile = new File(options.sourcePath);
            if (!sourceFile.exists()) {
                System.err.println("error: file does not exist - " + options.sourcePath);
                System.exit(1);
            }
            if (!sourceFile.canRead()) {
                System.err.println("error: file is not readable - " + options.sourcePath);
                System.exit(1);
            }

            Label.resetCounter();

            Lexer lexer = new Lexer(sourceFile);
            Parser parser = new Parser(lexer);
            Ast.Program.T program = parser.parse();
            if (options.dumpTokens) {
                dumpTokens(lexer);
            }

            SemanticVisitor semantic = new SemanticVisitor();
            semantic.visit(program);
            if (!semantic.passOrNot()) {
                System.err.println("compile failed: semantic analysis has errors");
                System.exit(1);
            }
            if (options.dumpAst) {
                System.out.println("== AST ==");
                System.out.print(AstPrinter.print(program));
            }

            Ast.Program.T optimizedProgram = new AstOptimizer().optimize(program);

            TranslatorVisitor translator = new TranslatorVisitor();
            translator.visit(optimizedProgram);
            if (options.dumpIr) {
                System.out.println("== IR ==");
                System.out.print(IrPrinter.print(translator.prog));
            }

            ByteCodeGenerator generator = new ByteCodeGenerator();
            generator.visit(translator.prog);
            File ilFile = generator.getOutputFile();
            jasmin.Main.main(new String[]{"-d", generator.getOutputDir().getPath(), ilFile.getPath()});
        } catch (CompilerException e) {
            System.err.println("compile failed: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("io error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void dumpTokens(Lexer lexer) {
        System.out.println("== TOKENS ==");
        for (Token token : lexer.tokens) {
            System.out.printf("%4d  %-14s  %s%n", token.lineNumber, token.kind, token.lexeme);
        }
    }

    private static void usage() {
        System.err.println("usage: java -jar LemonC.jar <source.lemon> [--dump-tokens] [--dump-ast] [--dump-ir]");
    }

    private static final class CompilerOptions {
        private final String sourcePath;
        private boolean dumpTokens;
        private boolean dumpAst;
        private boolean dumpIr;

        private CompilerOptions(String sourcePath) {
            this.sourcePath = sourcePath;
        }

        private static CompilerOptions parse(String[] args) {
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
                } else {
                    System.err.println("error: unknown option - " + args[i]);
                    return null;
                }
            }
            return options;
        }
    }
}
