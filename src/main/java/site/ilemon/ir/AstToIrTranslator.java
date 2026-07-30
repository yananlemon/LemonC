package site.ilemon.ir;

import site.ilemon.exception.CompilerException;
import site.ilemon.source.SourceSpan;
import site.ilemon.typedast.TypedAst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/** Translates immutable Typed-AST into typed three-address LemonIR. */
public final class AstToIrTranslator {
    private IrProgram program;
    private IrFunction currentFunction;
    private IrBlock currentBlock;
    private int nextVRegId;
    private int nextLabelId;
    private SourceSpan currentSourceSpan = SourceSpan.UNKNOWN;

    private final Map<TypedAst.Symbol, IrValue> variables =
            new HashMap<TypedAst.Symbol, IrValue>();
    private final Stack<String> breakStack = new Stack<String>();
    private final Stack<String> continueStack = new Stack<String>();

    public IrProgram translate(TypedAst.Program source) {
        program = new IrProgram();
        program.setClassName(source.getClassName());
        nextLabelId = 0;
        for (TypedAst.Method method : source.getMethods()) {
            translateMethod(method);
        }
        return program;
    }

    public IrProgram getProgram() {
        if (program == null) {
            throw new IllegalStateException("Typed-AST has not been translated");
        }
        return program;
    }

    private void translateMethod(TypedAst.Method source) {
        currentSourceSpan = source.getSourceSpan();
        IrFunction function = new IrFunction(source.getName());
        function.setReturnType(toIrType(source.getReturnType()));
        program.addFunction(function);
        currentFunction = function;
        currentBlock = null;
        nextVRegId = 0;
        variables.clear();
        breakStack.clear();
        continueStack.clear();

        for (TypedAst.Declaration formal : source.getFormals()) {
            IrValue parameter = newVReg(toIrType(formal.getType()));
            variables.put(formal.getSymbol(), parameter);
            function.addParameter(parameter);
        }
        for (TypedAst.Declaration local : source.getLocals()) {
            variables.put(local.getSymbol(), newVReg(toIrType(local.getType())));
        }

        startBlock(source.getName() + "_entry");
        translateStatements(source.getStatements());
        if (hasOpenBlock()) {
            if (function.getReturnType() != IrType.VOID) {
                throw new CompilerException("Non-void function can reach its end without returning: "
                        + function.getName());
            }
            emit(new IrInstruction(IrOpcode.RET));
        }
        currentFunction = null;
        currentBlock = null;
        currentSourceSpan = SourceSpan.UNKNOWN;
    }

    private void translateStatements(List<TypedAst.Stmt> statements) {
        for (TypedAst.Stmt statement : statements) {
            if (!hasOpenBlock()) {
                break;
            }
            translateStatement(statement);
        }
    }

    private void translateStatement(TypedAst.Stmt statement) {
        SourceSpan enclosingSpan = currentSourceSpan;
        currentSourceSpan = statement.getSourceSpan();
        try {
            translateStatementInternal(statement);
        } finally {
            currentSourceSpan = enclosingSpan;
        }
    }

