package site.ilemon.ir;

import site.ilemon.exception.CompilerException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the typed LemonIR contract before any backend consumes it.
 */
public final class IrVerifier {
    private final IrProgram program;
    private final Map<String, IrFunction> functionsByName = new HashMap<String, IrFunction>();

    private IrFunction currentFunction;
    private Set<String> blockLabels;
    private Map<String, IrBlock> blocksByLabel;
    private Map<String, Set<String>> predecessors;
    private Set<String> reachableBlocks;
    private Map<Integer, IrType> vregTypes;

    private IrVerifier(IrProgram program) {
        this.program = program;
    }

    public static void verify(IrProgram program) {
        new IrVerifier(program).verifyProgram();
    }

    private void verifyProgram() {
        if (program == null) {
            fail("program is null");
        }
        if (program.getFunctions().isEmpty()) {
            fail("program has no functions");
        }

        for (IrFunction function : program.getFunctions()) {
            if (function == null) {
                fail("program contains null function");
            }
            String name = function.getName();
            if (name == null || name.length() == 0) {
                fail("function has empty name");
            }
            if (functionsByName.containsKey(name)) {
                fail("duplicate function: " + name);
            }
            functionsByName.put(name, function);
        }

        IrFunction main = functionsByName.get("main");
        if (main == null) {
            fail("missing main function");
        }
        if (main.getReturnType() != IrType.VOID) {
            fail("main must return void");
        }
        if (!main.getParameters().isEmpty()) {
            fail("main must not declare Lemon parameters");
        }

        for (IrFunction function : program.getFunctions()) {
            verifyFunction(function);
        }
    }

    private void verifyFunction(IrFunction function) {
        this.currentFunction = function;
        this.blockLabels = new HashSet<String>();
        this.blocksByLabel = new HashMap<String, IrBlock>();
        this.predecessors = new HashMap<String, Set<String>>();
        this.reachableBlocks = new HashSet<String>();
        this.vregTypes = new HashMap<Integer, IrType>();

        if (function.getReturnType() == null) {
            fail("function has null return type");
        }
        if (function.getBlocks().isEmpty()) {
            fail("function has no blocks");
        }

        for (IrValue parameter : function.getParameters()) {
            requireVReg(parameter, "parameter");
            putVRegType(parameter);
        }
        if (function.getParameterTypes().size() != function.getParameters().size()) {
            fail("parameter type count does not match parameter count");
        }
        for (int i = 0; i < function.getParameters().size(); i++) {
            IrType declaredType = function.getParameterTypes().get(i);
            IrType actualType = function.getParameters().get(i).getType();
            if (declaredType != actualType) {
                fail("parameter type mismatch at index " + i);
            }
        }

        for (IrBlock block : function.getBlocks()) {
            if (block == null) {
                fail("function contains null block");
            }
            if (block.getLabel() == null || block.getLabel().length() == 0) {
                fail("block has empty label");
            }
            if (!blockLabels.add(block.getLabel())) {
                fail("duplicate block label: " + block.getLabel());
            }
            blocksByLabel.put(block.getLabel(), block);
            predecessors.put(block.getLabel(), new HashSet<String>());
        }

        for (IrBlock block : function.getBlocks()) {
            verifyBlockStructure(block);
            for (IrInstruction instruction : block.getInstructions()) {
                collectInstructionTypes(instruction);
            }
        }

        for (IrBlock block : function.getBlocks()) {
            for (IrInstruction instruction : block.getInstructions()) {
                verifyInstruction(instruction);
            }
        }
        buildControlFlowGraph(function);
        verifyDefinitions(function);

        this.currentFunction = null;
        this.blockLabels = null;
        this.blocksByLabel = null;
        this.predecessors = null;
        this.reachableBlocks = null;
        this.vregTypes = null;
    }

    private void collectInstructionTypes(IrInstruction instruction) {
        if (instruction == null) {
            fail("block contains null instruction");
        }
        putVRegType(instruction.getResult());
    }

