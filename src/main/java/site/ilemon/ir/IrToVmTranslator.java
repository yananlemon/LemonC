package site.ilemon.ir;

import site.ilemon.exception.CompilerException;
import site.ilemon.vm.Instruction;
import site.ilemon.vm.Opcode;
import site.ilemon.vm.Script;
import site.ilemon.vm.Value;
import site.ilemon.vm.VmFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LemonIR 到 LemonVM 字节码的翻译器。
 * 将 IrProgram 转换为可执行的 Script 对象。
 */
public class IrToVmTranslator {

    private IrProgram program;
    private Script script;
    
    private List<Instruction> instrStream = new ArrayList<Instruction>();
    private Map<String, Integer> labelToPc = new HashMap<String, Integer>();
    private List<LabelFixup> fixups = new ArrayList<LabelFixup>();
    private int internalLabelCounter = 0;

    private static class LabelFixup {
        int instrIndex;
        int operandIndex;
        String functionName;
        String labelTarget;

        LabelFixup(int instrIndex, int operandIndex, String functionName, String labelTarget) {
            this.instrIndex = instrIndex;
            this.operandIndex = operandIndex;
            this.functionName = functionName;
            this.labelTarget = labelTarget;
        }
    }

    public IrToVmTranslator(IrProgram program) {
        this.program = program;
        this.script = new Script();
    }

    private Map<String, Integer> funcNameToIndex = new HashMap<String, Integer>();

    public Script translate() {
        IrVerifier.verify(program);
        resetTranslationState();

        VmFunction[] vmFuncs = new VmFunction[program.getFunctions().size()];
        int funcIndex = 0;
        
        for (IrFunction irFunc : program.getFunctions()) {
            funcNameToIndex.put(irFunc.getName(), funcIndex++);
        }
        
        funcIndex = 0;

        for (IrFunction irFunc : program.getFunctions()) {
            this.currentFunction = irFunc;
            int entryPc = instrStream.size();
            int paramCount = irFunc.getParameters().size();
            int totalVRegs = irFunc.getUsedVRegCount();
            int localDataSize = totalVRegs - paramCount;
            if (localDataSize < 0) localDataSize = 0;

            VmFunction vmFunc = new VmFunction(irFunc.getName(), entryPc, paramCount, localDataSize);
            vmFuncs[funcIndex++] = vmFunc;

            for (IrBlock block : irFunc.getBlocks()) {
                labelToPc.put(labelKey(irFunc.getName(), block.getLabel()), instrStream.size());

                for (IrInstruction instr : block.getInstructions()) {
                    translateInstruction(instr);
                }
            }
        }

        script.setFuncTable(vmFuncs);

        for (LabelFixup fixup : fixups) {
            String targetKey = labelKey(fixup.functionName, fixup.labelTarget);
            if (!labelToPc.containsKey(targetKey)) {
                throw new CompilerException("Unresolved label: " + fixup.labelTarget);
            }
            int targetPc = labelToPc.get(targetKey);
            Instruction instr = instrStream.get(fixup.instrIndex);
            instr.getOperands().set(fixup.operandIndex, Value.ofInstrIndex(targetPc));
        }

        Instruction[] instrArray = new Instruction[instrStream.size()];
        instrStream.toArray(instrArray);
        script.setInstrStream(instrArray);
        
        script.setMainFuncName("main");

        return script;
    }

    private void resetTranslationState() {
        this.script = new Script();
        this.instrStream = new ArrayList<Instruction>();
        this.labelToPc = new HashMap<String, Integer>();
        this.fixups = new ArrayList<LabelFixup>();
        this.funcNameToIndex = new HashMap<String, Integer>();
        this.internalLabelCounter = 0;
    }

    private String labelKey(String functionName, String label) {
        return functionName + ":" + label;
    }

    private void addLabelFixup(int instrIndex, int operandIndex, String labelTarget) {
        fixups.add(new LabelFixup(instrIndex, operandIndex, currentFunction.getName(), labelTarget));
    }

    private IrFunction currentFunction;

    private Value mapVReg(int id) {
        int paramCount = currentFunction.getParameters().size();
        int totalVRegs = currentFunction.getUsedVRegCount();
        int localDataSize = totalVRegs - paramCount;
        if (localDataSize < 0) localDataSize = 0;
        
        if (id < paramCount) {
            // 参数
            return Value.ofStackRef(-(localDataSize + 1 + paramCount - id));
        } else {
            // 局部变量
            int localId = id - paramCount;
            return Value.ofStackRef(-(localDataSize - localId));
        }
    }