    private void translateStatementInternal(TypedAst.Stmt statement) {
        if (statement instanceof TypedAst.Assign) {
            TypedAst.Assign node = (TypedAst.Assign) statement;
            emitAssignment(node.getTarget(), node.getExpression());
        } else if (statement instanceof TypedAst.VarDecl) {
            TypedAst.VarDecl node = (TypedAst.VarDecl) statement;
            IrValue target = getVariable(node.getDeclaration().getSymbol());
            int size = node.getDeclaration().getType().getArraySize();
            if (size > 0) {
                IrInstruction allocation = new IrInstruction(IrOpcode.NEW_ARR);
                allocation.setType(toIrType(node.getDeclaration().getType()));
                allocation.setResult(target);
                allocation.addOperand(IrValue.constInt(size));
                emit(allocation);
            }
            if (node.getInitializer() != null) {
                emitAssignment(node.getDeclaration().getSymbol(), node.getInitializer());
            }
        } else if (statement instanceof TypedAst.Block) {
            translateStatements(((TypedAst.Block) statement).getStatements());
        } else if (statement instanceof TypedAst.Printf) {
            translatePrintf((TypedAst.Printf) statement);
        } else if (statement instanceof TypedAst.PrintLine) {
            emit(new IrInstruction(IrOpcode.PRINT_NL));
        } else if (statement instanceof TypedAst.Return) {
            TypedAst.Return node = (TypedAst.Return) statement;
            IrInstruction ret = new IrInstruction(IrOpcode.RET);
            if (node.getExpression() != null) {
                ret.addOperand(translateExpressionAs(node.getExpression(),
                        currentFunction.getReturnType()));
            }
            emit(ret);
        } else if (statement instanceof TypedAst.If) {
            translateIf((TypedAst.If) statement);
        } else if (statement instanceof TypedAst.While) {
            translateWhile((TypedAst.While) statement);
        } else if (statement instanceof TypedAst.For) {
            translateFor((TypedAst.For) statement);
        } else if (statement instanceof TypedAst.Break) {
            if (breakStack.isEmpty()) {
                throw new CompilerException("Typed-AST contains break outside a loop");
            }
            emitJump(breakStack.peek());
        } else if (statement instanceof TypedAst.Continue) {
            if (continueStack.isEmpty()) {
                throw new CompilerException("Typed-AST contains continue outside a loop");
            }
            emitJump(continueStack.peek());
        } else if (statement instanceof TypedAst.CallStmt) {
            TypedAst.CallStmt node = (TypedAst.CallStmt) statement;
            emitCall(node.getMethod(), node.getArguments(), false);
        } else if (statement instanceof TypedAst.ArrayAssign) {
            TypedAst.ArrayAssign node = (TypedAst.ArrayAssign) statement;
            IrValue array = getVariable(node.getArray());
            IrValue index = translateExpression(node.getIndex());
            IrValue value = translateExpressionAs(
                    node.getExpression(), array.getType().elementType());
            IrInstruction set = new IrInstruction(IrOpcode.ARR_SET);
            set.addOperand(array);
            set.addOperand(index);
            set.addOperand(value);
            emit(set);
        } else {
            throw new CompilerException("Unsupported Typed-AST statement: "
                    + statement.getClass().getSimpleName());
        }
    }

    private void emitAssignment(TypedAst.Symbol targetSymbol, TypedAst.Expr expression) {
        IrValue target = getVariable(targetSymbol);
        IrValue value = translateExpressionAs(expression, target.getType());
        emit(instruction(IrOpcode.MOV, target.getType(), target, value));
    }

    private void translatePrintf(TypedAst.Printf source) {
        String format = source.getFormat();
        StringBuilder literal = new StringBuilder();
        int argumentIndex = 0;
        for (int i = 0; i < format.length(); i++) {
            char ch = format.charAt(i);
            if (ch != '%') {
                literal.append(ch);
                continue;
            }
            emitPrintString(literal.toString());
            literal.setLength(0);
            char placeholder = format.charAt(++i);
            if (placeholder != 'd' && placeholder != 'f') {
                throw new CompilerException("Invalid placeholder reached IR translation: %" + placeholder);
            }
            IrValue value = translateExpression(source.getExpressions().get(argumentIndex++));
            IrInstruction print = new IrInstruction(IrOpcode.PRINT);
            print.setType(value.getType());
            print.addOperand(value);
            emit(print);
        }
        emitPrintString(literal.toString());
    }

    private void translateIf(TypedAst.If source) {
        IrValue condition = translateExpression(source.getCondition());
        String trueLabel = newLabel("if_true");
        String falseLabel = newLabel("if_false");
        String endLabel = newLabel("if_end");

        emitBranch(IrOpcode.BR_TRUE, condition, trueLabel);
        emitJump(source.getElseStatement() == null ? endLabel : falseLabel);

        startBlock(trueLabel);
        translateStatement(source.getThenStatement());
        boolean thenFallsThrough = hasOpenBlock();
        if (thenFallsThrough) emitJump(endLabel);

        if (source.getElseStatement() == null) {
            startBlock(endLabel);
            return;
        }
        startBlock(falseLabel);
        translateStatement(source.getElseStatement());
        boolean elseFallsThrough = hasOpenBlock();
        if (elseFallsThrough) emitJump(endLabel);
        if (thenFallsThrough || elseFallsThrough) {
            startBlock(endLabel);
        } else {
            currentBlock = null;
        }
    }

