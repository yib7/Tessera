package tessera.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A slim, chromeless scrollbar: a transparent track and a rounded
 * {@link Theme#BORDER_HI} thumb, with no arrow buttons. Replaces the stock
 * light-grey Windows scrollbar in the leaderboard so the table blends into the
 * theme instead of leaking native chrome.
 */
public final class SlimScrollBarUI extends BasicScrollBarUI {

    private static final int THUMB = 10;

    /** Apply the slim UI to both scrollbars of a scroll pane and thin them. */
    public static void apply(JScrollPane scroll) {
        JScrollBar v = scroll.getVerticalScrollBar();
        v.setUI(new SlimScrollBarUI());
        v.setPreferredSize(new Dimension(THUMB + 4, 0));
        v.setOpaque(false);
        JScrollBar hbar = scroll.getHorizontalScrollBar();
        hbar.setUI(new SlimScrollBarUI());
        hbar.setPreferredSize(new Dimension(0, THUMB + 4));
        hbar.setOpaque(false);
    }

    @Override
    protected void configureScrollBarColors() {
        // No stock colours; painting is done in paintThumb/paintTrack.
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private JButton zeroButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // Transparent track.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = 2;
        Color thumb = isDragging || isThumbRollover()
                ? Theme.TEXT_DIM : Theme.BORDER_HI;
        g2.setColor(thumb);
        int arc = THUMB;
        g2.fillRoundRect(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc);
        g2.dispose();
    }
}
