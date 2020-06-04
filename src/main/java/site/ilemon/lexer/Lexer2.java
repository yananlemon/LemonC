package site.ilemon.lexer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>基于DFA实现的词法分析器</p>
 * @author Andy.Yan
 */
public class Lexer2 {
    
    /*词法分析器当前所读入的字符*/
    private char currChar;

    /*输入流字符索引*/
    private int index = 0;

    /*缓冲流*/
    private BufferedReader reader;

    /*源程序字符串表示*/
    private StringBuffer source;

    /*DFA初始状态*/
    private DFAState state = DFAState.INITIAL;

    /*临时token*/
    private StringBuffer bufferToken = new StringBuffer();

    /*源程序行号*/
    private int lineNumber = 1;

    /*保留字哈希表*/
    private HashMap<String,Token> reserve = new HashMap<>();

    /*token集合*/
    private List<Token> tokens = new ArrayList<>();

    /**
     * 根据给定的源程序文件{@code file},构造词法分析器.
     * @param file Lemon源程序文件
     * @throws IOException
     */
    public Lexer2(File file) throws IOException {
        // 构建保留字映射表
        this.reserve.put("class",new Token(TokenKind.KEYWORD,"class"));
        this.reserve.put("void",new Token(TokenKind.KEYWORD,"void"));
        this.reserve.put("main",new Token(TokenKind.KEYWORD,"main"));
        this.reserve.put("int",new Token(TokenKind.KEYWORD,"int"));
        this.reserve.put("float",new Token(TokenKind.KEYWORD,"float"));
        this.reserve.put("bool",new Token(TokenKind.KEYWORD,"bool"));
        this.reserve.put("true",new Token(TokenKind.KEYWORD,"true"));
        this.reserve.put("false",new Token(TokenKind.KEYWORD,"false"));
        this.reserve.put("if",new Token(TokenKind.KEYWORD,"if"));
        this.reserve.put("else",new Token(TokenKind.KEYWORD,"else"));
        this.reserve.put("while",new Token(TokenKind.KEYWORD,"while"));
        this.reserve.put("return",new Token(TokenKind.KEYWORD,"return"));
        this.reserve.put("printf",new Token(TokenKind.KEYWORD,"printf"));

        //读取源程序文件
        this.reader=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
        this.source=new StringBuffer();
        readFile();
    }


    /**
     * 读取源程序文件内容到source
     * @throws IOException
     */
    private void readFile() throws IOException{
        int c=-1;
        while((c=reader.read())!=-1){
            source.append(String.valueOf((char)c));
        }
    }

    /**
     * 回滚索引
     */
    private void rollback(){
        index--;
    }


