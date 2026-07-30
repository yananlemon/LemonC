# LemonC 编译器项目总结

> 当前实现说明，更新于 2026-07-28。架构细节以
> [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md) 为准，语言能力以
> [`../docs/LEMONC_FEATURES.md`](../docs/LEMONC_FEATURES.md) 为准。

## 项目概述

LemonC 是一个使用 Java 8 实现的教学型 C-like 编译器。它把 `.lemon` 源码解析为
syntax-only source AST，再由语义分析构建不可变 Typed-AST。Typed-AST 经过安全的
局部优化后生成类型化 LemonIR；验证通过的 IR 可以下降到 JVM/Jasmin 或 LemonVM。

```text
.lemon
  -> Lexer
  -> Parser
  -> source Ast
  -> SemanticVisitor -> SemanticResult + immutable TypedAst
  -> AstOptimizer
  -> AstToIrTranslator -> LemonIR
  -> IrVerifier
  -> JVM: IrToJvmTranslator -> ByteCodeGenerator -> Jasmin -> .class
  -> VM:  IrToVmTranslator -> Script -> LemonVm
```

CLI 默认生成 JVM `.class`；`--target vm` 生成 LemonVM 字节码，`--run-vm` 会在编译后
直接执行 LemonVM。`--pipeline` 选择 VM 路径并打印 tokens、Typed-AST、IR 和 VM 字节码。

## 当前代码结构

```text
src/main/java/site/ilemon/
  lexer/       手写 scanner、Token、TokenKind、IntegerLiterals
  parser/      递归下降 Parser、ParseResult、ParseDiagnostic
  source/      end-exclusive SourceSpan
  ast/         syntax-only source AST
  semantic/    source AST -> Typed-AST、诊断、作用域与控制流分析
  typedast/    不可变 Typed-AST、Type、Symbol、MethodSymbol
  optimizer/   Typed-AST 常量折叠与安全化简
  ir/          LemonIR、Verifier、JVM/VM lowering
  codegen/     JVM 指令模型、Jasmin 输出、legacy direct translator
  vm/          LemonVM 字节码、运行栈、堆与解释器
  compiler/    CLI、Typed-AST/IR 打印器
  exception/   编译期、后端和 VM 异常层次
  visitor/     legacy source-AST visitor 接口
  list/        legacy translator 使用的双向链表
```

当前 Maven 主源码有 50 个 Java 文件、15 个 package。`tools/native-experiment/` 是归档的
Windows x86-64 实验，不属于 Maven 主构建。

## 前端

### Lexer

Lexer 是手写 scanner，使用 `peek()` / `advance()` 直接驱动字符流；当前源码中没有旧版
`LexerState` 枚举状态机。它支持：

- Java 字母、数字和下划线组成的标识符。
- 十进制、八进制、十六进制整数及范围校验。
- `float` / `double`、科学计数法和 `f/F/d/D` 后缀。
- `\n`、`\t`、`\r`、`\"`、`\\` 字符串转义。
- 单行、多行注释和 UTF-8 BOM。
- end-exclusive 起止行列、源码行和 caret 指针诊断。

### Parser

Parser 是手写递归下降分析器，表达式优先级为：

```text
|| < && < comparison < +,- < *,/,% < unary
```

它支持语句级和方法级错误恢复、局部标量初始化、任意 block-item 位置声明、数组形参、
`for`、`break`、`continue` 和 `return;`。完整规则见
[`LemonC文法规则.md`](LemonC文法规则.md)。

### Source AST 与 Typed-AST

`site.ilemon.ast.Ast` 只保存语法结构和显式声明类型，不保存推导类型、解析后符号或
`ErrorType`。`SemanticVisitor` 把 source AST 转换为独立的 `TypedAst.Program`：

- 每个正常表达式都有非空、不可变的类型。
- 标识符和调用绑定到 `Symbol` / `MethodSymbol`。
- `TypedAst.Type.ERROR` 和 `ErrorExpr` 只存在于语义结果中，用于错误恢复。
- optimizer 和 IR translator 的公开入口只接受 `TypedAst.Program`。

