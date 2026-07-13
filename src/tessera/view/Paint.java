package tessera.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared Graphics2D paint recipes for the whole view package: the tile, the
 * widget factory and the screen panels build their depth from these, so the
 * recipes live in one place instead of being copied per component.
 *
 * <p>Under the project's constraints there is no CSS, no blur and no real
 * box-shadow, so every effect here is a cheap painted approximation — an offset
 * translucent round-rect for a shadow, layered strokes at decreasing alpha for a
 * glow, a linear or radial gradient for a sheen. Every shadow and glow is tinted
 * from {@link Theme#SHADOW} (a warm near-black), never pure black, to preserve
 * the warm-paper feel. Helpers restore any {@link Stroke}/{@link Color}/clip they
 * change so callers can treat them as side-effect free.
 */
final class Paint {

    private Paint() {
    }

    /** A copy of {@code c} at the given alpha (0–255, clamped). */
    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, a)));
    }

    /** Vertical linear gradient: {@code top} at {@code yTop} → {@code bottom} at {@code yBottom}. */
    static GradientPaint vGradient(int yTop, int yBottom, Color top, Color bottom) {
        int y1 = yBottom <= yTop ? yTop + 1 : yBottom; // GradientPaint needs distinct points
        return new GradientPaint(0f, yTop, top, 0f, y1, bottom);
    }

    /**
     * Radial pool of light: {@link Theme#BG_POOL} at the centre fading to
     * {@link Theme#BACKGROUND} at {@code radius}. Panels paint this behind the
     * content column so the play area feels lit.
     */
    static RadialGradientPaint poolOfLight(float cx, float cy, float radius) {
        float r = Math.max(1f, radius);
        return new RadialGradientPaint(cx, cy, r,
                new float[] {0f, 1f},
                new Color[] {Theme.BG_POOL, Theme.BACKGROUND});
    }

    /**
     * Cheap drop shadow: a translucent {@link Theme#SHADOW} round-rect the same
     * size as the element, offset down by {@code dy}. Paint it before the element
     * so the element lands on top.
     */
    static void dropShadow(Graphics2D g, int x, int y, int w, int h, int arc, int dy, int a) {
        g.setColor(alpha(Theme.SHADOW, a));
        g.fillRoundRect(x, y + dy, w, h, arc, arc);
    }

    /** A 1px white inner highlight along the top edge and upper corners at {@code whiteAlpha}. */
    static void topHighlight(Graphics2D g, int x, int y, int w, int h, int arc, int whiteAlpha) {
        Stroke oldStroke = g.getStroke();
        Shape oldClip = g.getClip();
        g.clip(new RoundRectangle2D.Float(x, y, w, Math.max(2f, h * 0.5f), arc, arc));
        g.setColor(alpha(Color.WHITE, whiteAlpha));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
        g.setClip(oldClip);
        g.setStroke(oldStroke);
    }

    /**
     * Distinct keyboard-focus treatment, painted INSIDE the element bounds so a
     * component that clips to its own bounds never loses it: a soft accent halo
     * just inside the edge plus a crisp 2px accent ring. Deliberately unlike any
     * hover fill, so focus and hover never look the same.
     */
    static void focusRing(Graphics2D g, int x, int y, int w, int h, int arc) {
        Stroke old = g.getStroke();
        g.setColor(alpha(Theme.ACCENT, 45));
        g.setStroke(new BasicStroke(4f));
        int innerArc = Math.max(4, arc - 2);
        g.drawRoundRect(x + 3, y + 3, w - 7, h - 7, innerArc, innerArc);
        g.setColor(Theme.ACCENT);
        g.setStroke(new BasicStroke(Theme.STROKE_FOCUS));
        g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
        g.setStroke(old);
    }

    /**
     * Layered-stroke glow just outside a round-rect: {@code layers} strokes at
     * growing radius and decreasing alpha, so the element looks lit without any
     * real blur. Keep {@code layers} small (2–3) and leave a matching margin
     * around the element so the outermost ring is not clipped.
     */
    static void glow(Graphics2D g, int x, int y, int w, int h, int arc, Color color, int layers) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f));
        for (int i = 1; i <= layers; i++) {
            g.setColor(alpha(color, Math.round(70f / i)));
            g.drawRoundRect(x - i, y - i, w - 1 + 2 * i, h - 1 + 2 * i, arc + i, arc + i);
        }
        g.setStroke(old);
    }

    /** The tessera diamond motif: a 4-point polygon of radius {@code r} about (cx,cy). */
    static void diamond(Graphics2D g, int cx, int cy, int r, Color fill, Color stroke, float strokeW) {
        int[] xs = {cx, cx + r, cx, cx - r};
        int[] ys = {cy - r, cy, cy + r, cy};
        if (fill != null) {
            g.setColor(fill);
            g.fillPolygon(xs, ys, 4);
        }
        if (stroke != null) {
            Stroke old = g.getStroke();
            g.setColor(stroke);
            g.setStroke(new BasicStroke(strokeW));
            g.drawPolygon(xs, ys, 4);
            g.setStroke(old);
        }
    }
}
