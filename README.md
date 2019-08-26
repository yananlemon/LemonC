使用Java实现的编译器，包括词法分析，语法分析，语义分析，代码生成。目标代码是java字节码。  
1. 环境要求：  
	(1)jdk1.8+  
	(2)maven3.3+
2. 安装jasmin.jar到本地仓库
```
	mvn install:install-file -DgroupId=com.jasmin -DartifactId=jasmin -Dversion=1.0 -Dpackaging=jar -Dfile=/Users/yanan/git/LemonC/jars/jasmin.jar
```
3. 构建可执行jar
```
	mvn assembly:assembly
```
4. 运行
```
	java -jar LemonC-0.1-beta-jar-with-dependencies.jar CalHeightOfChild.lemon
```
5. 执行
```
	java CalHeightOfChild
```
更多内容请参考[说明](https://github.com/yananlemon/LemonC/blob/master/document/LemonC%20%E7%BC%96%E8%AF%91%E5%99%A8%E8%AF%B4%E6%98%8E.md)
