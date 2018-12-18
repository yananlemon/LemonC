package com.compiler;

import java.io.File;
import java.io.IOException;

import com.ast.forparse.MainClass;
import com.codegen.ByteCodeGenerator2;
import com.codegen.TranslatorVisitor2;
import com.lexer.Lexer;
import com.parser.Parser2;
import com.semantic.Semantic2;

public class LemonC2 {

	public static void main(String[] args) {
		Lexer lexer;
		try {
			//lexer = new Lexer(new File("test_scripts/Gauss.lemon"));ok
			//lexer = new Lexer(new File("test_scripts/Return.lemon"));ok
			//lexer = new Lexer(new File("test_scripts/Cal.lemon"));ok
			//lexer = new Lexer(new File("test_scripts/Fib.lemon"));ok
			//lexer = new Lexer(new File("test_scripts/SimpleMethodCall.lemon"));
			//lexer = new Lexer(new File("test_scripts/SimpleMethodCallFour.lemon"));
			//lexer = new Lexer(new File("test_scripts/SimpleMethodCallThree.lemon"));
			lexer = new Lexer(new File("test_scripts/CalHeightOfChild.lemon"));
			
			Parser2 parser = new Parser2(lexer);
			MainClass program = parser.parse();
			Semantic2 semantic = new Semantic2();
			semantic.visit(program);
			TranslatorVisitor2 translator = new TranslatorVisitor2(semantic);
			translator.visit(program);
			ByteCodeGenerator2 generator = new ByteCodeGenerator2();
			generator.visit(translator.prog);
			jasmin.Main.main(new String[]{translator.prog.name + ".il"});
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		//jasmin.Main.main(new String[]{"SimpleMethodCallThree" + ".il"});
	}

}
