package tessera.model;

/**
 * One persisted high score: who set it, on which board size, and the run's
 * score, turn count, and time. An optional {@link TileTheme} records which tile
 * theme the run used, so the leaderboard can be filtered by theme; it is null
 * for legacy entries saved before themes were recorded.
 *
 * <p>Stored as a single tab-delimited line so the file stays human-readable and
 * trivially parseable. For backward compatibility the theme is an optional 6th
 * field: a theme-less entry serialises to the original 5 columns, and both the
 * 5- and 6-column shapes parse.
 */
public record ScoreEntry(String name, BoardSize size, TileTheme theme, int score, int turns,
        long timeMillis) {

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

    /**
     * Theme-less constructor (backward compatible). Kept so existing call sites
     * and legacy data that predate per-theme tracking keep working; theme is null.
     */
    public ScoreEntry(String name, BoardSize size, int score, int turns, long timeMillis) {
        this(name, size, null, score, turns, timeMillis);
    }

    /**
     * Serialise to one tab-delimited line. Writes the original 5 columns when
     * there is no theme, or 6 columns (theme appended) when one is recorded, so
     * old files stay byte-identical and readers of either shape work.
     */
    public String toLine() {
        String base = String.join(DELIM,
                size.label(),
                name,
                Integer.toString(score),
                Integer.toString(turns),
                Long.toString(timeMillis));
        return theme == null ? base : base + DELIM + theme.name();
    }

    /**
     * Parse one line, or return null if it is blank, a comment, or malformed.
     * Accepts both the 5-column (theme-less) and 6-column (theme) shapes.
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
        if (parts.length != 5 && parts.length != 6) {
            return null;
        }
        try {
            BoardSize size = BoardSize.tryFromLabel(parts[0]);
            if (size == null) {
                return null;
            }
            String name = parts[1];
            int score = Integer.parseInt(parts[2].trim());
            int turns = Integer.parseInt(parts[3].trim());
            long time = Long.parseLong(parts[4].trim());
            if (score < 0 || turns < 0 || time < 0) {
                return null;
            }
            // 6th column is the theme; an unknown value degrades to null rather
            // than rejecting the whole (otherwise valid) entry.
            TileTheme theme = parts.length == 6 ? TileTheme.tryFromName(parts[5]) : null;
            return new ScoreEntry(name, size, theme, score, turns, time);
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
