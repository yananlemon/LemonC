# LemonC 编译器

LemonC 是一个基于 Java 实现的编译器，将自定义的 Lemon 语言编译为 **JVM 字节码**，可直接在 JVM 上运行。

项目包含完整的编译器前端与后端：**词法分析 → 语法分析 → 语义分析 → IR 翻译 → 字节码生成**。

完整语言功能、示例源码和真实 JVM 运行输出见：[LemonC 功能手册](docs/LEMONC_FEATURES.md)。

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.3+

### 构建

```bash
# 1. 安装 jasmin.jar 到本地 Maven 仓库（仅首次）
mvn install:install-file \
  -DgroupId=com.jasmin -DartifactId=jasmin -Dversion=1.0 \
  -Dpackaging=jar -Dfile=jars/jasmin.jar

# 2. 构建并打包
mvn clean package

# 3. 运行测试（168 个自动化测试用例）
mvn test
```

### 编译和运行 Lemon 程序

```bash
# 编译
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/Fib.lemon

# 运行（生成的 .class 在当前目录）
java Fib
```

### 教学演示：查看编译中间结果

LemonC CLI 支持直接打印编译管线中的关键产物，方便课堂展示从源码到 JVM 栈机指令的降级过程：

```bash
# 打印 token 流、语义分析后的 AST、降低后的 JVM IR，并继续生成 .class
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/ModTest.lemon \
  --dump-tokens --dump-ast --dump-ir

# 也可以只看某一层
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/ArrayLengthTest.lemon --dump-ir
```

典型链路示例：

| 源语言特性 | Token / AST | JVM IR |
|---|---|---|
| `10 % 3` | `Mod` | `Irem` |
| `a < b` where `a,b` are `double` | `LT` | `Dcmpl` + integer branch |
| `arr.length` | `ArrayLength` | `Arraylength` |

输出：
```
递归计算斐波那契数列，一年后总共有144对兔子
循环计算斐波那契数列，一年后总共有144对兔子
```

## Lemon 语言规范

### 数据类型

| 类型 | 关键字 | 示例 |
|------|--------|------|
| 整数 | `int` | `int x; x = 42;` |
| 浮点 | `float` | `float f; f = 3.14;` |
| 双精度 | `double` | `double d; d = 2.718;` |
| 布尔 | `bool` | `bool b; b = true;` |
| 整数数组 | `int[]` | `int arr[10];` |
| 浮点数组 | `float[]` | `float arr[5];` |

### 运算符（按优先级从高到低）

| 优先级 | 运算符 | 说明 |
|:------:|--------|------|
| 1 | `!` | 逻辑非 |
| 2 | `*`, `/`, `%` | 乘、除、取模 |
| 3 | `+`, `-` | 加、减 |
| 4 | `>`, `<`, `>=`, `<=` | 关系比较 |
| 5 | `==`, `!=` | 相等比较 |
| 6 | `&&` | 逻辑与 |
| 7 | `\|\|` | 逻辑或 |
| 8 | `=` | 赋值 |

### 控制流

```c
// if-else
if( a > b ) {
    printf("a大于b\n");
} else {
    printf("a不大于b\n");
}

// while 循环
while( i < 10 ) {
    sum = sum + i;
    i = i + 1;
}
```

### 方法定义与调用

```c
class Fib {
    void main() {
        int n;
        n = 12;
        printf("fib(%d)=%d\n", n, fib(n));
    }

    int fib(int n) {
        int result;
        if( n < 3 ) {
            result = 1;
        } else {
            result = fib(n-1) + fib(n-2);
        }
        return result;
    }
}
```

### BNF 文法

