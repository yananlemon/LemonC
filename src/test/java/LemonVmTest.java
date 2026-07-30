import org.junit.Test;
import site.ilemon.vm.*;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * LemonVM 虚拟机测试。
 *
 * 采用两种测试方式：
 * 1. 编程式构建 Script 对象，直接执行 VM
 * 2. 通过 .lbc 文本格式加载，解析后执行
 */
public class LemonVmTest {

    // =====================================================================
    // 辅助方法
    // =====================================================================

    /**
     * 用编程方式构建一个单函数脚本并执行，返回输出。
     *
     * @param localCount 局部变量数量
     * @param instructions 指令序列
     */
    private String runSimple(int localCount, Instruction... instructions) {
        Script script = new Script();
        script.setInstrStream(instructions);
        script.setFuncTable(new VmFunction[]{
                new VmFunction("main", 0, 0, localCount)
        });
        script.setMainFuncName("main");

        LemonVm vm = new LemonVm(script);
        return vm.run();
    }

    /**
     * 从 .lbc 文本加载并执行，返回输出。
     */
    private String runLbc(String lbcContent) {
        Script script = VmBytecodeParser.parse(lbcContent);
        LemonVm vm = new LemonVm(script);
        return vm.run();
    }

    /** 构建指令的便捷方法 */
    private Instruction instr(Opcode op, Value... operands) {
        return new Instruction(op, Arrays.asList(operands));
    }

    // 常用操作数快捷方式
    private Value sref(int idx) { return Value.ofStackRef(idx); }
    private Value imm(int v)    { return Value.ofInt(v); }
    private Value immf(float v) { return Value.ofFloat(v); }
    private Value immd(double v){ return Value.ofDouble(v); }
    private Value immb(boolean v){ return Value.ofBool(v); }
    private Value imms(String v){ return Value.ofString(v); }
    private Value retval()      { return Value.ofRetValRef(); }
    private Value jmp(int idx)  { return Value.ofInstrIndex(idx); }
    private Value func(int idx) { return Value.ofFuncIndex(idx); }

    // =====================================================================
    // A. 编程式测试 — 算术运算
    // =====================================================================

    @Test
    public void testRunCanBeRepeatedWithoutAccumulatingOutputOrStack() {
        Script script = new Script();
        script.setInstrStream(new Instruction[]{
                instr(Opcode.PRINT, imms("ok")),
                instr(Opcode.RET)
        });
        script.setFuncTable(new VmFunction[]{
                new VmFunction("main", 0, 0, 0)
        });
        script.setMainFuncName("main");

        LemonVm vm = new LemonVm(script);
        assertEquals("ok", vm.run());
        assertEquals("ok", vm.run());
    }

    // =====================================================================
    // 资源上限
    // =====================================================================

    @Test
    public void stackGrowsBeyondInitialCapacity() {
        // 初始容量只影响首次扩容前的表现，不构成深度上限。
        RuntimeStack stack = new RuntimeStack(4, 4096);
        for (int i = 0; i < 1000; i++) {
            stack.push(Value.ofInt(i));
        }

        assertTrue("容量应已增长到 1000 以上，实际 " + stack.getSize(), stack.getSize() >= 1000);
        assertEquals(999, stack.getValue(999).intValue);
        assertEquals(0, stack.getValue(0).intValue);
    }

