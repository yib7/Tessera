package guimemorygame.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import guimemorygame.model.Difficulty;
import guimemorygame.model.Leaderboard;


public class ResultsFrame {
	
	private final JFrame results;
	private Difficulty difficulty;
	
	
	public ResultsFrame(Difficulty difficulty) {
		this.difficulty = difficulty;
		updateLeaderboardFrame();
		this.results = createAndShowGUI();
	}
	
	private JFrame createAndShowGUI() {
		JFrame results = new JFrame("Results");
		results.setLayout(new BorderLayout());
		results.setSize(450,350);
		results.setResizable(false);
		results.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		results.add(createTitlePanel(), BorderLayout.NORTH);
		results.add(createTextPanel(), BorderLayout.CENTER);
		results.add(createContinuePanel(), BorderLayout.SOUTH);
		setCenterScreen(results);
		results.setVisible(true);
		return results;
	}
	
	private JPanel createTitlePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("YOU WON!");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTitleFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createTextPanel() {
		JPanel panel = new JPanel (new BorderLayout());
		JLabel label = new JLabel("Play Again?");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTileFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createContinuePanel() {
		JPanel panel = new JPanel(new FlowLayout());
		JButton playAgainButton = new JButton("Play Again");
		playAgainButton.addActionListener(event -> results.dispose());
		playAgainButton.addActionListener(event -> new LeaderboardFrame(this.difficulty));
		panel.add(playAgainButton);
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(event -> results.dispose());
		exitButton.addActionListener(event -> System.exit(0));
		panel.add(exitButton);
		return panel;
	}
	
	private void setCenterScreen(JFrame frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
	}
	
	private void updateLeaderboardFrame() {
		Leaderboard leaderboard = new Leaderboard();
		leaderboard.updateLeaderboard(this.difficulty.getName(), this.difficulty.getDifficulty(), this.difficulty.getTurns());
	}

}
