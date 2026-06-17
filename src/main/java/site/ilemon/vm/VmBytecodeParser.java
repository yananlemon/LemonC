package site.ilemon.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LemonVM 字节码解析器 — 将 .lbc 文本格式解析为 Script 对象。
 *
 * 支持的格式：
 * <pre>
 * .version 1
 * .class Test
 *
 * .func main 0 2 void
 *     Mov -1, 1
 *     Add -1, 2
 *     Print -1
 *     PrintNL
 *     Ret
 * .end
 * </pre>
 */
public class VmBytecodeParser {

    /**
     * 从文本内容解析出 Script 对象。
     */
    public static Script parse(String lbcContent) {
        Script script = new Script();
        String[] lines = lbcContent.split("\\r?\\n");

        // 第一遍：收集所有函数和指令，解析标签
        List<VmFunction> functions = new ArrayList<VmFunction>();
        List<Instruction> allInstructions = new ArrayList<Instruction>();
        Map<String, Integer> labelMap = new HashMap<String, Integer>(); // 标签名 -> 指令索引

        int currentInstrIndex = 0;
        boolean inFunc = false;
        String funcName = null;
        int funcParamCount = 0;
        int funcLocalCount = 0;
        int funcEntryPoint = 0;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            }

            // 元指令
            if (line.startsWith(".version") || line.startsWith(".class")) {
                continue;
            }

            // 函数开始
            if (line.startsWith(".func ")) {
                inFunc = true;
                String[] parts = line.split("\\s+");
                // .func <name> <paramCount> <localCount> <returnType>
                funcName = parts[1];
                funcParamCount = Integer.parseInt(parts[2]);
                funcLocalCount = Integer.parseInt(parts[3]);
                funcEntryPoint = currentInstrIndex;
                continue;
            }

            // 函数结束
            if (line.equals(".end")) {
                if (inFunc && funcName != null) {
                    int funcIndex = functions.size();
                    functions.add(new VmFunction(funcName, funcEntryPoint, funcParamCount, funcLocalCount));
                }
                inFunc = false;
                funcName = null;
                continue;
            }

            if (!inFunc) {
                continue;
            }

            // 标签定义（以冒号结尾，如 _loop_cond:）
            if (line.endsWith(":")) {
                String labelName = line.substring(0, line.length() - 1).trim();
                labelMap.put(labelName, currentInstrIndex);
                continue;
            }

