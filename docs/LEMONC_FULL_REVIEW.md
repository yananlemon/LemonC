# LemonC 编译原理全维度评审

> 当前评审，基准日期 2026-07-30。结论来自当前源码、测试报告、示例 manifest、
> Maven 配置和 CI 配置；不沿用历史报告中的类名、行数或能力判断。
>
> 2026-07-30 更新：修复三处数值语义缺陷（常量比较折叠精度、一元负号负零、
> 小数字面量在 double 位置的取值一致性），补齐真词法作用域、表达式优先级层级、
> 复合赋值与自增，解除 LemonVM 的指令数与栈深度硬上限并使运行时故障带源码位置。
> 下文各维度的扣分项中，通用 IR 优化、随机差分测试、覆盖率门禁和 legacy
> translator 仍未处理，故总评分未做上调。

## 评审基线

| 项目 | 当前事实 |
|---|---|
| 主源码 | 50 个 Java 文件，15 个 package |
| 自动化测试 | `mvn clean test`：349 个测试，0 failure，0 error，0 skipped |
| 端到端语料 | 86 个根目录 `.lemon` 示例，manifest 有 86 行有效记录 |
| 后端 | JVM/Jasmin 与 LemonVM |
| CI | GitHub Actions，Temurin JDK 8，执行 `mvn -B clean test` |
| 许可证 | Apache-2.0 |

## 总评分：89 / 100

| 维度 | 满分 | 得分 | 结论 |
|---|---:|---:|---|
| 词法分析 | 12 | 11 | 边界、位置和数值字面量处理扎实 |
| 语法分析 | 15 | 13 | 递归下降与 panic-mode 恢复成熟 |
| Source AST / Typed-AST | 10 | 9 | 两层已真实分离，Typed-AST 不可变 |
| 语义分析 | 15 | 14 | 符号、类型、控制流和错误传播完整 |
| Typed-AST 优化 | 10 | 8 | 局部优化可靠，尚无通用数据流优化 |
| LemonIR 与验证 | 15 | 14 | 类型化 TAC、源码范围、基本块和 verifier 完整 |
| JVM / LemonVM 后端 | 10 | 8 | 双后端共用 IR，有一致性回归 |
| 测试与工程化 | 8 | 7 | 349 测试、86 示例和 CI |
| 文档与教学价值 | 5 | 5 | 当前文档、历史材料和设计档案已分层 |
| **合计** | **100** | **89** | **高质量教学编译器** |

这个分数反映当前实现，而不是把“课程项目”和“工业编译器”两套标准混为一谈。
作为课程或教学项目，它已经是优秀档；按可扩展编译器基础设施衡量，主要扣分来自
缺少通用 IR 优化、随机差分测试、warning 层和 VM 内存回收。

## 1. 词法分析：11 / 12

[`Lexer.java`](../src/main/java/site/ilemon/lexer/Lexer.java) 是直接驱动字符流的手写
scanner，使用 `peek()` / `advance()`，当前代码中没有 `LexerState` 枚举状态机。

已确认的能力：

- 标识符、关键字、运算符和分隔符。
- 十进制、八进制、十六进制整数，整数范围与非法进制检查。
- `float`、`double`、科学记数法及数值后缀。
- 单行/多行注释、字符串转义、UTF-8 BOM。
- token 行列信息、源码行与 caret 诊断。
- 对单个 `&`、`|` 等常见错误给出定向提示。
- 对外返回不可修改 token 列表，关键字表也不可修改。

扣分项：

- token 持有 end-exclusive 的起止行列，转义字符串也按原始源码范围计数。
- 字符串不支持 `\uXXXX` 形式的 Unicode 转义。

## 2. 语法分析：13 / 15

[`Parser.java`](../src/main/java/site/ilemon/parser/Parser.java) 使用手写递归下降，表达式
优先级为：

```text
|| < && < comparison < +,- < *,/,% < unary
```

当前 parser 支持：

- block item 中任意位置出现局部声明。
- 标量声明初始化、固定正长度局部数组、无长度数组形参。
- `if/else`、`while`、三段式 `for`、`break`、`continue`。
- `return;` 和 `return expr;`。
- 一元负号和逻辑非。
- 语句级、方法级 panic-mode 恢复，以及结构化 `ParseResult` / `ParseDiagnostic`。

扣分项：

- 表达式内部的恢复粒度仍低于语句级恢复。
- 部分运算符分派仍可进一步使用 `TokenKind` 统一，减少字符串分支。
- 没有将 `else if` 建模为独立语法节点，但现有嵌套语义正确。

