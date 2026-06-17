# LemonC

**LemonC is a teaching-oriented C-like compiler written in Java.**

It compiles Lemon source code to real JVM `.class` files through lexical analysis, recursive descent parsing, semantic analysis, AST optimization, backpatching-based control-flow translation, Jasmin assembly, and bytecode generation.

LemonC 是一个面向编译原理教学与实践的小型 C-like 编译器。它不是只停留在 AST 或三地址码展示层，而是把 `.lemon` 源程序真正编译成 JVM 字节码，并用 JVM 执行结果做端到端回归验证。

<p align="center">
  <img src="./docs/assets/lemonc-pipeline.png" alt="LemonC compiler pipeline" width="100%">
</p>

```text
Java 8+ | Maven | JVM bytecode | 176 tests passing | 82 examples | MIT License
```

## Why LemonC

| What you get | Why it matters |
|---|---|
| Complete compiler pipeline | Lexer, parser, semantic analyzer, optimizer, IR translator, bytecode generator |
| Real JVM execution | Examples compile to `.class` and run on a standard JVM |
| Classic compiler theory | Recursive descent parsing, Visitor-based semantic analysis, backpatching, stack-machine codegen |
| Teaching-friendly visibility | CLI can dump tokens, AST, and JVM IR |
| Regression confidence | 82 example programs are checked against real JVM stdout |
| Small enough to read | A compact codebase for students who want to understand a whole compiler |

## At A Glance

The whole project is intentionally small enough to read, but complete enough to demonstrate a real compiler pipeline from source code to JVM execution.

## 30-Second Demo

Source: [examples/OptimizationTest.lemon](examples/OptimizationTest.lemon)

```c
class OptimizationTest {
    void main() {
        int a;
        int b;
        bool c;
        a = (2 + 3) * 4;
        b = (a * 1) + 0;
        c = (1 < 2) && true;
        if (c) {
            printf("a=%d,b=%d\n", a, b);
        } else {
            printf("bad\n");
        }
        while (false) {
            printf("dead\n");
        }
    }
}
```

Compile, inspect, and run:

```bash
mvn clean package

java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar \
  examples/OptimizationTest.lemon --dump-tokens --dump-ast --dump-ir

java OptimizationTest
```

Real JVM output:

```text
a=20,b=20
```

The same example also demonstrates constant folding, algebraic simplification, boolean folding, dead `while(false)` removal, and the CLI inspection pipeline.

## What Lemon Supports

| Category | Features |
|---|---|
| Types | `int`, `float`, `double`, `bool`, `void` |
| Arrays | `int[]`, `float[]`, `double[]`, indexed access, indexed assignment, `.length` |
| Arithmetic | `+`, `-`, `*`, `/`, `%`, unary `-` |
| Numeric widening | `int -> float`, `int -> double`, `float -> double` |
| Comparison | `>`, `<`, `>=`, `<=`, `==`, `!=` |
| Boolean logic | `true`, `false`, `!`, `&&`, `||`, short-circuit control flow |
| Control flow | `if/else`, `while`, `for`, `break`, `continue`, nested loops |
| Methods | parameters, return values, `void` methods, recursive calls, expression calls |
| Output | `printf`, `printLine`, `%d`, `%f`, `\n`, `\t` |
| Optimization | constant folding, boolean folding, algebraic simplification, constant branch simplification |
| Diagnostics | parse and semantic exceptions with source line context |

For the complete feature list with source code and real outputs, read [docs/LEMONC_FEATURES.md](docs/LEMONC_FEATURES.md).

## Compiler Architecture

```mermaid
flowchart TB
    subgraph Frontend
        L["site.ilemon.lexer<br/>DFA lexer"]
        P["site.ilemon.parser<br/>recursive descent parser"]
        S["site.ilemon.semantic<br/>symbol table and type checking"]
    end

    subgraph MiddleEnd
        O["site.ilemon.optimizer<br/>AST optimizer"]
    end

    subgraph Backend
        T["site.ilemon.codegen.TranslatorVisitor<br/>AST to JVM IR"]
        B["site.ilemon.codegen.ByteCodeGenerator<br/>Jasmin IL writer"]
        J["jasmin.Main<br/>IL to .class"]
    end

    L --> P --> S --> O --> T --> B --> J
```

