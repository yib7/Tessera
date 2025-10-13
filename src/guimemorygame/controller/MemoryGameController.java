package guimemorygame.controller;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import java.util.Timer;
import java.util.TimerTask;

import guimemorygame.model.Difficulty;
import guimemorygame.view.MemoryGameFrame;
import guimemorygame.view.ResultsFrame;

public class MemoryGameController extends AbstractAction {
	
	private static final long serialVersionUID = 1L;
	private Difficulty difficulty;
	private MemoryGameFrame game;
	private int shown, winCondition, turns, matches;
	private Object tileIDOne, tileIDTwo;
	private String tileOne, tileTwo;
	
	public MemoryGameController(Difficulty difficulty, MemoryGameFrame game) {
		this.turns = 1;
		this.difficulty = difficulty;
		this.difficulty.setTurns(this.turns);
		this.game = game;
		this.shown = 0;
		this.matches = 0;
		this.winCondition = (this.difficulty.getRows() * this.difficulty.getCols())/2;
		
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		if (this.shown == 0) {
			JButton button1 = (JButton) event.getSource();
			this.tileIDOne = button1.getClientProperty("ID");
			this.game.showTile(this.tileIDOne);
			this.tileOne = button1.getActionCommand();
			this.game.updateFrame();
			this.shown = 1;			
		}
		else if (this.shown == 1) {
			JButton button2 = (JButton) event.getSource();
			this.tileIDTwo = button2.getClientProperty("ID");
			this.game.showTile(this.tileIDTwo);
			this.tileTwo = button2.getActionCommand();
			if (this.tileOne.equals(tileTwo)) {
				this.game.updateFrame();
				this.shown = 0;
				this.matches++;
			}
			else {
				this.turns++;
				this.difficulty.setTurns(this.turns);
				this.game.turnOffButtons();
				Timer timer = new Timer();
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						timetask();					
					}
				};
				timer.schedule(task, 2000);
			}
		}
		if(this.matches == this.winCondition) {
			this.game.disposeFrame();
			new ResultsFrame(this.difficulty);
		}
	}

		
	public void timetask() {
		this.game.hideTile(this.tileIDOne);
		this.game.hideTile(this.tileIDTwo);
		this.game.updateTurnPanel(this.turns);
		this.shown = 0;
		this.game.turnOnButtons();
	}
}
