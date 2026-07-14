package tessera.view;

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
import java.util.function.IntConsumer;

import javax.swing.JComponent;

/**
 * A de-stocked segmented control: an inset track holding one cell per option,
 * with the selected cell lifted as a white raised chip carrying a 2px accent
 * underline and the rest shown as muted labels. Replaces a {@link javax.swing.JComboBox}
 * where the options are few and worth showing at once (the board-size picker).
 * Painted entirely from {@link Theme} colours; keyboard- and mouse-operable.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class SegmentedPicker extends JComponent {

    private static final int TRACK_PAD = 4;
    private static final int HEIGHT = 46;
    private static final int CELL_PAD_X = 18;

    private final String[] segments;
    private final Font font = Theme.label();
    private int selected;
    private int hovered = -1;
    private boolean focused;
    private IntConsumer onChange;

    public SegmentedPicker(String[] segments, int selected) {
        this.segments = segments.clone();
        this.selected = Math.max(0, Math.min(segments.length - 1, selected));
        setOpaque(false);
        setFocusable(true);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        installListeners();
    }

    public int getSelectedIndex() {
        return selected;
    }

    public void setSelectedIndex(int index) {
        int clamped = Math.max(0, Math.min(segments.length - 1, index));
        if (clamped != selected) {
            selected = clamped;
            repaint();
        }
    }

    /** Called with the new index whenever the user changes the selection. */
    public void setOnChange(IntConsumer onChange) {
        this.onChange = onChange;
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= segments.length || index == selected) {
            return;
        }
        selected = index;
        repaint();
        if (notify && onChange != null) {
            onChange.accept(index);
        }
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int i = cellAt(e.getX());
                if (i >= 0) {
                    requestFocusInWindow();
                    select(i, true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = -1;
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = cellAt(e.getX());
                if (i != hovered) {
                    hovered = i;
                    repaint();
                }
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
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    select(selected - 1, true);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    select(selected + 1, true);
                }
            }
        });
    }

    private int cellAt(int x) {
        int cw = cellWidth();
        int start = TRACK_PAD;
        for (int i = 0; i < segments.length; i++) {
            if (x >= start + i * cw && x < start + (i + 1) * cw) {
                return i;
            }
        }
        return -1;
    }

    private int cellWidth() {
        return (getWidth() - 2 * TRACK_PAD) / Math.max(1, segments.length);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        int cell = 0;
        for (String s : segments) {
            cell = Math.max(cell, fm.stringWidth(s) + CELL_PAD_X * 2);
        }
        return new Dimension(cell * segments.length + 2 * TRACK_PAD, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = Theme.R_INPUT + 4;

        // Inset track.
        g2.setColor(Theme.INSET);
        g2.fillRoundRect(0, 0, w, h, arc, arc);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        int cw = cellWidth();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < segments.length; i++) {
            int cx = TRACK_PAD + i * cw;
            int cyTop = TRACK_PAD;
            int ch = h - 2 * TRACK_PAD;
            boolean isSel = i == selected;

            if (isSel) {
                Paint.dropShadow(g2, cx + 2, cyTop, cw - 4, ch, Theme.R_INPUT, 2, 30);
                g2.setColor(Theme.SURFACE_RAISED);
                g2.fillRoundRect(cx + 2, cyTop, cw - 4, ch, Theme.R_INPUT, Theme.R_INPUT);
                Paint.topHighlight(g2, cx + 2, cyTop, cw - 4, ch, Theme.R_INPUT, 200);
                // Accent underline.
                g2.setColor(Theme.ACCENT);
                int uy = cyTop + ch - 6;
                int uw = Math.min(cw - 20, fm.stringWidth(segments[i]) + 10);
                g2.fillRoundRect(cx + (cw - uw) / 2, uy, uw, 3, 3, 3);
            } else if (i == hovered) {
                g2.setColor(Paint.alpha(Theme.SHADOW, 12));
                g2.fillRoundRect(cx + 2, cyTop, cw - 4, ch, Theme.R_INPUT, Theme.R_INPUT);
            }

            g2.setColor(isSel ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);
            int tw = fm.stringWidth(segments[i]);
            int tx = cx + (cw - tw) / 2;
            int ty = cyTop + (ch - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(segments[i], tx, ty);
        }

        if (focused) {
            Paint.focusRing(g2, 0, 0, w, h, arc);
        }
        g2.dispose();
    }
}
