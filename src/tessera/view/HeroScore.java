package tessera.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

/**
 * The results-screen hero: the run's score as a large tabular numeral inside a
 * painted celebration flourish — an accent ring with radiating geometric confetti
 * (small squares and diamonds in the accent trio plus success green) — and a
 * small "SCORE" caption beneath. Pure geometry, no asset; a still frame, so there
 * is no timer to tear down.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class HeroScore extends JComponent {

    private final String score;

    public HeroScore(int score) {
        this.score = Integer.toString(score);
        setOpaque(false);
        setPreferredSize(new Dimension(420, 250));
        setMaximumSize(new Dimension(420, 250));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2 - 6;

        Color[] palette = {Theme.ACCENT, Theme.ACCENT_HI, Theme.ACCENT_LO, Theme.SUCCESS_HI};
        int ringR = Math.min(cx, cy) - 8;

        // Radiating confetti just outside the ring.
        int confettiR = ringR + 22;
        for (int i = 0; i < 18; i++) {
            double ang = i * (2 * Math.PI / 18) + 0.15;
            int px = cx + (int) (Math.cos(ang) * confettiR);
            int py = cy + (int) (Math.sin(ang) * confettiR * 0.62); // squashed so it fits
            Color color = Paint.alpha(palette[i % palette.length], 165);
            if (i % 2 == 0) {
                Paint.diamond(g2, px, py, 6, color, null, 0f);
            } else {
                AffineTransform old = g2.getTransform();
                g2.translate(px, py);
                g2.rotate(ang);
                g2.setColor(color);
                g2.fillRoundRect(-5, -5, 10, 10, 3, 3);
                g2.setTransform(old);
            }
        }

        // Accent ring behind the numeral.
        g2.setColor(Paint.alpha(Theme.ACCENT, 55));
        g2.setStroke(new java.awt.BasicStroke(3f));
        g2.drawOval(cx - ringR, cy - (int) (ringR * 0.72), ringR * 2, (int) (ringR * 1.44));

        // Score numeral.
        g2.setFont(Theme.scoreHero());
        g2.setColor(Theme.ACCENT);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(score);
        g2.drawString(score, cx - tw / 2, cy + fm.getAscent() / 2 - 6);

        // Caption.
        g2.setFont(Theme.tracked(Theme.caption(), 0.2f));
        g2.setColor(Theme.TEXT_MUTED);
        FontMetrics cfm = g2.getFontMetrics();
        int cw = cfm.stringWidth("SCORE");
        g2.drawString("SCORE", cx - cw / 2, cy + ringR - 2);
        g2.dispose();
    }
}
