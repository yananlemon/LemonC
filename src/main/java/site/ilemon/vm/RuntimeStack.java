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

    /** 初始容量。栈按需增长，所以这只影响首次扩容前的表现，不构成递归深度上限。 */
    public static final int DEFAULT_STACK_SIZE = 4096;

    /**
     * 默认容量上限。存在的意义是让无穷递归以一条干净的诊断结束，
     * 而不是耗尽堆内存抛 OutOfMemoryError。
     */
    public static final int DEFAULT_MAX_STACK_SIZE = 1 << 20;

    /** 栈元素数组，按需增长 */
    private Value[] elements;

    /** 容量上限 */
    private final int maxSize;

    /** 栈顶索引（指向下一个空位） */
    private int topIndex;

    /** 当前栈帧索引（相对索引的基准点） */
    private int frameIndex;

    public RuntimeStack() {
        this(DEFAULT_STACK_SIZE, DEFAULT_MAX_STACK_SIZE);
    }

    /**
     * 固定容量的栈：初始容量与上限都是 size，不会增长。
     *
     * <p>保持这个重载的原有含义——"这就是整个栈"。想要按需增长请用双参构造。</p>
     */
    public RuntimeStack(int size) {
        this(size, size);
    }

    public RuntimeStack(int initialSize, int maxSize) {
        if (initialSize <= 0) {
            throw new VmException("栈初始容量必须为正数: " + initialSize);
        }
        if (maxSize < initialSize) {
            throw new VmException("栈容量上限不能小于初始容量: max=" + maxSize
                    + ", initial=" + initialSize);
        }
        this.maxSize = maxSize;
        this.elements = new Value[initialSize];
        this.topIndex = 0;
        this.frameIndex = 0;
        // 初始化所有元素为 NULL
        for (int i = 0; i < initialSize; i++) {
            elements[i] = new Value();
        }
    }

    /**
     * 确保容量至少为 required，不足则倍增扩容。
     *
     * <p>所有寻址都是相对 frameIndex 的负索引或绝对正索引，扩容只是把数组变长，
     * 不改变任何已有索引的含义，因此拷贝扩容是安全的。</p>
     */
    private void ensureCapacity(int required) {
        if (required <= elements.length) {
            return;
        }
        if (required > maxSize) {
            throw new VmException(String.format(
                    "栈溢出：需要 %d 个槽位，已达容量上限 %d。"
                    + "常见原因是无穷递归；确认递归有终止条件，"
                    + "或用 --vm-stack-size 提高上限", required, maxSize));
        }
        int newLength = elements.length;
        while (newLength < required) {
            newLength = newLength > maxSize / 2 ? maxSize : newLength * 2;
        }
        Value[] grown = new Value[newLength];
        System.arraycopy(elements, 0, grown, 0, elements.length);
        for (int i = elements.length; i < newLength; i++) {
            grown[i] = new Value();
        }
        this.elements = grown;
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
        ensureCapacity(topIndex + 1);
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
        ensureCapacity(newTopIndex);
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
    /** 当前容量（会随扩容变化）。 */
    public int getSize() { return elements.length; }
    /** 容量上限。 */
    public int getMaxSize() { return maxSize; }

    // ---- 工具方法 ----

    private void checkBounds(int absIndex) {
        if (absIndex < 0 || absIndex >= elements.length) {
            throw new VmException("栈索引越界: absIndex=" + absIndex
                    + ", frameIndex=" + frameIndex + ", size=" + elements.length);
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
