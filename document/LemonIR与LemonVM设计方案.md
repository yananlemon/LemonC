# LemonIR 与 LemonVM 设计方案

## 1. 设计目标

当前 LemonC 已经具备较完整的编译链路：

```text
LemonC Source
-> Lexer
-> Parser
-> AST
-> Semantic
-> TranslatorVisitor
-> JVM/Jasmin Backend
-> .class
```

但现有架构仍然以 JVM 字节码生成为主要目标，编译器中间层和后端边界不够清晰。为了让 LemonC 从“面向 JVM 的教学编译器”升级为“多后端编译器系统”，后续计划引入：

```text
LemonIR
LemonVM Bytecode
LemonVM
```

目标是形成新的编译架构：

```text
LemonC Source
-> Lexer
-> Parser
-> AST
-> Semantic
-> LemonIR
-> Backend 1: JVM/Jasmin
-> Backend 2: LemonVM Bytecode
-> LemonVM
```

这样 LemonC 可以同时保留现有 JVM 后端，并新增自研虚拟机执行路径。项目展示时可以强调：

- 编译器前端完整。
- 中间表示独立于具体后端。
- 支持多后端代码生成。
- 自研虚拟机包含字节码、操作数栈、调用栈、局部变量表和堆式数组模型。
- 可用同一组 LemonC 示例对比 JVM 后端与 LemonVM 后端输出一致性。

## 2. 核心结论

推荐设计：

```text
IR 使用：类型化三地址码 + 基本块 CFG
VM 使用：栈式字节码虚拟机
```

不要让 IR 和 VM 字节码混在一起。

两者定位不同：

| 层次 | 作用 | 形式 |
|---|---|---|
| AST | 表达源码语法结构 | 树 |
| LemonIR | 表达语义后的程序逻辑，服务优化和多后端 | 类型化三地址码 |
| LemonVM Bytecode | 面向虚拟机解释执行 | 栈式指令 |
| LemonVM | 执行字节码 | Java 实现的解释型 VM |

## 3. 为什么 IR 选三地址码

常见 IR 方案有：

| 方案 | 优点 | 缺点 | 是否推荐 |
|---|---|---|---|
| AST IR | 复用现有 AST，改动小 | 太接近源码，不适合优化和多后端 | 不推荐作为正式 IR |
| 栈式 IR | 接近 VM，实现简单 | 太接近执行器，不适合分析和优化 | 不推荐作为编译器 IR |
| 三地址码 | 清晰、经典、适合多后端 | 需要新建 IR 数据结构 | 推荐 |
| SSA | 专业，适合高级优化 | 实现复杂，成本高 | 后续进阶 |

LemonC 当前最合适的是三地址码。它足够专业，又不会像 SSA 那样把项目复杂度拉得过高。

示例：

```c
class Test {
    void main() {
        int x;
        x = 1 + 2;
        printf("%d\n", x);
    }
}
```

对应 LemonIR：

```text
function main() -> void
entry:
  v0 = const_i 1
  v1 = const_i 2
  v2 = add_i v0, v1
  store x, v2
  v3 = load x
  print_i v3
  ret_void
end
```

这个 IR 不依赖 JVM，也不依赖 LemonVM。它可以继续下降到 JVM/Jasmin，也可以下降到 LemonVM 字节码，未来还可以下降到 C 或 LLVM IR。

## 4. LemonIR 总体结构

建议新增包：

```text
src/main/java/site/ilemon/ir/
```

推荐结构：

```text
ir/
  IrProgram.java
  IrFunction.java
  IrBlock.java
  IrInstruction.java
  IrOpcode.java
  IrType.java
  IrValue.java
  IrLocal.java
  IrParam.java
  IrLabel.java
  AstToIrTranslator.java
  IrPrinter.java
```

核心对象关系：

```text
IrProgram
  List<IrFunction>

IrFunction
  String name
  IrType returnType
  List<IrParam> params
  List<IrLocal> locals
  List<IrBlock> blocks

IrBlock
  String label
  List<IrInstruction> instructions

IrInstruction
  IrOpcode opcode
  IrType type
  IrValue result
  List<IrValue> operands
```

## 5. LemonIR 类型系统

第一阶段建议覆盖现有 LemonC 类型：

```java
enum IrType {
    I32,
    F32,
    F64,
    BOOL,
    VOID,
    STRING,
    I32_ARRAY,
    F32_ARRAY,
    F64_ARRAY,
    BOOL_ARRAY,
    STRING_ARRAY
}
```

