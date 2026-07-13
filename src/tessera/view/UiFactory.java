package tessera.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Builders for the styled controls the panels share, so the look stays
 * consistent and the panels read as layout rather than per-widget styling. Every
 * control is painted by hand from {@link Theme} colours and {@link Paint}
 * recipes — gradient fills, painted shadows and top-edge highlights, layered
 * focus rings — with no look-and-feel library and no image assets. The larger
 * de-stocked controls (segmented picker, dropdown, toggle, text field, table)
 * live in their own classes; this class covers buttons, labels, chips and badges.
 */
public final class UiFactory {

    private UiFactory() {
    }

    /** Button visual weight. */
    public enum Kind { PRIMARY, SECONDARY, GHOST }

    /** Accent-filled button for the single primary action on a screen. */
    public static JButton primaryButton(String text) {
        return button(text, Kind.PRIMARY);
    }

    /** Raised paper button for secondary actions. */
    public static JButton secondaryButton(String text) {
        return button(text, Kind.SECONDARY);
    }

    /** Quiet, chromeless button for minor or destructive actions (e.g. Quit). */
    public static JButton ghostButton(String text) {
        return button(text, Kind.GHOST);
    }

    public static JButton button(String text, Kind kind) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                paintButton(g2, this, kind);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(foreground(kind, true));
        button.setFont(Theme.label());
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(11, 24, 13, 24));
        return button;
    }

    private static Color foreground(Kind kind, boolean enabled) {
        if (!enabled) {
            return Theme.TEXT_DIM;
        }
        return switch (kind) {
            case PRIMARY -> Theme.ACCENT_TEXT;
            case SECONDARY -> Theme.TEXT_PRIMARY;
            case GHOST -> Theme.TEXT_MUTED;
        };
    }

    private static void paintButton(Graphics2D g2, JButton b, Kind kind) {
        int w = b.getWidth();
        int h = b.getHeight();
        int arc = Theme.R_BTN;
        boolean pressed = b.getModel().isPressed() && b.getModel().isArmed();
        boolean hover = b.getModel().isRollover();
        boolean enabled = b.isEnabled();
        boolean focused = b.isFocusOwner();

        // Body sits inside a small margin so the painted shadow has room below.
        int x = 1;
        int y = 1;
        int bw = w - 2;
        int bh = h - 4;
        int drop = pressed ? 1 : 3;
        if (pressed) {
            y += 1; // nudge the whole body down on press
        }

        // Keep the label colour in step with enabled/disabled.
        b.setForeground(foreground(kind, enabled));

        if (!enabled) {
            g2.setColor(Theme.BORDER);
            g2.fillRoundRect(x, y, bw, bh, arc, arc);
            return;
        }

        if (kind == Kind.GHOST) {
            if (pressed) {
                g2.setColor(Paint.alpha(Theme.SHADOW, 26));
                g2.fillRoundRect(x, y, bw, bh, arc, arc);
            } else if (hover) {
                g2.setColor(Paint.alpha(Theme.SHADOW, 15)); // warm 6% wash
                g2.fillRoundRect(x, y, bw, bh, arc, arc);
            }
            if (focused) {
                Paint.focusRing(g2, x, y, bw, bh, arc);
            }
            return;
        }

        if (!pressed) {
            Paint.dropShadow(g2, x, y, bw, bh, arc, drop,
                    kind == Kind.PRIMARY ? 46 : 30);
        }

        Color top;
        Color bot;
        if (kind == Kind.PRIMARY) {
            top = hover ? new Color(0x5C, 0x7C, 0xFA) : Theme.ACCENT_HI;
            bot = Theme.ACCENT;
            if (pressed) {
                top = Theme.ACCENT_LO;
                bot = Theme.ACCENT_LO;
            }
        } else {
            top = hover ? Color.WHITE : Color.WHITE;
            bot = hover ? new Color(0xFB, 0xF9, 0xF3) : new Color(0xF6, 0xF3, 0xEC);
            if (pressed) {
                top = new Color(0xF0, 0xEC, 0xE2);
                bot = new Color(0xEA, 0xE5, 0xD8);
            }
        }
        g2.setPaint(Paint.vGradient(y, y + bh, top, bot));
        g2.fillRoundRect(x, y, bw, bh, arc, arc);

        if (kind == Kind.SECONDARY) {
            g2.setColor(Theme.BORDER_HI);
            g2.drawRoundRect(x, y, bw - 1, bh - 1, arc, arc);
        }
        if (!pressed) {
            Paint.topHighlight(g2, x, y, bw, bh, arc, kind == Kind.PRIMARY ? 72 : 180);
        }
        if (focused) {
            Paint.focusRing(g2, x, y, bw, bh, arc);
        }
    }

    // ---- Labels ------------------------------------------------------------

    /** Brand/title label (menu). */
    public static JLabel title(String text) {
        return label(text, Theme.tracked(Theme.title(), 0.01f), Theme.TEXT_PRIMARY);
    }

    /** Screen title (Settings, Leaderboard, Results). */
    public static JLabel screenTitle(String text) {
        return label(text, Theme.screenTitle(), Theme.TEXT_PRIMARY);
    }

    /** Section heading. */
    public static JLabel heading(String text) {
        return label(text, Theme.heading(), Theme.TEXT_PRIMARY);
    }

    /** Muted body / tagline. */
    public static JLabel muted(String text) {
        return label(text, Theme.body(), Theme.TEXT_MUTED);
    }

    /** Small uppercase letter-spaced caption / eyebrow. */
    public static JLabel caption(String text) {
        JLabel label = label(text.toUpperCase(), Theme.tracked(Theme.caption(), 0.14f),
                Theme.TEXT_MUTED);
        return label;
    }

    private static JLabel label(String text, Font font, Color fg) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(fg);
        return label;
    }

    // ---- Chips & badges ----------------------------------------------------

    /** A HUD chip: a small letter-spaced caption over a bold tabular value. */
    public static Chip hudChip(String caption) {
        return new Chip(caption, false);
    }

    /** The accent-tinted, pulsing countdown chip used during the memorize phase. */
    public static Chip countdownChip(String caption) {
        return new Chip(caption, true);
    }

    /** A small raised pill label — a one-line summary badge (e.g. the menu's size/theme). */
    public static JLabel pill(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Paint.dropShadow(g2, 1, 1, w - 2, h - 4, h, 2, 22);
                g2.setColor(Theme.SURFACE_RAISED);
                g2.fillRoundRect(1, 1, w - 2, h - 4, h, h);
                g2.setColor(Theme.BORDER_HI);
                g2.drawRoundRect(1, 1, w - 3, h - 5, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setFont(Theme.label());
        label.setForeground(Theme.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(7, 18, 9, 18));
        return label;
    }
}
