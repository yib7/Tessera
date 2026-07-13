package tessera.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * A recessed "board well": a rounded {@link Theme#SURFACE} plane with a soft
 * inner top shadow so it reads as sunk into the background — the frame the tile
 * grid sits in. Unlike {@link Card} (which is raised), the well is recessed.
 * Holds a layout manager so callers can put the grid (and a pause cover) inside.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class WellPanel extends JPanel {

    public WellPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = Theme.R_WELL;
        int x = 1;
        int y = 1;
        int bw = w - 2;
        int bh = h - 2;

        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x, y, bw, bh, arc, arc);

        // Inner top shadow → the recessed feel.
        Shape clip = new RoundRectangle2D.Float(x, y, bw, bh, arc, arc);
        g2.setClip(clip);
        g2.setPaint(Paint.vGradient(y, y + 26, Paint.alpha(Theme.SHADOW, 30),
                Paint.alpha(Theme.SHADOW, 0)));
        g2.fillRect(x, y, bw, 26);
        g2.setClip(null);

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(x, y, bw - 1, bh - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
