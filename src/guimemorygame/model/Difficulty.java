package guimemorygame.model;

public class Difficulty {
	
	private int row, col, turns;
	private String difficulty, name;

	public void setEasyDifficulty() {
		this.row = 3;
		this.col = 4;
		this.difficulty = "Easy";
	}
	
	public void setNormalDifficulty() {
		this.row = 4;
		this.col = 7;
		this.difficulty = "Normal";
	}
	
	public void setHardDifficulty() {
		this.row = 7;
		this.col = 8;
		this.difficulty = "Hard";
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setTurns(int turns) {
		this.turns = turns;
	}
	
	public int getRows() {
		return this.row;
	}
	
	public int getCols() {
		return this.col;
	}
	
	public int getTurns() {
		return this.turns;
	}
	
	public String getDifficulty() {
		return this.difficulty;
	}
	
	public String getName() {
		return this.name;
	}
}
