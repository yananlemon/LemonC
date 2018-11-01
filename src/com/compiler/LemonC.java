package com.compiler;

import java.io.File;
import java.io.IOException;

import com.codegen.ByteCodeGenerator;
import com.codegen.TranslatorVisitor;
import com.lexer.Lexer;
import com.parser.Ast;
import com.parser.Parser;
import com.semantic.Semantic;

public class LemonC {

	public static void main(String[] args) {
		Lexer lexer;
		try {
			//lexer = new Lexer(new File("test_scripts/Example01.lemon"));
			//lexer = new Lexer(new File("test_scripts/Example02.lemon"));
			lexer = new Lexer(new File("test_scripts/Example03.lemon"));
			Parser parser = new Parser(lexer);
			Ast.Program.ProgramSingle program = parser.parse();
			Semantic semantic = new Semantic();
			semantic.visit(program);
			TranslatorVisitor translator = new TranslatorVisitor();
			translator.visit(program);

			ByteCodeGenerator generator = new ByteCodeGenerator();
			generator.visit(translator.prog);
			jasmin.Main.main(new String[]{translator.prog.mainClass.id + ".il"});
		} catch (IOException e) {
			e.printStackTrace();
		}
		//jasmin.Main.main(new String[]{"TestMain" + ".il"});
	}

}
