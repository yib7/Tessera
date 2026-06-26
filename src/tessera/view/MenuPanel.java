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
import javax.swing.SwingConstants;

import tessera.controller.Navigator;
import tessera.model.Settings;

/**
 * The start screen. It names the game, shows the current size and theme so the
 * player knows what Play will start, and routes to the other screens. This is
 * the single entry point the old build lacked, where the program dropped you
 * straight into an instructions dialog.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class MenuPanel extends JPanel {

    private final Settings settings;
    private final JLabel subtitle = new JLabel("", SwingConstants.CENTER);

    public MenuPanel(Navigator navigator, Settings settings) {
        this.settings = settings;
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel title = UiFactory.title("Tessera");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = UiFactory.muted("A tile-matching memory game");
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitle.setFont(Theme.body());
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton play = UiFactory.primaryButton("Play");
        play.setAlignmentX(Component.CENTER_ALIGNMENT);
        play.setMaximumSize(new Dimension(220, 44));
        play.addActionListener(e -> navigator.startGame());

        JButton settingsButton = UiFactory.secondaryButton("Settings");
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsButton.setMaximumSize(new Dimension(220, 40));
        settingsButton.addActionListener(e -> navigator.show(Navigator.Screen.SETTINGS));

        JButton leaderboard = UiFactory.secondaryButton("Leaderboard");
        leaderboard.setAlignmentX(Component.CENTER_ALIGNMENT);
        leaderboard.setMaximumSize(new Dimension(220, 40));
        leaderboard.addActionListener(e -> navigator.show(Navigator.Screen.LEADERBOARD));

        JButton quit = UiFactory.secondaryButton("Quit");
        quit.setAlignmentX(Component.CENTER_ALIGNMENT);
        quit.setMaximumSize(new Dimension(220, 40));
        quit.addActionListener(e -> navigator.quit());

        column.add(title);
        column.add(Box.createRigidArea(new Dimension(0, 6)));
        column.add(tagline);
        column.add(Box.createRigidArea(new Dimension(0, 2)));
        column.add(subtitle);
        column.add(Box.createRigidArea(new Dimension(0, 36)));
        column.add(play);
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(settingsButton);
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(leaderboard);
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(quit);

        add(column, new GridBagConstraints());
        refresh();
    }

    /** Refresh the size/theme summary; called when the menu is shown. */
    public void refresh() {
        subtitle.setText(settings.boardSize().label() + " board  -  "
                + settings.tileTheme().displayName() + " tiles");
    }
}
