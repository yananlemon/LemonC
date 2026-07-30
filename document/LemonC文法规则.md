# LemonC 当前实现文法规则

本文档描述 LemonC 当前代码真实接受的词法规则与语法规则。它不是理想化语言设计稿，而是对 `Lexer` 与 `Parser` 当前实现的精确整理。

代码依据：

- `src/main/java/site/ilemon/lexer/Lexer.java`
- `src/main/java/site/ilemon/lexer/TokenKind.java`
- `src/main/java/site/ilemon/parser/Parser.java`
- `src/main/java/site/ilemon/ast/Ast.java`
- `src/main/java/site/ilemon/source/SourceSpan.java`
- `src/main/java/site/ilemon/semantic/SemanticVisitor.java`（作用域与静态语义边界）

## 1. 说明

本文使用接近 EBNF 的记号：

```text
*      重复 0 次或多次
+      重复 1 次或多次
?      可选
|      或
"..."  固定 lexeme
```

本文中的“语法支持”只表示 Parser 可以构建 AST。变量是否声明、类型是否兼容、`main` 是否合法、`break` 是否在循环中、非 `void` 方法是否覆盖所有返回路径等规则，属于语义分析阶段，不属于本文的纯语法规则。

Lexer token 与 parser AST 使用 end-exclusive
`SourceSpan(startLine, startColumn, endLine, endColumn)`；该范围会继续传播到 Typed-AST、
优化结果和 LemonIR 指令。

## 2. 词法规则

### 2.1 空白与注释

```ebnf
whitespace ::= " " | "\t" | "\r" | "\n"
lineComment ::= "//" anyCharExceptNewline*
blockComment ::= "/*" anyChar* "*/"
```

规则：

- 空白会被跳过。
- `//` 单行注释会被跳过，直到换行或 EOF。
- `/* ... */` 多行注释会被跳过。
- 未闭合的多行注释会抛出词法错误。
- 未闭合字符串会抛出词法错误。

### 2.2 标识符

```ebnf
identifier ::= identifierStart identifierPart*
identifierStart ::= JavaLetter | "_"
identifierPart ::= JavaLetterOrDigit | "_"
```

说明：

- `JavaLetter` 与 `JavaLetterOrDigit` 对应 Java `Character.isLetter` 和 `Character.isLetterOrDigit`。
- 因此当前实现支持下划线，也支持 Java 认为合法的 Unicode 字母。

### 2.3 关键字

```text
class
main
true
false
void
String
int
bool
float
double
if
else
while
for
printf
printLine
return
break
continue
```

注意：当前实现里 `String` 关键字记为 `TokenKind.StringType`，字符串字面量记为 `TokenKind.StringLiteral`。Parser 因此可以区分类型关键字和 `"..."` 字符串字面量。

### 2.4 字面量

```ebnf
intLiteral ::= decimalInt | octalInt | hexadecimalInt
decimalInt ::= "0" | nonZeroDigit digit*
octalInt ::= "0" octalDigit+
hexadecimalInt ::= "0" ("x" | "X") hexDigit+
floatLiteral ::= (digit+ "." digit* | "." digit+) "f"? | digit+ exponent "f" | digit+ "f"
doubleLiteral ::= (digit+ ("." digit*)? | "." digit+) exponent "d"? | (digit+ "." digit* | "." digit+ | digit+) "d"
exponent ::= ("e" | "E") ("+" | "-")? digit+
stringLiteral ::= "\"" anyCharExceptQuoteOrNewline* "\""
boolLiteral ::= "true" | "false"
```

说明：

- `.5` 属于当前浮点字面量。
- `1.` 属于当前浮点字面量。
- 整数字面量支持十进制、八进制（如 `077`）和十六进制（如 `0x2A`）。`089`、空的 `0x`、非法十六进制字符和超过 Java `int` 范围的值会报词法错误。
- 当前有独立的 `double` 字面量 token，支持科学计数法如 `1e2`，也支持 `d`/`D` 后缀。
- **无后缀小数字面量的类型是 `float`，但它的值由使用位置的目标类型决定**：出现在 `double`
  位置时按十进制原文以 `double` 精度取值，而不是先舍入成 `float` 再加宽。因此
  `double a = 3.14159265358979;` 保留全部有效数字，而不是变成 `3.1415927410125732`。
  这条规则在**所有**位置一致生效——赋值、二元运算、数组元素、实参、返回值、比较，
  以及编译期常量折叠。也就是说同一个字面量在任何语法位置都取到同一个值。
  实现上由 `AstToIrTranslator.translateExpressionAs` 和 `AstOptimizer.doubleValueOf`
  两处共同保证，二者必须保持一致。
