package tessera;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tessera.controller.GameController;
import tessera.controller.GameView;
import tessera.model.Board;
import tessera.model.BoardSize;
import tessera.model.GameSession;
import tessera.model.Leaderboard;
import tessera.model.ScoreCalculator;
import tessera.model.ScoreEntry;
import tessera.model.Settings;
import tessera.model.Tile;
import tessera.model.TileTheme;

/**
 * A dependency-free, headless check of the model and controller logic. Run with
 * {@code java -cp <bin> tessera.LogicTests}. Exits non-zero on the first
 * failure so a build script can gate on it.
 *
 * <p>This is intentionally a plain main rather than a JUnit suite: the project
 * ships no third-party jars, and a single self-contained runner keeps the build
 * to one javac invocation with nothing to download.
 */
public final class LogicTests {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        testBoardDealsExactPairs();
        testBoardSizesAllEven();
        testMatchDetection();
        testScoreRewardsCleanFastPlay();
        testLeaderboardRoundTrip();
        testLeaderboardCorruptFileIsSafe();
        testLeaderboardRanksByScore();
        testControllerFullPlaythrough();
        testPreviewBlocksInput();
        testRapidClicksDuringTurn();
        testMismatchResolves();
        testScoreEntrySanitizationAndRoundTrip();
        testCorruptLinesRejected();
        testQualifiesTieUsesRankOrder();
        testSettingsRoundTrip();

