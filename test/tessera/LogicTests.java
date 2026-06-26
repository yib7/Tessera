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
