package site.ilemon.ir;

/**
 * 打印 LemonIR，方便调试。
 */
public class IrPrinter {

    public static void printProgram(IrProgram program) {
        for (IrFunction func : program.getFunctions()) {
            System.out.println("func " + func.getName() + " {");
            for (IrBlock block : func.getBlocks()) {
                System.out.println(block.getLabel() + ":");
                for (IrInstruction instr : block.getInstructions()) {
                    System.out.println("    " + instr.toString());
                }
            }
            System.out.println("}");
            System.out.println();
        }
    }
}
