import site.ilemon.ast.Ast;
import site.ilemon.codegen.ByteCodeGenerator;
import site.ilemon.codegen.TranslatorVisitor;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;
import site.ilemon.semantic.SemanticVisitor;

import java.io.File;
import java.io.IOException;

/**
 * Created by andy on 2019/7/31.
 */
public class CompilerTest {

    @org.junit.Test
    public void testCal(){
         try {
            //Lexer lexer=new Lexer(new File("examples/Cal01.lemon")); ok
             // Lexer lexer=new Lexer(new File("examples/Cal.lemon")); ok
             // Lexer lexer=new Lexer(new File("examples/IterationDemo.lemon")); // ok
             //Lexer lexer=new Lexer(new File("examples/BoolTest01.lemon")); ok
             Lexer lexer=new Lexer(new File("examples/MulTable.lemon"));
            Parser parser = new Parser(lexer);
            Ast.Program.T programSingle = parser.parse();
            SemanticVisitor semanticVisitor = new SemanticVisitor();
            semanticVisitor.visit(programSingle);
            if( !semanticVisitor.passOrNot()){
                System.out.println("语义分析有错");
                System.exit(1);
            }
            TranslatorVisitor translatorVisitor = new TranslatorVisitor();
            translatorVisitor.visit(programSingle);

            ByteCodeGenerator generator = new ByteCodeGenerator();
            generator.visit(translatorVisitor.prog);

            jasmin.Main.main(new String[]{translatorVisitor.prog.mainClass.id + ".il"});
        } catch (IOException e) {
            e.printStackTrace();
        }/**/

        //jasmin.Main.main(new String[]{"Cal01.il"});

    }
}
