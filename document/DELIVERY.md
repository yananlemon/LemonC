# LemonC JVM Compiler Delivery

> Current note: LemonC now also has a typed LemonIR layer and a LemonVM backend. This file records
> the JVM delivery scope; for the current whole-project architecture, see `docs/ARCHITECTURE.md`.

This delivery scope is the JVM backend compiler. Experimental native/x86 work is kept under
`tools/native-experiment/` and is not part of the main Maven build or scoring baseline.

## Scope

The JVM compiler currently covers:

- lexical analysis, parsing, semantic analysis, IR translation, and Jasmin/JVM bytecode generation
- `int`, `float`, `double`, and `bool`
- arrays
- `if`, `while`, `break`, and `continue`
- method calls
- `void` methods
- non-`void` return-path checking for statically decidable paths
- `printf` with `%d` and `%f`

Generated `.il` and `.class` files are written to `target/lemonc/`.

## Verification

Run the full clean validation:

```bash
mvn -e clean test
```

Expected result:

```text
Tests run: 253, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

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

`Iteration05.lemon` is intentionally not used as a stdout golden test because the source program
contains an infinite `while (true)` loop. It remains a compile-pipeline example, not a runtime
termination contract.

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
- `return;` for empty `void` return is not supported by the current parser.
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
