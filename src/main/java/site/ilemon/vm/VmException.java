package site.ilemon.vm;

/**
 * LemonVM 运行时异常。
 * 包括栈溢出、除零、数组越界、无效操作码等错误。
 */
public class VmException extends RuntimeException {

    public VmException(String message) {
        super(message);
    }

    public VmException(String message, Throwable cause) {
        super(message, cause);
    }
}
