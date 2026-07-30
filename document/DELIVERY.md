# LemonC JVM Compiler Delivery

> Status: current JVM delivery note, verified against the 2026-07-28 source tree. LemonC also has
> a Typed-AST layer, typed LemonIR, and a LemonVM backend. For the whole-project architecture, see
> [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md).

This delivery scope is the JVM backend compiler. Experimental native/x86 work is kept under
`tools/native-experiment/` and is not part of the main Maven build or scoring baseline.

## Scope

The JVM compiler currently covers:

- lexical analysis, parsing, semantic analysis, IR translation, and Jasmin/JVM bytecode generation
- `int`, `float`, `double`, and `bool`
- arrays
- `if`, `while`, `for`, `break`, and `continue`
- method calls
- `void` methods
- block-local declarations, scalar declaration initializers, and `return;`
- non-`void` return-path checking for statically decidable paths
- `printf` with `%d` and `%f`
- end-exclusive source spans propagated from tokens through LemonIR instructions

Generated `.il` and `.class` files are written to `target/lemonc/`.

## Verification

Run the full clean validation:

```bash
mvn -e clean test
```

Expected result:

```text
Tests run: 349, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The example manifest currently contains 85 root programs. `AllExamplesJvmTest` compiles and
executes all 85 on the JVM, while the VM and backend-equivalence suites validate the same manifest.
GitHub Actions runs `mvn -B clean test` on JDK 8 for pushes and pull requests.

The integration tests compile Lemon programs through the full pipeline, assemble the generated
Jasmin IL, execute the JVM class, and compare stdout for representative examples, including:

- `Fib.lemon`
- float and double arithmetic examples
- boolean branch-combination examples
- complex `if` examples
- finite loop examples
- nested loops with `break` and `continue`
- `void` method calls
- `ReliabilityCanary.lemon`, which combines int/float/double/bool, arrays, recursion,
  method calls, void calls, discarded return values, loops, `break`, `continue`, and `printf`
- literal and mixed-type `printf`
- array output and bubble sort output

Negative tests cover:

- undefined variables and methods
- type mismatches
- `break` and `continue` outside loops
- invalid `main` return type
- `void` method used as an expression
- missing return paths in non-`void` methods
- conservative return-path boundaries, including `if/else` success and `while` non-guarantees
- `printf` argument count, type, and unsupported placeholder errors

## Known Limits

- `printf` supports `%d` for `int` and `%f` for `float`/`double`; `%s` is intentionally unsupported.
- `return;` is supported for `void` methods; returning no value from a non-`void` method is rejected.
- The return-path check is conservative: `if/else` branches are checked, while loops are not treated as guaranteed-return paths.
- The native/x86 prototype is experimental and excluded from the JVM compiler delivery.

## Hygiene

Root-level generated files are not part of delivery. Build and scratch outputs are ignored through
`.gitignore`, including:

- `target/`
- `*.class`
- `*.il`
- `out.txt`
- `errors.txt`
- `auto_fix.py`
- `fix_*.py`
- `test_tmp/`
