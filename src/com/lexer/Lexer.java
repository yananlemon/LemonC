package com.lexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 词法分析
 * @author andy
 * @version 1.0
 */
public class Lexer {
	
	private BufferedReader  reader;
	private StringBuffer source;
	public int position=0;
	public int tempPosition=0;
	private List<Token> tokens=new ArrayList<Token>();
	public int line=1;
	private int index=0;
	public Lexer(File f) throws IOException{
		this.reader=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
		this.source=new StringBuffer();
		
		//读取源程序文件
		readFile();
	}

	public static void main(String[] args) {
		try {
			Lexer lexer=new Lexer(new File("test_scripts/Example02.lemon"));
			lexer.lexicalAnalysis();
			System.out.println(lexer.tokens);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void lexicalAnalysis(){
		Token token=null;
		try {
			while((token=this.nextToken()) != null){
				tokens.add(token);
				if(token.getKind() == TokenKind.EndOfFile)
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Token next(){
		if(index<tokens.size()){
			return tokens.get(index++);
		}else{
			return null;
		}
	}
	
	public Token prev(){
		if( index < tokens.size()){
			index -= 2;
			return tokens.get(index);
		}else{
			return null;
		}
	}
	
	/**
	 * 向前看i个token
	 * @param i 向前看的个数
	 * @return {@link com.lemon.lexer.Token} 
	 */
	public Token lookahead(int i){
		if(index+i<tokens.size()){
			return tokens.get(index-1+i);
		}else{
			return null;
		}
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
		source.append(String.valueOf((char)-1));
	}

	/**
	 * 每次读取一个token并返回
	 * @return Token
	 * @throws IOException
	 */
	private Token nextToken() throws IOException{
		if(position >= source.length()-1){
			line++;
			return new Token("EOF",line,TokenKind.EndOfFile);
		}
		tempPosition=position;
		int c=source.charAt(position++);
		tempPosition--;
		
		while(c==' ' || c == '\t' || c == '\r' ||c == '\n'){
			c=source.charAt(position++);
			tempPosition--;
			if(c == '\n'){
				line++;
			}
		}
		
		// 忽略注释
		if( c == '/' ){
			StringBuffer letter=new StringBuffer();
			letter.append((char)c);
			while(c == '/'){
				letter.append((char)source.charAt(position));
				position++;
				tempPosition--;
				c=source.charAt(position);
			}
			if(letter.toString().equals("//")){
				while( c != '\n' ){
					c=source.charAt(position++);
					tempPosition--;
					if(c == '\n'){
						line++;
					}
				}
			}
			return this.nextToken();
		}
		
		//如果是字母则需要判断是否是标识符,关键字
		if(Character.isLetter(c)){
			StringBuffer letter=new StringBuffer();
			letter.append((char)c);
			while(Character.isLetter(source.charAt(position))){
				letter.append((char)source.charAt(position));
				position++;
				tempPosition--;
			}
			switch (letter.toString()) {
				case "class":
					return new Token(TokenKind.Class, "class",line);
				case "main":
					return new Token(TokenKind.Main, "main",line);
				case "void":
					return new Token(TokenKind.Void, "void",line);
				case "String":
					return new Token(TokenKind.String, "String",line);
				case "int":
					return new Token(TokenKind.Int, "int",line);
				case "if":
					return new Token(TokenKind.If, "if",line);
				case "else":
					return new Token(TokenKind.Else, "else",line);
				case "while":
					return new Token(TokenKind.While, "while",line);
				case "printf":
					return new Token(TokenKind.Printf, "printf",line);
				case "printNewLine":
					return new Token(TokenKind.PrintNewLine, "printNewLine",line);
				case "return":
					return new Token(TokenKind.Return, "return",line);
				default:
					return new Token(TokenKind.Id, letter.toString(),line);
			}
			
		}
		//进行数字处理
		else if(Character.isDigit(c)){
			StringBuffer letter=new StringBuffer();
			letter.append((char)c);
			while(Character.isDigit(source.charAt(position))){
				letter.append((char)source.charAt(position));
				position++;
				tempPosition--;
			}
			return new Token(TokenKind.Num,String.valueOf(letter),line);
		}
		//操作符,分界符处理
		else{
			switch (c) {
				case '+':
					return new Token(TokenKind.Add, "+",line);
				case '-':
					return new Token(TokenKind.Sub, "-",line);
				case '*':
					return new Token(TokenKind.Mul, "*",line);
				case '/':
					return new Token(TokenKind.Div, "/",line);
				case '%':
					return new Token(TokenKind.Mod, "%",line);
				case '{':
					return new Token(TokenKind.Lbrace, "{",line);
				case '}':
					return new Token(TokenKind.Rbrace, "}",line);
				case '(':
					return new Token(TokenKind.Lparen, "(",line);
				case ')':
					return new Token(TokenKind.Rparen, ")",line);
				case ',':
					return new Token(TokenKind.Commer, ",",line);
				case '>':
					c=source.charAt(position++);
					if(c==61){
						return new Token(TokenKind.GTE, ">=",line);
					}else{
						position--;
						tempPosition--;
						return new Token(TokenKind.GT, ">",line);
					}
				case '<':
					c=source.charAt(position++);
					if(c==61){
						return new Token(TokenKind.LTE, "<=",line);
					}else{
						position--;
						tempPosition--;
						return new Token(TokenKind.LT, "<",line);
					}
				case '=':
					c=source.charAt(position++);
					if(c==61){
						return new Token(TokenKind.EQ, "==",line);
					}else{
						position--;
						tempPosition--;
						return new Token(TokenKind.Assign, "=",line);
					}
				case '!':
					c=source.charAt(position++);
					if(c==61){
						return new Token(TokenKind.NEQ, "!=",line);
					}
				case ';':
					return new Token(TokenKind.Semicolon, ";",line);
				case '"':
					StringBuffer letter=new StringBuffer();
					while(source.charAt(position)!='"'){
						letter.append((char)source.charAt(position));
						position++;
						tempPosition--;
					}
					position++;
					tempPosition--;
					return new Token(TokenKind.String,String.valueOf(letter),line);
				default:
					break;
			}//end of switch
		}//end of else
		return new Token(TokenKind.Unknown, "Unknown Token"+(char)c+c,line);
	}
}
