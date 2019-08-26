package site.ilemon.compiler;

import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;

/**
 * LemonC 程序入口
 */
public class LemonC {

	public static void main(String[] args) {
		Lexer lexer;
		try {
			if( args == null || args.length != 1){
				System.out.println("使用示例:java LemonC HelloWorld.ilemon");
				System.exit(1);
			}
			if( args[0].lastIndexOf(".lemon") == -1 ){
				System.out.println("使用示例:java LemonC HelloWorld.ilemon");
				System.exit(1);
			}
			File sourceFile = new File(args[0]);
			lexer = new Lexer(sourceFile);
			
			Parser parser = new Parser(lexer);
			Ast.Program.T programSingle = parser.parse();
			SemanticVisitor semantic = new SemanticVisitor();
			semantic.visit(programSingle);
			if( !semantic.passOrNot() ){
				System.out.println("语义分析有错");
				System.exit(1);
			}
				
			TranslatorVisitor translator = new TranslatorVisitor();
			translator.visit(programSingle);
			ByteCodeGenerator generator = new ByteCodeGenerator();
			generator.visit(translator.prog);
			jasmin.Main.main(new String[]{translator.prog.mainClass.id + ".il"});
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
