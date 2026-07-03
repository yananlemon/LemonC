import os
import re

files = [
    'e:/personal-code-new-os/LemonC/src/main/java/site/ilemon/optimizer/AstOptimizer.java',
    'e:/personal-code-new-os/LemonC/src/main/java/site/ilemon/ir/AstToIrTranslator.java',
    'e:/personal-code-new-os/LemonC/src/main/java/site/ilemon/codegen/ByteCodeGenerator.java',
    'e:/personal-code-new-os/LemonC/src/main/java/site/ilemon/codegen/TranslatorVisitor.java'
]

for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'void visit(Ast.Expr.UnaryMinus' in content:
        continue

    # Find public void visit(Ast.Expr.Not obj) { ... }
    # and insert after it.
    match = re.search(r'public void visit\(Ast\.Expr\.Not (.*?)\{.*?\}', content, re.DOTALL)
    if match:
        end_idx = match.end()
        # count braces to find the real end
        braces = 0
        in_method = False
        for i in range(match.start(), len(content)):
            if content[i] == '{':
                braces += 1
                in_method = True
            elif content[i] == '}':
                braces -= 1
                if in_method and braces == 0:
                    end_idx = i + 1
                    break
        
        # Now construct the method based on the class
        method = '\n\n    @Override\n    public void visit(Ast.Expr.UnaryMinus obj) {\n'
        if 'AstOptimizer' in file:
            method += '''        obj.getExpr().accept(this);
        if (this.lastValue instanceof Ast.Expr.Number) {
            Ast.Expr.Number num = (Ast.Expr.Number) this.lastValue;
            if (num.getType() instanceof Ast.Type.Int) {
                this.lastValue = new Ast.Expr.Number(num.getType(), -((Integer) num.getValue()), obj.getLineNum());
                return;
            } else if (num.getType() instanceof Ast.Type.Float) {
                this.lastValue = new Ast.Expr.Number(num.getType(), -((Float) num.getValue()), obj.getLineNum());
                return;
            } else if (num.getType() instanceof Ast.Type.Double) {
                this.lastValue = new Ast.Expr.Number(num.getType(), -((Double) num.getValue()), obj.getLineNum());
                return;
            }
        }
        this.lastValue = new Ast.Expr.UnaryMinus((Ast.Expr.T) this.lastValue, obj.getLineNum());
'''
        elif 'AstToIrTranslator' in file:
            method += '''        obj.getExpr().accept(this);
        site.ilemon.ir.IrValue exprValue = this.lastValue;
        site.ilemon.ir.IrType type = exprValue.getType();
        site.ilemon.ir.IrValue zeroValue;
        if (type == site.ilemon.ir.IrType.INT) zeroValue = new site.ilemon.ir.IrValue.IntConstant(0);
        else if (type == site.ilemon.ir.IrType.FLOAT) zeroValue = new site.ilemon.ir.IrValue.FloatConstant(0f);
        else if (type == site.ilemon.ir.IrType.DOUBLE) zeroValue = new site.ilemon.ir.IrValue.DoubleConstant(0d);
        else throw new site.ilemon.exception.CompilerException("Cannot apply unary minus to type " + type);
        site.ilemon.ir.IrValue targetReg = allocateReg(type);
        emit(instruction(site.ilemon.ir.IrOpcode.SUB, type, targetReg, zeroValue, exprValue));
        this.lastValue = targetReg;
'''
        elif 'ByteCodeGenerator' in file:
            method += '''        // Not implemented because ByteCodeGenerator processes IR, not AST expressions directly.
        // It implements ISemanticVisitor due to legacy reasons.
'''
        elif 'TranslatorVisitor' in file:
            method += '''        obj.getExpr().accept(this);
        if (this.currType instanceof Ast.Type.Int) {
            emit(new Ast.Stmt.Ineg());
        } else if (this.currType instanceof Ast.Type.Float) {
            emit(new Ast.Stmt.Fneg());
        } else if (this.currType instanceof Ast.Type.Double) {
            emit(new Ast.Stmt.Dneg());
        } else {
            error("Unsupported type for unary minus");
        }
'''
        method += '    }'
        
        new_content = content[:end_idx] + method + content[end_idx:]
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {file}")