- 字符串字面量在词法阶段解析 `\n`、`\t`、`\r`、`\"`、`\\`。
- 字符串中不能直接包含换行。
- 字符串中可以通过 `\"` 表示双引号。

### 2.5 运算符与界符

```text
+  -  *  /  %
++  --
=  +=  -=  *=  /=  %=
<  >  <=  >=  ==  !=
&&  ||  !
{  }  (  )  [  ]  ;  ,  .
```

说明：

- 单个 `&` 和单个 `|` 会抛出词法错误。
- `/` 是除法 token；`//` 是单行注释，`/*` 是块注释开始，`/=` 是复合赋值。
  三者共享 `/` 前缀，注释由 trivia 阶段先行处理，因此不会与 `/=` 冲突。
- 采用**最长匹配**：`++`、`--` 以及各复合赋值符号优先于对应的单字符运算符。
  因此 `a - -b`（中间有空白或括号）仍是两个减号，而 `a--b` 会被切成 `a`、`--`、`b`。

## 3. 顶层语法

### 3.1 程序

```ebnf
program ::= "class" classNameToken "{" methodList "}" "EOF"
methodList ::= method*
```

当前实现细节：

- `classNameToken` 必须是普通 `identifier`，不能使用关键字。
- Parser 会检查类名 lexeme 与源文件名去掉 `.lemon` 后是否一致。
- 当前只支持单 class。
- 当前不支持字段声明。`Ast.MainClass` 有 `fields` 位置，但 Parser 固定传入 `null`。

## 4. 方法语法

### 4.1 方法声明

```ebnf
method ::= methodReturnType methodNameToken "(" formalParams? ")" methodBody
methodReturnType ::= "void" | "int" | "float" | "double" | "bool"
methodBody ::= "{" blockItem* "}"
blockItem ::= localVarDecl | stmt
```

当前实现细节：

- `methodNameToken` 必须是普通 `identifier`，或专门的 `main` 关键字 token。
- 方法体按 `blockItem` 顺序解析，局部声明可以与普通语句穿插。
- 嵌套块同样接受 `blockItem*`，因此支持块内局部声明。

### 4.2 形参

```ebnf
formalParams ::= formalParam ("," formalParam)*
formalParam ::= valueParam | arrayParam
valueParam ::= paramType identifier
arrayParam ::= paramType identifier "[" "]"
paramType ::= "int" | "float" | "double" | "bool"
```

当前实现细节：

- 形参不支持 `void`。
- 形参不支持 `String`。
- 数组形参只能写 `int a[]`、`float a[]`、`double a[]`、`bool a[]`。
- 数组形参不能写固定大小，例如 `int a[3]` 会在 Parser 阶段失败。

## 5. 局部变量声明

```ebnf
localVarDecl ::= scalarDecl | arrayDecl
scalarDecl ::= localType identifier ("=" expr)? ";"
arrayDecl ::= localType identifier "[" positiveIntLiteral "]" ";"
localType ::= "int" | "float" | "double" | "bool"
positiveIntLiteral ::= intLiteral
```

当前实现细节：

- 数组大小必须是正整数。
- 数组大小必须是整数 token，不支持表达式。
- 当前支持 `int[]`、`float[]`、`double[]`、`bool[]`。
- 标量支持可选初始化，例如 `int x = 1;`。
- 数组声明不支持初始化表达式。
- 局部声明可位于方法体或嵌套块中的任意 block-item 位置。
- 不支持全局变量。
- 不支持 `String` 变量。
- 不支持 `char`、指针、结构体、对象类型。

## 6. 语句

```ebnf
stmt ::= printfStmt
       | printLineStmt
       | breakStmt
       | continueStmt
       | whileStmt
       | forStmt
       | callStmt
       | assignStmt
       | arrayAssignStmt
       | blockStmt
       | returnStmt
       | ifStmt
```

### 6.1 输出语句

```ebnf
printfStmt ::= "printf" "(" formatToken printfArgs? ")" ";"
printfArgs ::= "," expr ("," expr)*
formatToken ::= stringLiteralToken

printLineStmt ::= "printLine" "(" ")" ";"
```

当前实现细节：

- `printf` 的第一个参数要求 token kind 为 `StringLiteral`。
- `String` 类型关键字是 `StringType`，不会被 Parser 当作格式串 token 接受。
- 格式串占位符数量和类型由语义阶段检查。

### 6.2 控制流语句

