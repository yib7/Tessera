package tessera.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import tessera.controller.Navigator;
import tessera.model.GameSession;
import tessera.model.Leaderboard;
import tessera.model.ScoreEntry;

/**
 * Shown when the board is cleared. The run's score is the hero, wrapped in a
 * painted celebration flourish, with turns / time / misses as a chip row. If the
 * score qualifies for the leaderboard it asks for a name and records it; name
 * entry only appears when the score actually makes the cut, so a player is not
 * prompted for a run that would not be saved.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class ResultsPanel extends BackgroundPanel {

    public ResultsPanel(Navigator navigator, GameSession session, Leaderboard leaderboard) {
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel heading = UiFactory.heading("Solved!");
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        HeroScore hero = new HeroScore(session.score());
        hero.setAlignmentX(Component.CENTER_ALIGNMENT);

        long seconds = session.elapsedMillis() / 1000;
        String timeText = String.format("%d:%02d", seconds / 60, seconds % 60);
        JPanel chips = new JPanel();
        chips.setOpaque(false);
        chips.setLayout(new BoxLayout(chips, BoxLayout.X_AXIS));
        chips.add(statChip("Turns", Integer.toString(session.turns())));
        chips.add(Box.createRigidArea(new Dimension(12, 0)));
        chips.add(statChip("Time", timeText));
        chips.add(Box.createRigidArea(new Dimension(12, 0)));
        chips.add(statChip("Misses", Integer.toString(session.mismatches())));
        chips.setAlignmentX(Component.CENTER_ALIGNMENT);

        column.add(heading);
        column.add(Box.createRigidArea(new Dimension(0, 4)));
        column.add(hero);
        column.add(Box.createRigidArea(new Dimension(0, 8)));
        column.add(chips);
        column.add(Box.createRigidArea(new Dimension(0, 28)));

        ScoreEntry candidate = new ScoreEntry("", session.size(), session.theme(),
                session.score(), session.turns(), session.elapsedMillis());
        boolean qualifies = leaderboard.qualifies(candidate);
        if (qualifies) {
            addNameEntry(navigator, session, leaderboard, column);
        } else {
            addPlainActions(navigator, session, column);
        }

        add(column, new GridBagConstraints());
    }

    private static Chip statChip(String caption, String value) {
        Chip chip = UiFactory.hudChip(caption);
        chip.setValue(value);
        return chip;
    }

    private void addNameEntry(Navigator navigator, GameSession session,
            Leaderboard leaderboard, JPanel column) {
        JLabel prompt = UiFactory.muted("That score makes the leaderboard. Enter a name:");
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        PaintedTextField nameField = new PaintedTextField(16);
        nameField.setMaximumSize(new Dimension(280, 44));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);

        JButton submit = UiFactory.primaryButton("Save score");

        Runnable doSubmit = () -> {
            String name = nameField.getText();
            try {
                leaderboard.submit(new ScoreEntry(name, session.size(), session.theme(),
                        session.score(), session.turns(), session.elapsedMillis()));
            } catch (RuntimeException e) {
                // The entry is already on the in-memory board, so the leaderboard
                // screen will still show it this session; but the disk write
                // failed, so warn the player it will not survive a restart rather
                // than let the failure vanish silently (mirrors SettingsPanel).
                JOptionPane.showMessageDialog(this,
                        "Your score could not be saved to disk, so it will not "
                                + "persist after you close Tessera.\n\n" + e.getMessage(),
                        "Score not saved", JOptionPane.WARNING_MESSAGE);
            }
            navigator.show(Navigator.Screen.LEADERBOARD);
        };
        submit.addActionListener(e -> doSubmit.run());
        nameField.addActionListener(e -> doSubmit.run());

        // Leaving without saving discards a leaderboard-worthy run, so confirm
        // first — the player was just told this score makes the board, and
        // "Play again"/"Skip"/"Replay" read as "keep going", not "throw it away".
        JButton skip = UiFactory.ghostButton("Skip");
        skip.addActionListener(e -> {
            if (confirmDiscard()) {
                navigator.show(Navigator.Screen.MENU);
            }
        });

        JButton replay = UiFactory.secondaryButton("Replay board");
        replay.addActionListener(e -> {
            if (confirmDiscard()) {
                navigator.replayGame(session);
            }
        });

        JButton again = UiFactory.secondaryButton("Play again");
        again.addActionListener(e -> {
            if (confirmDiscard()) {
                navigator.startGame();
            }
        });

        column.add(prompt);
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(nameField);
        column.add(Box.createRigidArea(new Dimension(0, 18)));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(skip);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(replay);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(again);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(submit);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(buttons);
    }

    /**
     * Warn before abandoning a run that qualifies for the leaderboard but has not
     * been saved. Returns true if the player confirms the discard.
     */
    private boolean confirmDiscard() {
        int choice = JOptionPane.showConfirmDialog(this,
                "This run makes the leaderboard, but you haven't saved it yet.\n\n"
                        + "Leave without saving? Its score will be lost.",
                "Score not saved", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void addPlainActions(Navigator navigator, GameSession session, JPanel column) {
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        JButton menu = UiFactory.secondaryButton("Menu");
        menu.addActionListener(e -> navigator.show(Navigator.Screen.MENU));
        // No score to lose here, so replay/play-again need no confirmation.
        JButton replay = UiFactory.secondaryButton("Replay board");
        replay.addActionListener(e -> navigator.replayGame(session));
        JButton again = UiFactory.primaryButton("Play again");
        again.addActionListener(e -> navigator.startGame());

        buttons.add(menu);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(replay);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(again);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(buttons);
    }
}
