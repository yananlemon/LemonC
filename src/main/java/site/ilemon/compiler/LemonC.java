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
import site.ilemon.typedast.TypedAst;
import site.ilemon.vm.LemonVm;
import site.ilemon.vm.RuntimeStack;
import site.ilemon.vm.Script;
import site.ilemon.vm.VmBytecodeWriter;
import site.ilemon.vm.VmException;
import site.ilemon.vm.VmFunction;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
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

    public static synchronized int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CompilerOptions options = CompilerOptions.parse(args, err);
            if (options == null) {
                usage(err);
                return 1;
            }
            if (options.target != Target.VM
                    && (options.instructionLimit != null || options.stackMaxSize != null)) {
                err.println("error: --vm-instruction-limit and --vm-stack-size require --target vm or --run-vm");
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
            Ast.Program.Base program = parseResult.getProgram();

            SemanticVisitor semantic = SemanticVisitor.collecting();
            TypedAst.Program typedProgram = semantic.analyze(program).getProgram();
            if (!semantic.passOrNot()) {
                err.println("compile failed: semantic analysis has errors");
                printSemanticDiagnostics(semantic, lexer, err);
                return 1;
            }
            if (options.dumpAst) {
                out.println("== AST ==");
                out.print(AstPrinter.print(typedProgram));
            }

            TypedAst.Program optimizedProgram = new AstOptimizer().optimize(typedProgram);
            IrProgram irProgram = buildLemonIr(optimizedProgram);
            if (options.target == Target.VM) {
                return runVmBackend(irProgram, options, out, err);
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
            File classFile = generator.getClassFile(jvmProgram.mainClass.id);
            Files.deleteIfExists(classFile.toPath());
            assembleWithJasmin(generator.getOutputDir(), ilFile, out, err, options.verbose);
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

    private static IrProgram buildLemonIr(TypedAst.Program optimizedProgram) {
        AstToIrTranslator astToIr = new AstToIrTranslator();
        IrProgram irProgram = astToIr.translate(optimizedProgram);
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

    private static int runVmBackend(IrProgram irProgram, CompilerOptions options,
                                    PrintStream out, PrintStream err) {
        if (options.dumpIr) {
            out.println("== LemonIR ==");
            out.print(formatLemonIr(irProgram));
        }

        IrToVmTranslator irToVm = new IrToVmTranslator(irProgram);
        Script script = irToVm.translate();
        if (options.stackMaxSize != null) {
            script.setStackMaxSize(options.stackMaxSize.intValue());
        }

        if (options.dumpVmBytecode) {
            out.println("== LemonVM Bytecode ==");
            out.print(formatVmBytecode(script));
        }

        if (options.emitLbc) {
            // 发射 .lbc 汇编而不执行：这一步的产物能被 VmBytecodeParser 读回，
            // 因此 compile -> emit -> parse -> run 与直接 run 必须等价。
            out.print(VmBytecodeWriter.write(script, irProgram.getClassName()));
            return 0;
        }

        if (options.pipeline) {
            out.println("== LemonVM Output ==");
        }

        LemonVm vm = new LemonVm(script);
        if (options.instructionLimit != null) {
            vm.setInstructionLimit(options.instructionLimit.longValue());
        }
        try {
            out.print(vm.run());
            return 0;
        } catch (VmException e) {
            // JVM 后端是直接写 stdout 的，出错前的输出天然可见；
            // VM 后端把输出攒在缓冲区里，所以这里要先把它吐出来，两个后端才表现一致。
            out.print(vm.getOutput());
            out.flush();
            err.println("runtime error: " + e.getDiagnostic());
            return 1;
        }
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
        err.println("usage: java -jar LemonC.jar <source.lemon> [--pipeline] [--target jvm|vm] [--run-vm]");
        err.println("       [--dump-tokens] [--dump-ast] [--dump-ir] [--dump-vm-bytecode] [--verbose]");
        err.println("       [--vm-instruction-limit N] [--vm-stack-size N] [--emit-lbc]");
        err.println();
        err.println("  --vm-instruction-limit N  LemonVM 指令执行上限，0 表示不限制（默认 "
                + LemonVm.DEFAULT_INSTRUCTION_LIMIT + "）");
        err.println("  --vm-stack-size N         LemonVM 运行时栈容量上限（槽位数，默认 "
                + RuntimeStack.DEFAULT_MAX_STACK_SIZE + "）");
        err.println("  --emit-lbc                发射可被 VmBytecodeParser 读回的 .lbc 汇编，不执行程序");
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
        private boolean emitLbc;
        private boolean runVm;
        private boolean pipeline;
        private boolean verbose;
        private Long instructionLimit;
        private Integer stackMaxSize;

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
                } else if ("--emit-lbc".equals(args[i])) {
                    options.emitLbc = true;
                    options.target = Target.VM;
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
                } else if ("--vm-instruction-limit".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        err.println("error: --vm-instruction-limit requires a number (0 = unlimited)");
                        return null;
                    }
                    Long limit = parseLong(args[++i], "--vm-instruction-limit", err);
                    if (limit == null) {
                        return null;
                    }
                    options.instructionLimit = limit;
                } else if ("--vm-stack-size".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        err.println("error: --vm-stack-size requires a positive number of slots");
                        return null;
                    }
                    Long size = parseLong(args[++i], "--vm-stack-size", err);
                    if (size == null) {
                        return null;
                    }
                    if (size.longValue() <= 0 || size.longValue() > Integer.MAX_VALUE) {
                        err.println("error: --vm-stack-size must be between 1 and " + Integer.MAX_VALUE);
                        return null;
                    }
                    options.stackMaxSize = Integer.valueOf(size.intValue());
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
            if (options.emitLbc && options.target != Target.VM) {
                err.println("error: --emit-lbc requires --target vm");
                return null;
            }
            return options;
        }

        private static Long parseLong(String value, String option, PrintStream err) {
            try {
                return Long.valueOf(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                err.println("error: " + option + " expects an integer, got: " + value);
                return null;
            }
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
