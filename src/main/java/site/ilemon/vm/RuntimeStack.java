package site.ilemon.vm;

/**
 * LemonVM 运行时栈。
 *
 * 参考 XVM 的 RuntimeStack 结构体。栈用 Value 数组实现，
 * 支持绝对索引（全局变量）和相对索引（局部变量/参数）两种寻址方式。
 *
 * 栈帧布局（与 XVM 一致）：
 * <pre>
 *   栈增长方向 ↑
 *
 *   ┌───────────────────┐ ← topIndex
 *   │ FrameMarker       │   栈帧顶部标记 (funcIndex + oldFrameIndex)
 *   ├───────────────────┤ ← frameIndex
 *   │ Local Var N-1     │   -1
 *   │ Local Var N-2     │   -2
 *   │ ...               │
 *   │ Local Var 0       │   -(N)
 *   ├───────────────────┤
 *   │ Return Address    │   -(N+1)
 *   ├───────────────────┤
 *   │ Param K-1         │   -(N+2)
 *   │ ...               │
 *   │ Param 0           │   -(N+K+1)
 *   └───────────────────┘
 * </pre>
 */
public class RuntimeStack {

    /** 默认栈大小 */
    public static final int DEFAULT_STACK_SIZE = 4096;

    /** 栈元素数组 */
    private final Value[] elements;

    /** 栈大小 */
    private final int size;

    /** 栈顶索引（指向下一个空位） */
    private int topIndex;

    /** 当前栈帧索引（相对索引的基准点） */
    private int frameIndex;

    public RuntimeStack() {
        this(DEFAULT_STACK_SIZE);
    }

    public RuntimeStack(int size) {
        this.size = size;
        this.elements = new Value[size];
        this.topIndex = 0;
        this.frameIndex = 0;
        // 初始化所有元素为 NULL
        for (int i = 0; i < size; i++) {
            elements[i] = new Value();
        }
    }

    // ---- 索引解析 ----

    /**
     * 解析栈索引：负索引相对于 frameIndex，正索引为绝对索引。
     * 参考 XVM 的 ResolveStackIndex 宏。
     */
    public int resolveIndex(int index) {
        if (index < 0) {
            return frameIndex + index;
        }
        return index;
    }

    // ---- 值访问 ----

    /**
     * 获取指定索引的值（自动解析相对索引）。
     */
    public Value getValue(int index) {
        int absIndex = resolveIndex(index);
        checkBounds(absIndex);
        return elements[absIndex];
    }

    /**
     * 设置指定索引的值（自动解析相对索引）。
     */
    public void setValue(int index, Value val) {
        int absIndex = resolveIndex(index);
        checkBounds(absIndex);
        elements[absIndex].copyFrom(val);
    }

    // ---- Push / Pop ----

    /**
     * 压入一个值到栈顶。
     * 参考 XVM 的 Push()。
     */
    public void push(Value val) {
        if (topIndex >= size) {
            throw new VmException("栈溢出: topIndex=" + topIndex + ", size=" + size);
        }
        elements[topIndex].copyFrom(val);
        topIndex++;
    }

    /**
     * 从栈顶弹出一个值。
     * 参考 XVM 的 Pop()。
     */
    public Value pop() {
        if (topIndex <= 0) {
            throw new VmException("栈下溢: topIndex=" + topIndex);
        }
        topIndex--;
        return elements[topIndex].copy();
    }

    // ---- 栈帧管理 ----

    /**
     * 压入一个栈帧。
     * 参考 XVM 的 PushFrame()。
     *
     * @param frameSize 帧大小（局部变量数量 + 1 给栈帧标记）
     */
    public void pushFrame(int frameSize) {
        int oldTopIndex = topIndex;
        int newTopIndex = topIndex + frameSize;
        if (newTopIndex > size) {
            throw new VmException("栈溢出（压入栈帧）: topIndex=" + newTopIndex + ", size=" + size);
        }
        topIndex = newTopIndex;
        for (int i = oldTopIndex; i < topIndex; i++) {
            elements[i] = new Value();
        }
        frameIndex = topIndex - 1;
    }

    /**
     * 弹出一个栈帧。
     * 参考 XVM 的 PopFrame()。
     *
     * @param frameSize 帧大小
     */
    public void popFrame(int frameSize) {
        int oldTopIndex = topIndex;
        int newTopIndex = topIndex - frameSize;
        if (newTopIndex < 0) {
            throw new VmException("栈下溢（弹出栈帧）: topIndex=" + newTopIndex);
        }
        topIndex = newTopIndex;
        for (int i = topIndex; i < oldTopIndex; i++) {
            elements[i] = new Value();
        }
    }

    // ---- Getter / Setter ----

    public int getTopIndex() { return topIndex; }
    public int getFrameIndex() { return frameIndex; }
    public void setFrameIndex(int frameIndex) { this.frameIndex = frameIndex; }
    public int getSize() { return size; }

    // ---- 工具方法 ----

    private void checkBounds(int absIndex) {
        if (absIndex < 0 || absIndex >= size) {
            throw new VmException("栈索引越界: absIndex=" + absIndex
                    + ", frameIndex=" + frameIndex + ", size=" + size);
        }
    }

    /**
     * 将栈的当前状态格式化为字符串（用于调试）。
     */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack[top=").append(topIndex)
                .append(", frame=").append(frameIndex).append("]: ");
        for (int i = 0; i < topIndex && i < 20; i++) {
            sb.append("[").append(i).append("]=").append(elements[i]).append(" ");
        }
        if (topIndex > 20) {
            sb.append("...");
        }
        return sb.toString();
    }
}
