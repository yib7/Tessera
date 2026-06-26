package tessera.model;

/**
 * The mutable state of one playthrough: the board, the running tallies, and the
 * clock. The controller drives this; the view only reads it. Keeping all run
 * state here (rather than smeared across the old Difficulty bag and the
 * controller's private fields) is what lets the HUD, pause, and scoring all read
 * from one source of truth.
 */
public final class GameSession {

    private final Board board;
    private final BoardSize size;
    private final TileTheme theme;

    private int turns;
    private int matches;
    private int mismatches;

    // Clock state. Time only advances while the game is running and not paused.
    private long startMillis;
    private long accumulatedMillis;
    private boolean clockRunning;
    private boolean finished;

    public GameSession(BoardSize size, TileTheme theme, Board board) {
        this.size = size;
        this.theme = theme;
        this.board = board;
    }

    public Board board() {
        return board;
    }

    public BoardSize size() {
        return size;
    }

    public TileTheme theme() {
        return theme;
    }

    public int turns() {
        return turns;
    }

    public int matches() {
        return matches;
    }

    public int mismatches() {
        return mismatches;
    }

    public boolean isFinished() {
        return finished;
    }

    /** Start the clock on the player's first flip. No-op if already running. */
    public void startClock() {
        if (!clockRunning && !finished) {
            startMillis = System.currentTimeMillis();
            clockRunning = true;
        }
    }

    public boolean isClockRunning() {
        return clockRunning;
    }

    /** Fold the live segment into the accumulated total and stop the clock. */
    public void pauseClock() {
        if (clockRunning) {
            accumulatedMillis += System.currentTimeMillis() - startMillis;
            clockRunning = false;
        }
    }

    /** Resume after a pause. */
    public void resumeClock() {
        if (!clockRunning && !finished) {
            startMillis = System.currentTimeMillis();
            clockRunning = true;
        }
    }

    /** Total elapsed play time, excluding any paused stretches. */
    public long elapsedMillis() {
        long live = clockRunning ? System.currentTimeMillis() - startMillis : 0;
        return accumulatedMillis + live;
    }

    public void recordMatch() {
        matches++;
        turns++;
    }

    public void recordMismatch() {
        mismatches++;
        turns++;
    }

    /** Stop the clock and mark the run complete. */
    public void finish() {
        pauseClock();
        finished = true;
    }

    public int score() {
        return ScoreCalculator.score(board.totalPairs(), mismatches, elapsedMillis());
    }
}
