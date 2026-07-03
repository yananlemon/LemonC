import os

file_path = 'e:/personal-code-new-os/LemonC/src/main/java/site/ilemon/semantic/SemanticVisitor.java'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
i = 0
while i < len(lines):
    line = lines[i]

    if 'private Ast.Type.T currType;' in line:
        line = line.replace('private Ast.Type.T currType;', 'private java.util.Stack<Ast.Type.T> typeStack = new java.util.Stack<>();')
    elif 'Ast.Type.T leftType = this.currType;' in line:
        line = line.replace('this.currType', 'this.typeStack.pop()')
    elif 'Ast.Type.T rightType = this.currType;' in line:
        line = line.replace('this.currType', 'this.typeStack.pop()')
    elif 'Ast.Type.T initializerType = this.currType;' in line:
        line = line.replace('this.currType', 'this.typeStack.pop()')
    elif 'exprType = this.currType;' in line:
        line = line.replace('this.currType', 'this.typeStack.pop()')
    elif 'leftType, this.currType);' in line:
        line = line.replace('this.currType', 'this.typeStack.pop()')
    elif 'obj.setType(this.currType);' in line:
        line = line.replace('this.currType', 'this.typeStack.peek()')
    elif 'if (this.currType.getKind() != TypeKind.BOOL)' in line:
        new_lines.append('        Ast.Type.T condType = this.typeStack.pop();\n')
        line = line.replace('this.currType', 'condType')
        # We also need to fix the next lines that use this.currType
        new_lines.append(line)
        i += 1
        while 'this.currType' in lines[i]:
            new_lines.append(lines[i].replace('this.currType', 'condType'))
            i += 1
        continue
    elif 'if (isArrayType(this.currType) || isArrayType(exprType)) {' in line:
        new_lines.append('            Ast.Type.T destType = this.typeStack.pop();\n')
        line = line.replace('this.currType', 'destType')
        new_lines.append(line)
        i += 1
        while '}' not in lines[i]:
            new_lines.append(lines[i].replace('this.currType', 'destType'))
            i += 1
        new_lines.append(lines[i]) # append the } line
        # The next if statement also uses this.currType (now destType)
        i += 1
        while '}' not in lines[i]:
            new_lines.append(lines[i].replace('this.currType', 'destType'))
            i += 1
        new_lines.append(lines[i]) # append the } line
        i += 1
        continue
    elif 'this.currType =' in line:
        import re
        line = re.sub(r'this\.currType\s*=\s*(.*?);', r'this.typeStack.push(\1);', line)
    elif 'if( this.currType.getKind() != TypeKind.BOOL)' in line:
        new_lines.append('        Ast.Type.T condType = this.typeStack.pop();\n')
        line = line.replace('this.currType', 'condType')
        new_lines.append(line)
        i += 1
        while 'this.currType' in lines[i]:
            new_lines.append(lines[i].replace('this.currType', 'condType'))
            i += 1
        continue
    elif 'if (placeholder == \'d\' && this.currType.getKind() != TypeKind.INT) {' in line:
        new_lines.append('                Ast.Type.T argType = this.typeStack.pop();\n')
        line = line.replace('this.currType', 'argType')
        new_lines.append(line)
        i += 1
        while '}' not in lines[i]:
            new_lines.append(lines[i].replace('this.currType', 'argType'))
            i += 1
        new_lines.append(lines[i])
        i += 1
        continue
    elif '&& this.currType.getKind() != TypeKind.FLOAT' in line:
        # this is part of printf float check
        # It was preceded by another line. 
        # Actually it's easier to manually replace the printf block.
        pass
    
    new_lines.append(line)
    i += 1

# Fix printf block manually
content = "".join(new_lines)
import re
content = re.sub(
    r'if\s*\(\s*placeholder\s*==\s*\'f\'\s*\n\s*&&\s*this\.currType\.getKind\(\)\s*!=\s*TypeKind\.FLOAT\s*\n\s*&&\s*this\.currType\.getKind\(\)\s*!=\s*TypeKind\.DOUBLE\s*\)\s*\{\s*\n\s*error\(obj\.getLineNum\(\),\s*String\.format\(\s*\n\s*"printf 占位符 %%f 需要 float 或 double，实际为 %s",\s*typeName\(this\.currType\)\)\);\s*\}',
    r'''Ast.Type.T argTypeF = this.typeStack.pop();
                if (placeholder == 'f'
                    && argTypeF.getKind() != TypeKind.FLOAT
                    && argTypeF.getKind() != TypeKind.DOUBLE) {
                    error(obj.getLineNum(), String.format(
                            "printf 占位符 %%f 需要 float 或 double，实际为 %s", typeName(argTypeF)));
                }''',
    content
)

# add visit(Ast.Expr.UnaryMinus) if not there
if 'public void visit(Ast.Expr.UnaryMinus obj)' not in content:
    method = '''
    @Override
    public void visit(Ast.Expr.UnaryMinus obj) {
        this.visit(obj.getExpr());
        Ast.Type.T type = this.typeStack.pop();
        if (type.getKind() != TypeKind.INT && type.getKind() != TypeKind.FLOAT && type.getKind() != TypeKind.DOUBLE) {
            error(obj.getLineNum(), "一元负号不能用于类型 " + typeName(type));
        }
        this.typeStack.push(type);
    }
'''
    content = content.replace('public void visit(Ast.Expr.Not obj) {', method + '\n    @Override\n    public void visit(Ast.Expr.Not obj) {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done")