    private Value mapConst(IrValue val) {
        switch (val.getKind()) {
            case CONST_INT: return Value.ofInt(val.getIntValue());
            case CONST_FLOAT: return Value.ofFloat(val.getFloatValue());
            case CONST_DOUBLE: return Value.ofDouble(val.getDoubleValue());
            case CONST_BOOL: return Value.ofInt(val.getBoolValue() ? 1 : 0);
            case CONST_STRING: return Value.ofString(val.getStringValue());
            default: throw new CompilerException("Unknown const kind: " + val.getKind());
        }
    }

    private Value mapConstOrVReg(IrValue val) {
        if (val.getKind() == IrValue.Kind.VREG) {
            return mapVReg(val.getId());
        }
        return mapConst(val);
    }

    /**
     * 翻译一条 LemonIR 指令，并把它的源码位置盖到本次新产生的所有 VM 指令上。
     *
     * <p>在这里统一盖章，而不是在每个 {@code new Instruction(...)} 处逐个设置：
     * 比较、二元运算、NOT、NEG、CALL 都会展开成多条 VM 指令，逐个设置容易漏。</p>
     */
    private void translateInstruction(IrInstruction instr) {
        int firstIndex = instrStream.size();
        translateInstructionInternal(instr);
        for (int i = firstIndex; i < instrStream.size(); i++) {
            instrStream.get(i).setSourceSpan(instr.getSourceSpan());
        }
    }

