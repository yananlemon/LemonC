# LemonC 编译器评价报告

## 一、项目概述

LemonC 是一个用 Java 实现的教学型编译器，将自定义的 Lemon 语言编译为 JVM 字节码。项目完整实现了编译器的四个核心阶段：词法分析、语法分析、语义分析、代码生成。

## 二、功能特性

### 2.1 支持的数据类型
| 类型 | 变量声明 | 字面量 | 数组 | 运算 |
|------|---------|--------|------|------|
| int | ✅ | ✅ | ✅ | ✅ |
| float | ✅ | ✅ | ✅ | ✅ |
| double | ✅ | ✅ | ✅ | ✅ |
| bool | ✅ | ✅ | ✅ | ✅ |
| String | ❌ | ✅ | ❌ | ❌ (仅printf) |

### 2.2 支持的语言结构
- 控制流: if-else, while
- 函数: 定义、调用、递归、多参数
- 数组: 声明 `int arr[10];`、访问 `arr[i]`、赋值 `arr[i] = x;`
- 运算符: 
  - 算术: `+`, `-`, `*`, `/`
  - 比较: `>`, `<`, `>=`, `<=`, `==`, `!=`
  - 逻辑: `&&`, `||`, `!` (支持短路求值)
- 输出: printf (格式化), printLine

### 2.3 示例程序
```c
// 冒泡排序
class BubbleSort {
    void main() {
        int arr[6];
        arr[0] = 64; arr[1] = 34; arr[2] = 25;
        // ... 排序逻辑
        if (arr[j] > arr[j + 1]) {
            temp = arr[j];
            arr[j] = arr[j + 1];
            arr[j + 1] = temp;
        }
    }
}
```

### 2.4 不支持的特性
- for 循环、break/continue
- String 变量
- 自增/自减 `++`, `--`
- 取模运算符 `%`
- 类和对象 (面向对象)
- 多维数组

## 三、架构设计优点

### 3.1 清晰的模块划分
```
Lexer → Parser → SemanticVisitor → TranslatorVisitor → ByteCodeGenerator
```
每个阶段职责单一，符合编译原理教科书的经典架构。

### 3.2 规范的 Visitor 模式 (源语言 AST)
AST 节点实现了 `accept()` 方法，实现真正的双重分派：
```java
// AST 节点
public void accept(ISemanticVisitor v) {
    v.visit(this);
}

// TranslatorVisitor 分发
public void visit(Expr.T obj) {
    obj.accept(this);  // 一行代码替代 instanceof 链
}
```

### 3.3 DFA 状态机词法分析
Lexer 使用显式的状态枚举和状态转移函数，代码结构清晰：
```java
private LexerState getNextState(LexerState state, char c) {
    switch (state) {
        case START: ...
        case IN_ID: ...
        case IN_NUM: ...
    }
}
```

### 3.4 龙书风格的布尔表达式翻译
采用回填技术实现短路求值，代码注释直接引用 SDT 规则：
```java
/**
 * E -> E1 and E2
 *  E1.true := newlabel
 *  E1.false := E.false
 *  ...
 */
```

### 3.5 两层 AST 设计
- `site.ilemon.ast.Ast`: 源语言 AST (高级)
- `site.ilemon.codegen.ast.Ast`: 中间代码 AST (低级)

这种分离使得前端和后端解耦，便于支持不同的目标平台。

### 3.6 完整的数组支持
新增的数组功能涉及全链路改动：
- Lexer: `[` `]` token
- Parser: 数组声明、访问、赋值解析
- Semantic: 数组类型检查、下标类型检查
- CodeGen: newarray, iaload, iastore 等 JVM 指令

## 四、实现缺陷

### 4.1 ByteCodeGenerator 和 SemanticVisitor 仍使用 instanceof
```java
// ByteCodeGenerator
public void visit(Ast.Stmt.T stmt) {
    if (stmt instanceof Ast.Stmt.Aload)
        this.visit((Ast.Stmt.Aload)stmt);
    // ... 50+ 行 instanceof 链
}
```
中间代码 AST 没有实现 `accept()` 方法，与源语言 AST 风格不一致。

### 4.2 Parser 代码重复
`parseInputParams()` 方法中，int/float/double/bool 的处理逻辑几乎相同，可以抽取为通用方法。

### 4.3 错误处理简单
- 遇到第一个错误就 `System.exit(1)`
- 没有错误恢复机制
- 错误信息不够详细

### 4.4 类型系统不完整
- 混合类型运算会报错 (int + float)
- 缺少完整的类型推断

### 4.5 未使用的代码
- `Lexer.retreat()`, `Lexer.lineSeparator`
- `Parser.parseBoolExpr()`, `Parser.parseMethodCall2()`

## 五、代码质量

### 5.1 命名问题
- `LET` (Less Than or Equal) vs `GET` (Greater Than or Equal) - 容易混淆
- `Commer` 应为 `Comma`
- `DNum` 含义不明确

### 5.2 注释质量
- 布尔表达式翻译部分注释优秀，直接引用龙书规则
- 其他部分注释较少

## 六、测试覆盖

项目有 60+ 个示例程序，覆盖了：
- 基本算术运算、各种布尔表达式组合
- 控制流嵌套、递归函数
- 数组操作 (冒泡排序)

## 七、改进建议

### 短期
1. 为 `codegen.ast.Ast` 添加 `accept()` 方法
2. 重构 Parser 中的重复代码
3. 清理未使用的代码

### 中期
1. 添加 for 循环、break/continue
2. 添加 String 变量支持
3. 改进错误恢复机制

### 长期
1. 添加类和对象支持
2. 添加类型推断
3. 添加优化 Pass

## 八、总结

LemonC 是一个**优秀的教学型编译器项目**：

**亮点：**
- 完整实现编译器四个阶段 + 数组支持
- 架构清晰，符合教科书设计
- 布尔表达式回填技术实现精准
- 丰富的测试用例
- 源语言 AST 使用规范的双重分派 Visitor 模式

**不足：**
- 部分模块实现风格不一致 (instanceof 链)
- 语言特性仍有扩展空间
- 错误处理简单

**评分：** ⭐⭐⭐⭐ (4/5)

作为编译原理课程的实践项目，LemonC 很好地展示了编译器的核心概念，特别是新增的数组功能展示了全链路改动的完整流程。代码可读性强，适合学习和参考。
