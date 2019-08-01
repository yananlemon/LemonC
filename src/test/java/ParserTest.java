import org.junit.Assert;
import org.junit.Before;
import site.ilemon.ast.Ast;
import site.ilemon.lexer.Lexer;
import site.ilemon.parser.Parser;

import java.io.File;
import java.io.IOException;

/**
 * Created by andy on 2019/7/31.
 */
public class ParserTest {

    private Lexer lexer;
    private Parser parser;

    @Before
    public void init() throws IOException{
        //File file = new File("examples/Cal.lemon");
        //File file = new File("examples/CalHeightOfChild.lemon");
        File file = new File("examples/BoolTest01.lemon");
        lexer=new Lexer(file);
        parser = new Parser(lexer);
    }

    @org.junit.Test
    public void testParser() throws IOException{
        Ast.MainClass.MainClassSingle mainClassSingle = parser.parse();
        Assert.assertNotNull(mainClassSingle);
    }
}
