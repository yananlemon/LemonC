package site.ilemon.lexer;

import site.ilemon.exception.LexException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    private final String source;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    public List<Token> tokens = new ArrayList<Token>();  // public for test compatibility
    private int tokenIndex = 0;
    private final String className;

    private static final Map<String, TokenKind> KEYWORDS = new HashMap<String, TokenKind>();

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
        KEYWORDS.put("for", TokenKind.For);
        KEYWORDS.put("printf", TokenKind.Printf);
        KEYWORDS.put("printLine", TokenKind.PrintLine);
        KEYWORDS.put("return", TokenKind.Return);
        KEYWORDS.put("break", TokenKind.Break);
        KEYWORDS.put("continue", TokenKind.Continue);
    }

    public Lexer(File f) throws IOException {
        this.className = f.getName().substring(0, f.getName().lastIndexOf("."));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            this.source = sb.toString();
        }
    }

    public String getClassName() {
        return this.className;
    }

    public String getSourceLine(int lineNumber) {
        if (lineNumber < 1) {
            return "";
        }
        int currentLine = 1;
        int start = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                if (currentLine == lineNumber) {
                    int end = i;
                    if (end > start && source.charAt(end - 1) == '\r') {
                        end--;
                    }
                    return source.substring(start, end);
                }
                currentLine++;
                start = i + 1;
            }
        }
        if (currentLine == lineNumber) {
            return source.substring(start);
        }
        return "";
    }

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

    private char peek() {
        if (position >= source.length()) {
            return '\0';
        }
        return source.charAt(position);
    }

    private char peek(int offset) {
        int index = position + offset;
        if (index >= source.length()) {
            return '\0';
        }
        return source.charAt(index);
    }

    private char advance() {
        if (position >= source.length()) {
            return '\0';
        }
        char c = source.charAt(position++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void skipWhitespace() {
        while (position < source.length()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peek(1) == '*') {
                skipMultilineComment();
            } else {
                break;
            }
        }
    }

    private void skipMultilineComment() {
        int startLine = line;
        int startColumn = column;
        advance();
        advance();
        while (position < source.length()) {
            if (peek() == '*' && peek(1) == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        throw lexicalError("unclosed multiline comment", startLine, startColumn);
    }

    private LexerState getNextState(LexerState state, char c) {
        switch (state) {
            case START:
                if (isIdentifierStart(c)) return LexerState.IN_ID;
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
                if (isIdentifierPart(c)) return LexerState.IN_ID;
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
                if (c == '\0' || c == '\n') return LexerState.ERROR;
                return LexerState.IN_STRING;

            case IN_COMMENT:
                if (c == '\n' || c == '\0') return LexerState.DONE;
                return LexerState.IN_COMMENT;

            case IN_ASSIGN:
            case IN_LT:
            case IN_GT:
            case IN_NOT:
                return LexerState.DONE;

            case IN_AND:
                if (c == '&') return LexerState.DONE;
                return LexerState.ERROR;

            case IN_OR:
                if (c == '|') return LexerState.DONE;
                return LexerState.ERROR;

            case IN_DIV:
                if (c == '/') return LexerState.IN_COMMENT;
                return LexerState.DONE;

            default:
                return LexerState.ERROR;
        }
    }

    private boolean isSingleCharToken(char c) {
        return c == '+' || c == '-' || c == '*' || c == '%' ||
               c == '{' || c == '}' || c == '(' || c == ')' ||
               c == '[' || c == ']' || c == ';' || c == ',' || c == '.';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

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
            case ',': return TokenKind.Comma;
            case '.': return TokenKind.Dot;
            default: return TokenKind.Unknown;
        }
    }

    private Token nextToken() {
        skipWhitespace();

        if (position >= source.length()) {
            return new Token(TokenKind.EOF, "EOF", line, column);
        }

        LexerState state = LexerState.START;
        StringBuilder lexeme = new StringBuilder();
        int startLine = line;
        int startColumn = column;

        while (state != LexerState.DONE && state != LexerState.ERROR) {
            char c = peek();
            LexerState nextState = getNextState(state, c);

            if (nextState == LexerState.DONE) {
                if (shouldConsumeOnDone(state, c)) {
                    lexeme.append(advance());
                }
                if (state == LexerState.IN_COMMENT) {
                    return nextToken();
                }
                break;
            } else if (nextState == LexerState.ERROR) {
                if (state == LexerState.START) {
                    advance();
                    throw lexicalError("illegal character '" + printable(c) + "'", startLine, startColumn);
                }
                throw lexicalError(errorMessageForState(state), startLine, startColumn);
            } else {
                lexeme.append(advance());
                state = nextState;
            }
        }

        return makeToken(state, lexeme.toString(), startLine, startColumn);
    }

    private boolean shouldConsumeOnDone(LexerState state, char c) {
        switch (state) {
            case START:
                return isSingleCharToken(c);
            case IN_STRING:
                return c == '"';
            case IN_ASSIGN:
                return c == '=';
            case IN_LT:
                return c == '=';
            case IN_GT:
                return c == '=';
            case IN_NOT:
                return c == '=';
            case IN_AND:
                return c == '&';
            case IN_OR:
                return c == '|';
            case IN_DIV:
                return false;
            default:
                return false;
        }
    }

    private Token makeToken(LexerState state, String lexeme, int line, int column) {
        switch (state) {
            case START:
                if (lexeme.length() == 1) {
                    return new Token(getSingleCharTokenKind(lexeme.charAt(0)), lexeme, line, column);
                }
                break;

            case IN_ID:
                TokenKind kind = KEYWORDS.get(lexeme);
                if (kind != null) {
                    return new Token(kind, lexeme, line, column);
                }
                return new Token(TokenKind.Id, lexeme, line, column);

            case IN_NUM:
                return new Token(TokenKind.Num, lexeme, line, column);

            case IN_FLOAT:
                return new Token(TokenKind.FloatLiteral, lexeme, line, column);

            case IN_STRING:
                String str = lexeme.substring(1);
                if (str.endsWith("\"")) {
                    str = str.substring(0, str.length() - 1);
                }
                return new Token(TokenKind.String, str, line, column);

            case IN_ASSIGN:
                if (lexeme.equals("==")) return new Token(TokenKind.EQ, lexeme, line, column);
                return new Token(TokenKind.Assign, lexeme, line, column);

            case IN_LT:
                if (lexeme.equals("<=")) return new Token(TokenKind.LTE, lexeme, line, column);
                return new Token(TokenKind.LT, lexeme, line, column);

            case IN_GT:
                if (lexeme.equals(">=")) return new Token(TokenKind.GTE, lexeme, line, column);
                return new Token(TokenKind.GT, lexeme, line, column);

            case IN_NOT:
                if (lexeme.equals("!=")) return new Token(TokenKind.NEQ, lexeme, line, column);
                return new Token(TokenKind.Not, lexeme, line, column);

            case IN_AND:
                return new Token(TokenKind.And, lexeme, line, column);

            case IN_OR:
                return new Token(TokenKind.Or, lexeme, line, column);

            case IN_DIV:
                return new Token(TokenKind.Div, lexeme, line, column);

            default:
                break;
        }
        throw lexicalError("unknown token '" + lexeme + "'", line, column);
    }

    private LexException lexicalError(String message, int lineNumber, int columnNumber) {
        String sourceLine = getSourceLine(lineNumber);
        StringBuilder result = new StringBuilder();
        result.append(String.format("[lexical analysis] line %d, column %d: %s",
                lineNumber, columnNumber, message));
        if (sourceLine != null && !sourceLine.isEmpty()) {
            result.append(System.lineSeparator());
            result.append("    ").append(sourceLine).append(System.lineSeparator());
            result.append("    ");
            for (int i = 1; i < columnNumber; i++) {
                result.append(' ');
            }
            result.append('^');
        }
        return new LexException(result.toString());
    }

    private String errorMessageForState(LexerState state) {
        if (state == LexerState.IN_STRING) {
            return "unclosed string literal";
        }
        if (state == LexerState.IN_AND) {
            return "illegal logical operator '&', did you mean '&&'?";
        }
        if (state == LexerState.IN_OR) {
            return "illegal logical operator '|', did you mean '||'?";
        }
        return "invalid token";
    }

    private String printable(char c) {
        if (c == '\0') {
            return "EOF";
        }
        if (c == '\n') {
            return "\\n";
        }
        if (c == '\r') {
            return "\\r";
        }
        if (c == '\t') {
            return "\\t";
        }
        return String.valueOf(c);
    }
}
