package site.ilemon.compiler;

import site.ilemon.ast.Ast;
import site.ilemon.codegen.X86_64Generator;
import site.ilemon.exception.CompilerException;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;

/**
 * LemonC Native 编译器入口
 * 
 * 将 LemonC 源码编译为 Windows x64 原生可执行文件
 */
public class LemonCNative {

    public static void main(String[] args) {
        try {
            if (args == null || args.length != 1) {
                System.err.println("使用示例: java -jar LemonCNative.jar HelloNative.lemon");
                System.exit(1);
            }
            
            if (!args[0].endsWith(".lemon")) {
                System.err.println("错误: 源文件必须以 .lemon 结尾，但得到: " + args[0]);
                System.exit(1);
            }
            
            File sourceFile = new File(args[0]);
            if (!sourceFile.exists()) {
                System.err.println("错误: 文件不存在 - " + args[0]);
                System.exit(1);
            }
            
            System.out.println("=== LemonC Native Compiler ===");
            System.out.println("源文件: " + sourceFile.getName());
            System.out.println();
            
            // 1. 词法分析
            System.out.print("[1/5] 词法分析... ");
            Lexer lexer = new Lexer(sourceFile);
            System.out.println("✓");
            
            // 2. 语法分析
            System.out.print("[2/5] 语法分析... ");
            Parser parser = new Parser(lexer);
            Ast.Program.T program = parser.parse();
            System.out.println("✓");
            
            // 3. 语义分析
            System.out.print("[3/5] 语义分析... ");
            SemanticVisitor semantic = new SemanticVisitor();
            semantic.visit(program);
            if (!semantic.passOrNot()) {
                System.err.println("✗");
                System.err.println("编译失败: 语义分析有错");
                System.exit(1);
            }
            System.out.println("✓");
            
            // 4. 生成 x86-64 汇编
            System.out.print("[4/5] 生成汇编代码... ");
            String baseName = sourceFile.getName().replace(".lemon", "");
            File asmFile = new File(baseName + ".asm");
            X86_64Generator generator = new X86_64Generator(asmFile);
            generator.visit(program);
            System.out.println("✓");
            System.out.println("      生成文件: " + asmFile.getName());
            
            // 5. 汇编和链接
            System.out.print("[5/5] 汇编和链接... ");
            String exeFile = baseName + ".exe";
            assemble(asmFile.getPath(), exeFile);
            System.out.println("✓");
            System.out.println("      生成文件: " + exeFile);
            
            System.out.println();
            System.out.println("编译成功！");
            System.out.println("运行: " + exeFile);
            
        } catch (CompilerException e) {
            System.err.println("✗");
            System.err.println("编译失败: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("✗");
            System.err.println("IO错误: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("✗");
            System.err.println("未知错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void assemble(String asmFile, String exeFile) throws Exception {
        // 检查 NASM 是否安装
        if (!checkCommand("nasm -v")) {
            throw new RuntimeException(
                "未找到 NASM 汇编器！\n" +
                "请从 https://www.nasm.us/ 下载并安装 NASM\n" +
                "确保 nasm.exe 在 PATH 环境变量中");
        }
        
        // 1. 汇编：.asm -> .obj
        String objFile = asmFile.replace(".asm", ".obj");
        exec("nasm -f win64 " + asmFile + " -o " + objFile);
        
        // 2. 链接：.obj -> .exe
        // 尝试使用 GoLink（轻量级链接器）
        if (checkCommand("golink /? >nul 2>&1")) {
            exec("golink /console /entry main " + objFile + 
                 " msvcrt.dll kernel32.dll");
        }
        // 尝试使用 Visual Studio 的 link.exe
        else if (checkCommand("link /? >nul 2>&1")) {
            exec("link /subsystem:console /entry:main " + objFile + 
                 " /defaultlib:msvcrt.lib /defaultlib:kernel32.lib /out:" + exeFile);
        }
        // 尝试使用 MinGW 的 ld
        else if (checkCommand("ld --version >nul 2>&1")) {
            exec("ld " + objFile + " -o " + exeFile + 
                 " -lmsvcrt -lkernel32 -e main");
        }
        else {
            throw new RuntimeException(
                "未找到链接器！\n" +
                "请安装以下任一工具：\n" +
                "1. Visual Studio (包含 link.exe)\n" +
                "2. MinGW-w64 (包含 ld.exe)\n" +
                "3. GoLink (https://www.godevtool.com/)");
        }
    }
    
    private static boolean checkCommand(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("cmd /c " + cmd);
            p.waitFor();
            return p.exitValue() == 0 || p.exitValue() == 1; // 某些命令返回1也是正常的
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void exec(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec("cmd /c " + cmd);
        p.waitFor();
        if (p.exitValue() != 0) {
            // 读取错误输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
            throw new RuntimeException("命令执行失败: " + cmd + "\n" + error.toString());
        }
    }
}