说明：

- `I32` 对应 LemonC `int`。
- `F32` 对应 LemonC `float`。
- `F64` 对应 LemonC `double`。
- `BOOL` 对应 LemonC `bool`。
- `STRING` 可为后续 String 类型预留。
- 数组类型单独列出，避免在后端反复判断元素类型。

## 6. LemonIR 指令集草案

### 6.1 常量指令

```text
v0 = const_i 1
v1 = const_f 1.5
v2 = const_d 3.14
v3 = const_b true
v4 = const_s "hello"
```

### 6.2 局部变量读写

```text
v1 = load x
store x, v1
```

也可以在实现中把局部变量统一编号：

```text
v1 = load_local 0
store_local 0, v1
```

推荐第一版使用名字，便于调试；VM 字节码阶段再分配局部变量编号。

### 6.3 算术指令

```text
v2 = add_i v0, v1
v2 = sub_i v0, v1
v2 = mul_i v0, v1
v2 = div_i v0, v1

v2 = add_f v0, v1
v2 = add_d v0, v1
```

### 6.4 比较指令

```text
v2 = eq_i v0, v1
v2 = ne_i v0, v1
v2 = lt_i v0, v1
v2 = le_i v0, v1
v2 = gt_i v0, v1
v2 = ge_i v0, v1
```

比较结果类型统一为 `BOOL`。

### 6.5 控制流指令

```text
br label
br_true v0, label
br_false v0, label
```

建议 IR 使用基本块结构，而不是把所有跳转混在线性文本里。示例：

```text
entry:
  v0 = load x
  v1 = const_i 0
  v2 = gt_i v0, v1
  br_true v2, then_block
  br else_block

then_block:
  print_s "positive"
  br end_if

else_block:
  print_s "non-positive"
  br end_if

end_if:
  ret_void
```

### 6.6 函数调用

```text
v0 = call f, arg0, arg1
call_void printSomething, arg0
ret v0
ret_void
```

如果函数返回 `void`，不生成 result。

### 6.7 数组指令

```text
v0 = new_array_i length
v1 = array_load_i arr, index
array_store_i arr, index, value
v2 = array_len arr
```

数组类型建议在语义阶段已经确定，IR 阶段不再猜类型。

### 6.8 输出指令

短期可以保留接近当前语言能力的输出指令：

```text
print_i v0
print_f v0
print_d v0
print_b v0
print_s v0
print_newline
```

如果继续支持 `printf("%d\n", x)`，可以在 AST 到 IR 阶段把格式串拆成多个 print 指令。

例如：

```c
printf("x=%d\n", x);
```

降低为：

```text
print_s "x="
print_i x
print_newline
```

## 7. LemonVM 字节码设计

LemonVM 字节码建议使用栈式模型。IR 是三地址码，VM 字节码是栈式，两者分工清楚。

示例 LemonIR：

```text
v0 = const_i 1
v1 = const_i 2
v2 = add_i v0, v1
store x, v2
v3 = load x
print_i v3
ret_void
```

对应 LemonVM bytecode：

```text
.function main
.locals 1
iconst 1
iconst 2
iadd
istore 0
iload 0
print_i
ret_void
.end
```

### 7.1 字节码文件格式

第一版建议使用文本格式 `.lbc`，方便调试和答辩展示。

示例：

```text
.version 1
.class Test

.function main 0 1 void
iconst 1
iconst 2
iadd
istore 0
iload 0
print_i
ret_void
.end
```

函数头含义：

```text
.function <name> <paramCount> <localCount> <returnType>
```

后续如果需要更像真实虚拟机，可以再做二进制 `.lbc`。

### 7.2 VM 指令集草案

常量：

```text
iconst <int>
fconst <float>
dconst <double>
bconst <true|false>
sconst <string>
```

局部变量：

```text
iload <slot>
istore <slot>
fload <slot>
fstore <slot>
dload <slot>
dstore <slot>
bload <slot>
bstore <slot>
aload <slot>
astore <slot>
```

算术：

```text
iadd
isub
imul
idiv
fadd
fsub
fmul
fdiv
dadd
dsub
dmul
ddiv
```

比较：

```text
icmp_eq
icmp_ne
icmp_lt
icmp_le
icmp_gt
icmp_ge
```

控制流：

```text
label <name>
br <label>
br_true <label>
br_false <label>
```

函数：

