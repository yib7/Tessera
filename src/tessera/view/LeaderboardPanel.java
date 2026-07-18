package tessera.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import tessera.controller.Navigator;
import tessera.model.BoardSize;
import tessera.model.Leaderboard;
import tessera.model.ScoreEntry;
import tessera.model.TileTheme;

/**
 * Shows high scores for one board size at a time, with a segmented tab control to
 * switch sizes. A theme filter narrows the list to one tile theme (or All), and a
 * Top-5/All toggle switches between the celebrated top five and the full retained
 * history so a run that just missed the cut still has a record. A Clear button
 * (with confirmation) wipes the current size. Rebuilds its table from the
 * {@link Leaderboard} each time it is shown so a freshly saved score appears
 * without a restart; an empty result shows a designed empty-state card.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class LeaderboardPanel extends BackgroundPanel {

    private static final String[] COLUMNS = {"#", "Name", "Theme", "Score", "Turns", "Time"};
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";
    private static final String THEME_ALL = "All";

    private final Leaderboard leaderboard;
    private final JTable table = new JTable();
    private final SegmentedPicker sizeTabs;
    private final SegmentedPicker themeTabs;
    private final SegmentedPicker modeTabs;
    private final CardLayout tableCards = new CardLayout();
    private final JPanel tableArea = new JPanel(tableCards);
    private BoardSize current = BoardSize.NORMAL;
    private TileTheme themeFilter = null; // null = All themes
    private boolean allMode = false;      // false = top 5, true = full history

    public LeaderboardPanel(Navigator navigator, Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(22, 44, 26, 44));

        JLabel heading = UiFactory.screenTitle("Leaderboard");
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        String[] sizeLabels = new String[BoardSize.values().length];
        for (int i = 0; i < sizeLabels.length; i++) {
            sizeLabels[i] = BoardSize.values()[i].label();
        }
        sizeTabs = new SegmentedPicker(sizeLabels, current.ordinal());
        sizeTabs.setOnChange(index -> {
            current = BoardSize.values()[index];
            rebuild();
        });

        // Theme filter: All, then one tab per theme.
        String[] themeLabels = new String[TileTheme.values().length + 1];
        themeLabels[0] = THEME_ALL;
        for (int i = 0; i < TileTheme.values().length; i++) {
            themeLabels[i + 1] = TileTheme.values()[i].displayName();
        }
        themeTabs = new SegmentedPicker(themeLabels, 0);
        themeTabs.setOnChange(index -> {
            themeFilter = index == 0 ? null : TileTheme.values()[index - 1];
            rebuild();
        });

        modeTabs = new SegmentedPicker(new String[] {"Top 5", "All"}, 0);
        modeTabs.setOnChange(index -> {
            allMode = index == 1;
            rebuild();
        });

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        controls.add(centered(sizeTabs));
        controls.add(Box.createRigidArea(new Dimension(0, 10)));
        controls.add(centered(themeTabs));
        controls.add(Box.createRigidArea(new Dimension(0, 10)));
        controls.add(centered(modeTabs));
        center.add(controls, BorderLayout.NORTH);

        TableStyle.apply(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        SlimScrollBarUI.apply(scroll);

        tableArea.setOpaque(false);
        tableArea.add(scroll, CARD_TABLE);
        tableArea.add(buildEmptyState(navigator), CARD_EMPTY);
        center.add(tableArea, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        JButton back = UiFactory.secondaryButton("Back to menu");
        back.addActionListener(e -> navigator.show(Navigator.Screen.MENU));
        JButton clear = UiFactory.ghostButton("Clear this board");
        clear.addActionListener(e -> clearCurrentSize());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        south.add(back);
        south.add(clear);
        add(south, BorderLayout.SOUTH);

        // The table is populated by rebuild() on every show (MainWindow.show and
        // showSize), so it is intentionally left empty here to avoid building it
        // twice on first load.
    }

    private static JPanel centered(JComponent control) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);
        wrap.add(control);
        return wrap;
    }

    /** The designed empty state: a dimmed motif, a headline, an invitation, a Play CTA. */
    private JPanel buildEmptyState(Navigator navigator) {
        Card card = new Card();
        card.setLayout(new java.awt.GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JComponent motif = new JComponent() {
            {
                setPreferredSize(new Dimension(80, 80));
                setMaximumSize(new Dimension(80, 80));
                setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                java.awt.Color line = Paint.alpha(Theme.TEXT_DIM, 130);
                Paint.diamond(g2, cx, cy - 16, 13, null, line, 2f);
                Paint.diamond(g2, cx + 16, cy, 13, null, line, 2f);
                Paint.diamond(g2, cx, cy + 16, 13, Paint.alpha(Theme.TEXT_DIM, 60), null, 0f);
                Paint.diamond(g2, cx - 16, cy, 13, null, line, 2f);
                g2.dispose();
            }
        };

        JLabel headline = new JLabel("No scores here yet", SwingConstants.CENTER);
        headline.setFont(Theme.heading());
        headline.setForeground(Theme.TEXT_PRIMARY);
        headline.setAlignmentX(Component.CENTER_ALIGNMENT);
        // A little horizontal slack: BoxLayout hands a bold label exactly its
        // (sometimes 1px-short) preferred width, which clips the last glyph.
        headline.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JLabel invite = UiFactory.muted("Clear a board to set the first time here.");
        invite.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton play = UiFactory.primaryButton("Play");
        play.setAlignmentX(Component.CENTER_ALIGNMENT);
        play.setMaximumSize(new Dimension(180, 46));
        play.addActionListener(e -> navigator.startGame());

        column.add(motif);
        column.add(Box.createRigidArea(new Dimension(0, 16)));
        column.add(headline);
        column.add(Box.createRigidArea(new Dimension(0, 8)));
        column.add(invite);
        column.add(Box.createRigidArea(new Dimension(0, 22)));
        column.add(play);

        card.add(column);
        return card;
    }

    /** Default to a given size when opened from a results screen. */
    public void showSize(BoardSize size) {
        this.current = size;
        sizeTabs.setSelectedIndex(size.ordinal());
        rebuild();
    }

    /** Reload rows for the current size, theme filter, and mode. */
    public void rebuild() {
        List<ScoreEntry> base = allMode
                ? leaderboard.historyFor(current)
                : leaderboard.topFor(current);
        List<ScoreEntry> rows = new ArrayList<>();
        for (ScoreEntry entry : base) {
            if (themeFilter == null || entry.theme() == themeFilter) {
                rows.add(entry);
            }
        }

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);
        int rank = 1;
        for (ScoreEntry entry : rows) {
            String themeText = entry.theme() == null ? "—" : entry.theme().displayName();
            model.addRow(new Object[] {
                    rank++, entry.name(), themeText, entry.score(), entry.turns(),
                    entry.formattedTime()});
        }
        table.setModel(model);
        tableCards.show(tableArea, rows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
    }

    private void clearCurrentSize() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove every saved score for the " + current.label() + " board?\n\n"
                        + "This cannot be undone.",
                "Clear " + current.label() + " scores?",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            leaderboard.clear(current);
            rebuild();
        }
    }
}
