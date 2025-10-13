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

public class InstructionsFrame {
	
	private final JFrame instructions;
	private final Difficulty difficulty;
	
	public InstructionsFrame(Difficulty difficulty) {
		this.difficulty = difficulty;
		this.instructions = createAndShowGUI();
	}
	
	private JFrame createAndShowGUI() {
		JFrame instructions = new JFrame("Instructions");
		instructions.setLayout(new BorderLayout());
		instructions.setSize(400,300);
		instructions.setResizable(false);
		instructions.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		instructions.add(createTitlePanel(), BorderLayout.NORTH);
		instructions.add(createTextPanel(), BorderLayout.CENTER);
		instructions.add(createContinuePanel(), BorderLayout.SOUTH);
		setCenterScreen(instructions);
		instructions.setVisible(true);
		return instructions;
	}
	
	private JPanel createTitlePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Instructions!");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTitleFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createTextPanel() {
		JPanel panel = new JPanel (new BorderLayout());
		JLabel label = new JLabel("<html>Welcome to the Memory Game! Your objective is to memorize the tiles"
								+ " before they are hidden, and match them with the corresponding correct "
								+ "tiles whilst using the least number of turns. Press the 'Continue' button "
								+ "to begin. Have fun and good luck!<html>");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setFont(CharFonts.getTextFont());
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createContinuePanel() {
		JPanel panel = new JPanel(new FlowLayout());
		JButton continueButton = new JButton("Continue");
		continueButton.addActionListener(event -> instructions.dispose());
		continueButton.addActionListener(event -> new LeaderboardFrame(this.difficulty));
		panel.add(continueButton);
		return panel;
	}
	
	private void setCenterScreen(JFrame frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
	}
}
	
