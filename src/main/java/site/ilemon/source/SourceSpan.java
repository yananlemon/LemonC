package site.ilemon.source;

import java.util.Objects;

/**
 * Immutable, end-exclusive source range.
 *
 * <p>A span from {@code 1:2} to {@code 1:5} covers columns 2, 3 and 4.
 * Unknown spans use zero for every coordinate.</p>
 */
public final class SourceSpan {
    public static final SourceSpan UNKNOWN = new SourceSpan(0, 0, 0, 0, false);

    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;

    private SourceSpan(int startLine, int startColumn, int endLine, int endColumn,
                       boolean validate) {
        if (validate) {
            validate(startLine, startColumn, endLine, endColumn);
        }
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public static SourceSpan of(int startLine, int startColumn, int endLine, int endColumn) {
        return new SourceSpan(startLine, startColumn, endLine, endColumn, true);
    }

    public static SourceSpan point(int line, int column) {
        return of(line, column, line, column);
    }

    /** Compatibility location for nodes constructed with only a historical line number. */
    public static SourceSpan line(int line) {
        return line > 0 ? point(line, 1) : UNKNOWN;
    }

    public static SourceSpan covering(SourceSpan first, SourceSpan second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (!first.isKnown()) {
            return second;
        }
        if (!second.isKnown()) {
            return first;
        }
        int startComparison = compare(first.startLine, first.startColumn,
                second.startLine, second.startColumn);
        int endComparison = compare(first.endLine, first.endColumn,
                second.endLine, second.endColumn);
        return of(
                startComparison <= 0 ? first.startLine : second.startLine,
                startComparison <= 0 ? first.startColumn : second.startColumn,
                endComparison >= 0 ? first.endLine : second.endLine,
                endComparison >= 0 ? first.endColumn : second.endColumn);
    }

    public boolean isKnown() {
        return startLine > 0;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    private static void validate(int startLine, int startColumn, int endLine, int endColumn) {
        if (startLine < 1 || startColumn < 1 || endLine < 1 || endColumn < 1) {
            throw new IllegalArgumentException("source coordinates must be positive");
        }
        if (compare(startLine, startColumn, endLine, endColumn) > 0) {
            throw new IllegalArgumentException("source span end precedes start");
        }
    }

    private static int compare(int leftLine, int leftColumn, int rightLine, int rightColumn) {
        if (leftLine != rightLine) {
            return leftLine < rightLine ? -1 : 1;
        }
        return Integer.compare(leftColumn, rightColumn);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SourceSpan)) {
            return false;
        }
        SourceSpan that = (SourceSpan) other;
        return startLine == that.startLine
                && startColumn == that.startColumn
                && endLine == that.endLine
                && endColumn == that.endColumn;
    }

    @Override
    public int hashCode() {
        int result = startLine;
        result = 31 * result + startColumn;
        result = 31 * result + endLine;
        result = 31 * result + endColumn;
        return result;
    }

    @Override
    public String toString() {
        if (!isKnown()) {
            return "<unknown>";
        }
        return startLine + ":" + startColumn + "-" + endLine + ":" + endColumn;
    }
}
