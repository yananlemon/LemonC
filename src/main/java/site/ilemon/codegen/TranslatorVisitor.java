package site.ilemon.codegen;


import site.ilemon.ast.Ast.*;
import site.ilemon.codegen.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TranslatorVisitor implements ISemanticVisitor {

    private String classId;
    // 变量索引
    private int index;

    // 变量表[key:变量名称,value:变量索引]
    private HashMap<String, Integer> indexTable;
    private Ast.Type.T type;
    private Ast.Declare.DeclareSingle dec;
    private Ast.Method.MethodSingle method;
    private Ast.MainClass.MainClassSingle mainClass;
    public Ast.Program.ProgramSingle prog;
    private List<Ast.Stmt.T> stmts;

    public TranslatorVisitor() {
        this.stmts = new ArrayList<Ast.Stmt.T>();
        this.classId = null;
        this.indexTable = null;
        this.type = null;
        this.dec = null;
        this.method = null;
        this.classId = null;
        this.mainClass = null;
        this.prog = null;
    }

    private void emit(Ast.Stmt.T stmt) {
        this.stmts.add(stmt);
    }


    @Override
    public void visit(Expr.T obj) {
        if (obj instanceof Expr.Add)
            this.visit((Expr.Add) obj);
        else if (obj instanceof Expr.Sub)
            this.visit((Expr.Sub) obj);
        else if (obj instanceof Expr.Mul)
            this.visit((Expr.Mul) obj);
        else if (obj instanceof Expr.Div)
            this.visit((Expr.Div) obj);
        else if (obj instanceof Expr.Number)
            this.visit((Expr.Number) obj);
        else if (obj instanceof Expr.True)
            this.visit((Expr.True) obj);
        else if (obj instanceof Expr.False)
            this.visit((Expr.False) obj);
        else if (obj instanceof Expr.Str)
            this.visit((Expr.Str) obj);
        else if (obj instanceof Expr.Id)
            this.visit((Expr.Id) obj);
        else if (obj instanceof Expr.Call)
            this.visit((Expr.Call) obj);
        else if (obj instanceof Expr.LT)
            this.visit((Expr.LT) obj);
        else if (obj instanceof Expr.GT)
            this.visit((Expr.GT) obj);
        else if (obj instanceof Expr.LET)
            this.visit((Expr.LET) obj);
        else if (obj instanceof Expr.GET)
            this.visit((Expr.GET) obj);
        else if (obj instanceof Expr.Not)
            this.visit((Expr.Not) obj);
        else if (obj instanceof Expr.And)
            this.visit((Expr.And) obj);
        else if (obj instanceof Expr.Or)
            this.visit((Expr.Or) obj);

    }

    @Override
    public void visit(Expr.Add obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if (t.toString().equals("@int")) {
            emit(new Ast.Stmt.Iadd());
        } else if (t.toString().equals("@float")) {
            emit(new Ast.Stmt.Fadd());
        } else {
            // error
        }
    }


    @Override
    public void visit(Expr obj) {

    }

    /**
     * E -> E1 and E2
     *  E1.true := newlabel
     *  E1.false := E.false
     *  E2.true := E.true
     *  E2.false := E.false
     *  E.code := E1.code || gen(E1.true ':') ||E2.code
     * @param obj
     */
    @Override
    public void visit(Expr.And obj) {
        // 遍历左子树
        obj.left.trueList.addToTail(new Label());
        obj.left.falseList = obj.falseList;
        this.visit(obj.left);
        emit(new Ast.Stmt.LabelJ(obj.left.trueList.get(0)));

        // 遍历右子树
        obj.right.trueList = obj.trueList;
        obj.right.falseList = obj.falseList;
        this.visit(obj.right);
    }

    /**
     * E-> id1 relop id2
     * E.code := gen('if' id1.place relop.op id2.place 'goto' E.true) || gen('goto' E.false)
     * @param obj
     */
    @Override
    public void visit(Expr.GT obj) {
        this.visit(obj.left);
        this.visit(obj.right);

        if( this.type instanceof Ast.Type.Float){
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmpgt(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }else{
            // int 类型比较
            emit(new Ast.Stmt.Ificmpgt(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }
    }

    @Override
    public void visit(Expr.LT obj) {
        this.visit(obj.left);
        this.visit(obj.right);
        if( this.type instanceof Ast.Type.Float){
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmplt(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }else {
            // int 类型比较
            emit(new Ast.Stmt.Ificmplt(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }

    }

    /**
     * E -> E1 or E2
     * E1.true := E.true
     * E1.false := newlabel
     * E2.true := E.true
     * E2.false := E.false
     * E.code := E1.code || gen(E1.false ':') || E2.code
     *
     * @param obj
     */
    @Override
    public void visit(Expr.Or obj) {

        // 遍历左子树
        obj.left.trueList = obj.trueList;
        obj.left.falseList.addToTail(new Label());
        this.visit(obj.left);
        emit(new Ast.Stmt.LabelJ(obj.left.falseList.get(0)));
        // 遍历右子树
        obj.right.trueList = obj.trueList;
        obj.right.falseList = obj.falseList;
        this.visit(obj.right);

    }

    // E-> not E1
    // E1.true := E.false
    // E1.false := E.true
    // E.code := E1.code
    @Override
    public void visit(Expr.Not obj) {
        obj.expr.trueList = obj.falseList;
        obj.expr.falseList = obj.trueList;
        this.visit(obj.expr);

    }

    @Override
    public void visit(Expr.True obj) {
        emit(new Ast.Stmt.Ldc(1));
        emit(new Ast.Stmt.Ifgt(obj.trueList.get(0)));
        emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
    }

    @Override
    public void visit(Expr.False obj) {
        emit(new Ast.Stmt.Ldc(0));
        emit(new Ast.Stmt.Ifgt(obj.trueList.get(0)));
        emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
    }

    /**
     * S -> if(E) S1 else S2
     * E.true := newlabel
     * E.false := newlabel
     * S1.next := S.next
     * S2.next := S.next
     * S.code := E.code || gen(E.true':') || S1.code || gen('goto' S.next) || gen(E.false':') || S2.code
     *
     * @param obj
     */
    @Override
    public void visit(Stmt.If obj) {
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        Label nextLabel = new Label();
        obj.condition.trueList.addToTail(trueLabel);
        obj.condition.falseList.addToTail(falseLabel);

        // E.code
        this.visit(obj.condition);
        // gen(E.true':')
        emit(new Ast.Stmt.LabelJ(trueLabel));
        // S1.code
        this.visit(obj.thenStmt);
        // goto S.next
        emit(new Ast.Stmt.Goto(nextLabel));
        // gen(E.false':')
        emit(new Ast.Stmt.LabelJ(falseLabel));
        // S2.code
        this.visit(obj.elseStmt);
        emit(new Ast.Stmt.Goto(nextLabel));
        emit(new Ast.Stmt.LabelJ(nextLabel));

    }

    @Override
    public void visit(Expr.LET obj) {

    }

    @Override
    public void visit(Expr.GET obj) {

    }

    @Override
    public void visit(Expr.Call obj) {
        this.visit(obj.returnType);
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for (int i = 0; i < obj.inputParams.size(); i++) {
            Expr.T expr = obj.inputParams.get(i);
            processExpression(obj, expr);
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokevirtual(obj.name, at, returnType));

        // 这里似乎不应该保存到局部，再load到操作栈
        //emit(new Ast.Stmt.Istore(++index));
        //emit(new Ast.Stmt.Iload(index));
        // E-> call(EList)
        if( obj.returnType instanceof Type.Bool){
            emit(new Ast.Stmt.Ifgt(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }

    }

    /**
     * 处理方法调用中的参数是表达式的情况
     * @param obj 方法调用
     * @param expr 参数
     */
    private void processExpression(Expr.Call obj, Expr.T expr) {
        // 如果方法参数是bool表达式
        // 或者方法返回类型是bool
        if( checkWhetherBoolExpression(expr) || ( expr instanceof Expr.Call && ((Expr.Call)expr).returnType instanceof Type.Bool) ){
            String iden = generateVarName();
            Expr.Id id = new Expr.Id(iden, obj.lineNum);
            this.indexTable.put(iden, index++);
            Stmt.Assign thenStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 1, obj.lineNum), obj.lineNum);
            Stmt.Assign elseStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 0, obj.lineNum), obj.lineNum);
            Stmt.If ifStmt = new Stmt.If(expr, thenStmt, elseStmt, obj.lineNum);
            this.visit(ifStmt);
            this.type = new Ast.Type.Int();
        }else{
            this.visit(expr);
        }
    }

    /**
     * 暂时使用UUID作为临时变量名
     * @return
     */
    private String generateVarName(){
        return  UUID.randomUUID().toString();
    }

    @Override
    public void visit(Stmt.Assign obj) {
        int index = this.indexTable.get(obj.id.id);

        // 如果赋值语句右侧是bool类型的表达式
        // 或者是bool类型的方法调用
        if( checkWhetherBoolExpression(obj.expr) || ( obj.expr instanceof Expr.Call && ((Expr.Call)obj.expr).returnType instanceof Type.Bool) ){
            String iden = generateVarName();
            int tempIndex = this.index++;
            Expr.Id id = new Expr.Id(iden,new Type.Int(), obj.lineNum);
            this.indexTable.put( iden, tempIndex );
            Stmt.Assign thenStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 1, obj.lineNum), obj.lineNum);
            Stmt.Assign elseStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 0, obj.lineNum), obj.lineNum);
            Stmt.If ifStmt = new Stmt.If(obj.expr, thenStmt, elseStmt, obj.lineNum);
            this.visit(ifStmt);
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Iload(tempIndex));
        } else{
            this.visit(obj.expr);
        }

        // 生成 xstore index
        if (obj.id.type instanceof Type.Int || obj.id.type instanceof Type.Bool)
            emit(new Ast.Stmt.Istore(index));
        else if (obj.id.type instanceof Type.Float)
            emit(new Ast.Stmt.Fstore(index));
    }


    @Override
    public void visit(Expr.Id obj) {
        int index = this.indexTable.get(obj.id);
        if (obj.type instanceof Type.Int) {
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Iload(index));
        } else if (obj.type instanceof Type.Float) {
            this.type = new Ast.Type.Float();
            emit(new Ast.Stmt.Fload(index));
        } else if (obj.type instanceof Type.Str) {
            this.type = new Ast.Type.Str();
            emit(new Ast.Stmt.Aload(index));
        }
        // 如果ID是bool类型
        else if( obj.type instanceof Type.Bool ){

            if( obj.trueList.isEmpty() && obj.falseList.isEmpty() ){
                emit(new Ast.Stmt.Iload(this.indexTable.get(obj.id)));
            }else{
                // 先load再比较
                emit(new Ast.Stmt.Iload(this.indexTable.get(obj.id)));
                emit(new Ast.Stmt.Ifgt(obj.trueList.get(0)));
                emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
            }
        }
    }

    @Override
    public void visit(Expr.Div obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if (t.toString().equals("@int")) {
            emit(new Ast.Stmt.Idiv());
        } else if (t.toString().equals("@float")) {
            emit(new Ast.Stmt.Fdiv());
        } else {
            // error
        }
    }

    @Override
    public void visit(Expr.Mul obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if (t.toString().equals("@int")) {
            emit(new Ast.Stmt.Imul());
        } else if (t.toString().equals("@float")) {
            emit(new Ast.Stmt.Fmul());
        } else {
            // error
        }
    }

    @Override
    public void visit(Expr.Number obj) {
        if (obj.type instanceof Type.Int) {
            emit(new Ast.Stmt.Ldc(Integer.parseInt(obj.value.toString())));
            this.type = new Ast.Type.Int();
        } else if (obj.type instanceof Type.Float) {
            emit(new Ast.Stmt.Ldc(Float.parseFloat(obj.value.toString())));
            this.type = new Ast.Type.Float();
        }
        //else
    }


    @Override
    public void visit(Expr.Sub obj) {
        this.visit(obj.left);
        Ast.Type.T t = this.type;
        this.visit(obj.right);
        if (t.toString().equals("@int")) {
            emit(new Ast.Stmt.Isub());
        } else if (t.toString().equals("@float")) {
            emit(new Ast.Stmt.Fsub());
        } else {
            // error
        }
    }


    @Override
    public void visit(Expr.Str obj) {
        this.type = new Ast.Type.Str();
        emit(new Ast.Stmt.Ldc("\"" + obj.value + "\""));
        emit(new Ast.Stmt.Astore(index++));
    }

    @Override
    public void visit(Type.T obj) {
        if (obj instanceof Type.Int)
            this.visit((Type.Int) obj);
        else if (obj instanceof Type.Float)
            this.visit((Type.Float) obj);
        else if (obj instanceof Type.Bool)
            this.visit((Type.Bool) obj);
        else if (obj instanceof Type.Str)
            this.visit((Type.Str) obj);
        else if (obj instanceof Type.Void)
            this.visit((Type.Void) obj);
    }

    @Override
    public void visit(Type.Bool obj) {
        this.type = new Ast.Type.Bool();
    }

    @Override
    public void visit(Type.Float obj) {
        this.type = new Ast.Type.Float();
    }

    @Override
    public void visit(Type.Str obj) {
        this.type = new Ast.Type.Str();
    }

    @Override
    public void visit(Type obj) {

    }

    @Override
    public void visit(Type.Void obj) {
        this.type = new Ast.Type.Void();
    }

    @Override
    public void visit(Type.Int obj) {
        this.type = new Ast.Type.Int();
    }

    @Override
    public void visit(Program.T programSingle) {
        this.visit(((Program.ProgramSingle) programSingle).mainClass);
        this.prog = new Ast.Program.ProgramSingle(this.mainClass);
    }

    @Override
    public void visit(Declare.T obj) {
        Declare.DeclareSingle declareSingle = ((Declare.DeclareSingle) obj);
        this.visit(declareSingle.type);
        this.dec = new Ast.Declare.DeclareSingle(this.type, declareSingle.id);
        if (this.indexTable != null) // if it is field
            this.indexTable.put(declareSingle.id, index++);
    }


    @Override
    public void visit(MainClass.T obj) {

        MainClass.MainClassSingle mainClassSingle = (MainClass.MainClassSingle) obj;
        this.classId = mainClassSingle.classId;
        List<Ast.Method.MethodSingle> methods = new ArrayList<>();
        for (int i = 0; i < mainClassSingle.methods.size(); i++) {
            Method.MethodSingle methodSingle = (Method.MethodSingle) mainClassSingle.methods.get(i);
            this.visit(methodSingle);
            methods.add(this.method);
        }
        this.mainClass = new Ast.MainClass.MainClassSingle(this.classId, methods);
    }

    @Override
    public void visit(Method.MethodSingle obj) {
        this.index = 0;
        this.indexTable = new HashMap<>();
        this.visit(obj.retType);
        Ast.Type.T returnType = this.type;

        // 遍历入参
        List<Ast.Declare.DeclareSingle> formals = new ArrayList<>();
        for (int i = 0; i < obj.formals.size(); i++) {
            this.visit(obj.formals.get(i));
            formals.add(this.dec);
        }

        // 遍历局部变量
        List<Ast.Declare.DeclareSingle> locals = new ArrayList<>();
        for (int i = 0; i < obj.locals.size(); i++) {
            this.visit(obj.locals.get(i));
            locals.add(this.dec);
        }

        // 遍历stmts
        this.stmts = new ArrayList<>();
        for (int i = 0; i < obj.stms.size(); i++) {
            this.visit(obj.stms.get(i));
        }
        //this.visit(obj.retExp);

        this.method = new Ast.Method.MethodSingle(returnType, obj.id, this.classId,
                formals, locals, this.stmts, 0, this.index);
    }

    //private


    @Override
    public void visit(Stmt.T obj) {
        if (obj instanceof Stmt.Assign)
            this.visit((Stmt.Assign) obj);
        else if (obj instanceof Stmt.Block)
            this.visit((Stmt.Block) obj);
        else if (obj instanceof Stmt.If)
            this.visit((Stmt.If) obj);
        else if (obj instanceof Stmt.Printf)
            this.visit((Stmt.Printf) obj);
        else if (obj instanceof Stmt.PrintLine)
            this.visit((Stmt.PrintLine) obj);
        else if (obj instanceof Stmt.While)
            this.visit((Stmt.While) obj);
        else if (obj instanceof Stmt.Return)
            this.visit((Stmt.Return) obj);
        else if (obj instanceof Stmt.Call)
            this.visit((Stmt.Call) obj);
        // else error
    }


    @Override
    public void visit(Stmt.Block obj) {
        for (int i = 0; i < obj.stmts.size(); i++) {
            this.visit(obj.stmts.get(i));
        }

    }

    @Override
    public void visit(Stmt.Printf obj) {
        String f = obj.format;
        String[] array = f.split("%d|%f");
        if (array.length == 0) {
            array = new String[1];
            array[0] = f;
        }
        for (int i = 0; i < array.length; i++) {
            this.visit(new Expr.Str(array[i], obj.lineNum));
            emit(new Ast.Stmt.Aload(index - 1));
            emit(new Ast.Stmt.Printf(new Ast.Type.Str(), array[i]));
            if (i + 1 < obj.exprs.size()) {
                Expr.T expr = obj.exprs.get(i + 1);
                this.visit(expr);
                if (this.type instanceof Ast.Type.Int)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Int(), null));
                else if (this.type instanceof Ast.Type.Float)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Float(), null));
            }

        }
    }

    @Override
    public void visit(Stmt.PrintLine obj) {
        emit(new Ast.Stmt.Ldc("\"\\n\""));
        emit(new Ast.Stmt.Astore(index));
        emit(new Ast.Stmt.Aload(index));
        emit(new Ast.Stmt.PrintLine());
        index++;
    }

    /**
     *
     * @param expr
     * @return 返回true,如果
     */
    private boolean checkWhetherBoolExpression(Expr.T expr){
        return expr instanceof Expr.GT || expr instanceof Expr.LT
                || expr instanceof Expr.Not || expr instanceof Expr.And
                || expr instanceof Expr.Or || expr instanceof Expr.True
                || expr instanceof Expr.False;
    }

    @Override
    public void visit(Stmt.Return obj) {
        // 如果return的表达式具有bool类型
        // 或者所返回的方法具有bool类型
        if ( checkWhetherBoolExpression(obj.expr) ||  ( obj.expr instanceof Expr.Call && ((Expr.Call)obj.expr).returnType instanceof Type.Bool)) {
            obj.expr.trueList.addToTail(new Label());
            obj.expr.falseList.addToTail(new Label());
        }
        this.visit(obj.expr);

        // 如果return的表达式具有bool类型
        // 或者所返回的方法具有bool类型
        // 需要生成跳转指令
        if ( checkWhetherBoolExpression(obj.expr) ||  ( obj.expr instanceof Expr.Call && ((Expr.Call)obj.expr).returnType instanceof Type.Bool)) {
            Label nextLabel = new Label();
            // gen(E.true':')
            emit(new Ast.Stmt.LabelJ(obj.expr.trueList.get(0)));;
            emit(new Ast.Stmt.Ldc(1));
            emit(new Ast.Stmt.Goto(nextLabel));
            // gen(E.false':')
            emit(new Ast.Stmt.LabelJ(obj.expr.falseList.get(0)));;
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Goto(nextLabel));
            emit(new Ast.Stmt.LabelJ(nextLabel));

            // 设置当前类型
            this.type = new Ast.Type.Int();
        }
        if (this.type.toString().equals("@int") || this.type.toString().equals("@bool"))
            emit(new Ast.Stmt.Ireturn());
        else if (this.type.toString().equals("@float"))
            emit(new Ast.Stmt.Freturn());

    }

    /**
     *  S -> while(E) do S1
     *      S.begin := newlabel
     *      E.true := newlabel
     *      E.false := S.next
     *      S1.next := S.begin
     *      S.code := gen(S.begin':') || E.code ||gen(E.true':')|| S1.code || gen('goto' S.begin)
     * @param obj
     */
    @Override
    public void visit(Stmt.While obj) {

        //S.begin := newlabel
        Label begin = new Label();

        // E.true := newlabel
        Label trueLabel = new Label();
        obj.condition.trueList.addToHead(trueLabel);

        // E.false := S.next
        Label next = new Label();
        obj.condition.falseList.addToHead(next);

        // gen(S.begin':')
        emit(new Ast.Stmt.LabelJ(begin));

        // E.code
        this.visit(obj.condition);

        // gen(E.true':')
        emit(new Ast.Stmt.LabelJ(trueLabel));

        // S1.code
        this.visit(obj.body);

        // gen('goto' S.begin)
        emit(new Ast.Stmt.Goto(begin));

        // gen(S.next ':')
        emit(new Ast.Stmt.LabelJ(next));
    }

    @Override
    public void visit(Stmt.Call obj) {
        this.visit(obj.returnType);
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for (int i = 0; i < obj.inputParams.size(); i++) {
            Expr.Call targetObj = new Expr.Call(obj.name,obj.inputParams,obj.lineNum,obj.returnType);
            processExpression(targetObj,obj.inputParams.get(i));
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokevirtual(obj.name, at, returnType));
    }
}
