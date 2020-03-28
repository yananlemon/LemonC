 A compiler front realization based on Java,it can generate Java byte code and then running on JVM.The LemonC include:
1. lexical analysis
2. syntactic parsing
3. semantic analysis
4. code generation
The following steps explain how to run.
1. require：  
	(1)jdk1.8+  
	(2)maven3.3+
2. use mvn install command to intall jasmin.jar to your local repository
```
	mvn install:install-file -DgroupId=com.jasmin -DartifactId=jasmin -Dversion=1.0 -Dpackaging=jar -Dfile=/Users/yanan/git/LemonC/jars/jasmin.jar
```
3. Build executable jar
```
	mvn assembly:assembly
```
4. compile the lemon language with LemonC
```
	java -jar LemonC-0.1-beta-jar-with-dependencies.jar CalHeightOfChild.lemon
```
5. run with java
```
	java CalHeightOfChild
```
please refer this link to get more information[more information](https://github.com/yananlemon/LemonC/blob/master/document/LemonC%20%E7%BC%96%E8%AF%91%E5%99%A8%E8%AF%B4%E6%98%8E.md)