    private void translateWhile(TypedAst.While source) {
        String conditionLabel = newLabel("while_cond");
        String bodyLabel = newLabel("while_body");
        String endLabel = newLabel("while_end");
        emitJump(conditionLabel);

        startBlock(conditionLabel);
        IrValue condition = translateExpression(source.getCondition());
        emitBranch(IrOpcode.BR_TRUE, condition, bodyLabel);
        emitJump(endLabel);

        startBlock(bodyLabel);
        breakStack.push(endLabel);
        continueStack.push(conditionLabel);
        translateStatement(source.getBody());
        continueStack.pop();
        breakStack.pop();
        if (hasOpenBlock()) emitJump(conditionLabel);
        startBlock(endLabel);
    }

    private void translateFor(TypedAst.For source) {
        if (source.getInitializer() != null) translateStatement(source.getInitializer());
        String conditionLabel = newLabel("for_cond");
        String bodyLabel = newLabel("for_body");
        String updateLabel = newLabel("for_update");
        String endLabel = newLabel("for_end");
        emitJump(conditionLabel);

        startBlock(conditionLabel);
        IrValue condition = translateExpression(source.getCondition());
        emitBranch(IrOpcode.BR_TRUE, condition, bodyLabel);
        emitJump(endLabel);

        startBlock(bodyLabel);
        breakStack.push(endLabel);
        continueStack.push(updateLabel);
        translateStatement(source.getBody());
        continueStack.pop();
        breakStack.pop();
        if (hasOpenBlock()) emitJump(updateLabel);

        startBlock(updateLabel);
        if (source.getUpdate() != null) translateStatement(source.getUpdate());
        if (hasOpenBlock()) emitJump(conditionLabel);
        startBlock(endLabel);
    }

    private IrValue translateExpression(TypedAst.Expr expression) {
        SourceSpan enclosingSpan = currentSourceSpan;
        currentSourceSpan = expression.getSourceSpan();
        try {
            return translateExpressionInternal(expression);
        } finally {
            currentSourceSpan = enclosingSpan;
        }
    }

    private IrValue translateExpressionInternal(TypedAst.Expr expression) {
        if (expression instanceof TypedAst.IntLiteral) {
            return IrValue.constInt(((TypedAst.IntLiteral) expression).getValue());
        }
        if (expression instanceof TypedAst.FloatLiteral) {
            return IrValue.constFloat(((TypedAst.FloatLiteral) expression).getValue());
        }
        if (expression instanceof TypedAst.DoubleLiteral) {
            return IrValue.constDouble(((TypedAst.DoubleLiteral) expression).getValue());
        }
        if (expression instanceof TypedAst.BoolLiteral) {
            return IrValue.constBool(((TypedAst.BoolLiteral) expression).getValue());
        }
        if (expression instanceof TypedAst.StringLiteral) {
            return IrValue.constString(((TypedAst.StringLiteral) expression).getValue());
        }
        if (expression instanceof TypedAst.Id) {
            return getVariable(((TypedAst.Id) expression).getSymbol());
        }
        if (expression instanceof TypedAst.Call) {
            TypedAst.Call node = (TypedAst.Call) expression;
            return emitCall(node.getMethod(), node.getArguments(), true);
        }
        if (expression instanceof TypedAst.ArrayAccess) {
            TypedAst.ArrayAccess node = (TypedAst.ArrayAccess) expression;
            IrValue array = getVariable(node.getArray());
            IrValue index = translateExpression(node.getIndex());
            IrValue result = newVReg(array.getType().elementType());
            emit(instruction(IrOpcode.ARR_GET, result.getType(), result, array, index));
            return result;
        }
        if (expression instanceof TypedAst.ArrayLength) {
            IrValue array = getVariable(((TypedAst.ArrayLength) expression).getArray());
            IrValue result = newVReg(IrType.INT);
            emit(instruction(IrOpcode.ARR_LEN, IrType.INT, result, array));
            return result;
        }
        if (expression instanceof TypedAst.Not) {
            IrValue operand = translateExpression(((TypedAst.Not) expression).getExpression());
            IrValue result = newVReg(IrType.BOOL);
            emit(instruction(IrOpcode.NOT, IrType.BOOL, result, operand));
            return result;
        }
        if (expression instanceof TypedAst.UnaryMinus) {
            // 必须用 NEG，不能降为 0 - x：IEEE-754 下 0.0 - 0.0 是 +0.0，而 -(0.0) 是 -0.0。
            IrValue operand = translateExpression(((TypedAst.UnaryMinus) expression).getExpression());
            IrType type = operand.getType();
            IrValue result = newVReg(type);
            emit(instruction(IrOpcode.NEG, type, result, operand));
            return result;
        }
        if (expression instanceof TypedAst.And) {
            return translateShortCircuit((TypedAst.BinaryExpr) expression, false);
        }
        if (expression instanceof TypedAst.Or) {
            return translateShortCircuit((TypedAst.BinaryExpr) expression, true);
        }
        if (expression instanceof TypedAst.BinaryExpr) {
            TypedAst.BinaryExpr binary = (TypedAst.BinaryExpr) expression;
            IrOpcode opcode = binaryOpcode(expression);
            if (isComparison(expression)) {
                return translateComparison(binary, opcode);
            }
            return translateBinary(binary, opcode);
        }
        if (expression instanceof TypedAst.ErrorExpr) {
            throw new CompilerException("ErrorExpr reached IR translation at line " + expression.getLineNum());
        }
        throw new CompilerException("Unsupported Typed-AST expression: "
                + expression.getClass().getSimpleName());
    }

