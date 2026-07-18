package tessera.model;

/**
 * A named board geometry. Tessera ships three sizes, but the class is just a
 * rows-by-columns pair with a label, so new sizes can be added without touching
 * the rest of the model. The cell count is always even, which the deal logic
 * relies on to form complete pairs.
 */
public enum BoardSize {

    // MAINTAINERS: every size must have an even rows*cols — the constructor's
    // parity guard throws otherwise, and because it runs inside the enum
    // constructor the failure surfaces only the first time BoardSize is loaded
    // (an ExceptionInInitializerError with a deep stack), not at compile time.
    EASY("Easy", 3, 4),
    NORMAL("Normal", 4, 7),
    HARD("Hard", 7, 8);

    private final String label;
    private final int rows;
    private final int cols;

    BoardSize(String label, int rows, int cols) {
        if (((long) rows * cols) % 2 != 0) {
            throw new IllegalArgumentException(
                    "Board " + label + " has an odd cell count; pairs cannot be formed.");
        }
        this.label = label;
        this.rows = rows;
        this.cols = cols;
    }

    public String label() {
        return label;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public int cellCount() {
        return rows * cols;
    }

    public int pairCount() {
        return cellCount() / 2;
    }

    /**
     * Resolve a saved label back to a size, or null if none matches
     * (case-insensitive, trimmed). Callers that need to reject an unknown label
     * use this; callers that want a safe default use {@link #fromLabel}.
     */
    public static BoardSize tryFromLabel(String label) {
        if (label != null) {
            for (BoardSize size : values()) {
                if (size.label.equalsIgnoreCase(label.trim())) {
                    return size;
                }
            }
        }
        return null;
    }

    /** Resolve a saved label back to a size, falling back to NORMAL. */
    public static BoardSize fromLabel(String label) {
        BoardSize size = tryFromLabel(label);
        return size != null ? size : NORMAL;
    }
}
