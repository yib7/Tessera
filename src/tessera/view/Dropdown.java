package tessera.view;

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
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A fully custom dropdown: a painted field button that opens a painted popup of
 * rows (a {@link JPopupMenu} of hand-painted items, not a restyled
 * {@link javax.swing.JComboBox}). The selected row is marked with an accent tint
 * and a diamond; hovering/keyboard-arming a row tints it. Used for the tile-theme
 * picker. Generic over the option type with a label function so callers keep
 * their enums.
 *
 * @param <T> the option type
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class Dropdown<T> extends JComponent {

    private static final int HEIGHT = 44;
    private static final int ROW_H = 38;
    private static final Font ROW_FONT = Theme.body();

    private final T[] items;
    private final Function<T, String> labeller;
    private int selected;
    private boolean focused;
    private boolean hover;
    private Consumer<T> onChange;

    public Dropdown(T[] items, Function<T, String> labeller, int selected) {
        this.items = items.clone();
        this.labeller = labeller;
        this.selected = Math.max(0, Math.min(items.length - 1, selected));
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installListeners();
    }

    public T getSelected() {
        return items[selected];
    }

    public void setSelected(T item) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(item)) {
                selected = i;
                repaint();
                return;
            }
        }
    }

    public void setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
    }

    private void choose(int index) {
        if (index >= 0 && index < items.length && index != selected) {
            selected = index;
            repaint();
            if (onChange != null) {
                onChange.accept(items[index]);
            }
        }
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                openPopup();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
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
                if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_DOWN) {
                    openPopup();
                }
            }
        });
    }

    private void openPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER_HI));
        popup.setBackground(Theme.SURFACE);
        int width = Math.max(getWidth(), getPreferredSize().width);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            final boolean sel = i == selected;
            JMenuItem item = new JMenuItem(labeller.apply(items[i])) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    paintRow(g2, getText(), getWidth(), getHeight(), sel, getModel().isArmed());
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setPreferredSize(new Dimension(width, ROW_H));
            item.addActionListener(e -> choose(idx));
            popup.add(item);
        }
        popup.show(this, 0, getHeight() + 4);
    }

    /** Row height used by the popup; also handy for an "open" preview. */
    public static int rowHeight() {
        return ROW_H;
    }

    /**
     * Paint one option row. Shared by the live {@link JMenuItem} popup and the
     * render harness so an "open" preview matches the real popup exactly.
     */
    public static void paintRow(Graphics2D g2, String label, int w, int h, boolean selected, boolean armed) {
        if (armed) {
            g2.setColor(Theme.ACCENT_TINT);
            g2.fillRect(0, 0, w, h);
        } else if (selected) {
            g2.setColor(Paint.alpha(Theme.ACCENT_TINT, 150));
            g2.fillRect(0, 0, w, h);
        }
        if (selected) {
            Paint.diamond(g2, 16, h / 2, 4, Theme.ACCENT, null, 0f);
        }
        g2.setFont(ROW_FONT);
        g2.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, 32, (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(ROW_FONT);
        int textW = 0;
        for (T item : items) {
            textW = Math.max(textW, fm.stringWidth(labeller.apply(item)));
        }
        return new Dimension(textW + 32 + 40, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = Theme.R_INPUT;
        int x = 1;
        int y = 1;
        int bw = w - 2;
        int bh = h - 4;

        Paint.dropShadow(g2, x, y, bw, bh, arc, 2, 26);
        g2.setPaint(Paint.vGradient(y, y + bh, Theme.SURFACE_RAISED, new Color(0xF6, 0xF3, 0xEC)));
        g2.fillRoundRect(x, y, bw, bh, arc, arc);
        g2.setColor(hover ? Paint.alpha(Theme.ACCENT, 120) : Theme.BORDER_HI);
        g2.drawRoundRect(x, y, bw - 1, bh - 1, arc, arc);
        Paint.topHighlight(g2, x, y, bw, bh, arc, 180);

        g2.setFont(ROW_FONT);
        g2.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(labeller.apply(items[selected]), x + 14,
                y + (bh - fm.getHeight()) / 2 + fm.getAscent());

        // Chevron.
        int cx = x + bw - 22;
        int cy = y + bh / 2;
        g2.setColor(Theme.TEXT_MUTED);
        int[] xs = {cx, cx + 10, cx + 5};
        int[] ys = {cy - 3, cy - 3, cy + 4};
        g2.fillPolygon(xs, ys, 3);

        if (focused) {
            Paint.focusRing(g2, x, y, bw, bh, arc);
        }
        g2.dispose();
    }
}
