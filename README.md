使用Java实现的编译器，包括词法分析，语法分析，语义分析，代码生成。目标代码是java字节码。
0. 环境要求：  
	(1)jdk1.8+  
	(2)maven3.3+
1. 安装jasmin.jar到本地仓库
```
	mvn install:install-file -DgroupId=com.jasmin -DartifactId=jasmin -Dversion=1.0 -Dpackaging=jar -Dfile=C:\Users\andy\git\LemonC\jars\jasmin.jar
```
2. 构建可执行jar
```
	mvn assembly:assembly
```
3. 运行
```
	java -jar LemonC-0.1-beta-jar-with-dependencies.jar CalHeightOfChild.lemon
```
4. 运行
```
	java CalHeightOfChild
```