    private IrValue translateBinary(TypedAst.BinaryExpr source, IrOpcode opcode) {
        IrType resultType = toIrType(source.getType());
        IrValue left = translateExpressionAs(source.getLeft(), resultType);
        IrValue right = translateExpressionAs(source.getRight(), resultType);
        IrValue result = newVReg(resultType);
        emit(instruction(opcode, resultType, result, left, right));
        return result;
    }

    private IrValue translateComparison(TypedAst.BinaryExpr source, IrOpcode opcode) {
        // 提升类型必须先从静态类型算出来，才能让字面量直接按提升后的类型materialize；
        // 先翻译再提升会让字面量已经定型，只能再补一条 F2D。
        IrType leftType = toIrType(source.getLeft().getType());
        IrType rightType = toIrType(source.getRight().getType());
        IrValue left;
        IrValue right;
        if (leftType.isNumeric() && rightType.isNumeric()) {
            IrType promoted = promotedNumericType(leftType, rightType);
            left = translateExpressionAs(source.getLeft(), promoted);
            right = translateExpressionAs(source.getRight(), promoted);
        } else {
            left = translateExpression(source.getLeft());
            right = translateExpression(source.getRight());
        }
        IrValue result = newVReg(IrType.BOOL);
        emit(instruction(opcode, IrType.BOOL, result, left, right));
        return result;
    }

    private IrValue translateShortCircuit(TypedAst.BinaryExpr source, boolean shortCircuitValue) {
        String rightLabel = newLabel(shortCircuitValue ? "or_right" : "and_right");
        String shortLabel = newLabel(shortCircuitValue ? "or_true" : "and_false");
        String endLabel = newLabel(shortCircuitValue ? "or_end" : "and_end");
        IrValue result = newVReg(IrType.BOOL);

        IrValue left = translateExpression(source.getLeft());
        emitBranch(shortCircuitValue ? IrOpcode.BR_TRUE : IrOpcode.BR_FALSE, left, shortLabel);
        emitJump(rightLabel);

        startBlock(rightLabel);
        emit(instruction(IrOpcode.MOV, IrType.BOOL, result,
                translateExpression(source.getRight())));
        emitJump(endLabel);

        startBlock(shortLabel);
        emit(instruction(IrOpcode.MOV, IrType.BOOL, result,
                IrValue.constBool(shortCircuitValue)));
        emitJump(endLabel);
        startBlock(endLabel);
        return result;
    }

