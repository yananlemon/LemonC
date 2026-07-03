package site.ilemon.ir;

import site.ilemon.codegen.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.CompilerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Translates typed LemonIR to the stack-oriented JVM/Jasmin IR consumed by
 * {@link site.ilemon.codegen.ByteCodeGenerator}.
 */
public class IrToJvmTranslator {
    private final IrProgram program;
    private final Map<String, IrFunction> functionsByName = new HashMap<String, IrFunction>();

    private IrFunction currentFunction;
    private Map<Integer, Integer> localSlots;
    private Map<Integer, IrType> vregTypes;
    private Map<String, Label> labels;
    private List<Ast.Stmt.T> stmts;

    public IrToJvmTranslator(IrProgram program) {
        this.program = program;
        for (IrFunction function : program.getFunctions()) {
            this.functionsByName.put(function.getName(), function);
        }
    }

    public Ast.Program.ProgramSingle translate() {
        IrVerifier.verify(program);
        List<Ast.Method.MethodSingle> methods = new ArrayList<Ast.Method.MethodSingle>();
        for (IrFunction function : program.getFunctions()) {
            methods.add(translateFunction(function));
        }
        return new Ast.Program.ProgramSingle(new Ast.MainClass.MainClassSingle(className(), methods));
    }

    private Ast.Method.MethodSingle translateFunction(IrFunction function) {
        this.currentFunction = function;
        this.localSlots = allocateLocalSlots(function);
        this.labels = new HashMap<String, Label>();
        this.stmts = new ArrayList<Ast.Stmt.T>();

        List<Ast.Declare.DeclareSingle> formals = new ArrayList<Ast.Declare.DeclareSingle>();
        for (int i = 0; i < function.getParameterTypes().size(); i++) {
            formals.add(new Ast.Declare.DeclareSingle(toJvmType(function.getParameterTypes().get(i)), "p" + i));
        }

        for (IrBlock block : function.getBlocks()) {
            labelFor(block.getLabel());
            for (IrInstruction instruction : block.getInstructions()) {
                if (instruction.getLabelTarget() != null) {
                    labelFor(instruction.getLabelTarget());
                }
            }
        }

        for (IrBlock block : function.getBlocks()) {
            emit(new Ast.Stmt.LabelJ(labelFor(block.getLabel())));
            for (IrInstruction instruction : block.getInstructions()) {
                translateInstruction(instruction);
            }
        }

        Ast.Type.T returnType = toJvmType(function.getReturnType());
        Ast.Method.MethodSingle method = new Ast.Method.MethodSingle(returnType, function.getName(),
                className(), formals, new ArrayList<Ast.Declare.DeclareSingle>(), stmts, 0, nextLocalSlot());

        this.currentFunction = null;
        this.localSlots = null;
        this.vregTypes = null;
        this.labels = null;
        this.stmts = null;
        return method;
    }

