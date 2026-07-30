# LemonC 当前代码评审

> 基准日期：2026-07-30。本文只描述当前源码；历史评分见
> [`docs/DOCUMENTATION_INDEX.md`](docs/DOCUMENTATION_INDEX.md) 的归档区。
> 详细证据与分项说明见 [`docs/LEMONC_FULL_REVIEW.md`](docs/LEMONC_FULL_REVIEW.md)。

## 综合评分：89 / 100

| 维度 | 满分 | 得分 | 当前判断 |
|---|---:|---:|---|
| 词法分析 | 12 | 11 | 手写 scanner 完整处理位置、注释、转义、数值进制和溢出 |
| 语法分析 | 15 | 13 | 递归下降、优先级和 panic-mode 恢复成熟 |
| Source AST / Typed-AST | 10 | 9 | 两棵树已经真正分离，Typed-AST 不可变且绑定符号 |
| 语义分析 | 15 | 14 | 系统化 `ErrorType`、真词法作用域、赋值状态和返回路径分析完整 |
| Typed-AST 优化 | 10 | 8 | 折叠、化简和求值效果分析可靠，但仍是局部单遍优化 |
| LemonIR 与验证 | 15 | 14 | 类型化三地址码、完整源码范围、CFG、可达性和定义验证齐全 |
| JVM / LemonVM 后端 | 10 | 8 | 双后端共用 IR，有输出一致性回归；LemonVM 资源上限可配且运行时故障带源码位置 |
| 测试与工程化 | 8 | 7 | 349 个测试、86 个端到端示例和 JDK 8 CI |
| 文档与教学价值 | 5 | 5 | 当前架构、语言能力、文法和历史材料已有明确分层 |
| **合计** | **100** | **89** | **高质量教学编译器，离 90 分主要差通用 IR 优化或随机差分验证** |

## 已确认的架构事实

```text
source
  -> Lexer
  -> Parser
  -> syntax-only Ast
  -> SemanticVisitor
  -> SemanticResult + immutable TypedAst
  -> AstOptimizer
  -> AstToIrTranslator
  -> IrVerifier
  -> IrToJvmTranslator -> Jasmin -> .class
  -> IrToVmTranslator  -> Script -> LemonVm
```

- Parser AST 不保存推导类型、符号绑定或 `ErrorType`。
- `SemanticVisitor` 构造新的 `TypedAst.Program`，不会反向修改 parser AST。
- `TypedAst.Type.ERROR` 和 `ErrorExpr` 只承担错误恢复与传播；正常程序到达 IR 前必须通过语义检查。
- `AstOptimizer` 和 `AstToIrTranslator` 的入口只接受 `TypedAst.Program`。
- `TranslatorVisitor` 是保留的 direct-JVM 教学路径，不属于 CLI 主链路。

## 主要亮点

1. **前端边界清楚**：syntax-only AST 与不可变 Typed-AST 在 Java 类型签名上隔离。
2. **错误恢复系统化**：lexer、parser、semantic 都可给出位置化诊断；语义阶段能在错误节点上继续分析。
3. **IR 验证不只检查格式**：还做 CFG 可达性和虚拟寄存器定义先于使用的数据流分析。
4. **优化尊重程序行为**：`PURE`、`MAY_TRAP`、`HAS_SIDE_EFFECT` 防止错误删除调用、数组访问和可能抛错的运算。
5. **验证链路完整**：JVM、LemonVM、golden output 和双后端一致性共同覆盖 86 个示例。

## 2026-07-30 更新

以下改动发生在上一版评审之后，已反映在上表的判断里：

- 修复三处会产生错误结果的数值语义缺陷：常量比较折叠按运行时提升类型取值；
  一元负号降级为 `NEG` 而非 `0 - x`（保留 IEEE-754 负零）；小数字面量在 `double`
  位置按十进制原文取值，赋值、二元运算、数组元素、实参、返回值、比较和常量折叠
  七处一致。`ArrayTest02` 的 golden 输出随之更正。
- 语义层改为真词法作用域：并列兄弟块可复用同一名字，重复声明只报一次且仍写入
  作用域，不再级联出“未定义的变量”。
- 表达式优先级拆层（相等运算符低于关系运算符，关系运算符非结合），语句层统一到
  赋值目标入口并新增复合赋值与后缀自增。
- LemonVM 解除两个硬上限：运行时栈按需增长，指令上限提高并可关闭；运行时故障
  带源码位置、保留出错前输出、以非零码退出。

下表的 P1/P2/P3 缺口**均未处理**，因此总评分未做上调。

## 距离 90 分的真实缺口

| 优先级 | 缺口 | 到 90 分的建议 |
|---|---|---|
| P1 | 没有随机差分测试 | 生成有界、类型正确的小程序，对比 JVM 与 LemonVM 的 stdout、退出状态和失败类别 |
| P1 | 缺少通用 IR 优化 | 先实现常量传播、复制传播和死代码消除，并复用 CFG/定义分析基础设施 |
| P2 | 缺少覆盖率门禁 | CI 生成 JaCoCo 报告，并逐步设置合理阈值 |
| P3 | legacy direct translator 仍在主源码树 | 明确兼容周期，或迁到独立教学模块，减少两条代码生成路径的维护成本 |

## 验证基线

```text
mvn clean test
Tests run: 349, Failures: 0, Errors: 0, Skipped: 0

root .lemon examples: 86
example-output-manifest.tsv rows: 86
main Java files: 50
Java packages: 15
```

GitHub Actions 在 Temurin JDK 8 上执行 `mvn -B clean test`。许可证为 Apache-2.0。
