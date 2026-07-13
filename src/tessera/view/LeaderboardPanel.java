package tessera.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import tessera.controller.Navigator;
import tessera.model.BoardSize;
import tessera.model.Leaderboard;
import tessera.model.ScoreEntry;

/**
 * Shows the top scores for one board size at a time, with a segmented tab control
 * to switch sizes. Rebuilds its table from the {@link Leaderboard} each time it is
 * shown so a freshly saved score appears without a restart. When a size has no
 * scores yet it shows a designed empty-state card instead of a blank table.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class LeaderboardPanel extends BackgroundPanel {

    private static final String[] COLUMNS = {"#", "Name", "Score", "Turns", "Time"};
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";

    private final Leaderboard leaderboard;
    private final JTable table = new JTable();
    private final SegmentedPicker sizeTabs;
    private final CardLayout tableCards = new CardLayout();
    private final JPanel tableArea = new JPanel(tableCards);
    private BoardSize current = BoardSize.NORMAL;

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
        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        switcher.setOpaque(false);
        switcher.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        switcher.add(sizeTabs);
        center.add(switcher, BorderLayout.NORTH);

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
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        south.add(back);
        add(south, BorderLayout.SOUTH);

        // The table is populated by rebuild() on every show (MainWindow.show and
        // showSize), so it is intentionally left empty here to avoid building it
        // twice on first load.
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

        JLabel headline = new JLabel("No scores yet on this board", SwingConstants.CENTER);
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

    /** Reload rows for the current size. */
    public void rebuild() {
        List<ScoreEntry> top = leaderboard.topFor(current);
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);
        int rank = 1;
        for (ScoreEntry entry : top) {
            model.addRow(new Object[] {
                    rank++, entry.name(), entry.score(), entry.turns(),
                    entry.formattedTime()});
        }
        table.setModel(model);
        tableCards.show(tableArea, top.isEmpty() ? CARD_EMPTY : CARD_TABLE);
    }
}
