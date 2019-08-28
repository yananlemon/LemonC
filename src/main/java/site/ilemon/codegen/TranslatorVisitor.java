package site.ilemon.codegen;


import site.ilemon.ast.Ast.*;
import site.ilemon.codegen.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.list.DoublyLinkedList;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TranslatorVisitor implements ISemanticVisitor {

    private String classId;
    private int index;
    private HashMap<String, Integer> indexTable;
    private Ast.Type.T type;
    private Ast.Declare.DeclareSingle dec;
    private Ast.Method.MethodSingle method;
    private Ast.MainClass.MainClassSingle mainClass;
    public Ast.Program.ProgramSingle prog;
    private List<Ast.Stmt.T> stmts;

    private Label tempLabel;

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

    private DoublyLinkedList<Label> union(DoublyLinkedList<Label> list1,DoublyLinkedList<Label> list2){
        DoublyLinkedList<Label> result = new DoublyLinkedList<>();
        for(int i = 0; i < list1.size(); i++){
            result.addToTail(list1.get(i));
        }
        for(int i = 0; i < list2.size(); i++){
            result.addToTail(list2.get(i));
        }
        return  result;
    }


    @Override
    public void visit(Expr.GT obj) {
        this.visit(obj.left);
        this.visit(obj.right);
        Label trueLabel = null;
        Label falseLabel = null;
        if (obj.trueList.isEmpty()) {
            if( obj.left.trueList.isEmpty() && obj.right.trueList.isEmpty())
                trueLabel = new Label();
            else{
                obj.trueList = union(obj.left.trueList,obj.right.trueList);
                trueLabel = obj.trueList.get(0);
                obj.trueList.clear();
            }

        } else {
            trueLabel = obj.trueList.get(0);
            obj.trueList.clear();
        }
        if (obj.falseList.isEmpty()) {

            if( obj.left.falseList.isEmpty() && obj.right.falseList.isEmpty())
                falseLabel = new Label();
            else{
                obj.falseList = union(obj.left.falseList,obj.right.falseList);
                falseLabel = obj.falseList.get(0);
                obj.falseList.clear();
            }

        } else {
            falseLabel = obj.falseList.get(0);
            obj.falseList.clear();
        }
        obj.trueList.addToTail(trueLabel);
        obj.falseList.addToTail(falseLabel);
        if( this.type instanceof Ast.Type.Float){
            emit(new Ast.Stmt.Fcmpl());
            //emit(new Ast.Stmt.Istore(varIndexOfMethod));
            //emit(new Ast.Stmt.Iload(varIndexOfMethod++));
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmpgt(trueLabel));
            emit(new Ast.Stmt.Goto(falseLabel));
        }

        else{
            emit(new Ast.Stmt.Ificmpgt(trueLabel));
            emit(new Ast.Stmt.Goto(falseLabel));
        }

    }

    private int varIndexOfMethod = 0;


    @Override
    public void visit(Expr.LT obj) {
        this.visit(obj.left);
        this.visit(obj.right);
        Label trueLabel = null;
        Label falseLabel = null;
        /*if (obj.trueList.isEmpty()) {
            trueLabel = new Label();
        } else {
            trueLabel = obj.trueList.get(0);
            obj.trueList.clear();
        }
        if (obj.falseList.isEmpty()) {
            falseLabel = new Label();
        } else {
            falseLabel = obj.falseList.get(0);
            obj.falseList.clear();
        }*/
        if (obj.trueList.isEmpty()) {
            if( obj.left.trueList.isEmpty() && obj.right.trueList.isEmpty())
                trueLabel = new Label();
            else{
                obj.trueList = union(obj.left.trueList,obj.right.trueList);
                trueLabel = obj.trueList.get(0);
                obj.trueList.clear();
            }

        } else {
            trueLabel = obj.trueList.get(0);
            obj.trueList.clear();
        }
        if (obj.falseList.isEmpty()) {

            if( obj.left.falseList.isEmpty() && obj.right.falseList.isEmpty())
                falseLabel = new Label();
            else{
                obj.falseList = union(obj.left.falseList,obj.right.falseList);
                falseLabel = obj.falseList.get(0);
                obj.falseList.clear();
            }

        } else {
            falseLabel = obj.falseList.get(0);
            obj.falseList.clear();
        }
        obj.trueList.addToTail(trueLabel);
        obj.falseList.addToTail(falseLabel);

        if( this.type instanceof Ast.Type.Float){
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmplt(trueLabel));
            emit(new Ast.Stmt.Goto(falseLabel));
        }
        else{
            emit(new Ast.Stmt.Ificmplt(trueLabel));
            emit(new Ast.Stmt.Goto(falseLabel));
        }

        //emit(new Ast.Stmt.Ificmplt(trueLabel));
        //emit(new Ast.Stmt.Goto(falseLabel));
    }

    @Override
    public void visit(Expr.And obj) {
        this.visit(obj.left);
        // 遍历左子表达式的假链，将其添加到父节点的假链中
        for (int i = 0; i < obj.left.falseList.size(); i++) {
            obj.falseList.addToTail(obj.left.falseList.get(i));
        }

        // E.code := E1.code || gen(E1.true ':') ||E2.code
        for (int i = 0; i < obj.left.trueList.size(); i++) {
            Label trueLabel = obj.left.trueList.get(i);
            emit(new Ast.Stmt.LabelJ(trueLabel));
            this.visit(obj.right);
        }


        // 遍历右子表达式的真链，将其添加到父节点的真链中
        for (int i = 0; i < obj.right.trueList.size(); i++) {
            obj.trueList.addToTail(obj.right.trueList.get(i));
        }
        // 遍历右子表达式的假链，将其添加到父节点的假链中
        for (int i = 0; i < obj.right.falseList.size(); i++) {
            obj.falseList.addToTail(obj.right.falseList.get(i));
        }

    }

    @Override
    public void visit(Expr.Or obj) {

        // 遍历左子树
        this.visit(obj.left);

        // 遍历左子表达式的真链，将其添加到父节点的真链中
        for (int i = 0; i < obj.left.trueList.size(); i++) {
            obj.trueList.addToTail(obj.left.trueList.get(i));
        }

        // E.code := E1.code || gen(E1.false ':') || E2.code
        for (int i = 0; i < obj.left.falseList.size(); i++) {
            Label falseLabel = obj.left.falseList.get(i);
            emit(new Ast.Stmt.LabelJ(falseLabel));
            this.visit(obj.right);

        }

        // 遍历右子表达式的真链，将其添加到父节点的真链中
        for (int i = 0; i < obj.right.trueList.size(); i++) {
            obj.trueList.addToTail(obj.right.trueList.get(i));
        }

        // 遍历右子表达式的假链，将其添加到父节点的假链中
        for (int i = 0; i < obj.right.falseList.size(); i++) {
            obj.falseList.addToTail(obj.right.falseList.get(i));
        }
    }

    // E-> not E1
    // E1.true := E.false
    // E1.false := E.true
    // E.code := E1.code
    @Override
    public void visit(Expr.Not obj) {
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        obj.expr.trueList.addToTail(falseLabel);
        obj.expr.falseList.addToTail(trueLabel);
        this.visit(obj.expr);
        obj.trueList = obj.expr.falseList;
        obj.falseList = obj.expr.trueList;
    }

    @Override
    public void visit(Expr.True obj) {
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        obj.trueList.addToTail(trueLabel);
        obj.falseList.addToTail(falseLabel);
        emit(new Ast.Stmt.Ldc(1));
        emit(new Ast.Stmt.Ldc(0));
        emit(new Ast.Stmt.Ificmpgt(trueLabel));
        emit(new Ast.Stmt.Goto(falseLabel));
    }

    @Override
    public void visit(Expr.False obj) {
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        obj.trueList.addToTail(trueLabel);
        obj.falseList.addToTail(falseLabel);
        emit(new Ast.Stmt.Ldc(0));
        emit(new Ast.Stmt.Ldc(0));
        emit(new Ast.Stmt.Ificmpgt(trueLabel));
        emit(new Ast.Stmt.Goto(falseLabel));
    }

    private boolean conditionFlag = false;

    @Override
    public void visit(Stmt.If obj) {
        conditionFlag = true;
        // 遍历条件表达式
        this.visit(obj.condition);
        Label nextStmtLabel = new Label();
        for (int i = 0; i < obj.condition.trueList.size(); i++) {
            emit(new Ast.Stmt.LabelJ(obj.condition.trueList.get(i)));
            this.visit(obj.thenStmt);
            emit(new Ast.Stmt.Goto(nextStmtLabel));
        }
        for (int i = 0; i < obj.condition.falseList.size(); i++) {
            emit(new Ast.Stmt.LabelJ(obj.condition.falseList.get(i)));
            this.visit(obj.elseStmt);
            emit(new Ast.Stmt.Goto(nextStmtLabel));
        }
        emit(new Ast.Stmt.LabelJ(nextStmtLabel));
        conditionFlag = false;
    }

    @Override
    public void visit(Expr.LET obj) {

    }

    @Override
    public void visit(Expr.GET obj) {

    }

    private boolean conditionWhileFlag = false;

    @Override
    public void visit(Expr.Call obj) {
        this.visit(obj.returnType);
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for (int i = 0; i < obj.inputParams.size(); i++) {
            Expr.T expr = obj.inputParams.get(i);
            if (expr instanceof Expr.GT || expr instanceof Expr.LT
                    || expr instanceof Expr.Not || expr instanceof Expr.And
                    || expr instanceof Expr.Or || expr instanceof Expr.True
                    || expr instanceof Expr.False) {
                String iden = UUID.randomUUID().toString();
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
                //return;
                this.type = new Ast.Type.Int();
            } else {
                this.visit(expr);
            }
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokevirtual(obj.name, at, returnType));
        if (conditionFlag || conditionWhileFlag) {
            if( this.type instanceof Ast.Type.Int || obj.returnType instanceof Type.Bool){
                emit(new Ast.Stmt.Istore(++index));
                emit(new Ast.Stmt.Iload(index));
            }
            else if( this.type instanceof Ast.Type.Float){
                emit(new Ast.Stmt.Fstore(++index));
                emit(new Ast.Stmt.Fload(index));
            }

            // 只有当方法是bool返回类型时，才需要生成跳转指令
            if (obj.returnType instanceof Type.Bool) {
                Label trueLabel = new Label();
                Label falseLabel = new Label();
                obj.trueList.addToTail(trueLabel);
                obj.falseList.addToTail(falseLabel);
                emit(new Ast.Stmt.Ldc(0));
                emit(new Ast.Stmt.Ificmpgt(trueLabel));
                emit(new Ast.Stmt.Goto(falseLabel));
            }

        }
    }

    @Override
    public void visit(Stmt.Assign obj) {
        int index = this.indexTable.get(obj.id.id);
        if (obj.expr instanceof Expr.GT || obj.expr instanceof Expr.LT
                || obj.expr instanceof Expr.Not || obj.expr instanceof Expr.And
                || obj.expr instanceof Expr.Or || obj.expr instanceof Expr.True
                || obj.expr instanceof Expr.False) {
            expr2IfStmt(obj);
            return;
        }
        this.visit(obj.expr);
        if (obj.id.type instanceof Type.Int || obj.id.type instanceof Type.Bool)
            emit(new Ast.Stmt.Istore(index));
        else if (obj.id.type instanceof Type.Float)
            emit(new Ast.Stmt.Fstore(index));
    }

    private void expr2IfStmt(Stmt.Assign obj) {
        Stmt.Assign thenStmt = new Stmt.Assign(
                obj.id,
                new Expr.Number(new Type.Int(), 1, obj.lineNum), obj.lineNum);
        Stmt.Assign elseStmt = new Stmt.Assign(
                obj.id,
                new Expr.Number(new Type.Int(), 0, obj.lineNum), obj.lineNum);
        Stmt.If ifStmt = new Stmt.If(obj.expr, thenStmt, elseStmt, obj.lineNum);
        this.visit(ifStmt);
    }


    @Override
    public void visit(Expr.Id obj) {
        int index = this.indexTable.get(obj.id);
        if (obj.type instanceof Type.Int || obj.type instanceof Type.Bool) {
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Iload(index));
            if (conditionFlag && obj.type instanceof Type.Bool) {
                emit(new Ast.Stmt.Ldc(0));
                Label trueLabel = null;
                Label falseLabel = null;
                if (obj.trueList.isEmpty()) {
                    trueLabel = new Label();
                    obj.trueList.addToTail(trueLabel);
                } else {
                    trueLabel = obj.trueList.get(0);
                }
                if (obj.falseList.isEmpty()) {
                    falseLabel = new Label();
                    obj.falseList.addToTail(falseLabel);
                } else {
                    falseLabel = obj.falseList.get(0);
                }
                emit(new Ast.Stmt.Ificmpgt(trueLabel));
                emit(new Ast.Stmt.Goto(falseLabel));
            }
        } else if (obj.type instanceof Type.Float) {
            this.type = new Ast.Type.Float();
            emit(new Ast.Stmt.Fload(index));
        } else if (obj.type instanceof Type.Str) {
            this.type = new Ast.Type.Str();
            emit(new Ast.Stmt.Aload(index));
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
        if (obj.type instanceof Type.Int){
            emit(new Ast.Stmt.Ldc(Integer.parseInt(obj.value.toString())));
            this.type = new Ast.Type.Int();
        }

        else if (obj.type instanceof Type.Float){
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
        //strIndex = this.indexTable.size();
    }


    @Override
    public void visit(MainClass.T obj) {

        MainClass.MainClassSingle mainClassSingle = (MainClass.MainClassSingle) obj;
        this.classId = mainClassSingle.classId;
        List<Ast.Method.MethodSingle> methods = new ArrayList<>();
        for (int i = 0; i < mainClassSingle.methods.size(); i++) {
            // 将方法变量索引设置为0
            varIndexOfMethod = 0;
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
                this.visit(obj.exprs.get(i + 1));
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

    @Override
    public void visit(Stmt.Return obj) {
        // 返回的表达式可能是bool表达式
        if (obj.expr instanceof Expr.GT || obj.expr instanceof Expr.LT
                || obj.expr instanceof Expr.Not || obj.expr instanceof Expr.And
                || obj.expr instanceof Expr.Or || obj.expr instanceof Expr.True
                || obj.expr instanceof Expr.False) {
            String iden = UUID.randomUUID().toString();
            Expr.Id id = new Expr.Id(iden, obj.lineNum);
            this.indexTable.put(iden, index++);
            Stmt.Assign thenStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 1, obj.lineNum), obj.lineNum);
            Stmt.Assign elseStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 0, obj.lineNum), obj.lineNum);
            Stmt.If ifStmt = new Stmt.If(obj.expr, thenStmt, elseStmt, obj.lineNum);
            this.visit(ifStmt);
            this.type = new Ast.Type.Int();
        }
        // 返回的表达式可能是方法调用
        else if (obj.expr instanceof Expr.Call) {
            this.visit((Expr.Call) obj.expr);
            if (this.type.toString().equals("@bool"))
                emit(new Ast.Stmt.Ireturn());
        } else {
            if (obj.expr instanceof Expr.Add)
                this.visit((Expr.Add) obj.expr);
            else if (obj.expr instanceof Expr.Sub)
                this.visit((Expr.Sub) obj.expr);
            else if (obj.expr instanceof Expr.Mul)
                this.visit((Expr.Mul) obj.expr);
            else if (obj.expr instanceof Expr.Div)
                this.visit((Expr.Div) obj.expr);
            else if (obj.expr instanceof Expr.Number)
                this.visit((Expr.Number) obj.expr);
            else if (obj.expr instanceof Expr.Id)
                this.visit((Expr.Id) obj.expr);
            else if (obj.expr instanceof Expr.Not) {
                this.visit((Expr.Not) obj.expr);
            }
        }

        if (this.type.toString().equals("@int"))
            emit(new Ast.Stmt.Ireturn());
        else if (this.type.toString().equals("@float"))
            emit(new Ast.Stmt.Freturn());


    }

    //S.begin := newlabel
    //E.true  := newlabel
    //E.false := S.next
    //S1.next := S.begin
    //S.code  := gen(S.begin':') || E.code || gen(E.true':')|| S1.code || gen('goto' S.begin)
    @Override
    public void visit(Stmt.While obj) {
        conditionWhileFlag = true;
        Label whileBegin = new Label();
        emit(new Ast.Stmt.LabelJ(whileBegin));
        this.visit(obj.condition);

        for (int i = 0; i < obj.condition.trueList.size(); i++) {
            Label trueLabel = obj.condition.trueList.get(i);
            emit(new Ast.Stmt.LabelJ(trueLabel));
            this.visit(obj.body);
            emit(new Ast.Stmt.Goto(whileBegin));
        }

        for (int i = 0; i < obj.condition.falseList.size(); i++) {
            Label falseLabel = obj.condition.falseList.get(i);
            emit(new Ast.Stmt.LabelJ(falseLabel));
        }
        conditionWhileFlag = false;
    }

    @Override
    public void visit(Stmt.Call obj) {
        this.visit(obj.returnType);
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for (int i = 0; i < obj.inputParams.size(); i++) {
            this.visit(obj.inputParams.get(i));
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokevirtual(obj.name, at, returnType));
    }
}