```bnf
<program>       ::= "class" <id> "{" <method>* "}"

<method>        ::= <type> <id> "(" <params>? ")" "{" <varDecl>* <stmt>* "}"
                  | "void" "main" "(" ")" "{" <varDecl>* <stmt>* "}"

<params>        ::= <type> <id> ("," <type> <id>)*

<varDecl>       ::= <type> <id> ";"
                  | <type> <id> "[" <integer> "]" ";"

<type>          ::= "int" | "float" | "double" | "bool" | "void"

<stmt>          ::= <id> "=" <expr> ";"                          (* 赋值 *)
                  | <id> "[" <expr> "]" "=" <expr> ";"           (* 数组赋值 *)
                  | <id> "(" <args>? ")" ";"                     (* 方法调用 *)
                  | "if" "(" <expr> ")" <stmt> ("else" <stmt>)?  (* 条件 *)
                  | "while" "(" <expr> ")" <stmt>                (* 循环 *)
                  | "{" <stmt>* "}"                               (* 块 *)
                  | "return" <expr> ";"                           (* 返回 *)
                  | "printf" "(" <string> ("," <expr>)* ")" ";"  (* 格式化输出 *)
                  | "printLine" "(" ")" ";"                      (* 换行 *)

<expr>          ::= <andExpr> ("||" <andExpr>)*
<andExpr>       ::= <relExpr> ("&&" <relExpr>)*
<relExpr>       ::= <addExpr> ((">" | "<" | ">=" | "<=" | "==" | "!=") <addExpr>)*
<addExpr>       ::= <term> (("+" | "-") <term>)*
<term>          ::= <factor> (("*" | "/") <factor>)*
<factor>        ::= "(" <expr> ")"
                  | <integer>
                  | <float>
                  | <id>
                  | <id> "(" <args>? ")"      (* 方法调用表达式 *)
                  | <id> "[" <expr> "]"        (* 数组访问 *)
                  | "!" "(" <expr> ")"         (* 逻辑非 *)
                  | "true" | "false"
                  | <string>

<args>          ::= <expr> ("," <expr>)*
```

## 编译器架构

```
                     ┌─────────────────────────────────────────┐
  源文件 (.lemon)    │            LemonC 编译管线               │    JVM 字节码
 ─────────────────► │                                         │ ──────────────►
                     │  Lexer → Parser → Semantic → Translator │
                     │    ↓        ↓        ↓          ↓       │
                     │  Token流   AST    类型检查    Jasmin IL  │
                     └─────────────────────────────────────────┘
                                                        ↓
                                                    Jasmin 汇编
                                                        ↓
                                                   .class 文件
```

### 模块说明

| 包 | 核心类 | 职责 |
|---|--------|------|
| `site.ilemon.lexer` | `Lexer` | 基于 DFA 的词法分析器，将源码转换为 Token 流 |
| `site.ilemon.parser` | `Parser` | 递归下降语法分析器，生成 AST |
| `site.ilemon.semantic` | `SemanticVisitor` | Visitor 模式语义分析：类型检查、变量声明/赋值检查 |
| `site.ilemon.codegen` | `TranslatorVisitor` | 将前端 AST 翻译为 Jasmin IL 指令序列 |
| `site.ilemon.codegen` | `ByteCodeGenerator` | 将 IL 指令序列写入 `.il` 文件 |
| `site.ilemon.ast` | `Ast` | 前端 AST 节点定义（Expr/Stmt/Type/Method/Program） |
| `site.ilemon.codegen.ast` | `Ast` | 后端 IR 节点定义（Jasmin 指令级 AST） |
| `site.ilemon.exception` | `CompilerException` | 统一异常层次：ParseException / SemanticException |
| `site.ilemon.compiler` | `LemonC` | 编译器入口，串联整个管线 |

## 测试

```bash
mvn test
```

测试套件包含 **168 个自动化测试用例**：

| 测试类 | 数量 | 说明 |
|--------|:---:|------|
| `AllExamplesJvmTest` | 1 | 所有根目录示例均编译为 `.class`，并在 JVM 上对比真实输出清单 |
| `AstOptimizerTest` | 3 | AST 优化单元测试 |
| `ByteCodeGeneratorTest` | 13 | JVM 字节码/栈帧生成测试 |
| `CompilerTest` | 69 | 端到端集成测试，覆盖可编译的 .lemon 示例 |
| `ErrorTest` | 34 | 负面测试，覆盖语义错误、语法错误和格式验证 |
| `LexerTest` | 18 | 词法分析单元测试 |
| `ParserTest` | 18 | 语法分析单元测试 |
| `SemanticTest` | 1 | 语义分析单元测试 |
| `TranslatorVisitorTest` | 11 | IR 翻译单元测试 |

## 示例程序

`examples/` 目录包含 82 个 Lemon 语言示例程序，覆盖：

- 基础计算与变量声明
- `if/else` 条件分支（13 个测试）
- `while` 循环与迭代（9 个测试）
- 布尔表达式与逻辑运算（16 个测试）
- `int`/`float`/`double` 多类型运算
- 方法定义、参数传递与递归调用
- 数组声明、访问与数组排序（冒泡排序）
- 格式化输出 (`printf`)

## 许可

MIT License