`SemanticResult` 还保存诊断和 source-node 到 typed-node 的 identity 映射，用于诊断与
legacy translator 兼容，不会反向修改 parser AST。

Token、source AST、Typed-AST 和优化结果共享不可变 `SourceSpan`；LemonIR 的每条生成
指令也携带对应范围，verifier 报错时会输出该位置。

## 语义分析

当前语义检查包括：

- 唯一 `void main()` 入口。
- 变量与方法声明、重复声明、声明前使用。
- 块级可见域和离开块后不可见。
- 同一方法内禁止同名局部变量遮蔽。
- 使用前赋值与 `if/else` 分支状态合并。
- 数值宽化 `int -> float -> double`。
- 算术、比较、布尔、数组、调用和返回类型检查。
- `break` / `continue` 循环深度检查。
- 非 `void` 方法返回路径检查。
- `printf` 占位符数量与类型检查。
- collecting 模式下的多错误收集。

## 中端与后端

### Typed-AST 优化

`AstOptimizer` 是纯转换器，不修改输入节点。它实现算术、比较和布尔常量折叠、一元负号
折叠、整数代数恒等式、常量分支化简和 `while(false)` 删除。求值效果分为 `PURE`、
`MAY_TRAP`、`HAS_SIDE_EFFECT`，避免错误删除调用、数组访问或可能抛错的除法/取模。

### LemonIR

LemonIR 是类型化三地址码：

```text
IrProgram -> IrFunction -> IrBlock -> IrInstruction
```

`IrVerifier` 检查签名、基本块终结、跳转目标、操作数、类型、数组、调用、返回、CFG
可达性和虚拟寄存器定义先于使用。

### 双后端

- JVM：LemonIR 下降为 JVM 指令模型，`ByteCodeGenerator` 写出 Jasmin，输出位于
  `target/lemonc/`。
- LemonVM：LemonIR 下降为 `Script`，由 `LemonVm` 解释执行。
- `TranslatorVisitor` 是保留的 source AST -> JVM legacy 路径，只用于测试和教学，
  构造时必须显式接收 `SemanticResult`。

## 语言能力

| 类别 | 当前支持 |
|---|---|
| 类型 | `int`、`float`、`double`、`bool`、`void` |
| 数组 | 四种基本数组、固定长度局部数组、无长度数组形参、索引、赋值、`.length` |
| 表达式 | 算术、取模、一元负号、比较、逻辑非、短路 `&&` / `||` |
| 控制流 | `if/else`、`while`、`for`、`break`、`continue`、嵌套块 |
| 方法 | 参数、返回值、`void`、递归、表达式调用、丢弃返回值 |
| 输出 | `printf` 的 `%d` / `%f`、`printLine`、字符串转义 |
| 诊断 | 词法/语法位置诊断、parser 恢复、语义多错误收集 |

字符串目前只作为字面量进入 `printf` 等内部路径，不支持 String 变量、参数或返回值。

## 构建与验证

```bash
mvn clean package
mvn clean test

java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/MulTable.lemon
java -cp target/lemonc MulTable

java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar \
  examples/ReliabilityCanary.lemon --pipeline
```

2026-07-28 基线：

```text
Tests run: 349, Failures: 0, Errors: 0, Skipped: 0
85 root examples
85 manifest rows
```

`AllExamplesJvmTest`、`AllExamplesVmTest` 和 `BackendEquivalenceTest` 共同验证根示例的 JVM
输出、LemonVM 输出与 manifest 基线。GitHub Actions 在 Temurin JDK 8 上执行
`mvn -B clean test`。

## 下一阶段

1. 增加随机差分测试。
2. 抽取可复用 CFG/data-flow 基础设施。
3. 实现 IR 级常量传播、复制传播和死代码消除。
4. 增加 JaCoCo 覆盖率报告与合理门禁。
5. 增加 warning 层，例如不可达代码和未使用变量。