    private IrValue emitCall(TypedAst.MethodSymbol method, List<TypedAst.Expr> arguments,
                             boolean needsResult) {
        IrType returnType = toIrType(method.getReturnType());
        IrInstruction call = new IrInstruction(IrOpcode.CALL);
        call.setType(returnType);
        call.setFuncTarget(method.getName());
        if (needsResult && returnType != IrType.VOID) {
            call.setResult(newVReg(returnType));
        }
        for (int i = 0; i < arguments.size(); i++) {
            call.addOperand(translateExpressionAs(
                    arguments.get(i), toIrType(method.getParameterTypes().get(i))));
        }
        emit(call);
        return call.getResult();
    }

    private IrValue getVariable(TypedAst.Symbol symbol) {
        IrValue value = variables.get(symbol);
        if (value == null) {
            throw new CompilerException("Unregistered symbol reached IR translation: " + symbol.getName());
        }
        return value;
    }

    private IrValue newVReg(IrType type) {
        IrValue value = IrValue.vreg(nextVRegId++, type);
        currentFunction.updateMaxVReg(value);
        return value;
    }

    private IrBlock startBlock(String label) {
        IrBlock block = new IrBlock(label);
        currentFunction.addBlock(block);
        currentBlock = block;
        return block;
    }

    private boolean hasOpenBlock() {
        return currentBlock != null && !currentBlock.isTerminated();
    }

    private void emit(IrInstruction instruction) {
        if (!hasOpenBlock()) {
            throw new CompilerException("Cannot emit LemonIR without an open basic block");
        }
        if (!instruction.getSourceSpan().isKnown()) {
            instruction.setSourceSpan(currentSourceSpan);
        }
        currentBlock.addInstruction(instruction);
    }

    private void emitJump(String target) {
        IrInstruction jump = new IrInstruction(IrOpcode.JMP);
        jump.setLabelTarget(target);
        emit(jump);
    }

    private void emitBranch(IrOpcode opcode, IrValue condition, String target) {
        IrInstruction branch = new IrInstruction(opcode);
        branch.addOperand(condition);
        branch.setLabelTarget(target);
        emit(branch);
    }

    private String newLabel(String prefix) {
        return "_" + prefix + "_" + nextLabelId++;
    }

    private void emitPrintString(String text) {
        if (text.isEmpty()) return;
        IrInstruction print = new IrInstruction(IrOpcode.PRINT);
        print.setType(IrType.STRING);
        print.addOperand(IrValue.constString(text));
        emit(print);
    }

    private IrInstruction instruction(IrOpcode opcode, IrType type, IrValue result,
                                      IrValue... operands) {
        IrInstruction instruction = new IrInstruction(opcode);
        instruction.setType(type);
        instruction.setResult(result);
        for (IrValue operand : operands) instruction.addOperand(operand);
        return instruction;
    }

    private IrValue castIfNeeded(IrValue value, IrType targetType) {
        if (value.getType() == targetType) return value;
        IrOpcode opcode = null;
        if (value.getType() == IrType.INT && targetType == IrType.FLOAT) opcode = IrOpcode.I2F;
        else if (value.getType() == IrType.INT && targetType == IrType.DOUBLE) opcode = IrOpcode.I2D;
        else if (value.getType() == IrType.FLOAT && targetType == IrType.DOUBLE) opcode = IrOpcode.F2D;
        else if (value.getType() == IrType.BOOL && targetType == IrType.INT) return value;
        if (opcode == null) {
            throw new CompilerException("Unsupported implicit cast from " + value.getType()
                    + " to " + targetType);
        }
        IrValue result = newVReg(targetType);
        emit(instruction(opcode, targetType, result, value));
        return result;
    }

    /**
     * 把表达式翻译成目标类型的值。
     *
     * <p>这是所有需要隐式转换的地方的唯一入口。数值字面量在这里按目标类型直接materialize，
     * 而不是先按自身类型materialize、再插一条转换指令——否则同一个字面量在不同语法位置
     * 会取到不同的值：{@code d = 0.1} 走赋值路径得到 0.1，而 {@code d = 0.0d + 0.1}
     * 走 F2D 会得到 0.10000000149011612。</p>
     *
     * <p>求值顺序与副作用不受影响：只有字面量走快路径，其余一律照常翻译。</p>
     */
    private IrValue translateExpressionAs(TypedAst.Expr expression, IrType targetType) {
        IrValue literal = numericLiteralForTarget(expression, targetType);
        if (literal != null) {
            return literal;
        }
        return castIfNeeded(translateExpression(expression), targetType);
    }

