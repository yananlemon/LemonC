# LemonC 编译器项目总结

> 当前状态说明：本文是早期项目总结，主要描述 JVM 字节码后端。当前源码已经发展为 shared LemonIR + JVM/LemonVM 双后端架构，测试规模和语言能力也已经扩展。当前实现请以 `docs/ARCHITECTURE.md`、`docs/LEMONC_FEATURES.md` 和源码为准。

## 📋 项目概述

LemonC 是一个基于 Java 实现的教学型 C-like 编译器，当前主线会将自定义 Lemon 语言降低到 typed LemonIR，再分别生成 JVM 字节码和 LemonVM 字节码，最终通过 JVM 与自研 LemonVM 双后端执行并比对输出。

```
.lemon
  -> Lexer
  -> Parser
  -> SemanticVisitor
  -> AstOptimizer
  -> LemonIR + IrVerifier
  -> JVM backend: IrToJvmTranslator -> ByteCodeGenerator -> Jasmin -> .class
  -> VM backend: IrToVmTranslator -> LemonVM
```

---

## 🏗️ 项目架构

```
src/main/java/site/ilemon/
├── compiler/          # 编译器主入口
│   └── LemonC.java
├── lexer/             # 词法分析器
│   ├── Lexer.java     # DFA 状态机实现
│   ├── LexerState.java
│   ├── Token.java
│   └── TokenKind.java
├── parser/            # 语法分析器
│   └── Parser.java    # 递归下降解析器
├── ast/               # 抽象语法树
│   └── Ast.java       # AST 节点定义
├── semantic/          # 语义分析
│   ├── SemanticVisitor.java
│   └── MethodVarTable.java
├── optimizer/         # AST 优化
│   └── AstOptimizer.java
├── ir/                # typed LemonIR、IR 校验、双后端 lowering
│   ├── AstToIrTranslator.java
│   ├── IrVerifier.java
│   ├── IrToJvmTranslator.java
│   └── IrToVmTranslator.java
├── codegen/           # 代码生成
│   ├── TranslatorVisitor.java  # 旧 AST → JVM 指令路径，保留作测试/参考
│   ├── ByteCodeGenerator.java  # JVM 指令 IR → Jasmin
│   ├── Visitor.java
│   └── ast/           # JVM 指令 IR
├── vm/                # LemonVM 运行时
│   ├── LemonVm.java
│   ├── VmBytecodeParser.java
│   └── RuntimeStack.java
└── visitor/           # Visitor 模式接口
    ├── ISemanticVisitor.java
    └── IElement.java
```

---

## 🔑 核心技术点

### 1. 词法分析器 (Lexer) - DFA 状态机

```java
// 状态转移函数 δ(state, char) -> newState
private LexerState getNextState(LexerState state, char c) {
    switch (state) {
        case START:
            if (Character.isLetter(c)) return LexerState.IN_ID;
            if (Character.isDigit(c)) return LexerState.IN_NUM;
            if (c == '"') return LexerState.IN_STRING;
            // ...
        case IN_STRING:
            if (c == '"') return LexerState.DONE;
            return LexerState.IN_STRING;
        // ...
    }
}
```

**状态转移图：**
```
        letter          digit           "
START ─────────▶ IN_ID    START ─────▶ IN_NUM    START ─────▶ IN_STRING
  │                │                     │                       │
  │   non-letter   │      non-digit      │          "            │
  │◀───────────────┘◀─────────────────────┘◀──────────────────────┘
  ▼                                                              ▼
DONE                                                           DONE
```

### 2. 语法分析器 (Parser) - 递归下降

采用 **LL(1) 递归下降**解析，每个非终结符对应一个解析方法：

```java
// Program → MainClass
public Ast.Program.T parse() {
    return parseProgram();
}

// MainClass → class ID { MethodList }
private Ast.MainClass.T parseMainClass() {
    eat(TokenKind.Class);
    String classId = currentToken.lexeme;
    eat(TokenKind.Id);
    eat(TokenKind.Lbrace);
    ArrayList<Ast.Method.T> methods = parseMethodList();
    eat(TokenKind.Rbrace);
    return new Ast.MainClass.MainClassSingle(classId, null, methods);
}
```

### 3. Visitor 模式 - 双重分派

**教科书式实现**：AST 节点实现 `accept` 方法，实现真正的双重分派：

```java
// AST 节点
public static class Add extends T {
    @Override
    public void accept(ISemanticVisitor v) {
        v.visit(this);  // 双重分派的关键
    }
}

// Visitor 中的分发
@Override
public void visit(Expr.T obj) {
    obj.accept(this);  // 一行代码替代 20+ 行 instanceof 链
}
```

