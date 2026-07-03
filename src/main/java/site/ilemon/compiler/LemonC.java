package site.ilemon.compiler;

import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.CompilerException;
import site.ilemon.ir.AstToIrTranslator;
import site.ilemon.ir.IrBlock;
import site.ilemon.ir.IrFunction;
import site.ilemon.ir.IrInstruction;
import site.ilemon.ir.IrProgram;
import site.ilemon.ir.IrToJvmTranslator;
import site.ilemon.ir.IrToVmTranslator;
import site.ilemon.ir.IrVerifier;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.optimizer.AstOptimizer;
import site.ilemon.parser.Parser;
import site.ilemon.parser.ParseDiagnostic;
import site.ilemon.parser.ParseResult;
import site.ilemon.semantic.SemanticVisitor;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.Script;
import site.ilemon.vm.VmFunction;

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
            ParseResult parseResult = parser.parseCollecting();
            if (options.dumpTokens) {
                dumpTokens(lexer, out);
            }
            if (parseResult.hasErrors()) {
                err.println("compile failed: syntax analysis has errors");
                for (ParseDiagnostic diagnostic : parseResult.getDiagnostics()) {
                    err.println(diagnostic.getMessage());
                }
                return 1;
            }
            Ast.Program.T program = parseResult.getProgram();

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
            IrProgram irProgram = buildLemonIr(optimizedProgram);
            if (options.target == Target.VM) {
                return runVmBackend(irProgram, options, out);
            }

            if (options.dumpIr) {
                out.println("== LemonIR ==");
                out.print(formatLemonIr(irProgram));
            }

            site.ilemon.codegen.ast.Ast.Program.ProgramSingle jvmProgram =
                    new IrToJvmTranslator(irProgram).translate();
            ByteCodeGenerator generator = new ByteCodeGenerator();
            generator.visit(jvmProgram);
            File ilFile = generator.getOutputFile();
            assembleWithJasmin(generator.getOutputDir(), ilFile, out, err, options.verbose);
            File classFile = generator.getClassFile(jvmProgram.mainClass.id);
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

    private static IrProgram buildLemonIr(Ast.Program.T optimizedProgram) {
        AstToIrTranslator astToIr = new AstToIrTranslator();
        optimizedProgram.accept(astToIr);
        IrProgram irProgram = astToIr.getProgram();
        IrVerifier.verify(irProgram);
        return irProgram;
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

    private static int runVmBackend(IrProgram irProgram, CompilerOptions options, PrintStream out) {
        if (options.dumpIr) {
            out.println("== LemonIR ==");
            out.print(formatLemonIr(irProgram));
        }

        IrToVmTranslator irToVm = new IrToVmTranslator(irProgram);
        Script script = irToVm.translate();

        if (options.dumpVmBytecode) {
            out.println("== LemonVM Bytecode ==");
            out.print(formatVmBytecode(script));
        }

        if (options.pipeline) {
            out.println("== LemonVM Output ==");
        }
        out.print(new LemonVm(script).run());
        return 0;
    }

    private static String formatLemonIr(IrProgram program) {
        StringBuilder sb = new StringBuilder();
        for (IrFunction func : program.getFunctions()) {
            sb.append("func ").append(func.getName()).append(" {").append(System.lineSeparator());
            for (IrBlock block : func.getBlocks()) {
                sb.append(block.getLabel()).append(":").append(System.lineSeparator());
                for (IrInstruction instr : block.getInstructions()) {
                    sb.append("    ").append(instr).append(System.lineSeparator());
                }
            }
            sb.append("}").append(System.lineSeparator()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String formatVmBytecode(Script script) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < script.getFuncCount(); i++) {
            VmFunction func = script.getFunc(i);
            sb.append("Func ").append(func.getName())
                    .append(" [Entry PC: ").append(func.getEntryPoint())
                    .append(", Params: ").append(func.getParamCount())
                    .append(", Locals: ").append(func.getLocalDataSize())
                    .append("]").append(System.lineSeparator());
        }
        for (int i = 0; i < script.getInstrStream().length; i++) {
            sb.append(String.format("%04d  %s", i, script.getInstrStream()[i]))
                    .append(System.lineSeparator());
        }
        return sb.toString();
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
        for (Token token : lexer.getTokens()) {
            out.printf("%4d:%-3d %-14s  %s%n",
                    token.getLineNumber(), token.getColumnNumber(), token.getKind(), token.getLexeme());
        }
    }

    private static void usage(PrintStream err) {
        err.println("usage: java -jar LemonC.jar <source.lemon> [--pipeline] [--target jvm|vm] [--run-vm] [--dump-tokens] [--dump-ast] [--dump-ir] [--dump-vm-bytecode] [--verbose]");
    }

    private enum Target {
        JVM,
        VM
    }

    private static final class CompilerOptions {
        private final String sourcePath;
        private Target target = Target.JVM;
        private boolean dumpTokens;
        private boolean dumpAst;
        private boolean dumpIr;
        private boolean dumpVmBytecode;
        private boolean runVm;
        private boolean pipeline;
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
                } else if ("--dump-vm-bytecode".equals(args[i])) {
                    options.dumpVmBytecode = true;
                } else if ("--pipeline".equals(args[i])) {
                    options.pipeline = true;
                    options.target = Target.VM;
                    options.dumpTokens = true;
                    options.dumpAst = true;
                    options.dumpIr = true;
                    options.dumpVmBytecode = true;
                } else if ("--run-vm".equals(args[i])) {
                    options.runVm = true;
                    options.target = Target.VM;
                } else if ("--target".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        err.println("error: --target requires jvm or vm");
                        return null;
                    }
                    options.target = parseTarget(args[++i], err);
                    if (options.target == null) {
                        return null;
                    }
                } else if (args[i].startsWith("--target=")) {
                    options.target = parseTarget(args[i].substring("--target=".length()), err);
                    if (options.target == null) {
                        return null;
                    }
                } else if ("--verbose".equals(args[i])) {
                    options.verbose = true;
                } else {
                    err.println("error: unknown option - " + args[i]);
                    return null;
                }
            }
            if (options.pipeline || options.runVm) {
                options.target = Target.VM;
            }
            if (options.dumpVmBytecode && options.target != Target.VM) {
                err.println("error: --dump-vm-bytecode requires --target vm or --run-vm");
                return null;
            }
            return options;
        }

        private static Target parseTarget(String value, PrintStream err) {
            if ("jvm".equalsIgnoreCase(value)) {
                return Target.JVM;
            }
            if ("vm".equalsIgnoreCase(value) || "lemonvm".equalsIgnoreCase(value)) {
                return Target.VM;
            }
            err.println("error: unsupported target - " + value);
            return null;
        }
    }
}
