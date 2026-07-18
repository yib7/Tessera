package tessera.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Persistent high scores, kept as the top {@value #MAX_PER_SIZE} runs per board
 * size. Backed by a tab-delimited file under the Tessera data directory.
 *
 * <p>The old leaderboard was a fixed three-line file holding one name per
 * difficulty, parsed by hand with positional {@code split} calls that threw on
 * any deviation. This version tolerates a missing file (starts empty), skips
 * malformed lines instead of failing, ranks by score with turns then time as
 * tie-breakers, and supports a real top-N table per size.
 *
 * <p><b>Threading:</b> this class is not synchronised. It is only ever touched
 * from the Swing event dispatch thread (submits from the results screen, reads
 * from the leaderboard panel), so {@link #load()}'s {@code clear()}-then-refill
 * of {@link #entries} is safe. Calling any method off the EDT would race on that
 * list and is not supported; add synchronisation before doing so.
 */
public final class Leaderboard {

    /** How many top runs per size are the "celebrated" board (name entry, results screen). */
    public static final int MAX_PER_SIZE = 5;
    /** How many runs per size are retained for the browsable history (the "All" view). */
    public static final int HISTORY_PER_SIZE = 25;
    private static final String FILE_NAME = "leaderboard.tsv";

    // Best score first; fewer turns, then faster time break ties.
    private static final Comparator<ScoreEntry> RANK =
            Comparator.comparingInt(ScoreEntry::score).reversed()
                    .thenComparingInt(ScoreEntry::turns)
                    .thenComparingLong(ScoreEntry::timeMillis);

    private final List<ScoreEntry> entries = new ArrayList<>();
    private final Path file;

    public Leaderboard() {
        this(DataPaths.dataDir().resolve(FILE_NAME));
    }

    /** Constructor used by tests to point at a temporary file. */
    public Leaderboard(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        entries.clear();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                ScoreEntry entry = ScoreEntry.fromLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            // Unreadable file: behave as if there were no scores yet.
            entries.clear();
        }
    }

    /** The top {@value #MAX_PER_SIZE} entries for one board size, best first. */
    public List<ScoreEntry> topFor(BoardSize size) {
        return rankedFor(size, MAX_PER_SIZE);
    }

    /**
     * The browsable history for one board size — up to {@value #HISTORY_PER_SIZE}
     * runs, best first — so a run that just misses the celebrated top-5 still has
     * a record instead of vanishing.
     */
    public List<ScoreEntry> historyFor(BoardSize size) {
        return rankedFor(size, HISTORY_PER_SIZE);
    }

    private List<ScoreEntry> rankedFor(BoardSize size, int limit) {
        List<ScoreEntry> filtered = new ArrayList<>();
        for (ScoreEntry entry : entries) {
            if (entry.size() == size) {
                filtered.add(entry);
            }
        }
        filtered.sort(RANK);
        if (filtered.size() > limit) {
            return new ArrayList<>(filtered.subList(0, limit));
        }
        return filtered;
    }

    /** Remove every entry for one board size and persist. */
    public void clear(BoardSize size) {
        entries.removeIf(entry -> entry.size() == size);
        save();
    }

    /**
     * True if the given candidate would land on the board for its size. Uses the
     * same {@link #RANK} ordering as ranking, so a run tied on score but with
     * fewer turns or a faster time than the current last-place entry correctly
     * qualifies.
     */
    public boolean qualifies(ScoreEntry candidate) {
        List<ScoreEntry> top = topFor(candidate.size());
        return top.size() < MAX_PER_SIZE
                || RANK.compare(candidate, top.get(top.size() - 1)) < 0;
    }

    /**
     * Add an entry, trim each size to the top {@value #MAX_PER_SIZE}, and
     * persist. Returns the 1-based rank the entry earned within its size, or -1
     * if it did not make the cut.
     */
    public int submit(ScoreEntry entry) {
        entries.add(entry);
        trimAllSizes();
        save();

        // Match by value, not identity: ScoreEntry is a record with value-based
        // equals(), so this keeps working if topFor() ever returns copies rather
        // than the exact instance that was submitted.
        List<ScoreEntry> top = topFor(entry.size());
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).equals(entry)) {
                return i + 1;
            }
        }
        return -1;
    }

    private void trimAllSizes() {
        List<ScoreEntry> kept = new ArrayList<>();
        for (BoardSize size : BoardSize.values()) {
            kept.addAll(historyFor(size));
        }
        entries.clear();
        entries.addAll(kept);
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        lines.add("# Tessera leaderboard  (size\tname\tscore\tturns\ttimeMillis\ttheme)");
        for (BoardSize size : BoardSize.values()) {
            for (ScoreEntry entry : historyFor(size)) {
                lines.add(entry.toLine());
            }
        }
        // Delegate to the shared atomic writer so the leaderboard and settings
        // share one durable write path and cannot drift. Throws
        // UncheckedIOException on failure, which submit()'s callers surface.
        DataPaths.writeAtomically(file, lines);
    }
}
