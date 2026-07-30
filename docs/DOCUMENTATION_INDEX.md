# LemonC 文档索引

> 更新于 2026-07-30。源码、自动化测试和示例 manifest 是最终事实来源。
> 当前基线：50 个主 Java 文件、15 个 package、349 个测试、86 个端到端示例、
> JVM/LemonVM 双后端、JDK 8 CI、Apache-2.0。

## 当前文档

这些文档描述当前实现，代码变化时应同步更新。

| 文档 | 职责 |
|---|---|
| [`../README.md`](../README.md) | 项目入口、构建、CLI、能力概览 |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | 当前编译管线、模块边界、数据所有权和 legacy 路径 |
| [`LEMONC_FEATURES.md`](LEMONC_FEATURES.md) | 语言能力、限制、示例和 CLI |
| [`../document/LemonC文法规则.md`](../document/LemonC文法规则.md) | 词法、文法和语义约束 |
| [`../CODE_REVIEW.md`](../CODE_REVIEW.md) | 当前评审摘要与 90 分路线 |
| [`LEMONC_FULL_REVIEW.md`](LEMONC_FULL_REVIEW.md) | 以当前源码为准的完整编译原理评审 |
| [`../document/LemonC项目总结.md`](../document/LemonC项目总结.md) | 当前项目结构与实现总结 |
| [`../document/DELIVERY.md`](../document/DELIVERY.md) | JVM 交付链路、验证方式与当前状态 |
| [`../document/LemonC 编译器说明.md`](../document/LemonC%20编译器说明.md) | JVM/Jasmin 后端教学补充 |
| [`BACKPATCHING_CONTROL_FLOW.md`](BACKPATCHING_CONTROL_FLOW.md) | legacy direct-JVM 路径的回填算法 |
| [`../tools/native-experiment/README.md`](../tools/native-experiment/README.md) | 归档的 native backend 实验说明 |
| [`../tools/native-experiment/BUILD_NATIVE.md`](../tools/native-experiment/BUILD_NATIVE.md) | native 实验构建步骤 |

## 历史快照

以下文件保留原始评分、旧架构和演进记录。它们已经在开头标明历史属性，
不应作为当前类名、能力、数量、许可证或 CI 状态的依据。

| 文档 | 快照性质 |
|---|---|
| [`CODE_REVIEW_REPORT.md`](CODE_REVIEW_REPORT.md) | 2026-05-27 多轮评审记录 |
| [`COMPILER_REVIEW.md`](COMPILER_REVIEW.md) | 2026-07-07 全维度评审 |
| [`../document/CODE_REVIEW.md`](../document/CODE_REVIEW.md) | JVM 单后端时期评估 |
| [`../document/LemonC编译器评价.md`](../document/LemonC编译器评价.md) | 旧能力与旧测试基线评价 |
| [`../document/lemonc_compiler_evaluation.md`](../document/lemonc_compiler_evaluation.md) | 旧 DFA/AST 评分报告 |
| [`../document/LemonIR与LemonVM设计方案.md`](../document/LemonIR与LemonVM设计方案.md) | LemonIR/LemonVM 实现前的设计提案 |

## 事实优先级

出现冲突时按以下顺序判断：

1. 当前源码、`pom.xml`、`.github/workflows/ci.yml` 和测试结果。
2. `ARCHITECTURE.md`、`LEMONC_FEATURES.md`、`LemonC文法规则.md`。
3. 当前评审与项目总结。
4. 历史快照和设计提案。

数量类信息必须能由仓库直接复核；易变化的 Java 行数不作为文档基线。CLI 参数、
输出目录、运行命令、许可证和 CI 状态发生变化时，应同时更新 README 与本索引。
