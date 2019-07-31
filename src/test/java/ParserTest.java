import org.junit.Assert;
import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.io.IOException;

/**
 * Created by andy on 2019/7/31.
 */
public class ParserTest {

    @org.junit.Test
    public void testParser(){
        try {
            Lexer lexer=new Lexer(new File("examples/Cal.lemon"));
            Parser parser = new Parser(lexer);
            Ast.MainClass.MainClassSingle mainClassSingle = parser.parse();
            Assert.assertNotNull(mainClassSingle);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