        System.out.println();
        System.out.printf("Ran %d checks, %d failure(s).%n", checks, failures);
        if (failures > 0) {
            System.exit(1);
        }
        System.out.println("All logic tests passed.");
    }

    // --- board ---------------------------------------------------------------

    private static void testBoardDealsExactPairs() {
        Board board = new Board(BoardSize.HARD, TileTheme.SHAPES, new Random(42));
        Map<String, Integer> counts = new HashMap<>();
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                String face = board.tileAt(r, c).face();
                counts.merge(face, 1, Integer::sum);
            }
        }
        check("hard board has pairCount distinct faces",
                counts.size() == BoardSize.HARD.pairCount());
        boolean allPairs = counts.values().stream().allMatch(n -> n == 2);
        check("every face on the board appears exactly twice", allPairs);
    }

    private static void testBoardSizesAllEven() {
        boolean allEven = true;
        for (BoardSize size : BoardSize.values()) {
            if (size.cellCount() % 2 != 0) {
                allEven = false;
            }
        }
        check("all board sizes have an even cell count", allEven);
    }

    // --- match detection -----------------------------------------------------

    private static void testMatchDetection() {
        Tile a = new Tile("Q");
        Tile b = new Tile("Q");
        Tile c = new Tile("Z");
        check("identical faces match", a.matches(b));
        check("different faces do not match", !a.matches(c));
        a.lockMatched();
        check("locked tile is matched and face up", a.isMatched() && a.isFaceUp());
    }

    // --- scoring -------------------------------------------------------------

    private static void testScoreRewardsCleanFastPlay() {
        int cleanFast = ScoreCalculator.score(28, 0, 30_000);
        int sloppySlow = ScoreCalculator.score(28, 40, 300_000);
        check("a clean fast game outscores a sloppy slow one of the same size",
                cleanFast > sloppySlow);
        check("score never goes negative", ScoreCalculator.score(6, 1000, 0) >= 0);
        int small = ScoreCalculator.score(6, 0, 30_000);
        int large = ScoreCalculator.score(28, 0, 30_000);
        check("larger boards are worth more for the same play", large > small);
    }

    // --- leaderboard ---------------------------------------------------------

    private static void testLeaderboardRoundTrip() throws IOException {
        Path tmp = Files.createTempFile("tessera-lb", ".tsv");
        Files.delete(tmp); // start with no file
        Leaderboard lb = new Leaderboard(tmp);
        check("a fresh leaderboard is empty",
                lb.topFor(BoardSize.NORMAL).isEmpty());

        lb.submit(new ScoreEntry("Ada", BoardSize.NORMAL, 1200, 18, 45_000));

        Leaderboard reloaded = new Leaderboard(tmp);
        List<ScoreEntry> top = reloaded.topFor(BoardSize.NORMAL);
        check("submitted score persists across reload", top.size() == 1);
        check("persisted name survives the round trip",
                !top.isEmpty() && top.get(0).name().equals("Ada"));
        Files.deleteIfExists(tmp);
    }

    private static void testLeaderboardCorruptFileIsSafe() throws IOException {
        Path tmp = Files.createTempFile("tessera-corrupt", ".tsv");
        Files.writeString(tmp, "garbage line with no tabs\n"
                + "Normal\tBob\tnotanumber\t3\t1000\n"
                + "Easy\tValid\t500\t4\t20000\n");
        Leaderboard lb = new Leaderboard(tmp);
        check("malformed lines are skipped, valid ones kept",
                lb.topFor(BoardSize.EASY).size() == 1);
        check("the bad Normal line was dropped",
                lb.topFor(BoardSize.NORMAL).isEmpty());
        Files.deleteIfExists(tmp);
    }

    private static void testLeaderboardRanksByScore() throws IOException {
        Path tmp = Files.createTempFile("tessera-rank", ".tsv");
        Files.delete(tmp);
        Leaderboard lb = new Leaderboard(tmp);
        lb.submit(new ScoreEntry("Low", BoardSize.HARD, 800, 30, 90_000));
        lb.submit(new ScoreEntry("High", BoardSize.HARD, 1500, 20, 60_000));
        lb.submit(new ScoreEntry("Mid", BoardSize.HARD, 1100, 25, 70_000));
        List<ScoreEntry> top = lb.topFor(BoardSize.HARD);
        check("highest score ranks first", top.get(0).name().equals("High"));
        check("lowest score ranks last", top.get(2).name().equals("Low"));

        // The 6th entry must not survive the top-5 cap if it is the weakest.
        for (int i = 0; i < 5; i++) {
            lb.submit(new ScoreEntry("Filler" + i, BoardSize.HARD, 2000 + i, 10, 30_000));
        }
        check("leaderboard caps at five per size",
                lb.topFor(BoardSize.HARD).size() == Leaderboard.MAX_PER_SIZE);
        Files.deleteIfExists(tmp);
    }

    /**
     * {@link Leaderboard#qualifies} must use the same ordering as ranking, so a
     * run that ties the last-place score but takes fewer turns still makes the cut
     * (P2-1: the old strict {@code >} logic dropped it). Also: a strictly lower
     * score is refused, a higher score qualifies, and any size whose board is not
     * yet full always qualifies.
     */
    private static void testQualifiesTieUsesRankOrder() throws IOException {
        Path tmp = Files.createTempFile("tessera-qualify", ".tsv");
        Files.delete(tmp);
        Leaderboard lb = new Leaderboard(tmp);
        // Fill HARD with five entries all at score 1000 and equal time, differing
        // only in turns (10..14), so the last-place incumbent has turns == 14.
        for (int turns = 10; turns <= 14; turns++) {
            lb.submit(new ScoreEntry("p" + turns, BoardSize.HARD, 1000, turns, 60_000));
        }

        check("a score-tying run with fewer turns qualifies (the P2-1 tie bug)",
                lb.qualifies(new ScoreEntry("x", BoardSize.HARD, 1000, 13, 60_000)));
        check("a strictly lower score does not qualify",
                !lb.qualifies(new ScoreEntry("x", BoardSize.HARD, 999, 1, 1000)));
        check("a higher score qualifies",
                lb.qualifies(new ScoreEntry("x", BoardSize.HARD, 1001, 30, 90_000)));
        check("a size whose board is not yet full always qualifies",
                lb.qualifies(new ScoreEntry("x", BoardSize.EASY, 1, 99, 999_999)));

        Files.deleteIfExists(tmp);
    }

    // --- score entry ---------------------------------------------------------

    /**
     * {@link ScoreEntry}'s compact constructor sanitises names (delimiter and
     * newline stripping, 24-char truncation, "Anonymous" fallback), and a clean
     * entry round-trips by value through {@code toLine}/{@code fromLine} (records
     * have value-based equals).
     */
    private static void testScoreEntrySanitizationAndRoundTrip() {
        check("an embedded tab in a name is replaced with a space",
                new ScoreEntry("Ada\tLovelace", BoardSize.NORMAL, 100, 5, 1000)
                        .name().equals("Ada Lovelace"));
        check("an over-long name is truncated to 24 characters",
                new ScoreEntry("A".repeat(30), BoardSize.NORMAL, 100, 5, 1000)
                        .name().length() == 24);
        check("a null name falls back to Anonymous",
                new ScoreEntry(null, BoardSize.NORMAL, 100, 5, 1000)
                        .name().equals("Anonymous"));
        check("a blank name falls back to Anonymous",
                new ScoreEntry("   ", BoardSize.NORMAL, 100, 5, 1000)
                        .name().equals("Anonymous"));

        ScoreEntry entry = new ScoreEntry("Ada", BoardSize.HARD, 1234, 21, 54_000);
        ScoreEntry parsed = ScoreEntry.fromLine(entry.toLine());
        check("a clean entry round-trips through toLine/fromLine by value",
                parsed != null && parsed.equals(entry));

        ScoreEntry tabbed = new ScoreEntry("Ada\tByron", BoardSize.EASY, 42, 3, 9000);
        ScoreEntry tabbedParsed = ScoreEntry.fromLine(tabbed.toLine());
        check("a de-tabbed name round-trips with no tab and equal value",
                tabbedParsed != null && !tabbedParsed.name().contains("\t")
                        && tabbedParsed.equals(tabbed));
    }

    /**
     * {@link ScoreEntry#fromLine} rejects lines with an unknown size label or any
     * negative numeric field (P2-6), and accepts a well-formed line.
     */
    private static void testCorruptLinesRejected() {
        check("an unknown size label is rejected",
                ScoreEntry.fromLine("Bogus\tX\t100\t5\t1000") == null);
        check("a negative score is rejected",
                ScoreEntry.fromLine("Normal\tX\t-5\t5\t1000") == null);
        check("negative turns are rejected",
                ScoreEntry.fromLine("Normal\tX\t100\t-1\t1000") == null);
        check("a negative time is rejected",
                ScoreEntry.fromLine("Normal\tX\t100\t5\t-1000") == null);

        ScoreEntry valid = ScoreEntry.fromLine("Normal\tX\t100\t5\t1000");
        check("a well-formed line parses to the right fields",
                valid != null && valid.size() == BoardSize.NORMAL
                        && valid.score() == 100 && valid.turns() == 5
                        && valid.timeMillis() == 1000);
    }

    // --- settings ------------------------------------------------------------

    /**
     * {@link Settings} round-trips board size, tile theme, and the sound flag
     * through its path-taking save/load seam. The theme check also confirms the
     * display-name serialisation ({@code SHAPES.displayName()} is "Symbols")
     * resolves back to the enum on load.
     */
    private static void testSettingsRoundTrip() throws IOException {
        Path tmp = Files.createTempFile("tessera-settings", ".properties");
        try {
            Settings s = new Settings();
            s.setBoardSize(BoardSize.HARD);
            s.setTileTheme(TileTheme.SHAPES);
            s.setSoundEnabled(false);
            s.save(tmp);

            Settings loaded = Settings.load(tmp);
            check("board size survives the settings round trip",
                    loaded.boardSize() == BoardSize.HARD);
            check("tile theme survives the settings round trip (Symbols -> SHAPES)",
                    loaded.tileTheme() == TileTheme.SHAPES);
            check("sound flag survives the settings round trip",
                    !loaded.soundEnabled());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // --- controller ----------------------------------------------------------

    /**
     * Drive a full game on a known seed through the controller with a no-op
     * view, clicking matching pairs directly so we never rely on animation
     * callbacks. Verifies the session reaches a solved, finished state.
     */
    private static void testControllerFullPlaythrough() {
        Board board = new Board(BoardSize.EASY, TileTheme.LETTERS, new Random(7));
        GameSession session = new GameSession(BoardSize.EASY, TileTheme.LETTERS, board);
        GameController controller = new GameController(session, new SyncView());

        // Group every cell coordinate by its face, then click each face's two
        // cells back to back so every turn is a guaranteed match.
        Map<String, List<int[]>> positions = new HashMap<>();
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                String face = board.tileAt(r, c).face();
                positions.computeIfAbsent(face, k -> new java.util.ArrayList<>())
                        .add(new int[] {r, c});
            }
        }

        for (List<int[]> slots : positions.values()) {
            controller.onTileClicked(slots.get(0)[0], slots.get(0)[1]);
            controller.onTileClicked(slots.get(1)[0], slots.get(1)[1]);
        }

        check("all pairs matched", session.matches() == board.totalPairs());
        check("no mismatches when only correct pairs are clicked",
                session.mismatches() == 0);
        check("board is solved", board.isSolved());
        check("session is finished", session.isFinished());
    }

    /**
     * The pre-game memorize phase must swallow clicks: while it is active a tap
     * neither flips a tile nor counts a turn, and once it ends play resumes
     * normally.
     */
    private static void testPreviewBlocksInput() {
        Board board = new Board(BoardSize.EASY, TileTheme.LETTERS, new Random(7));
        GameSession session = new GameSession(BoardSize.EASY, TileTheme.LETTERS, board);
        GameController controller = new GameController(session, new SyncView());

        controller.beginPreview();
        check("controller reports the preview is active", controller.isPreviewActive());
        controller.onTileClicked(0, 0);
        check("a click during the memorize phase is ignored",
                !board.tileAt(0, 0).isFaceUp() && session.turns() == 0);

        controller.endPreview();
        check("controller reports the preview is over", !controller.isPreviewActive());
        controller.onTileClicked(0, 0);
        check("a click after the memorize phase reveals the tile",
                board.tileAt(0, 0).isFaceUp());
    }

    /**
     * The turn state machine must ignore extra clicks that arrive while a turn
     * is mid-resolution (the {@code locked} guard) and clicks on tiles that are
     * already up or matched. In the real UI these arrive as rapid taps during
     * the flip animation; here we reproduce them by holding the second-flip
     * completion callback open so the controller stays locked, then firing more
     * clicks and asserting they are swallowed.
     */
    private static void testRapidClicksDuringTurn() {
        Board board = new Board(BoardSize.EASY, TileTheme.LETTERS, new Random(7));
        GameSession session = new GameSession(BoardSize.EASY, TileTheme.LETTERS, board);
        DeferredView view = new DeferredView();
        GameController controller = new GameController(session, view);

        // Find the matching partner of (0,0). Using a matching pair keeps the
        // turn resolution synchronous (handleMatch runs no timer), so the test
        // does not depend on the mismatch pause Timer.
        int[] first = {0, 0};
        int[] second = null;
        String firstFace = board.tileAt(0, 0).face();
        outer:
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                if ((r != 0 || c != 0) && board.tileAt(r, c).face().equals(firstFace)) {
                    second = new int[] {r, c};
                    break outer;
                }
            }
        }

        // Reveal the first tile (resolves immediately: no callback deferred).
        controller.onTileClicked(first[0], first[1]);
        check("first reveal turns the tile face up",
                board.tileAt(first[0], first[1]).isFaceUp());

        // Clicking the same face-up tile again is a no-op (turn count unchanged).
        int turnsBefore = session.turns();
        controller.onTileClicked(first[0], first[1]);
        check("clicking an already-face-up tile is ignored",
                session.turns() == turnsBefore);

        // Reveal the second tile. DeferredView withholds the flipUp callback, so
        // the controller is now locked awaiting turn resolution.
        controller.onTileClicked(second[0], second[1]);
        check("controller is locked while a turn resolves", view.hasPending());

        // A third distinct tile clicked during the lock must be swallowed: it
        // should not flip and must not advance the turn count.
        int[] third = null;
        for (int r = 0; r < board.rows() && third == null; r++) {
            for (int c = 0; c < board.cols(); c++) {
                if ((r != first[0] || c != first[1])
                        && (r != second[0] || c != second[1])) {
                    third = new int[] {r, c};
                    break;
                }
            }
        }
        int turnsDuringLock = session.turns();
        controller.onTileClicked(third[0], third[1]);
        check("a click while locked does not flip a third tile",
                !board.tileAt(third[0], third[1]).isFaceUp());
        check("a click while locked does not advance the turn count",
                session.turns() == turnsDuringLock);

        // Release the held callback so the turn resolves (a match) and unlocks.
        view.releasePending();
        check("after the turn resolves, play accepts a new click",
                acceptsClick(controller, board, third));
    }

    /**
     * The mismatch half of the turn state machine, previously only verified by
     * playing the game (the real flip-back runs on a Swing Timer that never fires
     * in these headless tests). An injected synchronous
     * {@link GameController.MismatchTimer} runs the flip-back in place, so clicking
     * two differing faces here must record one mismatch and one turn, flip both
     * tiles back face down, and leave input unlocked for the next click.
     */
    private static void testMismatchResolves() {
        Board board = new Board(BoardSize.EASY, TileTheme.LETTERS, new Random(7));
        GameSession session = new GameSession(BoardSize.EASY, TileTheme.LETTERS, board);
        GameController.MismatchTimer immediate = new GameController.MismatchTimer() {
            @Override public void schedule(Runnable flipBack) { flipBack.run(); }
            @Override public void cancel() { }
        };
        GameController controller = new GameController(session, new SyncView(), immediate);

        // Find any cell whose face differs from (0,0)'s: clicking the two is a
        // guaranteed mismatch.
        String firstFace = board.tileAt(0, 0).face();
        int[] mismatch = null;
        outer:
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                if (!board.tileAt(r, c).face().equals(firstFace)) {
                    mismatch = new int[] {r, c};
                    break outer;
                }
            }
        }

        controller.onTileClicked(0, 0);
        controller.onTileClicked(mismatch[0], mismatch[1]);

        check("a mismatched turn records exactly one mismatch",
                session.mismatches() == 1);
        check("a mismatched turn counts as one turn", session.turns() == 1);
        check("the first mismatched tile flips back face down",
                !board.tileAt(0, 0).isFaceUp());
        check("the second mismatched tile flips back face down",
                !board.tileAt(mismatch[0], mismatch[1]).isFaceUp());

        // Input must be unlocked again: clicking a third, still-down tile reveals
        // it.
        int[] third = null;
        for (int r = 0; r < board.rows() && third == null; r++) {
            for (int c = 0; c < board.cols(); c++) {
                if ((r != 0 || c != 0) && (r != mismatch[0] || c != mismatch[1])) {
                    third = new int[] {r, c};
                    break;
                }
            }
        }
        check("input is unlocked after a mismatch resolves",
                acceptsClick(controller, board, third));
    }

    /** True if clicking the given cell now reveals it (i.e. input is accepted). */
    private static boolean acceptsClick(GameController controller, Board board,
            int[] cell) {
        controller.onTileClicked(cell[0], cell[1]);
        return board.tileAt(cell[0], cell[1]).isFaceUp();
    }

    /**
     * A GameView that resolves flips immediately EXCEPT it withholds the callback
     * of the most recent flipUp, so a test can hold the controller in its locked
     * state and then release it on demand.
     */
    private static final class DeferredView implements GameView {
        private Runnable pending;

        boolean hasPending() {
            return pending != null;
        }

        void releasePending() {
            Runnable r = pending;
            pending = null;
            if (r != null) {
                r.run();
            }
        }

        @Override
        public void flipUp(int row, int col, Runnable whenDone) {
            // The second reveal of a turn passes a non-null callback; hold it so
            // the controller stays locked until the test releases it.
            if (whenDone != null) {
                pending = whenDone;
            }
        }

        @Override
        public void flipDown(int row, int col, Runnable whenDone) {
            if (whenDone != null) {
                whenDone.run();
            }
        }

        @Override
        public void markMatched(int row, int col) {
        }

        @Override
        public void setBoardInteractive(boolean interactive) {
        }

        @Override
        public void updateHud() {
        }

        @Override
        public void onWin() {
        }
    }

    /** A GameView that runs flip callbacks immediately, so the turn resolves
     *  synchronously for the test. */
    private static final class SyncView implements GameView {
        @Override
        public void flipUp(int row, int col, Runnable whenDone) {
            if (whenDone != null) {
                whenDone.run();
            }
        }

        @Override
        public void flipDown(int row, int col, Runnable whenDone) {
            if (whenDone != null) {
                whenDone.run();
            }
        }

        @Override
        public void markMatched(int row, int col) {
        }

        @Override
        public void setBoardInteractive(boolean interactive) {
        }

        @Override
        public void updateHud() {
        }

        @Override
        public void onWin() {
        }
    }

    // --- harness -------------------------------------------------------------

    private static void check(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  PASS  " + label);
        } else {
            failures++;
            System.out.println("  FAIL  " + label);
        }
    }
}
