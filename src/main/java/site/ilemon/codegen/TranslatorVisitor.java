package site.ilemon.codegen;


import site.ilemon.ast.Ast.*;
import site.ilemon.ast.Ast.Type.TypeKind;
import site.ilemon.codegen.ast.Ast;
import site.ilemon.codegen.ast.Label;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import site.ilemon.exception.CompilerException;

/**
 * IR 翻译 Visitor — 将前端 AST 翻译为后端 Jasmin IL 指令序列。
 *
 * <p>遍历 {@link site.ilemon.ast.Ast} 中的前端 AST 节点，生成对应的
 * {@link Ast}（后端 IR）指令序列。翻译过程中：</p>
 * <ul>
 *   <li>为每个方法的局部变量分配索引（{@code indexTable}）</li>
 *   <li>将布尔表达式翻译为 if-then-else 的 0/1 赋值形式</li>
 *   <li>生成 JVM 类型相关的 load/store/arithmetic 指令</li>
 *   <li>处理方法调用的 invokestatic 指令生成</li>
 * </ul>
 *
 * <p>翻译结果保存在 {@link #prog} 字段中，供 {@link ByteCodeGenerator} 写入 {@code .il} 文件。</p>
 *
 * @author andy
 * @see ByteCodeGenerator
 * @see Ast
 */
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

    /**
     * 从索引表中安全查找变量索引，未找到时抛出内部编译错误。
     */
    private int lookupIndex(String id) {
        Integer idx = this.indexTable.get(id);
        if (idx == null) {
            throw new CompilerException(
                "[代码生成] 内部错误: 变量 '" + id + "' 未在索引表中注册");
        }
        return idx;
    }


    @Override
    public void visit(Expr.T obj) {
        obj.accept(this);
    }

    @Override
    public void visit(Expr.Add obj) {
        this.visit(obj.getLeft());
        Ast.Type.T t = this.type;
        this.visit(obj.getRight());
        if (t.getKind() == TypeKind.INT) {
            emit(new Ast.Stmt.Iadd());
        } else if (t.getKind() == TypeKind.FLOAT) {
            emit(new Ast.Stmt.Fadd());
        } else if (t.getKind() == TypeKind.DOUBLE) {
            emit(new Ast.Stmt.Dadd());
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
        obj.getLeft().trueList.addToTail(new Label());
        obj.getLeft().falseList = obj.falseList;
        this.visit(obj.getLeft());
        emit(new Ast.Stmt.LabelJ(obj.getLeft().trueList.get(0)));

        // 遍历右子树
        obj.getRight().trueList = obj.trueList;
        obj.getRight().falseList = obj.falseList;
        this.visit(obj.getRight());
    }

    /**
     * E-> id1 relop id2
     * E.code := gen('if' id1.place relop.op id2.place 'goto' E.true) || gen('goto' E.false)
     * @param obj
     */
    @Override
    public void visit(Expr.GT obj) {
        this.visit(obj.getLeft());
        this.visit(obj.getRight());

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
        this.visit(obj.getLeft());
        this.visit(obj.getRight());
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
        obj.getLeft().trueList = obj.trueList;
        obj.getLeft().falseList.addToTail(new Label());
        this.visit(obj.getLeft());
        emit(new Ast.Stmt.LabelJ(obj.getLeft().falseList.get(0)));
        // 遍历右子树
        obj.getRight().trueList = obj.trueList;
        obj.getRight().falseList = obj.falseList;
        this.visit(obj.getRight());

    }

    // E-> not E1
    // E1.true := E.false
    // E1.false := E.true
    // E.code := E1.code
    @Override
    public void visit(Expr.Not obj) {
        obj.getExpr().trueList = obj.falseList;
        obj.getExpr().falseList = obj.trueList;
        this.visit(obj.getExpr());

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
        obj.getCondition().trueList.addToTail(trueLabel);
        obj.getCondition().falseList.addToTail(falseLabel);

        // E.code
        this.visit(obj.getCondition());
        // gen(E.true':')
        emit(new Ast.Stmt.LabelJ(trueLabel));
        // S1.code
        this.visit(obj.getThenStmt());
        // goto S.next
        emit(new Ast.Stmt.Goto(nextLabel));
        // gen(E.false':')
        emit(new Ast.Stmt.LabelJ(falseLabel));
        // S2.code (可能为null)
        if (obj.getElseStmt() != null) {
            this.visit(obj.getElseStmt());
        }
        emit(new Ast.Stmt.Goto(nextLabel));
        emit(new Ast.Stmt.LabelJ(nextLabel));

    }

    @Override
    public void visit(Expr.LTE obj) {
        this.visit(obj.getLeft());
        this.visit(obj.getRight());
        if (this.type instanceof Ast.Type.Float) {
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmple(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        } else {
            emit(new Ast.Stmt.Ificmple(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }
    }

    @Override
    public void visit(Expr.GTE obj) {
        this.visit(obj.getLeft());
        this.visit(obj.getRight());
        if (this.type instanceof Ast.Type.Float) {
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmpge(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        } else {
            emit(new Ast.Stmt.Ificmpge(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }
    }

    @Override
    public void visit(Expr.EQ obj) {
        this.visit(obj.getLeft());
        this.visit(obj.getRight());
        if (this.type instanceof Ast.Type.Float) {
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmpeq(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        } else {
            emit(new Ast.Stmt.Ificmpeq(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }
    }

    @Override
    public void visit(Expr.NEQ obj) {
        this.visit(obj.getLeft());
        this.visit(obj.getRight());
        if (this.type instanceof Ast.Type.Float) {
            emit(new Ast.Stmt.Fcmpl());
            emit(new Ast.Stmt.Istore(++index));
            emit(new Ast.Stmt.Iload(index));
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Ificmpne(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        } else {
            emit(new Ast.Stmt.Ificmpne(obj.trueList.get(0)));
            emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
        }
    }

    @Override
    public void visit(Expr.Call obj) {
        this.visit(obj.getReturnType());
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for (int i = 0; i < obj.getInputParams().size(); i++) {
            Expr.T expr = obj.getInputParams().get(i);
            processExpression(obj, expr);
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokestatic(obj.getName(), at, returnType));

        // 这里似乎不应该保存到局部，再load到操作栈
        //emit(new Ast.Stmt.Istore(++index));
        //emit(new Ast.Stmt.Iload(index));
        // E-> call(EList)
        if( obj.getReturnType() instanceof Type.Bool){
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
        if( checkWhetherBoolExpression(expr) || ( expr instanceof Expr.Call && ((Expr.Call)expr).getReturnType() instanceof Type.Bool) ){
            String iden = generateVarName();
            Expr.Id id = new Expr.Id(iden, obj.getLineNum());
            this.indexTable.put(iden, index++);
            Stmt.Assign thenStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 1, obj.getLineNum()), obj.getLineNum());
            Stmt.Assign elseStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 0, obj.getLineNum()), obj.getLineNum());
            Stmt.If ifStmt = new Stmt.If(expr, thenStmt, elseStmt, obj.getLineNum());
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
        int index = lookupIndex(obj.getId().getId());

        // 如果赋值语句右侧是bool类型的表达式
        // 或者是bool类型的方法调用
        if( checkWhetherBoolExpression(obj.getExpr()) || ( obj.getExpr() instanceof Expr.Call && ((Expr.Call)obj.getExpr()).getReturnType() instanceof Type.Bool) ){
            String iden = generateVarName();
            int tempIndex = this.index++;
            Expr.Id id = new Expr.Id(iden,new Type.Int(), obj.getLineNum());
            this.indexTable.put( iden, tempIndex );
            Stmt.Assign thenStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 1, obj.getLineNum()), obj.getLineNum());
            Stmt.Assign elseStmt = new Stmt.Assign(
                    id,
                    new Expr.Number(new Type.Int(), 0, obj.getLineNum()), obj.getLineNum());
            Stmt.If ifStmt = new Stmt.If(obj.getExpr(), thenStmt, elseStmt, obj.getLineNum());
            this.visit(ifStmt);
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Iload(tempIndex));
        } else if (obj.getId().getType() instanceof Type.Double && obj.getExpr() instanceof Expr.Number) {
            // 当float字面量赋值给double变量时，直接生成double常量
            Expr.Number num = (Expr.Number) obj.getExpr();
            emit(new Ast.Stmt.Ldc(java.lang.Double.parseDouble(num.getValue().toString())));
            this.type = new Ast.Type.Double();
        } else{
            this.visit(obj.getExpr());
        }

        // 生成 xstore index
        if (obj.getId().getType() instanceof Type.Int || obj.getId().getType() instanceof Type.Bool)
            emit(new Ast.Stmt.Istore(index));
        else if (obj.getId().getType() instanceof Type.Float)
            emit(new Ast.Stmt.Fstore(index));
        else if (obj.getId().getType() instanceof Type.Double) {
            emit(new Ast.Stmt.Dstore(index));
        }
    }


    @Override
    public void visit(Expr.Id obj) {
        int index = lookupIndex(obj.getId());
        if (obj.getType() instanceof Type.Int) {
            this.type = new Ast.Type.Int();
            emit(new Ast.Stmt.Iload(index));
        } else if (obj.getType() instanceof Type.Float) {
            this.type = new Ast.Type.Float();
            emit(new Ast.Stmt.Fload(index));
        } else if (obj.getType() instanceof Type.Double) {
            this.type = new Ast.Type.Double();
            emit(new Ast.Stmt.Dload(index));
        } else if (obj.getType() instanceof Type.Str) {
            this.type = new Ast.Type.Str();
            emit(new Ast.Stmt.Aload(index));
        }
        // 如果ID是bool类型
        else if( obj.getType() instanceof Type.Bool ){

            if( obj.trueList.isEmpty() && obj.falseList.isEmpty() ){
                emit(new Ast.Stmt.Iload(lookupIndex(obj.getId())));
            }else{
                // 先load再比较
                emit(new Ast.Stmt.Iload(lookupIndex(obj.getId())));
                emit(new Ast.Stmt.Ifgt(obj.trueList.get(0)));
                emit(new Ast.Stmt.Goto(obj.falseList.get(0)));
            }
        }
    }

    @Override
    public void visit(Expr.Div obj) {
        this.visit(obj.getLeft());
        Ast.Type.T t = this.type;
        this.visit(obj.getRight());
        if (t.getKind() == TypeKind.INT) {
            emit(new Ast.Stmt.Idiv());
        } else if (t.getKind() == TypeKind.FLOAT) {
            emit(new Ast.Stmt.Fdiv());
        } else if (t.getKind() == TypeKind.DOUBLE) {
            emit(new Ast.Stmt.Ddiv());
        } else {
            // error
        }
    }

    @Override
    public void visit(Expr.Mul obj) {
        this.visit(obj.getLeft());
        Ast.Type.T t = this.type;
        this.visit(obj.getRight());
        if (t.getKind() == TypeKind.INT) {
            emit(new Ast.Stmt.Imul());
        } else if (t.getKind() == TypeKind.FLOAT) {
            emit(new Ast.Stmt.Fmul());
        } else if (t.getKind() == TypeKind.DOUBLE) {
            emit(new Ast.Stmt.Dmul());
        } else {
            // error
        }
    }

    @Override
    public void visit(Expr.Number obj) {
        if (obj.getType() instanceof Type.Int) {
            emit(new Ast.Stmt.Ldc(Integer.parseInt(obj.getValue().toString())));
            this.type = new Ast.Type.Int();
        } else if (obj.getType() instanceof Type.Float) {
            emit(new Ast.Stmt.Ldc(Float.parseFloat(obj.getValue().toString())));
            this.type = new Ast.Type.Float();
        } else if (obj.getType() instanceof Type.Double) {
            emit(new Ast.Stmt.Ldc(java.lang.Double.parseDouble(obj.getValue().toString())));
            this.type = new Ast.Type.Double();
        }
        //else
    }


    @Override
    public void visit(Expr.Sub obj) {
        this.visit(obj.getLeft());
        Ast.Type.T t = this.type;
        this.visit(obj.getRight());
        if (t.getKind() == TypeKind.INT) {
            emit(new Ast.Stmt.Isub());
        } else if (t.getKind() == TypeKind.FLOAT) {
            emit(new Ast.Stmt.Fsub());
        } else if (t.getKind() == TypeKind.DOUBLE) {
            emit(new Ast.Stmt.Dsub());
        } else {
            // error
        }
    }


    @Override
    public void visit(Expr.Str obj) {
        this.type = new Ast.Type.Str();
        // 字符串值需要用引号包裹，但要避免重复添加
        String value = obj.getValue();
        // 处理转义字符：将实际的换行符转换回 \n 表示
        value = value.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        emit(new Ast.Stmt.Ldc("\"" + value + "\""));
        emit(new Ast.Stmt.Astore(index));
        index++;
    }

    @Override
    public void visit(Type.T obj) {
        if (obj != null) {
            obj.accept(this);
        }
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
    public void visit(Type.Double obj) {
        this.type = new Ast.Type.Double();
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
        this.visit(((Program.ProgramSingle) programSingle).getMainClass());
        this.prog = new Ast.Program.ProgramSingle(this.mainClass);
    }

    @Override
    public void visit(Declare.T obj) {
        Declare.DeclareSingle declareSingle = ((Declare.DeclareSingle) obj);
        this.visit(declareSingle.getType());
        this.dec = new Ast.Declare.DeclareSingle(this.type, declareSingle.getId());
        if (this.indexTable != null) { // if it is field
            this.indexTable.put(declareSingle.getId(), index);
            
            // 如果是数组类型，生成newarray指令
            if (declareSingle.getType() instanceof Type.IntArray) {
                Type.IntArray arr = (Type.IntArray) declareSingle.getType();
                emit(new Ast.Stmt.Ldc(arr.getSize()));
                emit(new Ast.Stmt.Newarray(new Ast.Type.Int()));
                emit(new Ast.Stmt.Astore(index));
                index++;
            } else if (declareSingle.getType() instanceof Type.FloatArray) {
                Type.FloatArray arr = (Type.FloatArray) declareSingle.getType();
                emit(new Ast.Stmt.Ldc(arr.getSize()));
                emit(new Ast.Stmt.Newarray(new Ast.Type.Float()));
                emit(new Ast.Stmt.Astore(index));
                index++;
            } else if (declareSingle.getType() instanceof Type.DoubleArray) {
                Type.DoubleArray arr = (Type.DoubleArray) declareSingle.getType();
                emit(new Ast.Stmt.Ldc(arr.getSize()));
                emit(new Ast.Stmt.Newarray(new Ast.Type.Double()));
                emit(new Ast.Stmt.Astore(index));
                index++;
            } else if (declareSingle.getType() instanceof Type.BoolArray) {
                Type.BoolArray arr = (Type.BoolArray) declareSingle.getType();
                emit(new Ast.Stmt.Ldc(arr.getSize()));
                emit(new Ast.Stmt.Newarray(new Ast.Type.Bool()));
                emit(new Ast.Stmt.Astore(index));
                index++;
            } else if (this.type instanceof Ast.Type.Double) {
                // double类型占用2个槽位
                index += 2;
            } else {
                index++;
            }
        }
    }


    @Override
    public void visit(MainClass.T obj) {

        MainClass.MainClassSingle mainClassSingle = (MainClass.MainClassSingle) obj;
        this.classId = mainClassSingle.getClassId();
        List<Ast.Method.MethodSingle> methods = new ArrayList<>();
        for (int i = 0; i < mainClassSingle.getMethods().size(); i++) {
            Method.MethodSingle methodSingle = (Method.MethodSingle) mainClassSingle.getMethods().get(i);
            this.visit(methodSingle);
            methods.add(this.method);
        }
        this.mainClass = new Ast.MainClass.MainClassSingle(this.classId, methods);
    }

    @Override
    public void visit(Method.MethodSingle obj) {
        this.index = 0;
        this.indexTable = new HashMap<>();
        this.stmts = new ArrayList<>(); // 先初始化stmts
        this.visit(obj.getRetType());
        Ast.Type.T returnType = this.type;

        // 遍历入参
        List<Ast.Declare.DeclareSingle> formals = new ArrayList<>();
        for (int i = 0; i < obj.getFormals().size(); i++) {
            this.visit(obj.getFormals().get(i));
            formals.add(this.dec);
        }

        // 遍历局部变量（这里会生成数组初始化代码）
        List<Ast.Declare.DeclareSingle> locals = new ArrayList<>();
        for (int i = 0; i < obj.getLocals().size(); i++) {
            this.visit(obj.getLocals().get(i));
            locals.add(this.dec);
        }

        // 遍历stmts
        for (int i = 0; i < obj.getStms().size(); i++) {
            this.visit(obj.getStms().get(i));
        }
        //this.visit(obj.retExp);

        this.method = new Ast.Method.MethodSingle(returnType, obj.getId(), this.classId,
                formals, locals, this.stmts, 0, this.index);
    }

    //private


    @Override
    public void visit(Stmt.T obj) {
        obj.accept(this);
    }


    @Override
    public void visit(Stmt.Block obj) {
        for (int i = 0; i < obj.getStmts().size(); i++) {
            this.visit(obj.getStmts().get(i));
        }

    }

    @Override
    public void visit(Stmt.Printf obj) {
        String f = obj.getFormat();
        String[] array = f.split("%d|%f");
        if (array.length == 0) {
            array = new String[1];
            array[0] = f;
        }
        for (int i = 0; i < array.length; i++) {
            this.visit(new Expr.Str(array[i], obj.getLineNum()));
            emit(new Ast.Stmt.Aload(index - 1));
            emit(new Ast.Stmt.Printf(new Ast.Type.Str(), array[i]));
            if (i + 1 < obj.getExprs().size()) {
                Expr.T expr = obj.getExprs().get(i + 1);
                this.visit(expr);
                if (this.type instanceof Ast.Type.Int)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Int(), null));
                else if (this.type instanceof Ast.Type.Float)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Float(), null));
                else if (this.type instanceof Ast.Type.Double)
                    emit(new Ast.Stmt.Printf(new Ast.Type.Double(), null));
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
                || expr instanceof Expr.LTE || expr instanceof Expr.GTE
                || expr instanceof Expr.EQ || expr instanceof Expr.NEQ
                || expr instanceof Expr.Not || expr instanceof Expr.And
                || expr instanceof Expr.Or || expr instanceof Expr.True
                || expr instanceof Expr.False;
    }

    @Override
    public void visit(Stmt.Return obj) {
        // 如果return的表达式具有bool类型
        // 或者所返回的方法具有bool类型
        if ( checkWhetherBoolExpression(obj.getExpr()) ||  ( obj.getExpr() instanceof Expr.Call && ((Expr.Call)obj.getExpr()).getReturnType() instanceof Type.Bool)) {
            obj.getExpr().trueList.addToTail(new Label());
            obj.getExpr().falseList.addToTail(new Label());
        }
        this.visit(obj.getExpr());

        // 如果return的表达式具有bool类型
        // 或者所返回的方法具有bool类型
        // 需要生成跳转指令
        if ( checkWhetherBoolExpression(obj.getExpr()) ||  ( obj.getExpr() instanceof Expr.Call && ((Expr.Call)obj.getExpr()).getReturnType() instanceof Type.Bool)) {
            Label nextLabel = new Label();
            // gen(E.true':')
            emit(new Ast.Stmt.LabelJ(obj.getExpr().trueList.get(0)));;
            emit(new Ast.Stmt.Ldc(1));
            emit(new Ast.Stmt.Goto(nextLabel));
            // gen(E.false':')
            emit(new Ast.Stmt.LabelJ(obj.getExpr().falseList.get(0)));;
            emit(new Ast.Stmt.Ldc(0));
            emit(new Ast.Stmt.Goto(nextLabel));
            emit(new Ast.Stmt.LabelJ(nextLabel));

            // 设置当前类型
            this.type = new Ast.Type.Int();
        }
        if (this.type.getKind() == TypeKind.INT || this.type.getKind() == TypeKind.BOOL)
            emit(new Ast.Stmt.Ireturn());
        else if (this.type.getKind() == TypeKind.FLOAT)
            emit(new Ast.Stmt.Freturn());
        else if (this.type.getKind() == TypeKind.DOUBLE)
            emit(new Ast.Stmt.Dreturn());

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
        obj.getCondition().trueList.addToHead(trueLabel);

        // E.false := S.next
        Label next = new Label();
        obj.getCondition().falseList.addToHead(next);

        // gen(S.begin':')
        emit(new Ast.Stmt.LabelJ(begin));

        // E.code
        this.visit(obj.getCondition());

        // gen(E.true':')
        emit(new Ast.Stmt.LabelJ(trueLabel));

        // S1.code
        this.visit(obj.getBody());

        // gen('goto' S.begin)
        emit(new Ast.Stmt.Goto(begin));

        // gen(S.next ':')
        emit(new Ast.Stmt.LabelJ(next));
    }

    @Override
    public void visit(Stmt.Call obj) {
        this.visit(obj.getReturnType());
        Ast.Type.T returnType = this.type;
        List<Ast.Type.T> at = new ArrayList<>();
        for (int i = 0; i < obj.getInputParams().size(); i++) {
            Expr.Call targetObj = new Expr.Call(obj.getName(),obj.getInputParams(),obj.getLineNum(),obj.getReturnType());
            processExpression(targetObj,obj.getInputParams().get(i));
            at.add(this.type);
        }
        emit(new Ast.Stmt.Invokestatic(obj.getName(), at, returnType));
    }

    // ========== 数组相关的 visit 方法 ==========

    @Override
    public void visit(Type.IntArray obj) {
        this.type = new Ast.Type.IntArray();
    }

    @Override
    public void visit(Type.FloatArray obj) {
        this.type = new Ast.Type.FloatArray();
    }

    @Override
    public void visit(Type.DoubleArray obj) {
        this.type = new Ast.Type.DoubleArray();
    }

    @Override
    public void visit(Type.BoolArray obj) {
        this.type = new Ast.Type.BoolArray();
    }

    @Override
    public void visit(Expr.ArrayAccess obj) {
        // 加载数组引用
        int arrayIndex = lookupIndex(obj.getArrayName());
        emit(new Ast.Stmt.Aload(arrayIndex));
        // 计算下标
        this.visit(obj.getIndex());
        // 根据元素类型生成对应的加载指令
        if (obj.getElementType() instanceof Type.Int) {
            emit(new Ast.Stmt.Iaload());
            this.type = new Ast.Type.Int();
        } else if (obj.getElementType() instanceof Type.Float) {
            emit(new Ast.Stmt.Faload());
            this.type = new Ast.Type.Float();
        } else if (obj.getElementType() instanceof Type.Double) {
            emit(new Ast.Stmt.Daload());
            this.type = new Ast.Type.Double();
        } else if (obj.getElementType() instanceof Type.Bool) {
            emit(new Ast.Stmt.Baload());
            this.type = new Ast.Type.Int(); // bool在JVM中用int表示
        }
    }

    @Override
    public void visit(Expr.ArrayLength obj) {
        int arrayIndex = lookupIndex(obj.getArrayName());
        emit(new Ast.Stmt.Aload(arrayIndex));
        emit(new Ast.Stmt.Arraylength());
        this.type = new Ast.Type.Int();
    }

    @Override
    public void visit(Stmt.ArrayAssign obj) {
        // 加载数组引用
        int arrayIndex = lookupIndex(obj.getArrayName());
        emit(new Ast.Stmt.Aload(arrayIndex));
        // 计算下标
        this.visit(obj.getIndex());
        // 计算值
        this.visit(obj.getExpr());
        // 根据数组元素类型生成存储指令
        if (obj.getElementType() instanceof Type.Int) {
            emit(new Ast.Stmt.Iastore());
        } else if (obj.getElementType() instanceof Type.Float) {
            emit(new Ast.Stmt.Fastore());
        } else if (obj.getElementType() instanceof Type.Double) {
            // 如果表达式是float，需要转换为double
            if (this.type instanceof Ast.Type.Float) {
                emit(new Ast.Stmt.F2d());
            }
            emit(new Ast.Stmt.Dastore());
        } else if (obj.getElementType() instanceof Type.Bool) {
            emit(new Ast.Stmt.Bastore());
        }
    }
}
