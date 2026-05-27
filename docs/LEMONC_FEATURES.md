# LemonC 功能手册

本文档按功能维度列出 LemonC 当前支持的语言能力和编译器能力。所有“运行输出”均来自 `examples/example-output-manifest.tsv` 中的端到端 JVM 输出基线，也就是 `.lemon` 源程序经 LemonC 编译为 `.class` 后，在 JVM 上实际执行得到的结果。

## 1. 编译链路

LemonC 的主链路是：

```text
Lemon source (.lemon)
  -> Lexer tokens
  -> Parser AST
  -> Semantic analysis
  -> AST optimization
  -> JVM IR
  -> Jasmin assembly
  -> JVM .class
  -> JVM execution output
```

命令行入口：

```bash
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/HelloWorld.lemon
java HelloWorld
```

可选教学输出：

```bash
java -jar target/LemonC-0.1-beta-jar-with-dependencies.jar examples/ModTest.lemon --dump-tokens --dump-ast --dump-ir
```

## 2. 类与 main 方法

LemonC 程序以单个 `class` 为顶层单位，入口方法为 `void main()`。

示例：[examples/HelloWorld.lemon](../examples/HelloWorld.lemon)

```c
class HelloWorld {
    void main() {
        int a;
        int b;
        a = 15;
        b = 27;
        printf("a=%d,b=%d,add=%d", a, b, add(a, b));
    }

    int add(int x, int y) {
        return x + y;
    }
}
```

运行输出：

```text
a=15,b=27,add=42
```

## 3. 基本类型

当前支持：

| 类型 | 用途 |
|---|---|
| `int` | 32 位整数 |
| `float` | 单精度浮点数 |
| `double` | 双精度浮点数 |
| `bool` | 布尔值，可用于条件表达式 |
| `void` | 无返回值方法 |

示例：[examples/LanguageFeatureTest.lemon](../examples/LanguageFeatureTest.lemon)

```c
class LanguageFeatureTest {
    float addOne(float x) {
        return x + 1;
    }

    double widen(int x) {
        return x;
    }

    void main() {
        int i;
        int sum;
        float f;
        double d;
        int arr[3];
        i = 0;
        sum = 0;
        for (i = 0; i < 6; i = i + 1) {
            if (i == 2) {
                continue;
            }
            if (i == 5) {
                break;
            }
            sum = sum + i;
        }
        printf("sum=%d\n", sum);
        printf("neg=%d\n", -sum);
        f = 1 + 2.5;
        d = 1 + f;
        printf("f=%f,d=%f\n", f, d);
        f = addOne(2);
        d = widen(7);
        printf("call=%f,%f\n", f, d);
        arr[0] = 1;
        arr[1] = 2;
        arr[2] = 3;
        printf("arr=%d\n", arr[1]);
    }
}
```

运行输出：

```text
sum=8
neg=-8
f=3.5,d=4.5
call=3.0,7.0
arr=2
```

## 4. 变量声明与赋值

局部变量在方法体前部声明，之后在语句区赋值和使用。

```c
int a;
float f;
double d;
bool ok;
a = 10;
f = 1.5;
d = 2.25;
ok = true;
```

语义分析会检查变量声明、重复声明、未赋值使用、赋值类型兼容性和返回类型兼容性。

## 5. 整数与浮点运算

支持：

| 运算 | 说明 |
|---|---|
| `+` | 加法 |
| `-` | 减法 |
| `*` | 乘法 |
| `/` | 除法 |
| `%` | 整数取模 |
| 一元 `-` | 负号 |

示例：[examples/ModTest.lemon](../examples/ModTest.lemon)

```c
class ModTest {
    void main() {
        int a;
        int b;
        int c;
        a = 10 % 3;
        b = 2 + 10 % 4 * 3;
        c = 20 / 6 + 20 % 6;
        printf("a=%d,b=%d,c=%d\n", a, b, c);
    }
}
```

运行输出：

```text
a=1,b=8,c=5
```

## 6. 数值提升

LemonC 支持安全的数值拓宽：

```text
int -> float
int -> double
float -> double
```

适用位置包括赋值、返回值、方法实参、数组元素赋值、算术表达式和比较表达式。

