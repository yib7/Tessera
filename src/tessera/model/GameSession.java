package tessera.model;

import java.util.function.LongSupplier;

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
    // The RNG seed this board was dealt from, so a finished run can be replayed
    // on the identical layout. 0 for boards not created for replay tracking.
    private final long seed;

    private int turns;
    private int matches;
    private int mismatches;

    // Clock state. Time only advances while the game is running and not paused.
    // Timed off a monotonic nanoTime source, so it is immune to wall-clock jumps
    // (NTP correction, manual clock changes, DST-adjacent adjustments).
    private final LongSupplier nanoClock;
    private long startNanos;
    private long accumulatedNanos;
    private boolean clockRunning;
    // Whether the clock has ever been started (the player has made a move). This
    // distinguishes "never started" from "started then paused" so resume before
    // the first flip cannot start the clock prematurely (the memorize phase must
    // never count against the score).
    private boolean everStarted;
    private boolean finished;

    public GameSession(BoardSize size, TileTheme theme, Board board) {
        this(size, theme, board, 0L, System::nanoTime);
    }

    /** Production constructor that records the deal {@code seed} for replay. */
    public GameSession(BoardSize size, TileTheme theme, Board board, long seed) {
        this(size, theme, board, seed, System::nanoTime);
    }

    /**
     * Test seam: inject the nanosecond clock so pause/resume elapsed-time
     * behaviour is deterministic without real wall-clock sleeps. Production uses
     * {@link System#nanoTime}.
     */
    public GameSession(BoardSize size, TileTheme theme, Board board, LongSupplier nanoClock) {
        this(size, theme, board, 0L, nanoClock);
    }

    private GameSession(BoardSize size, TileTheme theme, Board board, long seed,
            LongSupplier nanoClock) {
        this.size = size;
        this.theme = theme;
        this.board = board;
        this.seed = seed;
        this.nanoClock = nanoClock;
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

    /** The RNG seed this board was dealt from (for replaying the same layout). */
    public long seed() {
        return seed;
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
            startNanos = nanoClock.getAsLong();
            clockRunning = true;
            everStarted = true;
        }
    }

    /** Fold the live segment into the accumulated total and stop the clock. */
    public void pauseClock() {
        if (clockRunning) {
            accumulatedNanos += nanoClock.getAsLong() - startNanos;
            clockRunning = false;
        }
    }

    /**
     * Resume after a pause. No-op unless the clock has actually been started (a
     * pause/resume before the first flip must not start the clock), or once the
     * game is finished.
     */
    public void resumeClock() {
        if (everStarted && !clockRunning && !finished) {
            startNanos = nanoClock.getAsLong();
            clockRunning = true;
        }
    }

    /** Total elapsed play time, excluding any paused stretches. */
    public long elapsedMillis() {
        long liveNanos = clockRunning ? nanoClock.getAsLong() - startNanos : 0;
        return (accumulatedNanos + liveNanos) / 1_000_000L;
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
