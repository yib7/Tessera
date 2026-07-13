package tessera.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.UIManager;

/**
 * The single source of colours, fonts, radii and stroke widths for the whole
 * UI. Tessera does not depend on an external look and feel; it sets a few
 * UIManager defaults and paints its own tiles and widgets, so the jar carries no
 * theming library.
 *
 * <p>The palette is the bright "Tessera" scheme: a warm bone background with a
 * radial pool of light, three elevation layers, one blue accent, a success green
 * for matches and an error red for mismatches. Every value here maps to a hex
 * colour, a font, a radius or a stroke in the design token sheet, and every
 * painted effect (shadow, glow, highlight) is built from these in code — no
 * assets, no CSS.
 */
public final class Theme {

    // ---- Elevation: three layers on warm paper -----------------------------
    /** L0 — the window / table. Warm bone; everything sits above it. */
    public static final Color BACKGROUND = new Color(0xEC, 0xE7, 0xDC);
    /** Radial pool-of-light centre painted behind the content column / board. */
    public static final Color BG_POOL = new Color(0xF8, 0xF5, 0xEE);
    /** L1 — cards, board well, menus, table. */
    public static final Color SURFACE = new Color(0xFB, 0xFA, 0xF6);
    /** L2 — raised interactive elements: buttons, chips, tile-back base. */
    public static final Color SURFACE_RAISED = new Color(0xFF, 0xFF, 0xFF);
    /** Recessed wells: input fields, segmented track. */
    public static final Color INSET = new Color(0xE7, 0xE1, 0xD4);

    // ---- Accent (blue) -----------------------------------------------------
    public static final Color ACCENT = new Color(0x3B, 0x5B, 0xDB);
    /** Gradient top / hover. */
    public static final Color ACCENT_HI = new Color(0x4C, 0x6E, 0xF5);
    /** Pressed / rank-3 label. */
    public static final Color ACCENT_LO = new Color(0x2E, 0x45, 0xA8);
    /** Light accent wash: countdown chip, selected menu row. */
    public static final Color ACCENT_TINT = new Color(0xE7, 0xEB, 0xFB);
    public static final Color ACCENT_TEXT = Color.WHITE;

    // ---- Success (green) / matched tile ------------------------------------
    public static final Color SUCCESS = new Color(0x2F, 0x9E, 0x44);
    public static final Color SUCCESS_HI = new Color(0x40, 0xC0, 0x57);
    /** Face fill of a matched tile (light green). */
    public static final Color TILE_MATCHED = new Color(0xE6, 0xF5, 0xEA);
    public static final Color TILE_MATCHED_BORDER = new Color(0x40, 0xC0, 0x57);
    /** Dark green glyph — high contrast on the light-green matched face. */
    public static final Color MATCHED_GLYPH = new Color(0x1B, 0x7A, 0x32);

    // ---- Error (red) — mismatch flash --------------------------------------
    public static final Color ERROR = new Color(0xE0, 0x31, 0x31);
    public static final Color ERROR_DEEP = new Color(0xC9, 0x2A, 0x2A);

    // ---- Text (ink on light) -----------------------------------------------
    public static final Color TEXT_PRIMARY = new Color(0x28, 0x2B, 0x31);
    public static final Color TEXT_MUTED = new Color(0x6B, 0x72, 0x80);
    public static final Color TEXT_DIM = new Color(0xA3, 0xA9, 0xB2);

    // ---- Tile face (up) ----------------------------------------------------
    public static final Color FACE_TOP = new Color(0xFF, 0xFF, 0xFF);
    public static final Color FACE_BOT = new Color(0xF1, 0xEF, 0xE8);
    /** Default face glyph colour; per-theme tile accents override it. */
    public static final Color FACE_GLYPH = new Color(0x3B, 0x5B, 0xDB);
    /** Flat stand-in for the face; the tile paints the FACE_TOP->FACE_BOT gradient. */
    public static final Color TILE_FACE = new Color(0xFF, 0xFF, 0xFF);

    // ---- Tile back (periwinkle) --------------------------------------------
    public static final Color TB_TOP = new Color(0xEE, 0xF1, 0xFC);
    public static final Color TB_BOT = new Color(0xDC, 0xE3, 0xF7);
    public static final Color TB_BRD = new Color(0xC6, 0xD0, 0xEE);
    /** Flat stand-in for the back; the tile paints the TB_TOP->TB_BOT gradient. */
    public static final Color TILE_BACK = new Color(0xDC, 0xE3, 0xF7);
    public static final Color TILE_BACK_HOVER = new Color(0xE6, 0xEC, 0xFB);

    // ---- Lines & painted shadow --------------------------------------------
    /** 1px card / well outline (warm hairline). */
    public static final Color BORDER = new Color(0xDC, 0xD6, 0xC9);
    /** 1px raised-element outline. */
    public static final Color BORDER_HI = new Color(0xCF, 0xC8, 0xB9);
    /**
     * Warm near-black used for every painted shadow/glow, always at low alpha —
     * never pure black, so the paper feel is preserved.
     */
    public static final Color SHADOW = new Color(0x46, 0x3E, 0x30);