    private void translateInstructionInternal(IrInstruction instr) {
        switch (instr.getOpcode()) {
            case MOV: {
                Instruction m = new Instruction(Opcode.MOV);
                m.getOperands().add(mapVReg(instr.getResult().getId()));
                m.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                instrStream.add(m);
                break;
            }
            case ADD: translateBinary(instr, Opcode.ADD); break;
            case SUB: translateBinary(instr, Opcode.SUB); break;
            case MUL: translateBinary(instr, Opcode.MUL); break;
            case DIV: translateBinary(instr, Opcode.DIV); break;
            case MOD: translateBinary(instr, Opcode.MOD); break;
            case I2F: translateConvert(instr, Opcode.I2F); break;
            case I2D: translateConvert(instr, Opcode.I2D); break;
            case F2D: translateConvert(instr, Opcode.F2D); break;
            
            // 比较指令在 VM 中没有直接产生布尔值的对应指令，我们需要展开为跳转+赋值
            case EQ: translateCompare(instr, Opcode.JE); break;
            case NE: translateCompare(instr, Opcode.JNE); break;
            case GT: translateCompare(instr, Opcode.JG); break;
            case LT: translateCompare(instr, Opcode.JL); break;
            case GE: translateCompare(instr, Opcode.JGE); break;
            case LE: translateCompare(instr, Opcode.JLE); break;

            case PRINT: {
                Instruction p = new Instruction(Opcode.PRINT);
                p.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                instrStream.add(p);
                break;
            }
            case PRINT_NL: {
                instrStream.add(new Instruction(Opcode.PRINT_NL));
                break;
            }

            case JMP: {
                Instruction jmp = new Instruction(Opcode.JMP);
                jmp.getOperands().add(Value.ofInstrIndex(-1));
                addLabelFixup(instrStream.size(), 0, instr.getLabelTarget());
                instrStream.add(jmp);
                break;
            }

            case BR_TRUE: {
                Instruction jne = new Instruction(Opcode.JNE);
                jne.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                jne.getOperands().add(Value.ofInt(0)); // if cond != 0 (true)
                jne.getOperands().add(Value.ofInstrIndex(-1));
                addLabelFixup(instrStream.size(), 2, instr.getLabelTarget());
                instrStream.add(jne);
                break;
            }

            case BR_FALSE: {
                Instruction je = new Instruction(Opcode.JE);
                je.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                je.getOperands().add(Value.ofInt(0)); // if cond == 0 (false)
                je.getOperands().add(Value.ofInstrIndex(-1));
                addLabelFixup(instrStream.size(), 2, instr.getLabelTarget());
                instrStream.add(je);
                break;
            }

            case RET: {
                if (!instr.getOperands().isEmpty()) {
                    Instruction movRet = new Instruction(Opcode.MOV);
                    movRet.getOperands().add(Value.ofRetValRef());
                    movRet.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                    instrStream.add(movRet);
                }
                instrStream.add(new Instruction(Opcode.RET));
                break;
            }

            case CALL: {
                for (IrValue arg : instr.getOperands()) {
                    Instruction push = new Instruction(Opcode.PUSH);
                    push.getOperands().add(mapConstOrVReg(arg));
                    instrStream.add(push);
                }
                Instruction call = new Instruction(Opcode.CALL);
                Integer fIndex = funcNameToIndex.get(instr.getFuncTarget());
                if (fIndex == null) {
                    throw new CompilerException("Unknown function: " + instr.getFuncTarget());
                }
                call.getOperands().add(Value.ofFuncIndex(fIndex));
                instrStream.add(call);
                if (instr.getResult() != null) {
                    Instruction mov = new Instruction(Opcode.MOV);
                    mov.getOperands().add(mapVReg(instr.getResult().getId()));
                    mov.getOperands().add(Value.ofRetValRef());
                    instrStream.add(mov);
                }
                break;
            }

            case NEG: {
                Instruction mov = new Instruction(Opcode.MOV);
                mov.getOperands().add(mapVReg(instr.getResult().getId()));
                mov.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                instrStream.add(mov);

                Instruction neg = new Instruction(Opcode.NEG);
                neg.getOperands().add(mapVReg(instr.getResult().getId()));
                instrStream.add(neg);
                break;
            }

            case NOT: {
                Instruction mov = new Instruction(Opcode.MOV);
                mov.getOperands().add(mapVReg(instr.getResult().getId()));
                mov.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                instrStream.add(mov);

                Instruction not = new Instruction(Opcode.NOT);
                not.getOperands().add(mapVReg(instr.getResult().getId()));
                instrStream.add(not);
                break;
            }

            case NEW_ARR: {
                Instruction newArr = new Instruction(Opcode.NEW_ARR);
                newArr.getOperands().add(mapVReg(instr.getResult().getId()));
                newArr.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                // 无条件发射元素类型：IrVerifier 已保证 NEW_ARR 必带数组类型，
                // 而固定元数是 .lbc 能表达这条指令的前提。
                newArr.getOperands().add(Value.ofString(instr.getType().name()));
                instrStream.add(newArr);
                break;
            }

            case ARR_GET: {
                Instruction arrGet = new Instruction(Opcode.ARR_GET);
                arrGet.getOperands().add(mapVReg(instr.getResult().getId()));
                arrGet.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                arrGet.getOperands().add(mapConstOrVReg(instr.getOperands().get(1)));
                instrStream.add(arrGet);
                break;
            }

            case ARR_SET: {
                Instruction arrSet = new Instruction(Opcode.ARR_SET);
                arrSet.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                arrSet.getOperands().add(mapConstOrVReg(instr.getOperands().get(1)));
                arrSet.getOperands().add(mapConstOrVReg(instr.getOperands().get(2)));
                instrStream.add(arrSet);
                break;
            }

            case ARR_LEN: {
                Instruction arrLen = new Instruction(Opcode.ARR_LEN);
                arrLen.getOperands().add(mapVReg(instr.getResult().getId()));
                arrLen.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
                instrStream.add(arrLen);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unsupported IrOpcode: " + instr.getOpcode());
        }
    }

    private void translateBinary(IrInstruction instr, Opcode vmOp) {
        Value dest = mapVReg(instr.getResult().getId());
        
        Instruction mov = new Instruction(Opcode.MOV);
        mov.getOperands().add(dest);
        mov.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
        instrStream.add(mov);

        Instruction op = new Instruction(vmOp);
        op.getOperands().add(dest);
        op.getOperands().add(mapConstOrVReg(instr.getOperands().get(1)));
        instrStream.add(op);
    }

    private void translateConvert(IrInstruction instr, Opcode vmOp) {
        Instruction convert = new Instruction(vmOp);
        convert.getOperands().add(mapVReg(instr.getResult().getId()));
        convert.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
        instrStream.add(convert);
    }

    private void translateCompare(IrInstruction instr, Opcode cmpJumpOp) {
        // v2 = EQ v0, v1
        // -> Mov v2, 1
        // -> cmpJumpOp v0, v1, L_end
        // -> Mov v2, 0
        // -> L_end:
        Value dest = mapVReg(instr.getResult().getId());
        String endLabel = "_internal_cmp_end_" + (internalLabelCounter++);

        Instruction mov1 = new Instruction(Opcode.MOV);
        mov1.getOperands().add(dest);
        mov1.getOperands().add(Value.ofInt(1));
        instrStream.add(mov1);

        Instruction cmpJump = new Instruction(cmpJumpOp);
        cmpJump.getOperands().add(mapConstOrVReg(instr.getOperands().get(0)));
        cmpJump.getOperands().add(mapConstOrVReg(instr.getOperands().get(1)));
        cmpJump.getOperands().add(Value.ofInstrIndex(-1));
        addLabelFixup(instrStream.size(), 2, endLabel);
        instrStream.add(cmpJump);

        Instruction mov0 = new Instruction(Opcode.MOV);
        mov0.getOperands().add(dest);
        mov0.getOperands().add(Value.ofInt(0));
        instrStream.add(mov0);

        labelToPc.put(labelKey(currentFunction.getName(), endLabel), instrStream.size());
    }
}
