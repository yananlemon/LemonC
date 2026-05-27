# LemonC 编译器 代码评审报告

> **评审基准**：经典编译原理（Dragon Book / 虎书）  
> **评审轮次**：共 3 轮，每轮均全量阅读源码  
> **评审时间**：2026-05-27  
> **评审模型**：Claude Sonnet 4.6

---

## 目录

1. [评分总览](#1-评分总览)
2. [三轮进化轨迹](#2-三轮进化轨迹)
3. [第一轮评审（基准版）](#3-第一轮评审基准版)
4. [第二轮评审（重构回填算法）](#4-第二轮评审重构回填算法)
5. [第三轮评审（修复语法分析与 NaN 语义）](#5-第三轮评审修复语法分析与-nan-语义)
6. [毕业设计视角评分](#6-毕业设计视角评分)
7. [项目架构总览](#7-项目架构总览)
8. [各维度终态分析](#8-各维度终态分析)
9. [仍存在的问题清单](#9-仍存在的问题清单)
10. [后续改进建议](#10-后续改进建议)

---

## 1. 评分总览

### 编译原理维度评分（满分 100）

| 维度 | 满分 | 第一轮 | 第二轮 | **第三轮（终态）** |
|------|:----:|:------:|:------:|:-----------------:|
| 词法分析（Lexical Analysis） | 10 | 7 | 7 | **7** |
| 语法分析（Syntax Analysis） | 15 | 10 | 10 | **12** |
| AST 设计（AST Design） | 10 | 6 | 7 | **8** |
| 语义分析（Semantic Analysis） | 15 | 11 | 11 | **11** |
| 中间代码生成（IR Generation） | 15 | 9 | 13 | **14** |
| 目标代码生成（Target Code Gen） | 10 | 7 | 7 | **7** |
| 优化（Optimization） | 10 | 2 | 2 | **2** |
| 错误处理（Error Handling） | 10 | 6 | 6 | **6** |
| 工程质量（Engineering Quality） | 5 | 3 | 3 | **4** |
| **总分** | **100** | **61** | **66** | **71** |

### 毕业设计视角评分（超一流大学，满分 100）

| 维度 | 权重 | 得分 |
|------|:----:|:----:|
| 工作量与完整度 | 20 | 17 |
| 技术深度 | 25 | 18 |
| 创新性 | 20 | 10 |
| 实验验证 | 15 | 12 |
| 工程规范与文档 | 10 | 7 |
| 答辩表现（推测） | 10 | 8 |
| **毕业设计综合得分** | **100** | **72（良好）** |

---

## 2. 三轮进化轨迹

```
第一轮 (61分)
│
│  ✅ 从 Ast.Expr.T 移除 trueList / falseList 字段
│  ✅ 引入 BoolCode 内部类 + translateCondition() 递归回填
│  ✅ 引入 backpatch(list, label) 按指令下标回填
│  ✅ 删除 import java.util.UUID，消灭 UUID 临时变量
│  ✅ visit(Expr.True/False) 简化为 ldc 1 / ldc 0
│
▼
第二轮 (66分)  [+5]
│
│  ✅ Parser 删除 isValDecl 全局 flag，引入 isVarDeclarationStart()
│  ✅ Expr.And 删除重复 lineNum 字段（修复字段遮蔽 bug）
│  ✅ 引入 fcmpg / dcmpg，修复浮点比较 NaN 语义
│  ✅ processCallArgument 签名清理（移除冗余 Expr.Call 参数）
│
▼
第三轮 (71分)  [+5]
```

---

## 3. 第一轮评审（基准版）

### 总分：61 / 100

### 3.1 词法分析 —— 7/10

**亮点**
- 教科书级 DFA 实现：`LexerState` 枚举 + `getNextState()` 转移函数 δ
- 关键字表用 `HashMap` 静态初始化，O(1) 查找
- `shouldConsumeOnDone()` 正确区分"消费字符"与"退回字符"
- 行号精确跟踪

**问题**
- ❌ 不支持多行注释 `/* ... */`
- ❌ 标识符不允许下划线 `_`（`Character.isLetter` 不含 `_`）
- ❌ 词法层面无字符串转义处理（`\n`、`\t` 透传）
- ❌ `Token` 类字段（`lexeme`、`kind`、`lineNumber`）全为 `public`

### 3.2 语法分析 —— 10/15

**亮点**
- 递归下降 + LL(2) 前瞻，逻辑清晰
- 运算符优先级完整正确：`||` < `&&` < 关系 < 加减 < 乘除余
- 每个文法产生式均有 BNF 注释
- 错误消息包含行号与期望 token

**问题（主要）**
- ❌ **`isValDecl` 全局 flag**：用布尔副作用区分"变量声明"和"赋值语句"，极其脆弱
  ```java
  // 旧版：通过 isValDecl 副作用控制 parseVarDeclares 循环
  private boolean isValDecl;
  while(isTypeToken(look.kind)){
      parseDeclare(); // 内部操作 isValDecl
      if (!isValDecl) break; // 危险！
  }
  ```
- ❌ 变量声明只能在方法顶部，不支持任意位置声明
- ❌ 无 `for` 循环
- ❌ 无一元负号（`-x` 需写为 `0 - x`）
- ❌ 仅支持单类文件

### 3.3 AST 设计 —— 6/10

**亮点**
- Visitor 模式完整：`ISemanticVisitor` 接口 + `accept()` 全覆盖
- `TypeKind` 枚举替代字符串比较

**问题（主要）**
- ❌ **关注点严重混合**：`Ast.Expr.T` 基类携带 `trueList`/`falseList`（代码生成数据），`Ast.Stmt.T` 基类携带 `breakList`/`continueList`
  ```java
  // 旧版：前端 AST 节点被代码生成污染
  public static abstract class T {
      public DoublyLinkedList<Label> trueList = new DoublyLinkedList<>();
      public DoublyLinkedList<Label> falseList = new DoublyLinkedList<>();
      // ...
  }
  ```
- ❌ `Ast.Expr.And` 重复声明 `lineNum` 字段（遮蔽父类同名字段）
- ❌ `codegen.ast.Ast` 字段全为 `public`（无封装）
- ❌ 前端/后端两个同名 `Ast` 类容易混淆

### 3.4 语义分析 —— 11/15

**亮点**
- 类型检查全面（算术/比较/逻辑/赋值/方法调用）
- **使用前赋值检查**（`currMethodLocalVar` 集合，if/else 分支后正确合并）
- **控制流分析** `statementsMustReturn()`：正确识别所有路径有 return
- 方法重复定义、参数个数/类型检查
- `printf` 格式串占位符与参数类型验证
- 循环深度跟踪，`break`/`continue` 不在循环内报错

**问题**
- ❌ 无嵌套块作用域（方法内仅单层 `MethodVarTable`）
- ❌ `isMatch` 不支持 `int → float/double` 隐式提升
- ❌ `printf` 只支持 `%d`/`%f`

### 3.5 中间代码生成 —— 9/15

**亮点**
- 控制流基本正确（while/if/break/continue）
- 数组操作指令完整
- float/double 分离处理

**问题（主要）**
- ❌ **UUID 临时变量**：`UUID.randomUUID().toString()` 用作临时变量名
  ```java
  // 旧版：极不专业
  private String generateVarName(){
      return UUID.randomUUID().toString();
  }
  ```
- ❌ **布尔表达式回填污染 AST**：`trueList`/`falseList` 直接附加在 AST 节点上传递
- ❌ 浮点比较一律使用 `fcmpl`，NaN 语义不正确（应区分 `fcmpg`）
- ❌ `processCallArgument` 携带无意义 `Expr.Call` 参数

### 3.6 目标代码生成 —— 7/10

**亮点**
- Jasmin 汇编格式正确
- 动态计算 `.limit stack`（工作列表数据流分析）
- 动态计算 `.limit locals`
- 类型描述符（`I`/`F`/`D`/`V`/`[I` 等）正确
- double 使用 `ldc2_w`

**问题**
- ❌ 输出 Jasmin 文本再调用 `jasmin.Main` 汇编，引入外部依赖
- ❌ 字符串每次都占用一个新局部变量槽，浪费

### 3.7 优化 —— 2/10

几乎没有任何优化：

| 优化技术 | 状态 |
|----------|:----:|
| 常量折叠 | ❌ |
| 常量传播 | ❌ |
| 公共子表达式消除 | ❌ |
| 死代码消除 | ❌ |
| 循环不变量外提 | ❌ |
| 窥孔优化 | ❌ |
| 尾递归优化 | ❌ |

保留 2 分：`calculateMaxStack` 有实际数据流分析；布尔表达式短路求值（跳转语义）有一定优化意识。

### 3.8 错误处理 —— 6/10

**亮点**
- 异常层次清晰：`CompilerException → ParseException / SemanticException`
- 所有错误包含行号，格式统一

**问题**
- ❌ 遇到第一个错误即停止，无法一次报告多个错误
- ❌ 词法非法字符仅返回 `Unknown` token，无警告
- ❌ 无语法错误恢复机制

### 3.9 工程质量 —— 3/5

**亮点**
- 9 个测试类，覆盖全流程

**问题**
- ❌ `isValDecl` 全局状态产生隐式耦合
- ❌ `codegen.ast.Ast` 字段全 `public`
- ❌ `Expr.And` 重复字段
- ❌ `TranslatorVisitor.prog` 为 `public`

---

## 4. 第二轮评审（重构回填算法）

### 总分：66 / 100（较第一轮 +5）

### 核心变更

#### 变更 1：从 `Ast.Expr.T` 移除 `trueList`/`falseList` ✅ **架构级改进**

```java
// 旧版（污染 AST）：
public static abstract class T {
    public DoublyLinkedList<Label> trueList = new DoublyLinkedList<>();
    public DoublyLinkedList<Label> falseList = new DoublyLinkedList<>();
    private int lineNum;
    // ...
}

// 新版（干净的 AST）：
public static abstract class T {
    private int lineNum;
    public int getLineNum() { return this.lineNum; }
    public void setLineNum(int lineNum) { this.lineNum = lineNum; }
    public abstract void accept(ISemanticVisitor v);
}
```

前端 AST 节点彻底回归本职：仅承载语法结构与行号信息。这是分层设计原则的正确实践。

#### 变更 2：重构回填算法 ✅ **算法级改进**

引入 `BoolCode` 内部类，实现 Dragon Book §6.6 描述的经典回填算法：

```java
// 新版：标准回填算法
private static class BoolCode {
    final List<Integer> trueList;   // 需要回填 true 目标的指令下标列表
    final List<Integer> falseList;  // 需要回填 false 目标的指令下标列表

    BoolCode(List<Integer> trueList, List<Integer> falseList) { ... }
}

// 核心三元组：emitJump + backpatch + merge
private int emitJump(Ast.Stmt.T stmt) {
    emit(stmt);
    return this.stmts.size() - 1;  // 返回指令在列表中的下标
}

private void backpatch(List<Integer> list, Label target) {
    for (Integer index : list) {
        setJumpTarget(this.stmts.get(index), target);  // 回填跳转目标
    }
}

private List<Integer> merge(List<Integer> left, List<Integer> right) {
    // 合并两个链表
}

// And 翻译示例：E1 && E2
private BoolCode translateCondition(Expr.T expr) {
    if (expr instanceof Expr.And) {
        Expr.And and = (Expr.And) expr;
        BoolCode left = translateCondition(and.getLeft());
        Label rightBegin = new Label();
        emit(new Ast.Stmt.LabelJ(rightBegin));
        backpatch(left.trueList, rightBegin);   // E1.true -> 开始计算 E2
        BoolCode right = translateCondition(and.getRight());
        return new BoolCode(right.trueList, merge(left.falseList, right.falseList));
    }
    // ...
}
```

#### 变更 3：删除 UUID 临时变量 ✅

```java
// 旧版：
private String generateVarName(){
    return UUID.randomUUID().toString(); // 极不专业
}

// 新版：统一通过 emitBooleanValue() 物化布尔值
private void emitBooleanValue(Expr.T expr) {
    BoolCode code = translateCondition(expr);
    Label trueLabel = new Label();
    Label falseLabel = new Label();
    Label nextLabel = new Label();
    backpatch(code.trueList, trueLabel);
    backpatch(code.falseList, falseLabel);
    emit(new Ast.Stmt.LabelJ(trueLabel));
    emit(new Ast.Stmt.Ldc(1));
    emit(new Ast.Stmt.Goto(nextLabel));
    emit(new Ast.Stmt.LabelJ(falseLabel));
    emit(new Ast.Stmt.Ldc(0));
    emit(new Ast.Stmt.Goto(nextLabel));
    emit(new Ast.Stmt.LabelJ(nextLabel));
    this.type = new Ast.Type.Int();
}
```

### 各维度变化

| 维度 | 变化 | 原因 |
|------|:----:|------|
| AST 设计 | **+1**（6→7） | trueList/falseList 从 Expr.T 移除 |
| 中间代码生成 | **+4**（9→13） | BoolCode 回填架构重写，UUID 消除 |
| 其余维度 | 无变化 | 对应代码未改动 |

---

## 5. 第三轮评审（修复语法分析与 NaN 语义）

### 总分：71 / 100（较第二轮 +5）

### 核心变更

#### 变更 1：消灭 `isValDecl` hack ✅ **语法分析架构修复**

```java
// 旧版（全局 flag + 副作用控制）：
private boolean isValDecl;  // 危险的全局状态

private ArrayList<Ast.Declare.T> parseVarDeclares() {
    while(isTypeToken(look.kind)){
        parseDeclare();           // 内部通过 isValDecl 的副作用控制是否继续
        if (!isValDecl) break;    // 脆弱！依赖内部状态
    }
}

// 新版（LL(2) 前瞻，正确消歧）：
private boolean isVarDeclarationStart() {
    if (!isTypeToken(look.kind)) return false;
    Token id      = lexer.lookahead(1);   // 第 1 个前瞻 token
    Token afterId = lexer.lookahead(2);   // 第 2 个前瞻 token
    // 仅当匹配 "type id ;" 或 "type id [" 才认为是变量声明
    return id != null && id.kind == TokenKind.Id
        && afterId != null
        && (afterId.kind == TokenKind.Semicolon || afterId.kind == TokenKind.Lbracket);
}

private ArrayList<Ast.Declare.T> parseVarDeclares() {
    while(isVarDeclarationStart()){   // 干净、无副作用
        rs.add(parseDeclare());
    }
}
```

`isVarDeclarationStart()` 精确对应文法的 FIRST 集推导，是 LL 解析器消歧的正确方式。同时 `parseDeclare()` 本身也得到大幅简化——不再含任何 flag 操作。

#### 变更 2：修复 `Expr.And` 字段遮蔽 Bug ✅

```java
// 旧版（And 子类重复声明 lineNum，遮蔽父类同名字段）：
public static class And extends T {
    private Expr.T left, right;
    private int lineNum;               // ← BUG：遮蔽 T.lineNum
    public int getLineNum() { ... }    // ← 与父类同名方法语义不一致
    public void setLineNum(int n) { ... }
    // ...
}

// 新版（正确继承）：
public static class And extends T {
    private Expr.T left, right;
    // lineNum 已删除 —— 直接使用父类 T 中的字段
    public And(Expr.T left, Expr.T right, int lineNum) {
        this.setLeft(left);
        this.setRight(right);
        this.setLineNum(lineNum);  // 调用父类方法，正确
    }
}
```

#### 变更 3：修复浮点比较 NaN 语义 ✅ **正确性修复**

**背景**：JVM 规范定义了两条浮点比较指令：
- `fcmpl`：当任一操作数为 NaN 时，推入 **-1**（"less"）
- `fcmpg`：当任一操作数为 NaN 时，推入 **+1**（"greater"）

正确的选择取决于比较语义：对于 `a < b`，若 a 或 b 为 NaN，结果应为 `false`。此时需用 `fcmpg`（NaN→+1），后接 `if_icmplt`，+1 不满足 `< 0`，正确产生 false。

```java
// 旧版（一律使用 fcmpl，NaN 语义错误）：
emit(new Ast.Stmt.Fcmpl());

// 新版（根据运算符语义选择）：
private boolean usesCompareGreaterOnNaN(String op) {
    // < 和 <= 运算：NaN 应被视为"大于"，故用 fcmpg
    // > 和 >= 运算：NaN 应被视为"小于"，故用 fcmpl
    return "<".equals(op) || "<=".equals(op);
}

private BoolCode comparisonJumps(String op, Ast.Type.T operandType) {
    if (operandType instanceof Ast.Type.Float) {
        emit(usesCompareGreaterOnNaN(op)
            ? new Ast.Stmt.Fcmpg()   // < 和 <=
            : new Ast.Stmt.Fcmpl()); // > 和 >=
        // ...
    } else if (operandType instanceof Ast.Type.Double) {
        emit(usesCompareGreaterOnNaN(op)
            ? new Ast.Stmt.Dcmpg()
            : new Ast.Stmt.Dcmpl());
        // ...
    }
}
```

相应地，`codegen.ast.Ast` 新增 `Fcmpg`/`Dcmpg` 节点，`Visitor` 接口新增对应 `visit` 方法，`ByteCodeGenerator` 和 `stackDeltas()` 同步更新。

#### 变更 4：`processCallArgument` 签名清理 ✅

```java
// 旧版（携带无意义的 Expr.Call 参数，visit(Stmt.Call) 中构造垃圾对象）：
private void processCallArgument(Expr.Call call, Expr.T expr, Ast.Type.T expectedType)

// 调用处需要构造无意义对象：
Expr.Call targetObj = new Expr.Call(obj.getName(), obj.getInputParams(), ...);
processCallArgument(targetObj, expr, expectedType); // targetObj 完全没用到

// 新版（干净）：
private void processCallArgument(Expr.T expr, Ast.Type.T expectedType)
processCallArgument(expr, expectedType);
```

### 各维度变化

| 维度 | 变化 | 原因 |
|------|:----:|------|
| 语法分析 | **+2**（10→12） | isValDecl hack 完全消除 |
| AST 设计 | **+1**（7→8） | And.lineNum 字段遮蔽 bug 修复 |
| 中间代码生成 | **+1**（13→14） | NaN 语义修复 + 接口签名清理 |
| 工程质量 | **+1**（3→4） | 三处代码质量问题同步修复 |

---

## 6. 毕业设计视角评分

> 评审背景：超一流大学（清华、北大、中科大等）CS 系本科毕业设计答辩委员会视角

### 综合得分：72 / 100（良好）

```
超一流大学毕业设计评级标准：
┌─────────────────────────────────────────────────────┐
│  90-100  优秀  有创新点，实现精良，文档专业           │
│  80-89   良好  实现完整，技术扎实，略有创新           │
│  70-79   中等  实现基本完整，技术正确，创新不足        │ ← 本项目 (72)
│  60-69   及格  部分实现，基本原理正确                  │
│   <60  不及格                                         │
└─────────────────────────────────────────────────────┘
```

### 各维度打分

#### 工作量与完整度 —— 17/20

**充分肯定：**
- 6 阶段编译管线从零实现，无框架依赖，闭环到可运行的 JVM 字节码
- ~20 个 Java 类，70+ 个 `.lemon` 示例，9 个测试类
- 支持 int / float / double / bool / 一维数组，类型系统相对完整

**扣分：**
- 目标语言较窄（单类、无 `for`、无面向对象），与同等工作量项目相比功能集偏少

#### 技术深度 —— 18/25

正确实现了以下非平凡技术点：

| 技术点 | 对应理论 |
|--------|----------|
| DFA 状态机词法分析 | Dragon Book 第 3 章 |
| LL(2) 递归下降，isVarDeclarationStart 消歧 | Dragon Book 第 4 章 |
| 布尔表达式回填（BoolCode + backpatch） | Dragon Book §6.6 |
| 使用前赋值检查，if/else 分支合并 | 数据流分析基础 |
| JVM 操作数栈高度数据流分析 | Dragon Book 第 9 章（应用） |
| 浮点 NaN 语义（fcmpg vs fcmpl） | JVM 规范 §6.5 |

**扣分：**
- **零优化**：完整的编译器后端理应包含至少一个数据流优化 Pass
- 中间表示直接就是 Jasmin 文本，层次不够

#### 创新性 —— 10/20

这是与优秀毕业设计的最大差距。项目的所有技术点均可在 Dragon Book 中找到对应章节，是经典算法的高质量复现，**没有超出教材范畴的自主贡献**。

对比能在顶校拿优秀的编译器方向毕业设计，通常会有：
- 面向特定领域的新语言设计（DSL）
- 某个优化方向的实质性工作（如基于 SSA 的 GVN、循环变换）
- 新目标平台（RISC-V、WASM、LLVM IR）
- 与机器学习结合（神经网络推理编译优化）

本项目属于**「完成度高的课程大作业」**水平，在顶校答辩中很难被评为创新性强。

#### 实验验证 —— 12/15

**充分：**
- E2E 集成测试验证实际 JVM 运行输出
- `ErrorTest` 专门测试错误路径
- `AllExamplesJvmTest` 自动化比对输出

**不足：**
- 无性能基准测试，无与 gcc/javac 的代码质量对比
- 错误恢复能力从未被测试

#### 工程规范与文档 —— 7/10

项目有 `document/` 目录，包含 5 份说明文档，说明有完整书面材料。
代码工程问题：`codegen.ast.Ast` 字段全为 `public`，`TranslatorVisitor.prog` 字段 `public`。

#### 答辩表现（推测）—— 8/10

- ✅ 能清晰解释 DFA、递归下降、回填算法的数学模型
- ✅ 三轮迭代改进展现工程判断力和对评审意见的吸收能力
- ⚠️ 被追问「为什么不做优化」时可能答案有限

---

## 7. 项目架构总览

```
LemonC 编译管线
─────────────────────────────────────────────────────────────
 源文件 (.lemon)
     │
     ▼  [Lexer]
 Token 序列
     │  DFA 状态机，LexerState 枚举
     │  getNextState() 为转移函数 δ
     ▼  [Parser]
 前端 AST (site.ilemon.ast.Ast)
     │  递归下降，LL(2) 前瞻
     │  isVarDeclarationStart() 消歧
     ▼  [SemanticVisitor]
 带类型标注的 AST
     │  类型检查、变量检查、控制流分析
     │  MethodVarTable + Symbol 符号表
     ▼  [TranslatorVisitor]
 后端 IR (site.ilemon.codegen.ast.Ast)
     │  BoolCode 回填算法
     │  每方法分配局部变量索引
     ▼  [ByteCodeGenerator]
 Jasmin 汇编 (.il)
     │  动态计算 .limit stack（数据流）
     │  动态计算 .limit locals
     ▼  [jasmin.Main]
 JVM 字节码 (.class)
─────────────────────────────────────────────────────────────
```

### 包结构

| 包 | 职责 |
|----|------|
| `site.ilemon.lexer` | 词法分析：`Lexer`、`Token`、`TokenKind`、`LexerState` |
| `site.ilemon.parser` | 语法分析：`Parser` |
| `site.ilemon.ast` | 前端 AST 节点定义 |
| `site.ilemon.semantic` | 语义分析：`SemanticVisitor`、`MethodVarTable`、`Symbol` |
| `site.ilemon.codegen` | IR 翻译与目标代码生成：`TranslatorVisitor`、`ByteCodeGenerator` |
| `site.ilemon.codegen.ast` | 后端 IR 节点定义（Jasmin 指令集） |
| `site.ilemon.visitor` | Visitor 接口：`ISemanticVisitor`、`IElement` |
| `site.ilemon.exception` | 异常层次：`CompilerException`、`ParseException`、`SemanticException` |
| `site.ilemon.list` | 自定义双向链表 `DoublyLinkedList`（用于 breakList/continueList） |
| `site.ilemon.compiler` | 编译器入口 `LemonC`，工具类 `AstPrinter`、`IrPrinter` |

---

## 8. 各维度终态分析

### 8.1 词法分析（7/10）

DFA 实现规范，亮点是 `getNextState()` 显式建模状态转移函数，关键字 `HashMap` 查表效率高。主要缺口是缺少多行注释支持。

### 8.2 语法分析（12/15）

第三轮修复后，`isVarDeclarationStart()` 实现了真正的 LL(2) 消歧，`parseDeclare()` 恢复为纯粹的文法驱动实现。运算符优先级完整正确。剩余差距主要来自语言特性不足（无 `for`、单类等）。

### 8.3 AST 设计（8/10）

经过两轮改进：前端 AST 不再携带代码生成关注点；`Expr.And` 的字段遮蔽 bug 已修复；Visitor 模式完整。仍有 `codegen.ast.Ast` 字段公开的封装问题。

### 8.4 语义分析（11/15）

涵盖编译原理课程要求的主要静态语义检查，尤其亮眼的是使用前赋值检查中 if/else 分支的正确合并逻辑，以及基于 `statementsMustReturn()` 的控制流分析。主要缺口是无嵌套块作用域。

### 8.5 中间代码生成（14/15）

经过最大幅度的重构，现在的回填算法实现是教科书质量的：

- `translateCondition()` 递归翻译布尔表达式，返回 `BoolCode`
- `backpatch(list, label)` 按指令下标填入目标跳转
- `emitBooleanValue()` 统一处理布尔值物化
- NaN 语义通过 `usesCompareGreaterOnNaN()` 正确处理

仅剩 `visit(Declare.T)` 中类型翻译与数组初始化代码生成混杂的轻微职责问题。

### 8.6 目标代码生成（7/10）

`calculateMaxStack()` 基于工作列表算法的数据流分析是亮点，精确计算而非保守估计。`calculateMaxLocals()` 同样通过扫描指令动态推导，无需额外信息。

### 8.7 优化（2/10）

未实现任何优化 Pass，这是与完整编译器实现差距最大的维度。

### 8.8 错误处理（6/10）

异常层次清晰，错误消息格式统一且包含行号。核心不足是 fail-fast 模式，无法在单次编译中报告多个错误。

### 8.9 工程质量（4/5）

三轮持续改进后，主要"坏味道"已消除。仅剩 `codegen.ast.Ast` 中多处字段缺乏封装。

---

## 9. 仍存在的问题清单

以下问题在三轮评审后**仍未修复**，按严重程度排列：

### 严重

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 1 | 无任何编译器优化 Pass | 整体缺失 | 生成代码质量低 |
| 2 | `codegen.ast.Ast` 字段全为 `public` | `codegen/ast/Ast.java` | 破坏封装，外部可随意修改 IR 节点 |
| 3 | 遇到第一个错误即停止编译 | `SemanticVisitor`、`Parser` | 用户体验差，无法一次看到所有错误 |

### 中等

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 4 | 不支持多行注释 `/* ... */` | `Lexer.java` | 语言功能不完整 |
| 5 | `Stmt.T` 携带 `breakList`/`continueList` | `ast/Ast.java` | 轻微关注点混合 |
| 6 | `Token` 字段全 `public` | `lexer/Token.java` | 封装不足 |
| 7 | `TranslatorVisitor.prog` 为 `public` | `codegen/TranslatorVisitor.java` | 封装不足 |
| 8 | 变量声明只能在方法顶部 | `Parser.java` | 不支持 C/Java 风格任意位置声明 |

### 轻微

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 9 | 标识符不支持下划线 `_` | `Lexer.java` | 命名受限 |
| 10 | 无 `for` 循环 | `Parser.java` | 需手写 while |
| 11 | 无一元负号 | `Parser.java` | `-x` 需写 `0 - x` |
| 12 | `isMatch` 不支持 `int → float` 隐式提升 | `SemanticVisitor.java` | 与 C 语义不一致 |
| 13 | `printf` 只支持 `%d`/`%f` | `SemanticVisitor.java` | 无法打印字符串变量 |
| 14 | 全局 `Label` 计数器需手动 `resetCounter()` | `codegen/ast/Label.java` | 多次编译时需注意调用 |

---

## 10. 后续改进建议

### P0 — 核心改进（对毕业设计评分影响最大）

#### 1. 实现常量折叠优化

在语义分析阶段或 IR 翻译阶段，对常量表达式求值：

```
// 输入：
int a;
a = 2 + 3;  // → 编译时可直接得到 5

// 期望生成：
ldc 5
istore 0

// 而非当前的：
ldc 2
ldc 3
iadd
istore 0
```

实现位置：在 `SemanticVisitor` 或 `TranslatorVisitor` 中识别 `Expr.Add(Number, Number)` 等模式，直接折叠。

#### 2. 支持多错误收集

将 `SemanticVisitor` 中的 `error()` 改为收集模式：

```java
// 当前（fail-fast）：
private void error(int lineNum, String msg) {
    this.pass = false;
    throw new SemanticException("...");  // 立即抛出，停止编译
}

// 改进（收集模式）：
private List<String> errors = new ArrayList<>();

private void error(int lineNum, String msg) {
    this.pass = false;
    errors.add("[语义分析] 行 " + lineNum + ": " + msg);
    // 不抛出，继续遍历
}

public List<String> getErrors() { return errors; }
```

### P1 — 重要改进

#### 3. 封装 `codegen.ast.Ast` 字段

将所有 `public` 字段改为 `private` + getter/setter：

```java
// 当前：
public class Istore extends T {
    public int index;  // ← 公开
    public Istore(int index) { this.index = index; }
}

// 改进：
public class Istore extends T {
    private final int index;   // ← 不可变，私有
    public Istore(int index) { this.index = index; }
    public int getIndex() { return this.index; }
}
```

#### 4. 添加 `for` 循环（脱糖为 while）

文法扩展：
```
for ( init ; condition ; update ) body
→ 脱糖为：
{ init; while (condition) { body; update; } }
```

语法分析只需在 `parseStmt()` 中增加一个分支，构造对应的 `Ast.Stmt.While` 节点，零改动语义分析和代码生成。

### P2 — 锦上添花

#### 5. 支持死代码消除

在 `return` 语句之后的语句直接在 IR 层面删除，减少无效跳转标签。

#### 6. 支持一元负号

在 `parseFactor()` 中添加 `TokenKind.Sub` 分支，脱糖为 `0 - expr`：

```java
else if (look.kind == TokenKind.Sub) {
    move();
    Ast.Expr.T operand = parseFactor();
    return new Ast.Expr.Sub(
        new Ast.Expr.Number(new Ast.Type.Int(), "0", look.lineNumber),
        operand,
        look.lineNumber
    );
}
```

---

## 附录：编译器阶段对应文件索引

| 编译阶段 | 核心文件 | 关键类/方法 |
|----------|----------|-------------|
| 词法分析 | `lexer/Lexer.java` | `getNextState()`, `makeToken()` |
| Token 定义 | `lexer/Token.java`, `lexer/TokenKind.java` | — |
| 状态定义 | `lexer/LexerState.java` | — |
| 语法分析 | `parser/Parser.java` | `parse()`, `isVarDeclarationStart()` |
| 前端 AST | `ast/Ast.java` | 所有 AST 节点 |
| 语义分析 | `semantic/SemanticVisitor.java` | `visit()` 系列 |
| 符号表 | `semantic/MethodVarTable.java`, `semantic/Symbol.java` | — |
| Visitor 接口 | `visitor/ISemanticVisitor.java` | — |
| IR 翻译 | `codegen/TranslatorVisitor.java` | `translateCondition()`, `backpatch()` |
| 后端 IR | `codegen/ast/Ast.java` | 所有 IR 指令节点 |
| 目标代码 | `codegen/ByteCodeGenerator.java` | `calculateMaxStack()`, `visit()` 系列 |
| 编译器入口 | `compiler/LemonC.java` | `main()` |
| 调试工具 | `compiler/AstPrinter.java`, `compiler/IrPrinter.java` | `print()` |

---

*本报告由 Claude Sonnet 4.6 生成，基于对项目全量源码的三轮完整阅读与分析。*