    private void verifyBlockStructure(IrBlock block) {
        List<IrInstruction> instructions = block.getInstructions();
        if (instructions.isEmpty()) {
            fail("block " + block.getLabel() + " is empty and has no terminator");
        }
        for (int i = 0; i < instructions.size(); i++) {
            IrInstruction instruction = instructions.get(i);
            if (instruction == null) {
                fail("block " + block.getLabel() + " contains null instruction");
            }
            IrOpcode opcode = instruction.getOpcode();
            if (opcode == null) {
                fail("block " + block.getLabel() + " contains instruction with null opcode");
            }
            boolean last = i == instructions.size() - 1;
            if ((opcode == IrOpcode.JMP || opcode == IrOpcode.RET || opcode == IrOpcode.EXIT) && !last) {
                fail(instruction, "terminator must be the last instruction in block " + block.getLabel());
            }
            if (opcode == IrOpcode.BR_TRUE || opcode == IrOpcode.BR_FALSE) {
                boolean followedByFinalJump = i == instructions.size() - 2
                        && instructions.get(i + 1) != null
                        && instructions.get(i + 1).getOpcode() == IrOpcode.JMP;
                if (!followedByFinalJump) {
                    fail(instruction, "conditional branch must be followed by a final JMP in block "
                            + block.getLabel());
                }
            }
        }
        if (!block.isTerminated()) {
            fail("block " + block.getLabel() + " has no terminator");
        }
    }

    private void buildControlFlowGraph(IrFunction function) {
        for (IrBlock block : function.getBlocks()) {
            List<IrInstruction> instructions = block.getInstructions();
            int lastIndex = instructions.size() - 1;
            IrInstruction last = instructions.get(lastIndex);
            if (last.getOpcode() == IrOpcode.JMP) {
                addPredecessor(block.getLabel(), last.getLabelTarget());
            }
            if (lastIndex > 0) {
                IrInstruction possibleBranch = instructions.get(lastIndex - 1);
                if (possibleBranch.getOpcode() == IrOpcode.BR_TRUE
                        || possibleBranch.getOpcode() == IrOpcode.BR_FALSE) {
                    addPredecessor(block.getLabel(), possibleBranch.getLabelTarget());
                }
            }
        }

        String entryLabel = function.getBlocks().get(0).getLabel();
        Deque<String> work = new ArrayDeque<String>();
        reachableBlocks.add(entryLabel);
        work.add(entryLabel);
        while (!work.isEmpty()) {
            String label = work.removeFirst();
            for (String successor : successorsOf(blocksByLabel.get(label))) {
                if (reachableBlocks.add(successor)) {
                    work.addLast(successor);
                }
            }
        }
    }

    private void addPredecessor(String source, String target) {
        Set<String> targetPredecessors = predecessors.get(target);
        if (targetPredecessors != null) {
            targetPredecessors.add(source);
        }
    }

    private Set<String> successorsOf(IrBlock block) {
        Set<String> successors = new HashSet<String>();
        List<IrInstruction> instructions = block.getInstructions();
        int lastIndex = instructions.size() - 1;
        IrInstruction last = instructions.get(lastIndex);
        if (last.getOpcode() == IrOpcode.JMP && blocksByLabel.containsKey(last.getLabelTarget())) {
            successors.add(last.getLabelTarget());
        }
        if (lastIndex > 0) {
            IrInstruction branch = instructions.get(lastIndex - 1);
            if ((branch.getOpcode() == IrOpcode.BR_TRUE || branch.getOpcode() == IrOpcode.BR_FALSE)
                    && blocksByLabel.containsKey(branch.getLabelTarget())) {
                successors.add(branch.getLabelTarget());
            }
        }
        return successors;
    }

    private void verifyDefinitions(IrFunction function) {
        Set<Integer> parameterDefinitions = new HashSet<Integer>();
        for (IrValue parameter : function.getParameters()) {
            parameterDefinitions.add(parameter.getId());
        }
        Set<Integer> allDefinitions = new HashSet<Integer>(vregTypes.keySet());
        Map<String, Set<Integer>> in = new HashMap<String, Set<Integer>>();
        Map<String, Set<Integer>> out = new HashMap<String, Set<Integer>>();
        String entryLabel = function.getBlocks().get(0).getLabel();

        for (IrBlock block : function.getBlocks()) {
            if (!reachableBlocks.contains(block.getLabel())) {
                continue;
            }
            if (entryLabel.equals(block.getLabel())) {
                in.put(block.getLabel(), new HashSet<Integer>(parameterDefinitions));
                out.put(block.getLabel(), definitionsAfter(block, parameterDefinitions));
            } else {
                in.put(block.getLabel(), new HashSet<Integer>(allDefinitions));
                out.put(block.getLabel(), new HashSet<Integer>(allDefinitions));
            }
        }

        boolean changed;
        do {
            changed = false;
            for (IrBlock block : function.getBlocks()) {
                String label = block.getLabel();
                if (!reachableBlocks.contains(label) || entryLabel.equals(label)) {
                    continue;
                }
                Set<Integer> newIn = null;
                for (String predecessor : predecessors.get(label)) {
                    if (!reachableBlocks.contains(predecessor)) {
                        continue;
                    }
                    if (newIn == null) {
                        newIn = new HashSet<Integer>(out.get(predecessor));
                    } else {
                        newIn.retainAll(out.get(predecessor));
                    }
                }
                if (newIn == null) {
                    newIn = new HashSet<Integer>();
                }
                Set<Integer> newOut = definitionsAfter(block, newIn);
                if (!newIn.equals(in.get(label)) || !newOut.equals(out.get(label))) {
                    in.put(label, newIn);
                    out.put(label, newOut);
                    changed = true;
                }
            }
        } while (changed);

        for (IrBlock block : function.getBlocks()) {
            if (!reachableBlocks.contains(block.getLabel())) {
                continue;
            }
            Set<Integer> defined = new HashSet<Integer>(in.get(block.getLabel()));
            for (IrInstruction instruction : block.getInstructions()) {
                for (IrValue operand : instruction.getOperands()) {
                    if (operand != null && operand.getKind() == IrValue.Kind.VREG
                            && !defined.contains(operand.getId())) {
                        fail(instruction, "v" + operand.getId() + " is used before it is defined");
                    }
                }
                if (instruction.getResult() != null
                        && instruction.getResult().getKind() == IrValue.Kind.VREG) {
                    defined.add(instruction.getResult().getId());
                }
            }
        }
    }

