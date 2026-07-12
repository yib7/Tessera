package tessera.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A single card, painted by hand. It renders three states (face down, face up,
 * matched) and animates the change between down and up as a horizontal squash:
 * the tile scales its width to zero, swaps which side it shows at the midpoint,
 * then scales back out. That is the closest a 2D Swing component gets to a card
 * flip without a 3D pipeline, and it runs on a Swing Timer so it stays on the
 * event dispatch thread.
 *
 * <p>The component carries only presentation state (the glyph to show, hover,
 * the animation phase). The logical state of the card lives in the model.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class TileButton extends JComponent {

    /** Where in the flip we are. */
    private enum Flip { NONE, TO_UP, TO_DOWN }

    private final int row;
    private final int col;
    private final Color accent;

    private String glyph = "";
    private boolean showingFace = false;
    private boolean matched = false;
    private boolean hovered = false;
    private boolean focused = false;
    private boolean interactive = true;

    private Flip flip = Flip.NONE;
    private float progress = 1f; // 0 = edge-on, 1 = fully open
    private Timer animTimer;
    private Runnable onClick;

    public TileButton(int row, int col, Color accent) {
        this.row = row;
        this.col = col;
        this.accent = accent;
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(72, 72));
        installMouse();
        installKeyboard();
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        setCursor(Cursor.getPredefinedCursor(
                interactive ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Set the face value shown when face up. */
    public void setGlyph(String glyph) {
        this.glyph = glyph == null ? "" : glyph;
    }

    /** Snap to a state with no animation (used when dealing or resetting). */
    public void setStateImmediate(boolean faceUp, boolean matched) {
        stopAnim();
        this.showingFace = faceUp;
        this.matched = matched;
        this.progress = 1f;
        this.flip = Flip.NONE;
        repaint();
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
        repaint();
    }

    /** Animate a flip to face up, then run the callback when the open completes. */
    public void flipUp(Runnable whenDone) {
        startFlip(Flip.TO_UP, whenDone);
    }

    /** Animate a flip back to face down. */
    public void flipDown(Runnable whenDone) {
        startFlip(Flip.TO_DOWN, whenDone);
    }

    private void startFlip(Flip target, Runnable whenDone) {
        stopAnim();
        this.flip = target;
        this.progress = 1f; // start fully open, squash inward first
        final boolean[] swapped = {false};
        final int stepMs = 16;
        final float speed = 0.16f; // progress units per frame; ~6 frames each way
        animTimer = new Timer(stepMs, e -> {
            if (!swapped[0]) {
                progress -= speed;
                if (progress <= 0f) {
                    progress = 0f;
                    swapped[0] = true;
                    showingFace = (target == Flip.TO_UP);
                }
            } else {
                progress += speed;
                if (progress >= 1f) {
                    progress = 1f;
                    stopAnim();
                    flip = Flip.NONE;
                    if (whenDone != null) {
                        whenDone.run();
                    }
                }
            }
            repaint();
        });
        animTimer.start();
    }

    /**
     * Stop any running flip animation without invoking its completion callback.
     * Called during panel teardown so a Timer cannot fire {@code repaint()} or a
     * {@code whenDone} callback against a component that has been removed.
     */
    public void stopAnimation() {
        stopAnim();
    }

    private void stopAnim() {
        if (animTimer != null) {
            animTimer.stop();
            animTimer = null;
        }
    }

    private void installMouse() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Fire on press rather than click: mouseClicked only fires when
                // press and release land on the same pixel, so a 1-2 px hand
                // tremor between the two would silently drop the input.
                if (interactive && onClick != null && SwingUtilities.isLeftMouseButton(e)) {
                    onClick.run();
                }
            }
        });
    }

    /**
     * Make the tile keyboard-operable: a focus ring (reusing the hover paint) so
     * the focused tile is visible, and Enter/Space to flip it, mirroring the
     * mouse-press activation. GamePanel wires the arrow keys for grid traversal.
     */
    private void installKeyboard() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if ((k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER)
                        && interactive && onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Apply the horizontal squash about the centre to fake the flip.
        int squashedWidth = Math.max(2, Math.round(w * progress));
        int x = (w - squashedWidth) / 2;

        int arc = Math.max(10, Math.min(w, h) / 6);

        if (matched) {
            g2.setColor(Theme.TILE_MATCHED);
            g2.fillRoundRect(x, 0, squashedWidth, h, arc, arc);
            g2.setColor(Theme.TILE_MATCHED_BORDER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x + 1, 1, squashedWidth - 2, h - 2, arc, arc);
            drawGlyph(g2, w, h, accent);
        } else if (showingFace) {
            g2.setColor(Theme.TILE_FACE);
            g2.fillRoundRect(x, 0, squashedWidth, h, arc, arc);
            drawGlyph(g2, w, h, accent.darker());
        } else {
            Color back = (hovered || focused) && interactive ? Theme.TILE_BACK_HOVER : Theme.TILE_BACK;
            g2.setColor(back);
            g2.fillRoundRect(x, 0, squashedWidth, h, arc, arc);
            // A subtle inset diamond as the card back motif, scaled with the squash.
            if (progress > 0.4f) {
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                int cx = w / 2;
                int cy = h / 2;
                int d = Math.min(squashedWidth, h) / 5;
                int[] xs = {cx, cx + d, cx, cx - d};
                int[] ys = {cy - d, cy, cy + d, cy};
                g2.drawPolygon(xs, ys, 4);
            }
        }
        g2.dispose();
    }

    private void drawGlyph(Graphics2D g2, int w, int h, Color color) {
        // Hide the glyph while the tile is edge-on so it does not smear.
        if (progress < 0.5f || glyph.isEmpty()) {
            return;
        }
        Font font = Theme.tileFont(Math.min(w, h));
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(glyph);
        int textX = (w - textW) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(glyph, textX, textY);
    }
}
