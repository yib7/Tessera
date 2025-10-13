package guimemorygame.model;


public class CharacterGamePiece implements GamePiece {
	private final Character symbol;
	private Character hidden;
	private boolean isVisible;
	
	public CharacterGamePiece(char s) {
		this.symbol = s;
		this.hidden = '?';
		this.isVisible = true;
	}
	
	@Override
	public Character getSymbol() {
		/*
		 * returns the symbol of the game piece
		 */
		if (isVisible() == true) {
			return symbol;
		}
		else {
			return hidden;
		}
	}
	
	@Override
	public void setVisible(boolean v) {
		/*
		 * sets whether or not the game piece is visible to the player
		 */
		this.isVisible = v;
	}
	
	@Override
	public boolean isVisible() {
		/*
		 * checks if the game piece is visible to the player
		 */
		return isVisible;
	}
	
	@Override
	public boolean equals(GamePiece other) {
		/*
		 * checks if two different game piece are equal to one another
		 */
		if(this.symbol == other.getSymbol()) {
			return true;
		}
		else {
			return false;
		}
	}
	
}
