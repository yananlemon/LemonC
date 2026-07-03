package site.ilemon.ir;

import site.ilemon.ast.Ast;
import site.ilemon.exception.CompilerException;
import site.ilemon.lexer.IntegerLiterals;
import site.ilemon.visitor.ISemanticVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Translates the source AST to typed three-address LemonIR.
 */
public class AstToIrTranslator implements ISemanticVisitor {

    private final IrProgram program = new IrProgram();
    private final Map<String, IrType> functionReturnTypes = new HashMap<String, IrType>();
    private final Map<String, List<IrType>> functionParamTypes = new HashMap<String, List<IrType>>();

    private IrFunction currentFunction;
    private IrBlock currentBlock;
    private int nextVRegId = 0;
    private int nextLabelId = 0;
    private IrValue lastValue;

    private final Map<String, IrValue> varMap = new HashMap<String, IrValue>();
    private final Map<String, IrType> varTypes = new HashMap<String, IrType>();
    private final Stack<String> breakStack = new Stack<String>();
    private final Stack<String> continueStack = new Stack<String>();

    public IrProgram getProgram() {
        return program;
    }

    private IrValue newVReg(IrType type) {
        IrValue vreg = IrValue.vreg(nextVRegId++, type);
        if (currentFunction != null) {
            currentFunction.updateMaxVReg(vreg);
        }
        return vreg;
    }

    private IrValue getVar(String name) {
        IrValue value = varMap.get(name);
        if (value == null) {
            IrType type = varTypes.get(name);
            if (type == null) {
                type = IrType.INT;
            }
            value = newVReg(type);
            varMap.put(name, value);
        }
        return value;
    }

    private String newLabel(String prefix) {
        return "_" + prefix + "_" + (nextLabelId++);
    }

    private void emit(IrInstruction instr) {
        if (currentBlock != null) {
            currentBlock.addInstruction(instr);
        }
    }

    private IrBlock startBlock(String label) {
        IrBlock block = new IrBlock(label);
        currentFunction.addBlock(block);
        currentBlock = block;
        return block;
    }

    private IrType toIrType(Ast.Type.T type) {
        if (type instanceof Ast.Type.Int) return IrType.INT;
        if (type instanceof Ast.Type.Float) return IrType.FLOAT;
        if (type instanceof Ast.Type.Double) return IrType.DOUBLE;
        if (type instanceof Ast.Type.Bool) return IrType.BOOL;
        if (type instanceof Ast.Type.Str) return IrType.STRING;
        if (type instanceof Ast.Type.Void) return IrType.VOID;
        if (type instanceof Ast.Type.IntArray) return IrType.INT_ARRAY;
        if (type instanceof Ast.Type.FloatArray) return IrType.FLOAT_ARRAY;
        if (type instanceof Ast.Type.DoubleArray) return IrType.DOUBLE_ARRAY;
        if (type instanceof Ast.Type.BoolArray) return IrType.BOOL_ARRAY;
        throw new CompilerException("Unsupported Lemon type in IR translator: " + type);
    }

    private int arraySize(Ast.Type.T type) {
        if (type instanceof Ast.Type.IntArray) return ((Ast.Type.IntArray) type).getSize();
        if (type instanceof Ast.Type.FloatArray) return ((Ast.Type.FloatArray) type).getSize();
        if (type instanceof Ast.Type.DoubleArray) return ((Ast.Type.DoubleArray) type).getSize();
        if (type instanceof Ast.Type.BoolArray) return ((Ast.Type.BoolArray) type).getSize();
        return -1;
    }

    private IrType promotedNumericType(IrType left, IrType right) {
        if (left == IrType.DOUBLE || right == IrType.DOUBLE) return IrType.DOUBLE;
        if (left == IrType.FLOAT || right == IrType.FLOAT) return IrType.FLOAT;
        return IrType.INT;
    }

    private IrValue castIfNeeded(IrValue value, IrType targetType) {
        if (value == null || targetType == null || value.getType() == targetType) {
            return value;
        }
        IrType sourceType = value.getType();
        IrOpcode opcode = null;
        if (sourceType == IrType.INT && targetType == IrType.FLOAT) {
            opcode = IrOpcode.I2F;
        } else if (sourceType == IrType.INT && targetType == IrType.DOUBLE) {
            opcode = IrOpcode.I2D;
        } else if (sourceType == IrType.FLOAT && targetType == IrType.DOUBLE) {
            opcode = IrOpcode.F2D;
        } else if (sourceType == IrType.BOOL && targetType == IrType.INT) {
            return value;
        }
        if (opcode == null) {
            return value;
        }
        IrValue result = newVReg(targetType);
        IrInstruction cast = new IrInstruction(opcode);
        cast.setType(targetType);
        cast.setResult(result);
        cast.addOperand(value);
        emit(cast);
        return result;
    }