    /**
     * 读取源程序并切分token
     * @throws IOException
     */
    private void tokenize() throws IOException{
        int c;

        while(index < source.length()-1){
            switch (state){

                // 初始状态
                case INITIAL:
                    c = getChar();
                    switch (c){
                        // 处理空格，制表符，换行
                        case ' ':
                        case '\t':
                        case '\n':
                        case '\r':
                            if( c == '\n')
                                this.lineNumber++;
                            break;

                        // 处理标识符，关键字：
                        case 'a':case 'b':case 'c':case 'd':
                        case 'e':case 'f':case 'g':case 'h':
                        case 'i':case 'j':case 'k':case 'l':
                        case 'm':case 'n':case 'o':case 'p':
                        case 'q':case 'r':case 's':case 't':
                        case 'u':case 'v':case 'w':case 'x':
                        case 'y':case 'z':
                        case 'A':case 'B':case 'C':case 'D':
                        case 'E':case 'F':case 'G':case 'H':
                        case 'I':case 'J':case 'K':case 'L':
                        case 'M':case 'N':case 'O':case 'P':
                        case 'Q':case 'R':case 'S':case 'T':
                        case 'U':case 'V':case 'W':case 'X':
                        case 'Y':case 'Z':
                            bufferToken.append((char)c);
                            c = getChar();
                            if(Character.isAlphabetic(c) || c == '_' || Character.isDigit(c)){
                                bufferToken.append((char)c);

                            }else{
                                rollback();
                            }
                            state = DFAState.IDENTIFIER;
                            break;

                        // 处理注释
                        case '/':
                            bufferToken.append((char)c);
                            c = getChar();
                            if( c == '/'){
                                while( c != '\n' ){
                                    c = getChar();
                                }
                                this.lineNumber++;
                                bufferToken.delete(0,bufferToken.length());
                            }else{
                                state = DFAState.ARITH_OPERATION;
                                rollback();
                            }
                            break;

                        // 处理数字
                        case '0':case '1':case '2':case '3':case '4':
                        case '5':case '6':case '7':case '8':case '9':
                            bufferToken.append((char)c);
                            state = DFAState.DIGITAL;
                            break;

                        // 处理'='
                        case '=':
                            bufferToken.append((char)c);
                            state = DFAState.ASSIGN;
                            break;

                        // 处理'>'
                        case '>':
                            bufferToken.append((char)c);
                            state = DFAState.GREATER_THAN;
                            break;

                        // 处理'<'
                        case '<':
                            bufferToken.append((char)c);
                            state = DFAState.LESS_THAN;
                            break;

                        // 处理'!'
                        case '!':
                            bufferToken.append((char)c);
                            state = DFAState.NOT;
                            break;

                        // 处理'|'
                        case '|':
                            bufferToken.append((char)c);
                            state = DFAState.OR;
                            break;

                        // 处理'&'
                        case '&':
                            bufferToken.append((char)c);
                            state = DFAState.AND;
                            break;

                        // 处理'+','-','*'，'/'
                        case '+': case '-': case '*':
                            bufferToken.append((char)c);
                            state = DFAState.ARITH_OPERATION;
                            break;

                        // 处理'('
                        case '(':
                            bufferToken.append((char)c);
                            state = DFAState.LEFT_PAREN;
                            break;

                        // 处理'!'
                        case ')':
                            bufferToken.append((char)c);
                            state = DFAState.RIGHT_PAREN;
                            break;

                        // 处理';'
                        case ';':
                            bufferToken.append((char)c);
                            state = DFAState.SEMICOLON;
                            break;

                        // 处理','
                        case ',':
                            bufferToken.append((char)c);
                            state = DFAState.COMMA;
                            break;

                        // 处理'{'
                        case '{':
                            bufferToken.append((char)c);
                            state = DFAState.LEFT_BRACE;
                            break;

                        // 处理'}'
                        case '}':
                            bufferToken.append((char)c);
                            state = DFAState.RIGHT_BRACE;
                            break;

                        // 处理' " '
                        case '"':
                            state = DFAState.DOUBLE_QUOTATION;
                            break;
                        // 处理异常情况
                        default:
                            error(String.valueOf((char)c));
                            break;
                    }
                    break;

                // 标识符状态
                case IDENTIFIER:
                    c = getChar();
                    while( Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' ){
                        bufferToken.append((char)c);
                        c = getChar();
                    }
                    Token t = reserve.get(bufferToken.toString());
                    if(t == null ){ // 是标识符
                        tokens.add(new Token(TokenKind.Id,bufferToken.toString(),this.lineNumber));
                    }else{ // 是关键字
                        t.setLineNumber(this.lineNumber);
                        tokens.add(t);
                    }
                    rollback();
                    initDFAState(bufferToken);
                    break;

                // 数字状态
                case DIGITAL:
                    c = getChar();
                    while( Character.isDigit(c) ){
                        bufferToken.append((char)c);
                        c = getChar();
                    }
                    if( c == '.'){
                        bufferToken.append((char)c);
                        c = getChar();
                        while( Character.isDigit(c) ){
                            bufferToken.append((char)c);
                            c = getChar();
                        }
                        tokens.add( new Token(TokenKind.DNum,bufferToken.toString(),this.lineNumber));
                    }else{
                        tokens.add( new Token(TokenKind.Num,bufferToken.toString(),this.lineNumber));
                        rollback();

                    }
                    initDFAState(bufferToken);
                    break;
                case ASSIGN:
                    c = getChar();
                    if( c == '=' ){
                        tokens.add( new Token(TokenKind.EQ,bufferToken.toString(),this.lineNumber));
                    }else{
                        tokens.add(new Token(TokenKind.Assign,bufferToken.toString(),this.lineNumber));
                        rollback();
                    }
                    initDFAState(bufferToken);
                    break;
                case GREATER_THAN:
                    c = getChar();
                    if( c == '=' ){
                        tokens.add( new Token(TokenKind.GTE,bufferToken.toString(),this.lineNumber));
                    }else{
                        tokens.add(new Token(TokenKind.GT,bufferToken.toString(),this.lineNumber));
                        rollback();
                    }
                    initDFAState(bufferToken);
                    break;
                case LESS_THAN:
                    c = getChar();
                    if( c == '=' ){
                        tokens.add( new Token(TokenKind.LTE,bufferToken.toString(),this.lineNumber));
                    }else{
                        tokens.add(new Token(TokenKind.LT,bufferToken.toString(),this.lineNumber));
                        rollback();
                    }
                    initDFAState(bufferToken);
                    break;
                case NOT:
                    c = getChar();
                    if( c == '=' ){
                        tokens.add( new Token(TokenKind.NEQ,bufferToken.toString(),this.lineNumber));
                    }else{
                        tokens.add(new Token(TokenKind.Not,bufferToken.toString(),this.lineNumber));
                        rollback();
                    }
                    initDFAState(bufferToken);
                    break;

                case OR:
                    c = getChar();
                    bufferToken.append((char)c);
                    if( c == '|' ){
                        tokens.add( new Token(TokenKind.Or,bufferToken.toString(),this.lineNumber));
                    }
                    initDFAState(bufferToken);
                    break;
                case AND:
                    c = getChar();
                    bufferToken.append((char)c);
                    if( c == '&' ){
                        tokens.add( new Token(TokenKind.And,bufferToken.toString(),this.lineNumber));
                    }
                    initDFAState(bufferToken);
                    break;
                case ARITH_OPERATION:
                    switch ( bufferToken.toString()){
                        case "+":
                            tokens.add( new Token(TokenKind.Add,bufferToken.toString(),this.lineNumber));
                            break;
                        case "-":
                            tokens.add( new Token(TokenKind.Sub,bufferToken.toString(),this.lineNumber));
                            break;
                        case "*":
                            tokens.add( new Token(TokenKind.Mul,bufferToken.toString(),this.lineNumber));
                            break;
                        case "/":
                            tokens.add( new Token(TokenKind.Div,bufferToken.toString(),this.lineNumber));
                            break;
                    }
                    initDFAState(bufferToken);
                    break;
                case LEFT_PAREN:
                    tokens.add( new Token(TokenKind.Lparen,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;
                case RIGHT_PAREN:
                    tokens.add( new Token(TokenKind.Rparen,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;
                case SEMICOLON:
                    tokens.add( new Token(TokenKind.Semicolon,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;
                case COMMA:
                    tokens.add( new Token(TokenKind.Commer,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;
                case LEFT_BRACE:
                    tokens.add( new Token(TokenKind.Lbrace,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;
                case RIGHT_BRACE:
                    tokens.add( new Token(TokenKind.Rbrace,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;
                case DOUBLE_QUOTATION:
                    c = getChar();
                    while( c != '"'){
                        bufferToken.append((char)c);
                        c = getChar();
                    }
                    tokens.add( new Token(TokenKind.String,bufferToken.toString(),this.lineNumber));
                    initDFAState(bufferToken);
                    break;

                default:
                    error(bufferToken.toString());
                    break;
            }

        }
        tokens.add( new Token(TokenKind.EOF,bufferToken.toString(),this.lineNumber));
    }

    private void initDFAState(StringBuffer bufferToken) {
        state = DFAState.INITIAL;
        bufferToken.delete(0,bufferToken.length());
    }



    /**
     * 读入输入流中下一字符到currChar，并将index++
     * @return {@link Character}
     */
    private char getChar(){
        if( index < source.length()-1) {
            currChar = source.charAt(index++);
        } else {
            currChar = (char)-1;
        }
        return currChar;
    }



    private void error(String token){
        System.out.println("错误的Token"+token+" 在行："+this.lineNumber);
    }

    public static void main(String[] args)  {

        try {
            Lexer2 lexer=new Lexer2(new File("test_script/Hello.lemon"));
            lexer.tokenize();
            for(Token t : lexer.tokens){
                System.out.println(t);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