    private Set<Integer> definitionsAfter(IrBlock block, Set<Integer> incoming) {
        Set<Integer> definitions = new HashSet<Integer>(incoming);
        for (IrInstruction instruction : block.getInstructions()) {
            if (instruction.getResult() != null
                    && instruction.getResult().getKind() == IrValue.Kind.VREG) {
                definitions.add(instruction.getResult().getId());
            }
        }
        return definitions;
    }

    private void verifyInstruction(IrInstruction instruction) {
        if (instruction.getOpcode() == null) {
            fail("instruction has null opcode");
        }
        if (instruction.getOpcode() != IrOpcode.CALL && instruction.getFuncTarget() != null) {
            fail(instruction, "must not have function target");
        }
        if (instruction.getOpcode() != IrOpcode.JMP
                && instruction.getOpcode() != IrOpcode.BR_TRUE
                && instruction.getOpcode() != IrOpcode.BR_FALSE
                && instruction.getLabelTarget() != null) {
            fail(instruction, "must not have label target");
        }
        switch (instruction.getOpcode()) {
            case MOV:
                requireResult(instruction);
                requireOperandCount(instruction, 1);
                requireAssignable(typeOf(instruction.getOperands().get(0)), typeOf(instruction.getResult()), instruction);
                requireInstructionTypeIfPresent(instruction, typeOf(instruction.getResult()));
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                verifyNumericBinary(instruction);
                break;
            case MOD:
                verifyIntBinary(instruction);
                break;
            case I2F:
                verifyCast(instruction, IrType.INT, IrType.FLOAT);
                break;
            case I2D:
                verifyCast(instruction, IrType.INT, IrType.DOUBLE);
                break;
            case F2D:
                verifyCast(instruction, IrType.FLOAT, IrType.DOUBLE);
                break;
            case NOT:
                requireResult(instruction);
                requireResultType(instruction, IrType.BOOL);
                requireOperandCount(instruction, 1);
                requireType(instruction.getOperands().get(0), IrType.BOOL, instruction);
                requireInstructionTypeIfPresent(instruction, IrType.BOOL);
                break;
            case EQ:
            case NE:
                verifyEquality(instruction);
                break;
            case GT:
            case LT:
            case GE:
            case LE:
                verifyOrderedCompare(instruction);
                break;
            case JMP:
                requireNoResult(instruction);
                requireOperandCount(instruction, 0);
                requireLabel(instruction);
                break;
            case BR_TRUE:
            case BR_FALSE:
                requireNoResult(instruction);
                requireOperandCount(instruction, 1);
                requireType(instruction.getOperands().get(0), IrType.BOOL, instruction);
                requireLabel(instruction);
                break;
            case CALL:
                verifyCall(instruction);
                break;
            case RET:
                verifyReturn(instruction);
                break;
            case NEW_ARR:
                requireResult(instruction);
                requireOperandCount(instruction, 1);
                requireType(instruction.getOperands().get(0), IrType.INT, instruction);
                if (instruction.getType() == null || !instruction.getType().isArray()) {
                    fail(instruction, "NEW_ARR must carry an array type");
                }
                requireType(instruction.getResult(), instruction.getType(), instruction);
                break;
            case ARR_GET:
                requireResult(instruction);
                requireOperandCount(instruction, 2);
                requireArray(instruction.getOperands().get(0), instruction);
                requireType(instruction.getOperands().get(1), IrType.INT, instruction);
                requireType(instruction.getResult(), typeOf(instruction.getOperands().get(0)).elementType(), instruction);
                break;
            case ARR_SET:
                requireNoResult(instruction);
                requireOperandCount(instruction, 3);
                requireArray(instruction.getOperands().get(0), instruction);
                requireType(instruction.getOperands().get(1), IrType.INT, instruction);
                requireAssignable(typeOf(instruction.getOperands().get(2)),
                        typeOf(instruction.getOperands().get(0)).elementType(), instruction);
                break;
            case ARR_LEN:
                requireResult(instruction);
                requireResultType(instruction, IrType.INT);
                requireOperandCount(instruction, 1);
                requireArray(instruction.getOperands().get(0), instruction);
                break;
            case PRINT:
                requireNoResult(instruction);
                requireOperandCount(instruction, 1);
                requirePrintable(instruction.getOperands().get(0), instruction);
                break;
            case PRINT_NL:
                requireNoResult(instruction);
                requireOperandCount(instruction, 0);
                break;
            case NEG:
            case INC:
            case DEC:
            case EXIT:
                fail(instruction, "opcode is not supported by both current backends");
                break;
            default:
                fail(instruction, "unknown opcode");
                break;
        }
    }

