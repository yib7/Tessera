package tessera.model;

/**
 * One card on the board. A tile knows its face value, whether it is currently
 * turned face up, and whether it has already been matched and locked. The view
 * reads this state to decide what to paint; it does not store any state of its
 * own about a card.
 */
public final class Tile {

    private final String face;
    private boolean faceUp;
    private boolean matched;

    public Tile(String face) {
        if (face == null) {
            throw new IllegalArgumentException("Tile face must not be null.");
        }
        this.face = face;
        this.faceUp = false;
        this.matched = false;
    }

    public String face() {
        return face;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public boolean isMatched() {
        return matched;
    }

    /** Lock the tile face up once it has been paired. */
    public void lockMatched() {
        this.matched = true;
        this.faceUp = true;
    }

    /** Two tiles match when they show the same face. */
    public boolean matches(Tile other) {
        return other != null && this.face.equals(other.face);
    }

    @Override
    public String toString() {
        return "Tile[" + face + (matched ? ",matched" : faceUp ? ",up" : ",down") + "]";
    }
}