    private void translateInstruction(IrInstruction instruction) {
        switch (instruction.getOpcode()) {
            case MOV:
                loadValue(instruction.getOperands().get(0));
                storeVReg(instruction.getResult());
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
                translateBinary(instruction);
                break;
            case I2F:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.I2f());
                storeVReg(instruction.getResult());
                break;
            case I2D:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.I2d());
                storeVReg(instruction.getResult());
                break;
            case F2D:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.F2d());
                storeVReg(instruction.getResult());
                break;
            case EQ:
            case NE:
            case GT:
            case LT:
            case GE:
            case LE:
                translateCompare(instruction);
                break;
            case NOT:
                translateNot(instruction);
                break;
            case JMP:
                emit(new Ast.Stmt.Goto(labelFor(instruction.getLabelTarget())));
                break;
            case BR_TRUE:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.Ldc(0));
                emit(new Ast.Stmt.Ificmpne(labelFor(instruction.getLabelTarget())));
                break;
            case BR_FALSE:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.Ldc(0));
                emit(new Ast.Stmt.Ificmpeq(labelFor(instruction.getLabelTarget())));
                break;
            case CALL:
                translateCall(instruction);
                break;
            case RET:
                translateReturn(instruction);
                break;
            case NEW_ARR:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.Newarray(toJvmType(elementType(instruction.getType()))));
                storeVReg(instruction.getResult());
                break;
            case ARR_GET:
                loadValue(instruction.getOperands().get(0));
                loadValue(instruction.getOperands().get(1));
                emitArrayLoad(typeOf(instruction.getResult()));
                storeVReg(instruction.getResult());
                break;
            case ARR_SET:
                loadValue(instruction.getOperands().get(0));
                loadValue(instruction.getOperands().get(1));
                loadValue(instruction.getOperands().get(2));
                emitArrayStore(elementType(typeOf(instruction.getOperands().get(0))));
                break;
            case ARR_LEN:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.Arraylength());
                storeVReg(instruction.getResult());
                break;
            case PRINT:
                loadValue(instruction.getOperands().get(0));
                emit(new Ast.Stmt.Printf(printType(typeOf(instruction.getOperands().get(0))), null));
                break;
            case PRINT_NL:
                emit(new Ast.Stmt.Ldc("\"\\n\""));
                emit(new Ast.Stmt.PrintLine());
                break;
            default:
                throw new CompilerException("Unsupported LemonIR opcode for JVM backend: " + instruction.getOpcode());
        }
    }

    private void translateBinary(IrInstruction instruction) {
        IrType type = operationType(instruction);
        loadValue(instruction.getOperands().get(0));
        loadValue(instruction.getOperands().get(1));
        switch (instruction.getOpcode()) {
            case ADD:
                emitAdd(type);
                break;
            case SUB:
                emitSub(type);
                break;
            case MUL:
                emitMul(type);
                break;
            case DIV:
                emitDiv(type);
                break;
            case MOD:
                emit(new Ast.Stmt.Irem());
                break;
            default:
                throw new CompilerException("Internal error: not a binary opcode " + instruction.getOpcode());
        }
        storeVReg(instruction.getResult());
    }

    private void translateCompare(IrInstruction instruction) {
        IrValue left = instruction.getOperands().get(0);
        IrValue right = instruction.getOperands().get(1);
        IrType operandType = promotedCompareType(typeOf(left), typeOf(right));
        Label trueLabel = new Label();

        loadValue(left);
        loadValue(right);
        if (operandType == IrType.FLOAT) {
            emit(usesCompareGreaterOnNaN(instruction.getOpcode()) ? new Ast.Stmt.Fcmpg() : new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Ldc(0));
        } else if (operandType == IrType.DOUBLE) {
            emit(usesCompareGreaterOnNaN(instruction.getOpcode()) ? new Ast.Stmt.Dcmpg() : new Ast.Stmt.Dcmpl());
            emit(new Ast.Stmt.Ldc(0));
        }
        emitComparisonBranch(instruction.getOpcode(), trueLabel);
        emitBooleanResult(trueLabel, instruction.getResult());
    }

    private void translateNot(IrInstruction instruction) {
        Label trueLabel = new Label();
        loadValue(instruction.getOperands().get(0));
        emit(new Ast.Stmt.Ldc(0));
        emit(new Ast.Stmt.Ificmpeq(trueLabel));
        emitBooleanResult(trueLabel, instruction.getResult());
    }

    private void translateCall(IrInstruction instruction) {
        IrFunction target = functionsByName.get(instruction.getFuncTarget());
        List<Ast.Type.T> argumentTypes = new ArrayList<Ast.Type.T>();
        for (int i = 0; i < instruction.getOperands().size(); i++) {
            IrValue operand = instruction.getOperands().get(i);
            loadValue(operand);
            IrType argType = typeOf(operand);
            if (target != null && i < target.getParameterTypes().size()) {
                argType = target.getParameterTypes().get(i);
            }
            argumentTypes.add(toJvmType(argType));
        }

        IrType returnType = instruction.getType();
        if (target != null) {
            returnType = target.getReturnType();
        }
        emit(new Ast.Stmt.Invokestatic(instruction.getFuncTarget(), argumentTypes, toJvmType(returnType)));
        if (instruction.getResult() != null) {
            storeVReg(instruction.getResult());
        } else if (returnType != IrType.VOID) {
            emit(returnType == IrType.DOUBLE ? new Ast.Stmt.Pop2() : new Ast.Stmt.Pop());
        }
    }

    private void translateReturn(IrInstruction instruction) {
        if (instruction.getOperands().isEmpty()) {
            if (currentFunction.getReturnType() != IrType.VOID) {
                throw new CompilerException("Missing return value in function: " + currentFunction.getName());
            }
            emit(new Ast.Stmt.Vreturn());
            return;
        }

        loadValue(instruction.getOperands().get(0));
        IrType returnType = currentFunction.getReturnType();
        if (returnType == IrType.FLOAT) {
            emit(new Ast.Stmt.Freturn());
        } else if (returnType == IrType.DOUBLE) {
            emit(new Ast.Stmt.Dreturn());
        } else if (returnType == IrType.STRING || returnType.isArray()) {
            emit(new Ast.Stmt.Areturn());
        } else {
            emit(new Ast.Stmt.Ireturn());
        }
    }

    private void emitBooleanResult(Label trueLabel, IrValue result) {
        Label endLabel = new Label();
        emit(new Ast.Stmt.Ldc(0));
        emit(new Ast.Stmt.Goto(endLabel));
        emit(new Ast.Stmt.LabelJ(trueLabel));
        emit(new Ast.Stmt.Ldc(1));
        emit(new Ast.Stmt.LabelJ(endLabel));
        storeVReg(result);
    }

    private void emitComparisonBranch(IrOpcode opcode, Label trueLabel) {
        switch (opcode) {
            case EQ:
                emit(new Ast.Stmt.Ificmpeq(trueLabel));
                break;
            case NE:
                emit(new Ast.Stmt.Ificmpne(trueLabel));
                break;
            case GT:
                emit(new Ast.Stmt.Ificmpgt(trueLabel));
                break;
            case LT:
                emit(new Ast.Stmt.Ificmplt(trueLabel));
                break;
            case GE:
                emit(new Ast.Stmt.Ificmpge(trueLabel));
                break;
            case LE:
                emit(new Ast.Stmt.Ificmple(trueLabel));
                break;
            default:
                throw new CompilerException("Unsupported comparison opcode: " + opcode);
        }
    }

    private boolean usesCompareGreaterOnNaN(IrOpcode opcode) {
        return opcode == IrOpcode.LT || opcode == IrOpcode.LE;
    }

    private void loadValue(IrValue value) {
        switch (value.getKind()) {
            case VREG:
                emitLoad(typeOf(value), localSlot(value));
                break;
            case CONST_INT:
                emit(new Ast.Stmt.Ldc(value.getIntValue()));
                break;
            case CONST_FLOAT:
                emit(new Ast.Stmt.Ldc(value.getFloatValue()));
                break;
            case CONST_DOUBLE:
                emit(new Ast.Stmt.Ldc(value.getDoubleValue()));
                break;
            case CONST_BOOL:
                emit(new Ast.Stmt.Ldc(value.getBoolValue() ? 1 : 0));
                break;
            case CONST_STRING:
                emit(new Ast.Stmt.Ldc("\"" + escapeStringLiteralForJasmin(value.getStringValue()) + "\""));
                break;
            default:
                throw new CompilerException("Unsupported LemonIR value kind: " + value.getKind());
        }
    }

    private void storeVReg(IrValue value) {
        emitStore(typeOf(value), localSlot(value));
    }

    private void emitLoad(IrType type, int slot) {
        if (type == IrType.FLOAT) {
            emit(new Ast.Stmt.Fload(slot));
        } else if (type == IrType.DOUBLE) {
            emit(new Ast.Stmt.Dload(slot));
        } else if (type == IrType.STRING || type.isArray()) {
            emit(new Ast.Stmt.Aload(slot));
        } else {
            emit(new Ast.Stmt.Iload(slot));
        }
    }

    private void emitStore(IrType type, int slot) {
        if (type == IrType.FLOAT) {
            emit(new Ast.Stmt.Fstore(slot));
        } else if (type == IrType.DOUBLE) {
            emit(new Ast.Stmt.Dstore(slot));
        } else if (type == IrType.STRING || type.isArray()) {
            emit(new Ast.Stmt.Astore(slot));
        } else {
            emit(new Ast.Stmt.Istore(slot));
        }
    }

    private void emitAdd(IrType type) {
        if (type == IrType.FLOAT) emit(new Ast.Stmt.Fadd());
        else if (type == IrType.DOUBLE) emit(new Ast.Stmt.Dadd());
        else emit(new Ast.Stmt.Iadd());
    }

    private void emitSub(IrType type) {
        if (type == IrType.FLOAT) emit(new Ast.Stmt.Fsub());
        else if (type == IrType.DOUBLE) emit(new Ast.Stmt.Dsub());
        else emit(new Ast.Stmt.Isub());
    }

    private void emitMul(IrType type) {
        if (type == IrType.FLOAT) emit(new Ast.Stmt.Fmul());
        else if (type == IrType.DOUBLE) emit(new Ast.Stmt.Dmul());
        else emit(new Ast.Stmt.Imul());
    }

    private void emitDiv(IrType type) {
        if (type == IrType.FLOAT) emit(new Ast.Stmt.Fdiv());
        else if (type == IrType.DOUBLE) emit(new Ast.Stmt.Ddiv());
        else emit(new Ast.Stmt.Idiv());
    }

    private void emitArrayLoad(IrType elementType) {
        if (elementType == IrType.FLOAT) emit(new Ast.Stmt.Faload());
        else if (elementType == IrType.DOUBLE) emit(new Ast.Stmt.Daload());
        else if (elementType == IrType.BOOL) emit(new Ast.Stmt.Baload());
        else emit(new Ast.Stmt.Iaload());
    }

    private void emitArrayStore(IrType elementType) {
        if (elementType == IrType.FLOAT) emit(new Ast.Stmt.Fastore());
        else if (elementType == IrType.DOUBLE) emit(new Ast.Stmt.Dastore());
        else if (elementType == IrType.BOOL) emit(new Ast.Stmt.Bastore());
        else emit(new Ast.Stmt.Iastore());
    }

    private Map<Integer, Integer> allocateLocalSlots(IrFunction function) {
        this.vregTypes = collectVRegTypes(function);
        Map<Integer, Integer> slots = new LinkedHashMap<Integer, Integer>();
        int nextSlot = 0;

        for (IrValue parameter : function.getParameters()) {
            slots.put(parameter.getId(), nextSlot);
            nextSlot += localSlots(typeOf(parameter));
        }

        for (Map.Entry<Integer, IrType> entry : vregTypes.entrySet()) {
            if (!slots.containsKey(entry.getKey())) {
                slots.put(entry.getKey(), nextSlot);
                nextSlot += localSlots(entry.getValue());
            }
        }
        return slots;
    }

    private Map<Integer, IrType> collectVRegTypes(IrFunction function) {
        Map<Integer, IrType> types = new TreeMap<Integer, IrType>();
        for (IrValue parameter : function.getParameters()) {
            putVRegType(types, parameter);
        }
        for (IrBlock block : function.getBlocks()) {
            for (IrInstruction instruction : block.getInstructions()) {
                putVRegType(types, instruction.getResult());
                for (IrValue operand : instruction.getOperands()) {
                    putVRegType(types, operand);
                }
            }
        }
        return types;
    }

    private void putVRegType(Map<Integer, IrType> types, IrValue value) {
        if (value == null || value.getKind() != IrValue.Kind.VREG) {
            return;
        }
        IrType type = value.getType();
        if (type == null) {
            throw new CompilerException("Missing type for LemonIR vreg v" + value.getId());
        }
        IrType previous = types.get(value.getId());
        if (previous != null && previous != type) {
            throw new CompilerException("Conflicting types for LemonIR vreg v" + value.getId());
        }
        types.put(value.getId(), type);
    }

    private int localSlot(IrValue value) {
        Integer slot = localSlots.get(value.getId());
        if (slot == null) {
            throw new CompilerException("Missing JVM local slot for LemonIR vreg v" + value.getId());
        }
        return slot;
    }

    private IrType typeOf(IrValue value) {
        if (value.getKind() != IrValue.Kind.VREG) {
            return value.getType();
        }
        IrType type = vregTypes.get(value.getId());
        if (type == null) {
            type = value.getType();
        }
        if (type == null) {
            throw new CompilerException("Missing type for LemonIR vreg v" + value.getId());
        }
        return type;
    }

    private IrType operationType(IrInstruction instruction) {
        if (instruction.getType() != null) {
            return instruction.getType();
        }
        if (instruction.getResult() != null) {
            return typeOf(instruction.getResult());
        }
        return typeOf(instruction.getOperands().get(0));
    }

    private IrType promotedCompareType(IrType left, IrType right) {
        if (left == IrType.DOUBLE || right == IrType.DOUBLE) return IrType.DOUBLE;
        if (left == IrType.FLOAT || right == IrType.FLOAT) return IrType.FLOAT;
        return IrType.INT;
    }

    private IrType elementType(IrType type) {
        return type == null ? IrType.INT : type.elementType();
    }

    private int localSlots(IrType type) {
        return type == IrType.DOUBLE ? 2 : 1;
    }

    private int nextLocalSlot() {
        int nextSlot = 0;
        for (Map.Entry<Integer, Integer> entry : localSlots.entrySet()) {
            IrType type = vregTypes.get(entry.getKey());
            int limit = entry.getValue() + localSlots(type);
            if (limit > nextSlot) {
                nextSlot = limit;
            }
        }
        return nextSlot;
    }

    private String className() {
        String className = program.getClassName();
        return className == null || className.length() == 0 ? "LemonProgram" : className;
    }

    private Label labelFor(String name) {
        Label label = labels.get(name);
        if (label == null) {
            label = new Label();
            labels.put(name, label);
        }
        return label;
    }

    private void emit(Ast.Stmt.T stmt) {
        stmts.add(stmt);
    }

    private Ast.Type.T printType(IrType type) {
        if (type == IrType.BOOL) {
            return new Ast.Type.Int();
        }
        return toJvmType(type);
    }

    private Ast.Type.T toJvmType(IrType type) {
        if (type == null) {
            return new Ast.Type.Void();
        }
        switch (type) {
            case INT:
                return new Ast.Type.Int();
            case FLOAT:
                return new Ast.Type.Float();
            case DOUBLE:
                return new Ast.Type.Double();
            case BOOL:
                return new Ast.Type.Bool();
            case STRING:
                return new Ast.Type.Str();
            case INT_ARRAY:
                return new Ast.Type.IntArray();
            case FLOAT_ARRAY:
                return new Ast.Type.FloatArray();
            case DOUBLE_ARRAY:
                return new Ast.Type.DoubleArray();
            case BOOL_ARRAY:
                return new Ast.Type.BoolArray();
            case VOID:
                return new Ast.Type.Void();
            default:
                throw new CompilerException("Unsupported LemonIR type for JVM backend: " + type);
        }
    }

    private String escapeStringLiteralForJasmin(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }
}