**对比：**
| 方式 | instanceof 链 | 双重分派 |
|------|--------------|---------|
| 新增节点 | 需改 Visitor | 只需在节点中实现 accept |
| 编译检查 | 无 | 漏实现会报错 |
| 代码量 | 大量 if-else | 简洁 |

### 4. 布尔表达式翻译 - 回填技术

遵循龙书的 **SDT (语法制导翻译)** 规则。当前实现不再把 `trueList` / `falseList` 存在 AST 节点上，而是由 `translateCondition()` 返回局部的 `BoolCode`：

```java
private static class BoolCode {
    final List<Integer> trueList;
    final List<Integer> falseList;
}

private BoolCode translateCondition(Expr.T expr) {
    if (expr instanceof Expr.And) {
        Expr.And and = (Expr.And) expr;
        BoolCode left = translateCondition(and.getLeft());
        Label rightBegin = new Label();
        emit(new Ast.Stmt.LabelJ(rightBegin));
        backpatch(left.trueList, rightBegin);
        BoolCode right = translateCondition(and.getRight());
        return new BoolCode(right.trueList, merge(left.falseList, right.falseList));
    }
    // comparisons, !, ||, true/false and value materialization omitted
}
```

**短路求值示例：**
```
if (a > 0 && b < 10) { ... }

生成代码：
    iload a
    ldc 0
    if_icmpgt L1    ; a > 0 则跳到 L1
    goto L_false    ; 否则短路，直接跳到 false
L1:
    iload b
    ldc 10
    if_icmplt L_true
    goto L_false
L_true:
    ; then 分支
L_false:
    ; else 分支
```

### 5. 控制流翻译

**If 语句：**
```java
/**
 * S -> if(E) S1 else S2
 * S.code := E.code || gen(E.true':') || S1.code 
 *        || gen('goto' S.next) || gen(E.false':') || S2.code
 */
@Override
public void visit(Stmt.If obj) {
    Label trueLabel = new Label();
    Label falseLabel = new Label();
    Label nextLabel = new Label();

    BoolCode condition = translateCondition(obj.getCondition());
    backpatch(condition.trueList, trueLabel);
    backpatch(condition.falseList, falseLabel);

    emit(new Ast.Stmt.LabelJ(trueLabel)); // E.true:
    this.visit(obj.getThenStmt());       // S1.code
    emit(new Ast.Stmt.Goto(nextLabel));  // goto S.next

    emit(new Ast.Stmt.LabelJ(falseLabel)); // E.false:
    if (obj.getElseStmt() != null) {
        this.visit(obj.getElseStmt());   // S2.code
    }
    emit(new Ast.Stmt.LabelJ(nextLabel)); // S.next:
}
```

---

## 📊 支持的语言特性

| 特性 | 支持情况 |
|------|---------|
| 数据类型 | int, float, double, bool, void；字符串主要作为 printf 字面量 |
| 数组 | int[], float[], double[], bool[]，支持索引访问、赋值、`.length` |
| 算术运算 | +, -, *, /, %, 一元 - |
| 比较运算 | >, <, >=, <=, ==, != |
| 逻辑运算 | &&, \|\|, ! (短路求值) |
| 控制流 | if-else, while, for, break, continue |
| 函数 | 定义、调用、递归、参数、返回值、void 方法 |
| 输出 | printf, printLine |
| 后端 | JVM 字节码、LemonVM 字节码与解释执行 |

---

## 🎯 示例程序

**九九乘法表 (MulTable.lemon)：**
```c
class MulTable {
    void main() {
        int i; int j;
        i = 1;
        while (i < 10) {
            j = 1;
            while (j < 10) {
                printf("%d*%d=%d\t", i, j, i*j);
                j = j + 1;
            }
                printLine();
            i = i + 1;
        }
    }
}
```

**编译运行：**
```bash
java -jar LemonC-0.1-beta-jar-with-dependencies.jar MulTable.lemon
java MulTable
```

**输出：**
```
1*1=1   1*2=2   1*3=3   ...
2*1=2   2*2=4   2*3=6   ...
...
9*1=9   9*2=18  9*3=27  ... 9*9=81
```

---

## 🔧 历史优化内容

1. **实现双重分派 Visitor 模式** - 为所有 AST 节点添加 `accept` 方法
2. **补全比较运算符** - 实现 `==`, `!=`, `>=`, `<=`
3. **修复 ByteCodeGenerator** - 类型判断 bug、重复代码、动态计算 stack/locals
4. **修复 Lexer 字符串处理** - 正确去除首尾引号
5. **更新 pom.xml** - 修复 assembly 插件配置
6. **添加测试用例** - 当前干净测试规模为 253 个测试全部通过

---

## 📚 参考资料

- 《编译原理》(龙书) - 第 6 章 中间代码生成
- JVM 规范 - 字节码指令集
- Jasmin 汇编器文档