完整语法以 [`../document/LemonC文法规则.md`](../document/LemonC文法规则.md) 为准。

## 3. Source AST / Typed-AST：9 / 10

当前分离是**真实的类型边界**，不是在同一棵 AST 上加字段：

```text
Parser -> site.ilemon.ast.Ast
SemanticVisitor -> site.ilemon.typedast.TypedAst
```

[`Ast.java`](../src/main/java/site/ilemon/ast/Ast.java) 只表达语法结构和显式声明类型；
它不保存推导类型、解析后的符号、`ErrorType` 或 optimizer 状态。

[`TypedAst.java`](../src/main/java/site/ilemon/typedast/TypedAst.java) 的关键性质：

- 节点和字段不可变，集合在构造时复制并封装。
- 每个表达式都持有非空 `Type`。
- 变量和调用绑定到不可变 `Symbol` / `MethodSymbol`。
- 所有节点和符号持有不可变的完整 `SourceSpan`。
- `Type.ERROR` 与 `ErrorExpr` 为错误恢复提供稳定 sentinel。
- 构造器检查声明类型与推导类型的一致性。

[`TypedAstSeparationTest.java`](../src/test/java/TypedAstSeparationTest.java) 对不可变性、
source/typed 分离和入口类型约束做回归保护。

扣分项：

- 两棵 AST 仍主要集中在大型单文件内部类结构中。
- 数组类型可以进一步抽象为统一的 `ArrayType(elementType)`。

## 4. 语义分析：14 / 15

[`SemanticVisitor.java`](../src/main/java/site/ilemon/semantic/SemanticVisitor.java) 当前是
source AST 到 Typed-AST 的构造 pass，不再通过 `typeStack` 或修改 source AST 传递类型。
[`SemanticResult.java`](../src/main/java/site/ilemon/semantic/SemanticResult.java) 返回
Typed-AST、诊断和 source-node 到 typed-node 的 identity 映射。

已确认的语义能力：

- 唯一 `void main()` 入口约束。
- 方法签名预注册，支持递归调用。
- 分层块作用域；离开块后名字不可见。
- 同一方法内禁止同名局部变量遮蔽。
- 未声明、重复声明和使用前未赋值检查。
- `int -> float -> double` 数值宽化。
- 算术、比较、逻辑、数组、调用和返回类型检查。
- 非 `void` 方法返回路径分析。
- `break` / `continue` 循环深度检查。
- `printf` 参数个数与 `%d` / `%f` 类型检查。
- collecting 模式下的多错误收集。

### ErrorType 结论

系统化 `ErrorType` 已完成：

- parser AST 中没有语义错误类型。
- 语义层使用唯一的 `TypedAst.Type.ERROR`。
- 无法构造正常表达式时返回 `ErrorExpr`，并继续收集后续诊断。
- 类型兼容、数值提升、调用和赋值检查都显式识别 ERROR，避免级联报错。
- 有语义错误时主链路不会把 Typed-AST 交给 optimizer 或 IR translator。

扣分项：

- 尚无独立 warning 层，例如不可达代码、未使用变量。
- 符号解析与类型检查仍在一个 pass 中；当前规模下合理，但扩展泛型或重载时会变重。

## 5. Typed-AST 优化：8 / 10

[`AstOptimizer.java`](../src/main/java/site/ilemon/optimizer/AstOptimizer.java) 只接收
`TypedAst.Program`，以返回新节点的方式工作，不修改输入树。

已实现：

- int/float/double 算术常量折叠。
- 比较、布尔和一元运算折叠。
- 整数代数恒等式化简。
- 常量 `if`、`while(false)` 和空 block 消除。
- 除零保护。
- `PURE`、`MAY_TRAP`、`HAS_SIDE_EFFECT` 三级求值效果分析。

效果分析是重要亮点：例如 `x * 0` 只有在 `x` 为 PURE 时才能删除，调用、数组访问、
除法和取模不会被不安全地抹掉。

扣分项：

- 优化以局部、单遍树重写为主。
- 尚无 IR 级常量传播、复制传播、DCE、CSE 或循环优化。

## 6. LemonIR 与验证：14 / 15

当前 LemonIR 是类型化三地址码：

```text
IrProgram -> IrFunction -> IrBlock -> IrInstruction
```

[`AstToIrTranslator.java`](../src/main/java/site/ilemon/ir/AstToIrTranslator.java) 的入口只
接受 Typed-AST。IR 覆盖算术、比较、分支、调用、数组、I/O 和数值转换；`&&` / `||`
按控制流短路生成，不会提前求值右操作数。

[`IrVerifier.java`](../src/main/java/site/ilemon/ir/IrVerifier.java) 检查：

