# LemonC Architecture

This document describes the current implementation, not the older single-backend design notes.

## Current Pipeline

```text
.lemon source
  -> site.ilemon.lexer.Lexer
  -> site.ilemon.parser.Parser
  -> site.ilemon.ast.Ast
  -> site.ilemon.semantic.SemanticVisitor
  -> site.ilemon.optimizer.AstOptimizer
  -> site.ilemon.ir.AstToIrTranslator
  -> site.ilemon.ir.IrVerifier
  -> JVM backend: site.ilemon.ir.IrToJvmTranslator -> site.ilemon.codegen.ByteCodeGenerator -> Jasmin -> .class
  -> VM backend: site.ilemon.ir.IrToVmTranslator -> site.ilemon.vm.LemonVm
```

`LemonC` keeps the front end shared and lowers optimized AST into typed LemonIR. Both executable backends consume that same IR, which keeps the compiler from having two separate semantic interpretations of the language.

## Front End

| Package | Responsibility |
|---|---|
| `site.ilemon.lexer` | Hand-written lexer with source positions, comments, numeric literals, strings, keywords, and operators. |
| `site.ilemon.parser` | Recursive-descent parser that builds source-level AST and enforces the single-class file shape. |
| `site.ilemon.ast` | Source-level AST for declarations, statements, expressions, methods, types, and programs. |
| `site.ilemon.semantic` | Symbol tables, type checking, use-before-assignment checks, return-path checks, loop-depth checks, and printf validation. |
| `site.ilemon.optimizer` | Conservative AST-level constant folding, boolean simplification, algebraic identities, and constant branch simplification. |

## LemonIR

`site.ilemon.ir` is the main middle layer. It contains:

| Class | Role |
|---|---|
| `IrProgram`, `IrFunction`, `IrBlock`, `IrInstruction` | Program, function, basic-block, and instruction model. |
| `IrType`, `IrValue`, `IrOpcode` | Typed IR vocabulary. |
| `AstToIrTranslator` | Lowers optimized source AST into LemonIR. |
| `IrVerifier` | Checks IR shape, operand counts, type compatibility, control-flow structure, call signatures, array operations, and returns. |
| `IrPrinter` | Dumps LemonIR for teaching and debugging. |
| `IrToJvmTranslator` | Lowers LemonIR to JVM instruction IR. |
| `IrToVmTranslator` | Lowers LemonIR to LemonVM bytecode. |

The verifier is an important boundary: changes to the front end or optimization should still produce well-typed IR before either backend runs.

## JVM Backend

The active JVM backend is:

```text
LemonIR -> IrToJvmTranslator -> site.ilemon.codegen.ast.Ast -> ByteCodeGenerator -> Jasmin -> .class
```

`ByteCodeGenerator` writes Jasmin IL and computes `.limit stack` and `.limit locals` from the generated instruction stream. This path is validated by JVM execution tests and all example-program golden outputs.

## LemonVM Backend

The custom VM backend is:

```text
LemonIR -> IrToVmTranslator -> Script -> LemonVm
```

`site.ilemon.vm` implements the VM runtime:

| Class | Role |
|---|---|
| `Script`, `VmFunction`, `Instruction`, `Opcode` | LemonVM bytecode model. |
| `LemonVm` | Interpreter loop, calls, branches, arithmetic, conversions, arrays, and output. |
| `RuntimeStack`, `Value` | Stack-frame and typed value model. |
| `VmHeap`, `VmArray` | Heap-backed arrays. |
| `VmBytecodeParser` | Text bytecode parser for dumped `.lbc`-style output. |

The VM bytecode text format uses bare numeric operands for stack-frame references and `#`-prefixed numeric operands for immediate literals.

## Legacy JVM Translator

`site.ilemon.codegen.TranslatorVisitor` is the older direct AST-to-JVM-instruction translator. It is still useful as reference material and is still covered by tests, but the CLI main path now goes through LemonIR.

When adding new language features, prefer updating the LemonIR path first. Only update `TranslatorVisitor` when preserving legacy tests or reference behavior requires it.

## Test Protection

The current clean validation command is:

```bash
mvn clean test
```

The main protection layers are:

| Test | What it protects |
|---|---|
| `AllExamplesJvmTest` | Every root example compiles and runs on the JVM with manifest-matched stdout. |
| `AllExamplesVmTest` | Every root example compiles and runs on LemonVM with manifest-matched stdout. |
| `BackendEquivalenceTest` | JVM and LemonVM outputs stay equivalent through the shared LemonIR path. |
| `IrVerifierTest` | Invalid IR is rejected before backend lowering. |
| `LemonVmTest` | VM runtime semantics. |
| `ErrorTest`, `DiagnosticTest` | Parser, semantic, and CLI diagnostics. |

## Non-Main Source Trees

`dragon-book-front/` is a Dragon Book style reference front end.

`tools/native-experiment/` is an archived Windows x86-64 native backend experiment. It is intentionally outside the Maven source tree and should not be treated as production code without new tests, build wiring, and output isolation under `target/`.

