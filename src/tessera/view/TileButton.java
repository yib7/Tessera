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
 * A single card, painted by hand. It renders the full tile state sheet — face
 * down, face-down hover, keyboard focus, face up, matched, and a mismatch error
 * flash — and animates the change between down and up as a horizontal squash to
 * edge-on, swapping which side it shows at the midpoint, then opening back out.
 * A brightness veil darkens the body through the turn so it reads as a card
 * flipping rather than a slab being squeezed. That is the closest a 2D Swing
 * component gets to a card flip without a 3D pipeline, and it runs on a Swing
 * {@link Timer} so it stays on the event dispatch thread.
 *
 * <p>The body is painted inside a small margin of the component bounds; that
 * margin is where the painted under-shadow and the match/mismatch glow live, so
 * the tile reads as a physical object with edges and weight rather than a flat
 * fill. Depth recipes come from {@link Paint}; colours from {@link Theme}.
 *
 * <p>The component carries only presentation state (the glyph to show, hover,
 * focus, the animation phase). The logical state of the card lives in the model.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class TileButton extends JComponent {

    /** Which motion, if any, is currently animating. At most one runs at a time. */
    private enum Motion { NONE, FLIP, PULSE, SHAKE }

    /** Flip direction, meaningful only while {@link Motion#FLIP} runs. */
    private enum Flip { NONE, TO_UP, TO_DOWN }

    private static final int FLIP_MS = 220;
    private static final int PULSE_MS = 500;
    private static final int SHAKE_MS = 360;
    private static final int STEP_MS = 16;

    private final int row;
    private final int col;
    private final Color accent;

    private String glyph = "";
    private boolean showingFace = false;
    private boolean matched = false;
    private boolean hovered = false;
    private boolean focused = false;
    private boolean interactive = true;

    // Animation state. One Timer drives whichever motion is active; startNanos is
    // the wall-clock start so easing is time-based (frame-rate independent).
    private Motion motion = Motion.NONE;
    private Flip flipDir = Flip.NONE;
    private long startNanos;
    private boolean flipSwapped;
    private float openness = 1f;       // flip: 1 = fully open, 0 = edge-on
    private float pulse = 0f;          // match pulse progress 0..1 (0 = none)
    private boolean mismatchActive;    // red error border shown until the flip-back
    private float shakeX = 0f;         // current horizontal mismatch-shake offset
    private Timer animTimer;
    private Runnable onFlipDone;

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

    private Runnable onClick;

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
        this.openness = 1f;
        this.mismatchActive = false;
        this.shakeX = 0f;
        this.pulse = 0f;
        repaint();
    }

    /**
     * Mark the tile matched. When it becomes matched through gameplay this also
     * plays a one-shot green pulse; snapping via {@link #setStateImmediate} does
     * not, so dealing and resets stay silent.
     */
    public void setMatched(boolean matched) {
        boolean becameMatched = matched && !this.matched;
        this.matched = matched;
        this.mismatchActive = false;
        if (becameMatched) {
            startPulse();
        } else {
            repaint();
        }
    }

    /**
     * Flash this tile as a mismatch: an error-coloured border plus a short
     * horizontal shake. The border stays until the tile next flips down (the
     * controller flips both mismatched tiles back after the pause), so the player
     * sees which pair was wrong for the whole pause, not just the shake.
     */
    public void showMismatch() {
        stopAnim();
        mismatchActive = true;
        shakeX = 0f;
        motion = Motion.SHAKE;
        startNanos = System.nanoTime();
        startTimer();
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
        // A flip clears any lingering mismatch flash: the flip-back is exactly the
        // moment the error state should end.
        mismatchActive = false;
        shakeX = 0f;
        motion = Motion.FLIP;
        flipDir = target;
        flipSwapped = false;
        openness = 1f; // start fully open, squash inward first
        onFlipDone = whenDone;
        startNanos = System.nanoTime();
        startTimer();
    }

    private void startPulse() {
        stopAnim();
        motion = Motion.PULSE;
        pulse = 0f;
        startNanos = System.nanoTime();
        startTimer();
    }

    private void startTimer() {
        animTimer = new Timer(STEP_MS, e -> tick());
        animTimer.start();
        repaint();
    }

    private void tick() {
        float ms = (System.nanoTime() - startNanos) / 1_000_000f;
        switch (motion) {
            case FLIP -> tickFlip(ms);
            case PULSE -> tickPulse(ms);
            case SHAKE -> tickShake(ms);
            case NONE -> stopAnim();
        }
        repaint();
    }

    private void tickFlip(float ms) {
        float p = clamp01(ms / FLIP_MS);
        float e = easeInOut(p);
        // openness 1 -> 0 (edge-on) -> 1; swap the shown face at the midpoint.
        openness = Math.abs(1f - 2f * e);
        if (!flipSwapped && e >= 0.5f) {
            flipSwapped = true;
            showingFace = (flipDir == Flip.TO_UP);
        }
        if (p >= 1f) {
            openness = 1f;
            Runnable done = onFlipDone;
            onFlipDone = null;
            stopAnim();
            if (done != null) {
                done.run();
            }
        }
    }

    private void tickPulse(float ms) {
        float p = clamp01(ms / PULSE_MS);
        pulse = p;
        if (p >= 1f) {
            pulse = 0f;
            stopAnim();
        }
    }

    private void tickShake(float ms) {
        float p = clamp01(ms / SHAKE_MS);
        // Decaying wobble: ~3 cycles over the duration, amplitude fading to zero.
        shakeX = (float) (6.0 * Math.sin(3.0 * 2.0 * Math.PI * p) * (1.0 - p));
        if (p >= 1f) {
            shakeX = 0f;
            // Keep mismatchActive true: the red border persists until the flip-back.
            stopAnim();
        }
    }

    /**
     * Stop any running animation without invoking its completion callback. Called
     * during panel teardown so a Timer cannot fire {@code repaint()} or a
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
        motion = Motion.NONE;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** Ease-in-out on [0,1]: 3p² − 2p³. */
    private static float easeInOut(float p) {
        return p * p * (3f - 2f * p);
    }

    /** Ease-out on [0,1]: 1 − (1−p)². */
    private static float easeOut(float p) {
        float inv = 1f - p;
        return 1f - inv * inv;
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
     * Make the tile keyboard-operable: focus is drawn as a distinct accent ring
     * (see {@link Paint#focusRing}, not the hover paint) so the focused tile is
     * unmistakable, and Enter/Space flip it, mirroring the mouse-press activation.
     * GamePanel wires the arrow keys for grid traversal.
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
        int pad = Math.max(6, Math.round(Math.min(w, h) * 0.06f));
        int bw = Math.max(2, w - 2 * pad);
        int bh = Math.max(2, h - 2 * pad);
        int arc = Math.min(Theme.R_TILE, Math.min(bw, bh) / 3);

        boolean useHover = hovered && interactive && !focused && !showingFace && !matched;
        int lift = useHover ? 2 : 0;

        // Horizontal squash for the flip; body is centred in the padded box.
        int squashedW = Math.max(2, Math.round(bw * openness));
        int bx = pad + (bw - squashedW) / 2 + Math.round(shakeX);
        int by = pad - lift;

        // Under-shadow (skip while edge-on so the shadow doesn't flash mid-flip).
        if (openness > 0.6f) {
            Paint.dropShadow(g2, bx, pad, squashedW, bh, arc, useHover ? 5 : 3, 34);
        }

        if (matched) {
            paintMatched(g2, bx, by, squashedW, bh, arc, w, h);
        } else if (mismatchActive) {
            paintMismatch(g2, bx, by, squashedW, bh, arc, w, h);
        } else if (showingFace) {
            paintFace(g2, bx, by, squashedW, bh, arc, w, h);
        } else {
            paintBack(g2, bx, by, squashedW, bh, arc, w, h, useHover);
        }

        // Brightness veil: darken the body as it turns edge-on, easing back to
        // clear at the open ends. Sells the flip as a turn rather than a squash.
        if (motion == Motion.FLIP && openness < 1f) {
            int veil = Math.round(102f * (1f - openness));
            g2.setColor(Paint.alpha(Theme.SHADOW, veil));
            g2.fillRoundRect(bx, by, squashedW, bh, arc, arc);
        }

        // Keyboard focus sits on top of any fill, always inside the bounds.
        if (focused && interactive) {
            Paint.focusRing(g2, bx, by, squashedW, bh, arc);
        }

        g2.dispose();
    }

    private void paintBack(Graphics2D g2, int x, int y, int w, int h, int arc,
            int fullW, int fullH, boolean hover) {
        Color top = hover ? new Color(0xF5, 0xF7, 0xFE) : Theme.TB_TOP;
        Color bot = hover ? new Color(0xE6, 0xEC, 0xFB) : Theme.TB_BOT;
        g2.setPaint(Paint.vGradient(y, y + h, top, bot));
        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.setColor(hover ? Paint.alpha(Theme.ACCENT, 90) : Theme.TB_BRD);
        g2.setStroke(new BasicStroke(Theme.STROKE_HAIR));
        g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        Paint.topHighlight(g2, x, y, w, h, arc, 150);
        // Diamond motif, scaled with the squash so it collapses through the flip.
        if (openness > 0.4f) {
            int cx = x + w / 2;
            int cy = y + h / 2;
            int d = Math.min(w, h) / 5;
            int motifAlpha = hover ? 65 : 100;
            Paint.diamond(g2, cx, cy, d,
                    Paint.alpha(Theme.ACCENT, Math.round(motifAlpha * 1.4f)),
                    Paint.alpha(Theme.ACCENT, Math.round(motifAlpha * 1.07f)), 2f);
        }
    }

    private void paintFace(Graphics2D g2, int x, int y, int w, int h, int arc,
            int fullW, int fullH) {
        g2.setPaint(Paint.vGradient(y, y + h, Theme.FACE_TOP, Theme.FACE_BOT));
        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.setColor(new Color(0xE7, 0xE3, 0xD9));
        g2.setStroke(new BasicStroke(Theme.STROKE_HAIR));
        g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        Paint.topHighlight(g2, x, y, w, h, arc, 230);
        // Per-theme accent keeps each tile theme's face identity; FACE_GLYPH is the
        // documented default when a caller has no theme accent of its own.
        drawGlyph(g2, x, y, w, h, accent);
    }

    private void paintMatched(Graphics2D g2, int x, int y, int w, int h, int arc,
            int fullW, int fullH) {
        Paint.glow(g2, x, y, w, h, arc, Theme.SUCCESS_HI, 3);
        g2.setPaint(Paint.vGradient(y, y + h, new Color(0xEE, 0xF9, 0xF0), Theme.TILE_MATCHED));
        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.setColor(Theme.SUCCESS_HI);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
        drawGlyph(g2, x, y, w, h, Theme.MATCHED_GLYPH);
        // One-shot celebratory ring, expanding and fading. Capped to the free
        // margin on each side so the outermost radius is never clipped.
        if (pulse > 0f) {
            float e = easeOut(pulse);
            int maxRing = Math.max(0, Math.min(x, Math.min(fullW - (x + w), 8)));
            int ring = Math.round(maxRing * e);
            g2.setColor(Paint.alpha(Theme.SUCCESS_HI, Math.round(200f * (1f - pulse))));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(x - ring, y - ring, w - 1 + 2 * ring, h - 1 + 2 * ring,
                    arc + ring, arc + ring);
        }
    }

    private void paintMismatch(Graphics2D g2, int x, int y, int w, int h, int arc,
            int fullW, int fullH) {
        Paint.glow(g2, x, y, w, h, arc, Theme.ERROR, 3);
        // The mismatched tile is face-up (the player just saw its glyph); tint the
        // face faintly warm and ring it in the error colour.
        g2.setPaint(Paint.vGradient(y, y + h, Theme.FACE_TOP, new Color(0xFB, 0xEC, 0xEC)));
        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.setColor(Theme.ERROR);
        g2.setStroke(new BasicStroke(Theme.STROKE_ERROR));
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
        // Keep the tile's own glyph; the error is carried by the border, glow and
        // shake (the brief's "error-colour border flash"), so the tile stays itself.
        drawGlyph(g2, x, y, w, h, accent);
    }

    private void drawGlyph(Graphics2D g2, int x, int y, int w, int h, Color color) {
        // Hide the glyph while the tile is edge-on so it does not smear.
        if (openness < 0.5f || glyph.isEmpty()) {
            return;
        }
        Font font = Theme.tileFont(Math.min(w, h));
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(glyph);
        int textX = x + (w - textW) / 2;
        int textY = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(glyph, textX, textY);
    }

    // --- Render/QA seam: state injectors an offscreen render pass uses to compose
    // static frames (hover, focus, a frozen mid-flip, a match pulse, a mismatch)
    // without driving the real animation timers. They only set presentation flags
    // and repaint; they have no effect on interactive play. ---

    /** Harness only: set the hover flag for a static capture. */
    public void debugSetHovered(boolean hovered) {
        this.hovered = hovered;
        repaint();
    }

    /** Harness only: set the focus flag for a static capture. */
    public void debugSetFocused(boolean focused) {
        this.focused = focused;
        repaint();
    }

    /** Harness only: freeze a mid-flip frame (openness in [0,1], face shown or not). */
    public void debugSetFlipFrame(float openness, boolean faceUp) {
        stopAnim();
        this.motion = Motion.FLIP;
        this.openness = clamp01(openness);
        this.showingFace = faceUp;
        repaint();
    }

    /** Harness only: freeze the matched tile at a point in its green pulse (0..1). */
    public void debugSetMatchPulse(float pulse) {
        stopAnim();
        this.matched = true;
        this.showingFace = true;
        this.mismatchActive = false;
        this.openness = 1f;
        this.pulse = clamp01(pulse);
        repaint();
    }

    /** Harness only: freeze the mismatch flash, sampling the shake wobble at (0..1). */
    public void debugSetMismatch(float shakeProgress) {
        stopAnim();
        this.mismatchActive = true;
        this.matched = false;
        this.showingFace = true;
        this.openness = 1f;
        float p = clamp01(shakeProgress);
        this.shakeX = (float) (6.0 * Math.sin(3.0 * 2.0 * Math.PI * p) * (1.0 - p));
        repaint();
    }
}
