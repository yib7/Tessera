package guimemorygame.model;


public interface GamePiece {
	public boolean equals(GamePiece other);
	public void setVisible(boolean v);
	public boolean isVisible();
	public Character getSymbol();
}
