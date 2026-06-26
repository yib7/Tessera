package tessera.model;

/**
 * One persisted high score: who set it, on which board size, and the run's
 * score, turn count, and time. Stored as a single tab-delimited line so the
 * file stays human-readable and trivially parseable.
 */
public record ScoreEntry(String name, BoardSize size, int score, int turns, long timeMillis) {

    private static final String DELIM = "\t";

    public ScoreEntry {
        if (name == null || name.isBlank()) {
            name = "Anonymous";
        }
        // Names are single-line; strip anything that would corrupt the record.
        name = name.replace("\t", " ").replace("\n", " ").replace("\r", " ").trim();
        if (name.length() > 24) {
            name = name.substring(0, 24);
        }
    }

    /** Serialise to one tab-delimited line. */
    public String toLine() {
        return String.join(DELIM,
                size.label(),
                name,
                Integer.toString(score),
                Integer.toString(turns),
                Long.toString(timeMillis));
    }

    /**
     * Parse one line, or return null if it is blank, a comment, or malformed.
     * Returning null rather than throwing lets the loader skip bad lines and
     * keep the good ones.
     */
    public static ScoreEntry fromLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        String[] parts = trimmed.split(DELIM);
        if (parts.length != 5) {
            return null;
        }
        try {
            BoardSize size = BoardSize.fromLabel(parts[0]);
            String name = parts[1];
            int score = Integer.parseInt(parts[2].trim());
            int turns = Integer.parseInt(parts[3].trim());
            long time = Long.parseLong(parts[4].trim());
            return new ScoreEntry(name, size, score, turns, time);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Formats the time as m:ss for display. */
    public String formattedTime() {
        long totalSeconds = timeMillis / 1000;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
