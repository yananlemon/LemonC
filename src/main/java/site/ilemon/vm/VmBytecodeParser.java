package site.ilemon.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parses LemonVM textual bytecode into an executable script. */
public class VmBytecodeParser {

    public static Script parse(String lbcContent) {
        if (lbcContent == null) {
            throw new VmException("bytecode content is null");
        }

        String[] lines = lbcContent.split("\\r?\\n");
        List<VmFunction> functions = new ArrayList<VmFunction>();
        Map<String, Map<String, Integer>> labelsByFunction =
                new HashMap<String, Map<String, Integer>>();
        Set<String> functionNames = new HashSet<String>();

        int instructionIndex = 0;
        boolean inFunction = false;
        String functionName = null;
        int parameterCount = 0;
        int localCount = 0;
        int entryPoint = 0;
        Map<String, Integer> currentLabels = null;

        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String line = stripInlineComment(lines[lineNumber]).trim();
            if (line.length() == 0) {
                continue;
            }
            if (isMetadata(line)) {
                continue;
            }
            if (line.startsWith(".func")) {
                if (inFunction) {
                    throw parseError(lineNumber, "nested .func declaration");
                }
                String[] parts = line.split("\\s+");
                if (parts.length != 5 || !".func".equals(parts[0])) {
                    throw parseError(lineNumber,
                            "expected .func <name> <paramCount> <localCount> <returnType>");
                }
                functionName = parts[1];
                if (functionName.length() == 0 || !functionNames.add(functionName)) {
                    throw parseError(lineNumber, "duplicate or empty function name: " + functionName);
                }
                parameterCount = parseNonNegative(parts[2], lineNumber, "parameter count");
                localCount = parseNonNegative(parts[3], lineNumber, "local count");
                entryPoint = instructionIndex;
                currentLabels = new HashMap<String, Integer>();
                labelsByFunction.put(functionName, currentLabels);
                inFunction = true;
                continue;
            }
            if (".end".equals(line)) {
                if (!inFunction || functionName == null) {
                    throw parseError(lineNumber, ".end without matching .func");
                }
                functions.add(new VmFunction(functionName, entryPoint, parameterCount, localCount));
                inFunction = false;
                functionName = null;
                currentLabels = null;
                continue;
            }
            if (!inFunction) {
                throw parseError(lineNumber, "instruction or label outside a function");
            }
            if (line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                if (label.length() == 0 || currentLabels.containsKey(label)) {
                    throw parseError(lineNumber,
                            "duplicate or empty label in function " + functionName + ": " + label);
                }
                currentLabels.put(label, instructionIndex);
                continue;
            }
            instructionIndex++;
        }

        if (inFunction) {
            throw parseError(Math.max(0, lines.length - 1), "unterminated function: " + functionName);
        }
        if (functions.isEmpty()) {
            throw new VmException("bytecode contains no functions");
        }

        Map<String, Integer> functionIndexes = new HashMap<String, Integer>();
        for (int i = 0; i < functions.size(); i++) {
            functionIndexes.put(functions.get(i).getName(), i);
        }
        if (!functionIndexes.containsKey("main")) {
            throw new VmException("bytecode has no main function");
        }

