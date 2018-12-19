1.安装jasmin.jar到本地仓库
	mvn install:install-file -DgroupId=com.jasmin -DartifactId=jasmin -Dversion=1.0 -Dpackaging=jar -Dfile=C:\Users\andy\git\LemonC\jars\jasmin.jar
2.构建可执行jar
	mvn assembly:assembly
3.运行
	java -jar LemonC-0.1-beta-jar-with-dependencies.jar CalHeightOfChild.lemon
4.运行
	java CalHeightOfChild