    /**
     * 按目标类型直接构造数值字面量；无法这样处理时返回 null。
     *
     * <p>小数字面量默认是 float 类型，但它携带的是一个十进制常量。用在 double 位置时
     * 按十进制原文以 double 精度materialize，而不是先舍入成 float 再加宽——否则
     * {@code double a = 3.14159265358979;} 会静默损失 8 位有效数字。</p>
     */
    private IrValue numericLiteralForTarget(TypedAst.Expr expression, IrType targetType) {
        if (expression instanceof TypedAst.IntLiteral) {
            TypedAst.IntLiteral literal = (TypedAst.IntLiteral) expression;
            int value = literal.getValue();
            if (targetType == IrType.INT) return IrValue.constInt(value);
            if (targetType == IrType.FLOAT) return IrValue.constFloat((float) value);
            if (targetType == IrType.DOUBLE) return IrValue.constDouble((double) value);
        }
        if (expression instanceof TypedAst.FloatLiteral) {
            TypedAst.FloatLiteral literal = (TypedAst.FloatLiteral) expression;
            if (targetType == IrType.FLOAT) return IrValue.constFloat(literal.getValue());
            if (targetType == IrType.DOUBLE) return IrValue.constDouble(
                    Double.parseDouble(literal.getRawValue()));
        }
        if (expression instanceof TypedAst.DoubleLiteral && targetType == IrType.DOUBLE) {
            return IrValue.constDouble(((TypedAst.DoubleLiteral) expression).getValue());
        }
        return null;
    }

    private IrType toIrType(TypedAst.Type type) {
        switch (type.getKind()) {
            case INT: return IrType.INT;
            case FLOAT: return IrType.FLOAT;
            case DOUBLE: return IrType.DOUBLE;
            case BOOL: return IrType.BOOL;
            case STRING: return IrType.STRING;
            case VOID: return IrType.VOID;
            case INT_ARRAY: return IrType.INT_ARRAY;
            case FLOAT_ARRAY: return IrType.FLOAT_ARRAY;
            case DOUBLE_ARRAY: return IrType.DOUBLE_ARRAY;
            case BOOL_ARRAY: return IrType.BOOL_ARRAY;
            default: throw new CompilerException("Error type reached IR translation");
        }
    }

    private IrType promotedNumericType(IrType left, IrType right) {
        if (left == IrType.DOUBLE || right == IrType.DOUBLE) return IrType.DOUBLE;
        if (left == IrType.FLOAT || right == IrType.FLOAT) return IrType.FLOAT;
        return IrType.INT;
    }

    private boolean isComparison(TypedAst.Expr expression) {
        return expression instanceof TypedAst.EQ || expression instanceof TypedAst.NEQ
                || expression instanceof TypedAst.GT || expression instanceof TypedAst.LT
                || expression instanceof TypedAst.GTE || expression instanceof TypedAst.LTE;
    }

    private IrOpcode binaryOpcode(TypedAst.Expr expression) {
        if (expression instanceof TypedAst.Add) return IrOpcode.ADD;
        if (expression instanceof TypedAst.Sub) return IrOpcode.SUB;
        if (expression instanceof TypedAst.Mul) return IrOpcode.MUL;
        if (expression instanceof TypedAst.Div) return IrOpcode.DIV;
        if (expression instanceof TypedAst.Mod) return IrOpcode.MOD;
        if (expression instanceof TypedAst.EQ) return IrOpcode.EQ;
        if (expression instanceof TypedAst.NEQ) return IrOpcode.NE;
        if (expression instanceof TypedAst.GT) return IrOpcode.GT;
        if (expression instanceof TypedAst.LT) return IrOpcode.LT;
        if (expression instanceof TypedAst.GTE) return IrOpcode.GE;
        if (expression instanceof TypedAst.LTE) return IrOpcode.LE;
        throw new CompilerException("Unsupported binary Typed-AST expression");
    }
}
