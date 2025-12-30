package site.ilemon.lexer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词法分析器 - 基于有限状态自动机(DFA)实现
 * 
 * 状态机核心要素：
 * 1. 状态集合 S：由 LexerState 枚举定义
 * 2. 输入字母表 Σ：源程序中的所有字符
 * 3. 状态转移函数 δ：getNextState() 方法
 * 4. 初始状态 s0：LexerState.START
 * 5. 接受状态集合 F：LexerState.DONE
 * 
 * @author andy
 * @version 2.0
 */
public class Lexer {

    private String source;              // 源程序
    private int position = 0;           // 当前读取位置
    private int line = 1;               // 当前行号
    public List<Token> tokens = new ArrayList<>();  // public for test compatibility
    private int tokenIndex = 0;
    private String className;
    private String lineSeparator = System.getProperty("line.separator");
    
    // 关键字表
    private static final Map<String, TokenKind> KEYWORDS = new HashMap<>();
    
    static {
        KEYWORDS.put("class", TokenKind.Class);
        KEYWORDS.put("main", TokenKind.Main);
        KEYWORDS.put("true", TokenKind.True);
        KEYWORDS.put("false", TokenKind.False);
        KEYWORDS.put("void", TokenKind.Void);
        KEYWORDS.put("String", TokenKind.String);
        KEYWORDS.put("int", TokenKind.Int);
        KEYWORDS.put("bool", TokenKind.Bool);
        KEYWORDS.put("float", TokenKind.Float);
        KEYWORDS.put("double", TokenKind.Double);
        KEYWORDS.put("if", TokenKind.If);
        KEYWORDS.put("else", TokenKind.Else);
        KEYWORDS.put("while", TokenKind.While);
        KEYWORDS.put("printf", TokenKind.Printf);
        KEYWORDS.put("printLine", TokenKind.PrintLine);
        KEYWORDS.put("return", TokenKind.Return);
    }

