package tessera.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * The shared base for every screen: it paints the warm bone background with a
 * soft radial pool of light behind the content column, so the play area feels
 * lit and the screens share one layered ground instead of a flat fill. A couple
 * of very faint background diamonds echo the tessera motif without competing with
 * the content. Panels extend this and keep their own child components non-opaque
 * so the ground shows through.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public class BackgroundPanel extends JPanel {

    public BackgroundPanel() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // Pool of light, centred a little above the middle where the content sits.
        g2.setPaint(Paint.poolOfLight(w / 2f, h * 0.42f, Math.max(w, h) * 0.72f));
        g2.fillRect(0, 0, w, h);

        // Faint corner motif — barely-there geometry, never a texture.
        g2.setColor(Paint.alpha(Theme.SHADOW, 6));
        int d = Math.max(120, w / 6);
        Paint.diamond(g2, (int) (w * 0.12), (int) (h * 0.16), d, null,
                Paint.alpha(Theme.SHADOW, 8), 2f);
        Paint.diamond(g2, (int) (w * 0.9), (int) (h * 0.82), d, null,
                Paint.alpha(Theme.SHADOW, 8), 2f);
        g2.dispose();
    }
}
