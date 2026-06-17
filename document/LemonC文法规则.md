# LemonC 当前实现文法规则

本文档描述 LemonC 当前代码真实接受的词法规则与语法规则。它不是理想化语言设计稿，而是对 `Lexer` 与 `Parser` 当前实现的精确整理。

代码依据：

- `src/main/java/site/ilemon/lexer/Lexer.java`
- `src/main/java/site/ilemon/lexer/TokenKind.java`
- `src/main/java/site/ilemon/parser/Parser.java`
- `src/main/java/site/ilemon/ast/Ast.java`

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
intLiteral ::= digit+
floatLiteral ::= (digit+ "." digit* | "." digit+) "f"? | digit+ exponent "f" | digit+ "f"
doubleLiteral ::= (digit+ ("." digit*)? | "." digit+) exponent "d"? | (digit+ "." digit* | "." digit+ | digit+) "d"
exponent ::= ("e" | "E") ("+" | "-")? digit+
stringLiteral ::= "\"" anyCharExceptQuoteOrNewline* "\""
boolLiteral ::= "true" | "false"
```

说明：

- `.5` 属于当前浮点字面量。
- `1.` 属于当前浮点字面量。
- 当前有独立的 `double` 字面量 token，支持科学计数法如 `1e2`，也支持 `d`/`D` 后缀。
- 字符串字面量在词法阶段解析 `\n`、`\t`、`\r`、`\"`、`\\`。
- 字符串中不能直接包含换行。
- 字符串中可以通过 `\"` 表示双引号。

### 2.5 运算符与界符

```text
+  -  *  /  %
<  >  <=  >=  ==  !=
&&  ||  !
=
{  }  (  )  [  ]  ;  ,  .
```

说明：

- 单个 `&` 和单个 `|` 会抛出词法错误。
- `/` 是除法 token；`//` 是单行注释。

## 3. 顶层语法

### 3.1 程序

```ebnf
program ::= "class" classNameToken "{" methodList "}" "EOF"
methodList ::= method*
```

当前实现细节：

- `classNameToken` 当前没有强制必须是 `identifier` token。Parser 读取 `class` 后的当前 token lexeme 作为类名。
- Parser 会检查类名 lexeme 与源文件名去掉 `.lemon` 后是否一致。
- 当前只支持单 class。
- 当前不支持字段声明。`Ast.MainClass` 有 `fields` 位置，但 Parser 固定传入 `null`。

## 4. 方法语法

### 4.1 方法声明

```ebnf
method ::= methodReturnType methodNameToken "(" formalParams? ")" methodBody
methodReturnType ::= "void" | "int" | "float" | "double" | "bool"
methodBody ::= "{" localVarDecl* stmt* "}"
```

当前实现细节：

- `methodNameToken` 当前没有强制必须是 `identifier` token。Parser 读取返回类型后的当前 token lexeme 作为方法名，然后直接消费。
- 因为 `main` 是关键字 token，所以 `void main()` 可以被 Parser 接受。
- 方法体内局部变量声明必须出现在所有语句之前。
- 当前不支持方法体中穿插声明。
- 当前不支持块级局部变量声明。

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
scalarDecl ::= localType identifier ";"
arrayDecl ::= localType identifier "[" positiveIntLiteral "]" ";"
localType ::= "int" | "float" | "double" | "bool"
positiveIntLiteral ::= intLiteral
```

当前实现细节：

- 数组大小必须是正整数。
- 数组大小必须是整数 token，不支持表达式。
- 当前支持 `int[]`、`float[]`、`double[]`、`bool[]`。
- 不支持局部变量初始化，例如 `int x = 1;`。
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

blockStmt ::= "{" stmt* "}"
```

当前实现细节：

- `for (;;)` 支持，省略 condition 时 Parser 会生成 `true` 条件。
- `for` 的 init/update 只支持赋值、数组赋值、方法调用，不支持变量声明。
- `break` 与 `continue` 是否位于循环内部由语义阶段检查。
- block 里只能放语句，不能放局部变量声明。
- 当前没有空语句 `;`。
- 当前没有 `do while`、`switch`。

