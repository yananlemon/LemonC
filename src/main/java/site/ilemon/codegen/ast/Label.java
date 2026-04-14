package site.ilemon.codegen.ast;

public class Label {
    private final int i;
    private static int count = 0;

    public Label()
    {
        i = count++;
    }

    /**
     * 重置 label 计数器。
     * 每次新的编译任务开始前必须调用，避免同一 JVM 进程内多次编译时 label 编号冲突。
     */
    public static void resetCounter() {
        count = 0;
    }

    @Override
    public String toString()
    {
        return "Label_" + i;
    }
}