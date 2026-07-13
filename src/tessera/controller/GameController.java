package tessera.controller;

import javax.swing.Timer;

import tessera.model.Board;
import tessera.model.GameSession;
import tessera.model.Tile;

/**
 * Runs one game. It owns the turn state machine and is the only place that
 * mutates the session in response to clicks: reveal the first tile, reveal the
 * second, then either lock a match or flip both back after a short pause. The
 * view only reports clicks and animates what it is told; all the rules live
 * here.
 *
 * <p>This is the logic the headless tests exercise through {@link #onTileClicked}
 * with a no-op view, so it must not assume any painting has happened.
 */
public final class GameController {

    private static final int MISMATCH_PAUSE_MS = 850;

    private final GameSession session;
    private final GameView view;

    // The turn state machine.
    private Tile firstTile;
    private int firstRow = -1;
    private int firstCol = -1;
    private boolean awaitingSecond = false;
    private boolean locked = false; // input ignored during animations / pause
    private boolean paused = false;
    private boolean previewActive = false; // the pre-game memorize phase
    private final MismatchTimer mismatchTimer;

    /**
     * How the controller waits out the mismatch pause before flipping the two
     * mismatched tiles back. The running game uses a Swing Timer; tests inject a
     * synchronous runner so the mismatch branch is exercisable headlessly.
     */
    public interface MismatchTimer {
        /** Schedule flipBack to run after the mismatch pause. */
        void schedule(Runnable flipBack);
        /** Cancel a scheduled flipBack if it has not yet run. */
        void cancel();
    }

    /** Production mismatch pause: a one-shot Swing Timer. */
    private static final class SwingMismatchTimer implements MismatchTimer {
        private final int delayMs;
        private Timer timer;
        SwingMismatchTimer(int delayMs) { this.delayMs = delayMs; }
        @Override public void schedule(Runnable flipBack) {
            timer = new Timer(delayMs, e -> { timer = null; flipBack.run(); });
            timer.setRepeats(false);
            timer.start();
        }
        @Override public void cancel() {
            if (timer != null) { timer.stop(); timer = null; }
        }
    }

    public GameController(GameSession session, GameView view) {
        this(session, view, new SwingMismatchTimer(MISMATCH_PAUSE_MS));
    }

    public GameController(GameSession session, GameView view, MismatchTimer mismatchTimer) {
        this.session = session;
        this.view = view;
        this.mismatchTimer = mismatchTimer;
    }

    public GameSession session() {
        return session;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isPreviewActive() {
        return previewActive;
    }

    /**
     * Enter the pre-game memorize phase. While it is active the board shows every
     * face and clicks are ignored, so the run cannot start until the player has
     * had their look. The view owns the countdown and the reveal animation; the
     * controller only refuses input until {@link #endPreview} is called.
     */
    public void beginPreview() {
        previewActive = true;
    }

    /** Leave the memorize phase and accept play. */
    public void endPreview() {
        previewActive = false;
    }

    /** Pause the clock and freeze input. */
    public void pause() {
        if (!paused && !session.isFinished()) {
            paused = true;
            session.pauseClock();
            view.setBoardInteractive(false);
        }
    }

    /** Resume after a pause. */
    public void resume() {
        if (paused && !session.isFinished()) {
            paused = false;
            session.resumeClock();
            // Only re-enable if we are not mid-turn animation.
            if (!locked) {
                view.setBoardInteractive(true);
            }
        }
    }

    /**
     * Handle a click on the tile at (row, col). Ignored when paused, locked, or
     * when the tile is already up or matched.
     */
    public void onTileClicked(int row, int col) {
        if (previewActive || paused || locked || session.isFinished()) {
            return;
        }
        Board board = session.board();
        Tile tile = board.tileAt(row, col);
        if (tile.isMatched() || tile.isFaceUp()) {
            return;
        }

        session.startClock();

        if (!awaitingSecond) {
            revealFirst(row, col, tile);
        } else {
            revealSecond(row, col, tile);
        }
    }

    private void revealFirst(int row, int col, Tile tile) {
        firstTile = tile;
        firstRow = row;
        firstCol = col;
        awaitingSecond = true;
        tile.setFaceUp(true);
        view.flipUp(row, col, null);
    }

    private void revealSecond(int row, int col, Tile tile) {
        awaitingSecond = false;
        locked = true;
        tile.setFaceUp(true);

        final Tile second = tile;
        final int secondRow = row;
        final int secondCol = col;

        view.flipUp(row, col, () -> resolveTurn(secondRow, secondCol, second));
    }

    private void resolveTurn(int secondRow, int secondCol, Tile second) {
        if (firstTile.matches(second)) {
            handleMatch(secondRow, secondCol, second);
        } else {
            handleMismatch(secondRow, secondCol, second);
        }
    }

    private void handleMatch(int secondRow, int secondCol, Tile second) {
        session.recordMatch();
        firstTile.lockMatched();
        second.lockMatched();
        view.markMatched(firstRow, firstCol);
        view.markMatched(secondRow, secondCol);
        view.updateHud();
        clearTurn();
        locked = false;

        if (session.board().isSolved()) {
            session.finish();
            view.onWin();
        }
    }

    private void handleMismatch(int secondRow, int secondCol, Tile second) {
        session.recordMismatch();
        view.updateHud();
        view.setBoardInteractive(false);
        // Flash the two tiles (error border + shake) for the pause. firstRow/Col
        // are still valid here; clearTurn() runs later, inside the scheduled task.
        view.markMismatch(firstRow, firstCol, secondRow, secondCol);

        final Tile first = firstTile;
        final int fRow = firstRow;
        final int fCol = firstCol;

        mismatchTimer.schedule(() -> {
            first.setFaceUp(false);
            second.setFaceUp(false);
            view.flipDown(fRow, fCol, null);
            view.flipDown(secondRow, secondCol, () -> {
                clearTurn();
                locked = false;
                if (!paused) {
                    view.setBoardInteractive(true);
                }
            });
        });
    }

    /**
     * Cancel a scheduled mismatch flip-back, if one is pending. The view's
     * teardown calls this so a panel that is torn down during the 850ms pause
     * cannot fire the timer against a dead board.
     */
    public void cancelPending() {
        mismatchTimer.cancel();
    }

    private void clearTurn() {
        firstTile = null;
        firstRow = -1;
        firstCol = -1;
        awaitingSecond = false;
    }
}