### 6.3 赋值、调用与返回

```ebnf
assignStmt ::= identifier "=" expr ";"
arrayAssignStmt ::= identifier "[" expr "]" "=" expr ";"
callStmt ::= methodCall ";"
returnStmt ::= "return" expr ";"
```

当前实现细节：

- 当前不支持 `return;`。
- 当前不支持复合赋值，例如 `+=`、`-=`。
- 当前不支持自增自减，例如 `i++`、`++i`。
- 当前不支持整个数组赋值的专门语法；`a = b;` 可以被 Parser 接受为普通赋值，但语义阶段会拒绝数组整体赋值。

### 6.4 for 头部简单语句

```ebnf
simpleStmtWithoutTerminator ::= methodCall
                              | identifier "=" expr
                              | identifier "[" expr "]" "=" expr
```

当前实现细节：

- 该规则只用于 `for` 的 init/update。
- 该规则自身不消费分号，分号由 `forStmt` 消费。

## 7. 表达式

当前 Parser 的表达式优先级从低到高如下：

```ebnf
expr ::= orExpr

orExpr ::= andExpr ("||" andExpr)*

andExpr ::= relationExpr ("&&" relationExpr)*

relationExpr ::= additiveExpr (relOp additiveExpr)*
relOp ::= ">" | "<" | ">=" | "<=" | "==" | "!="

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
         | "!" "(" expr ")"
         | "true"
         | "false"

methodCall ::= methodNameToken "(" callArgs? ")"
callArgs ::= expr ("," expr)*

arrayAccess ::= identifier "[" expr "]"
arrayLength ::= identifier "." "length"
```

当前实现细节：

- `||` 和 `&&` 左结合。
- 比较运算也是左结合，并且语法上允许连续比较，例如 `a < b < c`。是否类型合法由语义阶段决定。
- `+`、`-` 左结合。
- `*`、`/`、`%` 左结合。
- 一元负号支持，例如 `-x`、`-(a + b)`。
- 逻辑非当前只支持 `!(expr)`，不支持 `!x`。
- 方法调用既可作为表达式，也可作为语句。
- `methodNameToken` 在表达式调用处实际必须从 `identifier` 分支进入；因此关键字形式的方法名即使定义阶段可能被宽松接受，也无法按普通调用语法调用。
- `arrayLength` 只接受 `.length`，其他属性名会在 Parser 阶段报错。
- `StringLiteral` token 可以作为表达式构建 `Ast.Expr.Str`，裸 `String` 关键字不会被当作字符串表达式接受。

## 8. 当前不属于语法规则的检查

以下规则由语义分析阶段负责，不应写进 Parser 文法：

- 必须存在 `void main()`。
- `main` 不能有参数。
- 非 `void` 方法必须在所有路径返回值。
- `void` 调用不能作为表达式使用。
- 变量必须先声明后使用。
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
块级变量声明
变量声明初始化：int x = 1;
数组初始化：int a[3] = ...
数组字面量
动态数组大小：int a[n];
return;
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
2. `parseMethod()` 没有强制方法名 token 必须是 `Id` 或 `Main`。
3. `parseMainClass()` 没有强制类名 token 必须是 `Id`，而是用 lexeme 与文件名比较。
4. `parseStmt()` 对未知 token 没有统一的最终 `else error(...)`，在某些“期待语句”的上下文里可能先构造出 `null`，再由后续阶段或后续匹配暴露问题。
5. 类注释中的简化 BNF 已落后于当前实现，不能作为正式文法依据。

## 11. 建议的下一步

如果要把 LemonC 做成顶级本科项目，建议下一步把当前 Parser 重构目标定为：

1. 为字符串变量、字符串参数和字符串返回值补完整语义与后端支持。
2. 明确 `className`、`methodName` 必须是合法标识符，`main` 作为入口名特殊处理。
3. 为本文每条核心产生式补 Parser 单元测试。
4. 将本文档作为正式语言规格，并在新增语法时先改文法、再改 Parser、再补测试。
