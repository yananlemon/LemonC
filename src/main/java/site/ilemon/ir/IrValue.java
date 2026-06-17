package site.ilemon.ir;

/**
 * LemonIR 操作数值。
 * 代表一个虚拟寄存器（VReg）或一个字面量常量。
 */
public class IrValue {

    public enum Kind {
        VREG,
        CONST_INT,
        CONST_FLOAT,
        CONST_DOUBLE,
        CONST_BOOL,
        CONST_STRING
    }

    private Kind kind;
    private IrType type;

    // 对于 VREG，id 是虚拟寄存器编号（0, 1, 2...）
    private int id;

    // 对于 CONST_INT
    private int intValue;
    // 对于 CONST_FLOAT
    private float floatValue;
    // 对于 CONST_DOUBLE
    private double doubleValue;
    // 对于 CONST_BOOL
    private boolean boolValue;
    // 对于 CONST_STRING
    private String stringValue;

    private IrValue(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() { return kind; }
    public IrType getType() { return type; }
    public void setType(IrType type) { this.type = type; }
    public int getId() { return id; }
    public int getIntValue() { return intValue; }
    public float getFloatValue() { return floatValue; }
    public double getDoubleValue() { return doubleValue; }
    public boolean getBoolValue() { return boolValue; }
    public String getStringValue() { return stringValue; }

    // ---- 工厂方法 ----

    public static IrValue vreg(int id) {
        return vreg(id, null);
    }

    public static IrValue vreg(int id, IrType type) {
        IrValue v = new IrValue(Kind.VREG);
        v.id = id;
        v.type = type;
        return v;
    }

    public static IrValue constInt(int val) {
        IrValue v = new IrValue(Kind.CONST_INT);
        v.type = IrType.INT;
        v.intValue = val;
        return v;
    }

    public static IrValue constFloat(float val) {
        IrValue v = new IrValue(Kind.CONST_FLOAT);
        v.type = IrType.FLOAT;
        v.floatValue = val;
        return v;
    }

    public static IrValue constDouble(double val) {
        IrValue v = new IrValue(Kind.CONST_DOUBLE);
        v.type = IrType.DOUBLE;
        v.doubleValue = val;
        return v;
    }

    public static IrValue constBool(boolean val) {
        IrValue v = new IrValue(Kind.CONST_BOOL);
        v.type = IrType.BOOL;
        v.boolValue = val;
        return v;
    }

    public static IrValue constString(String val) {
        IrValue v = new IrValue(Kind.CONST_STRING);
        v.type = IrType.STRING;
        v.stringValue = val;
        return v;
    }

    @Override
    public String toString() {
        switch (kind) {
            case VREG: return "v" + id;
            case CONST_INT: return String.valueOf(intValue);
            case CONST_FLOAT: return floatValue + "f";
            case CONST_DOUBLE: return String.valueOf(doubleValue);
            case CONST_BOOL: return String.valueOf(boolValue);
            case CONST_STRING: return "\"" + stringValue.replace("\n", "\\n") + "\"";
            default: return "?";
        }
    }
}
