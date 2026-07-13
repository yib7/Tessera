package tessera.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * A de-stocked on/off switch: a pill track (accent gradient when on, inset track
 * when off) with a raised round knob that slides between the two ends. Replaces a
 * {@link javax.swing.JCheckBox} for a boolean preference (sound cues). The knob
 * slide is a short timer-driven ease so the state change reads as a motion.
 */
public final class ToggleSwitch extends JComponent {

    private static final int W = 54;
    private static final int H = 30;
    private static final int SLIDE_MS = 150;

    private boolean on;
    private float knob;          // 0 = off (left), 1 = on (right)
    private boolean focused;
    private Consumer<Boolean> onChange;
    private Timer anim;
    private long animStart;
    private float animFrom;

    public ToggleSwitch(boolean on) {
        this.on = on;
        this.knob = on ? 1f : 0f;
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installListeners();
    }

    public boolean isOn() {
        return on;
    }

    /** Set the state without firing the change listener (for initial sync). */
    public void setOn(boolean on) {
        if (this.on != on) {
            this.on = on;
            animateTo(on ? 1f : 0f);
        }
    }

    public void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }

    private void toggle() {
        on = !on;
        animateTo(on ? 1f : 0f);
        if (onChange != null) {
            onChange.accept(on);
        }
    }

    private void animateTo(float target) {
        if (anim != null) {
            anim.stop();
        }
        animFrom = knob;
        animStart = System.nanoTime();
        anim = new Timer(16, e -> {
            float p = (System.nanoTime() - animStart) / 1_000_000f / SLIDE_MS;
            if (p >= 1f) {
                knob = target;
                anim.stop();
                anim = null;
            } else {
                float e2 = p * p * (3f - 2f * p); // ease-in-out
                knob = animFrom + (target - animFrom) * e2;
            }
            repaint();
        });
        anim.start();
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                toggle();
            }
        });
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
                if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) {
                    toggle();
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(W, H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int trackH = Math.min(h, H);
        int y = (h - trackH) / 2;
        int arc = trackH;

        // Track: accent gradient when on, inset when off (cross-fade by knob pos).
        if (knob > 0f) {
            g2.setPaint(Paint.vGradient(y, y + trackH, Theme.ACCENT_HI, Theme.ACCENT));
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, knob))));
            g2.fillRoundRect(0, y, W, trackH, arc, arc);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
        }
        if (knob < 1f) {
            g2.setColor(Theme.INSET);
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, 1f - knob))));
            g2.fillRoundRect(0, y, W, trackH, arc, arc);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, y, W - 1, trackH - 1, arc, arc);
        }

        // Knob.
        int d = trackH - 8;
        int kx = 4 + Math.round(knob * (W - d - 8));
        int ky = y + 4;
        Paint.dropShadow(g2, kx, ky, d, d, d, 2, 40);
        g2.setPaint(Paint.vGradient(ky, ky + d, Color.WHITE, new Color(0xEC, 0xE8, 0xE0)));
        g2.fillRoundRect(kx, ky, d, d, d, d);
        g2.setColor(Theme.BORDER_HI);
        g2.drawRoundRect(kx, ky, d - 1, d - 1, d, d);

        if (focused) {
            Paint.focusRing(g2, 0, y, W, trackH, arc);
        }
        g2.dispose();
    }
}
