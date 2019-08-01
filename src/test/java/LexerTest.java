import site.ilemon.lexer.Lexer;

import java.io.File;
import java.io.IOException;

/**
 * Created by andy on 2019/7/31.
 */
public class LexerTest {

    @org.junit.Test
    public void testCal(){
        try {
            //Lexer lexer=new Lexer(new File("examples/Cal.lemon"));
            Lexer lexer=new Lexer(new File("examples/BoolTest01.lemon"));
            lexer.lexicalAnalysis();
            System.out.println(lexer.tokens);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