    public Lexer(File f) throws IOException {
        this.className = f.getName().substring(0, f.getName().lastIndexOf("."));
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            sb.append((char) c);
        }
        reader.close();
        this.source = sb.toString();
    }

    public String getClassName() {
        return this.className;
    }

    /**
     * 执行词法分析，将源程序转换为token序列
     */
    public void lexicalAnalysis() {
        Token token;
        while ((token = nextToken()) != null) {
            tokens.add(token);
            if (token.kind == TokenKind.EOF) {
                break;
            }
        }
    }

    public Token next() {
        if (tokenIndex < tokens.size()) {
            return tokens.get(tokenIndex++);
        }
        return null;
    }

    public Token prev() {
        if (tokenIndex > 1) {
            tokenIndex -= 2;
            return tokens.get(tokenIndex++);
        }
        return null;
    }

    public Token lookahead(int i) {
        int idx = tokenIndex - 1 + i;
        if (idx >= 0 && idx < tokens.size()) {
            return tokens.get(idx);
        }
        return null;
    }

    // ==================== 状态机核心实现 ====================

    /**
     * 查看当前字符（不移动位置）
     */
    private char peek() {
        if (position >= source.length()) {
            return '\0';
        }
        return source.charAt(position);
    }

    /**
     * 读取当前字符并前进
     */
    private char advance() {
        if (position >= source.length()) {
            return '\0';
        }
        char c = source.charAt(position++);
        // 处理换行
        if (c == '\n') {
            line++;
        } else if (c == '\r' && peek() == '\n') {
            // Windows换行，\n时再计数
        }
        return c;
    }

    /**
     * 回退一个字符
     */
    private void retreat() {
        if (position > 0) {
            position--;
            if (source.charAt(position) == '\n') {
                line--;
            }
        }
    }

    /**
     * 跳过空白字符
     */
    private void skipWhitespace() {
        while (position < source.length()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else {
                break;
            }
        }
    }

    /**
     * 状态转移函数 δ(state, char) -> newState
     * 这是DFA的核心：根据当前状态和输入字符决定下一个状态
     */
    private LexerState getNextState(LexerState state, char c) {
        switch (state) {
            case START:
                if (Character.isLetter(c)) return LexerState.IN_ID;
                if (Character.isDigit(c)) return LexerState.IN_NUM;
                if (c == '"') return LexerState.IN_STRING;
                if (c == '=') return LexerState.IN_ASSIGN;
                if (c == '<') return LexerState.IN_LT;
                if (c == '>') return LexerState.IN_GT;
                if (c == '!') return LexerState.IN_NOT;
                if (c == '&') return LexerState.IN_AND;
                if (c == '|') return LexerState.IN_OR;
                if (c == '/') return LexerState.IN_DIV;
                if (isSingleCharToken(c)) return LexerState.DONE;
                if (c == '\0') return LexerState.DONE;
                return LexerState.ERROR;

            case IN_ID:
                if (Character.isLetterOrDigit(c)) return LexerState.IN_ID;
                return LexerState.DONE;

            case IN_NUM:
                if (Character.isDigit(c)) return LexerState.IN_NUM;
                if (c == '.') return LexerState.IN_FLOAT;
                return LexerState.DONE;

            case IN_FLOAT:
                if (Character.isDigit(c)) return LexerState.IN_FLOAT;
                return LexerState.DONE;

            case IN_STRING:
                if (c == '"') return LexerState.DONE;
                if (c == '\0' || c == '\n') return LexerState.ERROR; // 未闭合字符串
                return LexerState.IN_STRING;

            case IN_COMMENT:
                if (c == '\n' || c == '\0') return LexerState.DONE;
                return LexerState.IN_COMMENT;

            case IN_ASSIGN:
                if (c == '=') return LexerState.DONE; // ==
                return LexerState.DONE; // =

            case IN_LT:
                if (c == '=') return LexerState.DONE; // <=
                return LexerState.DONE; // <

            case IN_GT:
                if (c == '=') return LexerState.DONE; // >=
                return LexerState.DONE; // >

            case IN_NOT:
                if (c == '=') return LexerState.DONE; // !=
                return LexerState.DONE; // !

            case IN_AND:
                if (c == '&') return LexerState.DONE; // &&
                return LexerState.ERROR; // 单独的 & 不合法

            case IN_OR:
                if (c == '|') return LexerState.DONE; // ||
                return LexerState.ERROR; // 单独的 | 不合法

            case IN_DIV:
                if (c == '/') return LexerState.IN_COMMENT; // 进入注释
                return LexerState.DONE; // 除法

            default:
                return LexerState.ERROR;
        }
    }

    /**
     * 判断是否为单字符token
     */
    private boolean isSingleCharToken(char c) {
        return c == '+' || c == '-' || c == '*' || c == '%' ||
               c == '{' || c == '}' || c == '(' || c == ')' ||
               c == '[' || c == ']' || c == ';' || c == ',';
    }

    /**
     * 获取单字符token的类型
     */
    private TokenKind getSingleCharTokenKind(char c) {
        switch (c) {
            case '+': return TokenKind.Add;
            case '-': return TokenKind.Sub;
            case '*': return TokenKind.Mul;
            case '%': return TokenKind.Mod;
            case '{': return TokenKind.Lbrace;
            case '}': return TokenKind.Rbrace;
            case '(': return TokenKind.Lparen;
            case ')': return TokenKind.Rparen;
            case '[': return TokenKind.Lbracket;
            case ']': return TokenKind.Rbracket;
            case ';': return TokenKind.Semicolon;
            case ',': return TokenKind.Commer;
            default: return TokenKind.Unknown;
        }
    }

    /**
     * 状态机主循环 - 识别下一个token
     * 
     * 算法：
     * 1. 从START状态开始
     * 2. 读取字符，调用转移函数获取新状态
     * 3. 累积字符到lexeme
     * 4. 到达DONE状态时，根据之前的状态生成token
     */
    private Token nextToken() {
        skipWhitespace();

        if (position >= source.length()) {
            return new Token(TokenKind.EOF, "EOF", line);
        }

        LexerState state = LexerState.START;
        LexerState prevState = LexerState.START;
        StringBuilder lexeme = new StringBuilder();
        int startLine = line;

        // 状态机主循环
        while (state != LexerState.DONE && state != LexerState.ERROR) {
            char c = peek();
            LexerState nextState = getNextState(state, c);

            if (nextState == LexerState.DONE) {
                // 判断是否需要消费当前字符
                if (shouldConsumeOnDone(state, c)) {
                    lexeme.append(advance());
                }
                // 特殊处理：注释不产生token，递归获取下一个
                if (state == LexerState.IN_COMMENT) {
                    return nextToken();
                }
                break;
            } else if (nextState == LexerState.ERROR) {
                if (state == LexerState.START) {
                    // 起始状态遇到非法字符
                    advance();
                    return new Token(TokenKind.Unknown, String.valueOf(c), startLine);
                }
                break;
            } else {
                // 继续累积字符
                lexeme.append(advance());
                prevState = state;
                state = nextState;
            }
        }

        // 根据最终状态生成token
        return makeToken(state, lexeme.toString(), startLine);
    }

    /**
     * 判断到达DONE状态时是否需要消费当前字符
     */
    private boolean shouldConsumeOnDone(LexerState state, char c) {
        switch (state) {
            case START:
                return isSingleCharToken(c); // 单字符token需要消费
            case IN_STRING:
                return c == '"'; // 消费结束引号
            case IN_ASSIGN:
                return c == '='; // ==
            case IN_LT:
                return c == '='; // <=
            case IN_GT:
                return c == '='; // >=
            case IN_NOT:
                return c == '='; // !=
            case IN_AND:
                return c == '&'; // &&
            case IN_OR:
                return c == '|'; // ||
            case IN_DIV:
                return false; // 除法不消费下一个字符
            default:
                return false; // ID、NUM等遇到非法字符时不消费
        }
    }

    /**
     * 根据状态和lexeme生成token
     */
    private Token makeToken(LexerState state, String lexeme, int line) {
        switch (state) {
            case START:
                // 单字符token
                if (lexeme.length() == 1) {
                    return new Token(getSingleCharTokenKind(lexeme.charAt(0)), lexeme, line);
                }
                break;

            case IN_ID:
                // 检查是否为关键字
                TokenKind kind = KEYWORDS.get(lexeme);
                if (kind != null) {
                    return new Token(kind, lexeme, line);
                }
                return new Token(TokenKind.Id, lexeme, line);

            case IN_NUM:
                return new Token(TokenKind.Num, lexeme, line);

            case IN_FLOAT:
                return new Token(TokenKind.DNum, lexeme, line);

            case IN_STRING:
                // 去掉首尾引号
                String str = lexeme.substring(1);
                if (str.endsWith("\"")) {
                    str = str.substring(0, str.length() - 1);
                }
                return new Token(TokenKind.String, str, line);

            case IN_ASSIGN:
                if (lexeme.equals("==")) return new Token(TokenKind.EQ, lexeme, line);
                return new Token(TokenKind.Assign, lexeme, line);

            case IN_LT:
                if (lexeme.equals("<=")) return new Token(TokenKind.LTE, lexeme, line);
                return new Token(TokenKind.LT, lexeme, line);

            case IN_GT:
                if (lexeme.equals(">=")) return new Token(TokenKind.GTE, lexeme, line);
                return new Token(TokenKind.GT, lexeme, line);

            case IN_NOT:
                if (lexeme.equals("!=")) return new Token(TokenKind.NEQ, lexeme, line);
                return new Token(TokenKind.Not, lexeme, line);

            case IN_AND:
                return new Token(TokenKind.And, lexeme, line);

            case IN_OR:
                return new Token(TokenKind.Or, lexeme, line);

            case IN_DIV:
                return new Token(TokenKind.Div, lexeme, line);

            default:
                break;
        }
        return new Token(TokenKind.Unknown, lexeme, line);
    }
}