```ebnf
ifStmt ::= "if" "(" expr ")" stmt ("else" stmt)?

whileStmt ::= "while" "(" expr ")" stmt

forStmt ::= "for" "(" forInit? ";" forCondition? ";" forUpdate? ")" stmt
forInit ::= simpleStmtWithoutTerminator
forCondition ::= expr
forUpdate ::= simpleStmtWithoutTerminator

breakStmt ::= "break" ";"
continueStmt ::= "continue" ";"

blockStmt ::= "{" blockItem* "}"
```

当前实现细节：

- `for (;;)` 支持，省略 condition 时 Parser 会生成 `true` 条件。
- `for` 的 init/update 只支持赋值、数组赋值、方法调用，不支持变量声明。
- `break` 与 `continue` 是否位于循环内部由语义阶段检查。
- block 里可以穿插普通语句和局部变量声明。
- 当前没有空语句 `;`。
- 当前没有 `do while`、`switch`。

### 6.3 赋值、调用与返回

```ebnf
exprStmt ::= callStmt
           | assignTarget assignOp expr ";"
           | assignTarget ("++" | "--") ";"

assignTarget ::= identifier | identifier "[" expr "]"
assignOp     ::= "=" | "+=" | "-=" | "*=" | "/=" | "%="

callStmt   ::= methodCall ";"
returnStmt ::= "return" expr? ";"
```

当前实现细节：

- Parser 接受 `return;`；语义阶段只允许它出现在 `void` 方法中。
- 赋值语句、复合赋值、自增自减共用同一个入口：先解析赋值目标（`assignTarget`），
  再根据后续运算符分派。目标只能是普通变量或数组元素，其他形式报「不是左值」。
- 复合赋值与自增自减在**解析阶段脱糖**：`a op= b` 变为 `a = a op b`，`a++` 变为 `a = a + 1`。
  因此语义分析、LemonIR 和两个后端都不需要新增节点或指令。
- 脱糖会让数组下标被求值两次，而 AST 层没有临时变量可用来只求值一次。所以当目标是
  数组元素时，下标被限制为**变量或整数字面量**（重复求值安全）；`a[f()] += 1` 会被拒绝，
  诊断提示改写为 `a[i] = a[i] + ...`。这是一处有意的语言限制。
- 只支持后缀 `i++` / `i--` 作为语句；不支持前缀 `++i`，也不支持自增作为表达式使用
  （Lemon 的表达式没有副作用，保持这一性质可以让优化器的纯度判定保持简单）。
- 当前不支持整个数组赋值的专门语法；`a = b;` 可以被 Parser 接受为普通赋值，但语义阶段会拒绝数组整体赋值。

### 6.4 for 头部简单语句

`for` 的 init/update 复用上面的 `exprStmt` 规则（不含末尾分号），因此
`for (i = 0; i < n; i++)` 与 `for (i = 0; i < n; i += 2)` 都可以写。分号由 `forStmt` 消费。

## 7. 表达式

当前 Parser 的表达式优先级从低到高如下：

```ebnf
expr ::= orExpr

orExpr ::= andExpr ("||" andExpr)*

andExpr ::= equalityExpr ("&&" equalityExpr)*

equalityExpr ::= relationalExpr (eqOp relationalExpr)*
eqOp ::= "==" | "!="

relationalExpr ::= additiveExpr (relOp additiveExpr)?
relOp ::= ">" | "<" | ">=" | "<="

additiveExpr ::= term (("+" | "-") term)*

term ::= factor (("*" | "/" | "%") factor)*

factor ::= "(" expr ")"
         | "-" factor
         | intLiteral
         | floatLiteral
         | doubleLiteral
         | methodCall
         | arrayAccess
         | arrayLength
         | identifier
         | stringToken
         | "!" factor
         | "true"
         | "false"

methodCall ::= methodNameToken "(" callArgs? ")"
callArgs ::= expr ("," expr)*

arrayAccess ::= identifier "[" expr "]"
arrayLength ::= identifier "." "length"
```

当前实现细节：

- `||` 和 `&&` 左结合。
- 相等运算符 `==`、`!=` 左结合，且优先级**低于**关系运算符（与 C 一致）。因此
  `a < b == c < d` 解析为 `(a < b) == (c < d)`，这在 Lemon 中是合法的 `bool == bool` 比较。
