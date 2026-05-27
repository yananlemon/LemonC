# Native Experiment

This directory contains an archived Windows x86-64 native backend prototype.

The Java prototype is intentionally kept outside any `src/main/java` tree so it is not treated as
active source code by IDEs, build tooling, or project scoring. It is reference material only and is
not part of the current Maven build or automated test suite.

Contents:

- `reference/native-backend-java/site/ilemon/codegen/X86_64Generator.java`
- `reference/native-backend-java/site/ilemon/compiler/LemonCNative.java`
- `examples/HelloNative.lemon`
- `HelloNative.asm`
- `compile_native.bat` archived launcher note
- `BUILD_NATIVE.md` archived build notes

Before promoting this prototype back into the main source tree, it needs dedicated tests, clean
UTF-8 documentation, output isolation under `target/`, and clear support boundaries.
