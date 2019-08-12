import org.junit.Test;
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

    @Test
    public void testIf() {
        try {
             Lexer lexer = new Lexer(new File("examples/If01.lemon")); // ok
            // Lexer lexer=new Lexer(new File("examples/If02.lemon"));  // ok
            //  Lexer lexer=new Lexer(new File("examples/If03.lemon"));
            // Lexer lexer=new Lexer(new File("examples/If04.lemon")); ok
            // Lexer lexer=new Lexer(new File("examples/If05.lemon")); ok
            // Lexer lexer=new Lexer(new File("examples/If06.lemon")); ok
            // Lexer lexer=new Lexer(new File("examples/If07.lemon")); ok
            // Lexer lexer=new Lexer(new File("examples/If08.lemon")); ok
            // Lexer lexer=new Lexer(new File("examples/If09.lemon")); ok
            // Lexer lexer=new Lexer(new File("examples/If10.lemon")); ok
            Parser parser = new Parser(lexer);
            Ast.Program.T programSingle = parser.parse();
            SemanticVisitor semanticVisitor = new SemanticVisitor();
            semanticVisitor.visit(programSingle);
            if (!semanticVisitor.passOrNot()) {
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
        }
    }


    @Test
    public void testCal() {
        try {
            //Lexer lexer=new Lexer(new File("examples/Cal01.lemon"));
            //Lexer lexer=new Lexer(new File("examples/Cal.lemon"));
            //Lexer lexer=new Lexer(new File("examples/IterationDemo.lemon"));

            //Lexer lexer=new Lexer(new File("examples/MulTable.lemon"));

            //Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
            //Lexer lexer=new Lexer(new File("examples/If01.lemon"));
            //Lexer lexer=new Lexer(new File("examples/If03.lemon"));
            Lexer lexer = new Lexer(new File("examples/If04.lemon"));
            //Lexer lexer=new Lexer(new File("examples/If03.lemon"));
            Parser parser = new Parser(lexer);
            Ast.Program.T programSingle = parser.parse();
            SemanticVisitor semanticVisitor = new SemanticVisitor();
            semanticVisitor.visit(programSingle);
            if (!semanticVisitor.passOrNot()) {
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
        //jasmin.Main.main(new String[]{"If01.il"});
    }
}
