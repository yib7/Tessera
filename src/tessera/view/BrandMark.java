package tessera.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

/**
 * The painted Tessera logo mark: four rounded squares rotated 45° into a diamond
 * cluster — three in accent shades plus one success green — echoing the tile /
 * tessera motif. Pure geometry, no asset, sized by the caller.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class BrandMark extends JComponent {

    private final int size;

    public BrandMark(int size) {
        this.size = size;
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int s = Math.min(getWidth(), getHeight());
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        double half = s * 0.19;
        double off = s * 0.23;
        tile(g2, cx, cy - off, half, Theme.ACCENT_HI);
        tile(g2, cx + off, cy, half, Theme.ACCENT);
        tile(g2, cx, cy + off, half, Theme.SUCCESS_HI);
        tile(g2, cx - off, cy, half, Theme.ACCENT_LO);
        g2.dispose();
    }

    private void tile(Graphics2D g2, double cx, double cy, double half, java.awt.Color color) {
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(Math.PI / 4);
        int a = (int) Math.round(half * 2);
        int arc = Math.max(4, a / 4);
        g2.setColor(Paint.alpha(Theme.SHADOW, 34));
        g2.fillRoundRect((int) -half + 1, (int) -half + 2, a, a, arc, arc);
        g2.setColor(color);
        g2.fillRoundRect((int) -half, (int) -half, a, a, arc, arc);
        g2.setTransform(old);
    }
}
