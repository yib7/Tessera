package tessera.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * A small rounded, raised chip: a letter-spaced muted caption over a bold
 * tabular value. It is the HUD's Turns / Score / Time readouts and, in its
 * accent variant, the memorize-phase countdown. Values use the monospaced font
 * so a ticking clock or a growing score never changes width and jitters the row.
 *
 * <p>The accent (countdown) variant is tinted with {@link Theme#ACCENT_TINT} and
 * gently pulses (scale ~1→1.06) while the countdown is on screen. The pulse timer
 * is driven explicitly by {@link #startPulse()}/{@link #stopPulse()} — the owner
 * starts it when the chip is shown and stops it when the countdown ends — so the
 * 60 Hz timer runs only during the few-second countdown, not for the whole game.
 * {@code removeNotify} also stops it as a safety net if the chip is detached
 * mid-pulse.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class Chip extends JComponent {

    private static final int PAD_X = 16;
    private static final int PAD_Y = 9;
    private static final int GAP = 3;

    private final String caption;
    private final boolean accent;
    private final Font captionFont = Theme.tracked(Theme.caption(), 0.14f);
    private final Font valueFont;

    private String value = "";
    private float pulse = 1f;
    private long pulseStartNanos;
    private Timer pulseTimer;

    Chip(String caption, boolean accent) {
        this.caption = caption == null ? "" : caption.toUpperCase();
        this.accent = accent;
        this.valueFont = accent ? Theme.mono(26, true) : Theme.hudValue();
        setOpaque(false);
    }

    /** Update the value shown; re-lays-out in case the value got wider. */
    public void setValue(String value) {
        this.value = value == null ? "" : value;
        revalidate();
        repaint();
    }

    public String caption() {
        return caption;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics cfm = getFontMetrics(captionFont);
        FontMetrics vfm = getFontMetrics(valueFont);
        int textW = Math.max(cfm.stringWidth(caption), vfm.stringWidth(value.isEmpty() ? "00" : value));
        int w = textW + PAD_X * 2;
        int h = cfm.getHeight() + GAP + vfm.getHeight() + PAD_Y * 2;
        return new Dimension(Math.max(w, 72), h);
    }

    /**
     * Begin the accent pulse. Idempotent and a no-op on a non-accent chip. Called
     * by the owner when the countdown chip is shown, so the 60 Hz timer only runs
     * while the countdown is actually on screen.
     */
    public void startPulse() {
        if (!accent || pulseTimer != null) {
            return;
        }
        pulseStartNanos = System.nanoTime();
        pulseTimer = new Timer(16, e -> {
            float t = (System.nanoTime() - pulseStartNanos) / 1_000_000_000f;
            // Breathe between 1.0 and ~1.06 once per ~0.9s.
            pulse = 1f + 0.03f + 0.03f * (float) Math.sin(t * (2 * Math.PI / 0.9));
            repaint();
        });
        pulseTimer.start();
    }

    /** Stop the accent pulse and settle the chip back to its unscaled size. */
    public void stopPulse() {
        if (pulseTimer != null) {
            pulseTimer.stop();
            pulseTimer = null;
        }
        pulse = 1f;
        repaint();
    }

    @Override
    public void removeNotify() {
        // Safety net: if the chip is torn down mid-pulse the timer must not
        // outlive it. Normal lifecycle stops the pulse via stopPulse().
        stopPulse();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (accent && pulse != 1f) {
            AffineTransform old = g2.getTransform();
            g2.translate(w / 2.0, h / 2.0);
            g2.scale(pulse, pulse);
            g2.translate(-w / 2.0, -h / 2.0);
            paintChip(g2, w, h);
            g2.setTransform(old);
        } else {
            paintChip(g2, w, h);
        }
        g2.dispose();
    }

    private void paintChip(Graphics2D g2, int w, int h) {
        int arc = Theme.R_CHIP;
        int x = 1;
        int y = 1;
        int bw = w - 2;
        int bh = h - 4;

        if (accent) {
            Paint.dropShadow(g2, x, y, bw, bh, arc, 2, 26);
            g2.setColor(Theme.ACCENT_TINT);
            g2.fillRoundRect(x, y, bw, bh, arc, arc);
            g2.setColor(Paint.alpha(Theme.ACCENT, 120));
            g2.drawRoundRect(x, y, bw - 1, bh - 1, arc, arc);
        } else {
            Paint.dropShadow(g2, x, y, bw, bh, arc, 2, 26);
            g2.setPaint(Paint.vGradient(y, y + bh, Color.WHITE, new Color(0xF6, 0xF3, 0xEC)));
            g2.fillRoundRect(x, y, bw, bh, arc, arc);
            g2.setColor(Theme.BORDER_HI);
            g2.drawRoundRect(x, y, bw - 1, bh - 1, arc, arc);
            Paint.topHighlight(g2, x, y, bw, bh, arc, 200);
        }

        FontMetrics cfm = g2.getFontMetrics(captionFont);
        FontMetrics vfm = g2.getFontMetrics(valueFont);
        int contentH = cfm.getHeight() + GAP + vfm.getHeight();
        int top = y + (bh - contentH) / 2;

        g2.setFont(captionFont);
        g2.setColor(Theme.TEXT_MUTED);
        drawCentered(g2, caption, w, top + cfm.getAscent());

        g2.setFont(valueFont);
        g2.setColor(accent ? Theme.ACCENT : Theme.TEXT_PRIMARY);
        drawCentered(g2, value, w, top + cfm.getHeight() + GAP + vfm.getAscent());
    }

    private void drawCentered(Graphics2D g2, String text, int w, int baselineY) {
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (w - tw) / 2, baselineY);
    }
}
