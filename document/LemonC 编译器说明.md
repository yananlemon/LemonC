LemonC 包括完整的编译器前端，目标代码是Java 字节码，可以运行在JVM之上。

# 1.LemonC介绍

## 1.1.运算符
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

## 控制流
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

# 2.JVM字节码介绍
在LemonC中，用到了如下JVM指令：
指令 |类型|说明
---|---|---