示例：

```c
float f;
double d;
f = 1 + 2.5;
d = 1 + f;
```

运行输出见 `LanguageFeatureTest`：

```text
f=3.5,d=4.5
call=3.0,7.0
```

## 7. 比较运算

支持：

```text
>  <  >=  <=  ==  !=
```

示例：[examples/CompareTest.lemon](../examples/CompareTest.lemon)

运行输出：

```text
10 > 20 = 0
10 < 20 = 1
10 >= 20 = 0
20 >= 10 = 1
10 <= 20 = 1
20 <= 10 = 0
10 == 20 = 0
10 == 10 = 1
10 != 20 = 1
10 != 10 = 0
```

`float`/`double` 比较会生成 JVM 浮点比较指令，并按关系运算选择 `fcmpl/fcmpg`、`dcmpl/dcmpg`，以保证 NaN 语义正确。

示例：[examples/NaNCompareTest.lemon](../examples/NaNCompareTest.lemon)

运行输出：

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

## 8. 布尔表达式与短路求值

支持：

```text
true
false
!
&&
||
```

布尔表达式采用经典 backpatching 方式生成控制流，`&&` 与 `||` 保持短路语义。

示例：[examples/BoolTest02.lemon](../examples/BoolTest02.lemon)

```c
b1 = true;
b2 = testBoolCall(false);
b3 = !(b1) && b2 || !(b2);
```

运行输出：

```text
b1=1,b2=1,b3=0
```

## 9. if / else 条件分支

支持普通条件分支和嵌套分支：

```c
if (condition) {
    ...
} else {
    ...
}
```

`if` 条件可以是布尔变量、比较表达式、逻辑表达式或方法调用返回的 `bool`。

示例：[examples/DoubleCompareTest.lemon](../examples/DoubleCompareTest.lemon)

```c
if (b > a) {
    printf("if=1\n");
} else {
    printf("if=0\n");
}
```

运行输出：

```text
lt=1
gte=1
eq=0
neq=1
if=1
```

## 10. while 循环

支持 `while` 循环，条件表达式同 `if`。

示例：[examples/Loop.lemon](../examples/Loop.lemon)

运行输出：

```text
1
2
3
4
6
7
```

## 11. for 循环

支持 C 风格 `for`：

```c
for (i = 0; i < 6; i = i + 1) {
    ...
}
```

`continue` 会跳到 update 部分，再进入下一轮条件判断。

示例见 `LanguageFeatureTest`。

运行输出：

```text
sum=8
```

## 12. break 与 continue

支持在循环中使用 `break` 和 `continue`，包括嵌套循环。

示例：[examples/NestedLoops.lemon](../examples/NestedLoops.lemon)

```c
while (i < 3) {
    i = i + 1;
    if (i == 2) {
        printf("outer continue skip %d\n", i);
        continue;
    }

    j = 0;
    while (j < 3) {
        j = j + 1;
        if (j == 2) {
            printf("  inner break on %d\n", j);
            break;
        }
        printf("  inner run i=%d, j=%d\n", i, j);
    }
}
```

运行输出：

```text
  inner run i=1, j=1
  inner break on 2
outer continue skip 2
  inner run i=3, j=1
  inner break on 2
```

## 13. 方法定义、调用与返回值

支持：

| 功能 | 示例 |
|---|---|
| 有返回值方法 | `int add(int x, int y)` |
| `void` 方法 | `void hello()` |
| 方法参数 | `add(a, b)` |
| 表达式中的方法调用 | `printf("%d", add(a, b));` |
| 丢弃返回值 | `discardInt();` |
| 递归调用 | `fib(n - 1) + fib(n - 2)` |

示例：[examples/VoidMethod.lemon](../examples/VoidMethod.lemon)

```c
class VoidMethod {
    void main() {
        printf("before\n");
        hello();
        printf("after\n");
    }

    void hello() {
        printf("hello from void\n");
    }
}
```

运行输出：

```text
before
hello from void
after
```

递归示例：[examples/Fib.lemon](../examples/Fib.lemon)

运行输出：

