import os

files = {
"lexer/Token.java": """package lexer;
public class Token {
    public final int tag;
    public Token(int t) { tag = t; }
    public String toString() { return "" + (char)tag; }
}
""",
"lexer/Tag.java": """package lexer;
public class Tag {
    public final static int
        AND   = 256,  BASIC = 257,  BREAK = 258,  DO   = 259, ELSE  = 260,
        EQ    = 261,  FALSE = 262,  GE    = 263,  ID   = 264, IF    = 265,
        INDEX = 266,  LE    = 267,  MINUS = 268,  NE   = 269, NUM   = 270,
        OR    = 271,  REAL  = 272,  TEMP  = 273,  TRUE = 274, WHILE = 275;
}
""",
"lexer/Word.java": """package lexer;
public class Word extends Token {
    public String lexeme = "";
    public Word(String s, int tag) { super(tag); lexeme = s; }
    public String toString() { return lexeme; }
    public static final Word
        and = new Word("&&", Tag.AND),  or = new Word("||", Tag.OR),
        eq  = new Word("==", Tag.EQ),   ne = new Word("!=", Tag.NE),
        le  = new Word("<=", Tag.LE),   ge = new Word(">=", Tag.GE),
        minus  = new Word("minus", Tag.MINUS),
        True   = new Word("true",  Tag.TRUE),
        False  = new Word("false", Tag.FALSE),
        temp   = new Word("t",     Tag.TEMP);
}
""",
"lexer/Num.java": """package lexer;
public class Num extends Token {
    public final int value;
    public Num(int v) { super(Tag.NUM); value = v; }
    public String toString() { return "" + value; }
}
""",
"lexer/Real.java": """package lexer;
public class Real extends Token {
    public final float value;
    public Real(float v) { super(Tag.REAL); value = v; }
    public String toString() { return "" + value; }
}
""",
"lexer/Lexer.java": """package lexer;
import java.io.*;
import java.util.*;
import symbols.*;
public class Lexer {
    public static int line = 1;
    char peek = ' ';
    Hashtable<String, Word> words = new Hashtable<String, Word>();
    void reserve(Word w) { words.put(w.lexeme, w); }
    public Lexer() {
        reserve( new Word("if",    Tag.IF)    );
        reserve( new Word("else",  Tag.ELSE)  );
        reserve( new Word("while", Tag.WHILE) );
        reserve( new Word("do",    Tag.DO)    );
        reserve( new Word("break", Tag.BREAK) );
        reserve( Word.True );  reserve( Word.False );
        reserve( Type.Int  );  reserve( Type.Char  );
        reserve( Type.Bool );  reserve( Type.Float );
    }
    void readch() throws IOException { peek = (char)System.in.read(); }
    boolean readch(char c) throws IOException {
        readch();
        if( peek != c ) return false;
        peek = ' ';
        return true;
    }
    public Token scan() throws IOException {
        for( ; ; readch() ) {
            if( peek == ' ' || peek == '\\t' ) continue;
            else if( peek == '\\n' ) line = line + 1;
            else break;
        }
        switch( peek ) {
        case '&':
            if( readch('&') ) return Word.and;  else return new Token('&');
        case '|':
            if( readch('|') ) return Word.or;   else return new Token('|');
        case '=':
            if( readch('=') ) return Word.eq;   else return new Token('=');
        case '!':
            if( readch('=') ) return Word.ne;   else return new Token('!');
        case '<':
            if( readch('=') ) return Word.le;   else return new Token('<');
        case '>':
            if( readch('=') ) return Word.ge;   else return new Token('>');
        }
        if( Character.isDigit(peek) ) {
            int v = 0;
            do {
                v = 10*v + Character.digit(peek, 10); readch();
            } while( Character.isDigit(peek) );
            if( peek != '.' ) return new Num(v);
            float x = v; float d = 10;
            for(;;) {
                readch();
                if( ! Character.isDigit(peek) ) break;
                x = x + Character.digit(peek, 10) / d; d = d * 10;
            }
            return new Real(x);
        }
        if( Character.isLetter(peek) ) {
            StringBuffer b = new StringBuffer();
            do {
                b.append(peek); readch();
            } while( Character.isLetterOrDigit(peek) );
            String s = b.toString();
            Word w = (Word)words.get(s);
            if( w != null ) return w;
            w = new Word(s, Tag.ID);
            words.put(s, w);
            return w;
        }
        Token tok = new Token(peek); peek = ' ';
        return tok;
    }
}
""",

"symbols/Env.java": """package symbols;
import java.util.*;
import lexer.*;
import inter.*;
public class Env {
    private Hashtable<Token, Id> table;
    protected Env prev;
    public Env(Env n) { table = new Hashtable<Token, Id>(); prev = n; }
    public void put(Token w, Id i) { table.put(w, i); }
    public Id get(Token w) {
        for( Env e = this; e != null; e = e.prev ) {
            Id found = (Id)(e.table.get(w));
            if( found != null ) return found;
        }
        return null;
    }
}
""",
"symbols/Type.java": """package symbols;
import lexer.*;
public class Type extends Word {
    public int width = 0;
    public Type(String s, int tag, int w) { super(s, tag); width = w; }
    public static final Type
        Int   = new Type("int",   Tag.BASIC, 4),
        Float = new Type("float", Tag.BASIC, 8),
        Char  = new Type("char",  Tag.BASIC, 1),
        Bool  = new Type("bool",  Tag.BASIC, 1);
    public static boolean numeric(Type p) {
        if (p == Type.Char || p == Type.Int || p == Type.Float) return true;
        else return false;
    }
    public static Type max(Type p1, Type p2 ) {
        if ( ! numeric(p1) || ! numeric(p2) ) return null;
        else if ( p1 == Type.Float || p2 == Type.Float ) return Type.Float;
        else if ( p1 == Type.Int   || p2 == Type.Int   ) return Type.Int;
        else return Type.Char;
    }
}
""",
"symbols/Array.java": """package symbols;
import lexer.*;
public class Array extends Type {
    public Type of;
    public int size = 1;
    public Array(int sz, Type p) {
        super("[]", Tag.INDEX, sz*p.width); size = sz;  of = p;
    }
    public String toString() { return "[" + size + "] " + of.toString(); }
}
""",

"inter/Node.java": """package inter;
import lexer.*;
public class Node {
    int lexline = 0;
    Node() { lexline = Lexer.line; }
    void error(String s) { throw new Error("near line "+lexline+": "+s); }
    static int labels = 0;
    public int newlabel() { return ++labels; }
    public void emitlabel(int i) { System.out.print("L" + i + ":"); }
    public void emit(String s) { System.out.println("\\t" + s); }
}
""",
"inter/Expr.java": """package inter;
import lexer.*;
import symbols.*;
public class Expr extends Node {
    public Token op;
    public Type type;
    Expr(Token tok, Type p) { op = tok; type = p; }
    public Expr gen() { return this; }
    public Expr reduce() { return this; }
    public void jumping(int t, int f) { emitjumps(toString(), t, f); }
    public void emitjumps(String test, int t, int f) {
        if( t != 0 && f != 0 ) {
            emit("if " + test + " goto L" + t);
            emit("goto L" + f);
        }
        else if( t != 0 ) emit("if " + test + " goto L" + t);
        else if( f != 0 ) emit("iffalse " + test + " goto L" + f);
        else ;
    }
    public String toString() { return op.toString(); }
}
""",
"inter/Id.java": """package inter;
import lexer.*;
import symbols.*;
public class Id extends Expr {
    public int offset;
    public Id(Word id, Type p, int b) { super(id, p); offset = b; }
}
""",
"inter/Op.java": """package inter;
import lexer.*;
import symbols.*;
public class Op extends Expr {
    public Op(Token tok, Type p) { super(tok, p); }
    public Expr reduce() {
        Expr x = gen();
        Temp t = new Temp(type);
        emit( t.toString() + " = " + x.toString() );
        return t;
    }
}
""",
"inter/Temp.java": """package inter;
import lexer.*;
import symbols.*;
public class Temp extends Expr {
    static int count = 0;
    int number = 0;
    public Temp(Type p) { super(Word.temp, p); number = ++count; }
    public String toString() { return "t" + number; }
}
""",
"inter/Arith.java": """package inter;
import lexer.*;
import symbols.*;
public class Arith extends Op {
    public Expr expr1, expr2;
    public Arith(Token tok, Expr x1, Expr x2) {
        super(tok, null); expr1 = x1; expr2 = x2;
        type = Type.max(expr1.type, expr2.type);
        if (type == null ) error("type error");
    }
    public Expr gen() {
        return new Arith(op, expr1.reduce(), expr2.reduce());
    }
    public String toString() {
        return expr1.toString()+" "+op.toString()+" "+expr2.toString();
    }
}
""",
"inter/Unary.java": """package inter;
import lexer.*;
import symbols.*;
public class Unary extends Op {
    public Expr expr;
    public Unary(Token tok, Expr x) {
        super(tok, null);  expr = x;
        type = Type.max(Type.Int, expr.type);
        if (type == null ) error("type error");
    }
    public Expr gen() { return new Unary(op, expr.reduce()); }
    public String toString() { return op.toString()+" "+expr.toString(); }
}
""",
"inter/Constant.java": """package inter;
import lexer.*;
import symbols.*;
public class Constant extends Expr {
    public Constant(Token tok, Type p) { super(tok, p); }
    public Constant(int i) { super(new Num(i), Type.Int); }
    public static final Constant
        True  = new Constant(Word.True,  Type.Bool),
        False = new Constant(Word.False, Type.Bool);
    public void jumping(int t, int f) {
        if ( this == True && t != 0 ) emit("goto L" + t);
        else if ( this == False && f != 0) emit("goto L" + f);
    }
}
""",
"inter/Logical.java": """package inter;
import lexer.*;
import symbols.*;
public class Logical extends Expr {
    public Expr expr1, expr2;
    Logical(Token tok, Expr x1, Expr x2) {
        super(tok, null);
        expr1 = x1; expr2 = x2;
        type = check(expr1.type, expr2.type);
        if (type == null ) error("type error");
    }
    public Type check(Type p1, Type p2) {
        if ( p1 == Type.Bool && p2 == Type.Bool ) return Type.Bool;
        else return null;
    }
    public Expr gen() {
        int f = newlabel(); int a = newlabel();
        Temp temp = new Temp(type);
        this.jumping(0,f);
        emit(temp.toString() + " = true");
        emit("goto L" + a);
        emitlabel(f); emit(temp.toString() + " = false");
        emitlabel(a);
        return temp;
    }
    public String toString() {
        return expr1.toString()+" "+op.toString()+" "+expr2.toString();
    }
}
""",
"inter/Or.java": """package inter;
import lexer.*;
import symbols.*;
public class Or extends Logical {
    public Or(Token tok, Expr x1, Expr x2) { super(tok, x1, x2); }
    public void jumping(int t, int f) {
        int label = t != 0 ? t : newlabel();
        expr1.jumping(label, 0);
        expr2.jumping(t,f);
        if( t == 0 ) emitlabel(label);
    }
}
""",
"inter/And.java": """package inter;
import lexer.*;
import symbols.*;
public class And extends Logical {
    public And(Token tok, Expr x1, Expr x2) { super(tok, x1, x2); }
    public void jumping(int t, int f) {
        int label = f != 0 ? f : newlabel();
        expr1.jumping(0, label);
        expr2.jumping(t,f);
        if( f == 0 ) emitlabel(label);
    }
}
""",
"inter/Not.java": """package inter;
import lexer.*;
import symbols.*;
public class Not extends Logical {
    public Not(Token tok, Expr x2) { super(tok, x2, x2); }
    public void jumping(int t, int f) { expr2.jumping(f, t); }
    public String toString() { return op.toString()+" "+expr2.toString(); }
}
""",
"inter/Rel.java": """package inter;
import lexer.*;
import symbols.*;
public class Rel extends Logical {
    public Rel(Token tok, Expr x1, Expr x2) { super(tok, x1, x2); }
    public Type check(Type p1, Type p2) {
        if ( p1 instanceof Array || p2 instanceof Array ) return null;
        else if( p1 == p2 ) return Type.Bool;
        else return null;
    }
    public void jumping(int t, int f) {
        Expr a = expr1.reduce();
        Expr b = expr2.reduce();
        String test = a.toString() + " " + op.toString() + " " + b.toString();
        emitjumps(test, t, f);
    }
}
""",
"inter/Access.java": """package inter;
import lexer.*;
import symbols.*;
public class Access extends Op {
    public Id array;
    public Expr index;
    public Access(Id a, Expr i, Type p) {
        super(new Word("[]", Tag.INDEX), p);
        array = a; index = i;
    }
    public Expr gen() { return new Access(array, index.reduce(), type); }
    public void jumping(int t,int f) { emitjumps(reduce().toString(),t,f); }
    public String toString() {
        return array.toString() + " [ " + index.toString() + " ]";
    }
}
""",

"inter/Stmt.java": """package inter;
public class Stmt extends Node {
    public Stmt() { }
    public static Stmt Null = new Stmt();
    public void gen(int b, int a) {}
    int after = 0;
    public static Stmt Enclosing = Stmt.Null;
}
""",
"inter/If.java": """package inter;
import symbols.*;
public class If extends Stmt {
    Expr expr; Stmt stmt;
    public If(Expr x, Stmt s) {
        expr = x;  stmt = s;
        if( expr.type != Type.Bool ) expr.error("boolean required in if");
    }
    public void gen(int b, int a) {
        int label = newlabel();
        expr.jumping(0, a);
        emitlabel(label); stmt.gen(label, a);
    }
}
""",
"inter/Else.java": """package inter;
import symbols.*;
public class Else extends Stmt {
    Expr expr; Stmt stmt1, stmt2;
    public Else(Expr x, Stmt s1, Stmt s2) {
        expr = x; stmt1 = s1; stmt2 = s2;
        if( expr.type != Type.Bool ) expr.error("boolean required in if");
    }
    public void gen(int b, int a) {
        int label1 = newlabel();
        int label2 = newlabel();
        expr.jumping(0,label2);
        emitlabel(label1); stmt1.gen(label1, a); emit("goto L" + a);
        emitlabel(label2); stmt2.gen(label2, a);
    }
}
""",
"inter/While.java": """package inter;
import symbols.*;
public class While extends Stmt {
    Expr expr; Stmt stmt;
    public While() { expr = null; stmt = null; }
    public void init(Expr x, Stmt s) {
        expr = x;  stmt = s;
        if( expr.type != Type.Bool ) expr.error("boolean required in while");
    }
    public void gen(int b, int a) {
        after = a;
        expr.jumping(0, a);
        int label = newlabel();
        emitlabel(label); stmt.gen(label, b);
        emit("goto L" + b);
    }
}
""",
"inter/Do.java": """package inter;
import symbols.*;
public class Do extends Stmt {
    Expr expr; Stmt stmt;
    public Do() { expr = null; stmt = null; }
    public void init(Stmt s, Expr x) {
        expr = x; stmt = s;
        if( expr.type != Type.Bool ) expr.error("boolean required in do");
    }
    public void gen(int b, int a) {
        after = a;
        int label = newlabel();
        stmt.gen(b,label);
        emitlabel(label);
        expr.jumping(b,0);
    }
}
""",
"inter/Set.java": """package inter;
import symbols.*;
public class Set extends Stmt {
    public Id id; public Expr expr;
    public Set(Id i, Expr x) {
        id = i; expr = x;
        if ( check(id.type, expr.type) == null ) error("type error");
    }
    public Type check(Type p1, Type p2) {
        if ( Type.numeric(p1) && Type.numeric(p2) ) return p2;
        else if ( p1 == Type.Bool && p2 == Type.Bool ) return p2;
        else return null;
    }
    public void gen(int b, int a) {
        emit( id.toString() + " = " + expr.gen().toString() );
    }
}
""",
"inter/SetElem.java": """package inter;
import symbols.*;
public class SetElem extends Stmt {
    public Id array; public Expr index; public Expr expr;
    public SetElem(Access x, Expr y) {
        array = x.array; index = x.index; expr = y;
        if ( check(x.type, expr.type) == null ) error("type error");
    }
    public Type check(Type p1, Type p2) {
        if ( p1 instanceof Array || p2 instanceof Array ) return null;
        else if ( p1 == p2 ) return p2;
        else if ( Type.numeric(p1) && Type.numeric(p2) ) return p2;
        else return null;
    }
    public void gen(int b, int a) {
        String s1 = index.reduce().toString();
        String s2 = expr.reduce().toString();
        emit( array.toString() + " [ " + s1 + " ] = " + s2 );
    }
}
""",
"inter/Seq.java": """package inter;
public class Seq extends Stmt {
    Stmt stmt1; Stmt stmt2;
    public Seq(Stmt s1, Stmt s2) { stmt1 = s1; stmt2 = s2; }
    public void gen(int b, int a) {
        if ( stmt1 == Stmt.Null ) stmt2.gen(b, a);
        else if ( stmt2 == Stmt.Null ) stmt1.gen(b, a);
        else {
            int label = newlabel();
            stmt1.gen(b,label);
            emitlabel(label);
            stmt2.gen(label,a);
        }
    }
}
""",
"inter/Break.java": """package inter;
public class Break extends Stmt {
    Stmt stmt;
    public Break() {
        if( Stmt.Enclosing == Stmt.Null ) error("unenclosed break");
        stmt = Stmt.Enclosing;
    }
    public void gen(int b, int a) {
        emit( "goto L" + stmt.after );
    }
}
""",

"parser/Parser.java": """package parser;
import java.io.*;
import lexer.*;
import symbols.*;
import inter.*;
public class Parser {
    private Lexer lex;
    private Token look;
    Env top = null;
    int used = 0;
    public Parser(Lexer l) throws IOException { lex = l; move(); }
    void move() throws IOException { look = lex.scan(); }
    void error(String s) { throw new Error("near line "+lex.line+": "+s); }
    void match(int t) throws IOException {
        if( look.tag == t ) move();
        else error("syntax error");
    }
    public void program() throws IOException {
        Stmt s = block();
        int begin = s.newlabel();  int after = s.newlabel();
        s.emitlabel(begin);  s.gen(begin, after);  s.emitlabel(after);
    }
    Stmt block() throws IOException {
        match('{');  Env savedEnv = top;  top = new Env(top);
        decls(); Stmt s = stmts();
        match('}');  top = savedEnv;
        return s;
    }
    void decls() throws IOException {
        while( look.tag == Tag.BASIC ) {
            Type p = type(); Token tok = look; match(Tag.ID); match(';');
            Id id = new Id((Word)tok, p, used);
            top.put( tok, id );
            used = used + p.width;
        }
    }
    Type type() throws IOException {
        Type p = (Type)look;
        match(Tag.BASIC);
        if( look.tag != '[' ) return p;
        else return dims(p);
    }
    Type dims(Type p) throws IOException {
        match('[');  Token tok = look;  match(Tag.NUM);  match(']');
        if( look.tag == '[' )
            p = dims(p);
        return new Array(((Num)tok).value, p);
    }
    Stmt stmts() throws IOException {
        if ( look.tag == '}' ) return Stmt.Null;
        else return new Seq(stmt(), stmts());
    }
    Stmt stmt() throws IOException {
        Expr x;  Stmt s, s1, s2;
        Stmt savedStmt;
        switch( look.tag ) {
        case ';':
            move();
            return Stmt.Null;
        case Tag.IF:
            match(Tag.IF); match('('); x = bool(); match(')');
            s1 = stmt();
            if( look.tag != Tag.ELSE ) return new If(x, s1);
            match(Tag.ELSE);
            s2 = stmt();
            return new Else(x, s1, s2);
        case Tag.WHILE:
            While whilenode = new While();
            savedStmt = Stmt.Enclosing; Stmt.Enclosing = whilenode;
            match(Tag.WHILE); match('('); x = bool(); match(')');
            s1 = stmt();
            whilenode.init(x, s1);
            Stmt.Enclosing = savedStmt;
            return whilenode;
        case Tag.DO:
            Do donode = new Do();
            savedStmt = Stmt.Enclosing; Stmt.Enclosing = donode;
            match(Tag.DO);
            s1 = stmt();
            match(Tag.WHILE); match('('); x = bool(); match(')'); match(';');
            donode.init(s1, x);
            Stmt.Enclosing = savedStmt;
            return donode;
        case Tag.BREAK:
            match(Tag.BREAK); match(';');
            return new Break();
        case '{':
            return block();
        default:
            return assign();
        }
    }
    Stmt assign() throws IOException {
        Stmt stmt;  Token t = look;
        match(Tag.ID);
        Id id = top.get(t);
        if( id == null ) error(t.toString() + " undeclared");
        if( look.tag == '=' ) {
            move();  stmt = new Set(id, bool());
        }
        else {
            Access x = offset(id);
            match('=');  stmt = new SetElem(x, bool());
        }
        match(';');
        return stmt;
    }
    Expr bool() throws IOException {
        Expr x = join();
        while( look.tag == Tag.OR ) {
            Token tok = look;  move();  x = new Or(tok, x, join());
        }
        return x;
    }
    Expr join() throws IOException {
        Expr x = equality();
        while( look.tag == Tag.AND ) {
            Token tok = look;  move();  x = new And(tok, x, equality());
        }
        return x;
    }
    Expr equality() throws IOException {
        Expr x = rel();
        while( look.tag == Tag.EQ || look.tag == Tag.NE ) {
            Token tok = look;  move();  x = new Rel(tok, x, rel());
        }
        return x;
    }
    Expr rel() throws IOException {
        Expr x = expr();
        switch( look.tag ) {
        case '<': case Tag.LE: case Tag.GE: case '>':
            Token tok = look;  move();  return new Rel(tok, x, expr());
        default:
            return x;
        }
    }
    Expr expr() throws IOException {
        Expr x = term();
        while( look.tag == '+' || look.tag == '-' ) {
            Token tok = look;  move();  x = new Arith(tok, x, term());
        }
        return x;
    }
    Expr term() throws IOException {
        Expr x = unary();
        while( look.tag == '*' || look.tag == '/' ) {
            Token tok = look;  move();  x = new Arith(tok, x, unary());
        }
        return x;
    }
    Expr unary() throws IOException {
        if( look.tag == '-' ) {
            move();  return new Unary(Word.minus, unary());
        }
        else if( look.tag == '!' ) {
            Token tok = look;  move();  return new Not(tok, unary());
        }
        else return factor();
    }
    Expr factor() throws IOException {
        Expr x = null;
        switch( look.tag ) {
        case '(':
            move(); x = bool(); match(')');
            return x;
        case Tag.NUM:
            x = new Constant(look, Type.Int);    move(); return x;
        case Tag.REAL:
            x = new Constant(look, Type.Float);  move(); return x;
        case Tag.TRUE:
            x = Constant.True;                   move(); return x;
        case Tag.FALSE:
            x = Constant.False;                  move(); return x;
        default:
            Token t = look;
            match(Tag.ID);
            Id id = top.get(t);
            if( id == null ) error(t.toString() + " undeclared");
            if( look.tag != '[' ) return id;
            else return offset(id);
        }
    }
    Access offset(Id a) throws IOException {
        Expr i; Expr w; Expr t1, t2; Expr loc;
        Type type = a.type;
        match('['); i = bool(); match(']');
        type = ((Array)type).of;
        w = new Constant(type.width);
        t1 = new Arith(new Token('*'), i, w);
        loc = t1;
        while( look.tag == '[' ) {
            match('['); i = bool(); match(']');
            type = ((Array)type).of;
            w = new Constant(type.width);
            t1 = new Arith(new Token('*'), i, w);
            t2 = new Arith(new Token('+'), loc, t1);
            loc = t2;
        }
        return new Access(a, loc, type);
    }
}
""",

"main/Main.java": """package main;
import java.io.*;
import lexer.*;
import parser.*;
public class Main {
    public static void main(String[] args) throws IOException {
        Lexer lex = new Lexer();
        Parser parse = new Parser(lex);
        parse.program();
        System.out.print('\\n');
    }
}
"""
}

base_dir = "e:/personal-code-new-os/LemonC/dragon-book-front"
os.makedirs(base_dir, exist_ok=True)

for path, content in files.items():
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

print(f"Successfully generated all {len(files)} files into {base_dir}")
