package tessera.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Small builders for the styled controls the panels share, so the look stays
 * consistent and the panels read as layout rather than per-widget styling.
 */
public final class UiFactory {

    private UiFactory() {
    }

    /** A flat, rounded, accent-coloured button used for primary actions. */
    public static JButton primaryButton(String text) {
        return pillButton(text, Theme.ACCENT, Theme.ACCENT_TEXT);
    }

    /** A flat, rounded, muted button used for secondary actions. */
    public static JButton secondaryButton(String text) {
        return pillButton(text, Theme.SURFACE_RAISED, Theme.TEXT_PRIMARY);
    }

    private static JButton pillButton(String text, Color base, Color fg) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = base;
                if (getModel().isPressed()) {
                    fill = base.darker();
                } else if (getModel().isRollover()) {
                    fill = base.brighter();
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(fg);
        button.setFont(Theme.label());
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        return button;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.title());
        label.setForeground(Theme.TEXT_PRIMARY);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.heading());
        label.setForeground(Theme.TEXT_PRIMARY);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.body());
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    /** Fixed-size rigid gap. */
    public static Dimension gap(int w, int h) {
        return new Dimension(w, h);
    }

    /** Attach a hover cursor without changing painting. */
    public static void handCursor(JButton b) {
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
    }
}
