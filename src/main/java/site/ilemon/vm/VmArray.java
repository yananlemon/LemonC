package site.ilemon.vm;

/**
 * LemonVM 堆上的数组对象。
 *
 * 数组元素也是 Value，自带类型标签。
 * 不需要区分 int[] 和 float[] 的数组类型——ArrGet/ArrSet 直接搬运 Value。
 */
public class VmArray {

    /** 数组元素 */
    private final Value[] elements;

    /** 数组长度 */
    private final int length;

    public VmArray(int length) {
        this(length, Value.Type.NULL);
    }

    public VmArray(int length, Value.Type elementType) {
        this.length = length;
        this.elements = new Value[length];
        // 初始化为 Lemon/JVM 一致的数组默认值。
        for (int i = 0; i < length; i++) {
            elements[i] = defaultValue(elementType);
        }
    }

    private Value defaultValue(Value.Type elementType) {
        switch (elementType) {
            case INT: return Value.ofInt(0);
            case FLOAT: return Value.ofFloat(0.0f);
            case DOUBLE: return Value.ofDouble(0.0d);
            case BOOL: return Value.ofBool(false);
            case STRING: return Value.ofString("");
            default: return new Value();
        }
    }

    public int getLength() {
        return length;
    }

    /**
     * 获取指定索引的元素。
     * @throws VmException 如果索引越界
     */
    public Value get(int index) {
        checkBounds(index);
        return elements[index];
    }

    /**
     * 设置指定索引的元素。
     * @throws VmException 如果索引越界
     */
    public void set(int index, Value val) {
        checkBounds(index);
        elements[index].copyFrom(val);
    }

    private void checkBounds(int index) {
        if (index < 0 || index >= length) {
            throw new VmException("数组索引越界: index=" + index + ", length=" + length);
        }
    }
}
