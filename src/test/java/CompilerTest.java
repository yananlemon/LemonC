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
    public void testBool() {
        try {
            // Lexer lexer=new Lexer(new File("examples/BoolTest01.lemon")); // ok at 2019.8.31
            // Lexer lexer=new Lexer(new File("examples/BoolTest03.lemon")); //ok at 2019.8.31

            //Lexer lexer=new Lexer(new File("examples/BoolTest04.lemon")); // ok at 2019.8.31
            // Lexer lexer=new Lexer(new File("examples/BoolTest05.lemon"));// ok at 2019.8.31

           // Lexer lexer=new Lexer(new File("examples/BoolTest06.lemon")); // ok at 2019.8.31
           //  Lexer lexer=new Lexer(new File("examples/BoolTest07.lemon")); //ok at 2019.8.31

           // Lexer lexer=new Lexer(new File("examples/BoolTest08.lemon")); // ok at 2019.8.31
           // Lexer lexer=new Lexer(new File("examples/BoolTest09.lemon")); // error ok at 2019.8.31

           // Lexer lexer=new Lexer(new File("examples/BoolTest10.lemon"));// ok at 2019.8.31
           // Lexer lexer=new Lexer(new File("examples/BoolTest11.lemon"));// ok at 2019.8.31

           // Lexer lexer=new Lexer(new File("examples/BoolTest12.lemon"));// ok at 2019.8.31
           //  Lexer lexer=new Lexer(new File("examples/BoolTest13.lemon"));// ok at 2019.8.31

           // Lexer lexer=new Lexer(new File("examples/BoolTest14.lemon")); // ok at 2019.8.31
           // Lexer lexer = new Lexer(new File("examples/BoolTest15.lemon")); // ok at 2019.8.31
            Lexer lexer = new Lexer(new File("examples/BoolTest16.lemon")); // ok at 2019.9.4
            doCompiler(lexer);
            //jasmin.Main.main(new String[]{"BoolTest04.il"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMethodCall() {
        try {
            //Lexer lexer=new Lexer(new File("examples/MethodCallTest01.lemon"));// ok at 2019.8.27
            //Lexer lexer=new Lexer(new File("examples/MethodCallTest02.lemon"));// ok at 2019.8.27
            // Lexer lexer = new Lexer(new File("examples/MethodCallTest03.lemon"));// ok at 2019.9.2
            Lexer lexer = new Lexer(new File("examples/MethodCallTest04.lemon")); // ok at 2019.9.2
            doCompiler(lexer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testIf() {
        try {
           //   Lexer lexer = new Lexer(new File("examples/If01.lemon")); // ok at 2019/9/1
           //Lexer lexer=new Lexer(new File("examples/If02.lemon"));  // ok at 2019/9/1

           //  Lexer lexer=new Lexer(new File("examples/If03.lemon"));// ok at 2019/9/1
            // Lexer lexer=new Lexer(new File("examples/If04.lemon")); // ok at 2019/9/1

          // Lexer lexer=new Lexer(new File("examples/If05.lemon")); // ok at 2019/9/1
           // Lexer lexer=new Lexer(new File("examples/If06.lemon")); //ok at 2019/9/1

           // Lexer lexer=new Lexer(new File("examples/If07.lemon")); // ok at 2019/9/1
           // Lexer lexer=new Lexer(new File("examples/If08.lemon")); //ok at 2019/9/1

           // Lexer lexer=new Lexer(new File("examples/If09.lemon")); //ok at 2019/9/1
           // Lexer lexer=new Lexer(new File("examples/If10.lemon")); //ok at 2019/9/1

          //  Lexer lexer=new Lexer(new File("examples/If11.lemon")); // ok at 2019/9/1
          //  Lexer lexer=new Lexer(new File("examples/If12.lemon")); // ok at 2019/9/1

            Lexer lexer=new Lexer(new File("examples/If13.lemon")); // ok at 2019/9/1
            doCompiler(lexer);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doCompiler(Lexer lexer) throws IOException {
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
            //Lexer lexer = new Lexer(new File("examples/If04.lemon"));
            Lexer lexer=new Lexer(new File("examples/If03.lemon"));
            doCompiler(lexer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFloat() {
        try {
            Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon")); // ok at 2019.9.4
            //Lexer lexer = new Lexer(new File("examples/FloatTest02.lemon")); // ok at 2019.9.4
            doCompiler(lexer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInt() {
        try {
            Lexer lexer = new Lexer(new File("examples/IntTest01.lemon")); // ok at 2019.9.4
            doCompiler(lexer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWhile() {
        try {
            // Lexer lexer = new Lexer(new File("examples/Iteration01.lemon")); // ok at 2019.8.31
            // Lexer lexer = new Lexer(new File("examples/Iteration02.lemon"));// ok at 2019.8.31
            // Lexer lexer = new Lexer(new File("examples/Iteration03.lemon"));// ok at 2019.8.31
           // Lexer lexer = new Lexer(new File("examples/MulTable.lemon"));// ok at 2019.8.31
           // Lexer lexer = new Lexer(new File("examples/Iteration05.lemon")); // ok at 2019.8.31
           // Lexer lexer = new Lexer(new File("examples/Iteration06.lemon")); // ok at 2019.8.31
           // Lexer lexer = new Lexer(new File("examples/Gauss.lemon")); // ok at 2019.9.4
           // Lexer lexer = new Lexer(new File("examples/Fib.lemon")); // ok at 2019.9.4
            Lexer lexer = new Lexer(new File("examples/IterationDemo.lemon")); // ok at 2019.9.4

            doCompiler(lexer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
