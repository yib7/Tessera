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

import tessera.controller.Navigator;
import tessera.model.Settings;

/**
 * The start screen. It names the game with a painted brand mark, shows the
 * current size and theme as a summary pill so the player knows what Play will
 * start, and routes to the other screens. This is the single entry point the old
 * build lacked, where the program dropped you straight into an instructions
 * dialog.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class MenuPanel extends BackgroundPanel {

    private final Settings settings;
    private final JLabel summary;

    public MenuPanel(Navigator navigator, Settings settings) {
        this.settings = settings;
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        BrandMark mark = new BrandMark(72);
        mark.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = UiFactory.title("Tessera");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = UiFactory.muted("A tile-matching memory game");
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        summary = UiFactory.pill("");
        summary.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton play = UiFactory.primaryButton("Play");
        play.addActionListener(e -> navigator.startGame());

        JButton settingsButton = UiFactory.secondaryButton("Settings");
        settingsButton.addActionListener(e -> navigator.show(Navigator.Screen.SETTINGS));

        JButton leaderboard = UiFactory.secondaryButton("Leaderboard");
        leaderboard.addActionListener(e -> navigator.show(Navigator.Screen.LEADERBOARD));

        JButton quit = UiFactory.ghostButton("Quit");
        quit.addActionListener(e -> navigator.quit());

        for (JButton b : new JButton[] {play, settingsButton, leaderboard, quit}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(240, 48));
        }

        column.add(mark);
        column.add(Box.createRigidArea(new Dimension(0, 14)));
        column.add(title);
        column.add(Box.createRigidArea(new Dimension(0, 6)));
        column.add(tagline);
        column.add(Box.createRigidArea(new Dimension(0, 14)));
        column.add(summary);
        column.add(Box.createRigidArea(new Dimension(0, 36)));
        column.add(play);
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(settingsButton);
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(leaderboard);
        column.add(Box.createRigidArea(new Dimension(0, 8)));
        column.add(quit);

        add(column, new GridBagConstraints());
        refresh();
    }

    /** Refresh the size/theme summary; called when the menu is shown. */
    public void refresh() {
        summary.setText(settings.boardSize().label() + " board   ·   "
                + settings.tileTheme().displayName() + " tiles");
        summary.revalidate();
    }
}
