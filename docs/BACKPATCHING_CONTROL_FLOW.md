# TranslatorVisitor 控制流拉链回填详解

本文只解释一个点：`site.ilemon.codegen.TranslatorVisitor` 中，旧 direct JVM translator 如何用拉链回填翻译 `if` 条件控制流。

注意：当前 CLI 主编译链路已经走 `AST -> LemonIR -> JVM/LemonVM`。本文讲的是保留在源码中的旧 direct JVM translator，它仍然有测试和教学价值，但不是当前主路径。

## 源码位置

核心文件：

```text
src/main/java/site/ilemon/codegen/TranslatorVisitor.java
```

核心方法：

```text
BoolCode
makelist()
merge()
backpatch()
setJumpTarget()
translateCondition()
comparisonJumps()
visit(Stmt.If)
```

## 真实示例

示例来自：

```text
examples/If01.lemon
```

源码片段：

```c
if( 19 < 21 && 27 < 30) {
    a = 16;
}else{
    a = 17;
}
```

条件表达式的 AST 形状可以理解为：

```text
And(
  LT(19, 21),
  LT(27, 30)
)
```

## 拉链回填解决什么问题

翻译布尔表达式时，经常会先生成跳转指令，但暂时不知道目标 label。

例如 `19 < 21` 翻译时，我们知道：

```text
如果为 true，要跳到某个 true 分支
如果为 false，要跳到某个 false 分支
```

但在翻译表达式本身时，`then` 和 `else` 的 label 还没有最终确定。

所以源码采用这个策略：

```text
1. 先生成目标为 null 的跳转指令
2. 记录这些跳转指令在 stmts 列表里的下标
3. 等目标 label 确定后，再按下标回填
```

这就是这里的“拉链回填”。

## BoolCode：真假链

源码中用 `BoolCode` 保存两条链：

```java
private static class BoolCode {
    final List<Integer> trueList;
    final List<Integer> falseList;

    BoolCode(List<Integer> trueList, List<Integer> falseList) {
        this.trueList = trueList;
        this.falseList = falseList;
    }
}
```

这里的 `Integer` 是指令下标，不是 label。

含义是：

```text
trueList  = 这些指令以后要回填为 true 目标
falseList = 这些指令以后要回填为 false 目标
```

## 基础操作

### emitJump()

```java
private int emitJump(Ast.Stmt.T stmt) {
    emit(stmt);
    return this.stmts.size() - 1;
}
```

它会生成一条指令，并返回这条指令在 `stmts` 列表中的下标。

例如：

```java
int trueJump = emitJump(new Ast.Stmt.Ificmplt(null));
```

意思是：

```text
生成 if_icmplt ?
记录这条指令的下标
```

### makelist()

```java
private List<Integer> makelist(int index) {
    List<Integer> list = new ArrayList<Integer>();
    list.add(index);
    return list;
}
```

把单个待回填指令下标变成一条链。

### merge()

```java
private List<Integer> merge(List<Integer> left, List<Integer> right) {
    List<Integer> result = new ArrayList<Integer>();
    result.addAll(left);
    result.addAll(right);
    return result;
}
```

把两条待回填链合并。

### backpatch()

```java
private void backpatch(List<Integer> list, Label target) {
    for (Integer index : list) {
        setJumpTarget(this.stmts.get(index), target);
    }
}
```

这是真正的“回填”动作：

```text
根据指令下标找到跳转指令
把它的目标 label 从 null 改成 target
```

## 比较表达式如何产生真假链

对 `<` 来说，源码会进入 `comparisonJumps()`：

```java
private BoolCode comparisonJumps(String op, Ast.Type.T operandType) {
    int trueJump;

    if ("<".equals(op)) {
        trueJump = emitJump(new Ast.Stmt.Ificmplt(null));
    } else {
        // other comparisons omitted
    }

    int falseJump = emitJump(new Ast.Stmt.Goto(null));

    return new BoolCode(
            makelist(trueJump),
            makelist(falseJump)
    );
}
```

对于：

```c
19 < 21
```

可以理解为生成：

```text
ldc 19
ldc 21
if_icmplt ?
goto ?
```

此时返回：

```text
trueList  = [if_icmplt 的指令下标]
falseList = [goto 的指令下标]
```

目标都还没填。

## && 如何回填

`&&` 的源码：

```java
if (expr instanceof Expr.And) {
    Expr.And and = (Expr.And) expr;
    BoolCode left = translateCondition(and.getLeft());
    Label rightBegin = new Label();
    emit(new Ast.Stmt.LabelJ(rightBegin));
    backpatch(left.trueList, rightBegin);
    BoolCode right = translateCondition(and.getRight());
    return new BoolCode(right.trueList, merge(left.falseList, right.falseList));
}
```

这段正好对应 `E1 && E2`：

```text
E1 为 true  -> 继续计算 E2
E1 为 false -> 整个 && 为 false
```

因此：

