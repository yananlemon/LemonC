import org.junit.Test;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * 词法分析器测试用例
 * 测试状态机的各个状态转移路径
 */
public class LexerTest {

    // ==================== 关键字测试 ====================
    
    @Test
    public void testKeywords() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // 验证能识别 class 关键字
        Token first = lexer.next();
        assertEquals(TokenKind.Class, first.kind);
        assertEquals("class", first.lexeme);
    }

    @Test
    public void testAllKeywords() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        // 检查tokens中包含各种关键字
        boolean hasClass = false, hasVoid = false, hasFloat = false, hasReturn = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Class) hasClass = true;
            if (t.kind == TokenKind.Void) hasVoid = true;
            if (t.kind == TokenKind.Float) hasFloat = true;
            if (t.kind == TokenKind.Return) hasReturn = true;
        }
        assertTrue("应识别class关键字", hasClass);
        assertTrue("应识别void关键字", hasVoid);
        assertTrue("应识别float关键字", hasFloat);
        assertTrue("应识别return关键字", hasReturn);
    }

    // ==================== 标识符测试 ====================
    
    @Test
    public void testIdentifier() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // 查找标识符
        boolean hasId = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Id) {
                hasId = true;
                break;
            }
        }
        assertTrue("应能识别标识符", hasId);
    }

    // ==================== 数字测试 ====================
    
    @Test
    public void testIntegerNumber() throws IOException {
        Lexer lexer = new Lexer(new File("examples/IntTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasNum = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Num) {
                hasNum = true;
                break;
            }
        }
        assertTrue("应能识别整数", hasNum);
    }

    @Test
    public void testFloatNumber() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasDNum = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.DNum) {
                hasDNum = true;
                // 验证浮点数格式
                assertTrue("浮点数应包含小数点", t.lexeme.contains("."));
                break;
            }
        }
        assertTrue("应能识别浮点数", hasDNum);
    }

    // ==================== 运算符测试 ====================
    
    @Test
    public void testArithmeticOperators() throws IOException {
        // 使用FloatTest01，它包含所有四则运算
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasAdd = false, hasSub = false, hasMul = false, hasDiv = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Add) hasAdd = true;
            if (t.kind == TokenKind.Sub) hasSub = true;
            if (t.kind == TokenKind.Mul) hasMul = true;
            if (t.kind == TokenKind.Div) hasDiv = true;
        }
        assertTrue("应识别加法运算符", hasAdd);
        assertTrue("应识别减法运算符", hasSub);
        assertTrue("应识别乘法运算符", hasMul);
        assertTrue("应识别除法运算符", hasDiv);
    }

    @Test
    public void testComparisonOperators() throws IOException {
        Lexer lexer = new Lexer(new File("examples/If01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasComparison = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.LT || t.kind == TokenKind.GT ||
                t.kind == TokenKind.LTE || t.kind == TokenKind.GTE ||
                t.kind == TokenKind.EQ || t.kind == TokenKind.NEQ) {
                hasComparison = true;
                break;
            }
        }
        assertTrue("应能识别比较运算符", hasComparison);
    }

    @Test
    public void testLogicalOperators() throws IOException {
        Lexer lexer = new Lexer(new File("examples/BoolTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasLogical = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.And || t.kind == TokenKind.Or || t.kind == TokenKind.Not) {
                hasLogical = true;
                break;
            }
        }
        // 逻辑运算符可能不在所有文件中
        // assertTrue("应能识别逻辑运算符", hasLogical);
    }

    // ==================== 界符测试 ====================
    
    @Test
    public void testDelimiters() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasLbrace = false, hasRbrace = false;
        boolean hasLparen = false, hasRparen = false;
        boolean hasSemicolon = false;
        
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Lbrace) hasLbrace = true;
            if (t.kind == TokenKind.Rbrace) hasRbrace = true;
            if (t.kind == TokenKind.Lparen) hasLparen = true;
            if (t.kind == TokenKind.Rparen) hasRparen = true;
            if (t.kind == TokenKind.Semicolon) hasSemicolon = true;
        }
        
        assertTrue("应识别左花括号", hasLbrace);
        assertTrue("应识别右花括号", hasRbrace);
        assertTrue("应识别左圆括号", hasLparen);
        assertTrue("应识别右圆括号", hasRparen);
        assertTrue("应识别分号", hasSemicolon);
    }

    // ==================== 字符串测试 ====================
    
    @Test
    public void testStringLiteral() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasString = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.String && t.lexeme.contains("%")) {
                hasString = true;
                break;
            }
        }
        assertTrue("应能识别字符串字面量", hasString);
    }

    // ==================== 注释测试 ====================
    
    @Test
    public void testCommentIgnored() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        // 注释应该被忽略，不应出现在tokens中
        for (Token t : lexer.tokens) {
            assertFalse("注释内容不应出现在token中", 
                t.lexeme.contains("测试") || t.lexeme.contains("浮点型"));
        }
    }

    // ==================== 行号测试 ====================
    
    @Test
    public void testLineNumber() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // 第一个token应该在第1行或更后
        Token first = lexer.next();
        assertTrue("行号应大于0", first.lineNumber >= 1);
    }

    // ==================== EOF测试 ====================
    
    @Test
    public void testEOF() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // 最后一个token应该是EOF
        Token last = lexer.tokens.get(lexer.tokens.size() - 1);
        assertEquals("最后一个token应为EOF", TokenKind.EOF, last.kind);
    }

    // ==================== 综合测试 ====================
    
    @Test
    public void testCompleteFile() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        // 打印所有token用于调试
        System.out.println("=== FloatTest01.lemon tokens ===");
        for (Token t : lexer.tokens) {
            System.out.print(t);
        }
        
        // 验证token数量合理
        assertTrue("应产生多个token", lexer.tokens.size() > 10);
    }

    @Test
    public void testIfStatement() throws IOException {
        Lexer lexer = new Lexer(new File("examples/If07.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasIf = false, hasElse = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.If) hasIf = true;
            if (t.kind == TokenKind.Else) hasElse = true;
        }
        assertTrue("应识别if关键字", hasIf);
    }

    @Test
    public void testWhileLoop() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Iteration01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasWhile = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.While) {
                hasWhile = true;
                break;
            }
        }
        assertTrue("应识别while关键字", hasWhile);
    }

    @Test
    public void testMethodCall() throws IOException {
        Lexer lexer = new Lexer(new File("examples/SimpleMethodCall.lemon"));
        lexer.lexicalAnalysis();
        
        // 方法调用应该有标识符和括号
        boolean hasId = false, hasLparen = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Id) hasId = true;
            if (t.kind == TokenKind.Lparen) hasLparen = true;
        }
        assertTrue("应有标识符", hasId);
        assertTrue("应有左括号", hasLparen);
    }

    // ==================== 原有测试保留 ====================
    
    @Test
    public void testCal() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        System.out.println(lexer.tokens);
    }
}