            // 普通指令
            currentInstrIndex++;
        }

        // 第二遍：生成指令对象
        currentInstrIndex = 0;
        inFunc = false;
        // 构建函数名到索引的映射
        Map<String, Integer> funcNameToIndex = new HashMap<String, Integer>();
        for (int i = 0; i < functions.size(); i++) {
            funcNameToIndex.put(functions.get(i).getName(), i);
        }

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            if (line.isEmpty() || line.startsWith(";")) continue;
            if (line.startsWith(".version") || line.startsWith(".class")) continue;
            if (line.startsWith(".func ")) { inFunc = true; continue; }
            if (line.equals(".end")) { inFunc = false; continue; }
            if (!inFunc) continue;
            if (line.endsWith(":")) continue; // 标签

            // 解析指令
            Instruction instr = parseInstruction(line, labelMap, funcNameToIndex);
            allInstructions.add(instr);
            currentInstrIndex++;
        }

        // 组装 Script
        script.setInstrStream(allInstructions.toArray(new Instruction[0]));
        script.setFuncTable(functions.toArray(new VmFunction[0]));

        // 查找 main 函数
        if (funcNameToIndex.containsKey("main")) {
            script.setMainFuncName("main");
        }

        return script;
    }

    /**
     * 解析一行指令文本。
     */
    private static Instruction parseInstruction(String line,
                                                 Map<String, Integer> labelMap,
                                                 Map<String, Integer> funcNameToIndex) {
        // 去掉行内注释
        int commentIdx = line.indexOf(';');
        if (commentIdx >= 0) {
            line = line.substring(0, commentIdx).trim();
        }

        // 分割助记符和操作数
        String mnemonic;
        String operandStr = "";
        int firstSpace = line.indexOf(' ');
        if (firstSpace < 0) {
            mnemonic = line;
        } else {
            mnemonic = line.substring(0, firstSpace);
            operandStr = line.substring(firstSpace + 1).trim();
        }

        Opcode opcode = Opcode.fromMnemonic(mnemonic);

        List<Value> operands = new ArrayList<Value>();
        if (!operandStr.isEmpty()) {
            String[] parts = splitOperands(operandStr);
            for (String part : parts) {
                operands.add(parseOperand(part.trim(), labelMap, funcNameToIndex));
            }
        }

        return new Instruction(opcode, operands);
    }

    /**
     * 按逗号分割操作数，但要考虑字符串中可能包含逗号。
     */
    private static String[] splitOperands(String operandStr) {
        List<String> result = new ArrayList<String>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < operandStr.length(); i++) {
            char c = operandStr.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
            } else if (c == ',' && !inQuote) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * 解析单个操作数。
     *
     * 规则：
     * - 纯数字 / 负数 → 栈索引（isStackRef=true）
     * - 带有 # 前缀的数字（如 #42, #3.14）→ 字面量
     * - 以 _ 开头的标识符 → 标签引用（跳转目标）或 _RetVal
     * - 以 _Func 或小写字母开头 → 函数引用
     * - 双引号包围 → 字符串字面量
     * - true/false → 布尔字面量
     */
    private static Value parseOperand(String token,
                                       Map<String, Integer> labelMap,
                                       Map<String, Integer> funcNameToIndex) {
        // _RetVal 寄存器
        if (token.equals("_RetVal")) {
            return Value.ofRetValRef();
        }

        // 字符串字面量
        if (token.startsWith("\"") && token.endsWith("\"")) {
            String str = token.substring(1, token.length() - 1);
            // 处理转义
            str = str.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
            return Value.ofString(str);
        }

        // 布尔字面量
        if (token.equals("true")) return Value.ofBool(true);
        if (token.equals("false")) return Value.ofBool(false);

        // 标签引用（跳转目标）— 以 _ 开头但不是 _RetVal
        if (token.startsWith("_") && labelMap.containsKey(token)) {
            int instrIndex = labelMap.get(token);
            return Value.ofInstrIndex(instrIndex);
        }

        // 函数引用 — 以 _ 开头并在函数表中找到
        if (token.startsWith("_") && funcNameToIndex.containsKey(token.substring(1))) {
            int funcIndex = funcNameToIndex.get(token.substring(1));
            return Value.ofFuncIndex(funcIndex);
        }

        // 直接函数名引用
        if (funcNameToIndex.containsKey(token)) {
            int funcIndex = funcNameToIndex.get(token);
            return Value.ofFuncIndex(funcIndex);
        }

        // 数字字面量 — 以 # 开头表示立即数
        if (token.startsWith("#")) {
            String numStr = token.substring(1);
            return parseLiteral(numStr);
        }

        // 纯数字（无 # 前缀）→ 栈索引
        try {
            int index = Integer.parseInt(token);
            return Value.ofStackRef(index);
        } catch (NumberFormatException e) {
            // 不是整数
        }

        // 浮点数立即数（包含小数点）
        if (token.contains(".")) {
            return parseLiteral(token);
        }

        throw new VmException("无法解析操作数: " + token);
    }

    /**
     * 解析数字字面量（区分 int / float / double）。
     */
    private static Value parseLiteral(String numStr) {
        try {
            // 如果以 f 或 F 结尾，解析为 float
            if (numStr.endsWith("f") || numStr.endsWith("F")) {
                return Value.ofFloat(Float.parseFloat(numStr.substring(0, numStr.length() - 1)));
            }
            // 如果包含小数点，解析为 double
            if (numStr.contains(".")) {
                return Value.ofDouble(Double.parseDouble(numStr));
            }
            // 否则解析为 int
            return Value.ofInt(Integer.parseInt(numStr));
        } catch (NumberFormatException e) {
            throw new VmException("无法解析数字字面量: " + numStr);
        }
    }
}