```text
call <functionName> <argCount>
ret
ret_i
ret_f
ret_d
ret_b
ret_void
```

数组：

```text
newarray_i
newarray_f
newarray_d
newarray_b
array_load_i
array_store_i
array_load_f
array_store_f
array_load_d
array_store_d
array_load_b
array_store_b
array_len
```

输出：

```text
print_i
print_f
print_d
print_b
print_s
print_newline
```

程序结束：

```text
halt
```

如果 `main` 用 `ret_void` 结束，VM 顶层也可以自动结束，不一定显式生成 `halt`。

## 8. LemonVM 运行时模型

建议新增包：

```text
src/main/java/site/ilemon/vm/
```

推荐结构：

```text
vm/
  LemonVm.java
  VmProgram.java
  VmFunction.java
  VmInstruction.java
  VmOpcode.java
  VmType.java
  VmValue.java
  VmFrame.java
  VmHeap.java
  VmArray.java
  VmBytecodeParser.java
  VmException.java
```

### 8.1 VM 状态

VM 至少维护：

```text
functions: 函数表
callStack: 调用栈
heap: 堆
```

伪代码：

```java
class LemonVm {
    private Map<String, VmFunction> functions;
    private Deque<VmFrame> callStack;
    private VmHeap heap;

    public void run(String entryName) {
        pushFrame(entryName, emptyArgs);
        while (!callStack.isEmpty()) {
            VmFrame frame = callStack.peek();
            VmInstruction instruction = frame.nextInstruction();
            execute(instruction);
        }
    }
}
```

### 8.2 栈帧

每次函数调用创建一个栈帧：

```java
class VmFrame {
    VmFunction function;
    int pc;
    VmValue[] locals;
    Deque<VmValue> operandStack;
}
```

栈帧包含：

- 当前函数。
- 当前 pc。
- 局部变量表。
- 操作数栈。

### 8.3 Value 模型

建议第一版使用统一 Value：

```java
class VmValue {
    VmType type;
    Object value;
}
```

类型：

```java
enum VmType {
    I32,
    F32,
    F64,
    BOOL,
    STRING,
    REF,
    VOID
}
```

数组不直接放在 Value 里，而是放在 heap 中，Value 保存引用编号。

### 8.4 Heap 模型

数组、后续对象、复杂字符串都可以放到 heap。

```java
class VmHeap {
    private int nextRef = 1;
    private Map<Integer, VmArray> arrays = new HashMap<Integer, VmArray>();

    int allocateArray(VmType elementType, int length) { ... }
    VmValue loadArray(int ref, int index) { ... }
    void storeArray(int ref, int index, VmValue value) { ... }
    int arrayLength(int ref) { ... }
}
```

数组对象：

```java
class VmArray {
    VmType elementType;
    VmValue[] elements;
}
```

### 8.5 指令执行循环

最小 VM 的核心就是解释循环：

```java
while (!callStack.isEmpty()) {
    VmFrame frame = callStack.peek();
    VmInstruction ins = frame.function.instructions.get(frame.pc++);

    switch (ins.opcode) {
        case ICONST:
            frame.operandStack.push(VmValue.i32(ins.intOperand));
            break;

        case IADD:
            VmValue right = frame.operandStack.pop();
            VmValue left = frame.operandStack.pop();
            frame.operandStack.push(VmValue.i32(left.asInt() + right.asInt()));
            break;

        case ISTORE:
            frame.locals[ins.slot] = frame.operandStack.pop();
            break;

        case ILOAD:
            frame.operandStack.push(frame.locals[ins.slot]);
            break;

        case PRINT_I:
            output.print(frame.operandStack.pop().asInt());
            break;

        case RET_VOID:
            callStack.pop();
            break;
    }
}
```

这就是 VM 的心脏。真正的工程价值来自指令集、栈帧、堆、函数调用、错误处理和测试体系。

## 9. IR 到 VM 字节码的下降规则

三地址码需要下降为栈式指令。

例如：

```text
v2 = add_i v0, v1
```

如果 `v0`、`v1` 已经分配到局部变量 slot 0 和 slot 1，`v2` 分配到 slot 2：

```text
iload 0
iload 1
iadd
istore 2
```

基本规则：