```text
递归计算斐波那契数列，一年后总共有144对兔子
循环计算斐波那契数列，一年后总共有144对兔子
```

## 14. 数组

支持局部数组声明、索引访问、索引赋值和 `.length`：

```c
int values[5];
float weights[3];
values[0] = 1;
printf("%d\n", values.length);
```

当前数组类型包括：

```text
int[]
float[]
double[]
```

示例：[examples/ArrayLengthTest.lemon](../examples/ArrayLengthTest.lemon)

```c
class ArrayLengthTest {
    void main() {
        int values[5];
        float weights[3];
        int total;
        total = values.length + weights.length;
        printf("values=%d,weights=%d,total=%d\n", values.length, weights.length, total);
    }
}
```

运行输出：

```text
values=5,weights=3,total=8
```

## 15. printf 与 printLine

支持字符串字面量输出和格式化输出：

```text
%d  int/bool 输出
%f  float/double 输出
\n  换行
\t  制表符
```

示例：[examples/PrintfLiteral.lemon](../examples/PrintfLiteral.lemon)

运行输出：

```text
hello literal
```

示例：[examples/PrintfMixed.lemon](../examples/PrintfMixed.lemon)

```c
printf("i=%d, f=%f, d=%f\n", i, f, d);
```

运行输出：

```text
i=7, f=1.5, d=2.25
```

## 16. 注释

支持单行注释：

```c
// this is a comment
```

多行注释不是当前 Lemon 语言定义的一部分。

## 17. AST 优化

LemonC 在语义分析之后、IR 生成之前执行 AST 优化。

当前优化包括：

| 优化 | 示例 |
|---|---|
| 常量算术折叠 | `(2 + 3) * 4 -> 20` |
| 常量比较折叠 | `1 < 2 -> true` |
| 常量布尔折叠 | `true && x -> x` |
| 代数化简 | `x * 1 -> x`, `x + 0 -> x` |
| 常量条件分支简化 | `if (true) then else` |
| 常量 `while(false)` 删除 | `while(false) { ... }` |

示例：[examples/OptimizationTest.lemon](../examples/OptimizationTest.lemon)

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

运行输出：

```text
a=20,b=20
```

## 18. 端到端 JVM 示例覆盖

`examples/` 根目录下的所有 `.lemon` 示例均由 `AllExamplesJvmTest` 覆盖：

```text
source .lemon
  -> LemonC compile
  -> Jasmin assemble
  -> JVM run
  -> stdout compare with examples/example-output-manifest.tsv
```

当前覆盖规模：

```text
82 root examples
168 automated tests
```

综合示例：[examples/ReliabilityCanary.lemon](../examples/ReliabilityCanary.lemon)

运行输出：

```text
start
sum=10
f=1.5,d=2.25
arrays=1.25,2.5,4.5,3.5
bool=1
fib=8
discard-int
discard-double
void-call
loop=1
loop=3
end
```

## 19. 当前边界

以下不是 bug，而是当前 Lemon 语言的设计边界：

| 边界 | 说明 |
|---|---|
| 标识符不含 `_` | 当前词法定义只支持字母数字类标识符 |
| 不支持多行注释 | 当前只定义 `//` 单行注释 |
| 局部变量声明位于方法体前部 | 不支持任意语句位置声明变量 |
| 无块级作用域 | `{ ... }` 不引入独立变量作用域 |
| 无字符串变量类型 | 字符串主要用于 `printf` 字面量 |
| 单类模型 | 当前目标是教学用小型语言，不实现完整 Java 对象系统 |

## 20. 推荐阅读顺序

如果用于教学展示，建议按这个顺序演示：

1. `HelloWorld.lemon`：完整编译运行闭环。
2. `ModTest.lemon`：表达式优先级与 JVM 算术指令。
3. `BoolTest02.lemon`：布尔表达式与短路求值。
4. `NestedLoops.lemon`：循环、`break`、`continue`。
5. `LanguageFeatureTest.lemon`：`for`、一元负号、数值提升、方法调用、数组。
6. `NaNCompareTest.lemon`：浮点比较语义。
7. `OptimizationTest.lemon`：AST 优化。
8. `ReliabilityCanary.lemon`：综合端到端回归。
