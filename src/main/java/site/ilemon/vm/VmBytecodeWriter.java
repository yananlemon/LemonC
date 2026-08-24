package site.ilemon.vm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serializes an executable {@link Script} back into LemonVM textual bytecode (`.lbc`).
 *
 * <p>This is the exact inverse of {@link VmBytecodeParser}: whatever this class writes
 * must parse back into an equivalent script. The pair gives the VM backend a
 * round-trip property that is checked by tests — compile, emit, parse, run, and the
 * output must match running the script directly.</p>
 *
 * <h3>Operand encoding</h3>
 * <p>The `.lbc` grammar distinguishes stack references from immediates by spelling:
 * a bare integer is a stack slot, a `#`-prefixed number is a literal. Getting this
 * backwards silently turns `Mov -1, 15` (store literal 15) into "copy from slot 15",
 * so the mapping is centralized in {@link #operand}.</p>
 */
public final class VmBytecodeWriter {

    private VmBytecodeWriter() {
    }

    public static String write(Script script) {
        return write(script, "LemonProgram");
    }

    public static String write(Script script, String className) {
        if (script == null) {
            throw new VmException("script is null");
        }
        Instruction[] instructions = script.getInstrStream();
        if (instructions == null) {
            throw new VmException("script has no instruction stream");
        }

        Map<String, Integer> functionIndexes = new HashMap<String, Integer>();
        for (int i = 0; i < script.getFuncCount(); i++) {
            functionIndexes.put(script.getFunc(i).getName(), Integer.valueOf(i));
        }

        Set<Integer> jumpTargets = collectJumpTargets(instructions);

        StringBuilder out = new StringBuilder();
        out.append(".version 1").append('\n');
        out.append(".class ").append(
                className == null || className.length() == 0 ? "LemonProgram" : className)
                .append('\n');

        for (int i = 0; i < script.getFuncCount(); i++) {
            VmFunction function = script.getFunc(i);
            int start = function.getEntryPoint();
            int end = i + 1 < script.getFuncCount()
                    ? script.getFunc(i + 1).getEntryPoint() : instructions.length;
            if (start < 0 || end > instructions.length || start > end) {
                throw new VmException("function " + function.getName()
                        + " has an out-of-range instruction span: " + start + ".." + end);
            }

            out.append('\n');
            // 第 4 个字段是返回类型。解析器读取但不使用它，Script/VmFunction 也不携带
            // 返回类型，因此这里只能写占位值。
            out.append(".func ").append(function.getName())
                    .append(' ').append(function.getParamCount())
                    .append(' ').append(function.getLocalDataSize())
                    .append(' ').append("void").append('\n');

            for (int pc = start; pc < end; pc++) {
                if (jumpTargets.contains(Integer.valueOf(pc))) {
                    out.append(label(pc)).append(":").append('\n');
                }
                out.append("    ")
                        .append(instruction(instructions[pc], pc, start, end, functionIndexes, script))
                        .append('\n');
            }
            out.append(".end").append('\n');
        }
        return out.toString();
    }

    private static Set<Integer> collectJumpTargets(Instruction[] instructions) {
        Set<Integer> targets = new HashSet<Integer>();
        for (Instruction instruction : instructions) {
            if (instruction == null) {
                throw new VmException("instruction stream contains null");
            }
            for (Value operand : instruction.getOperands()) {
                if (operand != null && operand.type == Value.Type.INSTR_INDEX
                        && !operand.isStackRef && !operand.isRetValRef) {
                    targets.add(Integer.valueOf(operand.instrIndex));
                }
            }
        }
        return targets;
    }

    private static String instruction(Instruction instruction, int pc, int start, int end,
                                      Map<String, Integer> functionIndexes, Script script) {
        Opcode opcode = instruction.getOpcode();
        List<Value> operands = instruction.getOperands();
        if (operands.size() != opcode.getOperandCount()) {
            throw new VmException("cannot emit " + opcode.getMnemonic() + " at PC=" + pc
                    + ": expected " + opcode.getOperandCount()
                    + " operands, got " + operands.size());
        }
        StringBuilder text = new StringBuilder(opcode.getMnemonic());
        for (int i = 0; i < operands.size(); i++) {
            text.append(i == 0 ? " " : ", ");
            text.append(operand(operands.get(i), pc, start, end, functionIndexes, script));
        }
        return text.toString();
    }

    private static String operand(Value value, int pc, int start, int end,
                                  Map<String, Integer> functionIndexes, Script script) {
        if (value == null) {
            throw new VmException("null operand at PC=" + pc);
        }
        if (value.isRetValRef) {
            return "_RetVal";
        }
        if (value.isStackRef) {
            // 裸整数在 .lbc 里就是栈槽位；立即数必须带 '#'。
            return String.valueOf(value.intValue);
        }
        switch (value.type) {
            case INSTR_INDEX:
                if (value.instrIndex < start || value.instrIndex >= end) {
                    throw new VmException("jump target " + value.instrIndex + " at PC=" + pc
                            + " leaves its function span " + start + ".." + end
                            + "; .lbc labels are function-local");
                }
                return label(value.instrIndex);
            case FUNC_INDEX:
                return "_" + functionName(value.funcIndex, script);
            case INT:
                return "#" + value.intValue;
            case FLOAT:
                return "#" + value.floatValue + "f";
            case DOUBLE:
                return "#" + value.doubleValue + "d";
            case BOOL:
                return value.boolValue ? "true" : "false";
            case STRING:
                return "\"" + escape(value.stringValue) + "\"";
            default:
                throw new VmException("cannot emit operand of type " + value.type
                        + " at PC=" + pc);
        }
    }

    private static String functionName(int funcIndex, Script script) {
        if (funcIndex < 0 || funcIndex >= script.getFuncCount()) {
            throw new VmException("function index out of range: " + funcIndex);
        }
        return script.getFunc(funcIndex).getName();
    }

    private static String label(int pc) {
        return "L" + pc;
    }

    /** 与 {@code VmBytecodeParser.unescape} 支持的转义集合保持一致。 */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\n': escaped.append("\\n"); break;
                case '\t': escaped.append("\\t"); break;
                case '\r': escaped.append("\\r"); break;
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                default: escaped.append(ch); break;
            }
        }
        return escaped.toString();
    }
}
