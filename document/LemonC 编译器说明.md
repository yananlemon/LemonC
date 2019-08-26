LemonC 包括完整的编译器前端，目标代码是Java 字节码，可以运行在JVM之上。
[toc]
# 1 LemonC介绍

## 1.1 运算符
Lemon语言支持的运算符见下表
优先级 | 运算符|说明
---|---|---
2|!|逻辑非
3|*,/|乘，除
4|+,-|加，减
6|>,<,|
11|&&|逻辑与
12|\|\||逻辑或
14|=|赋值

**>=,<=,==,!=** 这些运算符暂不支持，它们的实现非常简单，和LemonC中已经实现的>,<非常类似。

## 1.2 关键字
LemonC中的关键字是Java语言的子集，下表列出了关键字：
关键字 |说明
---|---
class|
void|
main|
int|
float|
if|
true|
false|
while|
printf|
printNewLine|
在LemonC的源代码中，保留了bool关键字，但是并不支持直接在控制流语句中使用bool类型的变量，后续版本可能考虑支持。
## 1.3 控制流
LemonC中的控制流只有两种，if和while。
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
while的代码示例如下所示，while不支持break。
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
在LemonC中，用到了如下JVM指令：
1. 加载和存储指令
2. 算术指令
3. 转移指令
4. 方法调用和返回指令

## 2.1 加载和存储指令

指令 |示例|说明
---|---|---
ldc | ldc 1 | 将1加载到操作数栈栈顶
ldc | ldc "hello" | 将字符串hello加载到操作数栈栈顶
istore | istore index | 将栈顶int类型的数字存储到局部变量表索引为index处
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
goto | 无条件跳转到指定标号处执行
事实上，JVM还支持很多转移指令（if_icmpge,if_icmple,ifeq,ifne）,这也是实现>=,<=,==,!=所需要做的事情。  

## 2.4 方法调用和返回指令
方法调用和返回指令：
指令 |说明
---|---
invokevirtual | 用于调用对象的实例方法，根据对象的实际类型进行分派（虚方法分派）
return |