    // ---- Radii (px) --------------------------------------------------------
    public static final int R_TILE = 14;
    public static final int R_BTN = 12;
    public static final int R_CHIP = 12;
    public static final int R_INPUT = 10;
    public static final int R_WELL = 20;

    // ---- Stroke widths (px) ------------------------------------------------
    public static final float STROKE_HAIR = 1f;
    public static final float STROKE_FOCUS = 2f;
    public static final float STROKE_ERROR = 3f;

    private static final String FONT_FAMILY = pickUiFontFamily();
    private static final String MONO_FAMILY = pickMonoFontFamily();

    private Theme() {
    }

    // ---- Fonts -------------------------------------------------------------
    // JDK-safe logical families, regular + bold only. Hierarchy comes from
    // size, tracking and colour, never a faux weight. Tabular values (score,
    // time, ranks) use the monospaced family so their width never jitters.

    /** Brand title (menu). */
    public static Font title() {
        return new Font(FONT_FAMILY, Font.BOLD, 46);
    }

    /** Screen title (Settings, Leaderboard). */
    public static Font screenTitle() {
        return new Font(FONT_FAMILY, Font.BOLD, 30);
    }

    /** Section heading ("Paused", "Solved"). */
    public static Font heading() {
        return new Font(FONT_FAMILY, Font.BOLD, 24);
    }

    /** Board / panel title (HUD "Normal board"). */
    public static Font boardTitle() {
        return new Font(FONT_FAMILY, Font.BOLD, 20);
    }

    /** Body / tagline. */
    public static Font body() {
        return new Font(FONT_FAMILY, Font.PLAIN, 15);
    }

    /** Button label / bold inline label. */
    public static Font label() {
        return new Font(FONT_FAMILY, Font.BOLD, 14);
    }

    /** Legacy HUD label (bold sans 18); superseded by {@link #hudValue()}. */
    public static Font hud() {
        return new Font(FONT_FAMILY, Font.BOLD, 18);
    }

    /** HUD chip value, tabular. */
    public static Font hudValue() {
        return new Font(MONO_FAMILY, Font.BOLD, 22);
    }

    /** Hero numeral on the results screen (score), tabular. */
    public static Font scoreHero() {
        return new Font(MONO_FAMILY, Font.BOLD, 78);
    }

    /** Small uppercase caption / eyebrow (pair with {@link #tracked}). */
    public static Font caption() {
        return new Font(FONT_FAMILY, Font.BOLD, 11);
    }

    /** Monospaced font at an arbitrary size (ranks, small tabular values). */
    public static Font mono(int size, boolean bold) {
        return new Font(MONO_FAMILY, bold ? Font.BOLD : Font.PLAIN, size);
    }

    /**
     * Derive a letter-spaced ("tracked") variant of a font. Swing has no
     * per-string letter-spacing, so captions and titles that need tracking use
     * a {@link TextAttribute#TRACKING} derivation. {@code tracking} is a
     * fraction of the point size (e.g. 0.14 ~ the caption tracking in the spec).
     */
    public static Font tracked(Font base, float tracking) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attrs);
    }

    private static final Map<Integer, Font> TILE_FONTS = new HashMap<>();

    /** Tile glyph font, sized to the tile so big boards still read. Cached by
     *  computed size; called on the EDT so a plain HashMap is safe. */
    public static Font tileFont(int tileSize) {
        int size = Math.max(14, Math.min(48, (int) (tileSize * 0.45)));
        return TILE_FONTS.computeIfAbsent(size, s -> new Font(FONT_FAMILY, Font.BOLD, s));
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
        UIManager.put("ToolTip.background", SURFACE);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
    }

    /**
     * Prefer a clean sans-serif the JVM reports as installed; fall back to the
     * logical "SansSerif" family, which every JVM provides. This avoids naming a
     * font that does not exist (the old code asked for "Comic Sans", which Swing
     * silently replaced with a default).
     */
    private static String pickUiFontFamily() {
        Set<String> available = availableFamilies();
        for (String candidate : new String[] {
                "Segoe UI", "Helvetica Neue", "Roboto", "Arial", "DejaVu Sans" }) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    /**
     * Prefer an installed monospaced family for tabular digits; fall back to the
     * logical "Monospaced" family, which every JVM provides.
     */
    private static String pickMonoFontFamily() {
        Set<String> available = availableFamilies();
        for (String candidate : new String[] {
                "DejaVu Sans Mono", "Consolas", "Menlo", "Courier New" }) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.MONOSPACED;
    }

    private static Set<String> availableFamilies() {
        Set<String> available = new HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()) {
            available.add(name);
        }
        return available;
    }
}