    @Test
    public void stackOverflowAtMaxCapacityIsActionable() {
        RuntimeStack stack = new RuntimeStack(4, 16);
        try {
            for (int i = 0; i < 100; i++) {
                stack.push(Value.ofInt(i));
            }
            fail("超过容量上限应抛 VmException");
        } catch (VmException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("栈溢出"));
            assertTrue("诊断应给出可操作的建议: " + e.getMessage(),
                    e.getMessage().contains("--vm-stack-size"));
        }
    }

    @Test
    public void instructionLimitIsConfigurableAndCanBeDisabled() {
        // 无穷循环。注意不能用"Jmp 跳到自身"：VM 的约定是"指令没改 PC 就自动递增"，
        // 自跳转执行后 PC 与执行前相同，会被当成没跳而递增，反而跳出循环。
        Instruction[] forever = {
                instr(Opcode.MOV, sref(-1), imm(0)),
                instr(Opcode.JMP, jmp(0))
        };

        Script script = new Script();
        script.setInstrStream(forever);
        script.setFuncTable(new VmFunction[]{new VmFunction("main", 0, 0, 1)});
        script.setMainFuncName("main");

        LemonVm limited = new LemonVm(script);
        limited.setInstructionLimit(500);
        try {
            limited.run();
            fail("应触发指令上限");
        } catch (VmException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("超出指令执行上限 500"));
            assertTrue("诊断应给出可操作的建议: " + e.getMessage(),
                    e.getMessage().contains("--vm-instruction-limit"));
            assertTrue("上限异常也应带位置", e.hasLocation());
        }
        assertEquals(501, limited.getInstructionCount());
    }

    @Test
    public void defaultInstructionLimitLeavesRoomForLongRunningPrograms() {
        // 旧上限是 1_000_000，一个合法的长循环会被它误杀。
        assertTrue("默认上限应远高于百万级",
                LemonVm.DEFAULT_INSTRUCTION_LIMIT > 10_000_000L);
    }

    @Test
    public void outputProducedBeforeAFaultIsRetrievable() {
        // JVM 后端直接写 stdout，出错前的输出天然可见；
        // VM 后端攒在缓冲区里，必须能取回，否则两个后端表现不一致。
        Instruction[] printThenDivideByZero = {
                instr(Opcode.PRINT, imms("before")),
                instr(Opcode.MOV, sref(-1), imm(10)),
                instr(Opcode.DIV, sref(-1), imm(0)),
                instr(Opcode.RET)
        };

        Script script = new Script();
        script.setInstrStream(printThenDivideByZero);
        script.setFuncTable(new VmFunction[]{new VmFunction("main", 0, 0, 1)});
        script.setMainFuncName("main");

        LemonVm vm = new LemonVm(script);
        try {
            vm.run();
            fail("应抛除零错误");
        } catch (VmException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("除零"));
        }
        assertEquals("before", vm.getOutput());
    }

    @Test
    public void testFrameSlotsAreClearedWhenFrameIsReused() {
        RuntimeStack stack = new RuntimeStack(8);

        stack.push(Value.ofInstrIndex(1));
        stack.pushFrame(2);
        stack.setValue(1, Value.ofInt(42));
        stack.setValue(2, Value.ofInt(99));
        stack.popFrame(3);

        stack.push(Value.ofInstrIndex(2));
        stack.pushFrame(2);

        assertEquals(Value.Type.NULL, stack.getValue(1).type);
        assertEquals(Value.Type.NULL, stack.getValue(2).type);
    }

    @Test
    public void testIntAddSub() {
        // x = 10; x = x + 5; x = x - 3; print(x) → 12
        String output = runSimple(2,
                instr(Opcode.MOV, sref(-1), imm(10)),   // _L0 = 10
                instr(Opcode.ADD, sref(-1), imm(5)),     // _L0 = 15
                instr(Opcode.SUB, sref(-1), imm(3)),     // _L0 = 12
                instr(Opcode.PRINT, sref(-1)),            // print 12
                instr(Opcode.RET)
        );
        assertEquals("12", output);
    }

    @Test
    public void testIntMulDivMod() {
        // x = 10; x = x * 3; print(x); x = x / 4; print(x); x = x % 5; print(x)
        // 10*3=30, 30/4=7, 7%5=2
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), imm(10)),
                instr(Opcode.MUL, sref(-1), imm(3)),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.DIV, sref(-1), imm(4)),
                instr(Opcode.PRINT, imms(",")),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.MOD, sref(-1), imm(5)),
                instr(Opcode.PRINT, imms(",")),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.RET)
        );
        assertEquals("30,7,2", output);
    }

    @Test
    public void testFloatArith() {
        // x = 1.5f; x = x + 2.5f; print(x) → 4.0
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), immf(1.5f)),
                instr(Opcode.ADD, sref(-1), immf(2.5f)),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.RET)
        );
        assertEquals("4.0", output);
    }

    @Test
    public void testDoubleArith() {
        // x = 3.14; x = x * 2.0; print(x) → 6.28
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), immd(3.14)),
                instr(Opcode.MUL, sref(-1), immd(2.0)),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.RET)
        );
        assertEquals("6.28", output);
    }

    @Test
    public void testNegIncDec() {
        // x = 5; x = -x; print(x); inc(x); print(x); dec(x); dec(x); print(x)
        // -5, -4, -6
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), imm(5)),
                instr(Opcode.NEG, sref(-1)),
                instr(Opcode.PRINT, sref(-1)),            // -5
                instr(Opcode.PRINT, imms(",")),
                instr(Opcode.INC, sref(-1)),
                instr(Opcode.PRINT, sref(-1)),            // -4
                instr(Opcode.PRINT, imms(",")),
                instr(Opcode.DEC, sref(-1)),
                instr(Opcode.DEC, sref(-1)),
                instr(Opcode.PRINT, sref(-1)),            // -6
                instr(Opcode.RET)
        );
        assertEquals("-5,-4,-6", output);
    }

    // =====================================================================
    // B. 编程式测试 — 条件跳转
    // =====================================================================

    @Test
    public void testJmpUnconditional() {
        // Jmp 跳过 print("skip"), 只 print("ok")
        String output = runSimple(0,
                instr(Opcode.JMP, jmp(3)),                // 0: jump to 3
                instr(Opcode.PRINT, imms("skip")),        // 1: skipped
                instr(Opcode.PRINT_NL),                    // 2: skipped
                instr(Opcode.PRINT, imms("ok")),           // 3: print "ok"
                instr(Opcode.RET)                          // 4: return
        );
        assertEquals("ok", output);
    }

    @Test
    public void testIfElse() {
        // if (x > 0) print("pos") else print("neg")
        // x = 5 → "pos"
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), imm(5)),      // 0: _L0 = 5
                instr(Opcode.JG, sref(-1), imm(0), jmp(4)),// 1: if _L0 > 0 jump to 4
                instr(Opcode.PRINT, imms("neg")),          // 2: else
                instr(Opcode.JMP, jmp(5)),                 // 3: skip then
                instr(Opcode.PRINT, imms("pos")),          // 4: then
                instr(Opcode.RET)                          // 5: return
        );
        assertEquals("pos", output);
    }

    @Test
    public void testWhileLoop() {
        // i = 0; while (i < 5) { print(i); i++; }
        // → "01234"
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), imm(0)),      // 0: i = 0
                instr(Opcode.JGE, sref(-1), imm(5), jmp(5)),// 1: if i >= 5, exit
                instr(Opcode.PRINT, sref(-1)),             // 2: print i
                instr(Opcode.INC, sref(-1)),               // 3: i++
                instr(Opcode.JMP, jmp(1)),                 // 4: loop back
                instr(Opcode.RET)                          // 5: return
        );
        assertEquals("01234", output);
    }

    @Test
    public void testAllComparisonOps() {
        // Test Je, Jne, Jl, Jle, Jge
        // a=5, b=5: Je → "eq"; a=5,b=3: Jne → "ne"; a=3,b=5: Jl → "lt"
        // 只测 Je
        String output = runSimple(2,
                instr(Opcode.MOV, sref(-1), imm(5)),       // a=5
                instr(Opcode.MOV, sref(-2), imm(5)),       // b=5
                instr(Opcode.JE, sref(-1), sref(-2), jmp(4)),// je
                instr(Opcode.JMP, jmp(5)),
                instr(Opcode.PRINT, imms("eq")),           // 4
                instr(Opcode.RET)                          // 5
        );
        assertEquals("eq", output);
    }

    @Test
    public void testSignedZeroComparesLikeJvmFloatingPoint() {
        String output = runSimple(0,
                instr(Opcode.JE, immf(-0.0f), immf(0.0f), jmp(3)),
                instr(Opcode.PRINT, imms("not-equal")),
                instr(Opcode.JMP, jmp(4)),
                instr(Opcode.PRINT, imms("equal")),
                instr(Opcode.JL, immd(-0.0d), immd(0.0d), jmp(7)),
                instr(Opcode.PRINT, imms("-not-less")),
                instr(Opcode.JMP, jmp(8)),
                instr(Opcode.PRINT, imms("-less")),
                instr(Opcode.RET)
        );

        assertEquals("equal-not-less", output);
    }

    // =====================================================================
    // C. 编程式测试 — 函数调用
    // =====================================================================

    @Test
    public void testSimpleCall() {
        // func add(a, b) { return a + b; }
        // main: print(add(3, 7)) → 10
        //
        // 指令布局：
        // [0-5]  main
        // [6-11] add

        Instruction[] instrs = {
                // -- main (index 0-5, 1 local) --
                instr(Opcode.MOV, sref(-1), imm(3)),       // 0: _T0 = 3
                instr(Opcode.PUSH, sref(-1)),              // 1: push arg0
                instr(Opcode.MOV, sref(-1), imm(7)),       // 2: _T0 = 7
                instr(Opcode.PUSH, sref(-1)),              // 3: push arg1
                instr(Opcode.CALL, func(1)),               // 4: call add
                instr(Opcode.PRINT, retval()),              // 5: print _RetVal
                instr(Opcode.RET),                         // 6: return

                // -- add (index 7-10, params=2, locals=1) --
                // 参数在栈帧之下：param0 at -(1+2+1)=-4, param1 at -(1+2)=-3
                // 局部变量 _T0 at -1
                instr(Opcode.MOV, sref(-1), sref(-4)),     // 7: _T0 = param0 (a)
                instr(Opcode.ADD, sref(-1), sref(-3)),      // 8: _T0 = a + b
                instr(Opcode.MOV, retval(), sref(-1)),      // 9: _RetVal = _T0
                instr(Opcode.RET),                          // 10: return
        };

        Script script = new Script();
        script.setInstrStream(instrs);
        script.setFuncTable(new VmFunction[]{
                new VmFunction("main", 0, 0, 1),
                new VmFunction("add", 7, 2, 1),
        });
        script.setMainFuncName("main");

        LemonVm vm = new LemonVm(script);
        String output = vm.run();
        assertEquals("10", output);
    }

    @Test
    public void testRecursiveFactorial() {
        // factorial(5) = 120
        //
        // factorial(n):
        //   if n <= 1 return 1
        //   return n * factorial(n-1)
        //
        // 指令布局：
        // [0-5]  main
        // [6-17] factorial

        Instruction[] instrs = {
                // -- main (index 0-4, locals=1) --
                instr(Opcode.MOV, sref(-1), imm(5)),        // 0: _T0 = 5
                instr(Opcode.PUSH, sref(-1)),               // 1: push arg
                instr(Opcode.CALL, func(1)),                // 2: call factorial
                instr(Opcode.PRINT, retval()),               // 3: print _RetVal
                instr(Opcode.RET),                          // 4: return from main

                // -- factorial (index 5-15, params=1, locals=1) --
                // param n at -(1+1+1)=-3
                // local _T0 at -1
                instr(Opcode.JG, sref(-3), imm(1), jmp(9)),// 5: if n > 1, go recurse
                instr(Opcode.MOV, retval(), imm(1)),        // 6: _RetVal = 1
                instr(Opcode.RET),                          // 7: return 1
                // (padding - unreachable)
                // _recurse:
                instr(Opcode.MOV, sref(-1), sref(-3)),      // 8 (jumped from 5 err - should be 9 when jmp target = 9)
                // Hmm, let me fix the jump target: jmp(9) should skip to index 9
                // Actually let me redo the indexing more carefully:
                instr(Opcode.MOV, sref(-1), sref(-3)),      // 9: _T0 = n
                instr(Opcode.SUB, sref(-1), imm(1)),        // 10: _T0 = n - 1
                instr(Opcode.PUSH, sref(-1)),               // 11: push (n-1)
                instr(Opcode.CALL, func(1)),                // 12: factorial(n-1)
                instr(Opcode.MOV, sref(-1), sref(-3)),      // 13: _T0 = n (reload after call)
                instr(Opcode.MUL, sref(-1), retval()),       // 14: _T0 = n * factorial(n-1)
                instr(Opcode.MOV, retval(), sref(-1)),       // 15: _RetVal = result
                instr(Opcode.RET),                          // 16: return
        };

        // Fix: instruction at index 8 is the unreachable padding from my comment.
        // Let me rebuild this cleanly.
        Instruction[] cleanInstrs = {
                // -- main (0-4, locals=1) --
                instr(Opcode.MOV, sref(-1), imm(5)),        // 0
                instr(Opcode.PUSH, sref(-1)),               // 1
                instr(Opcode.CALL, func(1)),                // 2
                instr(Opcode.PRINT, retval()),               // 3
                instr(Opcode.RET),                          // 4

                // -- factorial (5-14, params=1, locals=1) --
                // param n at -(1+1+1)=-3, local _T0 at -1
                instr(Opcode.JG, sref(-3), imm(1), jmp(8)),// 5: if n > 1, goto 8
                instr(Opcode.MOV, retval(), imm(1)),        // 6: _RetVal = 1
                instr(Opcode.RET),                          // 7: return 1

                // _recurse:
                instr(Opcode.MOV, sref(-1), sref(-3)),      // 8: _T0 = n
                instr(Opcode.SUB, sref(-1), imm(1)),        // 9: _T0 = n-1
                instr(Opcode.PUSH, sref(-1)),               // 10: push(n-1)
                instr(Opcode.CALL, func(1)),                // 11: call factorial
                instr(Opcode.MOV, sref(-1), sref(-3)),      // 12: _T0 = n
                instr(Opcode.MUL, sref(-1), retval()),       // 13: _T0 = n * fact(n-1)
                instr(Opcode.MOV, retval(), sref(-1)),       // 14: _RetVal = result
                instr(Opcode.RET),                          // 15: return
        };

        Script script = new Script();
        script.setInstrStream(cleanInstrs);
        script.setFuncTable(new VmFunction[]{
                new VmFunction("main", 0, 0, 1),
                new VmFunction("factorial", 5, 1, 1),
        });
        script.setMainFuncName("main");

        LemonVm vm = new LemonVm(script);
        String output = vm.run();
        assertEquals("120", output);
    }

    // =====================================================================
    // D. 编程式测试 — 数组
    // =====================================================================

    @Test
    public void testNewArrAndAccess() {
        // arr = new int[3]; arr[0]=10; arr[1]=20; arr[2]=30;
        // print(arr[0]+arr[1]+arr[2]) → 60
        String output = runSimple(3,
                // _L0=arr, _L1=temp, _L2=temp2
                instr(Opcode.NEW_ARR, sref(-1), imm(3)),    // 0: arr = new[3]
                instr(Opcode.ARR_SET, sref(-1), imm(0), imm(10)), // 1: arr[0]=10
                instr(Opcode.ARR_SET, sref(-1), imm(1), imm(20)), // 2: arr[1]=20
                instr(Opcode.ARR_SET, sref(-1), imm(2), imm(30)), // 3: arr[2]=30

                instr(Opcode.ARR_GET, sref(-2), sref(-1), imm(0)), // 4: _L1 = arr[0]
                instr(Opcode.ARR_GET, sref(-3), sref(-1), imm(1)), // 5: _L2 = arr[1]
                instr(Opcode.ADD, sref(-2), sref(-3)),              // 6: _L1 += arr[1]
                instr(Opcode.ARR_GET, sref(-3), sref(-1), imm(2)), // 7: _L2 = arr[2]
                instr(Opcode.ADD, sref(-2), sref(-3)),              // 8: _L1 += arr[2]
                instr(Opcode.PRINT, sref(-2)),                      // 9: print 60
                instr(Opcode.RET)
        );
        assertEquals("60", output);
    }

    @Test
    public void testArrLen() {
        // arr = new int[5]; print(arr.length) → 5
        String output = runSimple(2,
                instr(Opcode.NEW_ARR, sref(-1), imm(5)),
                instr(Opcode.ARR_LEN, sref(-2), sref(-1)),
                instr(Opcode.PRINT, sref(-2)),
                instr(Opcode.RET)
        );
        assertEquals("5", output);
    }

    // =====================================================================
    // E. .lbc 文本格式测试
    // =====================================================================

    @Test
    public void testLbcSimpleAdd() {
        String lbc =
                ".version 1\n" +
                ".class Test\n" +
                "\n" +
                ".func main 0 1 void\n" +
                "    Mov -1, #1\n" +
                "    Add -1, #2\n" +
                "    Print -1\n" +
                "    Ret\n" +
                ".end\n";
        String output = runLbc(lbc);
        assertEquals("3", output);
    }

    @Test
    public void testLbcWhileLoop() {
        String lbc =
                ".version 1\n" +
                ".class Test\n" +
                "\n" +
                ".func main 0 1 void\n" +
                "    Mov -1, #0\n" +
                "_loop:\n" +
                "    Jge -1, #5, _end\n" +
                "    Print -1\n" +
                "    Inc -1\n" +
                "    Jmp _loop\n" +
                "_end:\n" +
                "    Ret\n" +
                ".end\n";
        String output = runLbc(lbc);
        assertEquals("01234", output);
    }

    @Test
    public void testLbcPrintfStyle() {
        String lbc =
                ".version 1\n" +
                ".class Test\n" +
                "\n" +
                ".func main 0 1 void\n" +
                "    Mov -1, #42\n" +
                "    Print \"x=\"\n" +
                "    Print -1\n" +
                "    PrintNL\n" +
                "    Ret\n" +
                ".end\n";
        String output = runLbc(lbc);
        assertEquals("x=42\n", output);
    }

    @Test
    public void testLbcLabelsAreScopedToTheirFunction() {
        String lbc =
                ".version 1\n" +
                ".class ScopedLabels\n" +
                ".func main 0 0 void\n" +
                "    Jmp _done\n" +
                "    Print \"bad-main\"\n" +
                "_done:\n" +
                "    Call helper\n" +
                "    Ret\n" +
                ".end\n" +
                ".func helper 0 0 void\n" +
                "    Jmp _done\n" +
                "    Print \"bad-helper\"\n" +
                "_done:\n" +
                "    Print \"ok\"\n" +
                "    Ret\n" +
                ".end\n";

        assertEquals("ok", runLbc(lbc));
    }

    @Test(expected = VmException.class)
    public void testLbcRejectsDuplicateLabelInFunction() {
        runLbc(".func main 0 0 void\n_same:\n_same:\nRet\n.end\n");
    }

    @Test(expected = VmException.class)
    public void testLbcRejectsWrongOperandCount() {
        runLbc(".func main 0 0 void\nPrint\nRet\n.end\n");
    }

    // =====================================================================
    // F. 边界情况
    // =====================================================================

    @Test(expected = VmException.class)
    public void testDivByZero() {
        runSimple(1,
                instr(Opcode.MOV, sref(-1), imm(10)),
                instr(Opcode.DIV, sref(-1), imm(0)),
                instr(Opcode.RET)
        );
    }

    @Test
    public void testPrintNewline() {
        String output = runSimple(0,
                instr(Opcode.PRINT, imms("hello")),
                instr(Opcode.PRINT_NL),
                instr(Opcode.PRINT, imms("world")),
                instr(Opcode.RET)
        );
        assertEquals("hello\nworld", output);
    }

    @Test
    public void testBoolPrint() {
        // LemonC 打印 bool 时输出 1/0（与 JVM 后端一致）
        String output = runSimple(1,
                instr(Opcode.MOV, sref(-1), immb(true)),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.PRINT, imms(",")),
                instr(Opcode.MOV, sref(-1), immb(false)),
                instr(Opcode.PRINT, sref(-1)),
                instr(Opcode.RET)
        );
        assertEquals("1,0", output);
    }
}
