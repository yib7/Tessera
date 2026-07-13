package tessera.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * De-stocks a {@link JTable} into the theme's look: a surface header with
 * uppercase muted labels and no grid lines, alternating warm row tints, tabular
 * (monospaced, right-aligned) numeric cells, and accent rank badges for the top
 * three. The leaderboard model is {@code {#, Name, Score, Turns, Time}} — rank in
 * column 0, tabular values in columns 2+.
 *
 * <p>Renderers are registered by column class (Object), not per {@code TableColumn},
 * so they survive the {@code setModel(...)} the leaderboard does on every rebuild.
 */
public final class TableStyle {

    private static final int RANK_COLUMN = 0;
    private static final int FIRST_NUMERIC_COLUMN = 2;

    private TableStyle() {
    }

    public static void apply(JTable table) {
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setFont(Theme.body());
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setBackground(Theme.SURFACE);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setDefaultRenderer(Object.class, new CellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 34));
        header.setBackground(Theme.SURFACE);
    }

    /** Header: surface bg, an uppercase letter-spaced muted caption, a bottom hairline. */
    private static final class HeaderRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            JLabel label = new JLabel(value == null ? "" : value.toString().toUpperCase()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(Theme.SURFACE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Theme.BORDER);
                    g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            label.setOpaque(false);
            label.setFont(Theme.tracked(Theme.caption(), 0.12f));
            label.setForeground(Theme.TEXT_MUTED);
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 12));
            label.setHorizontalAlignment(alignmentFor(column));
            return label;
        }
    }

    /**
     * Body cells: alternating warm row tint; column 0 draws a rank badge (accent
     * for the top three); columns 2+ are tabular (mono, right-aligned).
     */
    private static final class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setOpaque(false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 12));
            boolean rank = column == RANK_COLUMN;
            boolean numeric = column >= FIRST_NUMERIC_COLUMN;
            setFont(numeric || rank ? Theme.mono(15, rank) : Theme.body());
            setForeground(Theme.TEXT_PRIMARY);
            setHorizontalAlignment(alignmentFor(column));
            this.row = row;
            this.rankCell = rank;
            return this;
        }

        private int row;
        private boolean rankCell;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Alternating row tint over the surface.
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (row % 2 == 1) {
                g2.setColor(Paint.alpha(Theme.SHADOW, 9));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            if (rankCell && row <= 2) {
                paintRankBadge(g2, row);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        private void paintRankBadge(Graphics2D g2, int row) {
            int[] alphas = {255, 140, 56};
            int d = Math.min(getHeight() - 12, 26);
            int bx = (getWidth() - d) / 2;
            int by = (getHeight() - d) / 2;
            g2.setColor(Paint.alpha(Theme.ACCENT, alphas[row]));
            g2.fillRoundRect(bx, by, d, d, 9, 9);
            // Row 0 rides on solid accent, so its number is drawn light.
            setForeground(row == 0 ? Theme.ACCENT_TEXT : Theme.TEXT_PRIMARY);
        }
    }

    private static int alignmentFor(int column) {
        if (column == RANK_COLUMN) {
            return SwingConstants.CENTER;
        }
        return column >= FIRST_NUMERIC_COLUMN ? SwingConstants.RIGHT : SwingConstants.LEFT;
    }
}