    private void verifyNumericBinary(IrInstruction instruction) {
        requireResult(instruction);
        requireOperandCount(instruction, 2);
        IrType resultType = typeOf(instruction.getResult());
        if (!resultType.isNumeric()) {
            fail(instruction, "numeric operation result must be numeric");
        }
        requireType(instruction.getOperands().get(0), resultType, instruction);
        requireType(instruction.getOperands().get(1), resultType, instruction);
        requireInstructionTypeIfPresent(instruction, resultType);
    }

    private void verifyIntBinary(IrInstruction instruction) {
        requireResult(instruction);
        requireResultType(instruction, IrType.INT);
        requireOperandCount(instruction, 2);
        requireType(instruction.getOperands().get(0), IrType.INT, instruction);
        requireType(instruction.getOperands().get(1), IrType.INT, instruction);
        requireInstructionTypeIfPresent(instruction, IrType.INT);
    }

    private void verifyCast(IrInstruction instruction, IrType sourceType, IrType targetType) {
        requireResult(instruction);
        requireResultType(instruction, targetType);
        requireOperandCount(instruction, 1);
        requireType(instruction.getOperands().get(0), sourceType, instruction);
        requireInstructionTypeIfPresent(instruction, targetType);
    }

    private void verifyEquality(IrInstruction instruction) {
        requireResult(instruction);
        requireResultType(instruction, IrType.BOOL);
        requireOperandCount(instruction, 2);
        IrType left = typeOf(instruction.getOperands().get(0));
        IrType right = typeOf(instruction.getOperands().get(1));
        if (left.isNumeric() && right.isNumeric()) {
            if (left != right) {
                fail(instruction, "numeric comparison operands must already be promoted");
            }
        } else if (left != IrType.BOOL || right != IrType.BOOL) {
            fail(instruction, "equality operands must both be numeric or both be bool");
        }
        requireInstructionTypeIfPresent(instruction, IrType.BOOL);
    }

    private void verifyOrderedCompare(IrInstruction instruction) {
        requireResult(instruction);
        requireResultType(instruction, IrType.BOOL);
        requireOperandCount(instruction, 2);
        IrType left = typeOf(instruction.getOperands().get(0));
        IrType right = typeOf(instruction.getOperands().get(1));
        if (!left.isNumeric() || !right.isNumeric()) {
            fail(instruction, "ordered comparison operands must be numeric");
        }
        if (left != right) {
            fail(instruction, "ordered comparison operands must already be promoted");
        }
        requireInstructionTypeIfPresent(instruction, IrType.BOOL);
    }

    private void verifyCall(IrInstruction instruction) {
        if (instruction.getFuncTarget() == null || instruction.getFuncTarget().length() == 0) {
            fail(instruction, "CALL has no function target");
        }
        IrFunction target = functionsByName.get(instruction.getFuncTarget());
        if (target == null) {
            fail(instruction, "CALL target does not exist: " + instruction.getFuncTarget());
        }
        List<IrType> parameterTypes = target.getParameterTypes();
        if (instruction.getOperands().size() != parameterTypes.size()) {
            fail(instruction, "CALL argument count mismatch for " + instruction.getFuncTarget());
        }
        for (int i = 0; i < parameterTypes.size(); i++) {
            requireAssignable(typeOf(instruction.getOperands().get(i)), parameterTypes.get(i), instruction);
        }

        IrType returnType = target.getReturnType();
        requireInstructionTypeIfPresent(instruction, returnType);
        if (returnType == IrType.VOID && instruction.getResult() != null) {
            fail(instruction, "void CALL must not produce a result");
        }
        if (instruction.getResult() != null) {
            requireType(instruction.getResult(), returnType, instruction);
        }
    }

