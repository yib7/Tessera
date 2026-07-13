package tessera.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * The board-well cover shown while paused. Painted as an intentional screen — a
 * dimmed surface veil, a painted two-bar pause glyph in an accent disc, a
 * "Paused" heading and a resume hint — not a blank rectangle. It is shown by the
 * board's CardLayout in place of the grid, so the tiles beneath are not drawn at
 * all: the anti-peek rule (no free study time while the clock is frozen) holds by
 * construction, not by opacity.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class PauseCover extends JPanel {

    public PauseCover() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = Theme.R_WELL;

        // Dimmed surface veil filling the well.
        g2.setColor(Paint.alpha(Theme.SURFACE, 244));
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
        g2.setColor(Paint.alpha(Theme.SHADOW, 10));
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        int cx = w / 2;
        int cy = h / 2 - 24;

        // Accent disc + two-bar pause glyph.
        int disc = 92;
        g2.setColor(Theme.ACCENT_TINT);
        g2.fillOval(cx - disc / 2, cy - disc / 2, disc, disc);
        g2.setColor(Theme.ACCENT);
        int barW = 12;
        int barH = 40;
        int gap = 12;
        g2.fillRoundRect(cx - gap / 2 - barW, cy - barH / 2, barW, barH, 5, 5);
        g2.fillRoundRect(cx + gap / 2, cy - barH / 2, barW, barH, 5, 5);

        // Heading + hint.
        g2.setFont(Theme.heading());
        g2.setColor(Theme.TEXT_PRIMARY);
        drawCentered(g2, "Paused", w, cy + disc / 2 + 34);
        g2.setFont(Theme.body());
        g2.setColor(Theme.TEXT_MUTED);
        drawCentered(g2, "Press Escape or Resume to continue", w, cy + disc / 2 + 60);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int w, int baselineY) {
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (w - tw) / 2, baselineY);
    }
}
