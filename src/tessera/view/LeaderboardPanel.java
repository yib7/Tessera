package tessera.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import tessera.controller.Navigator;
import tessera.model.BoardSize;
import tessera.model.Leaderboard;
import tessera.model.ScoreEntry;

/**
 * Shows the top scores for one board size at a time, with buttons to switch
 * between sizes. Rebuilds its table from the {@link Leaderboard} each time it is
 * shown so a freshly saved score appears without a restart.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class LeaderboardPanel extends JPanel {

    private static final String[] COLUMNS = {"#", "Name", "Score", "Turns", "Time"};

    private final Leaderboard leaderboard;
    private final JTable table = new JTable();
    private final JLabel sizeLabel = new JLabel("", SwingConstants.CENTER);
    private BoardSize current = BoardSize.NORMAL;

    public LeaderboardPanel(Navigator navigator, Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 40, 24, 40));

        JLabel heading = UiFactory.heading("Leaderboard");
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        switcher.setOpaque(false);
        for (BoardSize size : BoardSize.values()) {
            JButton tab = UiFactory.secondaryButton(size.label());
            tab.addActionListener(e -> {
                current = size;
                rebuild();
            });
            switcher.add(tab);
        }
        sizeLabel.setFont(Theme.label());
        sizeLabel.setForeground(Theme.TEXT_MUTED);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        switcher.setAlignmentX(Component.CENTER_ALIGNMENT);
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(switcher);
        top.add(Box.createRigidArea(new Dimension(0, 8)));
        top.add(sizeLabel);

        center.add(top, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setFont(Theme.body());
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setBackground(Theme.SURFACE);
        table.setGridColor(Theme.SURFACE_RAISED);
        table.getTableHeader().setReorderingAllowed(false);
        table.setEnabled(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        center.add(scroll, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        JButton back = UiFactory.secondaryButton("Back to menu");
        back.addActionListener(e -> navigator.show(Navigator.Screen.MENU));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(back);
        add(south, BorderLayout.SOUTH);

        rebuild();
    }

    /** Default to a given size when opened from a results screen. */
    public void showSize(BoardSize size) {
        this.current = size;
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
        sizeLabel.setText(top.isEmpty()
                ? current.label() + " board  -  no scores yet"
                : current.label() + " board");
    }
}
