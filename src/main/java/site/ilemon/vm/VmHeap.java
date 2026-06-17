package site.ilemon.vm;

import java.util.HashMap;
import java.util.Map;

/**
 * LemonVM 堆管理器。
 *
 * 管理所有堆上的数组对象。每个数组通过唯一的 ref（整数 ID）访问。
 * XVM 没有堆（用栈相对索引模拟数组），LemonVM 新增此组件以支持 LemonC 的堆数组。
 */
public class VmHeap {

    /** 数组存储：ref → VmArray */
    private final Map<Integer, VmArray> arrays;

    /** 下一个可用的 ref */
    private int nextRef;

    public VmHeap() {
        this.arrays = new HashMap<Integer, VmArray>();
        this.nextRef = 0;
    }

    /**
     * 在堆上分配一个新数组。
     *
     * @param size 数组长度
     * @return 数组的引用 ID
     */
    public int allocate(int size) {
        return allocate(size, Value.Type.NULL);
    }

    public int allocate(int size, Value.Type elementType) {
        if (size < 0) {
            throw new VmException("非法数组大小: " + size);
        }
        int ref = nextRef++;
        arrays.put(ref, new VmArray(size, elementType));
        return ref;
    }

    /**
     * 读取数组元素。
     *
     * @param ref   数组引用 ID
     * @param index 元素索引
     * @return 元素值
     */
    public Value load(int ref, int index) {
        return getArray(ref).get(index);
    }

    /**
     * 写入数组元素。
     *
     * @param ref   数组引用 ID
     * @param index 元素索引
     * @param val   要写入的值
     */
    public void store(int ref, int index, Value val) {
        getArray(ref).set(index, val);
    }

    /**
     * 获取数组长度。
     *
     * @param ref 数组引用 ID
     * @return 数组长度
     */
    public int length(int ref) {
        return getArray(ref).getLength();
    }

    private VmArray getArray(int ref) {
        VmArray arr = arrays.get(ref);
        if (arr == null) {
            throw new VmException("无效的数组引用: " + ref);
        }
        return arr;
    }
}
