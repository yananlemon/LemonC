package site.ilemon.vm;

/**
 * LemonVM 虚拟机 — 解释执行引擎。
 *
 * 核心设计完全参考 XVM (Game Scripting Mastery) 的 XS_RunScripts()：
 * - 主循环 fetch → decode → execute
 * - 二地址指令格式（Dest = Dest op Source）
 * - 类型无关：运行时根据 Value 的类型标签分派
 * - 比较+跳转合一（Je/Jg/Jl 等一条指令完成比较和跳转）
 * - _RetVal 寄存器传递函数返回值
 */
public class LemonVm {

    private final Script script;

    /** 输出缓冲区（Print 指令的输出收集在这里） */
    private final StringBuilder output;

    /** 是否启用调试模式 */
    private boolean debugMode;

    /** 已执行的指令数（防止无限循环） */
    private int instructionCount;

    /** 最大指令执行数量（防止无限循环） */
    private static final int MAX_INSTRUCTIONS = 1_000_000;

    public LemonVm(Script script) {
        this.script = script;
        this.output = new StringBuilder();
        this.debugMode = false;
        this.instructionCount = 0;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * 执行脚本并返回所有 Print 输出。
     */
    public String run() {
        // 初始化：找到 main 函数，设置 PC 和栈帧
        reset();
        // 执行循环
        executeLoop();
        return output.toString();
    }

    /**
     * 重置脚本状态，准备执行。
     * 参考 XVM 的 XS_ResetScript()。
     */
    private void reset() {
        output.setLength(0);
        instructionCount = 0;
        script.resetRuntimeState();

        VmFunction mainFunc = script.getFunc(script.getMainFuncName());
        int mainFuncIndex = script.getFuncIndex(script.getMainFuncName());

        // 分配全局变量空间
        if (script.getGlobalDataSize() > 0) {
            script.getStack().pushFrame(script.getGlobalDataSize());
        }

        // 设置 PC 到 main 的入口点
        script.setPc(mainFunc.getEntryPoint());

        // 为 main 压入栈帧（局部变量 + 1 给栈帧标记）
        // 先 push 一个栈底标记作为返回地址（main 返回时用来终止执行）
        script.getStack().push(Value.ofInstrIndex(-1)); // 哨兵返回地址

        script.getStack().pushFrame(mainFunc.getLocalDataSize() + 1);

        // 在栈帧顶部写入 FrameMarker（标记为 main 函数）
        Value marker = Value.ofStackFrameMarker(mainFuncIndex,
                script.getGlobalDataSize()); // main 之前的 frameIndex
        script.getStack().setValue(
                script.getStack().getFrameIndex(), marker);

        script.setRunning(true);
    }

    /**
     * 主执行循环。
     * 参考 XVM 的 XS_RunScripts() 中的 while(TRUE) 循环。
     */
    private void executeLoop() {
        while (script.isRunning()) {
            // 防止无限循环
            if (instructionCount++ > MAX_INSTRUCTIONS) {
                throw new VmException("超出最大指令执行次数: " + MAX_INSTRUCTIONS);
            }

            int pcBefore = script.getPc();
            Instruction instr = script.getCurrentInstr();

            if (debugMode) {
                printDebugStep(pcBefore, instr);
            }

            // 指令分派
            switch (instr.getOpcode()) {
                // ---- 数据移动 ----
                case MOV:   executeMov(instr);   break;

                // ---- 算术运算 ----
                case ADD:   executeBinaryArith(instr, Opcode.ADD); break;
                case SUB:   executeBinaryArith(instr, Opcode.SUB); break;
                case MUL:   executeBinaryArith(instr, Opcode.MUL); break;
                case DIV:   executeBinaryArith(instr, Opcode.DIV); break;
                case MOD:   executeBinaryArith(instr, Opcode.MOD); break;
                case NEG:   executeNeg(instr);   break;
                case INC:   executeInc(instr);   break;
                case DEC:   executeDec(instr);   break;
                case I2F:   executeConvert(instr, Opcode.I2F); break;
                case I2D:   executeConvert(instr, Opcode.I2D); break;
                case F2D:   executeConvert(instr, Opcode.F2D); break;

                // ---- 逻辑运算 ----
                case NOT:   executeNot(instr);   break;

                // ---- 条件跳转 ----
                case JMP:   executeJmp(instr);   break;
                case JE:    executeConditionalJump(instr, Opcode.JE);  break;
                case JNE:   executeConditionalJump(instr, Opcode.JNE); break;
                case JG:    executeConditionalJump(instr, Opcode.JG);  break;
                case JL:    executeConditionalJump(instr, Opcode.JL);  break;
                case JGE:   executeConditionalJump(instr, Opcode.JGE); break;
                case JLE:   executeConditionalJump(instr, Opcode.JLE); break;

                // ---- 栈操作与函数调用 ----
                case PUSH:  executePush(instr);  break;
                case POP:   executePop(instr);   break;
                case CALL:  executeCall(instr);  break;
                case RET:   executeRet();        break;

                // ---- 数组操作 ----
                case NEW_ARR: executeNewArr(instr); break;
                case ARR_GET: executeArrGet(instr); break;
                case ARR_SET: executeArrSet(instr); break;
                case ARR_LEN: executeArrLen(instr); break;

                // ---- I/O 与控制 ----
                case PRINT:    executePrint(instr);   break;
                case PRINT_NL: executePrintNL();      break;
                case EXIT:     script.setRunning(false); break;

                default:
                    throw new VmException("未知的操作码: " + instr.getOpcode());
            }

            // 如果指令没有修改 PC（不是跳转/调用/返回），则自动递增
            // 参考 XVM: if (iCurrInstr == g_Scripts[...].InstrStream.iCurrInstr) ++...iCurrInstr;
            if (pcBefore == script.getPc()) {
                script.incPc();
            }
        }
    }

    // ========================================
    // 操作数解析 — 参考 XVM 的 ResolveOpValue / ResolveOpPntr
    // ========================================

    /**
     * 解析操作数的值。
     * - 栈索引（INT 类型的负数/正数）→ 从栈中读取
     * - 字面量（INT/FLOAT/DOUBLE/BOOL/STRING）→ 直接返回
     * - _RetVal 寄存器 → 返回 retVal
     *
     * 参考 XVM 的 ResolveOpValue()。
     */
    private Value resolveOpValue(Value operand) {
        switch (operand.type) {
            case INSTR_INDEX:
                // 如果是指令索引，直接返回（用于跳转目标）
                return operand;
            case FUNC_INDEX:
                // 如果是函数索引，直接返回（用于 Call）
                return operand;
            default:
                // 字面量直接返回
                return operand;
        }
    }

    /**
     * 将操作数解析为栈值或字面量。
     *
     * 约定：操作数如果是 INT 类型且在指令上下文中用作栈索引，
     * 则需要通过 resolveAsStackOrLiteral 来判断：
     * - 栈索引操作数使用特殊的 INSTR_INDEX 或直接在指令中标记
     * - 这里简化处理：操作数类型为 INT/FLOAT/DOUBLE/BOOL/STRING 时直接作为字面量
     *
     * 实际的"栈索引 vs 字面量"区分由 .lbc 解析器在加载时完成（见 VmBytecodeParser）。
     * 在运行时，操作数已经被正确设置为对应的 Value 类型。
     */

    // ========================================
    // Mov
    // ========================================

    private void executeMov(Instruction instr) {
        Value dest = instr.getOperand(0);
        Value source = instr.getOperand(1);
        Value sourceVal = resolveSourceValue(source);

        resolveDestAndSet(dest, sourceVal);
    }

    // ========================================
    // 二元算术 — 参考 XVM L1100-L1280
    // ========================================

    private void executeBinaryArith(Instruction instr, Opcode op) {
        Value destOp = instr.getOperand(0);
        Value sourceOp = instr.getOperand(1);

        Value dest = resolveSourceValue(destOp);
        Value source = resolveSourceValue(sourceOp);

        switch (op) {
            case ADD:
                switch (dest.type) {
                    case INT:    dest.intValue += source.coerceToInt(); break;
                    case FLOAT:  dest.floatValue += source.coerceToFloat(); break;
                    case DOUBLE: dest.doubleValue += source.coerceToDouble(); break;
                    default: break;
                }
                break;
            case SUB:
                switch (dest.type) {
                    case INT:    dest.intValue -= source.coerceToInt(); break;
                    case FLOAT:  dest.floatValue -= source.coerceToFloat(); break;
                    case DOUBLE: dest.doubleValue -= source.coerceToDouble(); break;
                    default: break;
                }
                break;
            case MUL:
                switch (dest.type) {
                    case INT:    dest.intValue *= source.coerceToInt(); break;
                    case FLOAT:  dest.floatValue *= source.coerceToFloat(); break;
                    case DOUBLE: dest.doubleValue *= source.coerceToDouble(); break;
                    default: break;
                }
                break;
            case DIV:
                switch (dest.type) {
                    case INT:
                        int divisor = source.coerceToInt();
                        if (divisor == 0) throw new VmException("除零错误");
                        dest.intValue /= divisor;
                        break;
                    case FLOAT:
                        dest.floatValue /= source.coerceToFloat();
                        break;
                    case DOUBLE:
                        dest.doubleValue /= source.coerceToDouble();
                        break;
                    default: break;
                }
                break;
            case MOD:
                // Mod 仅支持整数
                if (dest.type == Value.Type.INT) {
                    int mod = source.coerceToInt();
                    if (mod == 0) throw new VmException("取模除零错误");
                    dest.intValue %= mod;
                }
                break;
            default:
                break;
        }

        resolveDestAndSet(destOp, dest);
    }

    // ========================================
    // 一元运算 — 参考 XVM L1289-L1352
    // ========================================

    private void executeNeg(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value dest = resolveSourceValue(destOp);
        switch (dest.type) {
            case INT:    dest.intValue = -dest.intValue; break;
            case FLOAT:  dest.floatValue = -dest.floatValue; break;
            case DOUBLE: dest.doubleValue = -dest.doubleValue; break;
            default: break;
        }
        resolveDestAndSet(destOp, dest);
    }

    private void executeInc(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value dest = resolveSourceValue(destOp);
        switch (dest.type) {
            case INT:    dest.intValue++; break;
            case FLOAT:  dest.floatValue++; break;
            case DOUBLE: dest.doubleValue++; break;
            default: break;
        }
        resolveDestAndSet(destOp, dest);
    }

    private void executeDec(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value dest = resolveSourceValue(destOp);
        switch (dest.type) {
            case INT:    dest.intValue--; break;
            case FLOAT:  dest.floatValue--; break;
            case DOUBLE: dest.doubleValue--; break;
            default: break;
        }
        resolveDestAndSet(destOp, dest);
    }

    private void executeConvert(Instruction instr, Opcode op) {
        Value destOp = instr.getOperand(0);
        Value source = resolveSourceValue(instr.getOperand(1));
        Value converted;

        switch (op) {
            case I2F:
                converted = Value.ofFloat((float) source.coerceToInt());
                break;
            case I2D:
                converted = Value.ofDouble((double) source.coerceToInt());
                break;
            case F2D:
                converted = Value.ofDouble((double) source.coerceToFloat());
                break;
            default:
                throw new VmException("不支持的类型转换指令: " + op);
        }

        resolveDestAndSet(destOp, converted);
    }

    private void executeNot(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value dest = resolveSourceValue(destOp);
        switch (dest.type) {
            case INT:  dest.intValue = dest.intValue == 0 ? 1 : 0; break;
            case BOOL: dest.boolValue = !dest.boolValue; break;
            default: break;
        }
        resolveDestAndSet(destOp, dest);
    }

    // ========================================
    // 跳转 — 参考 XVM L1476-L1626
    // ========================================

    private void executeJmp(Instruction instr) {
        int target = resolveJumpTarget(instr.getOperand(0));
        script.setPc(target);
    }

    /**
     * 条件跳转：比较 Op0 和 Op1，若条件成立则跳转到 Target。
     * 参考 XVM 的 JE/JNE/JG/JL/JGE/JLE 实现。
     */
    private void executeConditionalJump(Instruction instr, Opcode op) {
        Value op0 = resolveSourceValue(instr.getOperand(0));
        Value op1 = resolveSourceValue(instr.getOperand(1));
        int target = resolveJumpTarget(instr.getOperand(2));

        Boolean nanJump = compareNaN(op0, op1, op);
        boolean jump = nanJump != null ? nanJump.booleanValue() : false;

        if (nanJump == null) {
            switch (op) {
                case JE:  jump = compareValues(op0, op1) == 0; break;
                case JNE: jump = compareValues(op0, op1) != 0; break;
                case JG:  jump = compareValues(op0, op1) > 0;  break;
                case JL:  jump = compareValues(op0, op1) < 0;  break;
                case JGE: jump = compareValues(op0, op1) >= 0; break;
                case JLE: jump = compareValues(op0, op1) <= 0; break;
                default: break;
            }
        }

        if (jump) {
            script.setPc(target);
        }
    }

    /**
     * 比较两个 Value，返回 <0, 0, >0。
     * 根据 Op0 的类型分派比较逻辑。
     */
    private int compareValues(Value a, Value b) {
        switch (a.type) {
            case INT:    return Integer.compare(a.intValue, b.coerceToInt());
            case FLOAT:  return compareFloating(a.floatValue, b.coerceToFloat());
            case DOUBLE: return compareFloating(a.doubleValue, b.coerceToDouble());
            case BOOL:
                int ai = a.boolValue ? 1 : 0;
                int bi = b.boolValue ? 1 : 0;
                return Integer.compare(ai, bi);
            case STRING:
                return a.stringValue.compareTo(b.stringValue != null ? b.stringValue : "");
            default:
                return 0;
        }
    }

    private int compareFloating(double left, double right) {
        if (left < right) {
            return -1;
        }
        if (left > right) {
            return 1;
        }
        return 0;
    }

    private Boolean compareNaN(Value a, Value b, Opcode op) {
        if (!isFloatingValue(a) && !isFloatingValue(b)) {
            return null;
        }

        double left = a.coerceToDouble();
        double right = b.coerceToDouble();
        if (!Double.isNaN(left) && !Double.isNaN(right)) {
            return null;
        }

        switch (op) {
            case JE:
                return Boolean.FALSE;
            case JNE:
                return Boolean.TRUE;
            case JG:
            case JL:
            case JGE:
            case JLE:
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private boolean isFloatingValue(Value value) {
        return value.type == Value.Type.FLOAT || value.type == Value.Type.DOUBLE;
    }

    // ========================================
    // 栈操作与函数调用 — 参考 XVM L1630-L1710
    // ========================================

    private void executePush(Instruction instr) {
        Value source = resolveSourceValue(instr.getOperand(0));
        script.getStack().push(source);
    }

    private void executePop(Instruction instr) {
        Value popped = script.getStack().pop();
        resolveDestAndSet(instr.getOperand(0), popped);
    }

    /**
     * 调用函数。
     * 参考 XVM 的 INSTR_CALL + CallFunc()。
     *
     * 调用约定：
     * 1. 调用者已经用 Push 压入了所有参数
     * 2. Call 指令保存返回地址、创建新栈帧
     * 3. PC 跳转到函数入口点
     */
    private void executeCall(Instruction instr) {
        int funcIndex = resolveFuncIndex(instr.getOperand(0));
        VmFunction func = script.getFunc(funcIndex);

        // 保存当前 FrameIndex
        int oldFrameIndex = script.getStack().getFrameIndex();

        // 推进 PC 到下一条指令（返回时恢复到这里）
        int returnAddr = script.getPc() + 1;

        // Push 返回地址
        script.getStack().push(Value.ofInstrIndex(returnAddr));

        // PushFrame: 局部变量空间 + 1 (栈帧标记)
        script.getStack().pushFrame(func.getLocalDataSize() + 1);

        // 在栈帧顶部写入 FrameMarker
        Value marker = Value.ofStackFrameMarker(funcIndex, oldFrameIndex);
        script.getStack().setValue(
                script.getStack().getFrameIndex(), marker);

        // 跳转到函数入口点
        script.setPc(func.getEntryPoint());
    }

    /**
     * 从函数返回。
     * 参考 XVM 的 INSTR_RET。
     *
     * 1. 从栈帧顶部读取 FrameMarker（函数索引 + 旧 FrameIndex）
     * 2. 从栈帧底部读取返回地址
     * 3. PopFrame
     * 4. 恢复 FrameIndex
     * 5. PC = 返回地址
     */
    private void executeRet() {
        // 1. 从栈帧顶部读取 FrameMarker
        Value frameMarker = script.getStack().getValue(
                script.getStack().getFrameIndex());

        // 如果是栈底标记（main 的返回），终止执行
        if (frameMarker.type != Value.Type.STACK_FRAME_MARKER) {
            script.setRunning(false);
            return;
        }

        int funcIndex = frameMarker.funcIndex;
        int oldFrameIndex = frameMarker.oldFrameIndex;

        VmFunction func = script.getFunc(funcIndex);

        // 2. 读取返回地址（在局部变量区之前）
        int retAddrStackIndex = script.getStack().getFrameIndex()
                - func.getLocalDataSize()     // 局部变量
                - 1;                          // 返回地址本身
        Value returnAddr = script.getStack().getValue(retAddrStackIndex);

        // 检查是否是 main 函数的哨兵返回地址
        if (returnAddr.instrIndex < 0) {
            script.setRunning(false);
            return;
        }

        // 3. PopFrame（局部变量 + 1 FrameMarker + 1 返回地址 + 参数）
        int totalFrameSize = func.getLocalDataSize() + 1 + 1 + func.getParamCount();
        script.getStack().popFrame(totalFrameSize);

        // 4. 恢复 FrameIndex
        script.getStack().setFrameIndex(oldFrameIndex);

        // 5. 跳转到返回地址
        script.setPc(returnAddr.instrIndex);
    }

    // ========================================
    // 数组操作
    // ========================================

    private void executeNewArr(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value sizeOp = instr.getOperand(1);
        int size = resolveSourceValue(sizeOp).coerceToInt();
        Value.Type elementType = Value.Type.NULL;
        if (instr.getOperandCount() > 2) {
            elementType = parseArrayElementType(resolveSourceValue(instr.getOperand(2)).stringValue);
        }
        int ref = script.getHeap().allocate(size, elementType);
        resolveDestAndSet(destOp, Value.ofArrayRef(ref));
    }

    private void executeArrGet(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value arrOp = instr.getOperand(1);
        Value indexOp = instr.getOperand(2);

        int ref = resolveSourceValue(arrOp).arrayRef;
        int index = resolveSourceValue(indexOp).coerceToInt();
        Value element = script.getHeap().load(ref, index);
        resolveDestAndSet(destOp, element);
    }

    private void executeArrSet(Instruction instr) {
        Value arrOp = instr.getOperand(0);
        Value indexOp = instr.getOperand(1);
        Value sourceOp = instr.getOperand(2);

        int ref = resolveSourceValue(arrOp).arrayRef;
        int index = resolveSourceValue(indexOp).coerceToInt();
        Value val = resolveSourceValue(sourceOp);
        script.getHeap().store(ref, index, val);
    }

    private void executeArrLen(Instruction instr) {
        Value destOp = instr.getOperand(0);
        Value arrOp = instr.getOperand(1);

        int ref = resolveSourceValue(arrOp).arrayRef;
        int length = script.getHeap().length(ref);
        resolveDestAndSet(destOp, Value.ofInt(length));
    }

    private Value.Type parseArrayElementType(String typeName) {
        if (typeName == null) {
            return Value.Type.NULL;
        }
        if ("INT_ARRAY".equals(typeName) || "INT".equals(typeName)) {
            return Value.Type.INT;
        }
        if ("FLOAT_ARRAY".equals(typeName) || "FLOAT".equals(typeName)) {
            return Value.Type.FLOAT;
        }
        if ("DOUBLE_ARRAY".equals(typeName) || "DOUBLE".equals(typeName)) {
            return Value.Type.DOUBLE;
        }
        if ("BOOL_ARRAY".equals(typeName) || "BOOL".equals(typeName)) {
            return Value.Type.BOOL;
        }
        return Value.Type.NULL;
    }

    // ========================================
    // I/O
    // ========================================

    private void executePrint(Instruction instr) {
        Value val = resolveSourceValue(instr.getOperand(0));
        switch (val.type) {
            case INT:    output.append(val.intValue); break;
            case FLOAT:  output.append(val.floatValue); break;
            case DOUBLE: output.append(val.doubleValue); break;
            case BOOL:   output.append(val.boolValue ? 1 : 0); break;
            case STRING: output.append(val.stringValue); break;
            default:     output.append("null"); break;
        }
    }

    private void executePrintNL() {
        output.append("\n");
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 解析源操作数的值：
     * - 如果操作数标记为栈索引类型（INT 且标记了 isStackIndex），从栈读取
     * - 如果是 _RetVal 引用，返回 retVal
     * - 否则作为字面量直接返回
     *
     * 注意：为了区分"整数字面量 42" 和 "栈索引 -2"，
     * VmBytecodeParser 在加载时会使用不同的 Value 子类型来标记。
     * 这里我们约定：
     * - 栈索引操作数用 INSTR_INDEX 类型存储（值为栈索引），在解析器中被标记
     *
     * 实际上更清晰的设计是引入专门的 OperandType（类似 XVM 的 OP_TYPE_ABS_STACK_INDEX
     * 和 OP_TYPE_REL_STACK_INDEX），但为了保持简单，我们使用一个标记字段。
     *
     * **简化方案**：Value 增加一个 isStackRef 标记。当 isStackRef=true 时，
     * intValue 作为栈索引处理；否则作为字面量处理。
     */
    private Value resolveSourceValue(Value operand) {
        if (operand.isStackRef) {
            // 从栈中读取
            return script.getStack().getValue(operand.intValue).copy();
        }
        if (operand.isRetValRef) {
            return script.getRetVal().copy();
        }
        // 字面量，直接返回副本
        return operand.copy();
    }

    /**
     * 将值写入目标操作数指向的位置。
     */
    private void resolveDestAndSet(Value destOp, Value value) {
        if (destOp.isStackRef) {
            script.getStack().setValue(destOp.intValue, value);
        } else if (destOp.isRetValRef) {
            script.setRetVal(value);
        } else {
            throw new VmException("目标操作数不可写: " + destOp);
        }
    }

    /**
     * 解析跳转目标（指令索引）。
     */
    private int resolveJumpTarget(Value operand) {
        if (operand.type == Value.Type.INSTR_INDEX) {
            return operand.instrIndex;
        }
        // 也可能通过栈索引间接获取
        if (operand.isStackRef) {
            return resolveSourceValue(operand).instrIndex;
        }
        return operand.instrIndex;
    }

    /**
     * 解析函数索引。
     */
    private int resolveFuncIndex(Value operand) {
        if (operand.type == Value.Type.FUNC_INDEX) {
            return operand.funcIndex;
        }
        throw new VmException("操作数不是函数索引: " + operand);
    }

    // ========================================
    // 调试
    // ========================================

    private void printDebugStep(int pc, Instruction instr) {
        System.out.printf("[Step %3d] PC=%3d %-20s | %s%n",
                instructionCount, pc, instr, script.getStack().dump());
    }
}