- 函数签名、操作码参数个数和操作数类型。
- 基本块终结符、分支目标和调用/返回契约。
- 虚拟寄存器类型一致性。
- CFG 可达性。
- 前向定义分析与 use-before-def。

扣分项：

- IR 不是 SSA，没有 phi 节点。
- 每条生成的 IR 指令携带 Typed-AST 继承的完整 `SourceSpan`，verifier 会在错误中输出。
- CFG 和数据流基础设施主要封装在 verifier 内，优化 pass 不能直接复用。

## 7. JVM / LemonVM 后端：8 / 10

两个后端消费同一份已验证 LemonIR：

- JVM：[`IrToJvmTranslator.java`](../src/main/java/site/ilemon/ir/IrToJvmTranslator.java)
  下降为 JVM 指令模型，由
  [`ByteCodeGenerator.java`](../src/main/java/site/ilemon/codegen/ByteCodeGenerator.java)
  输出 Jasmin 并组装 `.class`。
- VM：[`IrToVmTranslator.java`](../src/main/java/site/ilemon/ir/IrToVmTranslator.java)
  生成 `Script`，由 [`LemonVm.java`](../src/main/java/site/ilemon/vm/LemonVm.java) 解释。

LemonVM 有 31 条 opcode、类型化 `Value`、运行栈、调用帧、局部变量区、数组堆和
100 万条指令执行上限。`BackendEquivalenceTest` 与端到端测试持续比较两个后端。

扣分项：

- JVM 后端依赖 Jasmin，诊断和调试信息有限。
- VM 堆没有垃圾回收。
- VM 字节码以教学可读的文本/对象模型为主，不是紧凑二进制格式。

`TranslatorVisitor` 是保留的 source AST direct-JVM 教学路径，显式接收
`SemanticResult`；CLI 主链路不使用它。回填算法见
[`BACKPATCHING_CONTROL_FLOW.md`](BACKPATCHING_CONTROL_FLOW.md)。

## 8. 测试与工程化：7 / 8

当前测试层次包括：

- lexer、parser、恢复与诊断单元测试。
- 语义、`ErrorType`、作用域和 Typed-AST 分离测试。
- token、两层 AST、优化器和 IR 的 source-span 传播测试。
- optimizer、IR translator 和 IR verifier 测试。
- JVM 与 LemonVM 各自的 86 示例端到端测试。
- golden-output manifest。
- 双后端一致性测试。
- GitHub Actions 的 JDK 8 全量构建。

扣分项：

- 没有 property-based / fuzz / 随机差分测试。
- CI 没有覆盖率报告和覆盖率门禁。
- 当前只有一个 JDK 版本矩阵。

## 9. 文档与教学价值：5 / 5

当前事实分别由以下文档负责：

- [`ARCHITECTURE.md`](ARCHITECTURE.md)：主链路、模块边界和 legacy 路径。
- [`LEMONC_FEATURES.md`](LEMONC_FEATURES.md)：语言能力、限制和 CLI。
- [`../document/LemonC文法规则.md`](../document/LemonC文法规则.md)：词法、文法和语义约束。
- 本文：当前评分、证据和改进优先级。
- [`DOCUMENTATION_INDEX.md`](DOCUMENTATION_INDEX.md)：当前文档与历史快照的边界。

历史评审保留原始结论以展示演进，但已醒目标注，不能覆盖当前文档。

## 距离 90 分的最短路径

1. **随机差分测试**：生成有界且类型正确的小程序，同时运行 JVM 和 LemonVM，
   比较 stdout、退出状态和失败类别。它最能发现跨层组合错误。
2. **通用 IR 优化基础设施**：先把 CFG、use/def 和数据流求解器从 verifier 抽出，
   再实现常量传播、复制传播和死代码消除。
3. **覆盖率门禁**：在 CI 输出 JaCoCo 报告，先观察基线，再设置不过度追求数字的阈值。
4. **warning 层**：增加不可达代码和未使用变量等非阻断诊断。

完成前两项并补齐相应回归后，按本评审模型可达到 **90-92 分**。仅增加文档、示例数量
或局部重构，不足以把分数推过 90。

## 最终结论

LemonC 已经完成 Source AST / Typed-AST 分离和系统化 `ErrorType`，并形成了
“语法树、类型树、类型化 IR、双后端”的清晰层次。当前没有发现会阻断合法示例编译、
破坏双后端一致性或让错误程序进入后端的已知缺陷。

**当前评分：89 / 100。** 下一阶段应把投入集中在随机差分测试和可复用的 IR 数据流优化，
而不是继续扩大前端类层次。