    private IrValue numericLiteralForTarget(Ast.Expr.T expr, IrType targetType) {
        if (!(expr instanceof Ast.Expr.Number) || targetType == null) {
            return null;
        }
        Ast.Expr.Number number = (Ast.Expr.Number) expr;
        String text = number.getValue().toString();
        if (number.getType() instanceof Ast.Type.Int) {
            int value = IntegerLiterals.parse(text);
            if (targetType == IrType.DOUBLE) {
                return IrValue.constDouble((double) value);
            }
            if (targetType == IrType.FLOAT) {
                return IrValue.constFloat((float) value);
            }
            if (targetType == IrType.INT) {
                return IrValue.constInt(value);
            }
        }
        if (targetType == IrType.DOUBLE) {
            return IrValue.constDouble(Double.parseDouble(text));
        }
        if (targetType == IrType.FLOAT) {
            return IrValue.constFloat(Float.parseFloat(text));
        }
        return null;
    }

    private IrInstruction instruction(IrOpcode opcode, IrType type, IrValue result, IrValue... operands) {
        IrInstruction instr = new IrInstruction(opcode);
        instr.setType(type);
        instr.setResult(result);
        for (IrValue operand : operands) {
            instr.addOperand(operand);
        }
        return instr;
    }

    private void visitBinary(Ast.Expr.T left, Ast.Expr.T right, IrOpcode opcode) {
        left.accept(this);
        IrValue l = this.lastValue;
        right.accept(this);
        IrValue r = this.lastValue;

        IrType resultType;
        if (opcode == IrOpcode.MOD) {
            resultType = IrType.INT;
        } else {
            resultType = promotedNumericType(l.getType(), r.getType());
        }
        l = castIfNeeded(l, resultType);
        r = castIfNeeded(r, resultType);

        IrValue result = newVReg(resultType);
        emit(instruction(opcode, resultType, result, l, r));
        this.lastValue = result;
    }

    private void visitCompare(Ast.Expr.T left, Ast.Expr.T right, IrOpcode opcode) {
        left.accept(this);
        IrValue l = this.lastValue;
        right.accept(this);
        IrValue r = this.lastValue;

        if (l.getType().isNumeric() && r.getType().isNumeric()) {
            IrType promoted = promotedNumericType(l.getType(), r.getType());
            l = castIfNeeded(l, promoted);
            r = castIfNeeded(r, promoted);
        }

        IrValue result = newVReg(IrType.BOOL);
        emit(instruction(opcode, IrType.BOOL, result, l, r));
        this.lastValue = result;
    }

