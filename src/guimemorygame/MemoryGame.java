package guimemorygame;

import javax.swing.SwingUtilities;

import guimemorygame.model.Difficulty;
import guimemorygame.view.InstructionsFrame;

public class MemoryGame implements Runnable {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new MemoryGame());
	}
	
	@Override
	public void run() {
		Difficulty difficulty = new Difficulty();
		difficulty.setNormalDifficulty();
		new InstructionsFrame(difficulty);	
	}
}
