package tessera.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

import javax.swing.UIManager;

/**
 * The single source of colours and fonts for the whole UI. Tessera does not
 * depend on an external look and feel; it sets a few UIManager defaults and
 * paints its own tiles, so the jar carries no theming library. The palette is a
 * cool slate background with one warm accent, chosen so the custom tiles read
 * clearly in both their face-down and face-up states.
 */
public final class Theme {

    // Surfaces, dark to light.
    public static final Color BACKGROUND = new Color(0x1E, 0x22, 0x2B);
    public static final Color SURFACE = new Color(0x2A, 0x2F, 0x3A);
    public static final Color SURFACE_RAISED = new Color(0x35, 0x3C, 0x4A);

    // Tile states.
    public static final Color TILE_BACK = new Color(0x3B, 0x43, 0x54);
    public static final Color TILE_BACK_HOVER = new Color(0x49, 0x53, 0x67);
    public static final Color TILE_FACE = new Color(0xF4, 0xF6, 0xFB);
    public static final Color TILE_MATCHED = new Color(0x24, 0x3A, 0x33);
    public static final Color TILE_MATCHED_BORDER = new Color(0x40, 0xC0, 0x57);

    // Text.
    public static final Color TEXT_PRIMARY = new Color(0xEC, 0xEF, 0xF4);
    public static final Color TEXT_MUTED = new Color(0x9A, 0xA4, 0xB2);

    // Default accent; per-theme tile faces override their own accent.
    public static final Color ACCENT = new Color(0x4C, 0x6E, 0xF5);
    public static final Color ACCENT_TEXT = Color.WHITE;

    private static final String FONT_FAMILY = pickUiFontFamily();

    private Theme() {
    }

    public static Font title() {
        return new Font(FONT_FAMILY, Font.BOLD, 42);
    }

    public static Font heading() {
        return new Font(FONT_FAMILY, Font.BOLD, 24);
    }

    public static Font body() {
        return new Font(FONT_FAMILY, Font.PLAIN, 15);
    }

    public static Font label() {
        return new Font(FONT_FAMILY, Font.BOLD, 14);
    }

    public static Font hud() {
        return new Font(FONT_FAMILY, Font.BOLD, 18);
    }

    /** Tile glyph font, sized to the tile so big boards still read. */
    public static Font tileFont(int tileSize) {
        int size = Math.max(14, Math.min(48, (int) (tileSize * 0.45)));
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    /** Convert a packed 0xRRGGBB int from a TileTheme into a Color. */
    public static Color fromRgb(int rgb) {
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Apply a handful of UIManager defaults so stock Swing controls match the
     * palette without each panel restyling them. Called once at startup.
     */
    public static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("ToolTip.background", SURFACE_RAISED);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
    }

    /**
     * Prefer a clean sans-serif the JVM reports as installed; fall back to the
     * logical "SansSerif" family, which every JVM provides. This avoids naming a
     * font that does not exist (the old code asked for "Comic Sans", which Swing
     * silently replaced with a default).
     */
    private static String pickUiFontFamily() {
        Set<String> available = new HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()) {
            available.add(name);
        }
        for (String candidate : new String[] {
                "Segoe UI", "Helvetica Neue", "Roboto", "Arial", "DejaVu Sans" }) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }
}
