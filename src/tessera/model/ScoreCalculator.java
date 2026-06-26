package tessera.model;

/**
 * Turns a finished game into a single comparable score. Raw turn count rewards
 * luck as much as memory, so the score combines three things a player actually
 * controls:
 *
 * <ul>
 *   <li>a fixed reward for every matched pair, scaled by board size,</li>
 *   <li>a penalty for every mismatched flip,</li>
 *   <li>a speed bonus that decays the longer the game runs.</li>
 * </ul>
 *
 * The numbers are tuned so a clean, fast hard game scores well above a sloppy
 * easy one, and the floor is clamped to zero so a bad run never goes negative.
 */
public final class ScoreCalculator {

    private static final int POINTS_PER_PAIR = 100;
    private static final int MISMATCH_PENALTY = 15;
    private static final int SPEED_BONUS_CAP = 1000;
    // Seconds of "free" time before the speed bonus starts decaying.
    private static final int GRACE_SECONDS = 5;
    // How fast the bonus bleeds off, in points per second after the grace.
    private static final int DECAY_PER_SECOND = 6;

    private ScoreCalculator() {
    }

    /**
     * @param pairs        pairs on the board (size multiplier)
     * @param mismatches   number of mismatched flips over the whole game
     * @param elapsedMillis wall-clock time from first flip to solve
     */
    public static int score(int pairs, int mismatches, long elapsedMillis) {
        int base = pairs * POINTS_PER_PAIR;
        // Larger boards are worth proportionally more, so the multiplier grows
        // with pair count rather than being flat.
        int sizeMultiplier = base;
        int penalty = mismatches * MISMATCH_PENALTY;

        long elapsedSeconds = Math.max(0, elapsedMillis / 1000);
        long overGrace = Math.max(0, elapsedSeconds - GRACE_SECONDS);
        int speedBonus = (int) Math.max(0, SPEED_BONUS_CAP - overGrace * DECAY_PER_SECOND);

        return Math.max(0, sizeMultiplier - penalty + speedBonus);
    }
}