| IR | VM bytecode |
|---|---|
| `v = const_i n` | `iconst n; istore slot(v)` |
| `v = load x` | `iload slot(x); istore slot(v)` |
| `store x, v` | `iload slot(v); istore slot(x)` |
| `v = add_i a, b` | `iload slot(a); iload slot(b); iadd; istore slot(v)` |
| `br label` | `br label` |
| `br_true v, label` | `bload slot(v); br_true label` |
| `ret v` | `load slot(v); ret_i` |

第一版可以用简单 slot 分配：

```text
参数先占 slot
局部变量再占 slot
临时变量继续分配 slot
```

不需要做寄存器分配。

## 10. JVM 后端如何保留

短期内不要删除现有 JVM 后端。

推荐策略：

```text
阶段一：现有 JVM 后端继续走老路径，保证稳定。
阶段二：新增 LemonIR 和 LemonVM 后端。
阶段三：逐步让 JVM 后端也从 LemonIR 生成。
```

也就是先做到：

```text
AST -> old TranslatorVisitor -> JVM
AST -> LemonIR -> LemonVM bytecode -> LemonVM
```

等 LemonIR 稳定后，再重构为：

```text
AST -> LemonIR -> JVM
AST -> LemonIR -> LemonVM
```

这样风险更低。

## 11. 与现有 LemonC 功能的映射

### 11.1 int

IR：

```text
v0 = const_i 1
v1 = add_i v0, v0
```

VM：

```text
iconst 1
iconst 1
iadd
```

### 11.2 bool

IR：

```text
v0 = const_b true
v1 = eq_i a, b
```

VM：

```text
bconst true
icmp_eq
```

### 11.3 if

IR：

```text
entry:
  v0 = load cond
  br_true v0, then_block
  br else_block

then_block:
  ...
  br end_if

else_block:
  ...
  br end_if

end_if:
```

VM：

```text
bload 0
br_true then_block
br else_block
label then_block
...
br end_if
label else_block
...
br end_if
label end_if
```

### 11.4 while

IR：

```text
loop_cond:
  v0 = ...
  br_true v0, loop_body
  br loop_end

loop_body:
  ...
  br loop_cond

loop_end:
```

VM 对应 label 和 branch 即可。

### 11.5 函数调用

LemonC：

```c
int add(int a, int b) {
    return a + b;
}
```

IR：

```text
function add(I32 a, I32 b) -> I32
entry:
  v0 = load a
  v1 = load b
  v2 = add_i v0, v1
  ret v2
end
```

VM：

```text
.function add 2 3 int
iload 0
iload 1
iadd
ret_i
.end
```

调用方：

```text
iload 0
iload 1
call add 2
istore 2
```

### 11.6 数组

LemonC：

```c
int arr[3];
arr[0] = 10;
x = arr[0];
```

IR：

```text
v0 = const_i 3
v1 = new_array_i v0
store arr, v1
v2 = const_i 0
v3 = const_i 10
array_store_i arr, v2, v3
v4 = array_load_i arr, v2
store x, v4
```

VM：

```text
iconst 3
newarray_i
astore 0
aload 0
iconst 0
iconst 10
array_store_i
aload 0
iconst 0
array_load_i
istore 1
```

## 12. 测试策略

必须按层测试，不要只测最终输出。

### 12.1 IR 生成测试

新增：

```text
src/test/java/IrGenerationTest.java
```

测试内容：

- 常量表达式生成。
- 变量 load/store。
- 算术表达式。
- if 基本块。
- while 基本块。
- 函数调用。
- 数组创建、读写、length。

断言方式：

```text
输入 LemonC 源码
生成 LemonIR
pretty print
和 expected 文本比较
```

### 12.2 VM 字节码生成测试

新增：

```text
src/test/java/VmBytecodeGeneratorTest.java
```

测试内容：

- IR 到栈式字节码。
- 局部变量 slot 分配。
- label 和 branch。
- 函数调用。
- 数组指令。

### 12.3 LemonVM 执行测试

新增：

```text
src/test/java/LemonVmTest.java
```

测试内容：

- `1 + 2` 输出 `3`。
- if/else 输出正确。
- while 循环输出正确。
- 函数调用输出正确。
- 递归 factorial。
- 数组读写。
- 数组参数。

### 12.4 双后端一致性测试

这是展示价值最高的测试：

```text
同一个 .lemon 示例
JVM 后端运行 stdout
LemonVM 后端运行 stdout
两者必须一致
```

新增：

```text
src/test/java/DualBackendConsistencyTest.java
```

示例：

```text
examples/Factorial.lemon
examples/Fibonacci.lemon
examples/ArrayParamTest.lemon
examples/MergeSortTest.lemon
```