```text
left.trueList 回填到右表达式入口 rightBegin
整个 && 的 trueList = right.trueList
整个 && 的 falseList = left.falseList + right.falseList
```

## 示例逐步展开

为了讲清楚，假设进入这个 `if` 前，`stmts` 为空。真实编译中前面可能已有指令，但链表记录的是实际下标，逻辑完全一样。

### 1. 翻译左边 `19 < 21`

生成：

```text
0: ldc 19
1: ldc 21
2: if_icmplt ?
3: goto ?
```

返回：

```text
left.trueList  = [2]
left.falseList = [3]
```

### 2. 进入 `&&`，创建右表达式入口

源码：

```java
Label rightBegin = new Label();
emit(new Ast.Stmt.LabelJ(rightBegin));
backpatch(left.trueList, rightBegin);
```

生成 label：

```text
4: L_right:
```

并回填 `left.trueList = [2]`：

```text
2: if_icmplt L_right
```

现在指令变成：

```text
0: ldc 19
1: ldc 21
2: if_icmplt L_right
3: goto ?
4: L_right:
```

第 3 条 `goto ?` 还不能填，因为它要跳到整个 `if` 的 false 分支，而 false label 还没创建。

### 3. 翻译右边 `27 < 30`

生成：

```text
5: ldc 27
6: ldc 30
7: if_icmplt ?
8: goto ?
```

返回：

```text
right.trueList  = [7]
right.falseList = [8]
```

### 4. `&&` 返回整个条件的真假链

源码：

```java
return new BoolCode(right.trueList, merge(left.falseList, right.falseList));
```

因此整个条件：

```text
condition.trueList  = [7]
condition.falseList = [3, 8]
```

解释：

```text
第 7 条：右边 true，整个 && true
第 3 条：左边 false，整个 && false
第 8 条：右边 false，整个 && false
```

## if 语句最终回填

`if` 的源码：

```java
public void visit(Stmt.If obj) {
    Label trueLabel = new Label();
    Label falseLabel = new Label();
    Label nextLabel = new Label();

    BoolCode condition = translateCondition(obj.getCondition());
    backpatch(condition.trueList, trueLabel);
    backpatch(condition.falseList, falseLabel);

    emit(new Ast.Stmt.LabelJ(trueLabel));
    this.visit(obj.getThenStmt());

    emit(new Ast.Stmt.Goto(nextLabel));

    emit(new Ast.Stmt.LabelJ(falseLabel));
    if (obj.getElseStmt() != null) {
        this.visit(obj.getElseStmt());
    }

    emit(new Ast.Stmt.Goto(nextLabel));
    emit(new Ast.Stmt.LabelJ(nextLabel));
}
```

这里创建：

```text
trueLabel  = then 分支入口
falseLabel = else 分支入口
nextLabel  = if 结束后的位置
```

然后回填：

```java
backpatch(condition.trueList, trueLabel);
backpatch(condition.falseList, falseLabel);
```

也就是：

```text
[7]    -> L_true
[3, 8] -> L_false
```

最终结构：

```text
0:  ldc 19
1:  ldc 21
2:  if_icmplt L_right
3:  goto L_false

4:  L_right:
5:  ldc 27
6:  ldc 30
7:  if_icmplt L_true
8:  goto L_false

9:  L_true:
10: a = 16
11: goto L_next

12: L_false:
13: a = 17
14: goto L_next

15: L_next:
```

## 为什么这就是短路

看第 2、3 条：

```text
2: if_icmplt L_right
3: goto L_false
```

如果 `19 < 21` 为 false，会直接执行：

```text
goto L_false
```

也就是不会进入 `L_right`，因此不会计算 `27 < 30`。

这就是 `&&` 的短路。

## 和 || 的区别

`||` 的源码：

```java
if (expr instanceof Expr.Or) {
    Expr.Or or = (Expr.Or) expr;
    BoolCode left = translateCondition(or.getLeft());
    Label rightBegin = new Label();
    emit(new Ast.Stmt.LabelJ(rightBegin));
    backpatch(left.falseList, rightBegin);
    BoolCode right = translateCondition(or.getRight());
    return new BoolCode(merge(left.trueList, right.trueList), right.falseList);
}
```

`||` 正好相反：

```text
左边 false -> 继续算右边
左边 true  -> 整个 || true
```

所以它回填的是：

```text
left.falseList -> rightBegin
```

整个 `||` 的真假链是：

```text
trueList  = left.trueList + right.trueList
falseList = right.falseList
```

## 一句话总结

`TranslatorVisitor` 的拉链回填不是把 label 立即写死，而是把“需要填 label 的跳转指令下标”保存到 `trueList` / `falseList` 中。

等到右表达式入口、then 入口、else 入口这些 label 真正创建出来后，再用 `backpatch()` 统一填回去。

对于 `if (19 < 21 && 27 < 30)`：

```text
左 true 链  -> 回填到右表达式入口
整体 true 链 -> 回填到 then
整体 false 链 -> 回填到 else
```