| Module | Core classes | Responsibility |
|---|---|---|
| `site.ilemon.lexer` | `Lexer`, `Token`, `TokenKind` | Tokenize Lemon source code |
| `site.ilemon.parser` | `Parser` | Build frontend AST with recursive descent parsing |
| `site.ilemon.ast` | `Ast` | Define source-level expressions, statements, types, methods, and programs |
| `site.ilemon.semantic` | `SemanticVisitor`, `MethodVarTable`, `Symbol` | Type checking, declaration checks, assignment checks, return checks |
| `site.ilemon.optimizer` | `AstOptimizer` | Perform safe AST-level simplifications |
| `site.ilemon.codegen` | `TranslatorVisitor`, `ByteCodeGenerator` | Translate AST to JVM IR and write Jasmin assembly |
| `site.ilemon.codegen.ast` | `Ast`, `Label` | Define backend JVM instruction-level IR |
| `site.ilemon.compiler` | `LemonC`, `AstPrinter`, `IrPrinter` | CLI entrypoint and teaching-friendly dumps |

## Backpatching In Action

LemonC uses classic backpatching for boolean expressions and flow-control statements. Boolean code generation maintains pending jump lists instead of eagerly materializing `0` or `1`.

For:

```c
if (a < b || c < d && e < f) {
    printf("yes\n");
} else {
    printf("no\n");
}
```

The conceptual control-flow shape is:

```mermaid
flowchart LR
    A["a < b"] -- true --> T["then branch"]
    A -- false --> C["c < d"]
    C -- false --> F["else branch"]
    C -- true --> E["e < f"]
    E -- true --> T
    E -- false --> F
```

The implementation follows the textbook rules:

```text
E1 || E2:
  backpatch(E1.falseList, E2.entry)
  E.trueList  = merge(E1.trueList, E2.trueList)
  E.falseList = E2.falseList

E1 && E2:
  backpatch(E1.trueList, E2.entry)
  E.trueList  = E2.trueList
  E.falseList = merge(E1.falseList, E2.falseList)
```

This makes the project useful for students studying syntax-directed translation and control-flow generation.

## JVM Output Is Tested, Not Assumed

Every root example under [examples](examples) is compiled and executed by [AllExamplesJvmTest.java](src/test/java/AllExamplesJvmTest.java):

<p align="center">
  <img src="./docs/assets/lemonc-test-loop.png" alt="LemonC end-to-end JVM regression loop" width="100%">
</p>

Run the suite:

```bash
mvn test
```

Current coverage:

```text
Tests run: 176, Failures: 0, Errors: 0, Skipped: 0
82 root examples verified by real JVM execution
```

## More Real Examples

### Numeric Widening, for, break, continue, arrays

Source: [examples/LanguageFeatureTest.lemon](examples/LanguageFeatureTest.lemon)

```text
sum=8
neg=-8
f=3.5,d=4.5
call=3.0,7.0
arr=2
```

### Nested loops

Source: [examples/NestedLoops.lemon](examples/NestedLoops.lemon)

```text
  inner run i=1, j=1
  inner break on 2
outer continue skip 2
  inner run i=3, j=1
  inner break on 2
```

### Floating-point NaN comparison

Source: [examples/NaNCompareTest.lemon](examples/NaNCompareTest.lemon)

```text
flt_lt=0
flt_lte=0
flt_gt=0
flt_gte=0
flt_eq=0
flt_neq=1
dbl_lt=0
dbl_lte=0
dbl_gt=0
dbl_gte=0
dbl_eq=0
dbl_neq=1
```

### Recursive Fibonacci

Source: [examples/Fib.lemon](examples/Fib.lemon)

```text
递归计算斐波那契数列，一年后总共有144对兔子
循环计算斐波那契数列，一年后总共有144对兔子
```

## Quick Start

Requirements:

```text
JDK 1.8+
Maven 3.3+
```

Build directly. The Jasmin dependency is resolved from the project-local Maven repository under `jars/maven-repo`.

```bash
mvn clean package
```

Compile and run a Lemon program:

```bash
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/Fib.lemon
java Fib
```

Inspect compiler stages:

```bash
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar \
  examples/ModTest.lemon --dump-tokens --dump-ast --dump-ir
```

## Lemon Language In One Page

