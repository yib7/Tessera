package tessera.controller;

/**
 * What the controller needs the board view to do. Implemented by the game panel.
 * Keeping it an interface means the controller depends on behaviour, not on a
 * concrete Swing class, and the turn logic can be reasoned about without the
 * rendering code in the way.
 */
public interface GameView {

    /** Animate a tile face up, calling {@code whenDone} when the flip settles. */
    void flipUp(int row, int col, Runnable whenDone);

    /** Animate a tile face down. */
    void flipDown(int row, int col, Runnable whenDone);

    /** Mark a tile as permanently matched. */
    void markMatched(int row, int col);

    /** Enable or disable all board input (used during the mismatch pause). */
    void setBoardInteractive(boolean interactive);

    /**
     * Flash the two mismatched tiles (error border + shake) at the start of the
     * mismatch pause, before they flip back down. Default no-op: a headless view
     * (e.g. the logic tests) needs no animation, and the controller's turn logic
     * does not depend on it.
     */
    default void markMismatch(int firstRow, int firstCol, int secondRow, int secondCol) {
    }

    /** Refresh the turns / score / time readout. */
    void updateHud();

    /** Called once when the board is fully cleared. */
    void onWin();
}
