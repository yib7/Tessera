package tessera.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import tessera.controller.Navigator;
import tessera.model.BoardSize;
import tessera.model.Settings;
import tessera.model.TileTheme;

/**
 * Lets the player choose board size, tile theme, and whether sound cues play,
 * using the de-stocked controls (a segmented picker, a custom dropdown and a
 * toggle) in a raised surface card. Changes are written to {@link Settings} (and
 * persisted) the moment Save is pressed, and the sound player is updated live so
 * the toggle takes effect without a restart.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class SettingsPanel extends BackgroundPanel {

    private final Settings settings;
    private final SoundPlayer sound;
    private final Runnable onSaved;

    private final SegmentedPicker sizePicker;
    private final Dropdown<TileTheme> themePicker;
    private final ToggleSwitch soundToggle;

    public SettingsPanel(Navigator navigator, Settings settings, SoundPlayer sound,
            Runnable onSaved) {
        this.settings = settings;
        this.sound = sound;
        this.onSaved = onSaved;

        setLayout(new GridBagLayout());

        String[] sizeLabels = new String[BoardSize.values().length];
        for (int i = 0; i < sizeLabels.length; i++) {
            sizeLabels[i] = BoardSize.values()[i].label();
        }
        sizePicker = new SegmentedPicker(sizeLabels, settings.boardSize().ordinal());
        themePicker = new Dropdown<>(TileTheme.values(), TileTheme::displayName,
                settings.tileTheme().ordinal());
        soundToggle = new ToggleSwitch(settings.soundEnabled());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel heading = UiFactory.screenTitle("Settings");
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        Card card = new Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(26, 30, 28, 30));
        card.setMaximumSize(new Dimension(460, 320));
        card.add(row("Board size", sizePicker));
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(row("Tile theme", themePicker));
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(row("Sound cues", soundToggle));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JButton back = UiFactory.secondaryButton("Back");
        back.addActionListener(e -> navigator.show(Navigator.Screen.MENU));
        JButton save = UiFactory.primaryButton("Save");
        save.addActionListener(e -> save(navigator));
        buttons.add(back);
        buttons.add(Box.createRigidArea(new Dimension(12, 0)));
        buttons.add(save);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);

        column.add(heading);
        column.add(Box.createRigidArea(new Dimension(0, 24)));
        column.add(card);
        column.add(Box.createRigidArea(new Dimension(0, 26)));
        column.add(buttons);

        add(column, new GridBagConstraints());
        refresh();
    }

    /** One labelled control row: a fixed-width label on the left, the control on the right. */
    private JPanel row(String labelText, JComponent control) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        JLabel label = new JLabel(labelText);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setFont(Theme.label());
        label.setPreferredSize(new Dimension(120, 30));
        label.setMaximumSize(new Dimension(120, 46));
        row.add(label);
        row.add(Box.createRigidArea(new Dimension(16, 0)));
        control.setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(control);
        row.add(Box.createHorizontalGlue());
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(400, 48));
        return row;
    }

    /** Sync the controls with the current settings; called when shown. */
    public void refresh() {
        sizePicker.setSelectedIndex(settings.boardSize().ordinal());
        themePicker.setSelected(settings.tileTheme());
        soundToggle.setOn(settings.soundEnabled());
    }

    private void save(Navigator navigator) {
        settings.setBoardSize(BoardSize.values()[sizePicker.getSelectedIndex()]);
        settings.setTileTheme(themePicker.getSelected());
        settings.setSoundEnabled(soundToggle.isOn());
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
