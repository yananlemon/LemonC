> 当前状态说明：本文是早期编译器说明，保留了 JVM 单后端时期的介绍。当前 LemonC 已经引入 typed LemonIR、LemonVM 后端、更多语言特性和双后端一致性测试。当前实现请以 `docs/ARCHITECTURE.md`、`docs/LEMONC_FEATURES.md` 和源码为准。

LemonC 包括完整的编译器前端、typed LemonIR 中间层、JVM 后端和 LemonVM 后端。默认目标代码可以生成 Java 字节码运行在 JVM 之上，也可以通过 `--target vm` / `--run-vm` 走自研 LemonVM。
[toc]
# 1 LemonC介绍

## 1.1 运算符
Lemon语言支持的运算符见下表： 

优先级 | 运算符|说明
---|---|---
2|!|逻辑非
3|*,/,%|乘，除，取模
4|+,-|加，减
6|>,<,>=,<=,==,!=|比较
11|&&|逻辑与
12|\|\||逻辑或
14|=|赋值

当前实现已支持 `>=`、`<=`、`==`、`!=`，并覆盖 int/float/double 的比较语义。

## 1.2 关键字
LemonC中的关键字是Java语言的子集，下表列出了关键字：  

关键字 |说明
---|---
class|声明类
void|
main|
int|整型数字
float|浮点型数字
double|双精度浮点数字
bool|
if|
else|
true|
false|
while|
for|
break|
continue|
return|
printf|
printLine|

当前实现支持 bool 变量、bool 字面量、逻辑表达式和控制流条件中的 bool 类型。
## 1.3 控制流
LemonC 当前支持 `if/else`、`while`、`for`、`break` 和 `continue`。
if的代码示例如下所示：
```
int a;
if( !( 1 > 0 ) || !(10 > 9) && (5 < 4)   ){
    a = 12;
}else{
    a = 13;
}
printf("a=%d\n",a);// 13
```
while 的代码示例如下所示，循环内部支持 `break` 和 `continue`。
```
class Iteration02{
	void main(){
		int start;
		int sum;
        start = 1;
        sum = 0;
        while(start < 101){
            sum = sum + start;
            start = start + 1;
        }
        printf("1到100之和是：%d\n",sum);
	}
}
```
其中bool 表达式支持and，or，not（即&&,||,!），逻辑运算符的优先级请参考1.1运算符。

# 2 JVM字节码介绍
JVM 后端由 LemonIR 降低到 JVM 指令 IR，再由 `ByteCodeGenerator` 生成 Jasmin。LemonC 中用到了如下 JVM 指令：
1. 加载和存储指令
2. 算术指令
3. 转移指令
4. 方法调用和返回指令

## 2.1 加载和存储指令

指令 |示例|说明
---|---|---
ldc | ldc 1 | 将1加载到操作数栈栈顶
ldc | ldc "hello" | 将字符串hello加载到操作数栈栈顶
istore | istore index | 将操作数栈栈顶int类型的数字存储到局部变量表索引为index处
iload | iload index | 加载局部变量表中索引为index的int变量到操作数栈

与上述加载和存储指令类似，float类型的操作的指令是：fstore和fload。


## 2.2 算术指令
算术指令包括整型和浮点型（与整型类似）如下表所示：

指令 |说明
---|---
iadd | 将栈顶两int型数值相加并将结果压入栈顶
isub | 将栈顶两int型数值相减并将结果压入栈顶
imul | 将栈顶两int型数值相乘并将结果压入栈顶
idiv | 将栈顶两int型数值相除并将结果压入栈顶
## 2.3 转移指令
转移指令用来控制程序执行流程，在LemonC中用到了如下指令：  

指令 |说明
---|---
if_icmpgt | 如果次栈顶元素大于栈顶则跳转到指定标号处执行
if_icmplt | 如果次栈顶元素小于栈顶则跳转到指定标号处执行
fcmpl | compare two floats. If same, pushes 0; else if value 2 greater, pushes 1; else pushes -1. Returns -1 on NaN.
goto | 无条件跳转到指定标号处执行

当前实现已经使用相关转移指令支持 `>=`、`<=`、`==`、`!=` 等比较。

## 2.4 方法调用和返回指令
方法调用和返回指令：  

指令 |说明
---|---
invokevirtual | 用于调用对象的实例方法，根据对象的实际类型进行分派（虚方法分派）
return |