        List<Instruction> instructions = new ArrayList<Instruction>();
        inFunction = false;
        functionName = null;
        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String line = stripInlineComment(lines[lineNumber]).trim();
            if (line.length() == 0 || isMetadata(line)) {
                continue;
            }
            if (line.startsWith(".func")) {
                inFunction = true;
                functionName = line.split("\\s+")[1];
                continue;
            }
            if (".end".equals(line)) {
                inFunction = false;
                functionName = null;
                continue;
            }
            if (!inFunction || line.endsWith(":")) {
                continue;
            }
            instructions.add(parseInstruction(
                    line, labelsByFunction.get(functionName), functionIndexes, lineNumber));
        }

        Script script = new Script();
        script.setInstrStream(instructions.toArray(new Instruction[0]));
        script.setFuncTable(functions.toArray(new VmFunction[0]));
        script.setMainFuncName("main");
        return script;
    }

    private static Instruction parseInstruction(String line,
                                                Map<String, Integer> labels,
                                                Map<String, Integer> functionIndexes,
                                                int lineNumber) {
        String mnemonic;
        String operandsText = "";
        int firstSpace = firstWhitespace(line);
        if (firstSpace < 0) {
            mnemonic = line;
        } else {
            mnemonic = line.substring(0, firstSpace);
            operandsText = line.substring(firstSpace + 1).trim();
        }

        Opcode opcode;
        try {
            opcode = Opcode.fromMnemonic(mnemonic);
        } catch (VmException e) {
            throw parseError(lineNumber, e.getMessage());
        }

        List<Value> operands = new ArrayList<Value>();
        if (operandsText.length() > 0) {
            for (String operand : splitOperands(operandsText, lineNumber)) {
                operands.add(parseOperand(operand.trim(), labels, functionIndexes, lineNumber));
            }
        }
        if (operands.size() != opcode.getOperandCount()) {
            throw parseError(lineNumber, opcode.getMnemonic() + " expects "
                    + opcode.getOperandCount() + " operands, got " + operands.size());
        }
        return new Instruction(opcode, operands);
    }

    private static String[] splitOperands(String text, int lineNumber) {
        List<String> result = new ArrayList<String>();
        boolean inQuote = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
            } else if (ch == '\\' && inQuote) {
                current.append(ch);
                escaped = true;
            } else if (ch == '"') {
                current.append(ch);
                inQuote = !inQuote;
            } else if (ch == ',' && !inQuote) {
                addOperand(result, current, lineNumber);
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (inQuote || escaped) {
            throw parseError(lineNumber, "unterminated string operand");
        }
        addOperand(result, current, lineNumber);
        return result.toArray(new String[0]);
    }

    private static void addOperand(List<String> operands, StringBuilder current, int lineNumber) {
        String value = current.toString().trim();
        if (value.length() == 0) {
            throw parseError(lineNumber, "empty operand");
        }
        operands.add(value);
    }

    private static Value parseOperand(String token,
                                      Map<String, Integer> labels,
                                      Map<String, Integer> functionIndexes,
                                      int lineNumber) {
        if ("_RetVal".equals(token)) {
            return Value.ofRetValRef();
        }
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
            return Value.ofString(unescape(token.substring(1, token.length() - 1), lineNumber));
        }
        if ("true".equals(token)) {
            return Value.ofBool(true);
        }
        if ("false".equals(token)) {
            return Value.ofBool(false);
        }
        if (labels != null && labels.containsKey(token)) {
            return Value.ofInstrIndex(labels.get(token));
        }
        if (token.startsWith("_") && functionIndexes.containsKey(token.substring(1))) {
            return Value.ofFuncIndex(functionIndexes.get(token.substring(1)));
        }
        if (functionIndexes.containsKey(token)) {
            return Value.ofFuncIndex(functionIndexes.get(token));
        }
        if (token.startsWith("#")) {
            return parseLiteral(token.substring(1), lineNumber);
        }
        try {
            return Value.ofStackRef(Integer.parseInt(token));
        } catch (NumberFormatException ignored) {
            // Continue with floating-point literal parsing.
        }
        if (looksFloatingPoint(token)) {
            return parseLiteral(token, lineNumber);
        }
        throw parseError(lineNumber, "cannot parse operand: " + token);
    }

    private static Value parseLiteral(String text, int lineNumber) {
        try {
            if (text.endsWith("f") || text.endsWith("F")) {
                return Value.ofFloat(Float.parseFloat(text.substring(0, text.length() - 1)));
            }
            if (text.endsWith("d") || text.endsWith("D")) {
                return Value.ofDouble(Double.parseDouble(text.substring(0, text.length() - 1)));
            }
            if (looksFloatingPoint(text)) {
                return Value.ofDouble(Double.parseDouble(text));
            }
            return Value.ofInt(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            throw parseError(lineNumber, "invalid numeric literal: " + text);
        }
    }

    private static String unescape(String text, int lineNumber) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != '\\') {
                result.append(ch);
                continue;
            }
            if (++i >= text.length()) {
                throw parseError(lineNumber, "dangling escape in string operand");
            }
            char escaped = text.charAt(i);
            if (escaped == 'n') result.append('\n');
            else if (escaped == 't') result.append('\t');
            else if (escaped == 'r') result.append('\r');
            else if (escaped == '"') result.append('"');
            else if (escaped == '\\') result.append('\\');
            else throw parseError(lineNumber, "unsupported string escape: \\" + escaped);
        }
        return result.toString();
    }

    private static String stripInlineComment(String line) {
        boolean inQuote = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (ch == '\\' && inQuote) {
                escaped = true;
            } else if (ch == '"') {
                inQuote = !inQuote;
            } else if (ch == ';' && !inQuote) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static boolean looksFloatingPoint(String text) {
        return text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0
                || text.endsWith("f") || text.endsWith("F")
                || text.endsWith("d") || text.endsWith("D");
    }

    private static boolean isMetadata(String line) {
        return line.startsWith(".version") || line.startsWith(".class");
    }

    private static int firstWhitespace(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int parseNonNegative(String text, int lineNumber, String role) {
        try {
            int value = Integer.parseInt(text);
            if (value < 0) {
                throw parseError(lineNumber, role + " must be non-negative: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw parseError(lineNumber, "invalid " + role + ": " + text);
        }
    }

    private static VmException parseError(int zeroBasedLine, String message) {
        return new VmException("line " + (zeroBasedLine + 1) + ": " + message);
    }
}
