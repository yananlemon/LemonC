package site.ilemon.compiler;

public class LemonC {

	public static void main(String[] args) {
		/*Lexer lexer;
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
			MainClass program = parser.parse();
			Semantic semantic = new Semantic();
			semantic.visit(program);
			if( !semantic.pass ){
				System.out.println("语义分析有错");
				System.exit(1);
			}
				
			TranslatorVisitor translator = new TranslatorVisitor(semantic);
			translator.visit(program);
			ByteCodeGenerator generator = new ByteCodeGenerator();
			generator.visit(translator.prog);
			jasmin.Main.main(new String[]{translator.prog.name + ".il"});
			
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

}
