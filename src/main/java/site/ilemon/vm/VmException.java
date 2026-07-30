package site.ilemon.vm;

/**
 * LemonVM 运行时异常。
 * 包括栈溢出、除零、数组越界、无效操作码等错误。
 *
 * <p>抛出点通常只知道"发生了什么"，不知道"在哪里"。执行循环会在异常冒泡经过时
 * 补上位置（PC、指令、源码行列），因此这里保留一个只允许写一次的 location 字段。</p>
 */
public class VmException extends RuntimeException {

    private String location;

    public VmException(String message) {
        super(message);
    }

    public VmException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 是否已经带上了位置信息。 */
    public boolean hasLocation() {
        return location != null;
    }

    /** 补上位置信息；只有第一次调用生效，避免异常向外冒泡时被重复标注。 */
    public void locate(String location) {
        if (this.location == null) {
            this.location = location;
        }
    }

    public String getLocation() {
        return location;
    }

    /** 用于向用户展示的单行描述：位置在前，原因在后。 */
    public String getDiagnostic() {
        if (location == null) {
            return getMessage();
        }
        return location + ": " + getMessage();
    }
}
