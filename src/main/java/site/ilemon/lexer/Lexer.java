package site.ilemon.lexer;

import site.ilemon.exception.LexException;
import site.ilemon.source.SourceSpan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    private final String source;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private final List<Token> tokens = new ArrayList<Token>();
    private int tokenIndex = 0;
    private final String className;

    /** Returns an unmodifiable view of the token list. */
    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    private static final Map<String, TokenKind> KEYWORDS;

    static {
        Map<String, TokenKind> keywords = new HashMap<String, TokenKind>();
        keywords.put("class", TokenKind.Class);
        keywords.put("main", TokenKind.Main);
        keywords.put("true", TokenKind.True);
        keywords.put("false", TokenKind.False);
        keywords.put("void", TokenKind.Void);
        keywords.put("String", TokenKind.StringType);
        keywords.put("int", TokenKind.Int);
        keywords.put("bool", TokenKind.Bool);
        keywords.put("float", TokenKind.Float);
        keywords.put("double", TokenKind.Double);
        keywords.put("if", TokenKind.If);
        keywords.put("else", TokenKind.Else);
        keywords.put("while", TokenKind.While);
        keywords.put("for", TokenKind.For);
        keywords.put("printf", TokenKind.Printf);
        keywords.put("printLine", TokenKind.PrintLine);
        keywords.put("return", TokenKind.Return);
        keywords.put("break", TokenKind.Break);
        keywords.put("continue", TokenKind.Continue);
        KEYWORDS = Collections.unmodifiableMap(keywords);
    }

    public Lexer(File f) throws IOException {
        this.className = f.getName().substring(0, f.getName().lastIndexOf("."));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            String text = sb.toString();
            this.source = text.startsWith("\uFEFF") ? text.substring(1) : text;
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
            if (token.getKind() == TokenKind.EOF) {
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

    private void skipTrivia() {
        while (position < source.length()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peek(1) == '/') {
                skipLineComment();
            } else if (c == '/' && peek(1) == '*') {
                skipMultilineComment();
            } else {
                break;
            }
        }
    }

    private void skipLineComment() {
        advance();
        advance();
        while (position < source.length() && peek() != '\n') {
            advance();
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

    private Token scanStringLiteral(int startLine, int startColumn) {
        advance(); // opening quote
        StringBuilder value = new StringBuilder();

        while (position < source.length()) {
            char c = peek();
            if (c == '\0' || c == '\n' || c == '\r') {
                throw lexicalError("unclosed string literal", startLine, startColumn);
            }

            c = advance();
            if (c == '"') {
                return token(TokenKind.StringLiteral, value.toString(), startLine, startColumn);
            }

            if (c == '\\') {
                if (position >= source.length()) {
                    throw lexicalError("unclosed string literal", startLine, startColumn);
                }
                char escaped = peek();
                if (escaped == '\n' || escaped == '\r' || escaped == '\0') {
                    throw lexicalError("unclosed string literal", startLine, startColumn);
                }
                advance();
                switch (escaped) {
                    case 'n':
                        value.append('\n');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case '"':
                        value.append('"');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    default:
                        throw lexicalError("unknown escape sequence '\\" + printable(escaped) + "'", line, column - 1);
                }
            } else {
                value.append(c);
            }
        }

        throw lexicalError("unclosed string literal", startLine, startColumn);
    }

    private Token scanIdentifier(int startLine, int startColumn) {
        StringBuilder lexeme = new StringBuilder();
        while (isIdentifierPart(peek())) {
            lexeme.append(advance());
        }

        String text = lexeme.toString();
        TokenKind kind = KEYWORDS.get(text);
        return token(kind == null ? TokenKind.Id : kind, text, startLine, startColumn);
    }

    private Token scanNumberLiteral(int startLine, int startColumn) {
        if (peek() == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
            return scanHexadecimalInteger(startLine, startColumn);
        }

        StringBuilder lexeme = new StringBuilder();
        boolean hasDot = false;
        boolean hasExponent = false;

        if (peek() == '.') {
            hasDot = true;
            lexeme.append(advance());
        }

        while (Character.isDigit(peek())) {
            lexeme.append(advance());
        }

        if (peek() == '.') {
            hasDot = true;
            lexeme.append(advance());
            while (Character.isDigit(peek())) {
                lexeme.append(advance());
            }
        }

        if (peek() == 'e' || peek() == 'E') {
            hasExponent = true;
            lexeme.append(advance());
            if (peek() == '+' || peek() == '-') {
                lexeme.append(advance());
            }
            if (!Character.isDigit(peek())) {
                throw lexicalError("invalid exponent in numeric literal", startLine, startColumn);
            }
            while (Character.isDigit(peek())) {
                lexeme.append(advance());
            }
        }

        char suffix = peek();
        if (suffix == 'f' || suffix == 'F') {
            lexeme.append(advance());
            return token(TokenKind.FloatLiteral, lexeme.toString(), startLine, startColumn);
        }
        if (suffix == 'd' || suffix == 'D') {
            lexeme.append(advance());
            return token(TokenKind.DoubleLiteral, lexeme.toString(), startLine, startColumn);
        }
        if (hasExponent) {
            return token(TokenKind.DoubleLiteral, lexeme.toString(), startLine, startColumn);
        }
        if (hasDot) {
            return token(TokenKind.FloatLiteral, lexeme.toString(), startLine, startColumn);
        }

        String text = lexeme.toString();
        if (isInvalidOctal(text)) {
            throw lexicalError("invalid octal integer literal '" + text + "'", startLine, startColumn);
        }
        validateIntegerRange(text, startLine, startColumn);
        return token(TokenKind.Num, text, startLine, startColumn);
    }

    private Token scanHexadecimalInteger(int startLine, int startColumn) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(advance());
        lexeme.append(advance());

        if (!isHexDigit(peek())) {
            if (isIdentifierPart(peek())) {
                throw lexicalError("invalid hexadecimal integer literal", startLine, startColumn);
            }
            throw lexicalError("hexadecimal integer literal requires at least one digit",
                    startLine, startColumn);
        }
        while (isHexDigit(peek())) {
            lexeme.append(advance());
        }
        if (isIdentifierPart(peek())) {
            throw lexicalError("invalid hexadecimal integer literal", startLine, startColumn);
        }

        String text = lexeme.toString();
        validateIntegerRange(text, startLine, startColumn);
        return token(TokenKind.Num, text, startLine, startColumn);
    }

    private void validateIntegerRange(String text, int startLine, int startColumn) {
        try {
            IntegerLiterals.parse(text);
        } catch (NumberFormatException e) {
            throw lexicalError("integer literal out of range '" + text + "'",
                    startLine, startColumn);
        }
    }

    private boolean isInvalidOctal(String text) {
        if (text.length() <= 1 || text.charAt(0) != '0') {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '8' || c == '9') {
                return true;
            }
        }
        return false;
    }

    private boolean isHexDigit(char c) {
        return c >= '0' && c <= '9'
                || c >= 'a' && c <= 'f'
                || c >= 'A' && c <= 'F';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private Token scanOperatorOrDelimiter(int startLine, int startColumn) {
        char first = advance();
        switch (first) {
            case '+':
                if (peek() == '+') {
                    advance();
                    return token(TokenKind.Increment, "++", startLine, startColumn);
                }
                return scanOptionalEquals(TokenKind.Add, TokenKind.AddAssign,
                        first, startLine, startColumn);
            case '-':
                if (peek() == '-') {
                    advance();
                    return token(TokenKind.Decrement, "--", startLine, startColumn);
                }
                return scanOptionalEquals(TokenKind.Sub, TokenKind.SubAssign,
                        first, startLine, startColumn);
            case '*': return scanOptionalEquals(TokenKind.Mul, TokenKind.MulAssign,
                    first, startLine, startColumn);
            case '/': return scanOptionalEquals(TokenKind.Div, TokenKind.DivAssign,
                    first, startLine, startColumn);
            case '%': return scanOptionalEquals(TokenKind.Mod, TokenKind.ModAssign,
                    first, startLine, startColumn);
            case '{': return token(TokenKind.Lbrace, first, startLine, startColumn);
            case '}': return token(TokenKind.Rbrace, first, startLine, startColumn);
            case '(': return token(TokenKind.Lparen, first, startLine, startColumn);
            case ')': return token(TokenKind.Rparen, first, startLine, startColumn);
            case '[': return token(TokenKind.Lbracket, first, startLine, startColumn);
            case ']': return token(TokenKind.Rbracket, first, startLine, startColumn);
            case ';': return token(TokenKind.Semicolon, first, startLine, startColumn);
            case ',': return token(TokenKind.Comma, first, startLine, startColumn);
            case '.': return token(TokenKind.Dot, first, startLine, startColumn);
            case '=': return scanOptionalEquals(TokenKind.Assign, TokenKind.EQ,
                    first, startLine, startColumn);
            case '<': return scanOptionalEquals(TokenKind.LT, TokenKind.LTE,
                    first, startLine, startColumn);
            case '>': return scanOptionalEquals(TokenKind.GT, TokenKind.GTE,
                    first, startLine, startColumn);
            case '!': return scanOptionalEquals(TokenKind.Not, TokenKind.NEQ,
                    first, startLine, startColumn);
            case '&':
                if (peek() == '&') {
                    advance();
                    return token(TokenKind.And, "&&", startLine, startColumn);
                }
                throw lexicalError("illegal logical operator '&', did you mean '&&'?",
                        startLine, startColumn);
            case '|':
                if (peek() == '|') {
                    advance();
                    return token(TokenKind.Or, "||", startLine, startColumn);
                }
                throw lexicalError("illegal logical operator '|', did you mean '||'?",
                        startLine, startColumn);
            default:
                throw lexicalError("illegal character '" + printable(first) + "'",
                        startLine, startColumn);
        }
    }

    private Token scanOptionalEquals(TokenKind singleKind, TokenKind pairKind,
                                     char first, int startLine, int startColumn) {
        if (peek() == '=') {
            advance();
            return token(pairKind, String.valueOf(first) + '=', startLine, startColumn);
        }
        return token(singleKind, first, startLine, startColumn);
    }

    private Token token(TokenKind kind, char lexeme, int lineNumber, int columnNumber) {
        return token(kind, String.valueOf(lexeme), lineNumber, columnNumber);
    }

    private Token token(TokenKind kind, String lexeme, int startLine, int startColumn) {
        return new Token(kind, lexeme, SourceSpan.of(
                startLine, startColumn, line, column));
    }

    private Token nextToken() {
        skipTrivia();

        if (position >= source.length()) {
            return new Token(TokenKind.EOF, "EOF", SourceSpan.point(line, column));
        }

        int startLine = line;
        int startColumn = column;
        char first = peek();

        if (isIdentifierStart(first)) {
            return scanIdentifier(startLine, startColumn);
        }
        if (first == '"') {
            return scanStringLiteral(startLine, startColumn);
        }
        if (Character.isDigit(first) || (first == '.' && Character.isDigit(peek(1)))) {
            return scanNumberLiteral(startLine, startColumn);
        }
        return scanOperatorOrDelimiter(startLine, startColumn);
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