    private void verifyReturn(IrInstruction instruction) {
        requireNoResult(instruction);
        if (currentFunction.getReturnType() == IrType.VOID) {
            requireOperandCount(instruction, 0);
            return;
        }
        requireOperandCount(instruction, 1);
        requireAssignable(typeOf(instruction.getOperands().get(0)), currentFunction.getReturnType(), instruction);
    }

    private void requireOperandCount(IrInstruction instruction, int count) {
        if (instruction.getOperands().size() != count) {
            fail(instruction, "expected " + count + " operands, got " + instruction.getOperands().size());
        }
    }

    private void requireResult(IrInstruction instruction) {
        if (instruction.getResult() == null) {
            fail(instruction, "missing result");
        }
        requireVReg(instruction.getResult(), "result");
    }

    private void requireNoResult(IrInstruction instruction) {
        if (instruction.getResult() != null) {
            fail(instruction, "must not have result");
        }
    }

    private void requireResultType(IrInstruction instruction, IrType type) {
        requireType(instruction.getResult(), type, instruction);
    }

    private void requireType(IrValue value, IrType expected, IrInstruction instruction) {
        IrType actual = typeOf(value);
        if (actual != expected) {
            fail(instruction, "expected type " + expected + ", got " + actual);
        }
    }

    private void requireInstructionTypeIfPresent(IrInstruction instruction, IrType expected) {
        if (instruction.getType() != null && instruction.getType() != expected) {
            fail(instruction, "instruction type must be " + expected + ", got " + instruction.getType());
        }
    }

    private void requireAssignable(IrType source, IrType target, IrInstruction instruction) {
        if (source == target) {
            return;
        }
        if (source == IrType.BOOL && target == IrType.INT) {
            return;
        }
        fail(instruction, "cannot assign " + source + " to " + target);
    }

    private void requireArray(IrValue value, IrInstruction instruction) {
        if (!typeOf(value).isArray()) {
            fail(instruction, "expected array operand, got " + typeOf(value));
        }
    }

    private void requirePrintable(IrValue value, IrInstruction instruction) {
        IrType type = typeOf(value);
        if (type == IrType.VOID || type.isArray()) {
            fail(instruction, "PRINT does not support type " + type);
        }
    }

    private void requireLabel(IrInstruction instruction) {
        requireNoLabelMiss(instruction);
        if (!blockLabels.contains(instruction.getLabelTarget())) {
            fail(instruction, "unresolved label: " + instruction.getLabelTarget());
        }
    }

    private void requireNoLabelMiss(IrInstruction instruction) {
        if (instruction.getLabelTarget() == null || instruction.getLabelTarget().length() == 0) {
            fail(instruction, "missing label target");
        }
    }

    private void requireVReg(IrValue value, String role) {
        if (value == null || value.getKind() != IrValue.Kind.VREG) {
            fail(role + " must be a vreg");
        }
        if (value.getType() == null) {
            fail(role + " v" + value.getId() + " has null type");
        }
    }

    private void putVRegType(IrValue value) {
        if (value == null || value.getKind() != IrValue.Kind.VREG) {
            return;
        }
        if (value.getType() == null) {
            fail("v" + value.getId() + " has null type");
        }
        IrType previous = vregTypes.get(value.getId());
        if (previous != null && previous != value.getType()) {
            fail("v" + value.getId() + " has conflicting types: " + previous + " and " + value.getType());
        }
        vregTypes.put(value.getId(), value.getType());
    }

    private IrType typeOf(IrValue value) {
        if (value == null) {
            fail("null IR value");
        }
        if (value.getKind() != IrValue.Kind.VREG) {
            if (value.getType() == null) {
                fail("constant has null type");
            }
            return value.getType();
        }
        IrType type = vregTypes.get(value.getId());
        if (type == null) {
            fail("v" + value.getId() + " is not declared in function");
        }
        return type;
    }

    private void fail(IrInstruction instruction, String message) {
        throw new CompilerException("LemonIR verification failed in function "
                + currentFunction.getName() + ": " + instruction.getOpcode() + ": " + message);
    }

    private void fail(String message) {
        String function = currentFunction == null ? "" : " in function " + currentFunction.getName();
        throw new CompilerException("LemonIR verification failed" + function + ": " + message);
    }
}
