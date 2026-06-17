package site.ilemon.ir;

/**
 * LemonIR 类型枚举。
 */
public enum IrType {
    INT,
    FLOAT,
    DOUBLE,
    BOOL,
    STRING,
    INT_ARRAY,
    FLOAT_ARRAY,
    DOUBLE_ARRAY,
    BOOL_ARRAY,
    VOID;

    public boolean isNumeric() {
        return this == INT || this == FLOAT || this == DOUBLE;
    }

    public boolean isArray() {
        return this == INT_ARRAY || this == FLOAT_ARRAY
                || this == DOUBLE_ARRAY || this == BOOL_ARRAY;
    }

    public IrType elementType() {
        switch (this) {
            case INT_ARRAY:
                return INT;
            case FLOAT_ARRAY:
                return FLOAT;
            case DOUBLE_ARRAY:
                return DOUBLE;
            case BOOL_ARRAY:
                return BOOL;
            default:
                return this;
        }
    }
}
