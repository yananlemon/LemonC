package site.ilemon.compiler;

import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.codegen.ast.Label;
import site.ilemon.exception.CompilerException;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;

/**
 * LemonC 编译器入口。
 *
 * <p>串联完整的编译管线：</p>
 * <pre>
 * 源文件(.lemon) → Lexer → Parser → SemanticVisitor → TranslatorVisitor → ByteCodeGenerator → Jasmin → .class
 * </pre>
 *
 * <p>用法：{@code java -jar LemonC.jar HelloWorld.lemon}</p>
 *
 * @author andy
 */
public class LemonC {

	public static void main(String[] args) {
		Lexer lexer;
		try {
			if( args == null || args.length != 1){
				System.err.println("使用示例: java -jar LemonC.jar HelloWorld.lemon");
				System.exit(1);
			}
			if( !args[0].endsWith(".lemon") ){
				System.err.println("错误: 源文件必须以 .lemon 结尾，但得到: " + args[0]);
				System.err.println("使用示例: java -jar LemonC.jar HelloWorld.lemon");
				System.exit(1);
			}
			File sourceFile = new File(args[0]);
			if( !sourceFile.exists() ){
				System.err.println("错误: 文件不存在 - " + args[0]);
				System.exit(1);
			}
			if( !sourceFile.canRead() ){
				System.err.println("错误: 文件无法读取 - " + args[0]);
				System.exit(1);
			}

			// 每次编译前重置 label 计数器
			Label.resetCounter();

			lexer = new Lexer(sourceFile);
			
			Parser parser = new Parser(lexer);
			Ast.Program.T programSingle = parser.parse();
			SemanticVisitor semantic = new SemanticVisitor();
			semantic.visit(programSingle);
			if( !semantic.passOrNot() ){
				System.err.println("编译失败: 语义分析有错");
				System.exit(1);
			}
				
			TranslatorVisitor translator = new TranslatorVisitor();
			translator.visit(programSingle);
			ByteCodeGenerator generator = new ByteCodeGenerator();
			generator.visit(translator.prog);
			jasmin.Main.main(new String[]{translator.prog.mainClass.id + ".il"});
			
			
		} catch (CompilerException e) {
			System.err.println("编译失败: " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.err.println("IO错误: " + e.getMessage());
			System.exit(1);
		}
	}

}