## 13. 实现阶段

### 阶段一：LemonIR MVP

目标：

```text
AST -> LemonIR
```

支持：

- int
- bool
- void
- 局部变量
- 算术
- 比较
- if
- while
- printf 简单输出

完成标志：

- 有 IR 数据结构。
- 有 `AstToIrTranslator`。
- 有 `IrPrinter`。
- 有 `IrGenerationTest`。

### 阶段二：LemonVM Bytecode MVP

目标：

```text
LemonIR -> LemonVM Bytecode
```

支持：

- 常量。
- load/store。
- 算术。
- 比较。
- label/branch。
- print。
- ret。

完成标志：

- 能生成 `.lbc` 文本。
- 有 `VmBytecodeGeneratorTest`。

### 阶段三：Java LemonVM MVP

目标：

```text
LemonVM Bytecode -> 执行结果
```

支持：

- 操作数栈。
- 局部变量表。
- 单函数 main。
- int/bool。
- if/while。
- print。

完成标志：

- `LemonVmTest` 能跑基础程序。

### 阶段四：函数调用和调用栈

支持：

- 多函数。
- 参数传递。
- 返回值。
- 递归。

完成标志：

- factorial。
- fibonacci。
- 普通函数调用。

### 阶段五：数组和堆

支持：

- newarray。
- array_load。
- array_store。
- array_len。
- 数组参数。

完成标志：

- ArrayParamTest。
- BubbleSort。
- MergeSort。

### 阶段六：双后端一致性

目标：

```text
同一 LemonC 程序在 JVM 后端和 LemonVM 后端输出一致
```

完成标志：

- 新增 `DualBackendConsistencyTest`。
- 主示例全部通过一致性测试。

### 阶段七：后续扩展

可选：

- String 类型。
- char 类型。
- C 版 LemonVM。
- C backend。
- LLVM backend。
- 简单 IR 优化，如常量折叠、死代码删除。

## 14. CLI 设计

未来 CLI 可以支持：

```text
lemonc source.lemon
lemonc source.lemon --target jvm
lemonc source.lemon --target vm
lemonc source.lemon --dump-ir
lemonc source.lemon --dump-vm-bytecode
lemonc source.lemon --run-vm
```

建议默认：

```text
--target jvm
```

等 LemonVM 稳定后，可以考虑默认：

```text
--target vm
```

或者保持显式，避免破坏现有使用方式。

## 15. 风险与取舍

### 15.1 不要一开始就做 SSA

SSA 很专业，但会明显增加实现成本。当前先用三地址码，未来如果需要高级优化，再引入 SSA。

### 15.2 不要一开始就删除 JVM 后端

JVM 后端是当前稳定路径，也是回归测试基础。新 LemonVM 后端应该先并行存在。

### 15.3 不要让 VM 直接解释 AST

AST 解释器更容易做，但项目含金量低于：

```text
AST -> IR -> Bytecode -> VM
```

如果目标是顶级本科设计，应该保留 IR 和字节码层。

### 15.4 不要追求 VM 性能接近 C

第一版 LemonVM 是解释型 VM，不需要和原生 C 比性能。它的价值在于：

- 自定义指令集。
- 自定义执行模型。
- 编译器多后端。
- 可测试、可展示、可扩展。

如果未来追求性能，可以考虑：

```text
LemonIR -> C backend
LemonIR -> LLVM backend
LemonVM bytecode -> AOT
```

## 16. 推荐最终答辩表述

项目可以这样描述：

```text
LemonC 是一个使用 Java 实现的 C-like 教学编译器。项目实现了词法分析、递归下降语法分析、AST 构建、语义检查、统一中间表示 LemonIR，以及 JVM 与自研 LemonVM 双后端。

LemonIR 采用类型化三地址码和基本块控制流图，用于解耦前端和后端。LemonVM 采用栈式字节码模型，实现了操作数栈、局部变量表、函数调用栈和堆式数组模型。项目通过双后端一致性测试验证同一 LemonC 程序在 JVM 和 LemonVM 上的输出一致性。
```

## 17. 最终建议

推荐路线：

```text
先做 LemonIR
再做 LemonVM bytecode
再做 Java LemonVM
最后做 JVM 与 LemonVM 双后端一致性测试
```

这条路线不会推倒现有 LemonC，也不会继续被 JVM 单后端限制。它能把项目升级成一个更完整、更有设计深度的编译原理系统。

