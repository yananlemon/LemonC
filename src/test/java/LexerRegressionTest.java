import org.junit.Test;
import site.ilemon.exception.LexException;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LexerRegressionTest {

    @Test
    public void recognizesIdentifiersKeywordsOperatorsAndDelimiters() throws Exception {
        Lexer lexer = lex("class Example { void main() { int value_1; "
                + "value_1 = value_1 + 2 - 3 * 4 / 5 % 2; "
                + "if (value_1 <= 10 && value_1 != 0 || value_1 >= 1) { !false; } "
                + "float f; double d; bool b; while (b) { break; continue; } "
                + "for (;;) {} printf(\"ok\"); printLine(); return; "
                + "value_1 == 1; value_1 < 2; value_1 > 0; int values[2]; values.length; } }");

        assertKinds(lexer.getTokens(),
                TokenKind.Class, TokenKind.Id, TokenKind.Lbrace,
                TokenKind.Void, TokenKind.Main, TokenKind.Lparen, TokenKind.Rparen, TokenKind.Lbrace,
                TokenKind.Int, TokenKind.Id, TokenKind.Semicolon,
                TokenKind.Id, TokenKind.Assign, TokenKind.Id, TokenKind.Add, TokenKind.Num,
                TokenKind.Sub, TokenKind.Num, TokenKind.Mul, TokenKind.Num, TokenKind.Div,
                TokenKind.Num, TokenKind.Mod, TokenKind.Num, TokenKind.Semicolon,
                TokenKind.If, TokenKind.Lparen, TokenKind.Id, TokenKind.LTE, TokenKind.Num,
                TokenKind.And, TokenKind.Id, TokenKind.NEQ, TokenKind.Num, TokenKind.Or,
                TokenKind.Id, TokenKind.GTE, TokenKind.Num, TokenKind.Rparen, TokenKind.Lbrace,
                TokenKind.Not, TokenKind.False, TokenKind.Semicolon, TokenKind.Rbrace,
                TokenKind.Float, TokenKind.Id, TokenKind.Semicolon,
                TokenKind.Double, TokenKind.Id, TokenKind.Semicolon,
                TokenKind.Bool, TokenKind.Id, TokenKind.Semicolon,
                TokenKind.While, TokenKind.Lparen, TokenKind.Id, TokenKind.Rparen,
                TokenKind.Lbrace, TokenKind.Break, TokenKind.Semicolon,
                TokenKind.Continue, TokenKind.Semicolon, TokenKind.Rbrace,
                TokenKind.For, TokenKind.Lparen, TokenKind.Semicolon, TokenKind.Semicolon,
                TokenKind.Rparen, TokenKind.Lbrace, TokenKind.Rbrace,
                TokenKind.Printf, TokenKind.Lparen, TokenKind.StringLiteral, TokenKind.Rparen,
                TokenKind.Semicolon, TokenKind.PrintLine, TokenKind.Lparen, TokenKind.Rparen,
                TokenKind.Semicolon, TokenKind.Return, TokenKind.Semicolon,
                TokenKind.Id, TokenKind.EQ, TokenKind.Num, TokenKind.Semicolon,
                TokenKind.Id, TokenKind.LT, TokenKind.Num, TokenKind.Semicolon,
                TokenKind.Id, TokenKind.GT, TokenKind.Num, TokenKind.Semicolon,
                TokenKind.Int, TokenKind.Id, TokenKind.Lbracket, TokenKind.Num,
                TokenKind.Rbracket, TokenKind.Semicolon,
                TokenKind.Id, TokenKind.Dot, TokenKind.Id, TokenKind.Semicolon,
                TokenKind.Rbrace, TokenKind.Rbrace, TokenKind.EOF);
    }

    @Test
    public void recognizesAllSupportedNumberSpellings() throws Exception {
        Lexer lexer = lex("0 42 007 .5 1. 1.25 1e2 1E-2 1e+2 3f 4F 5d 6D");

        assertToken(lexer.getTokens().get(0), TokenKind.Num, "0");
        assertToken(lexer.getTokens().get(1), TokenKind.Num, "42");
        assertToken(lexer.getTokens().get(2), TokenKind.Num, "007");
        assertToken(lexer.getTokens().get(3), TokenKind.FloatLiteral, ".5");
        assertToken(lexer.getTokens().get(4), TokenKind.FloatLiteral, "1.");
        assertToken(lexer.getTokens().get(5), TokenKind.FloatLiteral, "1.25");
        assertToken(lexer.getTokens().get(6), TokenKind.DoubleLiteral, "1e2");
        assertToken(lexer.getTokens().get(7), TokenKind.DoubleLiteral, "1E-2");
        assertToken(lexer.getTokens().get(8), TokenKind.DoubleLiteral, "1e+2");
        assertToken(lexer.getTokens().get(9), TokenKind.FloatLiteral, "3f");
        assertToken(lexer.getTokens().get(10), TokenKind.FloatLiteral, "4F");
        assertToken(lexer.getTokens().get(11), TokenKind.DoubleLiteral, "5d");
        assertToken(lexer.getTokens().get(12), TokenKind.DoubleLiteral, "6D");
    }

    @Test
    public void recognizesHexadecimalAndOctalIntegerLiterals() throws Exception {
        Lexer lexer = lex("0x0 0x2A 0X7fffffff 077 01 0");

        assertLexemes(lexer.getTokens(), "0x0", "0x2A", "0X7fffffff", "077", "01", "0", "EOF");
        for (int i = 0; i < 6; i++) {
            assertEquals(TokenKind.Num, lexer.getTokens().get(i).getKind());
        }
    }

    @Test
    public void decodesStringsAndPreservesTokenLocationsAcrossComments() throws Exception {
        Lexer lexer = lex("// heading\r\n/* first\nsecond */\n  name \"a\\n\\t\\r\\\"\\\\\"");

        Token id = lexer.getTokens().get(0);
        assertToken(id, TokenKind.Id, "name");
        assertEquals(4, id.getLineNumber());
        assertEquals(3, id.getColumnNumber());

        Token string = lexer.getTokens().get(1);
        assertToken(string, TokenKind.StringLiteral, "a\n\t\r\"\\");
        assertEquals(4, string.getLineNumber());
        assertEquals(8, string.getColumnNumber());
    }

    @Test
    public void skipsManyConsecutiveLineCommentsWithoutRecursion() throws Exception {
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            source.append("// comment\n");
        }
        source.append("identifier");

        Lexer lexer = lex(source.toString());

        assertToken(lexer.getTokens().get(0), TokenKind.Id, "identifier");
        assertEquals(20001, lexer.getTokens().get(0).getLineNumber());
    }

    @Test
    public void reportsMalformedOperatorsStringsCommentsAndNumbers() throws Exception {
        assertLexError("&", "did you mean '&&'", 1, 1);
        assertLexError("|", "did you mean '||'", 1, 1);
        assertLexError("@", "illegal character '@'", 1, 1);
        assertLexError("\"unterminated", "unclosed string literal", 1, 1);
        assertLexError("\"bad\\q\"", "unknown escape sequence", 1, 6);
        assertLexError("/* unterminated", "unclosed multiline comment", 1, 1);
        assertLexError("1e+", "invalid exponent", 1, 1);
        assertLexError("0x", "hexadecimal integer literal requires at least one digit", 1, 1);
        assertLexError("0xFG", "invalid hexadecimal integer literal", 1, 1);
        assertLexError("09", "invalid octal integer literal", 1, 1);
        assertLexError("2147483648", "integer literal out of range", 1, 1);
        assertLexError("0x80000000", "integer literal out of range", 1, 1);
        assertLexError("020000000000", "integer literal out of range", 1, 1);
    }

    @Test
    public void scansIncrementDecrementAndCompoundAssignOperators() throws Exception {
        Lexer lexer = lex("class T { void main() { a++; b--; c += 1; d -= 1; "
                + "e *= 1; f /= 1; g %= 1; } }");

        List<TokenKind> kinds = new ArrayList<TokenKind>();
        for (Token token : lexer.getTokens()) {
            kinds.add(token.getKind());
        }
        assertTrue(kinds.toString(), kinds.contains(TokenKind.Increment));
        assertTrue(kinds.toString(), kinds.contains(TokenKind.Decrement));
        assertTrue(kinds.toString(), kinds.contains(TokenKind.AddAssign));
        assertTrue(kinds.toString(), kinds.contains(TokenKind.SubAssign));
        assertTrue(kinds.toString(), kinds.contains(TokenKind.MulAssign));
        assertTrue(kinds.toString(), kinds.contains(TokenKind.DivAssign));
        assertTrue(kinds.toString(), kinds.contains(TokenKind.ModAssign));
    }

    @Test
    public void separatedMinusSignsStayTwoSubtractionTokens() throws Exception {
        // 最长匹配：'--' 成词，但 'a - -b' 中间有空白，必须仍是两个 Sub，
        // 否则 a - -b 这种写法会被 '--' 吞掉。
        Lexer lexer = lex("class T { void main() { c = a - -b; } }");

        assertKinds(lexer.getTokens(),
                TokenKind.Class, TokenKind.Id, TokenKind.Lbrace,
                TokenKind.Void, TokenKind.Main, TokenKind.Lparen, TokenKind.Rparen,
                TokenKind.Lbrace,
                TokenKind.Id, TokenKind.Assign, TokenKind.Id, TokenKind.Sub, TokenKind.Sub,
                TokenKind.Id, TokenKind.Semicolon,
                TokenKind.Rbrace, TokenKind.Rbrace, TokenKind.EOF);
    }

    @Test
    public void divisionAssignDoesNotSwallowComments() throws Exception {
        // '/=' 与 '//'、'/*' 共享前缀，注释必须仍然先被 trivia 处理掉。
        Lexer lexer = lex("class T { void main() { a /= 2; // c\n b = 1; /* x */ c = 2; } }");

        List<TokenKind> kinds = new ArrayList<TokenKind>();
        for (Token token : lexer.getTokens()) {
            kinds.add(token.getKind());
        }
        assertTrue(kinds.toString(), kinds.contains(TokenKind.DivAssign));
        assertEquals(1, java.util.Collections.frequency(kinds, TokenKind.DivAssign));
        assertTrue(kinds.toString(), !kinds.contains(TokenKind.Div));
    }

    private static Lexer lex(String source) throws Exception {
        File directory = new File("test_tmp");
        directory.mkdirs();
        File file = File.createTempFile("lexer-regression-", ".lemon", directory);
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        Lexer lexer = new Lexer(file);
        lexer.lexicalAnalysis();
        return lexer;
    }

    private static void assertKinds(List<Token> tokens, TokenKind... expected) {
        List<TokenKind> actual = new ArrayList<TokenKind>();
        for (Token token : tokens) {
            actual.add(token.getKind());
        }
        assertEquals(Arrays.asList(expected), actual);
    }

    private static void assertLexemes(List<Token> tokens, String... expected) {
        List<String> actual = new ArrayList<String>();
        for (Token token : tokens) {
            actual.add(token.getLexeme());
        }
        assertEquals(Arrays.asList(expected), actual);
    }

    private static void assertToken(Token token, TokenKind kind, String lexeme) {
        assertEquals(kind, token.getKind());
        assertEquals(lexeme, token.getLexeme());
    }

    private static void assertLexError(String source, String message, int line, int column)
            throws Exception {
        try {
            lex(source);
            fail("Expected LexException for: " + source);
        } catch (LexException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(message));
            assertTrue(e.getMessage(), e.getMessage().contains(
                    "line " + line + ", column " + column));
        }
    }
}
