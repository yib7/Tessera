package guimemorygame.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import guimemorygame.model.Difficulty;
import guimemorygame.model.Leaderboard;


public class LeaderboardFrame {
	
	private final JFrame leaderboardFrame;
	private Leaderboard leaderboard;
	private Difficulty difficulty;
	
	
	public LeaderboardFrame(Difficulty difficulty) {
		this.leaderboard = new Leaderboard();
		this.leaderboardFrame = createAndShowGUI();
		this.difficulty = difficulty;
	}
	
	private JFrame createAndShowGUI() {
		JFrame leaderboard = new JFrame("leaderboard");
		leaderboard.setLayout(new BorderLayout());
		leaderboard.setSize(500,400);
		leaderboard.setResizable(false);
		leaderboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		leaderboard.add(createTitlePanel(), BorderLayout.NORTH);
		leaderboard.add(createTextPanel(), BorderLayout.CENTER);
		leaderboard.add(createContinuePanel(), BorderLayout.SOUTH);
		setCenterScreen(leaderboard);
		leaderboard.setVisible(true);
		return leaderboard;
	}
	
	private JPanel createTitlePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Leaderboard");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTitleFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createTextPanel() {
		this.leaderboard.createLeaderboard();
		JPanel panel = new JPanel (new BorderLayout());
		JLabel label = new JLabel("<html>" + this.leaderboard.displayLeaderboard() + "<html>");
		label.setHorizontalAlignment(JLabel.LEFT);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getLeaderboardFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createContinuePanel() {
		JPanel panel = new JPanel(new FlowLayout());
		JLabel label = new JLabel("Enter your name here:");
		JTextField textBox = new JTextField(20);
		textBox.addActionListener(event -> this.difficulty.setName(textBox.getText()));
		textBox.addActionListener(event -> this.leaderboardFrame.dispose());
		textBox.addActionListener(event -> new MemoryGameFrame(this.difficulty));
		panel.add(label);
		panel.add(textBox);
		return panel;
	}
	
	private void setCenterScreen(JFrame frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
	}

}
