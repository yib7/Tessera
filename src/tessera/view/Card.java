package tessera.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * A raised surface card: a rounded {@link Theme#SURFACE} panel with a warm
 * hairline border, a painted drop shadow and a top-edge highlight. Groups
 * related content (a settings form, a results summary, the leaderboard
 * empty-state) into one framed plane above the background. Children are laid out
 * normally on top; keep them non-opaque so the card surface shows through.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class Card extends JPanel {

    private final int arc;

    public Card() {
        this(Theme.R_WELL);
    }

    public Card(int arc) {
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int x = 2;
        int y = 2;
        int bw = w - 4;
        int bh = h - 6;
        Paint.dropShadow(g2, x, y, bw, bh, arc, 4, 30);
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x, y, bw, bh, arc, arc);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(x, y, bw - 1, bh - 1, arc, arc);
        Paint.topHighlight(g2, x, y, bw, bh, arc, 180);
        g2.dispose();
        super.paintComponent(g);
    }
}
