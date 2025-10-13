package guimemorygame.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import guimemorygame.controller.MemoryGameController;
import guimemorygame.model.Difficulty;
import guimemorygame.model.GameBoard;
import guimemorygame.model.LatinAlphabet;

public class MemoryGameFrame {
	
	private JFrame game;
	
	private GameBoard grid;
	
	private JPanel gridPanel;
	
	private JPanel turnPanel;
	
	private JButton begin;
	
	private JButton[][] tiles;
	
	private MemoryGameController controller;
	
	private Difficulty difficulty;
	
	
	public MemoryGameFrame(Difficulty difficulty) {
		this.difficulty = difficulty;
		this.grid = new GameBoard(this.difficulty.getRows(), this.difficulty.getCols(), new LatinAlphabet());
		this.grid.setUp(this.difficulty.getRows(), this.difficulty.getCols());
		this.controller = new MemoryGameController(this.difficulty, this);
		this.tiles = new JButton[this.difficulty.getRows()][this.difficulty.getCols()];
		this.game = createAndShowGUI();
	}
	
	private JFrame createAndShowGUI() {
		JFrame game = new JFrame("Memory Game");
		game.setLayout(new BorderLayout());
		game.setSize(1000, 800);
		game.setResizable(true);
		game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		game.setJMenuBar(createMenuBar());
		game.add(createTitlePanel(), BorderLayout.NORTH);
		game.add(createGridPanel(), BorderLayout.CENTER);
		game.add(createBeginButton(), BorderLayout.SOUTH);
		setCenterScreen(game);
		game.setVisible(true);
		return game;
	}
	
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu difficultyMenu = new JMenu("Difficulty");
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(event -> game.dispose());
		exitButton.addActionListener(event -> System.exit(0));
		menuBar.add(difficultyMenu);
		menuBar.add(Box.createHorizontalGlue());
		menuBar.add(exitButton);
		
		JMenuItem easy = new JMenuItem("Easy");
		Difficulty easyDiff = new Difficulty();
		easyDiff.setEasyDifficulty();
		easy.addActionListener(event -> game.dispose());
		easy.addActionListener(event -> new LeaderboardFrame(easyDiff));
		difficultyMenu.add(easy);
		
		
		JMenuItem normal = new JMenuItem("Normal");
		Difficulty normalDiff = new Difficulty();
		normalDiff.setNormalDifficulty();
		normal.addActionListener(event -> game.dispose());
		normal.addActionListener(event -> new LeaderboardFrame(normalDiff));
		difficultyMenu.add(normal);
		
		JMenuItem hard = new JMenuItem("Hard");
		Difficulty hardDiff = new Difficulty();
		hardDiff.setHardDifficulty();
		hard.addActionListener(event -> game.dispose());
		hard.addActionListener(event -> new LeaderboardFrame(hardDiff));
		difficultyMenu.add(hard);
		
		return menuBar;
	}
	
	private JPanel createTitlePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Memory Game");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTitleFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createGridPanel() {
		int id = 1;
		this.gridPanel = new JPanel(new GridLayout(this.difficulty.getRows(), this.difficulty.getCols(), 5, 5));
		for (int i = 0; i < this.difficulty.getRows(); i ++) {
			for (int j = 0; j < this.difficulty.getCols(); j++) {
				this.tiles[i][j] = new JButton("" + this.grid.getPiece(i, j) + "");
				this.tiles[i][j].putClientProperty("ID","" + id + "");
				this.tiles[i][j].setFont(CharFonts.getTileFont());
				gridPanel.add(this.tiles[i][j]);
				id++;
			}
		}
		return gridPanel;
	}
	
	private JButton createBeginButton() {
		this.begin = new JButton("BEGIN");
		begin.addActionListener(event -> beginGame(begin));
		return begin;
	}
	
	private JPanel createTurnPanel() {
		this.turnPanel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Turn # 1");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTurnFont());
		this.turnPanel.add(label, BorderLayout.CENTER);
		return this.turnPanel;

	}
	
	private void beginGame(JButton begin) {
		this.grid.setInvisible();
		for (int i = 0; i < this.difficulty.getRows(); i ++) {
			for (int j = 0; j < this.difficulty.getCols(); j++) {
				this.tiles[i][j].setText("" + this.grid.getPiece(i, j) + "");
				this.tiles[i][j].addActionListener(this.controller);
			}
		}
		this.game.remove(begin);
		this.game.add(createTurnPanel(), BorderLayout.SOUTH);
		this.game.validate();
		this.game.repaint();
	}
	
	public void showTile(Object id) {
		Object buttonID = id;
		for (int i = 0; i < this.difficulty.getRows(); i ++) {
			for (int j = 0; j < this.difficulty.getCols(); j++) {
				if (buttonID == this.tiles[i][j].getClientProperty("ID")) {
					this.grid.revealPiece(i, j);
					this.tiles[i][j].setText("" + this.grid.getPiece(i, j) + "");
					this.tiles[i][j].setEnabled(false);
				}
			}
		}
	}
	
	public void hideTile(Object id) {
		Object buttonID = id;
		for (int i = 0; i < this.difficulty.getRows(); i ++) {
			for (int j = 0; j < this.difficulty.getCols(); j++) {
				if (buttonID == this.tiles[i][j].getClientProperty("ID")) {
					this.grid.hidePiece(i, j);
					this.tiles[i][j].setText("" + this.grid.getPiece(i, j) + "");
					this.tiles[i][j].setEnabled(true);
				}
			}
		}
	}
	
	public void turnOffButtons() {
		for(int i = 0; i < this.difficulty.getRows(); i ++) {
			for (int j = 0; j < this.difficulty.getCols(); j++) {
				this.tiles[i][j].setEnabled(false);
			}
		}
		this.game.repaint();
	}
	
	public void turnOnButtons() {
		for(int i = 0; i < this.difficulty.getRows(); i ++) {
			for (int j = 0; j < this.difficulty.getCols(); j++) {
				if (this.grid.visiblePiece(i, j) == false) {
					this.tiles[i][j].setEnabled(true);
				}
			}
		}
		this.game.repaint();
	}
	
	public void updateTurnPanel(int turn) {
		this.game.remove(this.turnPanel);
		this.turnPanel.removeAll();
		JLabel turnLabel = new JLabel("Turn # " + turn);
		turnLabel.setHorizontalAlignment(JLabel.CENTER);
		turnLabel.setVerticalAlignment(JLabel.CENTER);
		turnLabel.setFont(CharFonts.getTurnFont());
		this.turnPanel.add(turnLabel, BorderLayout.CENTER);
		this.game.add(this.turnPanel, BorderLayout.SOUTH);
		this.game.repaint();

	}
	
	public void updateFrame() {
		this.game.repaint();
	}
	
	public void disposeFrame() {
		this.game.dispose();
	}
	
	private void setCenterScreen(JFrame frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
	}

}