    private IrValue emitCall(String name, List<Ast.Expr.T> args, boolean needsResult) {
        IrType returnType = functionReturnTypes.get(name);
        if (returnType == null) {
            returnType = IrType.INT;
        }
        List<IrType> expectedTypes = functionParamTypes.get(name);
        IrInstruction call = new IrInstruction(IrOpcode.CALL);
        call.setType(returnType);
        call.setFuncTarget(name);
        if (needsResult && returnType != IrType.VOID) {
            call.setResult(newVReg(returnType));
        }
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                args.get(i).accept(this);
                IrValue arg = this.lastValue;
                if (expectedTypes != null && i < expectedTypes.size()) {
                    arg = castIfNeeded(arg, expectedTypes.get(i));
                }
                call.addOperand(arg);
            }
        }
        emit(call);
        return call.getResult();
    }

    private void emitPrintString(String text) {
        if (text.length() == 0) {
            return;
        }
        IrInstruction print = new IrInstruction(IrOpcode.PRINT);
        print.setType(IrType.STRING);
        print.addOperand(IrValue.constString(text));
        emit(print);
    }

    @Override
    public void visit(Ast.Program.T obj) {
        ((Ast.Program.ProgramSingle) obj).getMainClass().accept(this);
    }

    @Override
    public void visit(Ast.MainClass.T obj) {
        Ast.MainClass.MainClassSingle mainClass = (Ast.MainClass.MainClassSingle) obj;
        program.setClassName(mainClass.getClassId());
        for (Ast.Method.T method : mainClass.getMethods()) {
            Ast.Method.MethodSingle methodSingle = (Ast.Method.MethodSingle) method;
            functionReturnTypes.put(methodSingle.getId(), toIrType(methodSingle.getRetType()));
            List<IrType> params = new ArrayList<IrType>();
            for (Ast.Declare.T formal : methodSingle.getFormals()) {
                Ast.Declare.DeclareSingle decl = (Ast.Declare.DeclareSingle) formal;
                params.add(toIrType(decl.getType()));
            }
            functionParamTypes.put(methodSingle.getId(), params);
        }
        for (Ast.Method.T method : mainClass.getMethods()) {
            method.accept(this);
        }
    }

    @Override
    public void visit(Ast.Method.MethodSingle obj) {
        IrFunction func = new IrFunction(obj.getId());
        func.setReturnType(toIrType(obj.getRetType()));
        program.addFunction(func);
        this.currentFunction = func;
        this.varMap.clear();
        this.varTypes.clear();
        this.nextVRegId = 0;

        for (Ast.Declare.T formal : obj.getFormals()) {
            Ast.Declare.DeclareSingle decl = (Ast.Declare.DeclareSingle) formal;
            IrType type = toIrType(decl.getType());
            IrValue param = newVReg(type);
            varMap.put(decl.getId(), param);
            varTypes.put(decl.getId(), type);
            func.addParameter(param);
        }

        for (Ast.Declare.T local : obj.getLocals()) {
            Ast.Declare.DeclareSingle decl = (Ast.Declare.DeclareSingle) local;
            IrType type = toIrType(decl.getType());
            IrValue localReg = newVReg(type);
            varMap.put(decl.getId(), localReg);
            varTypes.put(decl.getId(), type);
        }

        startBlock(obj.getId() + "_entry");

        for (Ast.Stmt.T stmt : obj.getStms()) {
            stmt.accept(this);
        }

        List<IrInstruction> instrs = currentBlock.getInstructions();
        if (instrs.isEmpty() || instrs.get(instrs.size() - 1).getOpcode() != IrOpcode.RET) {
            emit(new IrInstruction(IrOpcode.RET));
        }

        this.currentFunction = null;
        this.currentBlock = null;
    }

    @Override
    public void visit(Ast.Stmt.Assign obj) {
        IrValue targetReg = getVar(obj.getId().getId());
        IrValue exprValue = numericLiteralForTarget(obj.getExpr(), targetReg.getType());
        if (exprValue == null) {
            obj.getExpr().accept(this);
            exprValue = castIfNeeded(this.lastValue, targetReg.getType());
        }
        emit(instruction(IrOpcode.MOV, targetReg.getType(), targetReg, exprValue));
    }

    @Override
    public void visit(Ast.Stmt.VarDecl obj) {
        Ast.Declare.DeclareSingle declaration = obj.getDeclaration();
        IrValue target = varMap.get(declaration.getId());
        int size = arraySize(declaration.getType());
        if (size > 0) {
            IrInstruction newArr = new IrInstruction(IrOpcode.NEW_ARR);
            newArr.setType(toIrType(declaration.getType()));
            newArr.setResult(target);
            newArr.addOperand(IrValue.constInt(size));
            emit(newArr);
        }
        if (obj.getInitializer() != null) {
            visit(new Ast.Stmt.Assign(
                    new Ast.Expr.Id(declaration.getId(), declaration.getType(), obj.getLineNum()),
                    obj.getInitializer(), obj.getLineNum()));
        }
    }

    @Override
    public void visit(Ast.Stmt.Block obj) {
        for (Ast.Stmt.T stmt : obj.getStmts()) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(Ast.Stmt.Printf obj) {
        String format = obj.getFormat();
        StringBuilder literal = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < format.length(); i++) {
            char ch = format.charAt(i);
            if (ch != '%') {
                literal.append(ch);
                continue;
            }
            emitPrintString(literal.toString());
            literal.setLength(0);
            if (i + 1 >= format.length()) {
                throw new CompilerException("printf format has dangling %");
            }
            char placeholder = format.charAt(++i);
            if (placeholder != 'd' && placeholder != 'f') {
                throw new CompilerException("unsupported printf placeholder %" + placeholder);
            }
            Ast.Expr.T expr = obj.getExprs().get(argIndex++);
            expr.accept(this);
            IrInstruction print = new IrInstruction(IrOpcode.PRINT);
            print.setType(this.lastValue.getType());
            print.addOperand(this.lastValue);
            emit(print);
        }
        emitPrintString(literal.toString());
    }

    @Override
    public void visit(Ast.Stmt.PrintLine obj) {
        emit(new IrInstruction(IrOpcode.PRINT_NL));
    }

    @Override
    public void visit(Ast.Stmt.Return obj) {
        IrInstruction ret = new IrInstruction(IrOpcode.RET);
        if (obj.getExpr() != null) {
            obj.getExpr().accept(this);
            IrValue retValue = castIfNeeded(this.lastValue, currentFunction.getReturnType());
            ret.addOperand(retValue);
        }
        emit(ret);
    }

    @Override
    public void visit(Ast.Stmt.If obj) {
        obj.getCondition().accept(this);
        IrValue cond = this.lastValue;

        String trueLabel = newLabel("if_true");
        String falseLabel = newLabel("if_false");
        String endLabel = newLabel("if_end");

        IrInstruction brTrue = new IrInstruction(IrOpcode.BR_TRUE);
        brTrue.addOperand(cond);
        brTrue.setLabelTarget(trueLabel);
        emit(brTrue);

        IrInstruction jmpFalse = new IrInstruction(IrOpcode.JMP);
        jmpFalse.setLabelTarget(obj.getElseStmt() != null ? falseLabel : endLabel);
        emit(jmpFalse);

        startBlock(trueLabel);
        obj.getThenStmt().accept(this);
        IrInstruction thenEnd = new IrInstruction(IrOpcode.JMP);
        thenEnd.setLabelTarget(endLabel);
        emit(thenEnd);

        if (obj.getElseStmt() != null) {
            startBlock(falseLabel);
            obj.getElseStmt().accept(this);
            IrInstruction elseEnd = new IrInstruction(IrOpcode.JMP);
            elseEnd.setLabelTarget(endLabel);
            emit(elseEnd);
        }

        startBlock(endLabel);
    }

    @Override
    public void visit(Ast.Stmt.While obj) {
        String condLabel = newLabel("while_cond");
        String bodyLabel = newLabel("while_body");
        String endLabel = newLabel("while_end");

        IrInstruction jmpCond = new IrInstruction(IrOpcode.JMP);
        jmpCond.setLabelTarget(condLabel);
        emit(jmpCond);

        startBlock(condLabel);
        obj.getCondition().accept(this);
        IrInstruction brTrue = new IrInstruction(IrOpcode.BR_TRUE);
        brTrue.addOperand(this.lastValue);
        brTrue.setLabelTarget(bodyLabel);
        emit(brTrue);

        IrInstruction jmpEnd = new IrInstruction(IrOpcode.JMP);
        jmpEnd.setLabelTarget(endLabel);
        emit(jmpEnd);

        startBlock(bodyLabel);
        breakStack.push(endLabel);
        continueStack.push(condLabel);
        obj.getBody().accept(this);
        breakStack.pop();
        continueStack.pop();

        IrInstruction back = new IrInstruction(IrOpcode.JMP);
        back.setLabelTarget(condLabel);
        emit(back);

        startBlock(endLabel);
    }

    @Override
    public void visit(Ast.Stmt.For obj) {
        if (obj.getInit() != null) {
            obj.getInit().accept(this);
        }

        String condLabel = newLabel("for_cond");
        String bodyLabel = newLabel("for_body");
        String updateLabel = newLabel("for_update");
        String endLabel = newLabel("for_end");

        IrInstruction jmpCond = new IrInstruction(IrOpcode.JMP);
        jmpCond.setLabelTarget(condLabel);
        emit(jmpCond);

        startBlock(condLabel);
        obj.getCondition().accept(this);
        IrInstruction brTrue = new IrInstruction(IrOpcode.BR_TRUE);
        brTrue.addOperand(this.lastValue);
        brTrue.setLabelTarget(bodyLabel);
        emit(brTrue);

        IrInstruction jmpEnd = new IrInstruction(IrOpcode.JMP);
        jmpEnd.setLabelTarget(endLabel);
        emit(jmpEnd);

        startBlock(bodyLabel);
        breakStack.push(endLabel);
        continueStack.push(updateLabel);
        obj.getBody().accept(this);
        breakStack.pop();
        continueStack.pop();

        IrInstruction jmpUpdate = new IrInstruction(IrOpcode.JMP);
        jmpUpdate.setLabelTarget(updateLabel);
        emit(jmpUpdate);

        startBlock(updateLabel);
        if (obj.getUpdate() != null) {
            obj.getUpdate().accept(this);
        }
        IrInstruction back = new IrInstruction(IrOpcode.JMP);
        back.setLabelTarget(condLabel);
        emit(back);

        startBlock(endLabel);
    }

    @Override
    public void visit(Ast.Stmt.Break obj) {
        if (!breakStack.isEmpty()) {
            IrInstruction jmp = new IrInstruction(IrOpcode.JMP);
            jmp.setLabelTarget(breakStack.peek());
            emit(jmp);
        }
    }

    @Override
    public void visit(Ast.Stmt.Continue obj) {
        if (!continueStack.isEmpty()) {
            IrInstruction jmp = new IrInstruction(IrOpcode.JMP);
            jmp.setLabelTarget(continueStack.peek());
            emit(jmp);
        }
    }

    @Override
    public void visit(Ast.Stmt.Call obj) {
        emitCall(obj.getName(), obj.getInputParams(), false);
    }

    @Override
    public void visit(Ast.Stmt.ArrayAssign obj) {
        IrValue arrReg = getVar(obj.getArrayName());
        obj.getIndex().accept(this);
        IrValue index = this.lastValue;
        obj.getExpr().accept(this);
        IrValue value = castIfNeeded(this.lastValue, arrReg.getType().elementType());
        IrInstruction arrSet = new IrInstruction(IrOpcode.ARR_SET);
        arrSet.addOperand(arrReg);
        arrSet.addOperand(index);
        arrSet.addOperand(value);
        emit(arrSet);
    }

    @Override public void visit(Ast.Expr.Add obj) { visitBinary(obj.getLeft(), obj.getRight(), IrOpcode.ADD); }
    @Override public void visit(Ast.Expr.Sub obj) { visitBinary(obj.getLeft(), obj.getRight(), IrOpcode.SUB); }
    @Override public void visit(Ast.Expr.Mul obj) { visitBinary(obj.getLeft(), obj.getRight(), IrOpcode.MUL); }
    @Override public void visit(Ast.Expr.Div obj) { visitBinary(obj.getLeft(), obj.getRight(), IrOpcode.DIV); }
    @Override public void visit(Ast.Expr.Mod obj) { visitBinary(obj.getLeft(), obj.getRight(), IrOpcode.MOD); }
    @Override public void visit(Ast.Expr.EQ obj) { visitCompare(obj.getLeft(), obj.getRight(), IrOpcode.EQ); }
    @Override public void visit(Ast.Expr.NEQ obj) { visitCompare(obj.getLeft(), obj.getRight(), IrOpcode.NE); }
    @Override public void visit(Ast.Expr.GT obj) { visitCompare(obj.getLeft(), obj.getRight(), IrOpcode.GT); }
    @Override public void visit(Ast.Expr.LT obj) { visitCompare(obj.getLeft(), obj.getRight(), IrOpcode.LT); }
    @Override public void visit(Ast.Expr.GTE obj) { visitCompare(obj.getLeft(), obj.getRight(), IrOpcode.GE); }
    @Override public void visit(Ast.Expr.LTE obj) { visitCompare(obj.getLeft(), obj.getRight(), IrOpcode.LE); }

    @Override
    public void visit(Ast.Expr.And obj) {
        String falseLabel = newLabel("and_false");
        String endLabel = newLabel("and_end");
        IrValue result = newVReg(IrType.BOOL);

        obj.getLeft().accept(this);
        IrInstruction brFalse = new IrInstruction(IrOpcode.BR_FALSE);
        brFalse.addOperand(this.lastValue);
        brFalse.setLabelTarget(falseLabel);
        emit(brFalse);

        obj.getRight().accept(this);
        emit(instruction(IrOpcode.MOV, IrType.BOOL, result, this.lastValue));

        IrInstruction jmpEnd = new IrInstruction(IrOpcode.JMP);
        jmpEnd.setLabelTarget(endLabel);
        emit(jmpEnd);

        startBlock(falseLabel);
        emit(instruction(IrOpcode.MOV, IrType.BOOL, result, IrValue.constBool(false)));

        startBlock(endLabel);
        this.lastValue = result;
    }

    @Override
    public void visit(Ast.Expr.Or obj) {
        String trueLabel = newLabel("or_true");
        String endLabel = newLabel("or_end");
        IrValue result = newVReg(IrType.BOOL);

        obj.getLeft().accept(this);
        IrInstruction brTrue = new IrInstruction(IrOpcode.BR_TRUE);
        brTrue.addOperand(this.lastValue);
        brTrue.setLabelTarget(trueLabel);
        emit(brTrue);

        obj.getRight().accept(this);
        emit(instruction(IrOpcode.MOV, IrType.BOOL, result, this.lastValue));

        IrInstruction jmpEnd = new IrInstruction(IrOpcode.JMP);
        jmpEnd.setLabelTarget(endLabel);
        emit(jmpEnd);

        startBlock(trueLabel);
        emit(instruction(IrOpcode.MOV, IrType.BOOL, result, IrValue.constBool(true)));

        startBlock(endLabel);
        this.lastValue = result;
    }

    @Override
    public void visit(Ast.Expr.Not obj) {
        obj.getExpr().accept(this);
        IrValue result = newVReg(IrType.BOOL);
        emit(instruction(IrOpcode.NOT, IrType.BOOL, result, this.lastValue));
        this.lastValue = result;
    }

    @Override
    public void visit(Ast.Expr.UnaryMinus obj) {
        obj.getExpr().accept(this);
        IrValue exprValue = this.lastValue;
        IrType type = exprValue.getType();
        IrValue zeroValue;
        if (type == IrType.INT) zeroValue = IrValue.constInt(0);
        else if (type == IrType.FLOAT) zeroValue = IrValue.constFloat(0f);
        else if (type == IrType.DOUBLE) zeroValue = IrValue.constDouble(0d);
        else throw new CompilerException("Cannot apply unary minus to type " + type);
        IrValue targetReg = newVReg(type);
        emit(instruction(IrOpcode.SUB, type, targetReg, zeroValue, exprValue));
        this.lastValue = targetReg;
    }

    @Override
    public void visit(Ast.Expr.Call obj) {
        this.lastValue = emitCall(obj.getName(), obj.getInputParams(), true);
    }

    @Override
    public void visit(Ast.Expr.Id obj) {
        this.lastValue = getVar(obj.getId());
    }

    @Override
    public void visit(Ast.Expr.Number obj) {
        Object val = obj.getValue();
        String strVal = val.toString();
        if (obj.getType() instanceof Ast.Type.Int) {
            this.lastValue = IrValue.constInt(IntegerLiterals.parse(strVal));
        } else if (obj.getType() instanceof Ast.Type.Float) {
            this.lastValue = IrValue.constFloat(Float.parseFloat(strVal));
        } else if (obj.getType() instanceof Ast.Type.Double) {
            this.lastValue = IrValue.constDouble(Double.parseDouble(strVal));
        } else {
            throw new CompilerException("Unsupported numeric literal type: " + obj.getType());
        }
    }

    @Override public void visit(Ast.Expr.True obj) { this.lastValue = IrValue.constBool(true); }
    @Override public void visit(Ast.Expr.False obj) { this.lastValue = IrValue.constBool(false); }
    @Override public void visit(Ast.Expr.Str obj) { this.lastValue = IrValue.constString(obj.getValue()); }

    @Override
    public void visit(Ast.Expr.ArrayAccess obj) {
        IrValue arrReg = getVar(obj.getArrayName());
        obj.getIndex().accept(this);
        IrValue result = newVReg(arrReg.getType().elementType());
        emit(instruction(IrOpcode.ARR_GET, result.getType(), result, arrReg, this.lastValue));
        this.lastValue = result;
    }

    @Override
    public void visit(Ast.Expr.ArrayLength obj) {
        IrValue arrReg = getVar(obj.getArrayName());
        IrValue result = newVReg(IrType.INT);
        emit(instruction(IrOpcode.ARR_LEN, IrType.INT, result, arrReg));
        this.lastValue = result;
    }

    @Override public void visit(Ast.Expr.T obj) { obj.accept(this); }
    @Override public void visit(Ast.Stmt.T obj) { obj.accept(this); }
    @Override public void visit(Ast.Expr obj) {}
    @Override public void visit(Ast.Type.T obj) {}
    @Override public void visit(Ast.Type.Bool obj) {}
    @Override public void visit(Ast.Type.Float obj) {}
    @Override public void visit(Ast.Type.Double obj) {}
    @Override public void visit(Ast.Type.Str obj) {}
    @Override public void visit(Ast.Type obj) {}
    @Override public void visit(Ast.Type.Void obj) {}
    @Override public void visit(Ast.Type.Int obj) {}
    @Override public void visit(Ast.Type.IntArray obj) {}
    @Override public void visit(Ast.Type.FloatArray obj) {}
    @Override public void visit(Ast.Type.DoubleArray obj) {}
    @Override public void visit(Ast.Type.BoolArray obj) {}
    @Override public void visit(Ast.Declare.T obj) {}
}
