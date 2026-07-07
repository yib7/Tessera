package tessera.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import tessera.controller.Navigator;
import tessera.model.BoardSize;
import tessera.model.Settings;
import tessera.model.TileTheme;

/**
 * Lets the player choose board size, tile theme, and whether sound cues play.
 * Changes are written to {@link Settings} (and persisted) the moment Save is
 * pressed, and the sound player is updated live so the toggle takes effect
 * without a restart.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class SettingsPanel extends JPanel {

    private final Settings settings;
    private final SoundPlayer sound;
    private final Runnable onSaved;

    private final JComboBox<BoardSize> sizeBox = new JComboBox<>(BoardSize.values());
    private final JComboBox<TileTheme> themeBox = new JComboBox<>(TileTheme.values());
    private final JCheckBox soundBox = new JCheckBox("Sound cues");

    public SettingsPanel(Navigator navigator, Settings settings, SoundPlayer sound,
            Runnable onSaved) {
        this.settings = settings;
        this.sound = sound;
        this.onSaved = onSaved;

        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel heading = UiFactory.heading("Settings");
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        column.add(heading);
        column.add(Box.createRigidArea(new Dimension(0, 28)));
        column.add(row("Board size", sizeBox));
        column.add(Box.createRigidArea(new Dimension(0, 16)));
        column.add(row("Tile theme", themeBox));
        column.add(Box.createRigidArea(new Dimension(0, 16)));

        soundBox.setOpaque(false);
        soundBox.setForeground(Theme.TEXT_PRIMARY);
        soundBox.setFont(Theme.body());
        soundBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(soundBox);
        column.add(Box.createRigidArea(new Dimension(0, 32)));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JButton save = UiFactory.primaryButton("Save");
        save.addActionListener(e -> save(navigator));
        JButton back = UiFactory.secondaryButton("Back");
        back.addActionListener(e -> navigator.show(Navigator.Screen.MENU));
        buttons.add(back);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(save);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(buttons);

        add(column, new GridBagConstraints());
        refresh();
    }

    private JPanel row(String labelText, JComboBox<?> box) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        JLabel label = new JLabel(labelText);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setFont(Theme.label());
        label.setPreferredSize(new Dimension(110, 28));
        box.setMaximumSize(new Dimension(180, 28));
        box.setFont(Theme.body());
        row.add(label);
        row.add(Box.createRigidArea(new Dimension(12, 0)));
        row.add(box);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    /** Sync the controls with the current settings; called when shown. */
    public void refresh() {
        sizeBox.setSelectedItem(settings.boardSize());
        themeBox.setSelectedItem(settings.tileTheme());
        soundBox.setSelected(settings.soundEnabled());
    }

    private void save(Navigator navigator) {
        settings.setBoardSize((BoardSize) sizeBox.getSelectedItem());
        settings.setTileTheme((TileTheme) themeBox.getSelectedItem());
        settings.setSoundEnabled(soundBox.isSelected());
        sound.setEnabled(settings.soundEnabled());
        try {
            settings.save();
        } catch (RuntimeException e) {
            // Persisting preferences is best-effort: the in-memory change still
            // applies for this session, but the player should know it will not
            // survive a restart rather than have the failure vanish silently.
            JOptionPane.showMessageDialog(this,
                    "Your settings could not be saved to disk, so they will not "
                            + "persist after you close Tessera.\n\n" + e.getMessage(),
                    "Settings not saved", JOptionPane.WARNING_MESSAGE);
        }
        if (onSaved != null) {
            onSaved.run();
        }
        navigator.show(Navigator.Screen.MENU);
    }
}