```c
class Demo {
    int fib(int n) {
        int result;
        if (n < 3) {
            result = 1;
        } else {
            result = fib(n - 1) + fib(n - 2);
        }
        return result;
    }

    void main() {
        int i;
        int sum;
        int arr[3];
        sum = 0;

        for (i = 0; i < 3; i = i + 1) {
            arr[i] = i + 1;
            sum = sum + arr[i];
        }

        if (sum == 6 && fib(6) == 8) {
            printf("ok=%d\n", sum);
        } else {
            printf("bad\n");
        }
    }
}
```

## Grammar Snapshot

```bnf
<program>       ::= "class" <id> "{" <method>* "}"
<method>        ::= <type> <id> "(" <params>? ")" "{" <varDecl>* <stmt>* "}"
                  | "void" "main" "(" ")" "{" <varDecl>* <stmt>* "}"
<params>        ::= <type> <id> ("," <type> <id>)*
<varDecl>       ::= <type> <id> ";"
                  | <type> <id> "[" <integer> "]" ";"
<type>          ::= "int" | "float" | "double" | "bool" | "void"
<stmt>          ::= <id> "=" <expr> ";"
                  | <id> "[" <expr> "]" "=" <expr> ";"
                  | <id> "(" <args>? ")" ";"
                  | "if" "(" <expr> ")" <stmt> ("else" <stmt>)?
                  | "while" "(" <expr> ")" <stmt>
                  | "for" "(" <forInit> ";" <expr> ";" <forUpdate> ")" <stmt>
                  | "break" ";"
                  | "continue" ";"
                  | "{" <stmt>* "}"
                  | "return" <expr> ";"
                  | "printf" "(" <string> ("," <expr>)* ")" ";"
<expr>          ::= <andExpr> ("||" <andExpr>)*
<andExpr>       ::= <relExpr> ("&&" <relExpr>)*
<relExpr>       ::= <addExpr> ((">" | "<" | ">=" | "<=" | "==" | "!=") <addExpr>)*
<addExpr>       ::= <term> (("+" | "-") <term>)*
<term>          ::= <factor> (("*" | "/" | "%") <factor>)*
<forInit>       ::= <id> "=" <expr>
<forUpdate>     ::= <id> "=" <expr>
```

## Test Suite

| Test class | Count | Purpose |
|---|---:|---|
| `AllExamplesJvmTest` | 1 | Compile every root example to `.class`, run JVM, compare stdout |
| `AstOptimizerTest` | 5 | Verify AST optimization behavior |
| `ByteCodeGeneratorTest` | 13 | Verify JVM bytecode and stack/local metadata |
| `CompilerTest` | 69 | End-to-end compiler tests |
| `ErrorTest` | 40 | Negative parse and semantic tests |
| `LexerTest` | 18 | Lexer tests |
| `ParserTest` | 18 | Parser tests |
| `SemanticTest` | 1 | Semantic visitor smoke test |
| `TranslatorVisitorTest` | 11 | JVM IR translation tests |

## Repository Map

```text
src/main/java/site/ilemon
  ast/              source-level AST
  lexer/            tokenization
  parser/           recursive descent parser
  semantic/         symbol tables and type checking
  optimizer/        AST optimization
  codegen/          JVM IR and Jasmin generation
  compiler/         CLI, AST printer, IR printer

examples/           82 Lemon programs and output manifest
docs/               feature guide and review notes
tools/              native backend experiment, kept outside main source
src/test/java/      automated compiler tests
```

## Current Language Boundaries

LemonC intentionally keeps the language small:

| Boundary | Status |
|---|---|
| Identifier `_` | Supported in identifiers and class names |
| Multi-line comments | Supported with `/* ... */` |
| Block scope | Blocks do not introduce independent local scopes |
| String variables | Strings are primarily `printf` literals |
| Object model | Single-class teaching language, not full Java |

## Roadmap

The codebase now has enough substance for a serious teaching compiler. The next milestones are:

1. Keep GitHub Actions green and show a real CI badge.
2. Publish `v0.2.0` release with a ready-to-run jar.
3. Add an English tutorial: "Build a JVM compiler from scratch with LemonC".
4. Add visual snapshots of token, AST, and IR dumps.
5. Add CFG/data-flow optimization as the next advanced chapter.
6. Add GitHub topics: `compiler`, `compiler-design`, `jvm`, `bytecode`, `parser`, `semantic-analysis`, `backpatching`, `teaching`.

## License

LemonC is released under the [MIT License](LICENSE).
