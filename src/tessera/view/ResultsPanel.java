package tessera.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import tessera.controller.Navigator;
import tessera.model.GameSession;
import tessera.model.Leaderboard;
import tessera.model.ScoreEntry;

/**
 * Shown when the board is cleared. It reports the run's score, turns, and time,
 * and if the score qualifies for the leaderboard it asks for a name and records
 * it. Name entry only appears when the score actually makes the cut, so a player
 * is not prompted for a run that would not be saved.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class ResultsPanel extends JPanel {

    public ResultsPanel(Navigator navigator, GameSession session, Leaderboard leaderboard) {
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel title = UiFactory.title("Solved");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        long seconds = session.elapsedMillis() / 1000;
        String timeText = String.format("%d:%02d", seconds / 60, seconds % 60);
        JLabel stats = new JLabel(String.format(
                "Score %d   -   %d turns   -   %s   -   %d misses",
                session.score(), session.turns(), timeText, session.mismatches()),
                SwingConstants.CENTER);
        stats.setFont(Theme.hud());
        stats.setForeground(Theme.TEXT_PRIMARY);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);

        column.add(title);
        column.add(Box.createRigidArea(new Dimension(0, 10)));
        column.add(stats);
        column.add(Box.createRigidArea(new Dimension(0, 28)));

        boolean qualifies = leaderboard.qualifies(session.size(), session.score());
        if (qualifies) {
            addNameEntry(navigator, session, leaderboard, column);
        } else {
            addPlainActions(navigator, column);
        }

        add(column, new GridBagConstraints());
    }

    private void addNameEntry(Navigator navigator, GameSession session,
            Leaderboard leaderboard, JPanel column) {
        JLabel prompt = UiFactory.muted("That score makes the leaderboard. Enter a name:");
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField nameField = new JTextField(16);
        nameField.setMaximumSize(new Dimension(260, 32));
        nameField.setFont(Theme.body());
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton submit = UiFactory.primaryButton("Save score");
        submit.setAlignmentX(Component.CENTER_ALIGNMENT);

        Runnable doSubmit = () -> {
            String name = nameField.getText();
            leaderboard.submit(new ScoreEntry(name, session.size(),
                    session.score(), session.turns(), session.elapsedMillis()));
            navigator.show(Navigator.Screen.LEADERBOARD);
        };
        submit.addActionListener(e -> doSubmit.run());
        nameField.addActionListener(e -> doSubmit.run());

        JButton skip = UiFactory.secondaryButton("Skip");
        skip.setAlignmentX(Component.CENTER_ALIGNMENT);
        skip.addActionListener(e -> navigator.show(Navigator.Screen.MENU));

        column.add(prompt);
        column.add(Box.createRigidArea(new Dimension(0, 10)));
        column.add(nameField);
        column.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(skip);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(submit);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(buttons);
    }

    private void addPlainActions(Navigator navigator, JPanel column) {
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        JButton again = UiFactory.primaryButton("Play again");
        again.addActionListener(e -> navigator.startGame());
        JButton menu = UiFactory.secondaryButton("Menu");
        menu.addActionListener(e -> navigator.show(Navigator.Screen.MENU));

        buttons.add(menu);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(again);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(buttons);
    }
}
