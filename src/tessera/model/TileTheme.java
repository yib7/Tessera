package tessera.model;

import java.util.Arrays;
import java.util.List;

/**
 * A tile theme is a named palette of distinct face values plus a single accent
 * colour used when the faces are drawn. The hardest board needs 28 distinct
 * faces, so every theme must supply at least that many.
 *
 * <p>Faces are stored as strings so a theme can mix single glyphs (letters,
 * digits, geometric symbols) without committing the rest of the code to a
 * {@code char}. Nothing here is an image; the view paints these glyphs, so the
 * jar carries no bundled art.
 */
public enum TileTheme {

    LETTERS("Letters",
            0x4C6EF5,
            chars("ABCDEFGHIJKLMNOPQRSTUVWXYZ") /* 26 */,
            chars("0123456789") /* +10 = 36 */),

    DIGITS("Numbers",
            0x12B886,
            split("1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 "
                    + "21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36")),

    SHAPES("Symbols",
            0xE8590C,
            // 36 distinct geometric / box-drawing / arrow glyphs, all in the
            // basic multilingual plane so a default JVM font renders them.
            split("■ ● ▲ ◆ ★ ♠ ♣ ♥ ♦ □ "
                    + "○ △ ◇ ☆ ← ↑ → ↓ ↔ ↕ "
                    + "♪ ♫ ✓ ✗ § ¶ ⊕ ⊗ ⊞ ⊠ "
                    + "⁂ ※ ⌘ ⌨ ⚑ ⚐"));

    private final String displayName;
    private final int accentRgb;
    private final String[] faces;

    TileTheme(String displayName, int accentRgb, String[]... faceGroups) {
        this.displayName = displayName;
        this.accentRgb = accentRgb;
        int total = 0;
        for (String[] group : faceGroups) {
            total += group.length;
        }
        String[] merged = new String[total];
        int i = 0;
        for (String[] group : faceGroups) {
            for (String face : group) {
                merged[i++] = face;
            }
        }
        this.faces = merged;
    }

    public String displayName() {
        return displayName;
    }

    /** Packed 0xRRGGBB accent colour; the view wraps it in a Color. */
    public int accentRgb() {
        return accentRgb;
    }

    /** Number of distinct faces this theme can supply. */
    public int faceCount() {
        return faces.length;
    }

    /**
     * The first {@code count} distinct faces. The dealer asks for exactly one
     * face per pair, so this guards against requesting more than exist.
     */
    public List<String> faces(int count) {
        if (count < 0 || count > faces.length) {
            throw new IllegalArgumentException(
                    "Theme " + displayName + " has " + faces.length
                            + " faces; requested " + count + ".");
        }
        return Arrays.asList(Arrays.copyOf(faces, count));
    }

    /**
     * Resolve a saved theme name (either {@link #displayName} or the enum
     * {@link #name}) to a theme, or null if none matches. Callers that must
     * distinguish "no theme recorded" from a known theme use this; callers that
     * want a safe default use {@link #fromName}.
     */
    public static TileTheme tryFromName(String name) {
        if (name != null) {
            for (TileTheme theme : values()) {
                if (theme.displayName.equalsIgnoreCase(name.trim())
                        || theme.name().equalsIgnoreCase(name.trim())) {
                    return theme;
                }
            }
        }
        return null;
    }

    public static TileTheme fromName(String name) {
        TileTheme theme = tryFromName(name);
        return theme != null ? theme : LETTERS;
    }

    private static String[] chars(String s) {
        String[] out = new String[s.length()];
        for (int i = 0; i < s.length(); i++) {
            out[i] = String.valueOf(s.charAt(i));
        }
        return out;
    }

    private static String[] split(String s) {
        return s.trim().split("\\s+");
    }
}
