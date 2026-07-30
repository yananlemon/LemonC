# LemonC Architecture

This document describes the current implementation, not the older single-backend design notes.

## Current Pipeline

```text
.lemon source
  -> site.ilemon.lexer.Lexer
  -> site.ilemon.parser.Parser
  -> site.ilemon.ast.Ast
  -> site.ilemon.semantic.SemanticVisitor
  -> site.ilemon.typedast.TypedAst
  -> site.ilemon.optimizer.AstOptimizer
  -> site.ilemon.ir.AstToIrTranslator
  -> site.ilemon.ir.IrVerifier
  -> JVM backend: site.ilemon.ir.IrToJvmTranslator -> site.ilemon.codegen.ByteCodeGenerator -> Jasmin -> .class
  -> VM backend: site.ilemon.ir.IrToVmTranslator -> site.ilemon.vm.LemonVm
```

`SemanticVisitor` is the boundary between syntax and semantics: it reads the parser-owned source AST and produces a separate immutable Typed-AST with resolved variable and method symbols. The optimizer and IR translator accept only Typed-AST nodes. The CLI's `--dump-ast` option therefore prints the Typed-AST, not the parser AST. Both executable backends then consume the same verified LemonIR, which keeps the compiler from having two separate semantic interpretations of the language.

## Front End

| Package | Responsibility |
|---|---|
| `site.ilemon.lexer` | Hand-written lexer with source positions, comments, numeric literals, strings, keywords, and operators. |
| `site.ilemon.parser` | Recursive-descent parser that builds source-level AST and enforces the single-class file shape. |
| `site.ilemon.source` | Immutable end-exclusive `SourceSpan` shared by tokens, both AST layers, optimization, and LemonIR instructions. |
| `site.ilemon.ast` | Syntax-only source AST. Parser nodes contain source structure and declared types, but no inferred types or resolved symbols. |
| `site.ilemon.semantic` | Builds Typed-AST while performing symbol resolution, type checking, use-before-assignment checks, return-path checks, loop-depth checks, and printf validation. |
| `site.ilemon.typedast` | Immutable semantic tree with non-null expression types, resolved variable/method symbols, and the semantic-only `ErrorType`. |
| `site.ilemon.optimizer` | Conservative Typed-AST constant folding, boolean simplification, algebraic identities, and constant branch simplification. |

## LemonIR

`site.ilemon.ir` is the main middle layer. It contains:

| Class | Role |
|---|---|
| `IrProgram`, `IrFunction`, `IrBlock`, `IrInstruction` | Program, function, basic-block, and instruction model. |
| `IrType`, `IrValue`, `IrOpcode` | Typed IR vocabulary. |
| `AstToIrTranslator` | Lowers optimized Typed-AST into LemonIR. |
| `IrVerifier` | Checks IR shape, operand counts, types, control flow, calls, arrays and returns; instruction failures include source spans when available. |
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

### Runtime resource limits

The VM carries two resource limits. Both exist only to make a runaway program fail
instead of hanging or exhausting memory; neither is part of the language semantics,
and the JVM backend has no counterpart to either.

| Limit | Default | Flag | Behaviour |
|---|---|---|---|
| Instructions executed | 100,000,000 (~2s) | `--vm-instruction-limit N` (`0` = unlimited) | Clean diagnostic naming the source line where execution stopped. |
| Runtime stack slots | 1,048,576 max | `--vm-stack-size N` | Stack starts at 4096 slots and **grows on demand**; the max only bounds runaway recursion. |

Because any finite instruction limit makes the two backends diverge on a long enough
program, strict equivalence requires `--vm-instruction-limit 0`. The default is a
deliberate trade: it is high enough that realistic programs are unaffected (a 300,000
iteration loop uses well under 2% of it) while still turning an infinite loop into a
fast failure rather than a hang in CI.

Runtime faults — division by zero, array bounds, stack overflow, instruction limit —
carry the source span of the failing instruction, because `IrToVmTranslator` stamps each
LemonIR instruction's span onto every VM instruction it expands to. The CLI reports them
as `runtime error: 行 N, 列 M（指令 X，PC=n）: ...`, flushes the output produced before the
fault, and exits non-zero. No Java stack trace reaches the user.

## Legacy JVM Translator

`site.ilemon.codegen.TranslatorVisitor` is the older direct AST-to-JVM-instruction translator. It receives semantic information through an explicit `SemanticResult` side table instead of mutating parser nodes. It is still useful as reference material and is still covered by tests, but the CLI main path now goes through Typed-AST and LemonIR.

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
| `TypedAstSeparationTest` | Source AST isolation, Typed-AST immutability, resolved-type invariants, and semantic-only error typing. |
| `SourceSpanPropagationTest` | End-exclusive token ranges and propagation through Source AST, Typed-AST, optimization, and LemonIR. |

The current baseline is 349 passing tests and 86 root examples matched against
`examples/example-output-manifest.tsv`. GitHub Actions runs `mvn -B clean test`
on JDK 8 for pushes and pull requests.

## Non-Main Source Trees

`dragon-book-front/` is a Dragon Book style reference front end.

`tools/native-experiment/` is an archived Windows x86-64 native backend experiment. It is intentionally outside the Maven source tree and should not be treated as production code without new tests, build wiring, and output isolation under `target/`.