- 关系运算符 `>`、`<`、`>=`、`<=` 是**非结合**的：`a < b < c` 在语法阶段即被拒绝，
  诊断为「关系运算符不可连用」。这是对 C 的有意偏离——Lemon 的 `bool` 是独立类型且
  没有 `int`/`bool` 隐式转换，`a < b < c` 永远无法通过类型检查，在语法阶段给出准确诊断
  优于让它漂到语义阶段报「左侧为 bool」。
  在错误收集模式下，剩余的比较链会按左结合就地消费掉，因此一条链只产生一条诊断，
  不会触发恐慌模式同步而产生连带错误。
- `+`、`-` 左结合。
- `*`、`/`、`%` 左结合。
- 一元负号支持，例如 `-x`、`-(a + b)`。
- 逻辑非递归作用于下一个 factor，因此 `!x`、`!(expr)` 和 `!!x` 都可解析。
- 方法调用既可作为表达式，也可作为语句。
- 表达式中的方法调用只接受普通 `identifier`；`main` 关键字 token 只用于入口方法定义，不能作为普通方法调用。
- `arrayLength` 只接受 `.length`，其他属性名会在 Parser 阶段报错。
- `StringLiteral` token 可以作为表达式构建 `Ast.Expr.Str`，裸 `String` 关键字不会被当作字符串表达式接受。

## 8. 当前不属于语法规则的检查

以下规则由语义分析阶段负责，不应写进 Parser 文法：

- 必须存在 `void main()`。
- `main` 不能有参数。
- 非 `void` 方法必须在所有路径返回值。
- `void` 调用不能作为表达式使用。
- 变量必须先声明后使用。
- 块内变量离开块后不可见。禁止遮蔽：只要名字在任一尚未关闭的作用域中可见，
  再次声明即为重复声明（与 Java 一致，形参也参与判定）。作用域关闭后名字随之消失，
  因此**并列的兄弟块可以复用同一个名字**，且各自类型可以不同：
  `{ int v; } { double v; }` 合法，而 `int v; { int v; }` 是重复声明。
  重复声明只报一次——重复的符号仍会进入当前作用域，避免后续引用级联报「未定义的变量」。
- 局部变量必须先赋值后读取。
- 赋值类型必须兼容。
- `if`、`while`、`for` 条件必须是 `bool`。
- 方法实参数量与类型必须匹配。
- 数组下标必须是 `int`。
- 数组元素赋值类型必须兼容。
- `.length` 只能用于数组。
- `break` 和 `continue` 只能在循环中使用。
- `printf` 占位符数量与参数类型必须匹配。

## 9. 当前不支持的语法

```text
char 类型
String 变量、String 参数、String 返回值
指针
结构体 / 类字段 / 多 class
全局变量
数组初始化：int a[3] = ...
数组字面量
动态数组大小：int a[n];
空语句：;
do while
switch
复合赋值：+= -= *= /= %=
自增自减：++ --
位运算
三目运算符
函数重载
泛型
import / package
```

## 10. Parser 与文法的已知不理想实现点

这些不是本文档错误，而是当前代码事实：

1. 字符串变量、字符串参数和字符串返回值仍未作为完整语言特性开放。
2. 语句层已统一到 `assignTarget` 入口，Parser 只用 `lookahead(1)` 区分「方法调用语句」和
   「赋值目标」这一处。表达式层的 `factor` 仍用 `lookahead(1)` 区分 `f(`、`a[`、`a.length`。
   要做成通用的 postfix 链（`f()[0]`、`a[i][j]`），需要先把 `Expr.ArrayAccess`／`Expr.ArrayLength`
   从「持有数组名字符串」改为「持有基表达式」，这会连带改动语义分析、Typed-AST 与 IR 翻译。
   在语言引入嵌套数组之前，这个改动没有对应的类型可表达，因此暂未进行。
3. 方法级 locals 清单仍然存在，但已改为**从语句树派生**（`MethodSingle` 构造时按源码顺序
   遍历收集），Parser 不再在解析过程中把声明提升到方法级。语义分析完全不使用这张清单，
   作用域可见性只由作用域栈决定；清单只服务于需要方法级槽位列表的后端（含遗留的
   `TranslatorVisitor` 直译路径）。被遮蔽或同名的声明各自是独立符号，因此会得到独立槽位。
4. `String` 已被 Lexer 识别为关键字，但 Parser 仍不把它作为可声明类型。

## 11. 建议的下一步

如果要把 LemonC 做成顶级本科项目，建议下一步把当前 Parser 重构目标定为：

1. 为字符串变量、字符串参数和字符串返回值补完整语义与后端支持。
2. 为本文每条核心产生式补 Parser 单元测试。
3. 将本文档作为正式语言规格，并在新增语法时先改文法、再改 Parser、再补测试。
