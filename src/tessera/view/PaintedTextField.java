package tessera.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

/**
 * A de-stocked single-line text input: a raised rounded field with an accent
 * focus ring and a soft inner accent glow, plus an accent caret. Used for the
 * results-screen name entry. Paints its own background and border (the field is
 * non-opaque), then lets {@link JTextField} draw the text on top.
 */
@SuppressWarnings("serial") // Swing component; never serialized.
public final class PaintedTextField extends JTextField {

    @SuppressWarnings("this-escape")
    public PaintedTextField(int columns) {
        super(columns);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
        setFont(Theme.body());
        setForeground(Theme.TEXT_PRIMARY);
        setCaretColor(Theme.ACCENT);
        setSelectionColor(Theme.ACCENT_TINT);
        setSelectedTextColor(Theme.TEXT_PRIMARY);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = Theme.R_INPUT;
        boolean focused = isFocusOwner();

        g2.setColor(Theme.SURFACE_RAISED);
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

        if (focused) {
            // 2px accent ring plus a soft inner glow (the "28/12%" accent halo).
            g2.setColor(Paint.alpha(Theme.ACCENT, 30));
            g2.setStroke(new java.awt.BasicStroke(4f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, arc, arc);
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new java.awt.BasicStroke(Theme.STROKE_FOCUS));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        } else {
            g2.setColor(Theme.BORDER_HI);
            g2.setStroke(new java.awt.BasicStroke(Theme.STROKE_HAIR));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
